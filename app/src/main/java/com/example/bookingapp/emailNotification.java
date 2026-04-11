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
    private static final String SERVICE_ID  = "service_xzg9d03";
    private static final String TEMPLATE_ID = "template_2ikwt2u";
    private static final String PUBLIC_KEY  = "uyeEbo2-ZzNb5hfY9";

    private static final String EMAILJS_URL =
            "https://api.emailjs.com/api/v1.0/email/send";

    /** Unit tests set this to a MockWebServer URL; production leaves it null. */
    static String testEmailEndpointUrl;

    /**
     * When true, {@link #sendEmail} returns immediately without making any HTTP call.
     * Set this in instrumented tests to prevent real emails from being sent.
     */
    public static boolean suppressEmailsForTesting = false;

    /**
     * Distinguishes why this notification is being sent so the email content
     * and subject line can differ between confirmations and cancellations.
     */
    public enum NotificationType {
        /** Sent after a customer successfully books a ticket. */
        BOOKING_CONFIRMATION,
        /** Sent when the customer themselves cancels their own reservation. */
        USER_CANCELLATION,
        /** Sent when an admin cancels the whole event, affecting all reservations. */
        EVENT_CANCELLATION
    }

    private final String notificationId;
    private String message;
    private Date sentDate;

    private final String toEmail;
    private final String eventTitle;
    private final String eventLocation;
    private final String eventDate;
    private final String price;
    private final String reservationId;
    private final NotificationType notificationType;

    /**
     * Full constructor — used by BookingRepository.
     *
     * @param notificationId  reuses the Firestore reservation doc ID
     * @param toEmail         the customer's email address
     * @param eventTitle      shown in email subject + body
     * @param eventLocation   shown in email body
     * @param eventDate       already formatted as a readable string
     * @param price           shown in email body (confirmation only)
     * @param reservationId   reference number shown in email
     * @param notificationType controls which message variant is sent
     */
    public emailNotification(String notificationId, String toEmail, String eventTitle,
                             String eventLocation, String eventDate, String price,
                             String reservationId, NotificationType notificationType) {
        this.notificationId  = notificationId;
        this.toEmail         = toEmail;
        this.eventTitle      = eventTitle;
        this.eventLocation   = eventLocation;
        this.eventDate       = eventDate;
        this.price           = price;
        this.reservationId   = reservationId;
        this.notificationType = notificationType;
    }

    /**
     * Backward-compatible constructor — defaults to BOOKING_CONFIRMATION.
     * Existing callers (e.g. tests) do not need to change.
     */
    public emailNotification(String notificationId, String toEmail, String eventTitle,
                             String eventLocation, String eventDate, String price,
                             String reservationId) {
        this(notificationId, toEmail, eventTitle, eventLocation, eventDate,
                price, reservationId, NotificationType.BOOKING_CONFIRMATION);
    }

    /** Called by Reservation.notifyObservers() */
    @Override
    public void update(String message) {
        this.message  = message;
        this.sentDate = new Date();
        Log.d(TAG, "Observer notified [" + notificationType + "]: " + message);
        sendEmail(message);
    }

    /**
     * Sends an email via EmailJS with content that varies by {@link NotificationType}.
     *
     * Template variables sent (add these to your EmailJS template):
     *   {{email}}             — recipient address
     *   {{event_title}}       — name of the event
     *   {{event_date}}        — formatted date string
     *   {{price}}             — price / seat info  (confirmation only)
     *   {{reservation_id}}    — booking reference number
     *   {{notification_type}} — "booking_confirmation" | "user_cancellation" | "event_cancellation"
     *   {{subject}}           — suggested email subject line
     *   {{message}}           — main body text describing what happened
     */
    public void sendEmail(String message) {
        if (suppressEmailsForTesting) {
            Log.d(TAG, "Email suppressed for testing [" + notificationType + "] to " + toEmail);
            return;
        }
        try {
            JSONObject templateParams = new JSONObject();
            templateParams.put("email",             toEmail);
            templateParams.put("event_title",       eventTitle);
            templateParams.put("event_date",        eventDate);
            templateParams.put("price",             price);
            templateParams.put("reservation_id",    reservationId);
            templateParams.put("notification_type", notificationType.name().toLowerCase());

            // Subject and body differ per notification type
            switch (notificationType) {

                case USER_CANCELLATION:
                    templateParams.put("subject",
                            "Reservation Cancelled – " + eventTitle);
                    templateParams.put("message",
                            "Your reservation for \"" + eventTitle + "\" has been cancelled "
                            + "as requested. We hope to see you at a future event!");
                    break;

                case EVENT_CANCELLATION:
                    templateParams.put("subject",
                            "Event Cancelled – " + eventTitle);
                    templateParams.put("message",
                            "We're sorry to inform you that \"" + eventTitle + "\" has been "
                            + "cancelled by the organizer. Your reservation has been "
                            + "automatically cancelled. We apologize for any inconvenience.");
                    break;

                case BOOKING_CONFIRMATION:
                default:
                    templateParams.put("subject",
                            "Booking Confirmed – " + eventTitle);
                    templateParams.put("message",
                            "Your booking for \"" + eventTitle + "\" is confirmed! "
                            + "We look forward to seeing you there.");
                    break;
            }

            JSONObject body = new JSONObject();
            body.put("service_id",      SERVICE_ID);
            body.put("template_id",     TEMPLATE_ID);
            body.put("user_id",         PUBLIC_KEY);
            body.put("template_params", templateParams);

            OkHttpClient client = new OkHttpClient();
            String url = testEmailEndpointUrl != null ? testEmailEndpointUrl : EMAILJS_URL;
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("origin", "http://localhost")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Email network failure: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Email sent [" + notificationType + "] to " + toEmail);
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
    public String getNotificationId()      { return notificationId; }
    public String getMessage()             { return message; }
    public Date getSentDate()              { return sentDate; }
    public NotificationType getNotificationType() { return notificationType; }
}
