# WeekendOut — v1.1 feature briefs

These briefs scope the work queued up for v1.1, after the v1.0 production
submission on 2026-05-26. Each brief is self-contained: a cloud Claude Code
session can pick one up cold and ship it.

## Sequencing (recommended)

1. **[01-accounts-onboarding.md](01-accounts-onboarding.md)** — required first.
   Gates the app behind sign-in and adds the 3-slide welcome carousel. Touches
   nav, auth, every existing screen's entry point. Land this before anything
   else so subsequent features build on a real account.
2. **[02-date-picker.md](02-date-picker.md)** — small, independent. Replaces
   the misleadingly-labeled "When" section with a real date/weekend picker.
3. **[03-share-plan.md](03-share-plan.md)** — small, independent. Android
   share-sheet from Detail + Results.
4. **[04-multi-city.md](04-multi-city.md)** — content-heavy. Needs seed data
   for 1-2 more cities + a city picker on Planner. Worker already supports it.
5. **[05-history.md](05-history.md)** — biggest scope, lowest urgency.
   Stores each Plan/Results session under the user account.

## Working notes for cloud sessions

- All Firebase services are on **Spark plan ($0)**. Do not introduce features
  that require Blaze (phone OTP, Cloud Functions). If you think you need one,
  flag it and stop.
- The Cloudflare Worker holds the Gemini key — do **not** put it in the app.
- Tests in this repo are minimal. For non-trivial logic, add a JVM unit test
  rather than relying on instrumented tests (the cloud env has no emulator).
- Before pushing a branch, run `./gradlew :app:assembleDebug` to confirm the
  app still builds. Don't push a red branch.
- Open a PR per feature against `main`. Title format: `feat: <short>` or
  `fix: <short>`.

## What's already done in v1.0 (do not re-do)

- Closed test passed; production review submitted 2026-05-26
- Anonymous Firebase Auth on first launch; optional Google upgrade via
  Credential Manager when the user taps Save
- Planner → Results → Detail → Saved screens
- Cloudflare Worker proxying Gemini 2.5 Flash for ranking
- Privacy + account-deletion HTML pages under `docs/`
- Play Store assets under `playstore-assets/`
