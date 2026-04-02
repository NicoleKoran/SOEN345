package com.example.bookingapp;

import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.Date;

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
        AdminActivity activity = Robolectric.buildActivity(AdminActivity.class).setup().get();
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
}
