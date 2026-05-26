# 04 — Multi-city support

**Status:** queued for v1.1 · **Owner:** cloud session · **Depends on:** [01-accounts-onboarding.md](01-accounts-onboarding.md) (uses Profile screen for default-city setting)

## Goal

Extend beyond Bengaluru. Add a city picker on the Planner so users in
other Indian cities can plan weekends locally. Worker already accepts a
`city` parameter — most of the lift is seed data and UI.

## Scope

### In scope
- **Cities to support in v1.1**: Bengaluru, Mumbai, Pune. Pick these because of similar weekend-trip geography (nearby hill stations / coast).
- **Seed data**: add `~30 places per new city` under the same `places` Firestore collection, following the existing schema in [seed/](../../seed/). Hand-curated, no scraping. Each new place needs `city`, `region`, `vibe`, `good_for`, `crowd`, `temp_class`, `budget`, `drive_time_from_blr_min` → rename this field generically to `drive_time_min_from_city_center` (migration concern: handle both old + new field in [Place.kt](../../app/src/main/java/com/akhil/weekendout/data/model/Place.kt) until existing rows are backfilled).
- **City picker UI**: a dropdown/selector at the top of the Planner above the "When" section. Show the current city; tap → bottom-sheet list of supported cities.
- **Default city**: stored per-user in Firestore (`users/{uid}/profile.defaultCity`). Show a one-time prompt on first multi-city launch: "Which city are you planning for?"
- **Profile screen** (from [01-accounts-onboarding.md](01-accounts-onboarding.md)): add a "Default city" setting.
- **Worker payload**: send the selected city. The Worker's `city_allowlist` should be derived from the selected city + reachable getaways (already exists in code — confirm it's not hardcoded to Bengaluru).
- **"Nearby getaways" toggle copy**: change "Include nearby getaways (Coorg, Ooty…)" to dynamically list 2-3 destinations relevant to the selected city.

### Out of scope
- Device GPS auto-detection (the location permission ask was deferred — keep deferred).
- User-suggested places.
- City-specific theming.

## Acceptance criteria
- [ ] Planner shows a city picker; default city populates from Profile.
- [ ] Firestore has ≥ 30 places each for Mumbai and Pune.
- [ ] Selecting Mumbai returns Mumbai+nearby places, not Bengaluru ones.
- [ ] The "nearby getaways" copy updates with the selected city.
- [ ] Default city persists across launches and devices for the same account.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Files to touch
**New:**
- `seed/data/mumbai.csv` and `seed/data/pune.csv` (or extend the existing seed source).
- `app/src/main/java/com/akhil/weekendout/ui/planner/CityPickerSheet.kt`

**Modified:**
- [PlannerScreen.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerScreen.kt) — city picker + dynamic getaway copy.
- [PlannerViewModel.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerViewModel.kt) — load default city from profile, hold selected city.
- [Place.kt](../../app/src/main/java/com/akhil/weekendout/data/model/Place.kt) — generalize the drive-time field.
- [RecommendApi.kt](../../app/src/main/java/com/akhil/weekendout/data/remote/RecommendApi.kt) — confirm `city` is passed.
- [seed/](../../seed/) scripts — handle multi-city CSVs.
- [Worker](../../worker/) — verify `city_allowlist` isn't hardcoded.
- `ProfileScreen.kt` (from feature 01) — add default-city setting.

## Open questions
- Mumbai/Pune nearby getaways: Lonavala, Mahabaleshwar, Alibaug, Matheran for both? **Default: yes — they're shared.**
- Should we let users see ALL cities' places ("Open in Mumbai while I'm in Bengaluru")? **Default: yes — show all, just default to user's city.**
- Generalize `drive_time_from_blr_min` field name — schema migration risk. **Default: support both fields in Place.kt for a release; backfill seed data in a follow-up.**
