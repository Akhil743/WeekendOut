# 01 — Accounts, sign-up, and onboarding

**Status:** queued for v1.1 · **Owner:** cloud session · **Depends on:** nothing

## Goal

Replace the silent-anonymous-auth flow with a proper account gate. New users
see a 3-slide welcome carousel followed by a sign-in/sign-up screen. They
cannot reach the Planner without an authenticated, non-anonymous user. Existing
anonymous testers keep their saved data via Firebase auth-credential linking.

## Scope

### In scope
1. **Welcome carousel** (3 slides, swipeable, skippable):
   - Slide 1: "Your weekend, planned in 10 seconds" — value prop
   - Slide 2: "Hand-picked Bengaluru spots, ranked by AI for your vibe" — how it works
   - Slide 3: "Save what you love. Sync across devices." — final CTA → sign-in
   - Use `HorizontalPager` from Compose Foundation. Show page indicators. "Skip" in top-right; "Next" / "Get started" CTA at the bottom.

2. **Sign-in / sign-up screen**:
   - Two methods: **Google** (Credential Manager — already wired in [GoogleSignInHelper.kt](../../app/src/main/java/com/akhil/weekendout/data/auth/GoogleSignInHelper.kt)) and **Email + Password** (new — Firebase Auth supports natively).
   - Email/password screen: one tabbed surface or two buttons that swap to forms. Validate: email regex, password ≥ 8 chars. Show inline errors.
   - "Forgot password?" link → `sendPasswordResetEmail()` → confirmation toast.
   - No email verification gate on sign-in (Firebase sends a verification email automatically; we don't block on it for v1.1).

3. **Anonymous account migration**:
   - On app launch, if the current Firebase user is `isAnonymous == true` AND has any saved places (`users/{uid}/saved` has docs), route them through a one-time **"Save your account"** screen instead of the welcome carousel.
   - This screen explains: "Sign in to keep your X saved places."
   - On sign-in success, use `currentUser.linkWithCredential(...)` for both Google and Email/Password to preserve the UID and saved data.
   - On link failure (`FirebaseAuthUserCollisionException` — the credential already exists for a different account), surface a clear "This Google account is already used. Sign in instead, and your current saves will not transfer." — give them the option to proceed with sign-in (losing local anonymous saves).

4. **Navigation guard**:
   - Add a `Gate` composable / route that decides: → onboarding (first-launch) → migration-prompt (anonymous-with-saves) → sign-in (signed-out) → Planner (signed-in non-anonymous).
   - Use `DataStore` (already in repo? check) or `SharedPreferences` to remember "has seen onboarding" so it doesn't re-show on sign-out.

5. **Sign-out + minimal Profile screen**:
   - Add a profile / account icon to the Planner top bar (replacing or alongside the existing Saved bookmark).
   - Profile screen: shows email, sign-in method, "Sign out" button, "Delete account" link to web ([delete-account.html](../delete-account.html)).
   - Sign-out → `FirebaseAuth.signOut()` → route back to sign-in screen (NOT back to anonymous).

### Out of scope (defer)
- **Phone OTP**: requires Blaze plan above free SMS quota. Not in v1.1.
- **Email verification gate**: we send the email but don't gate access.
- **Profile photo / username**: Profile screen is functional-only for v1.1.
- **Location permission ask**: defer until [04-multi-city.md](04-multi-city.md) ships.
- **Notification permission ask**: defer until notifications exist.
- **Social sign-in (Apple, Facebook)**: not requested.
- **Anonymous-fallback ("Continue without signing in")**: explicit product call — this is a *gated* model, not soft.

## Acceptance criteria

- [ ] First launch of a clean install shows the welcome carousel.
- [ ] Tapping "Skip" or completing the carousel routes to the sign-in screen.
- [ ] Sign-in screen has working Google + Email/Password paths.
- [ ] Sign-up via email creates a Firebase user with `isAnonymous == false`.
- [ ] After sign-in, the user lands on the Planner.
- [ ] Hitting back from Planner exits the app (not back to sign-in).
- [ ] An anonymous-tester install with existing saves sees the "Save your account" prompt before the carousel, and saves survive sign-in.
- [ ] The carousel does not re-appear on subsequent launches (persist a flag).
- [ ] Profile screen shows email + sign-out + delete-account link.
- [ ] Sign-out routes back to the sign-in screen, not the anonymous flow.
- [ ] App still builds (`./gradlew :app:assembleDebug`).

## Files to touch

**New:**
- `app/src/main/java/com/akhil/weekendout/ui/onboarding/WelcomeCarousel.kt`
- `app/src/main/java/com/akhil/weekendout/ui/auth/SignInScreen.kt`
- `app/src/main/java/com/akhil/weekendout/ui/auth/SignUpScreen.kt` (or combine with SignInScreen via tabs)
- `app/src/main/java/com/akhil/weekendout/ui/auth/AuthViewModel.kt`
- `app/src/main/java/com/akhil/weekendout/ui/auth/MigrationPromptScreen.kt`
- `app/src/main/java/com/akhil/weekendout/ui/profile/ProfileScreen.kt`
- `app/src/main/java/com/akhil/weekendout/ui/profile/ProfileViewModel.kt`
- `app/src/main/java/com/akhil/weekendout/data/prefs/OnboardingPrefs.kt` (DataStore-backed flag)

**Modified:**
- [Nav.kt](../../app/src/main/java/com/akhil/weekendout/ui/Nav.kt) — add routes: `onboarding`, `signIn`, `migration`, `profile`; add gate logic.
- [AuthRepository.kt](../../app/src/main/java/com/akhil/weekendout/data/repo/AuthRepository.kt) — add `signInWithEmail`, `signUpWithEmail`, `sendPasswordReset`, `linkEmail`.
- [PlannerScreen.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerScreen.kt) — add profile-icon action in the top bar.
- [strings.xml](../../app/src/main/res/values/strings.xml) — all onboarding + auth copy.
- [MainActivity.kt](../../app/src/main/java/com/akhil/weekendout/MainActivity.kt) — ensure auth state observation drives the gate.

## Edge cases to handle

- User signs up with an email that already exists → show "That email is already registered. Sign in instead?"
- Network failure mid-sign-in → keep them on the sign-in screen with a friendly error (use the same `friendlyError` pattern from [ResultsViewModel.kt](../../app/src/main/java/com/akhil/weekendout/ui/results/ResultsViewModel.kt)).
- User backs out of carousel mid-flow → close app (don't loop).
- Anonymous user with no saves → skip migration prompt, go straight to sign-in (no point preserving an empty UID).
- Sign-in succeeds but Firestore profile write fails → don't block sign-in; retry profile write next launch.

## Open questions (resolve before starting)

- Should the Profile screen be a route or a bottom sheet from Planner? **Default: route.**
- Should email/password require a confirm-password field on sign-up? **Default: yes.**
- Do we capture display name during email sign-up? **Default: no — keep v1.1 minimal; can add later.**
