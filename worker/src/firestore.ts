/**
 * Tiny Firestore REST wrapper — no SDK, just fetch().
 *
 * For unauthenticated read of public collections we hit the REST endpoint with
 * the public Web API key. Firestore rules restrict /places to authenticated
 * users, but the Worker is "the server" — we layer App Check on the Android
 * client so only our APK can reach this Worker.
 */
import type { Env, Place } from "./types";

const BASE = (projectId: string) =>
  `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;

type FsValue =
  | { stringValue: string }
  | { integerValue: string }
  | { doubleValue: number }
  | { booleanValue: boolean }
  | { arrayValue: { values?: FsValue[] } }
  | { mapValue: { fields: Record<string, FsValue> } };

function readVal(v: FsValue | undefined): unknown {
  if (!v) return undefined;
  if ("stringValue" in v) return v.stringValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("doubleValue" in v) return v.doubleValue;
  if ("booleanValue" in v) return v.booleanValue;
  if ("arrayValue" in v)
    return (v.arrayValue.values ?? []).map(readVal);
  if ("mapValue" in v) {
    const out: Record<string, unknown> = {};
    for (const [k, val] of Object.entries(v.mapValue.fields)) out[k] = readVal(val);
    return out;
  }
  return undefined;
}

interface FsDoc {
  name: string;
  fields: Record<string, FsValue>;
}

function parseDoc(doc: FsDoc): Place {
  const id = doc.name.split("/").pop() ?? "";
  const f = doc.fields;
  return {
    id,
    name: readVal(f.name) as string,
    city: readVal(f.city) as string,
    region: readVal(f.region) as string,
    address: readVal(f.address) as string,
    geo: readVal(f.geo) as Place["geo"],
    vibe: (readVal(f.vibe) as string[]) ?? [],
    crowd: readVal(f.crowd) as Place["crowd"],
    good_for: (readVal(f.good_for) as Place["good_for"]) ?? [],
    temp_class: readVal(f.temp_class) as Place["temp_class"],
    has_stay: (readVal(f.has_stay) as boolean) ?? false,
    duration_class: readVal(f.duration_class) as Place["duration_class"],
    budget: (readVal(f.budget) as number) ?? 1,
    season: (readVal(f.season) as string[]) ?? [],
    drive_time_from_blr_min: (readVal(f.drive_time_from_blr_min) as number) ?? 0,
    photo_urls: (readVal(f.photo_urls) as string[]) ?? [],
    short_summary: (readVal(f.short_summary) as string) ?? "",
    maps_query: (readVal(f.maps_query) as string) ?? "",
  };
}

/**
 * Pull all places for a city (or list of cities). For ~300 docs this is fine
 * on Spark plan — one structured query per /recommend call.
 */
export async function fetchCandidatePlaces(
  env: Env,
  cities: string[],
  monthIso: string,
  stayRequired: boolean
): Promise<Place[]> {
  const url = `${BASE(env.FIREBASE_PROJECT_ID)}:runQuery?key=${env.FIREBASE_API_KEY}`;

  const body = {
    structuredQuery: {
      from: [{ collectionId: "places" }],
      where: {
        compositeFilter: {
          op: "AND",
          filters: [
            {
              fieldFilter: {
                field: { fieldPath: "city" },
                op: "IN",
                value: { arrayValue: { values: cities.map((c) => ({ stringValue: c })) } },
              },
            },
            {
              fieldFilter: {
                field: { fieldPath: "season" },
                op: "ARRAY_CONTAINS",
                value: { stringValue: monthIso },
              },
            },
            ...(stayRequired
              ? [
                  {
                    fieldFilter: {
                      field: { fieldPath: "has_stay" },
                      op: "EQUAL",
                      value: { booleanValue: true },
                    },
                  },
                ]
              : []),
          ],
        },
      },
      limit: 200,
    },
  };

  const res = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error(`Firestore query failed: ${res.status} ${await res.text()}`);
  }

  const arr = (await res.json()) as Array<{ document?: FsDoc }>;
  return arr.filter((r) => r.document).map((r) => parseDoc(r.document!));
}
