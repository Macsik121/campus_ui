package com.sfedu.campus.notifications;

import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.NotificationsApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.Notification;
import com.sfedu.campus.generated.model.ReadAllNotifications200Response;
import com.sfedu.campus.generated.model.ReadNotificationRequest;
import com.sfedu.campus.generated.model.ReadNotificationResponse;
import com.sfedu.campus.helpers.ApiProvider;

import android.content.Context;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {
    private final NotificationsApi api;

    public NotificationRepository(Context context) {
        ApiClient client = ApiProvider.getApiClient(context);
        this.api = new NotificationsApi(client);
    }

    public void getNotifications(DataCallback<List<Notification>> callback) {
        new Thread(() -> {
            try {
                List<Notification> response = api.getNotifications(20);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void markAsRead(UUID notificationId, DataCallback<ReadNotificationResponse> callback) {
        new Thread(() -> {
            try {
                ReadNotificationRequest request = new ReadNotificationRequest();
                request.setNotifId(notificationId);
                ReadNotificationResponse response = api.readNotification(request);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void markAllAsRead(DataCallback<ReadAllNotifications200Response> callback) {
        new Thread(() -> {
            try {
                ReadAllNotifications200Response response = api.readAllNotifications();
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}