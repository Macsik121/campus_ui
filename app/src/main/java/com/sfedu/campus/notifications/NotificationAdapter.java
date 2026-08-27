package com.sfedu.campus.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sfedu.campus.R;
import com.sfedu.campus.generated.model.Notification;
import com.sfedu.campus.helpers.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications = new ArrayList<>();
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onMarkAsRead(Notification notification, int position);
    }

    public void setOnNotificationActionListener(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public int getUnreadCount() {
        int count = 0;
        for (Notification n : notifications) {
            if (Boolean.FALSE.equals(n.getIsRead())) {
                count++;
            }
        }
        return count;
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;
        TextView titleText;
        TextView descriptionText;
        TextView timeText;
        Button readButton;
        View itemView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            iconView = itemView.findViewById(R.id.notification_icon);
            titleText = itemView.findViewById(R.id.notification_title);
            descriptionText = itemView.findViewById(R.id.notification_description);
            timeText = itemView.findViewById(R.id.notification_time);
            readButton = itemView.findViewById(R.id.read_button);
        }

        void bind(Notification notification, int position) {
            titleText.setText(notification.getTitle());
            descriptionText.setText(notification.getDescription());

            // Set time
            if (notification.getSentAt() != null) {
                timeText.setText(TimeUtils.getRelativeTime(notification.getSentAt()));
            } else {
                timeText.setText("");
            }

            // Set icon based on notification type
            // We'll use the title/description to determine type, or you could add a type field
            // For now, we'll use a simple heuristic based on title keywords
            int iconRes = getIconForNotification(notification);
            iconView.setImageResource(iconRes);

            // Set content description for accessibility
            String contentDesc = getContentDescriptionForIcon(iconRes);
            iconView.setContentDescription(contentDesc);

            // Set background color based on read status
            boolean isRead = Boolean.TRUE.equals(notification.getIsRead());
            if (isRead) {
                itemView.setBackgroundColor(itemView.getContext().getColor(android.R.color.white));
            } else {
                // Transparent blue for unread
                itemView.setBackgroundColor(itemView.getContext().getColor(R.color.unread_background));
            }

            // Show/hide read button based on read status
            if (isRead) {
                readButton.setVisibility(View.GONE);
            } else {
                readButton.setVisibility(View.VISIBLE);
                readButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onMarkAsRead(notification, position);
                    }
                });
            }
        }

        private int getIconForNotification(Notification notification) {
            // Determine icon based on notification content
            // You can extend this logic based on your actual notification types
            String title = notification.getTitle() != null ? notification.getTitle().toLowerCase() : "";
            String description = notification.getDescription() != null ? notification.getDescription().toLowerCase() : "";

            if (title.contains("карта") || title.contains("перемещ") ||
                description.contains("карта") || description.contains("перемещ") ||
                title.contains("map") || title.contains("location")) {
                return R.drawable.ic_notification_map;
            } else if (title.contains("важно") || title.contains("срочно") || title.contains("критич") ||
                    description.contains("важно") || description.contains("срочно") || description.contains("критич") ||
                    title.contains("important") || title.contains("urgent")) {
                return R.drawable.ic_notification_important;
            } else {
                // Default to bell for informational announcements
                return R.drawable.ic_notification_bell;
            }
        }

        private String getContentDescriptionForIcon(int iconRes) {
            if (iconRes == R.drawable.ic_notification_map) {
                return itemView.getContext().getString(R.string.notification_type_map);
            } else if (iconRes == R.drawable.ic_notification_important) {
                return itemView.getContext().getString(R.string.notification_type_important);
            } else {
                return itemView.getContext().getString(R.string.notification_type_info);
            }
        }
    }
}