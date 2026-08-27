package com.sfedu.campus.squad;

import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.UsersApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.GetUserSquad200Response;
import com.sfedu.campus.helpers.ApiProvider;

import android.content.Context;

public class UserRepository {
    private final UsersApi api;

    public UserRepository(Context context) {
        ApiClient client = ApiProvider.getApiClient(context);
        this.api = new UsersApi(client);
    }

    public void getUserSquad(DataCallback<GetUserSquad200Response> callback) {
        new Thread(() -> {
            try {
                GetUserSquad200Response response = api.getUserSquad();
                callback.onSuccess(response);
            } catch (ApiException e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
}