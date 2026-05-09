/**
 * Deterministic rules-based scoring. Narrows ~200 candidates to top-30 that we
 * hand to Gemini for re-ranking. Cheap, explainable, and works offline.
 */
import type { Place, UserPrefs, Crowd, TempPref } from "./types";

const CROWD_ORDER: Record<Crowd, number> = { low: 0, medium: 1, high: 2 };
const TEMP_ORDER: Record<TempPref, number> = { cool: 0, temperate: 1, warm: 2 };

const DURATION_COMPATIBLE: Record<string, string[]> = {
  "few-hours": ["few-hours"],
  "half-day": ["few-hours", "half-day"],
  "full-day": ["few-hours", "half-day", "full-day"],
  "weekend": ["full-day", "weekend"],
};

function jaccard(a: string[], b: string[]): number {
  if (!a.length && !b.length) return 0;
  const A = new Set(a);
  const B = new Set(b);
  let inter = 0;
  for (const x of A) if (B.has(x)) inter++;
  return inter / (A.size + B.size - inter);
}

export function scoreAndPick(prefs: UserPrefs, places: Place[], topN = 30) {
  const allowedDurations = DURATION_COMPATIBLE[prefs.durationClass] ?? [];

  const scored = places
    .filter((p) => allowedDurations.includes(p.duration_class))
    .filter((p) => !prefs.stayRequired || p.has_stay)
    .map((p) => {
      const vibeFit = jaccard(prefs.vibe, p.vibe);
      const groupFit = p.good_for.includes(prefs.groupType) ? 1 : 0;
      const crowdFit =
        1 - Math.abs(CROWD_ORDER[prefs.crowd] - CROWD_ORDER[p.crowd]) / 2;
      const tempFit =
        1 - Math.abs(TEMP_ORDER[prefs.tempPref] - TEMP_ORDER[p.temp_class]) / 2;
      const budgetFit = 1 - Math.min(Math.abs(prefs.budget - p.budget) / 3, 1);

      const score =
        0.30 * vibeFit +
        0.25 * groupFit +
        0.15 * crowdFit +
        0.15 * tempFit +
        0.15 * budgetFit;

      return { place: p, score };
    });

  scored.sort((a, b) => b.score - a.score);
  return scored.slice(0, topN);
}
