package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
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
        assertTrue(!preferences.getBoolean(LoginActivity.KEY_ADMIN_MODE, false));
    }
}
