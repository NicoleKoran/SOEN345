# BookingApp

A mobile event booking application built for Android. Users can browse events, make and manage reservations, and receive email notifications. Administrators can create, update, cancel, and delete events, as well as view all reservations per event.

The app is backed by **Firebase** (Firestore + Authentication) and sends transactional emails via **EmailJS**.

---

## Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 |
| Android SDK | API 29 minimum, API 34 target |
| Gradle | Wrapper included (no separate install needed) |

### Running the App

1. **Clone the repository**
   ```bash
   git clone https://github.com/NicoleKoran/SOEN345.git
   cd SOEN345
   ```

2. **Open in Android Studio**
   File → Open → select the `SOEN345` folder. Let Gradle sync finish.

3. **Connect a device or start an emulator**
   Use a physical Android device (USB debugging on) or create an AVD via the AVD Manager (API 29+).

4. **Run the app**
   Press the green **Run** button (▶) or use `Shift+F10`. Select your device and the app will install and launch.

### Admin Login

To access administrator features, use the following credentials on the login screen:

- **Email:** `admin@test.com`
- **Password:** `123456`

Admin users can create, edit, cancel, and delete events, view reservations per event, and send automatic email notifications to affected customers when an event is cancelled.

---

## Features

### Customer
- Browse upcoming events with category badges, seat counts, and status indicators
- Book a ticket — seat count is decremented atomically via a Firestore transaction, preventing double-booking
- View full reservation history with status badges (Confirmed / Cancelled / Pending)
- Cancel an active reservation — the seat is restored and a confirmation email is sent

### Administrator
- Create new events with title, description, location, date, category, and seat count
- Edit and update existing events
- Cancel an event and automatically notify all customers with confirmed reservations by email
- Restore (un-cancel) a previously cancelled event
- Delete an event after password re-authentication
- View all reservations for a specific event

---

## Testing

### Unit Tests (JVM)

Unit tests use **JUnit**, **Robolectric**, and **Mockito**. They run on the JVM — no emulator required.

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.example.bookingapp.EventFormValidatorTest"
```

HTML report after a run:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### Instrumented Tests (Device / Emulator)

End-to-end and UI tests use **Espresso** and `ActivityScenario`. A connected device or running emulator is required.

```bash
./gradlew connectedDebugAndroidTest
```

Reports are written to:
```
app/build/reports/androidTests/connected/
```

### Code Coverage (JaCoCo)

```bash
# Run tests and generate coverage report in one step
./gradlew testDebugUnitTest jacocoTestReport
```

HTML coverage report:
```
app/build/reports/jacoco/jacocoTestReport/html/index.html
```

---

## Continuous Integration

GitHub Actions runs automatically on every push and pull request:

1. Build the project
2. Run JUnit unit tests
3. Run instrumented tests on an Android emulator
4. Generate and upload JaCoCo coverage reports to Codecov
