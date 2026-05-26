# 05 — History of past plans

**Status:** queued for v1.1 · **Owner:** cloud session · **Depends on:** [01-accounts-onboarding.md](01-accounts-onboarding.md)

## Goal

Persist every plan a user submits so they can revisit past Results pages.
This gives the app a sense of memory and unlocks future features like
"re-plan with last weekend's filters".

## Scope

### In scope
- On successful `recommend()` in [RecommendRepository.kt](../../app/src/main/java/com/akhil/weekendout/data/repo/RecommendRepository.kt), write a new doc under `users/{uid}/history/{auto-id}` with:
  - `prefs`: the full `UserPrefs` object
  - `placeIds`: the ordered list of returned place ids
  - `whys`: parallel array of LLM "why" strings
  - `createdAt`: server timestamp
- New **History** screen accessible from the Profile screen (from [01](01-accounts-onboarding.md)) or as a third top-bar action on Planner.
- History screen shows a chronological list of past plans, most recent first:
  - Top line: "Plan for {durationClass} · {city} · {dateMillis formatted}"
  - Sub-line: top 2 vibe tags joined with "·"
  - Tappable → re-renders that plan's Results page (read-only — no re-call to Worker, just show stored places + whys)
- Cap history at the **last 50 plans per user**. Delete older on write (client-side delete is fine; no Cloud Functions).

### Out of scope
- "Replay this plan" (re-submit with same filters and see fresh results).
- Sharing a historical plan.
- Local-only history for unsigned users — since auth is now required, history is always per-account.

## Acceptance criteria
- [ ] Submitting a plan writes a history doc.
- [ ] History screen lists past plans in reverse-chronological order.
- [ ] Tapping a history entry shows the Results page with the *stored* places and whys.
- [ ] No more than 50 entries per user persist.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Files to touch
**New:**
- `app/src/main/java/com/akhil/weekendout/data/repo/HistoryRepository.kt`
- `app/src/main/java/com/akhil/weekendout/data/model/PlanHistoryEntry.kt`
- `app/src/main/java/com/akhil/weekendout/ui/history/HistoryScreen.kt`
- `app/src/main/java/com/akhil/weekendout/ui/history/HistoryViewModel.kt`

**Modified:**
- [RecommendRepository.kt](../../app/src/main/java/com/akhil/weekendout/data/repo/RecommendRepository.kt) — write history on success.
- [firestore.rules](../../firestore.rules) — allow user to read/write their own `users/{uid}/history/*` docs only.
- [firestore.indexes.json](../../firestore.indexes.json) — add a composite index on `(uid, createdAt desc)`.
- [Nav.kt](../../app/src/main/java/com/akhil/weekendout/ui/Nav.kt) — add the history route.
- [strings.xml](../../app/src/main/res/values/strings.xml) — history screen copy.

## Open questions
- Where do we enter the History screen — from Planner top bar or only via Profile? **Default: Profile only, to keep Planner top bar clean.**
- Should re-opening a historical plan re-rank with current Gemini output? **Default: no — store and replay verbatim.**
- Privacy: do we keep history of deleted accounts? **Default: no — wipe `users/{uid}/*` on account deletion. Already in scope of the existing delete flow.**
