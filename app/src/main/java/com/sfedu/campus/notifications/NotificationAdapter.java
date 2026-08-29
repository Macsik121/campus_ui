package com.sfedu.campus.notifications;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final View cardView;
        private final View unreadLeftBorder;
        private final ImageView notificationIcon;
        private final TextView notificationTitle;
        private final TextView notificationDescription;
        private final TextView notificationTime;
        private final Button readButton;
        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.notification_card);
            unreadLeftBorder = itemView.findViewById(R.id.unread_left_border);
            notificationIcon = itemView.findViewById(R.id.notification_icon);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            notificationDescription = itemView.findViewById(R.id.notification_description);
            notificationTime = itemView.findViewById(R.id.notification_time);
            readButton = itemView.findViewById(R.id.read_button);

        }

        void bind(Notification notification, int position) {
            // Set title and description
            notificationTitle.setText(notification.getTitle() != null ? notification.getTitle() : "");
            notificationDescription.setText(notification.getDescription() != null ? notification.getDescription() : "");

            // Set time using new format
            notificationTime.setText(TimeUtils.getNotificationRelativeTime(notification.getSentAt()));

            // Set notification type icon
            setNotificationIcon(notification.getTitle());

            // Set read/unread styling
            boolean isRead = notification.getIsRead() != null && notification.getIsRead();
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(23);
            gd.setStroke(2, Color.WHITE);
            Context context = cardView.getContext();
            int colorInt;

            if (isRead) {
                // Read: white background, no left border
                colorInt = ContextCompat.getColor(context, R.color.white);
                unreadLeftBorder.setVisibility(View.GONE);
                readButton.setVisibility(View.GONE);
            } else {
                // Unread: transparent blue background with blue left border
                colorInt = ContextCompat.getColor(context, R.color.unread_background);
                unreadLeftBorder.setVisibility(View.VISIBLE);
                readButton.setVisibility(View.VISIBLE);
            }
            gd.setColor(colorInt);
            cardView.setBackground(gd);

            // Set read button click listener
            readButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkAsRead(notification, position);
                }
            });
        }

        private void setNotificationIcon(String title) {
            if (title == null) {
                notificationIcon.setImageResource(R.drawable.notification);
//                notificationIcon.setHintl
                return;
            }

            String lowerTitle = title.toLowerCase();
            if (lowerTitle.contains("карт") || lowerTitle.contains("перемещ") || lowerTitle.contains("map") || lowerTitle.contains("location")) {
                notificationIcon.setImageResource(R.drawable.ic_notification_map);
            } else if (lowerTitle.contains("важно") || lowerTitle.contains("important") || lowerTitle.contains("экстрен") || lowerTitle.contains("срочн")) {
                notificationIcon.setImageResource(R.drawable.ic_notification_important);
            } else if (lowerTitle.contains("информ") || lowerTitle.contains("объявлен") || lowerTitle.contains("info") || lowerTitle.contains("announce")) {
                notificationIcon.setImageResource(R.drawable.notification);
            } else {
                // Default to bell for general notifications
                notificationIcon.setImageResource(R.drawable.notification);
            }
        }
    }
}