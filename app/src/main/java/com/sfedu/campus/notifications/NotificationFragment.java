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
import androidx.recyclerview.widget.RecyclerView;

import com.sfedu.campus.R;
import com.sfedu.campus.generated.api.NotificationsApi;
import com.sfedu.campus.generated.invoker.ApiCallback;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.Notification;
import com.sfedu.campus.generated.model.ReadAllNotifications200Response;
import com.sfedu.campus.generated.model.ReadNotificationRequest;
import com.sfedu.campus.generated.model.ReadNotificationResponse;
import com.sfedu.campus.helpers.ApiProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private View loadingOverlay;
    private TextView unreadCountText;
    private Button readAllButton;
    private NotificationsApi notificationsApi;

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

        // Initialize views
        recyclerView = view.findViewById(R.id.notifications_recycler);
        progressBar = view.findViewById(R.id.notifications_progress);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        unreadCountText = view.findViewById(R.id.unread_count_text);
        readAllButton = view.findViewById(R.id.read_all_button);

        // Initialize API with bearer token from SharedPreferences
        notificationsApi = new NotificationsApi(ApiProvider.getApiClient(requireContext()));

        // Setup adapter
        adapter = new NotificationAdapter();
        adapter.setOnNotificationActionListener(new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onMarkAsRead(Notification notification, int position) {
                markNotificationAsRead(notification, position);
            }
        });
        recyclerView.setAdapter(adapter);

        // Setup Read All button
        readAllButton.setOnClickListener(v -> markAllNotificationsAsRead());

        // Fetch notifications on start
        fetchNotifications();
    }

    private void fetchNotifications() {
        showLoading(true);

        try {
            notificationsApi.getNotificationsAsync(20, new ApiCallback<List<Notification>>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Log.e("NotificationFragment", "Failed to fetch notifications", e);
                        Toast.makeText(requireContext(), R.string.notification_error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSuccess(List<Notification> result, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (result != null) {
                            adapter.setNotifications(result);
                            updateUnreadCount();
                        }
                    });
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            showLoading(false);
            Log.e("NotificationFragment", "ApiException during fetch", e);
        }
    }

    private void markNotificationAsRead(Notification notification, int position) {
        if (notification.getId() == null) {
            Log.w("NotificationFragment", "Notification ID is null");
            return;
        }

        // 3.1. Замораживается после: нажатия на кнопку "Прочитано"
        freezeUI(true);

        ReadNotificationRequest request = new ReadNotificationRequest();
        request.setNotifId(notification.getId());

        try {
            notificationsApi.readNotificationAsync(request, new ApiCallback<ReadNotificationResponse>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        freezeUI(false);
                        Log.e("NotificationFragment", "Failed to mark notification as read", e);
                        Toast.makeText(requireContext(), R.string.notification_error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSuccess(ReadNotificationResponse result, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        freezeUI(false);
                        // 3.2. Изменяется: состояние одной карточки уведомления, кол-во непрочитанных на 1,
                        // если непрочитанных = 0, то исчезает кнопка "Прочитать всё"
                        notification.setIsRead(true);
                        adapter.notifyItemChanged(position);
                        updateUnreadCount();
                        Toast.makeText(requireContext(), R.string.notification_read_success, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            freezeUI(false);
            Log.e("NotificationFragment", "ApiException during mark as read", e);
        }
    }

    private void markAllNotificationsAsRead() {
        // 3.1. Замораживается после: нажатия на кнопку "Прочитать всё"
        freezeUI(true);

        try {
            notificationsApi.readAllNotificationsAsync(new ApiCallback<ReadAllNotifications200Response>() {
                @Override
                public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        freezeUI(false);
                        Log.e("NotificationFragment", "Failed to mark all notifications as read", e);
                        Toast.makeText(requireContext(), R.string.notification_error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onSuccess(ReadAllNotifications200Response result, int statusCode, Map<String, List<String>> responseHeaders) {
                    requireActivity().runOnUiThread(() -> {
                        freezeUI(false);
                        // 3.2. Изменяется: состояние всех карточек уведомления, которые были не прочитаны,
                        // кол-во непрочитанных = 0, исчезает кнопка "Прочитать всё"
                        for (int i = 0; i < adapter.getItemCount(); i++) {
                            Notification n = adapter.getNotifications().get(i);
                            if (Boolean.FALSE.equals(n.getIsRead())) {
                                n.setIsRead(true);
                                adapter.notifyItemChanged(i);
                            }
                        }
                        updateUnreadCount();
                        Toast.makeText(requireContext(), R.string.notification_read_all_success, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
                }
            });
        } catch (ApiException e) {
            freezeUI(false);
            Log.e("NotificationFragment", "ApiException during mark all as read", e);
        }
    }

    private void updateUnreadCount() {
        int unreadCount = adapter.getUnreadCount();
        unreadCountText.setText(getString(R.string.unread_count_format, unreadCount));

        // Show/hide Read All button based on unread count
        if (unreadCount > 0) {
            readAllButton.setVisibility(View.VISIBLE);
        } else {
            readAllButton.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void freezeUI(boolean freeze) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(freeze ? View.VISIBLE : View.GONE);
        }
        // Also disable RecyclerView interaction during freeze
        if (recyclerView != null) {
            recyclerView.setEnabled(!freeze);
        }
        // Disable header buttons
        if (readAllButton != null) {
            readAllButton.setEnabled(!freeze);
        }
    }
}
