package com.example.bookingapp;

import android.util.Log;
import org.json.JSONObject;
import java.util.Date;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class emailNotification implements NotificationListener {

    private static final String TAG = "emailNotification";

    //api keys from EmailJS
    private static final String SERVICE_ID  = "service_lkkgoai";
    private static final String TEMPLATE_ID = "template_mkcl7m5";
    private static final String PUBLIC_KEY  = "H6TGxL56nWcxHnGeJ";

    private static final String EMAILJS_URL =
            "https://api.emailjs.com/api/v1.0/email/send";


    private final String notificationId;
    private String message;
    private Date sentDate;

    private final String toEmail;
    private final String eventTitle;
    private final String eventLocation;
    private final String eventDate;   //hardcoded format
    private final String price;
    private final String reservationId;

    /**
     * Constructor — called inside BookingRepository after the transaction succeeds
     *
     *notificationId  reuses the Firestore reservation doc ID
     * toEmail         the logged-in user's email from FirebaseAuth
     * eventTitle      shown in email subject + body
     * eventLocation   shown in email body
     * eventDate       already formatted as a readable string
     * price           shown in email body
     * reservationId   shown as reference number in email
     */
    public emailNotification(String notificationId, String toEmail, String eventTitle, String eventLocation, String eventDate, String price, String reservationId) {
        this.notificationId = notificationId;
        this.toEmail        = toEmail;
        this.eventTitle     = eventTitle;
        this.eventLocation  = eventLocation;
        this.eventDate      = eventDate;
        this.price          = price;
        this.reservationId  = reservationId;
    }

    //called by Reservation.notifyObservers()
    @Override
    public void update(String message) {
        this.message  = message;
        this.sentDate = new Date(); // timestamp of when notification was triggered
        Log.d(TAG, "Observer notified: " + message);
        sendEmail(message); // hand off to the actual email sender
    }

    /**
     * Sends the confirmation email via EmailJS.
     * Uses OkHttp to make an async HTTP POST — never blocks the UI thread.
     */
    public void sendEmail(String message) {
        try {
            // template_params JSON
            // Each key here must match a {{variable}} in EmailJS template
            JSONObject templateParams = new JSONObject();
            templateParams.put("email",       toEmail);
            templateParams.put("event_title",    eventTitle);
            templateParams.put("event_date",     eventDate);
            templateParams.put("price",          price);
            templateParams.put("reservation_id", reservationId);


            // EmailJS request body
            JSONObject body = new JSONObject();
            body.put("service_id",      SERVICE_ID);
            body.put("template_id",     TEMPLATE_ID);
            body.put("user_id",         PUBLIC_KEY);
            body.put("template_params", templateParams);

            // async HTTP request
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(EMAILJS_URL)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("origin", "http://localhost") // required by EmailJS
                    .build();

            // enqueue() runs on a background thread — UI stays smooth
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Network error — booking is still saved, just log it
                    Log.e(TAG, "Email network failure: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Email sent successfully to " + toEmail);
                    } else {
                        Log.e(TAG, "EmailJS error: HTTP " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to build email request: " + e.getMessage());
        }
    }

    // Getters
    public String getNotificationId() { return notificationId; }
    public String getMessage()        { return message; }
    public Date getSentDate()         { return sentDate; }
}