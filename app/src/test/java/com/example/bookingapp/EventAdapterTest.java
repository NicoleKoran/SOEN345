package com.example.bookingapp;

import android.app.Application;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;

import com.example.bookingapp.adapters.EventAdapter;
import com.example.bookingapp.models.Event;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class EventAdapterTest {

    private EventAdapter adapter;
    private List<Event> events;
    private Event concertEvent;
    private Event movieEvent;
    private boolean bookClickFired;
    private Event lastBookedEvent;

    @Before
    public void setUp() {
        bookClickFired = false;
        lastBookedEvent = null;

        concertEvent = new Event("1", "Jazz Night", new Date(), "Montreal",
                "Jazz concert", Event.EventCategory.concert,
                200, 50, Event.EventStatus.available, 30);

        movieEvent = new Event("2", "Inception", new Date(), "Toronto",
                "Sci-fi film", Event.EventCategory.movie,
                100, 20, Event.EventStatus.available, 15);

        events = new ArrayList<>(Arrays.asList(concertEvent, movieEvent));

        adapter = new EventAdapter(events, event -> {
            bookClickFired = true;
            lastBookedEvent = event;
        });
    }

    // getItemCount() tests
    @Test
    public void getItemCountReturnsCorrectSizeTest() {
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void getItemCountReturnsZeroForEmptyListTest() {
        adapter.setEvents(new ArrayList<>());
        assertEquals(0, adapter.getItemCount());
    }

    // setEvents() tests
    @Test
    public void setEventsUpdatesListAndCountTest() {
        List<Event> newEvents = new ArrayList<>();
        newEvents.add(new Event("3", "Marathon", new Date(), "Quebec City",
                "Running event", Event.EventCategory.sport,
                500, 300, Event.EventStatus.available, 0));

        adapter.setEvents(newEvents);

        assertEquals(1, adapter.getItemCount());
    }

    @Test
    public void setEventsWithEmptyListGivesZeroCount() {
        adapter.setEvents(new ArrayList<>());
        assertEquals(0, adapter.getItemCount());
    }

    private EventAdapter.ViewHolder createBoundViewHolder(int position) {
        Application app = ApplicationProvider.getApplicationContext();
        // Robolectric requires a theme that has the expected attributes
        app.setTheme(androidx.appcompat.R.style.Theme_AppCompat);

        FrameLayout parent = new FrameLayout(app);
        EventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, position);
        return holder;
    }

    //OnBindViewHolder tests
    @Test
    public void setTitleCorrectlyTest() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        assertEquals("Jazz Night", holder.title.getText().toString());
    }

    @Test
    public void setLocationWithIconTest() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        assertTrue(holder.location.getText().toString().contains("Montreal"));
        assertTrue(holder.location.getText().toString().startsWith("📍"));
    }

    @Test
    public void setAvailableSeatsText() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        assertTrue(holder.priceText.getText().toString().contains("50"));
        assertTrue(holder.priceText.getText().toString().contains("seats left"));
    }

    @Test
    public void setCategoryBadgeTextToConcertTest() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        assertEquals("CONCERT", holder.categoryBadge.getText().toString());
    }

    @Test
    public void setCategoryBadgeTextToMovieTest() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(1);
        assertEquals("MOVIE", holder.categoryBadge.getText().toString());
    }

    @Test
    public void displayCalendarIconWithDate() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        assertTrue(holder.date.getText().toString().startsWith("🗓"));
    }

    // Book button tests
    @Test
    public void clickFiresListenerWithCorrectEvent() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);
        holder.bookButton.performClick();

        assertTrue(bookClickFired);
        assertEquals(concertEvent, lastBookedEvent);
    }

    @Test
    public void clickFiresCorrectEventForSecondItem() {
        EventAdapter.ViewHolder holder = createBoundViewHolder(1);
        holder.bookButton.performClick();

        assertTrue(bookClickFired);
        assertEquals(movieEvent, lastBookedEvent);
    }

    @Test
    public void doesNotCrashWhenListenerIsNull() {
        EventAdapter noListenerAdapter = new EventAdapter(events, null);
        FrameLayout parent = new FrameLayout(ApplicationProvider.getApplicationContext());
        EventAdapter.ViewHolder holder = noListenerAdapter.onCreateViewHolder(parent, 0);
        noListenerAdapter.onBindViewHolder(holder, 0);

        holder.bookButton.performClick();
    }


    @Test
    public void nullCategoryShowsDefaultBadge() {
        Event noCategory = new Event();
        noCategory.setTitle("Mystery Event");
        noCategory.setLocation("Somewhere");
        noCategory.setDate(new Date());
        noCategory.setAvailableSeats(10);

        adapter.setEvents(new ArrayList<>(List.of(noCategory)));
        EventAdapter.ViewHolder holder = createBoundViewHolder(0);

        assertEquals("EVENT", holder.categoryBadge.getText().toString());
    }
}