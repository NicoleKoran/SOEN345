package com.example.bookingapp;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.TimeUnit;

/**
 * Covers {@link emailNotification} OkHttp callback branches via {@link emailNotification#testEmailEndpointUrl}.
 */
@RunWith(RobolectricTestRunner.class)
public class emailNotificationHttpTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
        server = new MockWebServer();
        server.start();
        emailNotification.testEmailEndpointUrl = server.url("/send").toString();
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() throws Exception {
        emailNotification.testEmailEndpointUrl = null;
        server.shutdown();
    }

    private emailNotification build() {
        return new emailNotification(
                "n1", "to@test.com", "Title", "Loc", "Jan 1", "10", "res-1");
    }

    private static boolean logContains(String tag, String substring) {
        for (ShadowLog.LogItem item : ShadowLog.getLogs()) {
            if (tag.equals(item.tag) && item.msg != null && item.msg.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void sendEmail_http200_invokesSuccessLog() throws Exception {
        ShadowLog.clear();
        server.enqueue(new MockResponse().setResponseCode(200));
        build().sendEmail("msg");
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        Thread.sleep(500);
        assertTrue(logContains("emailNotification", "Email sent successfully"));
    }

    @Test
    public void sendEmail_httpError_invokesErrorLog() throws Exception {
        ShadowLog.clear();
        server.enqueue(new MockResponse().setResponseCode(422).setBody("err"));
        build().sendEmail("msg");
        assertNotNull(server.takeRequest(5, TimeUnit.SECONDS));
        Thread.sleep(500);
        assertTrue(logContains("emailNotification", "EmailJS error"));
    }

    @Test
    public void sendEmail_disconnect_invokesNetworkFailureLog() throws Exception {
        ShadowLog.clear();
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        build().sendEmail("msg");
        Thread.sleep(1500);
        assertTrue(logContains("emailNotification", "Email network failure"));
    }
}
