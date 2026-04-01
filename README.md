# SOEN345

## Testing & Continuous Integration

This project uses an automated testing and CI pipeline to ensure code quality and reliability.

### Unit Testing

Unit tests use **JUnit**, **Robolectric**, and **Mockito**. They run on the **JVM** (no emulator), which is fast for activity and logic tests.

**Run all debug unit tests** (from the project root):

```bash
./gradlew :app:testDebugUnitTest
```

Shorter equivalent used in CI:

```bash
./gradlew testDebugUnitTest
```

**See results:** after a run, open the HTML report in a browser:

`app/build/reports/tests/testDebugUnitTest/index.html`

Run a single test class:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.bookingapp.MainActivityTest"
```

### Instrumentation / System Testing

Instrumentation tests run on a **device or emulator** (not the JVM). This project includes **Espresso smoke tests** for login and registration (`LoginRegistrationSmokeTest`): main fields are visible, empty-submit validation shows errors, and register links open the expected screens. They do **not** sign in or create real accounts.

**Run instrumentation tests** (emulator running or device connected with USB debugging):

```bash
./gradlew connectedDebugAndroidTest
```

**Test report:** after a run, Gradle prints where reports were written; they are usually under `app/build/reports/` or `app/build/outputs/androidTest-results/` (exact layout depends on the Android Gradle Plugin version). Android Studio also shows results in the **Run** tool window.

The **Android Instrumentation** GitHub Actions workflow runs these tests on an emulator.

### Continuous Integration

All tests are automatically executed through **GitHub Actions** on every push to `main` and on all pull requests. The CI pipeline performs the following steps:

1. Build the Android project
2. Execute JUnit unit tests
3. Run instrumentation tests on an Android emulator
4. Generate test coverage reports

### Code Coverage (JaCoCo)

Coverage for **unit tests** is collected with **JaCoCo** when you run `testDebugUnitTest` (debug build has coverage enabled).

**Generate the HTML and XML reports** (runs tests if needed, then writes reports):

```bash
./gradlew jacocoTestReport
```

**View the report:** open this file in your browser (Finder: right‑click → Open With → browser):

`app/build/reports/jacoco/jacocoTestReport/html/index.html`

Browse by package, then open a class to see green/red line coverage. The machine-readable report for CI tools is:

`app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`

**One-shot:** tests + coverage report:

```bash
./gradlew testDebugUnitTest jacocoTestReport
```

On **CI**, results are also uploaded to **Codecov** (requires a repository secret). Locally, the HTML report above is the usual way to inspect coverage.

---

This automated workflow helps maintain code quality by ensuring that new changes are tested and validated before being merged.
