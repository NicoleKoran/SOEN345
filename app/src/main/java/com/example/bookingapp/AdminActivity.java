package com.example.bookingapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingapp.adapters.AdminEventAdapter;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "event_id";
    private static final String[] EDITABLE_STATUS_VALUES = {
            "available",
            "soldOut"
    };
    private EventRepository eventRepository;
    private RecyclerView eventsRecyclerView;
    private android.view.View formScrollView;
    private android.view.View formContainer;
    private android.view.View newEventButton;
    private EditText searchInput;
    private EditText eventIdInput;
    private EditText titleInput;
    private EditText descriptionInput;
    private EditText locationInput;
    private EditText dateInput;
    private EditText totalSeatsInput;
    private EditText availableSeatsInput;
    private Spinner categorySpinner;
    private Spinner statusSpinner;
    private Button addEventButton;
    private Button updateEventButton;
    private Button viewReservationsButton;
    private Button cancelEventButton;
    private Button deleteEventButton;
    private BookingRepository bookingRepository;
    private TextView reservationsLabel;
    private TextView statusLabel;
    private TextView eventHeaderText;
    private TextView formModeText;
    private TextView resultsCount;
    private TextView feedbackText;
    private TextView reservationsText;
    private AdminEventAdapter adminEventAdapter;
    private final List<Event> allEvents = new ArrayList<>();
    private boolean isEditMode;
    private boolean hasUnsavedChanges;
    private boolean suppressChangeTracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        eventRepository = new EventRepository();
        bookingRepository = new BookingRepository();
        bindViews();
        configureEventsList();
        configureSpinners();
        configureSearch();
        configureDateInput();
        configureButtons();
        configureBackNavigation();
        configureFormScreenMode();
    }

    private void bindViews() {
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        formScrollView = findViewById(R.id.formScrollView);
        formContainer = findViewById(R.id.formContainer);
        newEventButton = findViewById(R.id.newEventButton);
        searchInput = findViewById(R.id.searchInput);
        eventIdInput = findViewById(R.id.eventIdInput);
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        locationInput = findViewById(R.id.locationInput);
        dateInput = findViewById(R.id.dateInput);
        totalSeatsInput = findViewById(R.id.totalSeatsInput);
        availableSeatsInput = findViewById(R.id.availableSeatsInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        statusSpinner = findViewById(R.id.statusSpinner);
        addEventButton = findViewById(R.id.addEventButton);
        updateEventButton = findViewById(R.id.updateEventButton);
        viewReservationsButton = findViewById(R.id.viewReservationsButton);
        cancelEventButton = findViewById(R.id.cancelEventButton);
        deleteEventButton = findViewById(R.id.deleteEventButton);
        reservationsLabel = findViewById(R.id.reservationsLabel);
        statusLabel = findViewById(R.id.statusLabel);
        eventHeaderText = findViewById(R.id.eventHeaderText);
        formModeText = findViewById(R.id.formModeText);
        resultsCount = findViewById(R.id.resultsCount);
        feedbackText = findViewById(R.id.feedbackText);
        reservationsText = findViewById(R.id.reservationsText);
    }

    private void configureEventsList() {
        adminEventAdapter = new AdminEventAdapter(new AdminEventAdapter.EventActionListener() {
            @Override
            public void onEdit(Event event) {
                populateForm(event);
                showSuccess("Event loaded for editing.");
            }
        });

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(adminEventAdapter);
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
                EDITABLE_STATUS_VALUES
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                syncAvailableSeatsWithStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
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
        TextView backToEventsButton = findViewById(R.id.backToEventsButton);

        backToEventsButton.setOnClickListener(view -> handleBackNavigation());
        newEventButton.setOnClickListener(view -> showNewEventForm());
        addEventButton.setOnClickListener(view -> addEvent());
        updateEventButton.setOnClickListener(view -> updateEvent());
        viewReservationsButton.setOnClickListener(view -> viewReservations());
        cancelEventButton.setOnClickListener(view -> promptCancelEvent());
        deleteEventButton.setOnClickListener(view -> promptDeleteWithPassword());

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateHeaderTitle(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                markUnsavedChanges();
            }
        });

        totalSeatsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSeatCounts();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        availableSeatsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateSeatCounts();
            }

            @Override
            public void afterTextChanged(Editable s) {
                markUnsavedChanges();
            }
        });

        attachChangeWatcher(descriptionInput);
        attachChangeWatcher(locationInput);
        attachChangeWatcher(dateInput);
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    private void configureFormScreenMode() {
        searchInput.setVisibility(android.view.View.GONE);
        resultsCount.setVisibility(android.view.View.GONE);
        eventsRecyclerView.setVisibility(android.view.View.GONE);
        newEventButton.setVisibility(android.view.View.GONE);

        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (eventId != null && !eventId.trim().isEmpty()) {
            isEditMode = true;
            showForm();
            eventIdInput.setText(eventId);
            loadEvent();
            return;
        }

        isEditMode = false;
        showNewEventForm();
    }

    private void configureSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEvents();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void addEvent() {
        if (!validateSeatCounts()) {
            return;
        }

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
                suppressChangeTracking = true;
                eventIdInput.setText(event.getEventId());
                suppressChangeTracking = false;
                isEditMode = true;
                hasUnsavedChanges = false;
                addEventButton.setVisibility(android.view.View.GONE);
                updateEventButton.setVisibility(android.view.View.VISIBLE);
                showAddEventSavedDialog();
            }

            @Override
            public void onError(String message) {
                if ("An event with this ID already exists.".equals(message)) {
                    showAddEventExistsDialog();
                    return;
                }
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
        if (!validateSeatCounts()) {
            return;
        }

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
                hasUnsavedChanges = false;
                showUpdateSavedDialog();
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

    /** US-14: Cancels the event and emails all customers with confirmed reservations. */
    private void promptCancelEvent() {
        String eventId = readEventId();
        if (eventId.isEmpty()) {
            showError("Load an event before cancelling it.");
            return;
        }

        String eventTitle = titleInput.getText().toString().trim();
        String eventLocation = locationInput.getText().toString().trim();
        String eventDate = dateInput.getText().toString().trim();

        new AlertDialog.Builder(this)
                .setTitle("Cancel Event")
                .setMessage("Cancel '" + (eventTitle.isEmpty() ? eventId : eventTitle)
                        + "'? All customers with confirmed reservations will be notified by email.")
                .setPositiveButton("Cancel Event", (dialog, which) ->
                        doCancelEvent(eventId, eventTitle, eventLocation, eventDate))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doCancelEvent(String eventId, String eventTitle,
                               String eventLocation, String eventDate) {
        bookingRepository.cancelEventWithNotifications(
                eventId, eventTitle, eventLocation, eventDate,
                new BookingRepository.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        cancelEventButton.setVisibility(android.view.View.GONE);
                        showSuccess(message);
                        feedbackText.setText(message);
                        feedbackText.setTextColor(getColor(android.R.color.holo_green_dark));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showError(errorMessage);
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
                isEditMode ? String.valueOf(statusSpinner.getSelectedItem()) : EventStatus.AVAILABLE.toFirestoreValue(),
                requireEventId
        );
    }

    private void populateForm(Event event) {
        showForm();
        suppressChangeTracking = true;
        addEventButton.setVisibility(android.view.View.GONE);
        updateEventButton.setVisibility(android.view.View.VISIBLE);
        viewReservationsButton.setVisibility(android.view.View.VISIBLE);
        cancelEventButton.setVisibility(android.view.View.VISIBLE);
        deleteEventButton.setVisibility(android.view.View.VISIBLE);
        statusLabel.setVisibility(android.view.View.VISIBLE);
        statusSpinner.setVisibility(android.view.View.VISIBLE);
        reservationsLabel.setVisibility(android.view.View.VISIBLE);
        reservationsText.setVisibility(android.view.View.VISIBLE);
        eventIdInput.setText(event.getEventId());
        titleInput.setText(event.getTitle());
        descriptionInput.setText(event.getDescription());
        locationInput.setText(event.getLocation());
        dateInput.setText(EventFormValidator.formatDate(event.getDate()));
        totalSeatsInput.setText(String.valueOf(event.getTotalSeats()));
        availableSeatsInput.setText(String.valueOf(event.getAvailableSeats()));
        setSpinnerValue(categorySpinner, event.getCategory().toFirestoreValue());
        setSpinnerValue(statusSpinner, event.getStatus().toFirestoreValue());
        updateHeaderTitle(event.getTitle());
        formModeText.setText(R.string.admin_form_title);
        suppressChangeTracking = false;
        isEditMode = true;
        hasUnsavedChanges = false;
    }

    private void showNewEventForm() {
        clearForm();
        showForm();
        addEventButton.setVisibility(android.view.View.VISIBLE);
        updateEventButton.setVisibility(android.view.View.GONE);
        viewReservationsButton.setVisibility(android.view.View.GONE);
        cancelEventButton.setVisibility(android.view.View.GONE);
        deleteEventButton.setVisibility(android.view.View.GONE);
        statusLabel.setVisibility(android.view.View.GONE);
        statusSpinner.setVisibility(android.view.View.GONE);
        reservationsLabel.setVisibility(android.view.View.GONE);
        reservationsText.setVisibility(android.view.View.GONE);
        statusSpinner.setSelection(0);
        updateHeaderTitle("");
        titleInput.requestFocus();
        isEditMode = false;
        hasUnsavedChanges = false;
    }

    private void showForm() {
        formScrollView.setVisibility(android.view.View.VISIBLE);
        formContainer.setVisibility(android.view.View.VISIBLE);
    }

    private void refreshEvents() {
        eventRepository.getAllEvents(new EventRepository.EventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                allEvents.clear();
                allEvents.addAll(events);
                filterEvents();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void filterEvents() {
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.US);
        List<Event> filteredEvents = new ArrayList<>();

        for (Event event : allEvents) {
            if (query.isEmpty() || matchesQuery(event, query)) {
                filteredEvents.add(event);
            }
        }

        adminEventAdapter.setEvents(filteredEvents);
        resultsCount.setText(getString(R.string.events_count, filteredEvents.size()));
        if (filteredEvents.isEmpty()) {
            feedbackText.setText(R.string.no_events_found);
        } else if (getString(R.string.no_events_found).contentEquals(feedbackText.getText())) {
            feedbackText.setText("");
        }
    }

    private boolean matchesQuery(Event event, String query) {
        return contains(event.getTitle(), query)
                || contains(event.getLocation(), query)
                || contains(event.getDescription(), query)
                || contains(event.getCategory().toFirestoreValue(), query)
                || contains(event.getStatus().toFirestoreValue(), query)
                || contains(event.getEventId(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.US).contains(query);
    }

    private void promptDeleteWithPassword() {
        String eventId = readEventId();
        if (eventId.isEmpty()) {
            showError("Load an event before deleting it.");
            return;
        }

        String eventTitle = titleInput.getText().toString().trim();
        EditText passwordInput = new EditText(this);
        passwordInput.setHint(R.string.password_hint);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        passwordInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirmation_title)
                .setMessage(getString(R.string.delete_confirmation_message,
                        eventTitle.isEmpty() ? eventId : eventTitle) + "\n\n"
                        + getString(R.string.delete_password_prompt))
                .setView(passwordInput)
                .setPositiveButton(R.string.confirm_delete, (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.trim().isEmpty()) {
                        showError(getString(R.string.delete_password_required));
                        return;
                    }
                    verifyDeletionPasswordAndDelete(eventId, password);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void verifyDeletionPasswordAndDelete(String eventId, String password) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String email = currentUser != null ? currentUser.getEmail() : LoginActivity.ADMIN_EMAIL;

        if (LoginActivity.ADMIN_EMAIL.equalsIgnoreCase(email) && LoginActivity.ADMIN_PASSWORD.equals(password)) {
            deleteEvent(eventId);
            return;
        }

        if (currentUser == null || currentUser.getEmail() == null) {
            showError(getString(R.string.delete_password_invalid));
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> deleteEvent(eventId))
                .addOnFailureListener(error -> showError(
                        error.getMessage() == null ? getString(R.string.delete_password_invalid) : error.getMessage()));
    }

    private void deleteEvent(String eventId) {
        String deletedEventName = titleInput.getText().toString().trim();
        eventRepository.deleteEvent(eventId, new EventRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putString(
                                MainActivity.KEY_PENDING_DELETE_EVENT_NAME,
                                deletedEventName.isEmpty() ? getString(R.string.new_event_title) : deletedEventName
                        )
                        .apply();
                finish();
            }

            @Override
            public void onError(String message) {
                showError(message);
            }
        });
    }

    private void showUpdateSavedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_saved_title)
                .setMessage(R.string.event_saved_message)
                .setNegativeButton(R.string.continue_editing, null)
                .setPositiveButton(R.string.back_to_list, (dialog, which) -> finish())
                .show();
    }

    private void showAddEventSavedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_added_title)
                .setMessage(R.string.event_added_message)
                .setNegativeButton(R.string.continue_editing, null)
                .setPositiveButton(R.string.back_to_list, (dialog, which) -> finish())
                .show();
    }

    private void showAddEventExistsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_exists_title)
                .setMessage(R.string.event_exists_message)
                .setNegativeButton(R.string.add_another_event, (dialog, which) -> showNewEventForm())
                .setPositiveButton(R.string.back_to_list, (dialog, which) -> finish())
                .show();
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
        suppressChangeTracking = true;
        eventIdInput.setText("");
        titleInput.setText("");
        descriptionInput.setText("");
        locationInput.setText("");
        dateInput.setText("");
        totalSeatsInput.setText("");
        availableSeatsInput.setText("");
        categorySpinner.setSelection(0);
        statusSpinner.setSelection(0);
        eventHeaderText.setText(R.string.new_event_title);
        formModeText.setText(R.string.admin_form_title);
        formScrollView.setVisibility(android.view.View.GONE);
        formContainer.setVisibility(android.view.View.GONE);
        feedbackText.setText("");
        reservationsText.setText(getString(R.string.reservations_placeholder));
        suppressChangeTracking = false;
        hasUnsavedChanges = false;
    }

    private void updateHeaderTitle(String title) {
        String trimmedTitle = title == null ? "" : title.trim();
        eventHeaderText.setText(trimmedTitle.isEmpty() ? getString(R.string.new_event_title) : trimmedTitle);
    }

    private void syncAvailableSeatsWithStatus() {
        EventStatus selectedStatus = EventStatus.fromValue(String.valueOf(statusSpinner.getSelectedItem()));
        if (selectedStatus == EventStatus.CANCELLED || selectedStatus == EventStatus.SOLDOUT) {
            availableSeatsInput.setText("0");
        }
        markUnsavedChanges();
    }

    private boolean validateSeatCounts() {
        String totalSeatsValue = totalSeatsInput.getText().toString().trim();
        String availableSeatsValue = availableSeatsInput.getText().toString().trim();

        totalSeatsInput.setError(null);
        availableSeatsInput.setError(null);

        if (totalSeatsValue.isEmpty() || availableSeatsValue.isEmpty()) {
            return true;
        }

        try {
            int totalSeats = Integer.parseInt(totalSeatsValue);
            int availableSeats = Integer.parseInt(availableSeatsValue);

            if (availableSeats > totalSeats) {
                availableSeatsInput.setError("Available seats cannot exceed total seats.");
                return false;
            }
        } catch (NumberFormatException ignored) {
            return true;
        }

        return true;
    }

    private void attachChangeWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                markUnsavedChanges();
            }
        });
    }

    private void markUnsavedChanges() {
        if (!suppressChangeTracking) {
            hasUnsavedChanges = true;
        }
    }

    private void handleBackNavigation() {
        if (!hasUnsavedChanges) {
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.unsaved_changes_title)
                .setMessage(R.string.unsaved_changes_message)
                .setNegativeButton(R.string.discard_changes, (dialog, which) -> finish())
                .setPositiveButton(R.string.update_event, (dialog, which) -> saveCurrentForm())
                .show();
    }

    private void saveCurrentForm() {
        if (isEditMode) {
            updateEvent();
        } else {
            addEvent();
        }
    }

    private String readEventId() {
        return eventIdInput.getText().toString().trim();
    }

    private void showSuccess(String message) {
        feedbackText.setText("");
    }

    private void showError(String message) {
        feedbackText.setText(message);
        feedbackText.setTextColor(getColor(android.R.color.holo_red_dark));
    }
}
