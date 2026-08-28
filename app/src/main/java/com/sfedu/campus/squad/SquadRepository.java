package com.sfedu.campus.squad;

import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.SquadsApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.Child;
import com.sfedu.campus.generated.model.GetSquadChildrenResponse;
import com.sfedu.campus.helpers.ApiProvider;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class SquadRepository {
    private final SquadsApi api;

    public SquadRepository(Context context) {
        ApiClient client = ApiProvider.getApiClient(context);
        this.api = new SquadsApi(client);
    }

    public void getChildrenBySquad(UUID squadId, DataCallback<List<Child>> callback) {
        new Thread(() -> {
            try {
                // The API now returns a single object instead of a list
                GetSquadChildrenResponse response = api.getChildrenBySquad(squadId);

//                Log.i("SquadFragment", response.toString());
                if (response != null) {
                    List<Child> children = response.getChildren();
                    callback.onSuccess(children != null ? children : new ArrayList<>());
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            } catch (Exception e) {
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    public void updateChildNotes(UUID childId, String notes, DataCallback<Child> callback) {
        new Thread(() -> {
            try {
                // Create Child with only id and notes fields
                Child child = new Child();
                child.setId(childId);
                child.setNotes(notes);
                Log.i("SquadRepository", "updateChildNotes: child: " + child.toString());
                Child updatedChild = api.updateChild(child);
                callback.onSuccess(updatedChild);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            } catch (Exception e) {
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
}
