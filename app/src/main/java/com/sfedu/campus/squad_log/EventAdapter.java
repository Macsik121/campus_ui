package com.sfedu.campus.squad_log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sfedu.campus.R;
import com.sfedu.campus.generated.model.Event;
import com.sfedu.campus.helpers.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events = new ArrayList<>();
    private UUID selectedEventId = null;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEvents(List<Event> events) {
        this.events = events != null ? events : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedEventId(UUID eventId) {
        UUID previous = this.selectedEventId;
        this.selectedEventId = eventId;
        if (previous != null) {
            notifyItemChanged(events.indexOf(findEventById(previous)));
        }
        if (eventId != null) {
            notifyItemChanged(events.indexOf(findEventById(eventId)));
        }
    }

    public UUID getSelectedEventId() {
        return selectedEventId;
    }

    private Event findEventById(UUID id) {
        for (Event e : events) {
            if (id.equals(e.getId())) return e;
        }
        return null;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventTitle;
        private final TextView eventTime;
        private final TextView eventDescription;
        private final View selectedIndicator;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventTime = itemView.findViewById(R.id.event_time);
            eventDescription = itemView.findViewById(R.id.event_description);
            selectedIndicator = itemView.findViewById(R.id.event_selected_indicator);
        }

        void bind(Event event) {
            eventTitle.setText(event.getTitle() != null ? event.getTitle() : "");
            eventDescription.setText(event.getDescription() != null ? event.getDescription() : "");

            // Format time
            String timeText = "";
            if (event.getTimeFrom() != null && event.getTimeTo() != null) {
                String from = TimeUtils.formatTime(event.getTimeFrom());
                String to = TimeUtils.formatTime(event.getTimeTo());
                timeText = from + " - " + to;
            } else if (event.getTimeFrom() != null) {
                timeText = TimeUtils.formatTime(event.getTimeFrom());
            }
            eventTime.setText(timeText);

            // Show selected indicator
            boolean isSelected = selectedEventId != null && selectedEventId.equals(event.getId());
            selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            // Highlight selected item
            itemView.setBackgroundColor(isSelected ?
                itemView.getContext().getColor(android.R.color.holo_blue_light) :
                android.graphics.Color.TRANSPARENT);

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event);
                }
            });
        }
    }
}