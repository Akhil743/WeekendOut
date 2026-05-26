# Continue — handoff notes

**Last touched:** 2026-05-26 (local laptop) by Akhil + Claude
**Next worker:** cloud Claude Code session

## Where we are

- v1.0 closed test passed; **production review submitted 2026-05-26 at 12:01**. Google says 7 days or less for the review.
- Polish pass done on `main` (commit `2c93e55`): friendly network errors, Detail load-error retry state, AsyncImage tinted fallbacks on Results / Saved / Detail, "Open in Maps" contentDescription fix, "Signing in…" moved to strings.xml.
- Play Store assets committed (commit `420af07`); Firebase Analytics dep removed.

## What to pick up next (in order)

See [docs/features/README.md](features/README.md) for the full queue. Recommended order:

1. **[features/01-accounts-onboarding.md](features/01-accounts-onboarding.md)** — required sign-in gate + Google + Email/Password + 3-slide welcome carousel + anonymous-to-credential auto-link migration. **This is the big one. Start here.**
2. [features/02-date-picker.md](features/02-date-picker.md) — small, independent.
3. [features/03-share-plan.md](features/03-share-plan.md) — small, independent.
4. [features/04-multi-city.md](features/04-multi-city.md) — content-heavy; depends on feature 01.
5. [features/05-history.md](features/05-history.md) — depends on feature 01.

## Constraints to respect

- **Firebase Spark plan only**. No Blaze-tier services (no Cloud Functions, no >10/day phone SMS, no GCS).
- **App still anonymous-first today.** Feature 01 is what flips the model. Don't half-implement it.
- **Existing testers have anonymous accounts with saved places.** Feature 01's brief covers the migration explicitly — read it before touching auth.
- **Worker has the Gemini key.** Never put a Gemini key in the Android app.
- **Tests are sparse.** For non-trivial logic in viewmodels / repos, add JVM unit tests rather than instrumented ones.

## How to verify a build before pushing

```bash
./gradlew :app:assembleDebug
```

If you cannot run this in the cloud environment, **say so in the PR**. Don't push a branch you haven't compiled.

## Production review note

If the production review **completes during cloud work** (likely within 7 days), the version reviewed is the one at commit `2c93e55`. Once approved, that commit becomes the live `1.0` on Play Store. Anything you ship after needs to be a new version code (`versionCode++` in [app/build.gradle.kts](../app/build.gradle.kts)) and a new staged rollout. Don't bump `versionCode` casually — only when you're ready to ship.

If production review is **rejected**, the rejection email will say why. Fix that first before continuing with new features.
