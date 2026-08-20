package com.sfedu.campus.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.sfedu.campus.helpers.ApiClient;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.main.MainActivity;
import com.sfedu.campus.R;

public class AuthActivity extends AppCompatActivity {
    private String emailLogin;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferencesHelper prefs = new PreferencesHelper(this);
        // is token valid
//        if (ApiClient.getInstance().isTokenValid(findViewById(R.id.auth_activity_layout), this)) {
        if (prefs.isTokenSet()) {
            // Токен есть - перенаправляем в MainActivity
            NavigationHelper.goToMain(this);
            return;
        } // Иначе остаемся в MainActivity и грузим Fragment Отряд

        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
        }

        findViewById(R.id.btn_to_login).setOnClickListener(v -> switchFragment(new LoginFragment()));
        findViewById(R.id.btn_to_register).setOnClickListener(v -> switchFragment(new RegisterFragment()));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}