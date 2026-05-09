/**
 * One-shot importer: places.csv -> Firestore /places.
 *
 * Two ways to run:
 *   1. Against the local emulator (no creds, free):
 *        npm run seed:emulator
 *   2. Against your real (Spark-plan) Firebase project:
 *        export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
 *        export FIREBASE_PROJECT_ID=your-project-id
 *        npm run seed
 *
 * The CSV is the source of truth; re-running this script upserts every row by
 * place_id. Rows where tags_overridden=true (set in Firestore manually) will
 * NOT have their tag fields overwritten — only neutral fields refresh.
 */
import { existsSync, readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import admin from "firebase-admin";
import { parse } from "csv-parse/sync";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
// Prefer the enriched CSV (~235 rows from the JioCreate MCP) when present;
// fall back to the hand-curated places.csv otherwise.
const ENRICHED = path.join(__dirname, "places-enriched.csv");
const CSV_PATH = existsSync(ENRICHED) ? ENRICHED : path.join(__dirname, "places.csv");

type Row = Record<string, string>;

interface Place {
  name: string;
  city: string;
  region: string;
  address: string;
  geo: { lat: number; lng: number; geohash: string };
  vibe: string[];
  crowd: "low" | "medium" | "high";
  good_for: string[];
  temp_class: "cool" | "temperate" | "warm";
  has_stay: boolean;
  duration_class: "few-hours" | "half-day" | "full-day" | "weekend";
  budget: number;
  season: string[];
  drive_time_from_blr_min: number;
  photo_urls: string[];
  short_summary: string;
  maps_query: string;
  tags_overridden?: boolean;
}

function pipeList(s: string): string[] {
  return s ? s.split("|").map((x) => x.trim()).filter(Boolean) : [];
}

// Cheap geohash (precision 7) — only used for rough geo queries.
function geohash(lat: number, lng: number, precision = 7): string {
  const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
  let minLat = -90,
    maxLat = 90,
    minLng = -180,
    maxLng = 180;
  let bit = 0,
    ch = 0,
    even = true,
    out = "";
  while (out.length < precision) {
    if (even) {
      const mid = (minLng + maxLng) / 2;
      if (lng >= mid) {
        ch = (ch << 1) | 1;
        minLng = mid;
      } else {
        ch = ch << 1;
        maxLng = mid;
      }
    } else {
      const mid = (minLat + maxLat) / 2;
      if (lat >= mid) {
        ch = (ch << 1) | 1;
        minLat = mid;
      } else {
        ch = ch << 1;
        maxLat = mid;
      }
    }
    even = !even;
    if (++bit === 5) {
      out += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return out;
}

function toPlace(r: Row): Place {
  const lat = Number(r.lat);
  const lng = Number(r.lng);
  return {
    name: r.name,
    city: r.city,
    region: r.region,
    address: r.address,
    geo: { lat, lng, geohash: geohash(lat, lng) },
    vibe: pipeList(r.vibe),
    crowd: r.crowd as Place["crowd"],
    good_for: pipeList(r.good_for),
    temp_class: r.temp_class as Place["temp_class"],
    has_stay: r.has_stay === "true",
    duration_class: r.duration_class as Place["duration_class"],
    budget: Number(r.budget),
    season: pipeList(r.season),
    drive_time_from_blr_min: Number(r.drive_time_from_blr_min),
    photo_urls: pipeList(r.photo_urls),
    short_summary: r.short_summary,
    maps_query: r.maps_query,
  };
}

async function main() {
  if (!process.env.FIREBASE_PROJECT_ID && !process.env.FIRESTORE_EMULATOR_HOST) {
    throw new Error(
      "Set FIREBASE_PROJECT_ID (and GOOGLE_APPLICATION_CREDENTIALS) for prod, " +
        "or FIRESTORE_EMULATOR_HOST=localhost:8080 for the emulator."
    );
  }

  admin.initializeApp({
    projectId: process.env.FIREBASE_PROJECT_ID || "weekendout-dev",
  });
  const db = admin.firestore();

  const csv = readFileSync(CSV_PATH, "utf8");
  const rows: Row[] = parse(csv, { columns: true, skip_empty_lines: true });

  console.log(`Seeding ${rows.length} places…`);

  let written = 0,
    skipped = 0;
  for (const row of rows) {
    if (!row.place_id) continue;
    const ref = db.collection("places").doc(row.place_id);
    const existing = await ref.get();

    const fresh = toPlace(row);

    if (existing.exists && existing.get("tags_overridden") === true) {
      // Only refresh neutral fields, preserve manual tag overrides.
      const { vibe, crowd, good_for, temp_class, ...neutral } = fresh;
      await ref.set(neutral, { merge: true });
      skipped++;
      console.log(`  ~ ${row.place_id} (tags preserved)`);
    } else {
      await ref.set(fresh, { merge: true });
      written++;
      console.log(`  ✓ ${row.place_id}`);
    }
  }

  console.log(`\nDone. Wrote: ${written}, tag-preserved: ${skipped}.`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
