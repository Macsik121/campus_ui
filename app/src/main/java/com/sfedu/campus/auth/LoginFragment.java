package com.sfedu.campus.auth;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.helpers.ApiClient;
import com.sfedu.campus.R;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;
import com.sfedu.campus.models.server_responses.LoginResponse;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean validateLoginForm(TextInputLayout emailLayout, TextInputLayout passwordLayout) {
        String email = emailLayout.getEditText().getText().toString().trim();
        String password = passwordLayout.getEditText().getText().toString().trim();

        boolean isValid = true;

        // 1. Почта не пуста
        if (email.isEmpty()) {
            emailLayout.setError("Введите адрес электронной почты");
            isValid = false;
        }
        // 3. Валидация правильности почты
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Введите корректный email (например, name@domain.ru)");
            isValid = false;
        } else {
            emailLayout.setError(null); // Сбрасываем ошибку, если всё ок
        }

        // 2. Пароль не пуст
        if (password.isEmpty()) {
            passwordLayout.setError("Введите пароль");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        ViewUtils.setBGColor(getActivity().findViewById(R.id.btn_to_login), Color.WHITE);
        ViewUtils.setBGColor(getActivity().findViewById(R.id.btn_to_register), Color.TRANSPARENT);

        ViewUtils.setButtonCornerRadius(view.findViewById(R.id.btn_login), 14);

        // 1. Получаем ViewModel (общий для Activity)
        AuthViewModel viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // 2. Находим TextInputLayout
        TextInputLayout emailLayout = view.findViewById(R.id.login_email_layout);
        TextInputLayout passLayout = view.findViewById(R.id.login_password_layout);

        // Восстанавливаем данные из ViewModel в поля
        if (viewModel.getEmail().getValue() != null) {
            emailLayout.getEditText().setText(viewModel.getEmail().getValue());
        }
        if (viewModel.getPassword().getValue() != null) {
            passLayout.getEditText().setText(viewModel.getPassword().getValue());
        }

        ViewUtils.bindTextInputLayoutAuth(emailLayout, viewModel, "email");
        ViewUtils.bindTextInputLayoutAuth(passLayout, viewModel, "password");

        MaterialButton login_btn = view.findViewById(R.id.btn_login);
        login_btn.setOnClickListener(v -> {
//                ViewUtils.toast();
            if (!validateLoginForm(emailLayout, passLayout)) {
                return; // Если валидация не пройдена - выходим, запрос НЕ отправляется
            }
            ApiClient.getInstance().login(
                viewModel.getEmail().getValue(),
                viewModel.getPassword().getValue(),
                new ApiClient.ApiCallback<LoginResponse>() {
                    @Override
                    public void onFailure(String errorMessage) {
                        ViewUtils.toast(view, requireContext(), errorMessage);
                    }
                    @Override
                    public void onSuccess(LoginResponse data) {
                        new PreferencesHelper(requireContext()).saveToken(data.getToken());
                        NavigationHelper.goToMain(getContext());
                    }
                }
            );
        });

        return view;
    }
}