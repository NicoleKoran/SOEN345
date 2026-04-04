package com.example.bookingapp;

import static org.junit.Assert.*;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BookingConfirmedActivityTest {

    @Before
    public void initFirebase() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
    }

    private Intent buildFullIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                BookingConfirmedActivity.class
        );
        intent.putExtra("reservationId", "res-123");
        intent.putExtra("eventTitle",    "Rock Concert");
        intent.putExtra("eventLocation", "Montreal");
        intent.putExtra("eventDate",     "May 10, 2026");
        intent.putExtra("price",         "75");
        intent.putExtra("ticketId",      "TKT-ABCD");
        intent.putExtra("seatNumber",    "AUTO-42");
        return intent;
    }

    @Test
    public void onCreate_populatesEventTitle() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView title = activity.findViewById(R.id.confirmedEventTitle);
        assertEquals("Rock Concert", title.getText().toString());
    }

    @Test
    public void onCreate_populatesLocationWithIcon() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView location = activity.findViewById(R.id.confirmedLocation);
        assertTrue(location.getText().toString().contains("Montreal"));
    }

    @Test
    public void onCreate_populatesDateWithIcon() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView date = activity.findViewById(R.id.confirmedDate);
        assertTrue(date.getText().toString().contains("May 10, 2026"));
    }

    @Test
    public void onCreate_populatesPriceWithIcon() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView price = activity.findViewById(R.id.confirmedPrice);
        assertTrue(price.getText().toString().contains("75"));
    }

    @Test
    public void onCreate_populatesReservationId() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView resId = activity.findViewById(R.id.confirmedReservationId);
        assertTrue(resId.getText().toString().contains("res-123"));
    }

    @Test
    public void onCreate_populatesTicketId() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView ticketId = activity.findViewById(R.id.confirmedTicketId);
        assertTrue(ticketId.getText().toString().contains("TKT-ABCD"));
    }

    @Test
    public void onCreate_populatesSeatNumber() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        TextView seat = activity.findViewById(R.id.confirmedSeatNumber);
        assertTrue(seat.getText().toString().contains("AUTO-42"));
    }

    @Test
    public void goHomeBtn_click_startsMainActivity() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        Button goHomeBtn = activity.findViewById(R.id.goHomeBtn);
        goHomeBtn.performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void goHomeBtn_click_finishesActivity() {
        BookingConfirmedActivity activity =
                Robolectric.buildActivity(BookingConfirmedActivity.class, buildFullIntent())
                        .setup().get();

        Button goHomeBtn = activity.findViewById(R.id.goHomeBtn);
        goHomeBtn.performClick();

        assertTrue(activity.isFinishing());
    }
}
