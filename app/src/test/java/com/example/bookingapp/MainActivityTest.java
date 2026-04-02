package com.example.bookingapp;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MainActivityTest {

    private MainActivity activity;

    private MainActivity buildActivity(MockedStatic<FirebaseAuth> authMock,
                                       MockedStatic<FirebaseFirestore> fsMock,
                                       boolean loggedIn) {
        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        FirebaseUser mockUser = mock(FirebaseUser.class);
        authMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
        when(mockAuth.getCurrentUser()).thenReturn(loggedIn ? mockUser : null);

        FirebaseFirestore mockFs = mock(FirebaseFirestore.class);
        CollectionReference mockCollection = mock(CollectionReference.class);
        Task mockTask = mock(Task.class);
        fsMock.when(FirebaseFirestore::getInstance).thenReturn(mockFs);
        when(mockFs.collection("events")).thenReturn(mockCollection);
        when(mockCollection.get()).thenReturn(mockTask);
        when(mockTask.addOnSuccessListener(any())).thenReturn(mockTask);

        return Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().get();
    }

    @Before
    public void setUp() {
        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> fsMock = mockStatic(FirebaseFirestore.class)) {
            activity = buildActivity(authMock, fsMock, true);
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

    // Search
    @Test
    public void typingInSearchMakesClearFiltersVisibleTest() {
        EditText search = (EditText) activity.findViewById(R.id.searchInput);
        search.setText("jazz");
        assertEquals(View.VISIBLE, activity.findViewById(R.id.btnClearFilters).getVisibility());
    }

    // Clear filters
    @Test
    public void clearFiltersHidesItselfTest() {
        ((EditText) activity.findViewById(R.id.searchInput)).setText("x"); // cast it
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

    // Logout

    @Test
    public void logoutButtonNavigatesToLoginActivity() {
        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            authMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            doNothing().when(mockAuth).signOut();

            activity.findViewById(R.id.logoutBtn).performClick();

            ShadowActivity shadow = Shadows.shadowOf(activity);
            Intent started = shadow.getNextStartedActivity();
            assertNotNull(started);
            assertEquals(LoginActivity.class.getName(),
                    started.getComponent().getClassName());
        }
    }

    // Auth redirect
    @Test
    public void onStartWithNoUserRedirectsToLoginTest() {
        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> fsMock = mockStatic(FirebaseFirestore.class)) {

            MainActivity unauthActivity = buildActivity(authMock, fsMock, false);

            ShadowActivity shadow = Shadows.shadowOf(unauthActivity);
            Intent started = shadow.getNextStartedActivity();
            assertNotNull(started);
            assertEquals(LoginActivity.class.getName(),
                    started.getComponent().getClassName());
        }
    }
}