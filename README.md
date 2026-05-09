# WeekendOut

A zero-spend Bengaluru weekend planner. Tell it the duration, group type, vibe,
crowd preference, climate and stay needs — get back ranked picks (cafes, pubs,
day trips, getaways) with a one-line *"why this fits you"* per place.

> **Stack:** Kotlin + Jetpack Compose · Firebase (Auth + Firestore on Spark, $0)
> · Cloudflare Worker proxy · Gemini 2.5 Flash via AI Studio (free tier) ·
> hand-curated places · `geo:` intent for directions.
>
> See [.claude/plans/hello-claude-i-am-structured-conway.md](../.claude/plans/hello-claude-i-am-structured-conway.md)
> (relative to your home dir) for the full design rationale.

---

## Repo layout

```
weekendout/
├── app/            # Android app (Kotlin + Compose)
├── worker/         # Cloudflare Worker (TS) — holds Gemini key, calls Firestore + Gemini
├── seed/           # CSV → Firestore importer (Node + ts-node)
├── firestore.rules
├── firestore.indexes.json
├── firebase.json
└── docs/
```

## Prereqs

| Tool | Why |
|---|---|
| Android Studio Ladybug+ (Kotlin 2.0) | Build & run the app |
| Node 20+ | Worker + seed script |
| `firebase-tools` (`npm i -g firebase-tools`) | Deploy rules, run emulator |
| `wrangler` (`npm i -g wrangler` or `npx wrangler`) | Cloudflare Worker dev/deploy |
| A Google account | Free Firebase + Gemini AI Studio + Cloudflare |

No credit card required for any step below.

---

## Phase 0 — One-time accounts (15 min)

### 1. Firebase project (Spark plan)
1. Go to <https://console.firebase.google.com> → **Add project** → name it (e.g. `weekendout-dev`).
2. Skip Google Analytics (or accept defaults — free).
3. **Authentication** → enable **Anonymous** and **Google** providers.
4. **Firestore** → create database in **production** mode, region `asia-south1` (Mumbai) for low Bengaluru latency.
5. **Project settings → General → Your apps**: register an Android app with
   package name `com.akhil.weekendout`. Download `google-services.json` and put
   it at `app/google-services.json` (this file is git-ignored).
6. **Project settings → App Check**: enable Play Integrity for the Android app.
   Add a debug token from Android Studio > Logcat after first run.

### 2. Gemini AI Studio (free tier)
1. <https://aistudio.google.com/app/apikey> → **Create API key** in the same
   Google project (or a fresh one). Free tier on `gemini-2.5-flash` is plenty
   for solo dev.
2. Copy the key — you'll paste it into `wrangler secret put GEMINI_API_KEY`.

### 3. Cloudflare account (free)
1. <https://dash.cloudflare.com> → sign up. Workers free plan = 100k req/day,
   no card required.
2. `wrangler login` from the `worker/` directory.

---

## Phase 1 — Seed places into Firestore

```bash
cd seed
npm install
```

### Option A: against the local Firestore emulator (zero network, zero cost)
```bash
# In one terminal:
firebase emulators:start --only firestore,auth

# In another:
cd seed
npm run seed:emulator
```
Open <http://localhost:4000> to inspect.

### Option B: against your real Firebase project
```bash
# Create a service-account JSON from Project Settings > Service accounts.
export GOOGLE_APPLICATION_CREDENTIALS=/abs/path/to/sa.json
export FIREBASE_PROJECT_ID=weekendout-dev
cd seed
npm run seed
```
Re-running upserts every row keyed on `place_id`. Rows you've manually marked
`tags_overridden=true` in the console keep their tag fields untouched — only
neutral fields refresh.

Then deploy the rules + indexes:
```bash
firebase use weekendout-dev
firebase deploy --only firestore:rules,firestore:indexes
```

---

## Phase 2 — Cloudflare Worker

```bash
cd worker
npm install

# Free public Firebase Web API key (Project settings > General > Web apps)
wrangler secret put FIREBASE_API_KEY
# Gemini key from AI Studio (step 2 above)
wrangler secret put GEMINI_API_KEY

# Optional: KV namespace for caching (1k writes/day free) — uncomment binding
# in wrangler.toml first
wrangler kv:namespace create REC_CACHE

# Run locally
npm run dev      # serves on http://localhost:8787

# Smoke-test
curl http://localhost:8787/healthz
```

Test the recommend endpoint against a seeded emulator:
```bash
curl -X POST http://localhost:8787/recommend \
  -H 'content-type: application/json' \
  -d '{
    "city": "Bengaluru",
    "durationClass": "full-day",
    "groupType": "couples",
    "vibe": ["scenic","quiet"],
    "crowd": "low",
    "tempPref": "cool",
    "stayRequired": false,
    "monthIso": "Nov",
    "budget": 2,
    "city_allowlist": ["Bengaluru","Nandi Hills","Madikeri","Chikmagalur"]
  }'
```

When you're ready to ship:
```bash
wrangler deploy
```
Note the public URL (e.g. `https://weekendout-recommend.<sub>.workers.dev`) and
paste it into `app/build.gradle.kts` → `WORKER_BASE_URL` in the **release**
build type.

---

## Phase 3 — Android app

1. Open the project root in Android Studio (it'll auto-import via Gradle).
2. Drop `google-services.json` into `app/`.
3. Build & run on a real device or emulator (Android 8.0+).
4. The **debug** build points at `http://10.0.2.2:8787` so the emulator can
   reach `wrangler dev` running on your laptop.
5. The **release** build points at the deployed Worker URL.

### First-time grant for App Check (debug)
On first launch in debug mode you'll see a Logcat line like:
```
DebugAppCheckProviderFactory ... Enter this debug secret into the allow list...
```
Copy the UUID, paste it into Firebase Console → App Check → your app → Manage
debug tokens.

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Firestore query failed: 403` from the Worker | `FIREBASE_API_KEY` not set, or the Web API key is restricted to a different referrer/origin |
| Gemini returns empty `ranked` | `place_id` enum was empty — make sure you seeded places and the rules query returns ≥1 candidate |
| Compose previews fail to build | Make sure Kotlin ≥ 2.0 and the Compose Compiler plugin (`kotlin-compose`) is applied — already configured in `app/build.gradle.kts` |
| Detail screen "Open in Maps" does nothing | The `<queries>` block in the manifest is required on Android 11+; included already |

## Free-tier headroom

At ~100 DAU × 3 reqs/day:
- Firestore: 9k reads/day vs 50k limit (5.5×)
- Worker: 300/day vs 100k (333×)
- Gemini: 300/day vs ~250 free — caching same-day prefs in Cloudflare KV
  collapses ~80% of repeats, putting you well under

## License

Private — all rights reserved (until you decide otherwise).
