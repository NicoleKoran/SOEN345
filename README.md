# SOEN345

## Testing & Continuous Integration

This project uses an automated testing and CI pipeline to ensure code quality and reliability.

### Unit Testing

Unit tests are implemented using **JUnit** and run on the JVM to verify the correctness of core application logic.

### Instrumentation / System Testing

End-to-end and UI tests are implemented using Android instrumentation tests and executed on an Android emulator to validate full application workflows.

### Continuous Integration

All tests are automatically executed through **GitHub Actions** on every push to `main` and on all pull requests. The CI pipeline performs the following steps:

1. Build the Android project
2. Execute JUnit unit tests
3. Run instrumentation tests on an Android emulator
4. Generate test coverage reports

### Code Coverage

Code coverage is generated using **JaCoCo** and uploaded to **Codecov**, allowing the team to track and monitor test coverage across the project.

This automated workflow helps maintain code quality by ensuring that new changes are tested and validated before being merged.
