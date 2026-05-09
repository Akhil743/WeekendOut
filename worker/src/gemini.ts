/**
 * Gemini 2.5 Flash via the public AI Studio endpoint.
 *
 * Free-tier model, schema-constrained JSON output. The schema pins place_id to
 * the candidate set so the model can't hallucinate IDs.
 */
import type { Place, RankedPlace, UserPrefs } from "./types";

const ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

export interface Recommender {
  rank(prefs: UserPrefs, candidates: Place[]): Promise<RankedPlace[]>;
}

export class GeminiRecommender implements Recommender {
  constructor(private apiKey: string, private topK = 10) {}

  async rank(prefs: UserPrefs, candidates: Place[]): Promise<RankedPlace[]> {
    if (candidates.length === 0) return [];
    const k = Math.min(this.topK, candidates.length);

    // Slim the candidates payload — keep only fields that affect ranking.
    const slim = candidates.map((c) => ({
      id: c.id,
      name: c.name,
      vibe: c.vibe,
      crowd: c.crowd,
      good_for: c.good_for,
      temp: c.temp_class,
      has_stay: c.has_stay,
      duration: c.duration_class,
      budget: c.budget,
      drive_min: c.drive_time_from_blr_min,
      summary: c.short_summary,
    }));

    const prompt = [
      "You are a Bengaluru weekend planner. Given a user's preferences and a",
      `list of candidate places, pick the top ${k} that best fit. For each pick,`,
      "write ONE short, specific sentence (≤22 words) explaining why this place",
      "fits THIS user — reference their actual prefs (vibe/group/crowd/etc.).",
      "Avoid generic phrases like 'a great place to visit'.",
      "",
      `User prefs: ${JSON.stringify(prefs)}`,
      `Candidates: ${JSON.stringify(slim)}`,
    ].join("\n");

    const body = {
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: {
        temperature: 0.4,
        responseMimeType: "application/json",
        // No enum on place_id — Gemini's schema engine rejects 30-string enums
        // × array maxItems=10 ("too many states for serving"). We filter
        // invalid IDs post-parse below.
        responseSchema: {
          type: "OBJECT",
          properties: {
            ranked: {
              type: "ARRAY",
              items: {
                type: "OBJECT",
                properties: {
                  place_id: { type: "STRING" },
                  why: { type: "STRING" },
                },
                required: ["place_id", "why"],
              },
            },
          },
          required: ["ranked"],
        },
      },
    };

    const res = await fetch(`${ENDPOINT}?key=${this.apiKey}`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      throw new Error(`Gemini call failed: ${res.status} ${await res.text()}`);
    }

    const data = (await res.json()) as {
      candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
    };
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text ?? "";
    let parsed: { ranked: RankedPlace[] };
    try {
      parsed = JSON.parse(text);
    } catch {
      throw new Error(`Gemini returned non-JSON: ${text.slice(0, 200)}`);
    }

    // Defensive: drop any place_id not in the candidate set.
    const validIds = new Set(candidates.map((c) => c.id));
    return (parsed.ranked ?? [])
      .filter((r) => validIds.has(r.place_id))
      .slice(0, k);
  }
}
