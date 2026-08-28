package com.sfedu.campus.helpers;

import android.content.Context;
import android.util.Log;

import com.sfedu.campus.generated.invoker.ApiClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import java.io.IOException;

public class ApiProvider {
    private static ApiClient apiClient;

    public static synchronized ApiClient getApiClient(Context context) {
        if (apiClient == null) {
            // Use Application Context to avoid leaking Activity/Fragment context
            Context appContext = context.getApplicationContext();
            
            apiClient = new ApiClient();
            apiClient.setBasePath("http://localhost:3000/api/v1");

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            OkHttpClient newClient = apiClient.getHttpClient().newBuilder()
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            // Use the application context stored in the closure
                            String token = new PreferencesHelper(appContext).getToken();
                            
                            if (token != null && !token.isEmpty()) {
                                Log.d("ApiProvider", "Intercepting: adding Bearer token to " + original.url());
                                Request request = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build();
                                return chain.proceed(request);
                            }
                            return chain.proceed(original);
                        }
                    })
                    .build();

            apiClient.setHttpClient(newClient);
        }
        return apiClient;
    }
}
