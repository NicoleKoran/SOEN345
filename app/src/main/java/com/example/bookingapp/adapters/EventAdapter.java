package com.example.bookingapp.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.bookingapp.R;
import com.example.bookingapp.models.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(Event event);
    }

    private List<Event> events;
    private OnBookClickListener bookListener;

    public EventAdapter(List<Event> events, OnBookClickListener listener) {
        this.events = events;
        this.bookListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, date, categoryBadge, priceText;
        View categoryStripe;
        Button bookButton;

        public ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.titleText);
            location = itemView.findViewById(R.id.locationText);
            date = itemView.findViewById(R.id.dateText);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
            priceText = itemView.findViewById(R.id.priceText);
            categoryStripe = itemView.findViewById(R.id.categoryStripe);
            bookButton = itemView.findViewById(R.id.bookButton);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.title.setText(event.getTitle());
        holder.location.setText("📍 " + event.getLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault());
        holder.date.setText("🗓 " + sdf.format(event.getDate()));

        Event.EventCategory category = event.getCategory();
        String cat = category != null ? category.name().toUpperCase() : "EVENT";
        int color = getCategoryColor(category != null ? category.name() : null);

        holder.categoryBadge.getBackground().setTint(color);
        holder.categoryStripe.setBackgroundColor(color);

        holder.priceText.setText(event.getAvailableSeats() + " seats left");

        holder.bookButton.setOnClickListener(v -> {
            if (bookListener != null) bookListener.onBookClick(event);
        });
    }

    private int getCategoryColor(String category) {
        if (category == null) return Color.parseColor("#3949AB");
        switch (category.toLowerCase()) {
            case "concert": return Color.parseColor("#6A1B9A");
            case "movie":   return Color.parseColor("#1565C0");
            case "sport": return Color.parseColor("#2E7D32");
            case "travel":  return Color.parseColor("#E65100");
            default:        return Color.parseColor("#3949AB");
        }
    }

    @Override
    public int getItemCount() { return events.size(); }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }
}