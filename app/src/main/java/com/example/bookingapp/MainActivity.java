package com.example.bookingapp;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingapp.adapters.EventAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_IS_ADMIN = "extra_is_admin";
    public static final String KEY_PENDING_DELETE_EVENT_NAME = "pending_delete_event_name";

    /**
     * Set to {@code true} in instrumented tests to suppress the onStart redirect to
     * LoginActivity when there is no signed-in user.  Must be reset to {@code false}
     * in {@code @After} to avoid polluting other tests.
     */
    public static boolean skipRedirectForTesting = false;

    RecyclerView recyclerView;
    EventAdapter adapter;
    List<Event> allEvents = new ArrayList<>();
    List<Event> filteredEvents = new ArrayList<>();
    FirebaseFirestore db;

    EditText searchInput;
    LinearLayout filterChipsContainer;
    Button btnDateFilter, btnLocationFilter, btnClearFilters;
    TextView resultsCount;

    String selectedCategory = null;
    Date selectedDate = null;
    String selectedLocation = null;
    boolean isAdmin;

    final String[] CATEGORIES = {"All", "concert", "movie", "sport", "travel"};

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!skipRedirectForTesting && user == null && !isAdminSessionPersisted()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadEvents();
        }
        showPendingDeleteDialogIfNeeded();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_main);
        isAdmin = resolveAdminMode();

        recyclerView = findViewById(R.id.eventsRecyclerView);
        searchInput = findViewById(R.id.searchInput);
        filterChipsContainer = findViewById(R.id.filterChipsContainer);
        btnDateFilter = findViewById(R.id.btnDateFilter);
        btnLocationFilter = findViewById(R.id.btnLocationFilter);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        resultsCount = findViewById(R.id.resultsCount);
        Button addEventBtn = findViewById(R.id.addEventBtn);

        Button logoutBtn = findViewById(R.id.logoutBtn);
        logoutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            clearAdminSession();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // US-10: Show "My Bookings" for non-admin customers
        Button myReservationsBtn = findViewById(R.id.myReservationsBtn);
        if (!isAdmin) {
            myReservationsBtn.setVisibility(View.VISIBLE);
            myReservationsBtn.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, MyReservationsActivity.class)));
        }

        if (isAdmin) {
            addEventBtn.setVisibility(View.VISIBLE);
            addEventBtn.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(
                filteredEvents,
                event -> {
                    // format the event date
                    String formattedDate = "Date TBD";
                    if (event.getDate() != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                "MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault());
                        formattedDate = sdf.format(event.getDate());
                    }

                    // Launch BookingActivity pass all event details as extras
                    // BookingActivity unpacks these to show the booking screen
                    Intent intent = new Intent(MainActivity.this, BookingActivity.class);
                    intent.putExtra("eventId",        event.getEventId());
                    intent.putExtra("eventTitle",     event.getTitle());
                    intent.putExtra("eventLocation",  event.getLocation());
                    intent.putExtra("eventDate",      formattedDate);
                    intent.putExtra("eventPrice",     String.valueOf(event.getTotalSeats()));
                    intent.putExtra("eventStatus",    event.getStatus() != null
                            ? event.getStatus().toFirestoreValue() : "available");
                    intent.putExtra("availableSeats", event.getAvailableSeats());
                    startActivity(intent);
                },
                event -> {
                    // ADMIN edit button
                    Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                    intent.putExtra("event_id", event.getEventId());
                    startActivity(intent);
                },
                isAdmin
        );
        recyclerView.setAdapter(adapter);

        setupCategoryChips();
        setupSearch();
        setupDateFilter();
        setupLocationFilter();
        setupClearFilters();
        loadEvents();
    }

    private void setupCategoryChips() {
        for (String cat : CATEGORIES) {
            Button chip = new Button(this);
            chip.setText(cat);
            chip.setTextSize(12f);
            chip.setPadding(24, 4, 24, 4);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(4, 0, 4, 0);
            chip.setLayoutParams(params);

            updateChipStyle(chip, false);
            chip.setOnClickListener(v -> {
                selectedCategory = cat.equals("All") ? null : cat;
                // Reset all chip styles
                for (int i = 0; i < filterChipsContainer.getChildCount(); i++) {
                    updateChipStyle((Button) filterChipsContainer.getChildAt(i), false);
                }
                updateChipStyle(chip, true);
                applyFilters();
            });

            filterChipsContainer.addView(chip);
        }
        // Set "All" as selected by default
        updateChipStyle((Button) filterChipsContainer.getChildAt(0), true);
    }

    private void updateChipStyle(Button chip, boolean selected) {
        if (selected) {
            chip.setBackgroundTintList(android.content.res.ColorStateList
                    .valueOf(Color.parseColor("#1A1A2E")));
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundTintList(android.content.res.ColorStateList
                    .valueOf(Color.parseColor("#E0E0E0")));
            chip.setTextColor(Color.parseColor("#333333"));
        }
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupDateFilter() {
        btnDateFilter.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                cal.set(year, month, day, 0, 0, 0);
                selectedDate = cal.getTime();
                btnDateFilter.setText(year + "-" + (month + 1) + "-" + day);
                btnClearFilters.setVisibility(View.VISIBLE);
                applyFilters();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupLocationFilter() {
        btnLocationFilter.setOnClickListener(v -> {
            // Build unique location list
            Set<String> locations = new LinkedHashSet<>();
            locations.add("Any Location");
            for (Event e : allEvents) {
                if (e.getLocation() != null) locations.add(e.getLocation());
            }
            String[] locArray = locations.toArray(new String[0]);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Filter by Location")
                    .setItems(locArray, (dialog, which) -> {
                        selectedLocation = which == 0 ? null : locArray[which];
                        btnLocationFilter.setText(which == 0 ? "Any Location" : locArray[which]);
                        if (selectedLocation != null) btnClearFilters.setVisibility(View.VISIBLE);
                        applyFilters();
                    }).show();
        });
    }

    private void setupClearFilters() {
        btnClearFilters.setOnClickListener(v -> {
            selectedCategory = null;
            selectedDate = null;
            selectedLocation = null;
            searchInput.setText("");
            btnDateFilter.setText("Any Date");
            btnLocationFilter.setText("Any Location");
            btnClearFilters.setVisibility(View.GONE);
            // Reset chip selection to "All"
            for (int i = 0; i < filterChipsContainer.getChildCount(); i++) {
                updateChipStyle((Button) filterChipsContainer.getChildAt(i), i == 0);
            }
            applyFilters();
        });
    }

    private void applyFilters() {
        String query = searchInput.getText().toString().toLowerCase().trim();

        List<Event> result = new ArrayList<>();
        for (Event e : allEvents) {
            // Search filter
            if (!query.isEmpty()) {
                boolean matchesSearch = (e.getTitle() != null && e.getTitle().toLowerCase().contains(query))
                        || (e.getLocation() != null && e.getLocation().toLowerCase().contains(query))
                        || (e.getCategory() != null && e.getCategory().toFirestoreValue().toLowerCase().contains(query));
                if (!matchesSearch) continue;
            }
            // Category filter
            if (selectedCategory != null && e.getCategory() != null &&
                    !selectedCategory.equalsIgnoreCase(e.getCategory().toFirestoreValue())) continue;
            if (selectedDate != null && e.getDate() != null) {
                Calendar selCal = Calendar.getInstance(); selCal.setTime(selectedDate);
                Calendar evCal = Calendar.getInstance(); evCal.setTime(e.getDate());
                boolean sameDay = selCal.get(Calendar.YEAR) == evCal.get(Calendar.YEAR)
                        && selCal.get(Calendar.DAY_OF_YEAR) == evCal.get(Calendar.DAY_OF_YEAR);
                if (!sameDay) continue;
            }
            // Location filter
            if (selectedLocation != null && !selectedLocation.equalsIgnoreCase(e.getLocation())) continue;

            result.add(e);
        }

        filteredEvents.clear();
        filteredEvents.addAll(result);
        adapter.setEvents(filteredEvents);
        resultsCount.setText(result.size() + " event" + (result.size() == 1 ? "" : "s") + " found");

        boolean hasActiveFilter = selectedDate != null || selectedLocation != null
                || selectedCategory != null || !query.isEmpty();
        btnClearFilters.setVisibility(hasActiveFilter ? View.VISIBLE : View.GONE);
    }

    private void loadEvents() {
        db.collection("events").get().addOnSuccessListener(snap -> {
            allEvents.clear();
            for (DocumentSnapshot doc : snap) {
                Event event = toListEvent(doc);
                if (event != null) allEvents.add(event);
            }
            applyFilters();
        });
    }

    private Event toListEvent(DocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        if (data == null) {
            return null;
        }

        Event event = new Event();
        event.setEventId(readString(data.get("eventId"), doc.getId()));
        event.setTitle(readString(data.get("title"), ""));
        event.setDescription(readString(data.get("description"), ""));
        event.setLocation(readString(data.get("location"), ""));
        event.setCategory(readString(data.get("category"), "movie"));
        event.setStatus(readString(data.get("status"), "available"));
        event.setDate(readDate(data.get("date")));
        event.setTotalSeats(readInt(data.get("totalSeats")));
        event.setAvailableSeats(readInt(data.get("availableSeats")));
        return event;
    }

    private String readString(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private int readInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? 0 : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Date readDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return new Date();
    }

    private boolean resolveAdminMode() {
        boolean adminFromIntent = getIntent().getBooleanExtra(EXTRA_IS_ADMIN, false);
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        boolean persistedAdmin = preferences.getBoolean(LoginActivity.KEY_ADMIN_MODE, false);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean firebaseAdmin = user != null && LoginActivity.ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
        boolean admin = adminFromIntent || persistedAdmin || firebaseAdmin;
        preferences.edit().putBoolean(LoginActivity.KEY_ADMIN_MODE, admin).apply();
        return admin;
    }

    private boolean isAdminSessionPersisted() {
        return getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                .getBoolean(LoginActivity.KEY_ADMIN_MODE, false);
    }

    private void clearAdminSession() {
        getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.KEY_ADMIN_MODE, false)
                .apply();
    }

    private void showPendingDeleteDialogIfNeeded() {
        SharedPreferences preferences = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        String deletedEventName = preferences.getString(KEY_PENDING_DELETE_EVENT_NAME, null);
        if (deletedEventName == null || deletedEventName.trim().isEmpty()) {
            return;
        }

        preferences.edit().remove(KEY_PENDING_DELETE_EVENT_NAME).apply();
        new AlertDialog.Builder(this)
                .setTitle(R.string.event_deleted_title)
                .setMessage(getString(R.string.event_deleted_message, deletedEventName))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
