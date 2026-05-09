/**
 * POST /recommend  — main endpoint called from the Android app.
 * GET  /healthz    — for uptime checks.
 */
import { fetchCandidatePlaces } from "./firestore";
import { GeminiRecommender } from "./gemini";
import { scoreAndPick } from "./score";
import type { Env, RecommendResponse, UserPrefs } from "./types";

const CACHE_TTL_SECONDS = 6 * 60 * 60; // 6h — collapses repeat queries

function cacheKey(prefs: UserPrefs): string {
  // Round to the day-bucket so identical-prefs-on-same-day hits the cache.
  const day = new Date().toISOString().slice(0, 10);
  return `rec:${day}:${JSON.stringify(prefs)}`;
}

function corsHeaders(): HeadersInit {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "POST, GET, OPTIONS",
    "access-control-allow-headers": "content-type, x-firebase-appcheck",
  };
}

async function handleRecommend(req: Request, env: Env): Promise<Response> {
  let prefs: UserPrefs;
  try {
    prefs = (await req.json()) as UserPrefs;
  } catch {
    return new Response("invalid json", { status: 400, headers: corsHeaders() });
  }

  // TODO Phase 4: verify x-firebase-appcheck JWT against APP_CHECK_PROJECT_NUMBER
  // before reaching Gemini. Stub-pass for now in dev.

  // Cache hit?
  const key = cacheKey(prefs);
  if (env.REC_CACHE) {
    const hit = await env.REC_CACHE.get(key);
    if (hit) {
      return new Response(hit, {
        headers: { "content-type": "application/json", ...corsHeaders() },
      });
    }
  }

  const cities = prefs.city_allowlist?.length
    ? prefs.city_allowlist
    : [prefs.city];

  const places = await fetchCandidatePlaces(env, cities, prefs.monthIso, prefs.stayRequired);

  const top30 = scoreAndPick(prefs, places, 30);

  let ranked;
  if (top30.length === 0) {
    ranked = [];
  } else {
    const recommender = new GeminiRecommender(env.GEMINI_API_KEY, 10);
    ranked = await recommender.rank(prefs, top30.map((s) => s.place));
  }

  const out: RecommendResponse = {
    ranked,
    candidate_count: top30.length,
    cached: false,
  };
  const body = JSON.stringify(out);

  if (env.REC_CACHE && ranked.length > 0) {
    await env.REC_CACHE.put(key, JSON.stringify({ ...out, cached: true }), {
      expirationTtl: CACHE_TTL_SECONDS,
    });
  }

  return new Response(body, {
    headers: { "content-type": "application/json", ...corsHeaders() },
  });
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    if (req.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(req.url);
    if (url.pathname === "/healthz") {
      return new Response("ok", { headers: corsHeaders() });
    }
    if (url.pathname === "/recommend" && req.method === "POST") {
      try {
        return await handleRecommend(req, env);
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e);
        return new Response(JSON.stringify({ error: msg }), {
          status: 500,
          headers: { "content-type": "application/json", ...corsHeaders() },
        });
      }
    }
    return new Response("not found", { status: 404, headers: corsHeaders() });
  },
} satisfies ExportedHandler<Env>;
