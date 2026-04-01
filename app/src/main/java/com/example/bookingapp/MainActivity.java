package com.example.bookingapp;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EventRepository eventRepository;
    private EditText eventIdInput;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText locationInput;
    private EditText dateInput;
    private EditText totalSeatsInput;
    private EditText availableSeatsInput;
    private Spinner categorySpinner;
    private Spinner statusSpinner;
    private TextView feedbackText;
    private TextView reservationsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        eventRepository = new EventRepository();
        bindViews();
        configureSpinners();
        configureDateInput();
        configureButtons();
    }

    private void bindViews() {
        eventIdInput = findViewById(R.id.eventIdInput);
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        dateInput = findViewById(R.id.dateInput);
        totalSeatsInput = findViewById(R.id.totalSeatsInput);
        availableSeatsInput = findViewById(R.id.availableSeatsInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        statusSpinner = findViewById(R.id.statusSpinner);
        feedbackText = findViewById(R.id.feedbackText);
        reservationsText = findViewById(R.id.reservationsText);
    }

    private void configureSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                EventCategory.displayValues()
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                EventStatus.displayValues()
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
    }

    private void configureDateInput() {
        dateInput.setKeyListener(null);
        dateInput.setOnClickListener(view -> openDateTimePicker());
        dateInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                openDateTimePicker();
            }
        });
    }

    private void configureButtons() {
        Button addEventButton = findViewById(R.id.addEventButton);
        Button loadEventButton = findViewById(R.id.loadEventButton);
        Button updateEventButton = findViewById(R.id.updateEventButton);
        Button cancelEventButton = findViewById(R.id.cancelEventButton);
        Button viewReservationsButton = findViewById(R.id.viewReservationsButton);
        Button clearFormButton = findViewById(R.id.clearFormButton);

        addEventButton.setOnClickListener(view -> addEvent());
        loadEventButton.setOnClickListener(view -> loadEvent());
        updateEventButton.setOnClickListener(view -> updateEvent());
        cancelEventButton.setOnClickListener(view -> cancelEvent());
        viewReservationsButton.setOnClickListener(view -> viewReservations());
        clearFormButton.setOnClickListener(view -> clearForm());
    }

    private void addEvent() {
        Event event;
        try {
            event = buildEvent(false);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }

        eventRepository.addEvent(event, new EventRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                eventIdInput.setText(event.getEventId());
                showSuccess(message);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void loadEvent() {
        String eventId = readEventId();
        if (eventId.isEmpty()) {
            showError("Enter an event ID before loading an event.");
            return;
        }

        eventRepository.getEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                populateForm(event);
                showSuccess("Event loaded.");
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void updateEvent() {
        Event event;
        try {
            event = buildEvent(true);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }

        eventRepository.updateEvent(event, new EventRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                showSuccess(message);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void cancelEvent() {
        String eventId = readEventId();
        if (eventId.isEmpty()) {
            showError("Enter an event ID before cancelling an event.");
            return;
        }

        eventRepository.cancelEvent(eventId, new EventRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                setSpinnerValue(statusSpinner, EventStatus.CANCELLED.toFirestoreValue());
                showSuccess(message);
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void viewReservations() {
        String eventId = readEventId();
        if (eventId.isEmpty()) {
            showError("Enter an event ID before viewing reservations.");
            return;
        }

        eventRepository.getReservationsForEvent(eventId, new EventRepository.ReservationsCallback() {
            @Override
            public void onSuccess(List<ReservationSummary> reservations) {
                if (reservations.isEmpty()) {
                    reservationsText.setText(getString(R.string.no_reservations_found));
                } else {
                    reservationsText.setText(buildReservationDisplay(reservations));
                }
                showSuccess("Reservations loaded.");
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private Event buildEvent(boolean requireEventId) {
        return EventFormValidator.buildEvent(
                eventIdInput.getText().toString(),
                titleInput.getText().toString(),
                descriptionInput.getText().toString(),
                locationInput.getText().toString(),
                dateInput.getText().toString(),
                totalSeatsInput.getText().toString(),
                availableSeatsInput.getText().toString(),
                String.valueOf(categorySpinner.getSelectedItem()),
                String.valueOf(statusSpinner.getSelectedItem()),
                requireEventId
        );
    }

    private void populateForm(Event event) {
        eventIdInput.setText(event.getEventId());
        titleInput.setText(event.getTitle());
        descriptionInput.setText(event.getDescription());
        locationInput.setText(event.getLocation());
        dateInput.setText(EventFormValidator.formatDate(event.getDate()));
        totalSeatsInput.setText(String.valueOf(event.getTotalSeats()));
        availableSeatsInput.setText(String.valueOf(event.getAvailableSeats()));
        setSpinnerValue(categorySpinner, event.getCategory().toFirestoreValue());
        setSpinnerValue(statusSpinner, event.getStatus().toFirestoreValue());
    }

    private void openDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        applyExistingDateTime(calendar);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    openTimePicker(selectedCalendar);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void openTimePicker(Calendar selectedCalendar) {
        Calendar currentCalendar = Calendar.getInstance();
        applyExistingDateTime(currentCalendar);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (timePicker, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);
                    dateInput.setText(EventFormValidator.formatDate(selectedCalendar.getTime()));
                },
                currentCalendar.get(Calendar.HOUR_OF_DAY),
                currentCalendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }

    private void applyExistingDateTime(Calendar calendar) {
        String existingValue = dateInput.getText().toString().trim();
        if (existingValue.isEmpty()) {
            return;
        }

        try {
            calendar.setTime(EventFormValidator.buildEvent(
                    "preview",
                    "preview",
                    "preview",
                    "preview",
                    existingValue,
                    "1",
                    "1",
                    EventCategory.MOVIE.toFirestoreValue(),
                    EventStatus.AVAILABLE.toFirestoreValue(),
                    false
            ).getDate());
        } catch (IllegalArgumentException ignored) {
            // Fall back to the current date/time when the field contains an invalid value.
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int index = 0; index < adapter.getCount(); index++) {
            if (value.equals(adapter.getItem(index))) {
                spinner.setSelection(index);
                break;
            }
        }
    }

    private String buildReservationDisplay(List<ReservationSummary> reservations) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < reservations.size(); index++) {
            if (index > 0) {
                builder.append("\n\n");
            }
            builder.append(reservations.get(index).toDisplayString());
        }

        return builder.toString();
    }

    private void clearForm() {
        eventIdInput.setText("");
        titleInput.setText("");
        descriptionInput.setText("");
        locationInput.setText("");
        dateInput.setText("");
        totalSeatsInput.setText("");
        availableSeatsInput.setText("");
        categorySpinner.setSelection(0);
        statusSpinner.setSelection(0);
        feedbackText.setText("");
        reservationsText.setText(getString(R.string.reservations_placeholder));
    }

    private String readEventId() {
        return eventIdInput.getText().toString().trim();
    }

    private void showSuccess(String message) {
        feedbackText.setText(message);
        feedbackText.setTextColor(getColor(android.R.color.holo_green_dark));
    }

    private void showError(String message) {
        feedbackText.setText(message);
        feedbackText.setTextColor(getColor(android.R.color.holo_red_dark));
    }
}
