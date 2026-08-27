package com.sfedu.campus.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sfedu.campus.generated.model.Child;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import android.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PreferencesHelper {
    private static final String PREF_NAME = "AppPrefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_JESUS_SAYING = "Jesus_says";
    // Squad caching
    private static final String KEY_SQUAD_ID = "squad_id";
    private static final String KEY_SQUAD_TITLE = "squad_title";
    private static final String KEY_CHILDREN_LIST = "children_list";
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

    // Squad caching methods
    public void saveSquadId(UUID squadId) {
        if (squadId != null) {
            editor.putString(KEY_SQUAD_ID, squadId.toString()).apply();
        } else {
            editor.remove(KEY_SQUAD_ID).apply();
        }
    }

    public UUID getSquadId() {
        String squadIdStr = prefs.getString(KEY_SQUAD_ID, null);
        if (squadIdStr != null) {
            try {
                return UUID.fromString(squadIdStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public boolean hasSquadId() {
        return prefs.contains(KEY_SQUAD_ID);
    }

    public void saveSquadTitle(String squadTitle) {
        if (squadTitle != null) {
            editor.putString(KEY_SQUAD_TITLE, squadTitle).apply();
        } else {
            editor.remove(KEY_SQUAD_TITLE).apply();
        }
    }

    public String getSquadTitle() {
        return prefs.getString(KEY_SQUAD_TITLE, null);
    }

    public void saveChildrenList(List<Child> children) {
        Gson gson = new Gson();
        String json = gson.toJson(children);
        editor.putString(KEY_CHILDREN_LIST, json).apply();
    }

    public List<Child> getChildrenList() {
        String json = prefs.getString(KEY_CHILDREN_LIST, null);
        if (json != null && !json.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Child>>() {}.getType();
            List<Child> children = gson.fromJson(json, type);
            return children != null ? children : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public boolean hasChildrenCache() {
        return prefs.contains(KEY_CHILDREN_LIST);
    }

    public void clearSquadData() {
        editor.remove(KEY_SQUAD_ID).remove(KEY_SQUAD_TITLE).remove(KEY_CHILDREN_LIST).apply();
    }
}
