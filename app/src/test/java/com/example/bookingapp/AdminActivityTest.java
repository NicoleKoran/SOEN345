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
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
