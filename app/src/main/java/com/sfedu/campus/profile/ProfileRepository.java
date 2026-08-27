package com.sfedu.campus.profile;
//import com.sfedu.campus.helpers.ApiClient;

import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.ProfileApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiCallback;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.model.UserProfile;
import com.sfedu.campus.helpers.ApiProvider;
import com.sfedu.campus.helpers.SimpleApiCallback;

import android.content.Context;

import java.util.List;
import java.util.Map;

public class ProfileRepository {
    private final ProfileApi api;

    public ProfileRepository(Context context) {
        ApiClient client = ApiProvider.getApiClient(context);
        // Передаем настроенный ApiClient в конструктор сгенерированного API класса
        this.api = new ProfileApi(client);
    }

    public void getProfile(DataCallback<UserProfile> callback) {
        try {
            callback.onSuccess(api.getUserProfile());
        } catch (ApiException e) {
            callback.onError(e.getMessage());
        }
    }
    public void setProfile(UserProfile profile, DataCallback<UserProfile> callback) {
        try {
            callback.onSuccess(api.updateUserProfile(profile));
        } catch (ApiException e) {
            callback.onError(e.getMessage());
        }
    }
}
