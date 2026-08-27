package com.sfedu.campus.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sfedu.campus.R;
import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.model.Notification;
import com.sfedu.campus.generated.model.ReadAllNotifications200Response;
import com.sfedu.campus.generated.model.ReadNotificationResponse;
import com.sfedu.campus.helpers.ApiProvider;
import com.sfedu.campus.helpers.ViewUtils;

import java.util.List;
import java.util.UUID;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationActionListener {

    private static final String TAG = "NotificationFragment";

    // UI Elements
    private TextView pageTitle;
    private TextView unreadCountText;
    private Button readAllButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View loadingOverlay;
    private TextView emptyStateText;

    // Data
    private NotificationAdapter adapter;
    private NotificationRepository repository;
    private boolean isLoading = false;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI elements
        pageTitle = view.findViewById(R.id.page_title);
        unreadCountText = view.findViewById(R.id.unread_count_text);
        readAllButton = view.findViewById(R.id.read_all_button);
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        // Setup RecyclerView
        adapter = new NotificationAdapter();
        adapter.setOnNotificationActionListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup Repository
        repository = new NotificationRepository(requireContext());

        // Setup Read All button
        readAllButton.setOnClickListener(v -> markAllAsRead());

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        setLoading(true);
        repository.getNotifications(new DataCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    adapter.setNotifications(notifications);
                    updateUnreadCount(notifications);
                    updateEmptyState(notifications);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    ViewUtils.toast(requireView(), requireContext(), getString(R.string.notification_error));
                    Log.e(TAG, "Error loading notifications: " + error);
                });
            }
        });
    }

    private void updateUnreadCount(List<Notification> notifications) {
        int unreadCount = 0;
        if (notifications != null) {
            for (Notification n : notifications) {
                if (n.getIsRead() == null || !n.getIsRead()) {
                    unreadCount++;
                }
            }
        }
        unreadCountText.setText(getString(R.string.unread_count_format, unreadCount));
        readAllButton.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState(List<Notification> notifications) {
        boolean isEmpty = notifications == null || notifications.isEmpty();
        emptyStateText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        recyclerView.setEnabled(!loading);
        readAllButton.setEnabled(!loading);
        if (adapter != null) {
            // Disable read buttons in adapter during loading
            // We'll handle this by checking isLoading in the adapter callback
        }
    }

    @Override
    public void onMarkAsRead(Notification notification, int position) {
        if (isLoading) return;
        if (notification.getId() == null) return;

        // Freeze the specific card - disable the read button
        freezeCard(position, true);

        repository.markAsRead(notification.getId(), new DataCallback<ReadNotificationResponse>() {
            @Override
            public void onSuccess(ReadNotificationResponse response) {
                requireActivity().runOnUiThread(() -> {
                    // Update the notification in the list
                    Notification updatedNotification = new Notification();
                    updatedNotification.setId(notification.getId());
                    updatedNotification.setTitle(notification.getTitle());
                    updatedNotification.setDescription(notification.getDescription());
                    updatedNotification.setSentAt(notification.getSentAt());
                    updatedNotification.setIsRead(true);

                    List<Notification> currentList = adapter.getNotifications();
                    if (position < currentList.size()) {
                        currentList.set(position, updatedNotification);
                        adapter.notifyItemChanged(position);
                    }

                    // Update unread count
                    updateUnreadCount(currentList);

                    freezeCard(position, false);
                    ViewUtils.toast(requireView(), requireContext(), getString(R.string.notification_read_success));
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    freezeCard(position, false);
                    ViewUtils.toast(requireView(), requireContext(), getString(R.string.notification_error));
                    Log.e(TAG, "Error marking notification as read: " + error);
                });
            }
        });
    }

    private void markAllAsRead() {
        if (isLoading) return;

        // Freeze entire list
        setLoading(true);

        repository.markAllAsRead(new DataCallback<ReadAllNotifications200Response>() {
            @Override
            public void onSuccess(ReadAllNotifications200Response response) {
                requireActivity().runOnUiThread(() -> {
                    // Update all notifications to read
                    List<Notification> currentList = adapter.getNotifications();
                    for (int i = 0; i < currentList.size(); i++) {
                        Notification n = currentList.get(i);
                        if (n.getIsRead() == null || !n.getIsRead()) {
                            Notification updated = new Notification();
                            updated.setId(n.getId());
                            updated.setTitle(n.getTitle());
                            updated.setDescription(n.getDescription());
                            updated.setSentAt(n.getSentAt());
                            updated.setIsRead(true);
                            currentList.set(i, updated);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Update unread count
                    updateUnreadCount(currentList);

                    setLoading(false);
                    ViewUtils.toast(requireView(), requireContext(), getString(R.string.notification_read_all_success));
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    ViewUtils.toast(requireView(), requireContext(), getString(R.string.notification_error));
                    Log.e(TAG, "Error marking all notifications as read: " + error);
                });
            }
        });
    }

    private void freezeCard(int position, boolean freeze) {
        RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
        if (holder instanceof NotificationAdapter.NotificationViewHolder) {
            NotificationAdapter.NotificationViewHolder vh = (NotificationAdapter.NotificationViewHolder) holder;
            // We need to access the readButton - but it's private
            // Instead, we can just disable the whole item view
            vh.itemView.setEnabled(!freeze);
            vh.itemView.setAlpha(freeze ? 0.5f : 1.0f);
        }
    }
}