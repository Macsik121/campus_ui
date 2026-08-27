package com.sfedu.campus.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.R;
import com.sfedu.campus.auth.AuthActivity;
import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.ProfileApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.ApiException;
import com.sfedu.campus.generated.invoker.Configuration;
import com.sfedu.campus.generated.model.UserProfile;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;

import org.json.JSONObject;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    // UI Elements
    private CircleImageView ivAvatar;
    private TextView tvName;
    private TextView tvRole;
    private TextView tvSquad;
    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPhone;
    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private DataEditWaitButton btnSaveChanges;
    private MaterialButton btnChangePassword;
    private MaterialButton btnLogout;
    private ProgressBar progressBar;
    private View viewOverlay;

    // Data
    private PreferencesHelper preferencesHelper;
    private ProfileApi profileApi;
    private UserProfile currentProfile;
    private UserProfile originalProfile;
    private boolean isLoading = false;

    // Track original values for change detection
    private String originalName;
    private String originalEmail;
    private String originalPhone;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencesHelper = new PreferencesHelper(requireContext());

        // Initialize ProfileApi with configured ApiClient (includes Bearer token)
        ApiClient apiClient = Configuration.getDefaultApiClient();
        apiClient.setBasePath("http://localhost:3000/api/v1");

        // Set Bearer token for authentication
        String token = preferencesHelper.getToken();
        Log.i("ProfileFragment", "Setting Bearer token: " + token);
        if (token != null) {
            apiClient.setApiKey("Bearer " + token);
        }

        profileApi = new ProfileApi(apiClient);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
        loadProfile();
    }

    private void initViews(View view) {
        ivAvatar = view.findViewById(R.id.iv_avatar);
        tvName = view.findViewById(R.id.tv_name);
        tvRole = view.findViewById(R.id.tv_role);
        tvSquad = view.findViewById(R.id.tv_squad);
        etName = view.findViewById(R.id.et_name);
        etEmail = view.findViewById(R.id.et_email);
        etPhone = view.findViewById(R.id.et_phone);
        tilName = view.findViewById(R.id.til_name);
        tilEmail = view.findViewById(R.id.til_email);
        tilPhone = view.findViewById(R.id.til_phone);
        btnSaveChanges = view.findViewById(R.id.btn_save_changes);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);
        progressBar = view.findViewById(R.id.progress_bar);
        viewOverlay = view.findViewById(R.id.view_overlay);
    }

    private void setupListeners() {
        // Text change listeners for change detection
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etName.addTextChangedListener(changeWatcher);
        etEmail.addTextChangedListener(changeWatcher);
        etPhone.addTextChangedListener(changeWatcher);

        // Save button
        btnSaveChanges.setOnClickListener(v -> saveProfile());

        // Change password button
        btnChangePassword.setOnClickListener(v -> {
            // Placeholder for change password functionality
            ViewUtils.toast(requireView(), requireContext(), "Функция смены пароля будет реализована позже");
        });

        // Logout button
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        ProfileRepository repository = new ProfileRepository(requireContext());
        setLoading(true);

        new Thread(() -> {
//            try {
            // Update token before request
//            Log.i("ProfileFragment", "Loading profile. Token before the request: " + profileApi.getApiClient().getBasePath() + " " + profileApi.getApiClient().getHttpClient().toString());
//            String token = preferencesHelper.getToken();
//            if (token != null) {
//                profileApi.getApiClient().setApiKey("Bearer " + token);
//            }
            repository.getProfile(new DataCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile data) {
                    requireActivity().runOnUiThread(() -> onProfileLoaded(data));
                }

                @Override
                public void onError(String e) {
                    Log.e("ProfileFragment", "Error loading profile: " + e);
                    requireActivity().runOnUiThread(() -> onProfileLoadError(e));
                }
            });
//            } catch (ApiException e) {
//                Log.e("ProfileFragment", "Error loading profile: " + e.getMessage());
//                requireActivity().runOnUiThread(() -> onProfileLoadError(e));
//            }
        }).start();
    }

    private void onProfileLoaded(UserProfile profile) {
        currentProfile = profile;
        originalProfile = cloneProfile(profile);
        storeOriginalValues();

        populateUI(profile);
        setLoading(false);
        setFieldsEnabled(true);
        btnSaveChanges.reset();
    }

    private void onProfileLoadError(String errorMessage) {
        setLoading(false);
//        String errorMessage = "Ошибка загрузки профиля";
//        if (e.getCode() == 401) {
//        errorMessage = "Сессия истекла. Войдите снова.";
        logout();
//        } else if (e.getResponseBody() != null) {
//            try {
//                JSONObject json = new JSONObject(e.getResponseBody());
//                if (json.has("message")) {
//                    errorMessage = json.getString("message");
//                }
//            } catch (Exception ignored) {}
//        }
        ViewUtils.toast(requireView(), requireContext(), errorMessage);
        Log.e("ProfileFragment", errorMessage);
    }

    private void storeOriginalValues() {
        if (currentProfile != null) {
            originalName = currentProfile.getFullName() != null ? currentProfile.getFullName() : "";
            originalEmail = currentProfile.getEmail() != null ? currentProfile.getEmail() : "";
            originalPhone = currentProfile.getPhoneNumber() != null ? currentProfile.getPhoneNumber() : "";
        } else {
            originalName = "";
            originalEmail = "";
            originalPhone = "";
        }
    }

    private void populateUI(UserProfile profile) {
        if (profile == null) return;

        // Name
        String fullName = profile.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            tvName.setText(fullName);
            etName.setText(fullName);
        }

        // Email
        String email = profile.getEmail();
        if (email != null && !email.isEmpty()) {
            etEmail.setText(email);
        }

        // Phone
        String phone = profile.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            etPhone.setText(phone);
        }

        // Role
        if (profile.getRole() != null) {
            String roleDisplay = mapRoleToDisplay(profile.getRole());
            tvRole.setText(roleDisplay);
        }

        // Squad (placeholder as requested)
        tvSquad.setText("Отряд «Капельки»");

        // Avatar - using placeholder for now
        // TODO: Load avatar from URL when image loading library is added
        // if (profile.getAvatar() != null) { Glide.with(this).load(profile.getAvatar()).into(ivAvatar); }
    }

    private String mapRoleToDisplay(UserProfile.RoleEnum role) {
        switch (role) {
            case COUNSELOUR:
                return "Вожатый";
            case SENIOR_COUNSELOUR:
                return "Старший вожатый";
            case DIRECTOR:
                return "Директор";
            default:
                return role.getValue();
        }
    }

    private void checkForChanges() {
        String currentName = etName.getText() != null ? etName.getText().toString().trim() : "";
        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String currentPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        boolean hasChanges = !currentName.equals(originalName)
                || !currentEmail.equals(originalEmail)
                || !currentPhone.equals(originalPhone);

        btnSaveChanges.setHasChanges(hasChanges);
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        setLoading(true);
        btnSaveChanges.showLoading();
        setFieldsEnabled(false);

        // Build UserProfile with only changed fields
        UserProfile updateRequest = new UserProfile();
        updateRequest.setId(currentProfile.getId());

        String currentName = etName.getText() != null ? etName.getText().toString().trim() : "";
        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String currentPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

        boolean nameChanged = !currentName.equals(originalName);
        boolean emailChanged = !currentEmail.equals(originalEmail);
        boolean phoneChanged = !currentPhone.equals(originalPhone);

        if (nameChanged) updateRequest.setFullName(currentName);
        if (emailChanged) updateRequest.setEmail(currentEmail);
        if (phoneChanged) updateRequest.setPhoneNumber(currentPhone);

        // If nothing actually changed (shouldn't happen since button is disabled), just reset
        if (!nameChanged && !emailChanged && !phoneChanged) {
            requireActivity().runOnUiThread(() -> {
                btnSaveChanges.hideLoading(false);
                setLoading(false);
                setFieldsEnabled(true);
            });
            return;
        }

        new Thread(() -> {
            ProfileRepository repository = new ProfileRepository(requireContext());
            repository.setProfile(updateRequest, new DataCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile data) {
                    requireActivity().runOnUiThread(() -> onProfileSaved(data, nameChanged, emailChanged, phoneChanged));
                }

                @Override
                public void onError(String e) {
                    requireActivity().runOnUiThread(() -> onProfileSaveError(e));
                }
            });
        }).start();
    }

    private void onProfileSaved(UserProfile updatedProfile, boolean nameChanged, boolean emailChanged, boolean phoneChanged) {
        // Update current profile with saved data
        if (nameChanged) currentProfile.setFullName(updatedProfile.getFullName());
        if (emailChanged) currentProfile.setEmail(updatedProfile.getEmail());
        if (phoneChanged) currentProfile.setPhoneNumber(updatedProfile.getPhoneNumber());

        // Update original values to match saved state
        storeOriginalValues();

        // Update UI
        populateUI(currentProfile);

        // Reset button state
        btnSaveChanges.hideLoading(false);
        setLoading(false);
        setFieldsEnabled(true);

        ViewUtils.toast(requireView(), requireContext(), "Профиль успешно обновлён");
    }

    private void onProfileSaveError(String errorMessage) {
        btnSaveChanges.hideLoading(true); // Keep changes since save failed
        setLoading(false);
        setFieldsEnabled(true);

//        String errorMessage = "Ошибка сохранения";
//        if (e.getCode() == 401) {
//            errorMessage = "Сессия истекла. Войдите снова.";
//            logout();
//        } else if (e.getResponseBody() != null) {
//            try {
//                JSONObject json = new JSONObject(e.getResponseBody());
//                if (json.has("message")) {
//                    errorMessage = json.getString("message");
//                }
//            } catch (Exception ignored) {}
//        }
        ViewUtils.toast(requireView(), requireContext(), errorMessage);
    }

    private void logout() {
        preferencesHelper.clear();
        NavigationHelper.goToAuth(requireContext());
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            viewOverlay.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            viewOverlay.setVisibility(View.GONE);
        }
    }

    private void setFieldsEnabled(boolean enabled) {
        etName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPhone.setEnabled(enabled);
        btnChangePassword.setEnabled(enabled);
        btnLogout.setEnabled(enabled);
    }

    private UserProfile cloneProfile(UserProfile profile) {
        if (profile == null) return null;
        UserProfile clone = new UserProfile();
        clone.setId(profile.getId());
        clone.setFullName(profile.getFullName());
        clone.setEmail(profile.getEmail());
        clone.setPhoneNumber(profile.getPhoneNumber());
        clone.setRole(profile.getRole());
        clone.setAvatar(profile.getAvatar());
        return clone;
    }
}
