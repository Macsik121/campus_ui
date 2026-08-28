package com.sfedu.campus.squad_log;

import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.JournalApi;
import com.sfedu.campus.generated.api.SquadsApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.Child;
import com.sfedu.campus.generated.model.Event;
import com.sfedu.campus.generated.model.GetAttendanceResponse;
import com.sfedu.campus.generated.model.GetSquadChildrenResponse;
import com.sfedu.campus.generated.model.UpdateAttendanceRequest;
import com.sfedu.campus.generated.model.UpdateAttendanceResponse;
import com.sfedu.campus.helpers.ApiProvider;

import android.content.Context;
import android.util.Log;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SquadLogRepository {
    private final JournalApi journalApi;
    private final SquadsApi squadsApi;

    public SquadLogRepository(Context context) {
        ApiClient client = ApiProvider.getApiClient(context);
        this.journalApi = new JournalApi(client);
        this.squadsApi = new SquadsApi(client);
    }

    public void getChildren(UUID squadId, DataCallback<GetSquadChildrenResponse> callback) {
        new Thread(() -> {
            try {
                GetSquadChildrenResponse response = squadsApi.getChildrenBySquad(squadId);
                Log.i("SquadLogRepository", "getChildren: " + response);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getEvents(LocalDate date, Integer limit, DataCallback<List<Event>> callback) {
        new Thread(() -> {
            try {
                List<Event> response = journalApi.getEvents(date, limit);
                Log.i("SquadLogRepository", "getEvents: " + response);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void getEventAttendance(UUID eventId, UUID squadId, DataCallback<GetAttendanceResponse> callback) {
        new Thread(() -> {
            try {
                GetAttendanceResponse response = journalApi.getEventAttendance(eventId, squadId);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void updateAttendance(UUID eventId, UUID childId, boolean isPresent, DataCallback<UpdateAttendanceResponse> callback) {
        new Thread(() -> {
            try {
                UpdateAttendanceRequest request = new UpdateAttendanceRequest();
                request.setChildId(childId);
                request.setPresent(isPresent);
                UpdateAttendanceResponse response = journalApi.updateAttendance(eventId, request);
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}