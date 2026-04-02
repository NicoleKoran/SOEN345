package com.example.bookingapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingapp.Event;
import com.example.bookingapp.EventCategory;
import com.example.bookingapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.ViewHolder> {

    public interface EventActionListener {
        void onEdit(Event event);
    }

    private final EventActionListener actionListener;
    private List<Event> events = new ArrayList<>();

    public AdminEventAdapter(EventActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setEvents(List<Event> updatedEvents) {
        events = new ArrayList<>(updatedEvents);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event event = events.get(position);
        holder.title.setText(event.getTitle());
        holder.location.setText(event.getLocation());
        holder.date.setText(new SimpleDateFormat("EEE, MMM dd yyyy HH:mm", Locale.getDefault())
                .format(event.getDate()));
        holder.categoryBadge.setText(event.getCategory().toFirestoreValue().toUpperCase(Locale.US));
        holder.seatsText.setText(event.getAvailableSeats() + "/" + event.getTotalSeats() + " seats");
        holder.statusText.setText(event.getStatus().toFirestoreValue());

        int categoryColor = getCategoryColor(event.getCategory());
        holder.categoryStripe.setBackgroundColor(categoryColor);
        holder.categoryBadge.getBackground().setTint(categoryColor);

        holder.editButton.setOnClickListener(view -> actionListener.onEdit(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private int getCategoryColor(EventCategory category) {
        if (category == null) {
            return Color.parseColor("#3949AB");
        }

        switch (category) {
            case CONCERT:
                return Color.parseColor("#6A1B9A");
            case MOVIE:
                return Color.parseColor("#1565C0");
            case SPORT:
                return Color.parseColor("#E65100");
            case TRAVEL:
                return Color.parseColor("#2E7D32");
            default:
                return Color.parseColor("#3949AB");
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView location;
        final TextView date;
        final TextView categoryBadge;
        final TextView seatsText;
        final TextView statusText;
        final View categoryStripe;
        final Button bookButton;
        final Button editButton;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleText);
            location = itemView.findViewById(R.id.locationText);
            date = itemView.findViewById(R.id.dateText);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            seatsText = itemView.findViewById(R.id.seatsText);
            statusText = itemView.findViewById(R.id.statusText);
            categoryStripe = itemView.findViewById(R.id.categoryStripe);
            bookButton = itemView.findViewById(R.id.bookButton);
            editButton = itemView.findViewById(R.id.editButton);
            bookButton.setEnabled(false);
        }
    }
}
