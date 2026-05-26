# 03 — Share a plan

**Status:** queued for v1.1 · **Owner:** cloud session · **Depends on:** nothing

## Goal

Let a user share their plan (a single place from Detail, or the whole Results
list) via Android's system share sheet so they can send picks to friends on
WhatsApp / SMS / etc. This is the cheapest virality lever a weekend-planner
app has.

## Scope

### In scope
- **Detail screen**: add a share icon (`Icons.Outlined.Share`) to the TopAppBar `actions` next to the bookmark icon. Tapping fires `Intent.ACTION_SEND` with text like:
  > Going to *{place_name}* this weekend — found it on WeekendOut.
  > {short_summary}
  > Maps: https://www.google.com/maps/search/?api=1&query={url-encoded maps_query}
  >
  > Plan your weekend: https://play.google.com/store/apps/details?id=com.akhil.weekendout
- **Results screen**: add a single "Share these picks" button at the bottom of the list (below all cards). Fires `Intent.ACTION_SEND` with a numbered list:
  > My weekend picks (Bengaluru, full-day, scenic):
  >  1. {place_name_1} — {why_1}
  >  2. {place_name_2} — {why_2}
  >  …
  >
  > Plan yours: https://play.google.com/store/apps/details?id=com.akhil.weekendout

### Out of scope
- Generating a shareable image card (would need Compose Canvas + writing to MediaStore; nice but not v1.1).
- Deep links — tapping the shared link does NOT open the specific place in someone else's app for v1.1; just opens Play Store. Add a `weekendout://place/{id}` deep link later.
- Tracking who shared / who installed via attribution.

## Acceptance criteria
- [ ] Detail screen: tapping share opens the system share sheet with the place text pre-filled.
- [ ] Results screen: tapping the bottom share button opens the share sheet with the ranked list pre-filled.
- [ ] Special characters in place names / summaries don't break the Maps URL (use `Uri.encode`).
- [ ] Play Store link is the production package id `com.akhil.weekendout`.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Files to touch
- [DetailScreen.kt](../../app/src/main/java/com/akhil/weekendout/ui/detail/DetailScreen.kt) — add share icon + intent.
- [ResultsScreen.kt](../../app/src/main/java/com/akhil/weekendout/ui/results/ResultsScreen.kt) — add share button at list end.
- [strings.xml](../../app/src/main/res/values/strings.xml) — share copy templates with `%1$s` placeholders.

## Open questions
- Should the share text include the rendered emoji (📍, ✨) or stay plain-text? **Default: plain-text for v1.1 — emoji can be flaky in some Indian carriers' SMS.**
