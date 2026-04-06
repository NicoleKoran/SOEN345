package com.example.bookingapp.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import com.example.bookingapp.Event;
import com.example.bookingapp.EventCategory;
import com.example.bookingapp.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class EventAdapterTest {

    @Test
    public void onBindViewHolder_nonAdminShowsBookAndHidesEdit() {
        AtomicReference<Event> booked = new AtomicReference<>();
        Event event = createEvent("concert");
        EventAdapter adapter = new EventAdapter(List.of(event), booked::set, ignored -> {}, false);
        android.view.ViewGroup parent = new android.widget.FrameLayout(ApplicationProvider.getApplicationContext());
        EventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals(android.view.View.VISIBLE, holder.bookButton.getVisibility());
        assertEquals(android.view.View.GONE, holder.editEventButton.getVisibility());
        assertTrue(holder.location.getText().toString().contains("Montreal"));
        assertTrue(holder.date.getText().toString().contains("🗓"));
        assertEquals("CONCERT", holder.categoryBadge.getText().toString());
        assertEquals("40 seats left", holder.priceText.getText().toString());

        holder.bookButton.performClick();
        assertEquals(event, booked.get());
    }

    @Test
    public void onBindViewHolder_adminShowsEditAndHidesBook() {
        AtomicReference<Event> edited = new AtomicReference<>();
        Event event = createEvent("movie");
        EventAdapter adapter = new EventAdapter(List.of(event), ignored -> {}, edited::set, true);
        android.view.ViewGroup parent = new android.widget.FrameLayout(ApplicationProvider.getApplicationContext());
        EventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals(android.view.View.GONE, holder.bookButton.getVisibility());
        assertEquals(android.view.View.VISIBLE, holder.editEventButton.getVisibility());

        holder.editEventButton.performClick();
        assertEquals(event, edited.get());
    }

    @Test
    public void onBindViewHolder_withUnknownCategoryUsesFallbackBadge() {
        Event event = createEvent(null);
        EventAdapter adapter = new EventAdapter(List.of(event), ignored -> {}, ignored -> {}, false);
        android.view.ViewGroup parent = new android.widget.FrameLayout(ApplicationProvider.getApplicationContext());
        EventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("EVENT", holder.categoryBadge.getText().toString());
    }

    @Test
    public void setEvents_updatesItemCount() {
        EventAdapter adapter = new EventAdapter(List.of(), ignored -> {}, ignored -> {}, false);

        adapter.setEvents(List.of(createEvent("sport"), createEvent("travel")));

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void getCategoryColor_returnsExpectedColors() throws Exception {
        EventAdapter adapter = new EventAdapter(List.of(), ignored -> {}, ignored -> {}, false);

        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, "concert"));
        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, "movie"));
        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, "sport"));
        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, "travel"));
        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, null));
        assertNotNull(invokePrivate(adapter, "getCategoryColor", String.class, "unknown"));
    }

    private Event createEvent(String category) {
        Event event = new Event();
        event.setEventId("event-1");
        event.setTitle("Jazz Night");
        event.setLocation("Montreal");
        event.setDate(new Date(1775452380000L));
        event.setAvailableSeats(40);
        event.setCategory(category == null ? null : EventCategory.fromValue(category));
        return event;
    }

    private Object invokePrivate(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(target, argument);
    }
}
