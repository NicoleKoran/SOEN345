package com.example.bookingapp;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**important imports, some are for the email notifier EmailJS**/
/**
 * Concrete Observer from the UML diagram.
 *
 * Implements NotificationListener so it can be attached to a Reservation.
 * When the Reservation calls notifyObservers(), this class receives the
 * update() call and triggers sendEmail() via EmailJS.
 *
 * UML: emailNotification
 *   - notificationId: String
 *   - message: String
 *   - sentDate: Date
 *   + sendEmail(message: String): void
 *   + update(message: String): void        ← called by Reservation
 */
public class emailNotification implements NotificationListener {

    private static final String TAG = "emailNotification";

    // ── Paste your EmailJS credentials here ───────────────────────────────────
    private static final String SERVICE_ID  = "service_xxxxxx";  // Email Services tab
    private static final String TEMPLATE_ID = "template_xxxxxx"; // Email Templates tab
    private static final String PUBLIC_KEY  = "xxxxxxxxxxxx";     // Account → API Keys

    private static final String EMAILJS_URL =
            "https://api.emailjs.com/api/v1.0/email/send";

    // ── UML fields ────────────────────────────────────────────────────────────
    private String notificationId;
    private String message;
    private java.util.Date sentDate;

    // ── Extra fields needed to build the email ────────────────────────────────
    // These are populated by Reservation before calling notifyObservers()
    private final String toEmail;
    private final String eventTitle;
    private final String eventLocation;
    private final String eventDate;
    private final String price;
    private final String reservationId;

    /**
     * Constructor — called by Reservation when attaching this observer.
     * We pass in all the details needed to build the confirmation email.
     */
    public emailNotification(String notificationId,
                             String toEmail,
                             String eventTitle,
                             String eventLocation,
                             String eventDate,
                             String price,
                             String reservationId) {
        this.notificationId = notificationId;
        this.toEmail        = toEmail;
        this.eventTitle     = eventTitle;
        this.eventLocation  = eventLocation;
        this.eventDate      = eventDate;
        this.price          = price;
        this.reservationId  = reservationId;
    }

    /**
     * Called automatically by Reservation.notifyObservers(message).
     * This is the Observer pattern entry point.
     *
     * UML: + update(message: String): void
     */
    @Override
    public void update(String message) {
        // Store the message as per UML field
        this.message  = message;
        this.sentDate = new java.util.Date(); // right now

        Log.d(TAG, "Observer notified: " + message);

        // Trigger the actual email send
        sendEmail(message);
    }

    /**
     * Sends the confirmation email via EmailJS.
     * Runs asynchronously so it never freezes the UI.
     *
     * UML: + sendEmail(message: String): void
     */
    public void sendEmail(String message) {
        try {
            // ── Build the template variables ──────────────────────────────────
            // These {{variable}} names must match your EmailJS template exactly
            JSONObject templateParams = new JSONObject();
            templateParams.put("to_email",       toEmail);
            templateParams.put("event_title",    eventTitle);
            templateParams.put("event_location", eventLocation);
            templateParams.put("event_date",     eventDate);
            templateParams.put("price",          price);
            templateParams.put("reservation_id", reservationId);
            templateParams.put("message",        message); // e.g. "Booking confirmed!"

            // ── Build the full EmailJS request body ───────────────────────────
            JSONObject body = new JSONObject();
            body.put("service_id",      SERVICE_ID);
            body.put("template_id",     TEMPLATE_ID);
            body.put("user_id",         PUBLIC_KEY);
            body.put("template_params", templateParams);

            // ── Build and fire the HTTP request ───────────────────────────────
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(EMAILJS_URL)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("origin", "http://localhost") // required by EmailJS
                    .build();

            // enqueue() runs on a background thread automatically
            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Email send failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Email sent successfully to " + toEmail);
                    } else {
                        Log.e(TAG, "EmailJS error: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to build email: " + e.getMessage());
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getNotificationId() { return notificationId; }
    public String getMessage()        { return message; }
    public java.util.Date getSentDate(){ return sentDate; }


}
