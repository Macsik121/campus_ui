package com.sfedu.campus.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sfedu.campus.auth.AuthActivity;
import com.sfedu.campus.models.data_models.User;
import com.sfedu.campus.models.server_requests.LoginRequest;
import com.sfedu.campus.models.server_requests.RegisterRequest;
import com.sfedu.campus.models.server_responses.LoginResponse;
import com.sfedu.campus.models.server_responses.RegisterResponse;
import com.sfedu.campus.models.server_responses.VerifyJWTResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.*;

public class ApiClient {
    // For Android Emulator, 10.0.2.2 points to the host machine's localhost
    private static final String BASE_URL = "http://localhost:3000/api/v1/";
    private static OkHttpClient client;
    private static ApiClient instance;
    private final Gson gson;
    private ApiClient() {
        client = new OkHttpClient.Builder().build();
        gson = new Gson();
    }
    public static OkHttpClient getClient(Context context) {
        if (client == null) {
            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String token = prefs.getString("jwt_token", null);

            client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        if (token != null) {
                            request = request.newBuilder()
                                    .addHeader("Authorization", "Bearer " + token)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    .build();
        }
        return client;
    }
    public static synchronized ApiClient getInstance() {
        if (ApiClient.instance == null) {
            ApiClient.instance = new ApiClient();
        }
        return ApiClient.instance;
    }

    public interface ApiCallback<T> {
        void onSuccess(T data);
        void onFailure(String errorMessage);
    }
    private <T, R> void execute(Request request, Class<R> responseType, ApiCallback<R> callback, Context context) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        R data = gson.fromJson(responseBody, responseType);
                        callback.onSuccess(data);
                    } catch (Exception e) {
                        callback.onFailure(e.getMessage());
                    }
                } else {
                    String errorMessage;
                    try {
                        JsonObject errorJson = gson.fromJson(responseBody, JsonObject.class);
                        errorMessage = errorJson.get("error").getAsString();
                    } catch (JsonSyntaxException e) {
                        errorMessage = "Error from the server: error code " + response.code();
                    }
                    callback.onFailure(errorMessage);
//                    if ((response.code() == 401 || response.code() == 403) && context != null) {
//                        new PreferencesHelper(context).clear();
//                        NavigationHelper.goToAuth(context);
//                    }
                }
            }
        });
    }

    public <T, R> void post(String endpoint, T requestBody, Class<R> responseType, ApiCallback<R> callback, Context context) {
        String json = gson.toJson(requestBody);
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
            .url(BASE_URL + endpoint)
            .addHeader("Authorization", context != null ? "Bearer " + new PreferencesHelper(context).getToken() : "!")
            .post(body)
            .build();
        execute(request, responseType, callback, context);
    }

    public <R> void get(String endpoint, Class<R> responseType, ApiCallback<R> callback, Context context) {
        String token = context != null ? new PreferencesHelper(context).getToken() : "!";
        Request request = new Request.Builder()
            .url(BASE_URL + endpoint)
            .addHeader("Authorization", token != null ? "Bearer " + token : "")
            .get()
            .build();
        execute(request, responseType, callback, context);
    }

    public <R> void getWithParams(String endpoint, Map<String, String> params, Class<R> responseType, ApiCallback<R> callback, Context context) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + endpoint).newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .get()
                .build();
        execute(request, responseType, callback, context);
    }
    public void login(
        String email,
        String password,
        ApiCallback<LoginResponse> callback,
        Context context
    ) {
        LoginRequest req = new LoginRequest(email, password);
        post("auth/login", req, LoginResponse.class, callback, context);
    }
    public void register(
        String name,
        String email,
        String password,
        ApiCallback<RegisterResponse> callback,
        Context context
    ) {
        RegisterRequest req = new RegisterRequest(name, email, password);
        post("auth/register", req, RegisterResponse.class, callback, context);
    }
    public boolean isTokenValid(View v, Context context) {
        boolean isValid = false;
        class jwtValidation implements ApiCallback<VerifyJWTResponse> {
            private boolean isValid = false;
            public boolean getIsValid() { return isValid; };
            public void setIsValid(boolean isIt) { isValid = isIt; };
            @Override
            public void onSuccess(VerifyJWTResponse data) {
                setIsValid(true);
            }
            @Override
            public void onFailure(String errorMessage) {
                setIsValid(false);
            }
        }
        jwtValidation callback = new jwtValidation();
        get("auth/verify-jwt", VerifyJWTResponse.class, callback, context);
        return callback.getIsValid();
    }
    public void getUser(View view, Context context, ApiCallback<User> callback) {
        JSONObject json = new PreferencesHelper(context).decodePayload();
        try {
            get("users/profile/" + json.getString("userId"), User.class, callback, context);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
