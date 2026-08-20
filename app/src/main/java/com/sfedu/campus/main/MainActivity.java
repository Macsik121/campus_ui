package com.sfedu.campus.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sfedu.campus.R;
import com.sfedu.campus.auth.AuthActivity;
import com.sfedu.campus.helpers.ApiClient;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;
import com.sfedu.campus.map.MapFragment;
import com.sfedu.campus.models.data_models.User;
import com.sfedu.campus.notifications.NotificationFragment;
import com.sfedu.campus.profile.ProfileFragment;
import com.sfedu.campus.squad.SquadFragment;
import com.sfedu.campus.squad_log.SquadLogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    // Карта для хранения фрагментов. Ключ = ID пункта меню, Значение = Фрагмент
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    // По умолчанию показываем раздел "Отряд"
    private int currentNavItemId = R.id.nav_squad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        PreferencesHelper prefs = new PreferencesHelper(this);
//        if (!ApiClient.getInstance().isTokenValid(findViewById(R.id.main), this)) {
        if (!prefs.isTokenSet()) {
            NavigationHelper.goToAuth(this);
            return;
        }

        TextView sign_of_what_jesus_says = findViewById(R.id.jesus_saying);
        sign_of_what_jesus_says.setText(new PreferencesHelper(this).getJesusSaying());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // changing the behavior of the "back" arrow on the phone
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        switchFragment(R.id.nav_squad, false);

        // Слушатель нажатий на нижнюю навигацию
        // new BottomNavigationView.OnItemSelectedListener() - can be replaced with lambda: item ->
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId(), true);
            return true;
        });
    }
    /**
     * Метод переключения фрагментов
     * @param navItemId ID пункта меню
     * @param addToBackStack нужно ли добавлять в стек (для кнопки Назад)
     */
    private void switchFragment(int navItemId, boolean addToBackStack) {
        // Если нажали на тот же самый раздел - ничего не делаем
        if (currentNavItemId == navItemId) return;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // 1. Скрываем текущий фрагмент (если он есть)
        Fragment currentFragment = fragmentMap.get(currentNavItemId);
        if (currentFragment != null && currentFragment.isAdded()) {
            transaction.hide(currentFragment);
        }

        // 2. Получаем или создаем новый фрагмент по ID
        Fragment targetFragment = fragmentMap.get(navItemId);
        if (targetFragment == null) {
            if (navItemId == R.id.nav_squad) {
                targetFragment = new SquadFragment();
            } else if (navItemId == R.id.nav_notifications) {
                targetFragment = new NotificationFragment();
            } else if (navItemId == R.id.nav_map) {
                targetFragment = new MapFragment();
            } else if (navItemId == R.id.nav_squad_log) {
                targetFragment = new SquadLogFragment();
            } else if (navItemId == R.id.nav_profile) {
                targetFragment = new ProfileFragment();
            } else {
                return; // Если ID не совпадает ни с одним пунктом - прерываем
            }

            fragmentMap.put(navItemId, targetFragment);
        }

        // 3. Проверяем, добавлен ли фрагмент в контейнер
        if (!targetFragment.isAdded()) {
            // Если нет - добавляем
            transaction.add(R.id.fragment_container, targetFragment, String.valueOf(navItemId));
        } else {
            // Если уже был добавлен (но скрыт) - просто показываем
            transaction.show(targetFragment);
        }

        // 4. Сохраняем текущий ID
        currentNavItemId = navItemId;

        // 5. Опционально: добавляем в BackStack для кнопки Назад
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}
