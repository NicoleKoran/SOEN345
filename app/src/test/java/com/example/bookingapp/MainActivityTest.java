//package com.example.bookingapp;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.mockStatic;
//import static org.mockito.Mockito.when;
//
//import android.widget.Button;
//import android.widget.EditText;
//import android.os.Looper;
//import android.widget.TextView;
//
//import androidx.test.core.app.ApplicationProvider;
//
//import com.google.android.gms.tasks.Tasks;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.MockedStatic;
//import org.robolectric.Robolectric;
//import org.robolectric.RobolectricTestRunner;
//import org.robolectric.Shadows;
//
//@RunWith(RobolectricTestRunner.class)
//public class MainActivityTest {
//
//    @Before
//    public void setUp() {
//        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
//    }
//
//    @Test
//    public void calculate_emptyInputs_showsInvalidInputMessage() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            stubEntriesReference(db);
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            Button calculateBtn = activity.findViewById(R.id.calculateBtn);
//            calculateBtn.performClick();
//            TextView result = activity.findViewById(R.id.resultField);
//            assertEquals("Wrong input: can't be null or non-numeric", result.getText().toString());
//        }
//    }
//
//    @Test
//    public void calculate_validNumbers_showsSum() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            stubEntriesReference(db);
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            ((EditText) activity.findViewById(R.id.input1)).setText("2");
//            ((EditText) activity.findViewById(R.id.input2)).setText("3");
//            activity.findViewById(R.id.calculateBtn).performClick();
//            assertEquals("= 5", ((TextView) activity.findViewById(R.id.resultField)).getText().toString());
//        }
//    }
//
//    @Test
//    public void calculate_nonNumeric_showsError() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            stubEntriesReference(db);
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            ((EditText) activity.findViewById(R.id.input1)).setText("x");
//            ((EditText) activity.findViewById(R.id.input2)).setText("1");
//            activity.findViewById(R.id.calculateBtn).performClick();
//            assertEquals(
//                    "Wrong input: can't be null or non-numeric",
//                    ((TextView) activity.findViewById(R.id.resultField)).getText().toString());
//        }
//    }
//
//    @Test
//    public void database_emptyInput_setsError() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            stubEntriesReference(db);
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            EditText databaseInput = activity.findViewById(R.id.databaseinput);
//            databaseInput.setText("");
//            activity.findViewById(R.id.databasebutton).performClick();
//            assertEquals("Field can't be empty", databaseInput.getError().toString());
//        }
//    }
//
//    @Test
//    public void database_failure_setsError() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            FirebaseDatabase mockDb = mock(FirebaseDatabase.class);
//            DatabaseReference rootRef = mock(DatabaseReference.class);
//            DatabaseReference pushedRef = mock(DatabaseReference.class);
//            db.when(FirebaseDatabase::getInstance).thenReturn(mockDb);
//            when(mockDb.getReference("entries")).thenReturn(rootRef);
//            when(rootRef.push()).thenReturn(pushedRef);
//            when(pushedRef.setValue(anyString()))
//                    .thenReturn(Tasks.forException(new Exception("write denied")));
//
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            EditText databaseInput = activity.findViewById(R.id.databaseinput);
//            databaseInput.setText("value");
//            activity.findViewById(R.id.databasebutton).performClick();
//            Shadows.shadowOf(Looper.getMainLooper()).idle();
//            assertTrue(databaseInput.getError().toString().contains("Failed"));
//        }
//    }
//
//    @Test
//    public void database_success_clearsInput() {
//        try (MockedStatic<FirebaseDatabase> db = mockStatic(FirebaseDatabase.class)) {
//            stubEntriesReference(db);
//            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
//            EditText databaseInput = activity.findViewById(R.id.databaseinput);
//            databaseInput.setText("hello");
//            activity.findViewById(R.id.databasebutton).performClick();
//            Shadows.shadowOf(Looper.getMainLooper()).idle();
//            assertTrue(databaseInput.getText().toString().isEmpty());
//        }
//    }
//
//    private static void stubEntriesReference(MockedStatic<FirebaseDatabase> dbStatic) {
//        FirebaseDatabase mockDb = mock(FirebaseDatabase.class);
//        DatabaseReference rootRef = mock(DatabaseReference.class);
//        DatabaseReference pushedRef = mock(DatabaseReference.class);
//        dbStatic.when(FirebaseDatabase::getInstance).thenReturn(mockDb);
//        when(mockDb.getReference("entries")).thenReturn(rootRef);
//        when(rootRef.push()).thenReturn(pushedRef);
//        when(pushedRef.setValue(anyString())).thenReturn(Tasks.forResult(null));
//    }
//}
