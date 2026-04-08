package com.example.bookingapp;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * US-10: Displays the logged-in customer's reservation history.
 * US-11: Allows the customer to cancel an active reservation.
 */
public class MyReservationsActivity extends AppCompatActivity {

    /**
     * Debuggable-build only: when set on the launch intent, skips the Firestore fetch
     * and populates the list with two hard-coded test reservations so E2E tests can
     * exercise the full UI without backend data.
     */
    public static final String EXTRA_INSTRUMENTATION_PREFILL =
            "com.example.bookingapp.INSTRUMENTATION_MY_RESERVATIONS_PREFILL";

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TextView statusText;
    private ReservationsAdapter adapter;

    // Package-private so instrumentation tests can inject fakes via scenario.onActivity()
    EventRepository eventRepository;
    BookingRepository bookingRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        eventRepository = new EventRepository();
        bookingRepository = new BookingRepository();

        recyclerView = findViewById(R.id.reservationsRecyclerView);
        emptyText    = findViewById(R.id.emptyText);
        statusText   = findViewById(R.id.myReservationsStatus);

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        adapter = new ReservationsAdapter(new ArrayList<>(), this::onCancelClicked);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadReservations();
    }

    private boolean isDebuggableBuild() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private void loadReservations() {
        // ── Instrumentation / debug fast-path ─────────────────────────────────
        if (isDebuggableBuild() && getIntent().getBooleanExtra(EXTRA_INSTRUMENTATION_PREFILL, false)) {
            loadInstrumentationReservations();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showStatus("You must be logged in to view reservations.");
            return;
        }

        eventRepository.getReservationsForUser(user.getUid(),
                new EventRepository.UserReservationsCallback() {
                    @Override
                    public void onSuccess(List<Reservation> reservations) {
                        if (reservations.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyText.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.setReservations(reservations);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        showStatus("Failed to load reservations: " + message);
                    }
                });
    }

    private void onCancelClicked(Reservation reservation) {
        if (ReservationStatus.CANCELLED.toFirestoreValue().equals(reservation.getStatus())) {
            showStatus("This reservation is already cancelled.");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Reservation")
                .setMessage("Are you sure you want to cancel your reservation for '"
                        + reservation.getEventTitle() + "'?")
                .setPositiveButton("Yes, Cancel", (dialog, which) ->
                        doCancel(reservation))
                .setNegativeButton("Keep It", null)
                .show();
    }

    private void doCancel(Reservation reservation) {
        String eventDate = "";
        if (reservation.getEventDate() != null) {
            eventDate = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                    .format(reservation.getEventDate());
        }

        bookingRepository.cancelReservation(
                reservation.getReservationId(),
                reservation.getEventId(),
                reservation.getUserEmail(),
                reservation.getEventTitle(),
                reservation.getEventLocation(),
                eventDate,
                new BookingRepository.SimpleCallback() {
                    @Override
                    public void onSuccess(String message) {
                        showStatus("Reservation cancelled. A confirmation email has been sent.");
                        loadReservations(); // refresh the list
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showStatus("Could not cancel: " + errorMessage);
                    }
                });
    }

    /**
     * Populates the list with two deterministic test reservations (one CONFIRMED,
     * one CANCELLED-by-user). Only called in debuggable builds when the
     * {@link #EXTRA_INSTRUMENTATION_PREFILL} intent extra is present.
     */
    private void loadInstrumentationReservations() {
        Reservation confirmed = new Reservation(
                "test-evt-1", "concert", "test-uid", "test@example.com",
                "Jazz Night", "Montreal", new Date(), 0);
        confirmed.setReservationId("test-res-confirmed");
        confirmed.setStatus(ReservationStatus.CONFIRMED.toFirestoreValue());

        Reservation cancelled = new Reservation(
                "test-evt-2", "movie", "test-uid", "test@example.com",
                "Movie Night", "Toronto", new Date(), 0);
        cancelled.setReservationId("test-res-cancelled");
        cancelled.setStatus(ReservationStatus.CANCELLED.toFirestoreValue());
        cancelled.setCancellationReason("user_cancelled");

        List<Reservation> testData = Arrays.asList(confirmed, cancelled);
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        adapter.setReservations(testData);
    }

    private void showStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }

    // ── Inner RecyclerView Adapter ─────────────────────────────────────────────

    interface CancelListener {
        void onCancel(Reservation reservation);
    }

    static class ReservationsAdapter
            extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {

        private List<Reservation> reservations;
        private final CancelListener cancelListener;

        ReservationsAdapter(List<Reservation> reservations, CancelListener cancelListener) {
            this.reservations   = reservations;
            this.cancelListener = cancelListener;
        }

        void setReservations(List<Reservation> reservations) {
            this.reservations = reservations;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.reservation_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Reservation r = reservations.get(position);

            holder.eventTitle.setText(r.getEventTitle() != null ? r.getEventTitle() : "Unknown Event");
            holder.location.setText(r.getEventLocation() != null
                    ? "📍 " + r.getEventLocation() : "");

            if (r.getEventDate() != null) {
                String formatted = new SimpleDateFormat("EEE, MMM dd yyyy 'at' h:mm a",
                        Locale.getDefault()).format(r.getEventDate());
                holder.date.setText("🗓 " + formatted);
            } else {
                holder.date.setText("🗓 Date TBD");
            }

            holder.reservationId.setText("Ref: " + r.getReservationId());

            // Badge label and colour differ by status + cancellation reason
            boolean isCancelled = ReservationStatus.CANCELLED.toFirestoreValue()
                    .equalsIgnoreCase(r.getStatus());
            boolean isConfirmed = ReservationStatus.CONFIRMED.toFirestoreValue()
                    .equalsIgnoreCase(r.getStatus());

            int badgeColor;
            String badgeLabel;
            if (isConfirmed) {
                badgeLabel = "CONFIRMED";
                badgeColor = android.graphics.Color.parseColor("#2E7D32");
            } else if (isCancelled) {
                if ("event_cancelled".equals(r.getCancellationReason())) {
                    badgeLabel = "EVENT CANCELLED";
                    badgeColor = android.graphics.Color.parseColor("#6A1B9A"); // purple — organiser's fault
                } else {
                    badgeLabel = "CANCELLED BY YOU";
                    badgeColor = android.graphics.Color.parseColor("#C62828"); // red — self-cancelled
                }
            } else {
                badgeLabel = "PENDING";
                badgeColor = android.graphics.Color.parseColor("#F57C00");
            }

            holder.statusBadge.setText(badgeLabel);
            holder.statusBadge.getBackground().setTint(badgeColor);

            boolean cancellable = !ReservationStatus.CANCELLED.toFirestoreValue()
                    .equalsIgnoreCase(r.getStatus());
            holder.cancelBtn.setVisibility(cancellable ? View.VISIBLE : View.GONE);
            holder.cancelBtn.setOnClickListener(v -> cancelListener.onCancel(r));
        }

        @Override
        public int getItemCount() {
            return reservations.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView eventTitle, location, date, reservationId, statusBadge;
            Button cancelBtn;

            ViewHolder(View itemView) {
                super(itemView);
                eventTitle    = itemView.findViewById(R.id.reservationEventTitle);
                location      = itemView.findViewById(R.id.reservationLocation);
                date          = itemView.findViewById(R.id.reservationDate);
                reservationId = itemView.findViewById(R.id.reservationId);
                statusBadge   = itemView.findViewById(R.id.reservationStatus);
                cancelBtn     = itemView.findViewById(R.id.cancelReservationBtn);
            }
        }
    }
}
