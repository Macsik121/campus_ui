package com.sfedu.campus.profile;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.R;
import com.sfedu.campus.data.datasource.DataCallback;
import com.sfedu.campus.generated.api.ProfileApi;
import com.sfedu.campus.generated.invoker.ApiClient;
import com.sfedu.campus.generated.invoker.Configuration;
import com.sfedu.campus.generated.model.UserProfile;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;

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

        // Set input types via EditText, hints via TextInputLayout for proper floating label behavior (placeholder on focus only)
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        tilPhone.setHint("+7 (xxx) xxx-xx-xx");

        // Set email input type and basic white space filter
        etEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setFilters(new InputFilter[]{
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    if (Character.isWhitespace(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        });
    }

    private void setupListeners() {
        // General text watcher for changes in name
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Requirement 1: Email validation following the pattern and preventing wrong address to be written (via error display and button disabling)
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !isValidEmail(email)) {
                    tilEmail.setError("Неверный формат почты");
                } else {
                    tilEmail.setError(null);
                }
                checkForChanges();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Requirement 2: Phone field following the pattern +7 (xxx) xxx-xx-xx
        etPhone.addTextChangedListener(new PhoneTextWatcher(etPhone));
        // Still need to check for changes on phone field
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Save button
        btnSaveChanges.setOnClickListener(v -> saveProfile());

        // Change password button
        btnChangePassword.setOnClickListener(v -> {
            ViewUtils.toast(requireView(), requireContext(), "Функция смены пароля будет реализована позже");
        });

        // Logout button
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        ProfileRepository repository = new ProfileRepository(requireContext());
        setLoading(true);

        new Thread(() -> {
            repository.getProfile(new DataCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile data) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> onProfileLoaded(data));
                    }
                }

                @Override
                public void onError(String e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> onProfileLoadError(e));
                    }
                }
            });
        }).start();
    }

    private void onProfileLoaded(UserProfile profile) {
        currentProfile = profile;
        storeOriginalValues();

        populateUI(profile);
        setLoading(false);
        setFieldsEnabled(true);
        btnSaveChanges.reset();
    }

    private void onProfileLoadError(String errorMessage) {
        setLoading(false);
        logout();
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

        // Squad - get from PreferencesHelper (cached from SquadFragment)
        String squadTitle = preferencesHelper.getSquadTitle();
        if (squadTitle != null && !squadTitle.isEmpty()) {
            tvSquad.setText("Отряд «" + squadTitle + "»");
        } else {
            tvSquad.setText("Отряд не назначен");
        }
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

        // Requirement 1 & 2 Validation: email must be valid, phone must be empty or complete (18 chars with mask)
        boolean isEmailValid = isValidEmail(currentEmail);
        boolean isNameValid = !currentName.isEmpty();
        boolean isPhoneValid = currentPhone.isEmpty() || currentPhone.length() == 18;

        btnSaveChanges.setHasChanges(hasChanges && isEmailValid && isNameValid && isPhoneValid);
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void saveProfile() {
        if (currentProfile == null) return;

        String currentEmail = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        if (!isValidEmail(currentEmail)) {
            tilEmail.setError("Неверный формат почты");
            return;
        }

        String currentPhone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        if (!currentPhone.isEmpty() && currentPhone.length() != 18) {
            tilPhone.setError("Введите номер полностью");
            return;
        }

        setLoading(true);
        btnSaveChanges.showLoading();
        setFieldsEnabled(false);

        // Build UserProfile with current fields
        UserProfile updateRequest = new UserProfile();
        updateRequest.setId(currentProfile.getId());
        updateRequest.setFullName(etName.getText().toString().trim());
        updateRequest.setEmail(currentEmail);
        updateRequest.setPhoneNumber(currentPhone);

        new Thread(() -> {
            ProfileRepository repository = new ProfileRepository(requireContext());
            repository.setProfile(updateRequest, new DataCallback<UserProfile>() {
                @Override
                public void onSuccess(UserProfile data) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> onProfileSaved(data));
                    }
                }

                @Override
                public void onError(String e) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> onProfileSaveError(e));
                    }
                }
            });
        }).start();
    }

    private void onProfileSaved(UserProfile updatedProfile) {
        currentProfile = updatedProfile;
        storeOriginalValues();
        populateUI(currentProfile);

        btnSaveChanges.hideLoading(false);
        setLoading(false);
        setFieldsEnabled(true);

        ViewUtils.toast(requireView(), requireContext(), "Профиль успешно обновлён");
    }

    private void onProfileSaveError(String errorMessage) {
        btnSaveChanges.hideLoading(true);
        setLoading(false);
        setFieldsEnabled(true);
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

    /**
     * Requirement 2: TextWatcher for Russian phone number formatting: +7 (xxx) xxx-xx-xx
     */
    private static class PhoneTextWatcher implements TextWatcher {
        private final TextInputEditText editText;
        private boolean isUpdating = false;

        public PhoneTextWatcher(TextInputEditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (isUpdating) return;
            isUpdating = true;

            String digits = s.toString().replaceAll("\\D", "");

            // If user typed 7 or 8 at the start, treat it as the country code and skip it
            if (digits.startsWith("7") || digits.startsWith("8")) {
                digits = digits.substring(1);
            }

            // Limit to 10 digits (after country code)
            if (digits.length() > 10) {
                digits = digits.substring(0, 10);
            }

            StringBuilder formatted = new StringBuilder();
            if (digits.length() > 0) {
                formatted.append("+7 (");
                formatted.append(digits.substring(0, Math.min(digits.length(), 3)));
                
                if (digits.length() >= 3) {
                    formatted.append(") ");
                }
                
                if (digits.length() > 3) {
                    formatted.append(digits.substring(3, Math.min(digits.length(), 6)));
                }
                
                if (digits.length() >= 6) {
                    formatted.append("-");
                }
                
                if (digits.length() > 6) {
                    formatted.append(digits.substring(6, Math.min(digits.length(), 8)));
                }
                
                if (digits.length() >= 8) {
                    formatted.append("-");
                }
                
                if (digits.length() > 8) {
                    formatted.append(digits.substring(8, Math.min(digits.length(), 10)));
                }
            }

            editText.setText(formatted.toString());
            editText.setSelection(formatted.length());
            isUpdating = false;
        }
    }
}
