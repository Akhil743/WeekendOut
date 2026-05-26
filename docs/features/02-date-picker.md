# 02 — Real date picker on Planner

**Status:** queued for v1.1 · **Owner:** cloud session · **Depends on:** nothing

## Goal

The Planner currently labels its first section "When" but only collects
`DurationClass` (half-day / full-day / weekend). Add a real date / weekend
picker so the user can plan for a specific upcoming date, and pass the
selected date through to the Worker.

## Scope

### In scope
- Replace the **"When"** section's content with two controls stacked:
  1. A **date pill** that opens Material 3 `DatePickerDialog`. Default selection: next Saturday. Disabled past dates.
  2. The existing `DurationRow` segmented control, repositioned below the date pill with a "Duration" sub-label.
- Wire the picked date into [PlannerViewModel.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerViewModel.kt) — replace the `dateMillis = System.currentTimeMillis()` default with the user's selection.
- Derive `monthIso` (e.g. "Nov") from the selected date and send it in the Worker payload (already wired in [RecommendApi.kt](../../app/src/main/java/com/akhil/weekendout/data/remote/RecommendApi.kt) — confirm and update if not).
- Show a human-readable label on the date pill: "Sat, 30 Nov" or "Today" / "Tomorrow" / "This Saturday" for nearby dates.

### Out of scope
- Range picker (single date only for v1.1).
- Weekend-aware shortcuts ("next weekend", "long weekend"). Note them in the brief if you want them later.
- Calendar integration (block out user's busy dates).

## Acceptance criteria
- [ ] Tapping the date pill opens a Material 3 date picker.
- [ ] Picking a date updates the pill label.
- [ ] Submitting the planner sends the picked date to the Worker; verify in Logcat or a print.
- [ ] Past dates cannot be selected.
- [ ] No regression on the duration segmented control.
- [ ] `./gradlew :app:assembleDebug` succeeds.

## Files to touch
- [PlannerScreen.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerScreen.kt) — replace the "When" section content.
- [PlannerViewModel.kt](../../app/src/main/java/com/akhil/weekendout/ui/planner/PlannerViewModel.kt) — add `selectedDate: LocalDate` state.
- `app/src/main/java/com/akhil/weekendout/data/model/UserPrefs.kt` — confirm `dateMillis` field is wired through.
- [strings.xml](../../app/src/main/res/values/strings.xml) — pill label, "Duration" sub-label.

## Open questions
- Default date: next Saturday or today? **Default: next Saturday.**
- Should picking a Monday-Friday date filter to "Half day / Full day" only and hide "Weekend" duration? **Default: no, leave duration independent.**
