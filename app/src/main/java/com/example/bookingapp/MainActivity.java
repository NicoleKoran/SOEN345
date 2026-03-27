package com.example.bookingapp;

import android.app.DatePickerDialog;
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
import com.example.bookingapp.models.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EventAdapter adapter;
    List<Event> allEvents = new ArrayList<>();
    List<Event> filteredEvents = new ArrayList<>();
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    EditText searchInput;
    LinearLayout filterChipsContainer;
    Button btnDateFilter, btnLocationFilter, btnClearFilters;
    TextView resultsCount;

    String selectedCategory = null;
    Date selectedDate = null;
    String selectedLocation = null;

    final String[] CATEGORIES = {"All", "Concert", "Movie", "Sports", "Travel"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.eventsRecyclerView);
        searchInput = findViewById(R.id.searchInput);
        filterChipsContainer = findViewById(R.id.filterChipsContainer);
        btnDateFilter = findViewById(R.id.btnDateFilter);
        btnLocationFilter = findViewById(R.id.btnLocationFilter);
        btnClearFilters = findViewById(R.id.btnClearFilters);
        resultsCount = findViewById(R.id.resultsCount);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(filteredEvents, event -> {
            Toast.makeText(this, "Booking: " + event.getTitle(), Toast.LENGTH_SHORT).show();
        });
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
                        || (e.getCategory() != null && e.getCategory().name().toLowerCase().contains(query));
                if (!matchesSearch) continue;
            }
            // Category filter
            if (selectedCategory != null && e.getCategory() != null &&
                    !selectedCategory.equalsIgnoreCase(e.getCategory().name())) continue;
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
                Event event = doc.toObject(Event.class);
                if (event != null) allEvents.add(event);
            }
            applyFilters(); 
        });
    }
}