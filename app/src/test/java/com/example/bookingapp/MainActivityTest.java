package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MainActivityTest {

    private MainActivity activity;

    @Before
    public void setUp() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
        activity = Robolectric.buildActivity(MainActivity.class).create().get();
    }

    @Test
    public void onStart_withoutUserAndAdminSession_navigatesToLogin() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void onCreate_adminMode_showsAddEventButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        Button addEventButton = activity.findViewById(R.id.addEventBtn);

        assertEquals(View.VISIBLE, addEventButton.getVisibility());
    }

    @Test
    public void addEventButton_adminMode_opensAdminActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.findViewById(R.id.addEventBtn).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(AdminActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void logout_clearsAdminSessionAndNavigatesToLogin() {
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().putBoolean(LoginActivity.KEY_ADMIN_MODE, true).apply();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);

        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.findViewById(R.id.logoutBtn).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
        assertFalse(preferences.getBoolean(LoginActivity.KEY_ADMIN_MODE, false));
    }

    @Test
    public void onCreate_nonAdmin_hidesAddEventButton() {
        Button addEventButton = activity.findViewById(R.id.addEventBtn);
        assertEquals(View.GONE, addEventButton.getVisibility());
    }

    @Test
    public void applyFilters_filtersBySearchCategoryDateAndLocation() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        Event matching = createListEvent("1", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5));
        Event wrongLocation = createListEvent("2", "Jazz Night", "Toronto", "concert", dateAt(2026, Calendar.APRIL, 5));
        Event wrongCategory = createListEvent("3", "Jazz Night", "Montreal", "movie", dateAt(2026, Calendar.APRIL, 5));
        Event wrongDate = createListEvent("4", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 6));

        activity.allEvents = new ArrayList<>();
        activity.filteredEvents = new ArrayList<>();
        activity.allEvents.add(matching);
        activity.allEvents.add(wrongLocation);
        activity.allEvents.add(wrongCategory);
        activity.allEvents.add(wrongDate);

        ((EditText) activity.findViewById(R.id.searchInput)).setText("jazz");
        activity.selectedCategory = "concert";
        activity.selectedLocation = "Montreal";
        activity.selectedDate = dateAt(2026, Calendar.APRIL, 5);

        invokePrivate(activity, "applyFilters");

        assertEquals(1, activity.filteredEvents.size());
        assertEquals("1", activity.filteredEvents.get(0).getEventId());
        assertEquals("1 event found", activity.resultsCount.getText().toString());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.btnClearFilters).getVisibility());
    }

    @Test
    public void clearFiltersButton_resetsSelectionsAndHidesButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        ((EditText) activity.findViewById(R.id.searchInput)).setText("concert");
        activity.selectedCategory = "concert";
        activity.selectedLocation = "Montreal";
        activity.selectedDate = dateAt(2026, Calendar.APRIL, 5);
        activity.btnDateFilter.setText("2026-4-5");
        activity.btnLocationFilter.setText("Montreal");
        activity.btnClearFilters.setVisibility(View.VISIBLE);

        activity.findViewById(R.id.btnClearFilters).performClick();

        assertEquals("", activity.searchInput.getText().toString());
        assertNull(activity.selectedCategory);
        assertNull(activity.selectedLocation);
        assertNull(activity.selectedDate);
        assertEquals("Any Date", activity.btnDateFilter.getText().toString());
        assertEquals("Any Location", activity.btnLocationFilter.getText().toString());
        assertEquals(View.GONE, activity.btnClearFilters.getVisibility());
    }

    @Test
    public void toListEvent_mapsDocumentFieldsAndParsesStringSeatCounts() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        Date date = new Date(1775452380000L);
        when(doc.getId()).thenReturn("doc-1");
        when(doc.getData()).thenReturn(new java.util.HashMap<String, Object>() {{
            put("title", "Movie Night");
            put("description", "Marvel marathon");
            put("location", "Montreal");
            put("category", "movie");
            put("status", "available");
            put("date", new Timestamp(date));
            put("totalSeats", "120");
            put("availableSeats", 75L);
        }});

        Event event = (Event) invokePrivate(activity, "toListEvent", DocumentSnapshot.class, doc);

        assertEquals("doc-1", event.getEventId());
        assertEquals("Movie Night", event.getTitle());
        assertEquals("Montreal", event.getLocation());
        assertEquals(EventCategory.MOVIE, event.getCategory());
        assertEquals(EventStatus.AVAILABLE, event.getStatus());
        assertEquals(120, event.getTotalSeats());
        assertEquals(75, event.getAvailableSeats());
        assertEquals(date, event.getDate());
    }

    @Test
    public void showPendingDeleteDialogIfNeeded_displaysDialogAndClearsPreference() throws Exception {
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().putString(MainActivity.KEY_PENDING_DELETE_EVENT_NAME, "Jazz Night").apply();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        invokePrivate(activity, "showPendingDeleteDialogIfNeeded");

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals(activity.getString(R.string.event_deleted_title), shadowOf(dialog).getTitle().toString());
        assertTrue(shadowOf(dialog).getMessage().toString().contains("Jazz Night"));
        assertFalse(preferences.contains(MainActivity.KEY_PENDING_DELETE_EVENT_NAME));
    }

    @Test
    public void nonAdminEventCard_bookButtonLaunchesBookingActivity() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
        Event event = createListEvent("1", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5));
        event.setAvailableSeats(40);
        activity.filteredEvents.clear();
        activity.filteredEvents.add(event);
        activity.adapter.setEvents(activity.filteredEvents);

        android.widget.FrameLayout parent = new android.widget.FrameLayout(activity);
        com.example.bookingapp.adapters.EventAdapter.ViewHolder holder =
                activity.adapter.onCreateViewHolder(parent, 0);
        activity.adapter.onBindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.bookButton).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(BookingActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("Jazz Night", next.getStringExtra("eventTitle"));
        assertEquals("Montreal", next.getStringExtra("eventLocation"));
        assertEquals("1", next.getStringExtra("eventId"));
    }

    @Test
    public void adminEventCard_editButtonOpensAdminActivityWithEventId() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        Event event = createListEvent("event-7", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5));
        activity.filteredEvents.clear();
        activity.filteredEvents.add(event);
        activity.adapter.setEvents(activity.filteredEvents);

        android.widget.FrameLayout parent = new android.widget.FrameLayout(activity);
        com.example.bookingapp.adapters.EventAdapter.ViewHolder holder = activity.adapter.onCreateViewHolder(parent, 0);
        activity.adapter.onBindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.editEventButton).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(AdminActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("event-7", next.getStringExtra("event_id"));
    }

    @Test
    public void categoryChipClick_updatesSelectionAndFiltersResults() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.allEvents = new ArrayList<>();
        activity.filteredEvents = new ArrayList<>();
        Event concert = createListEvent("1", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5));
        Event movie = createListEvent("2", "Movie Night", "Montreal", "movie", dateAt(2026, Calendar.APRIL, 5));
        activity.allEvents.add(concert);
        activity.allEvents.add(movie);

        Button concertChip = (Button) activity.filterChipsContainer.getChildAt(1);
        concertChip.performClick();

        assertEquals("concert", activity.selectedCategory);
        assertEquals(1, activity.filteredEvents.size());
        assertEquals("1", activity.filteredEvents.get(0).getEventId());
    }

    @Test
    public void dateFilterDialog_setsDateAndShowsClearButton() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        activity.btnDateFilter.performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(dialog instanceof DatePickerDialog);
        DatePickerDialog.OnDateSetListener listener =
                (DatePickerDialog.OnDateSetListener) readField(dialog, "mDateSetListener");
        listener.onDateSet(null, 2026, Calendar.APRIL, 5);

        assertEquals("2026-4-5", activity.btnDateFilter.getText().toString());
        assertEquals(View.VISIBLE, activity.btnClearFilters.getVisibility());
        assertNotNull(activity.selectedDate);
    }

    @Test
    public void locationFilterDialog_selectsLocationAndCanResetToAnyLocation() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.allEvents = new ArrayList<>();
        activity.allEvents.add(createListEvent("1", "Jazz Night", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5)));
        activity.allEvents.add(createListEvent("2", "Movie Night", "Toronto", "movie", dateAt(2026, Calendar.APRIL, 5)));

        activity.btnLocationFilter.performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        shadowOf(dialog).clickOnItem(1);

        assertEquals("Montreal", activity.selectedLocation);
        assertEquals("Montreal", activity.btnLocationFilter.getText().toString());
        assertEquals(View.VISIBLE, activity.btnClearFilters.getVisibility());

        activity.btnLocationFilter.performClick();
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        shadowOf(dialog).clickOnItem(0);

        assertNull(activity.selectedLocation);
        assertEquals("Any Location", activity.btnLocationFilter.getText().toString());
    }

    @Test
    public void applyFilters_matchesLocationAndCategorySearchBranches() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        Event byLocation = createListEvent("1", "Something", "Montreal", "concert", dateAt(2026, Calendar.APRIL, 5));
        Event byCategory = createListEvent("2", "Else", "Toronto", "movie", dateAt(2026, Calendar.APRIL, 5));
        activity.allEvents = new ArrayList<>();
        activity.filteredEvents = new ArrayList<>();
        activity.allEvents.add(byLocation);
        activity.allEvents.add(byCategory);

        activity.searchInput.setText("montreal");
        invokePrivate(activity, "applyFilters");
        assertEquals(1, activity.filteredEvents.size());
        assertEquals("1", activity.filteredEvents.get(0).getEventId());

        activity.searchInput.setText("movie");
        invokePrivate(activity, "applyFilters");
        assertEquals(1, activity.filteredEvents.size());
        assertEquals("2", activity.filteredEvents.get(0).getEventId());
    }

    @Test
    public void showPendingDeleteDialogIfNeeded_withoutPendingDeleteDoesNothing() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        invokePrivate(activity, "showPendingDeleteDialogIfNeeded");

        assertNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void toListEvent_withNullDataReturnsNull() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getData()).thenReturn(null);

        Object result = invokePrivate(activity, "toListEvent", DocumentSnapshot.class, doc);

        assertNull(result);
    }

    @Test
    public void readInt_handlesInvalidStringAndNull() throws Exception {
        assertEquals(0, invokePrivate(activity, "readInt", Object.class, "abc"));
        assertEquals(0, invokePrivate(activity, "readInt", Object.class, null));
    }

    @Test
    public void readDate_returnsFallbackForInvalidValue() throws Exception {
        Object value = invokePrivate(activity, "readDate", Object.class, "invalid");
        assertTrue(value instanceof Date);
    }

    @Test
    public void resolveAdminMode_usesPersistedAdminPreference() throws Exception {
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().putBoolean(LoginActivity.KEY_ADMIN_MODE, true).apply();
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();

        boolean admin = (boolean) invokePrivate(activity, "resolveAdminMode");

        assertTrue(admin);
    }

    @Test
    public void resolveAdminMode_usesFirebaseAdminUser() throws Exception {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getEmail()).thenReturn(LoginActivity.ADMIN_EMAIL);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        when(auth.getCurrentUser()).thenReturn(user);

        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class)) {
            authMock.when(FirebaseAuth::getInstance).thenReturn(auth);
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();

            boolean admin = (boolean) invokePrivate(activity, "resolveAdminMode");

            assertTrue(admin);
        }
    }

    @Test
    public void clearFiltersButtonHiddenByDefaultTest() {
        Button clearBtn = activity.findViewById(R.id.btnClearFilters);
        assertEquals(View.GONE, clearBtn.getVisibility());
    }

    @Test
    public void searchInputIsEmptyByDefaultTest() {
        EditText search = activity.findViewById(R.id.searchInput);
        assertEquals("", search.getText().toString());
    }

    @Test
    public void dateFilterButtonshowsDefaultText() {
        Button dateBtn = activity.findViewById(R.id.btnDateFilter);
        assertEquals("Any Date", dateBtn.getText().toString());
    }

    @Test
    public void locationFilterButtonshowsDefaultText() {
        Button locationBtn = activity.findViewById(R.id.btnLocationFilter);
        assertEquals("Any Location", locationBtn.getText().toString());
    }

    @Test
    public void typingInSearchMakesClearFiltersVisibleTest() {
        EditText search = activity.findViewById(R.id.searchInput);
        search.setText("jazz");
        assertEquals(View.VISIBLE, activity.findViewById(R.id.btnClearFilters).getVisibility());
    }

    @Test
    public void clearFiltersHidesItselfTest() {
        ((EditText) activity.findViewById(R.id.searchInput)).setText("x");
        Button clearBtn = activity.findViewById(R.id.btnClearFilters);
        clearBtn.performClick();
        assertEquals(View.GONE, clearBtn.getVisibility());
    }

    @Test
    public void clearFiltersResetsDateButtonText() {
        Button dateBtn = activity.findViewById(R.id.btnDateFilter);
        Button clearBtn = activity.findViewById(R.id.btnClearFilters);
        dateBtn.setText("2025-6-15");
        clearBtn.setVisibility(View.VISIBLE);
        clearBtn.performClick();
        assertEquals("Any Date", dateBtn.getText().toString());
    }

    @Test
    public void clearFiltersResetsLocationButtonText() {
        Button locationBtn = activity.findViewById(R.id.btnLocationFilter);
        Button clearBtn = activity.findViewById(R.id.btnClearFilters);
        locationBtn.setText("Montreal");
        clearBtn.setVisibility(View.VISIBLE);
        clearBtn.performClick();
        assertEquals("Any Location", locationBtn.getText().toString());
    }

    @Test
    public void readString_returnsFallbackWhenValueIsNull() throws Exception {
        assertEquals("fallback", invokePrivate(activity, "readString", Object.class, String.class, null, "fallback"));
    }

    @Test
    public void readInt_numericString_returnsParsedValue() throws Exception {
        assertEquals(42, invokePrivate(activity, "readInt", Object.class, "42"));
    }

    @Test
    public void readDate_javaUtilDate_returnsSameInstance() throws Exception {
        Date d = new Date(999888777L);
        assertEquals(d, invokePrivate(activity, "readDate", Object.class, d));
    }

    @Test
    public void nonAdminBookButton_usesDateTbdWhenEventDateNull() {
        Event event = createListEvent("1", "No Date Show", "Montreal", "concert", null);
        event.setAvailableSeats(12);
        activity.filteredEvents.clear();
        activity.filteredEvents.add(event);
        activity.adapter.setEvents(activity.filteredEvents);

        android.widget.FrameLayout parent = new android.widget.FrameLayout(activity);
        com.example.bookingapp.adapters.EventAdapter.ViewHolder holder =
                activity.adapter.onCreateViewHolder(parent, 0);
        activity.adapter.onBindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.bookButton).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertTrue(next.getStringExtra("eventDate").contains("Date TBD"));
    }

    @Test
    public void applyFilters_zeroResults_usesPluralEventsFound() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.allEvents = new ArrayList<>();
        activity.filteredEvents = new ArrayList<>();
        invokePrivate(activity, "applyFilters");
        assertEquals("0 events found", activity.resultsCount.getText().toString());
    }

    @Test
    public void showPendingDeleteDialogIfNeeded_blankString_doesNothing() throws Exception {
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().putString(MainActivity.KEY_PENDING_DELETE_EVENT_NAME, "   ").apply();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();

        invokePrivate(activity, "showPendingDeleteDialogIfNeeded");

        assertNull(ShadowAlertDialog.getLatestAlertDialog());
        assertEquals("   ", preferences.getString(MainActivity.KEY_PENDING_DELETE_EVENT_NAME, ""));
    }

    @Test
    public void categoryAllChip_resetsSelectedCategoryToNull() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);
        MainActivity activity = Robolectric.buildActivity(MainActivity.class, intent).setup().get();
        activity.selectedCategory = "concert";

        Button allChip = (Button) activity.filterChipsContainer.getChildAt(0);
        allChip.performClick();

        assertNull(activity.selectedCategory);
    }

    private Event createListEvent(
            String id,
            String title,
            String location,
            String category,
            Date date
    ) {
        Event event = new Event();
        event.setEventId(id);
        event.setTitle(title);
        event.setLocation(location);
        event.setCategory(EventCategory.fromValue(category));
        event.setDate(date);
        return event;
    }

    private Date dateAt(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Object invokePrivate(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private Object invokePrivate(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(target, argument);
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

    private Object readField(Object target, String fieldName) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
        throw new AssertionError("Field not found: " + fieldName);
    }
}
