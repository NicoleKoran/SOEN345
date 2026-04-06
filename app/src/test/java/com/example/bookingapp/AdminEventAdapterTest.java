package com.example.bookingapp.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import android.graphics.Color;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.example.bookingapp.Event;
import com.example.bookingapp.EventCategory;
import com.example.bookingapp.EventStatus;
import com.example.bookingapp.R;
import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class AdminEventAdapterTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void setEvents_updatesItemCount() {
        AdminEventAdapter adapter = new AdminEventAdapter(event -> {});

        adapter.setEvents(List.of(
                createEvent("1", "Jazz Night", EventCategory.CONCERT, EventStatus.AVAILABLE),
                createEvent("2", "Movie Night", EventCategory.MOVIE, EventStatus.SOLDOUT)
        ));

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void onBindViewHolder_bindsEventFieldsAndInvokesEditAction() {
        AtomicReference<Event> editedEvent = new AtomicReference<>();
        AdminEventAdapter adapter = new AdminEventAdapter(editedEvent::set);
        Event event = createEvent("1", "Jazz Night", EventCategory.CONCERT, EventStatus.AVAILABLE);
        adapter.setEvents(List.of(event));

        android.view.ViewGroup parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext()
        );
        AdminEventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Jazz Night", ((TextView) holder.itemView.findViewById(R.id.titleText)).getText().toString());
        assertEquals("Montreal", ((TextView) holder.itemView.findViewById(R.id.locationText)).getText().toString());
        assertEquals("CONCERT", ((TextView) holder.itemView.findViewById(R.id.categoryBadge)).getText().toString());
        assertEquals("40/200 seats", ((TextView) holder.itemView.findViewById(R.id.seatsText)).getText().toString());
        assertEquals("available", ((TextView) holder.itemView.findViewById(R.id.statusText)).getText().toString());
        assertTrue(((TextView) holder.itemView.findViewById(R.id.dateText)).getText().toString().contains("Apr"));

        Button bookButton = holder.itemView.findViewById(R.id.bookButton);
        assertFalse(bookButton.isEnabled());

        holder.itemView.findViewById(R.id.editButton).performClick();
        assertEquals(event, editedEvent.get());
    }

    @Test
    public void getCategoryColor_returnsExpectedPalette() throws Exception {
        AdminEventAdapter adapter = new AdminEventAdapter(event -> {});

        assertEquals(Color.parseColor("#6A1B9A"), invokeCategoryColor(adapter, EventCategory.CONCERT));
        assertEquals(Color.parseColor("#1565C0"), invokeCategoryColor(adapter, EventCategory.MOVIE));
        assertEquals(Color.parseColor("#E65100"), invokeCategoryColor(adapter, EventCategory.SPORT));
        assertEquals(Color.parseColor("#2E7D32"), invokeCategoryColor(adapter, EventCategory.TRAVEL));
        assertEquals(Color.parseColor("#3949AB"), invokeCategoryColor(adapter, null));
    }

    @Test
    public void onCreateViewHolder_inflatesAdminEventCard() {
        AdminEventAdapter adapter = new AdminEventAdapter(event -> {});
        android.view.ViewGroup parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext()
        );

        AdminEventAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull(holder.itemView.findViewById(R.id.titleText));
        assertNotNull(holder.itemView.findViewById(R.id.editButton));
    }

    private Event createEvent(String id, String title, EventCategory category, EventStatus status) {
        return new Event(
                id,
                title,
                "Live experience",
                "Montreal",
                new Date(1775452380000L),
                200,
                40,
                category,
                status
        );
    }

    private int invokeCategoryColor(AdminEventAdapter adapter, EventCategory category) throws Exception {
        Method method = AdminEventAdapter.class.getDeclaredMethod("getCategoryColor", EventCategory.class);
        method.setAccessible(true);
        return (int) method.invoke(adapter, category);
    }
}
