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
            // 1. Создаем инстанс
            apiClient = new ApiClient();

            // 2. Задаем базовый URL
            apiClient.setBasePath("http://localhost:3000/api/v1");

            // 3. Создаем логгер для отладки (поможет увидеть, уходит ли заголовок)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            // 4. Настраиваем OkHttpClient с интерцептором для JWT
            OkHttpClient newClient = apiClient.getHttpClient().newBuilder()
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = new PreferencesHelper(context).getToken();
                            
                            // Если токен существует, добавляем заголовок Authorization
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

            // 5. ВАЖНО: Устанавливаем настроенный клиент обратно в ApiClient
            apiClient.setHttpClient(newClient);
        }
        return apiClient;
    }
}
