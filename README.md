# SOEN345

## Admin Login

To log in as an admin, use the following credentials on the login screen:

- **Email:** `admin@test.com`
- **Password:** `123456`

Admin users can create, edit, and cancel events, view reservations per event, and cancel events with automatic email notifications sent to all affected customers.

---

## User Stories

### US-09 — Reserve a ticket for an event
Customers browse events on the main screen, tap **Book**, and confirm their booking. A Firestore transaction atomically decrements the available seat count and creates a reservation. A confirmation email is sent via EmailJS.

### US-10 — View my reservation history
After logging in, customers tap **My Bookings** (top-right of the event list) to see all their past and upcoming reservations with status badges (Confirmed / Cancelled / Pending).

### US-11 — Cancel a reservation
From the **My Bookings** screen, customers can tap **Cancel Reservation** on any active reservation. The cancellation is applied atomically in Firestore (seat is restored, event is re-opened if it was sold out), and a cancellation email is sent to the customer.

### US-14 — Receive notification when a booked event is cancelled
When an admin cancels an event via the **Cancel Event & Notify Customers** button in the admin form, the app queries all confirmed reservations for that event and sends a cancellation email to each affected customer via the Observer/Notification pattern.

### US-19 — Handle concurrent users without double-booking
All bookings run inside a Firestore **transaction** that atomically reads the seat count, validates availability, decrements it, and writes the reservation. This prevents race conditions where two users simultaneously book the last available seat. The UI also disables the confirm button immediately on tap to prevent duplicate submissions.

### US-20 — Remain highly available via cloud deployment
The app backend runs entirely on **Firebase** (Firestore + Authentication), which is a multi-region, auto-scaled, managed cloud platform with built-in redundancy and no single point of failure. Firebase guarantees 99.95 % uptime SLA for Firestore, ensuring the app remains available under load.

---

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
