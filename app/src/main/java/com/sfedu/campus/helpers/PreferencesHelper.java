package com.sfedu.campus.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import android.util.Base64;

public class PreferencesHelper {
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_JESUS_SAYING = "Jesus_says";
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    public PreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    // token
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token).apply();
    }
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
    public Boolean isTokenSet() {
        return prefs.contains(KEY_TOKEN);
    }
    // Jesus saying
    public void saveJesusSaying(String JesusSaying) {
        editor.putString(KEY_JESUS_SAYING, JesusSaying).apply();
    }
    public String getJesusSaying() {
        return prefs.getString(
            KEY_JESUS_SAYING,
            "John 3:16 - For God so loved the world that He Gave His only Son, for whoever believes in Him shall not perish, but have eternal life."
        );
    }
    public Boolean hasJesusSpoke() {
        return prefs.contains(KEY_JESUS_SAYING);
    }
    public void clear() {
        prefs.edit().clear().apply();
    }

    public JSONObject decodePayload() {
        try {
            // 1. Разбиваем токен по точке
            String token = getToken();
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Неверный формат JWT");
            }

            // 2. Берем payload (вторую часть)
            String payloadEncoded = parts[1];

            // 3. Декодируем из Base64Url в строку
            // Используем стандартный декодер Base64.getUrlDecoder()
            byte[] decodedBytes = Base64.decode(payloadEncoded, Base64.URL_SAFE);

            String jsonString = new String(decodedBytes, StandardCharsets.UTF_8);

            // 4. Парсим в JSONObject (подключите org.json в зависимостях)
            return new JSONObject(jsonString);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
