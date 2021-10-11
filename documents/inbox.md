# Inbox

## Refactoring Targets

As of 2021-10-11 we are focusing our energy on splitting the class `MeditationAssistant` apart while also looking for
an opportunity to stop it from subclassing `android.app.Application`.

### Targets within class `MeditationAssistant`

- Remove duplication in writing to `sessionDialog(Started|Completed)(Year|Month|Day)` as a precursor to extracting smaller View-related classes.
- Move `chooseSessionDateAsCompletedSessionOrStartedSessionWhicheverIsMoreRecent()` into the Happy Zone.
- Use `LocalDate` or another value similar to "Year-Month-Day" as a package for the 3-tuple (year, month, day).
  - To maintain backwards compatibility with Android SDK 16, we can't use `java.time`, so we would need to use <https://github.com/JakeWharton/ThreeTenABP>
- Remove the coupling centered on the field `sessionDialogCurrentOption`.
- Crack open `updateSessionDialog()` and extract code that can move into the Happy Zone.
