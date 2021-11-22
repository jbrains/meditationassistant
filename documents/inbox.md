# Inbox

- Find silly usages of `sessionDialogStartedDate` and improve them.
- Similar to what we did with `sessionDialogStartedDate`, replace the component date fields for `sessionDialogCompleted(Year|Month|Day)` with a `LocalDate` field.
- Similar to what we did with the session dialog dates, replace the component time fields with a `LocalTime` field.

## Refactoring Targets

As of 2021-10-11 we are focusing our energy on splitting the class `MeditationAssistant` apart while also looking for
an opportunity to stop it from subclassing `android.app.Application`.

### Targets within class `MeditationAssistant`

- Remove the coupling centered on the field `sessionDialogCurrentOption`.
  - There appears to be an emerging View component that is "two related DatePicker widgets".
- Crack open `updateSessionDialog()` and extract code that can move into the Happy Zone.
- Replace `sessionDialog(Started|Completed)(Year|Month|Day)` fields with LocalDate values as a precursor to extracting smaller View-related classes.
  - It seems like there's some kind of modal dialog behavior to isolate from `MeditationAssistant` into its own Views(?).

### Larger-Scale Untangling

- Try to find a place where we can decouple the rest of the system from `MeditationAssistant` using interfaces.
