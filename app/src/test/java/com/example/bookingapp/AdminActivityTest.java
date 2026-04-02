package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
public class AdminActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void addMode_showsOnlyCreateActions() throws Exception {
        AdminActivity activity = Robolectric.buildActivity(AdminActivity.class).setup().get();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.addEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.updateEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.viewReservationsButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.deleteEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.statusLabel).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.statusSpinner).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.reservationsLabel).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.reservationsText).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.searchInput).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.resultsCount).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.eventsRecyclerView).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.newEventButton).getVisibility());
    }

    @Test
    public void populateForm_switchesToEditModeAndPopulatesFields() throws Exception {
        AdminActivity activity = buildActivity();
        Event event = new Event(
                "event-7",
                "Jazz Night",
                "Live music",
                "Montreal",
                new Date(1775452380000L),
                200,
                40,
                EventCategory.CONCERT,
                EventStatus.AVAILABLE
        );

        Method populateForm = AdminActivity.class.getDeclaredMethod("populateForm", Event.class);
        populateForm.setAccessible(true);
        populateForm.invoke(activity, event);

        assertEquals(View.GONE, activity.findViewById(R.id.addEventButton).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.updateEventButton).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.viewReservationsButton).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.deleteEventButton).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.statusLabel).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.statusSpinner).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.reservationsLabel).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.reservationsText).getVisibility());
        assertEquals("Jazz Night", ((EditText) activity.findViewById(R.id.titleInput)).getText().toString());
        assertEquals("Live music", ((EditText) activity.findViewById(R.id.descriptionInput)).getText().toString());
        assertEquals("Montreal", ((EditText) activity.findViewById(R.id.locationInput)).getText().toString());
        assertEquals("Jazz Night", ((TextView) activity.findViewById(R.id.eventHeaderText)).getText().toString());
    }

    @Test
    public void validateSeatCounts_rejectsAvailableSeatsGreaterThanTotal() throws Exception {
        AdminActivity activity = buildActivity();

        ((EditText) activity.findViewById(R.id.totalSeatsInput)).setText("10");
        EditText availableSeatsInput = activity.findViewById(R.id.availableSeatsInput);
        availableSeatsInput.setText("11");

        boolean isValid = (boolean) invokePrivate(activity, "validateSeatCounts");

        assertFalse(isValid);
        assertEquals(
                "Available seats cannot exceed total seats.",
                String.valueOf(availableSeatsInput.getError())
        );
    }

    @Test
    public void editMode_selectingSoldOutStatusSetsAvailableSeatsToZero() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        Spinner statusSpinner = activity.findViewById(R.id.statusSpinner);
        statusSpinner.setSelection(1);

        assertEquals("0", ((EditText) activity.findViewById(R.id.availableSeatsInput)).getText().toString());
    }

    @Test
    public void updateEvent_successShowsSavedDialog() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onSuccess("Event updated successfully.");
            return null;
        }).when(repository).updateEvent(any(Event.class), any(EventRepository.MessageCallback.class));

        ((Button) activity.findViewById(R.id.updateEventButton)).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.event_saved_title),
                shadowOf(dialog).getTitle().toString()
        );
    }

    @Test
    public void addEvent_successSwitchesToEditModeAndShowsConfirmation() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        fillValidForm(activity);

        doAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setEventId("generated-id");
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onSuccess("Event added.");
            return null;
        }).when(repository).addEvent(any(Event.class), any(EventRepository.MessageCallback.class));

        ((Button) activity.findViewById(R.id.addEventButton)).performClick();

        assertEquals("generated-id", ((EditText) activity.findViewById(R.id.eventIdInput)).getText().toString());
        assertEquals(View.GONE, activity.findViewById(R.id.addEventButton).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.updateEventButton).getVisibility());

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.event_added_title),
                shadowOf(dialog).getTitle().toString()
        );
    }

    @Test
    public void addEvent_duplicateShowsExistsDialog() {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        fillValidForm(activity);

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onError("An event with this ID already exists.");
            return null;
        }).when(repository).addEvent(any(Event.class), any(EventRepository.MessageCallback.class));

        ((Button) activity.findViewById(R.id.addEventButton)).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.event_exists_title),
                shadowOf(dialog).getTitle().toString()
        );
    }

    @Test
    public void viewReservations_emptyResultShowsPlaceholderMessage() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.ReservationsCallback callback = invocation.getArgument(1);
            callback.onSuccess(List.of());
            return null;
        }).when(repository).getReservationsForEvent(eq("event-7"), any(EventRepository.ReservationsCallback.class));

        ((Button) activity.findViewById(R.id.viewReservationsButton)).performClick();

        assertEquals(
                activity.getString(R.string.no_reservations_found),
                ((TextView) activity.findViewById(R.id.reservationsText)).getText().toString()
        );
    }

    @Test
    public void viewReservations_withResultsDisplaysReservationSummary() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", "Jane Doe");
        fields.put("tickets", 2);
        ReservationSummary summary = new ReservationSummary("res-1", fields);

        doAnswer(invocation -> {
            EventRepository.ReservationsCallback callback = invocation.getArgument(1);
            callback.onSuccess(List.of(summary));
            return null;
        }).when(repository).getReservationsForEvent(eq("event-7"), any(EventRepository.ReservationsCallback.class));

        ((Button) activity.findViewById(R.id.viewReservationsButton)).performClick();

        String displayText = ((TextView) activity.findViewById(R.id.reservationsText)).getText().toString();
        assertTrue(displayText.contains("Reservation ID: res-1"));
        assertTrue(displayText.contains("name: Jane Doe"));
        assertTrue(displayText.contains("tickets: 2"));
    }

    @Test
    public void deletePrompt_emptyPasswordShowsError() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        ((Button) activity.findViewById(R.id.deleteEventButton)).performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(
                activity.getString(R.string.delete_password_required),
                ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString()
        );
    }

    @Test
    public void deleteEvent_successStoresDeletedNameAndFinishes() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onSuccess("Event deleted successfully.");
            return null;
        }).when(repository).deleteEvent(eq("event-7"), any(EventRepository.MessageCallback.class));

        invokePrivate(activity, "deleteEvent", String.class, "event-7");

        SharedPreferences preferences = activity.getSharedPreferences(LoginActivity.PREFS_NAME, AdminActivity.MODE_PRIVATE);
        assertEquals(
                "Jazz Night",
                preferences.getString(MainActivity.KEY_PENDING_DELETE_EVENT_NAME, null)
        );
        assertTrue(activity.isFinishing());
    }

    @Test
    public void handleBackNavigation_withUnsavedChangesShowsDiscardDialog() throws Exception {
        AdminActivity activity = buildActivity();

        ((EditText) activity.findViewById(R.id.titleInput)).setText("Changed");
        invokePrivate(activity, "handleBackNavigation");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(
                activity.getString(R.string.unsaved_changes_title),
                shadowOf(dialog).getTitle().toString()
        );

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void handleBackNavigation_withoutUnsavedChangesFinishesImmediately() throws Exception {
        AdminActivity activity = buildActivity();

        invokePrivate(activity, "handleBackNavigation");

        assertTrue(activity.isFinishing());
    }

    @Test
    public void addEvent_invalidSeatCountsDoesNotCallRepository() {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        fillValidForm(activity);
        ((EditText) activity.findViewById(R.id.totalSeatsInput)).setText("10");
        ((EditText) activity.findViewById(R.id.availableSeatsInput)).setText("11");

        ((Button) activity.findViewById(R.id.addEventButton)).performClick();

        verify(repository, never()).addEvent(any(Event.class), any(EventRepository.MessageCallback.class));
    }

    @Test
    public void addEvent_repositoryErrorShowsFeedback() {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        fillValidForm(activity);

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onError("Add failed.");
            return null;
        }).when(repository).addEvent(any(Event.class), any(EventRepository.MessageCallback.class));

        ((Button) activity.findViewById(R.id.addEventButton)).performClick();

        assertEquals("Add failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void updateEvent_repositoryErrorShowsFeedback() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onError("Update failed.");
            return null;
        }).when(repository).updateEvent(any(Event.class), any(EventRepository.MessageCallback.class));

        ((Button) activity.findViewById(R.id.updateEventButton)).performClick();

        assertEquals("Update failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void viewReservations_withoutEventIdShowsError() {
        AdminActivity activity = buildActivity();

        ((Button) activity.findViewById(R.id.viewReservationsButton)).performClick();

        assertEquals(
                "Enter an event ID before viewing reservations.",
                ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString()
        );
    }

    @Test
    public void viewReservations_repositoryErrorShowsFeedback() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.ReservationsCallback callback = invocation.getArgument(1);
            callback.onError("Reservations failed.");
            return null;
        }).when(repository).getReservationsForEvent(eq("event-7"), any(EventRepository.ReservationsCallback.class));

        ((Button) activity.findViewById(R.id.viewReservationsButton)).performClick();

        assertEquals("Reservations failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void deleteEvent_errorShowsFeedback() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onError("Delete failed.");
            return null;
        }).when(repository).deleteEvent(eq("event-7"), any(EventRepository.MessageCallback.class));

        invokePrivate(activity, "deleteEvent", String.class, "event-7");

        assertEquals("Delete failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void loadEvent_withoutIdShowsError() throws Exception {
        AdminActivity activity = buildActivity();

        invokePrivate(activity, "loadEvent");

        assertEquals(
                "Enter an event ID before loading an event.",
                ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString()
        );
    }

    @Test
    public void loadEvent_repositorySuccessPopulatesForm() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        ((EditText) activity.findViewById(R.id.eventIdInput)).setText("event-7");

        doAnswer(invocation -> {
            EventRepository.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(sampleEvent());
            return null;
        }).when(repository).getEvent(eq("event-7"), any(EventRepository.EventCallback.class));

        invokePrivate(activity, "loadEvent");

        assertEquals("Jazz Night", ((EditText) activity.findViewById(R.id.titleInput)).getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.updateEventButton).getVisibility());
    }

    @Test
    public void addEventExistsDialog_negativeActionResetsToNewEventForm() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        invokePrivate(activity, "showAddEventExistsDialog");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(View.VISIBLE, activity.findViewById(R.id.addEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.updateEventButton).getVisibility());
        assertEquals(
                activity.getString(R.string.new_event_title),
                ((TextView) activity.findViewById(R.id.eventHeaderText)).getText().toString()
        );
    }

    @Test
    public void showNewEventForm_clearsFieldsAndHidesEditOnlyControls() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        invokePrivate(activity, "showNewEventForm");

        assertEquals("", ((EditText) activity.findViewById(R.id.eventIdInput)).getText().toString());
        assertEquals("", ((EditText) activity.findViewById(R.id.titleInput)).getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.addEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.updateEventButton).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.deleteEventButton).getVisibility());
    }

    @Test
    public void validateSeatCounts_acceptsBlankAndNumericValues() throws Exception {
        AdminActivity activity = buildActivity();

        assertTrue((boolean) invokePrivate(activity, "validateSeatCounts"));

        ((EditText) activity.findViewById(R.id.totalSeatsInput)).setText("10");
        ((EditText) activity.findViewById(R.id.availableSeatsInput)).setText("5");

        assertTrue((boolean) invokePrivate(activity, "validateSeatCounts"));
    }

    @Test
    public void buildReservationDisplay_joinsReservationsWithBlankLines() throws Exception {
        AdminActivity activity = buildActivity();
        ReservationSummary first = new ReservationSummary("r1", java.util.Map.of("name", "Jane"));
        ReservationSummary second = new ReservationSummary("r2", java.util.Map.of("name", "John"));

        String display = (String) invokePrivate(activity, "buildReservationDisplay", List.class, List.of(first, second));

        assertTrue(display.contains("Reservation ID: r1"));
        assertTrue(display.contains("\n\n"));
        assertTrue(display.contains("Reservation ID: r2"));
    }

    @Test
    public void updateSavedDialog_positiveActionFinishesActivity() throws Exception {
        AdminActivity activity = buildActivity();

        invokePrivate(activity, "showUpdateSavedDialog");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void addSavedDialog_positiveActionFinishesActivity() throws Exception {
        AdminActivity activity = buildActivity();

        invokePrivate(activity, "showAddEventSavedDialog");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void eventsListEditAction_populatesFormThroughConfiguredListener() throws Exception {
        AdminActivity activity = buildActivity();
        RecyclerView.Adapter adapter = ((RecyclerView) activity.findViewById(R.id.eventsRecyclerView)).getAdapter();
        ((com.example.bookingapp.adapters.AdminEventAdapter) adapter).setEvents(List.of(sampleEvent()));
        android.widget.FrameLayout parent = new android.widget.FrameLayout(activity);
        RecyclerView.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        //noinspection unchecked,rawtypes
        ((RecyclerView.Adapter) adapter).onBindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.editButton).performClick();

        assertEquals("Jazz Night", ((EditText) activity.findViewById(R.id.titleInput)).getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.updateEventButton).getVisibility());
    }

    @Test
    public void dateInput_clickAndFocusOpenDatePicker() {
        AdminActivity activity = buildActivity();
        EditText dateInput = activity.findViewById(R.id.dateInput);

        assertEquals(null, dateInput.getKeyListener());

        dateInput.performClick();
        assertTrue(ShadowAlertDialog.getLatestAlertDialog() instanceof DatePickerDialog);

        ShadowAlertDialog.getLatestAlertDialog().dismiss();
        dateInput.getOnFocusChangeListener().onFocusChange(dateInput, true);
        assertTrue(ShadowAlertDialog.getLatestAlertDialog() instanceof DatePickerDialog);
    }

    @Test
    public void onBackPressedDispatcher_triggersUnsavedChangesDialog() {
        AdminActivity activity = buildActivity();
        ((EditText) activity.findViewById(R.id.titleInput)).setText("Changed");

        activity.getOnBackPressedDispatcher().onBackPressed();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(activity.getString(R.string.unsaved_changes_title), shadowOf(dialog).getTitle().toString());
    }

    @Test
    public void refreshEvents_successLoadsEventsAndFilters() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        List<Event> events = List.of(sampleEvent(), new Event(
                "event-8",
                "Movie Night",
                "Marvel marathon",
                "Toronto",
                new Date(1775538780000L),
                100,
                80,
                EventCategory.MOVIE,
                EventStatus.AVAILABLE
        ));

        doAnswer(invocation -> {
            EventRepository.EventsCallback callback = invocation.getArgument(0);
            callback.onSuccess(events);
            return null;
        }).when(repository).getAllEvents(any(EventRepository.EventsCallback.class));

        invokePrivate(activity, "refreshEvents");

        assertEquals("2 event(s)", ((TextView) activity.findViewById(R.id.resultsCount)).getText().toString());
    }

    @Test
    public void refreshEvents_errorShowsFeedback() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);

        doAnswer(invocation -> {
            EventRepository.EventsCallback callback = invocation.getArgument(0);
            callback.onError("Load failed.");
            return null;
        }).when(repository).getAllEvents(any(EventRepository.EventsCallback.class));

        invokePrivate(activity, "refreshEvents");

        assertEquals("Load failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void searchWatcher_filtersEventsAndShowsNoResultsMessage() throws Exception {
        AdminActivity activity = buildActivity();
        @SuppressWarnings("unchecked")
        List<Event> events = (List<Event>) getField(activity, "allEvents");
        events.clear();
        events.add(sampleEvent());

        ((EditText) activity.findViewById(R.id.searchInput)).setText("nomatch");

        assertEquals("No events match the current search.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void filterEvents_matchingResultClearsNoEventsMessage() throws Exception {
        AdminActivity activity = buildActivity();
        @SuppressWarnings("unchecked")
        List<Event> events = (List<Event>) getField(activity, "allEvents");
        events.clear();
        events.add(sampleEvent());
        ((TextView) activity.findViewById(R.id.feedbackText)).setText(R.string.no_events_found);
        ((EditText) activity.findViewById(R.id.searchInput)).setText("jazz");

        invokePrivate(activity, "filterEvents");

        assertEquals("", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void matchesQuery_andContains_coverAllBranches() throws Exception {
        AdminActivity activity = buildActivity();
        Event event = sampleEvent();

        assertTrue((boolean) invokePrivate(activity, "matchesQuery", Event.class, String.class, event, "montreal"));
        assertFalse((boolean) invokePrivate(activity, "contains", String.class, String.class, null, "montreal"));
    }

    @Test
    public void promptDeleteWithPassword_withoutEventIdShowsError() throws Exception {
        AdminActivity activity = buildActivity();

        invokePrivate(activity, "promptDeleteWithPassword");

        assertEquals("Load an event before deleting it.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void verifyDeletionPasswordAndDelete_hardcodedAdminPasswordDeletesEvent() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onSuccess("deleted");
            return null;
        }).when(repository).deleteEvent(eq("event-7"), any(EventRepository.MessageCallback.class));

        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.getCurrentUser()).thenReturn(null);

            invokePrivate(activity, "verifyDeletionPasswordAndDelete", String.class, String.class, "event-7", LoginActivity.ADMIN_PASSWORD);
        }

        assertTrue(activity.isFinishing());
    }

    @Test
    public void verifyDeletionPasswordAndDelete_invalidWithoutCurrentUserShowsError() throws Exception {
        AdminActivity activity = buildActivity();

        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.getCurrentUser()).thenReturn(null);

            invokePrivate(activity, "verifyDeletionPasswordAndDelete", String.class, String.class, "event-7", "wrong");
        }

        assertEquals(
                activity.getString(R.string.delete_password_invalid),
                ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString()
        );
    }

    @Test
    public void verifyDeletionPasswordAndDelete_reauthFailureShowsError() throws Exception {
        AdminActivity activity = buildActivity();
        FirebaseUser user = mock(FirebaseUser.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        AuthCredential credential = mock(AuthCredential.class);

        when(user.getEmail()).thenReturn("user@test.com");
        when(user.reauthenticate(credential)).thenReturn(Tasks.forException(new RuntimeException("Bad password")));
        when(firebaseAuth.getCurrentUser()).thenReturn(user);

        try (var auth = mockStatic(FirebaseAuth.class);
             var emailAuth = mockStatic(com.google.firebase.auth.EmailAuthProvider.class)) {
            auth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            emailAuth.when(() -> com.google.firebase.auth.EmailAuthProvider.getCredential("user@test.com", "wrong"))
                    .thenReturn(credential);

            invokePrivate(activity, "verifyDeletionPasswordAndDelete", String.class, String.class, "event-7", "wrong");
            shadowOf(Looper.getMainLooper()).idle();
        }

        assertEquals("Bad password", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void applyExistingDateTime_handlesValidAndInvalidExistingDate() throws Exception {
        AdminActivity activity = buildActivity();
        Calendar calendar = Calendar.getInstance();
        ((EditText) activity.findViewById(R.id.dateInput)).setText("2026-04-05 19:13");

        invokePrivate(activity, "applyExistingDateTime", Calendar.class, calendar);

        assertEquals(2026, calendar.get(Calendar.YEAR));

        ((EditText) activity.findViewById(R.id.dateInput)).setText("invalid");
        invokePrivate(activity, "applyExistingDateTime", Calendar.class, calendar);
        assertEquals(2026, calendar.get(Calendar.YEAR));
    }

    @Test
    public void openDateTimePicker_showsDateDialogUsingExistingDate() throws Exception {
        AdminActivity activity = buildActivity();
        ((EditText) activity.findViewById(R.id.dateInput)).setText("2026-04-05 19:13");

        invokePrivate(activity, "openDateTimePicker");

        assertTrue(ShadowAlertDialog.getLatestAlertDialog() instanceof DatePickerDialog);
    }

    @Test
    public void formScreenMode_withIntentEventIdLoadsEvent() {
        android.content.Intent intent = new android.content.Intent(ApplicationProvider.getApplicationContext(), AdminActivity.class);
        intent.putExtra(AdminActivity.EXTRA_EVENT_ID, "event-123");
        AdminActivity activity = Robolectric.buildActivity(AdminActivity.class, intent).setup().get();

        assertEquals("event-123", ((EditText) activity.findViewById(R.id.eventIdInput)).getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.formScrollView).getVisibility());
    }

    @Test
    public void loadEvent_repositoryErrorShowsFeedback() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        ((EditText) activity.findViewById(R.id.eventIdInput)).setText("event-7");

        doAnswer(invocation -> {
            EventRepository.EventCallback callback = invocation.getArgument(1);
            callback.onError("Load failed.");
            return null;
        }).when(repository).getEvent(eq("event-7"), any(EventRepository.EventCallback.class));

        invokePrivate(activity, "loadEvent");

        assertEquals("Load failed.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void updateEvent_invalidFormShowsValidationError() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());
        ((EditText) activity.findViewById(R.id.titleInput)).setText("");

        invokePrivate(activity, "updateEvent");

        assertEquals("Title is required.", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    @Test
    public void promptDeleteWithPassword_showsConfirmationDialogForLoadedEvent() throws Exception {
        AdminActivity activity = buildActivity();
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());

        invokePrivate(activity, "promptDeleteWithPassword");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(shadowOf(dialog).getMessage().toString().contains("Jazz Night"));
    }

    @Test
    public void verifyDeletionPasswordAndDelete_reauthSuccessDeletesEvent() throws Exception {
        AdminActivity activity = buildActivity();
        EventRepository repository = mock(EventRepository.class);
        setField(activity, "eventRepository", repository);
        invokePrivate(activity, "populateForm", Event.class, sampleEvent());
        FirebaseUser user = mock(FirebaseUser.class);
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        AuthCredential credential = mock(AuthCredential.class);

        when(user.getEmail()).thenReturn("user@test.com");
        when(user.reauthenticate(credential)).thenReturn(Tasks.forResult(null));
        when(firebaseAuth.getCurrentUser()).thenReturn(user);
        doAnswer(invocation -> {
            EventRepository.MessageCallback callback = invocation.getArgument(1);
            callback.onSuccess("deleted");
            return null;
        }).when(repository).deleteEvent(eq("event-7"), any(EventRepository.MessageCallback.class));

        try (var auth = mockStatic(FirebaseAuth.class);
             var emailAuth = mockStatic(com.google.firebase.auth.EmailAuthProvider.class)) {
            auth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            emailAuth.when(() -> com.google.firebase.auth.EmailAuthProvider.getCredential("user@test.com", "secret"))
                    .thenReturn(credential);

            invokePrivate(activity, "verifyDeletionPasswordAndDelete", String.class, String.class, "event-7", "secret");
            shadowOf(Looper.getMainLooper()).idle();
        }

        assertTrue(activity.isFinishing());
    }

    @Test
    public void applyExistingDateTime_withEmptyFieldLeavesCalendarUnchanged() throws Exception {
        AdminActivity activity = buildActivity();
        Calendar calendar = Calendar.getInstance();
        calendar.set(2030, Calendar.JANUARY, 1, 0, 0, 0);
        ((EditText) activity.findViewById(R.id.dateInput)).setText("");

        invokePrivate(activity, "applyExistingDateTime", Calendar.class, calendar);

        assertEquals(2030, calendar.get(Calendar.YEAR));
    }

    @Test
    public void showSuccess_clearsExistingFeedbackText() throws Exception {
        AdminActivity activity = buildActivity();
        ((TextView) activity.findViewById(R.id.feedbackText)).setText("Old error");

        invokePrivate(activity, "showSuccess", String.class, "ok");

        assertEquals("", ((TextView) activity.findViewById(R.id.feedbackText)).getText().toString());
    }

    private AdminActivity buildActivity() {
        return Robolectric.buildActivity(AdminActivity.class).setup().get();
    }

    private Event sampleEvent() {
        return new Event(
                "event-7",
                "Jazz Night",
                "Live music",
                "Montreal",
                new Date(1775452380000L),
                200,
                40,
                EventCategory.CONCERT,
                EventStatus.AVAILABLE
        );
    }

    private void fillValidForm(AdminActivity activity) {
        ((EditText) activity.findViewById(R.id.titleInput)).setText("Britney Spears");
        ((EditText) activity.findViewById(R.id.descriptionInput)).setText("Britney in Toronto!");
        ((EditText) activity.findViewById(R.id.locationInput)).setText("Toronto");
        ((EditText) activity.findViewById(R.id.dateInput)).setText("2026-04-05 19:13");
        ((EditText) activity.findViewById(R.id.totalSeatsInput)).setText("120");
        ((EditText) activity.findViewById(R.id.availableSeatsInput)).setText("120");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Object getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private Object invokePrivate(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(target, argument);
    }

    private Object invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Object invokePrivate(
            Object target,
            String methodName,
            Class<?> firstType,
            Class<?> secondType,
            Object firstArgument,
            Object secondArgument
    ) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, firstType, secondType);
        method.setAccessible(true);
        return method.invoke(target, firstArgument, secondArgument);
    }
}
