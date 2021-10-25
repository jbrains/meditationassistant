# Inbox

- Continue to move `foo()` into the Happy Zone, then set our sights on `updateSessionDialog()`.
  - Continue replacing `List<int>` with `LocalDate` as the single value for a started/completed date.
- Make it easier to remember to adjust the month when converting between `Calendar` and `LocalDate`.
  - `Calendar` thinks January == 0
  - `LocalDate` thinks January == 1
- Did we introduce a bug related to session started date inside the DateSetListener?

## Refactoring Targets

As of 2021-10-11 we are focusing our energy on splitting the class `MeditationAssistant` apart while also looking for
an opportunity to stop it from subclassing `android.app.Application`.

### Targets within class `MeditationAssistant`

- Crack open `updateSessionDialog()` and extract code that can move into the Happy Zone.
- Remove duplication in writing to `sessionDialog(Started|Completed)(Year|Month|Day)` as a precursor to extracting smaller View-related classes.
  - It seems like there's some kind of modal dialog behavior to isolate from `MeditationAssistant` into its own Views(?).
- Move `chooseSessionDateAsCompletedSessionOrStartedSessionWhicheverIsMoreRecent()` into the Happy Zone.
- Remove the coupling centered on the field `sessionDialogCurrentOption`.

### Larger-Scale Untangling

- Try to find a place where we can decouple the rest of the system from `MeditationAssistant` using interfaces.
