package com.sfedu.campus.auth;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.helpers.ApiClient;
import com.sfedu.campus.helpers.NavigationHelper;
import com.sfedu.campus.helpers.PreferencesHelper;
import com.sfedu.campus.helpers.ViewUtils;
import com.sfedu.campus.R;
import com.sfedu.campus.models.server_responses.RegisterResponse;

import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RegisterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RegisterFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RegisterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RegisterFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RegisterFragment newInstance(String param1, String param2) {
        RegisterFragment fragment = new RegisterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean validateRegisterForm(TextInputLayout nameLayout, TextInputLayout emailLayout, TextInputLayout passwordLayout) {
        String name = nameLayout.getEditText().getText().toString().trim();
        String email = emailLayout.getEditText().getText().toString().trim();
        String password = passwordLayout.getEditText().getText().toString().trim();

        boolean isValid = true;

        // Регулярка для спецсимволов (любой символ, кроме букв и цифр)
        Pattern specialCharPattern = Pattern.compile("[^a-zA-Z0-9]");

        // 1. Имя не пусто
        if (name.isEmpty()) {
            nameLayout.setError("Введите ваше имя");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        // 2. Почта не пуста
        if (email.isEmpty()) {
            emailLayout.setError("Введите адрес электронной почты");
            isValid = false;
        }
        // 4. Почта соответствует регулярному выражению
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Введите корректный email");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // 3. Пароль не пуст
        if (password.isEmpty()) {
            passwordLayout.setError("Введите пароль");
            isValid = false;
        }
        // 5. Пароль не менее 6 символов
        else if (password.length() < 6) {
            passwordLayout.setError("Пароль должен содержать минимум 6 символов");
            isValid = false;
        }
        // 6. Пароль имеет хотя бы 1 спец. символ
        else if (!specialCharPattern.matcher(password).find()) {
            passwordLayout.setError("Пароль должен содержать хотя бы один спецсимвол (!@#$%^&* и т.д.)");
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
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // setting the color of the active Register form on buttons those switches the forms
        ViewUtils.setBGColor(getActivity().findViewById(R.id.btn_to_login), Color.TRANSPARENT);
        ViewUtils.setBGColor(getActivity().findViewById(R.id.btn_to_register), Color.WHITE);

        ViewUtils.setButtonCornerRadius(view.findViewById(R.id.btn_signup), 14);

        // setting a listener to changing the text of fields: name, email, password
        // name
        // 1. Получаем ViewModel (общий для Activity)
        RegViewModel viewModel = new ViewModelProvider(requireActivity()).get(RegViewModel.class);

        TextInputLayout nameLayout = view.findViewById(R.id.signup_name_layout);
        TextInputLayout emailLayout = view.findViewById(R.id.signup_email_layout);
        TextInputLayout passLayout = view.findViewById(R.id.signup_password_layout);

        // 3. Биндим через наш новый метод
        ViewUtils.bindTextInputLayoutReg(nameLayout, viewModel, "name");
        ViewUtils.bindTextInputLayoutReg(emailLayout, viewModel, "email");
        ViewUtils.bindTextInputLayoutReg(passLayout, viewModel, "password");

        if (viewModel.getName().getValue() != null) {
            nameLayout.getEditText().setText(viewModel.getName().getValue());
        }
        if (viewModel.getEmail().getValue() != null) {
            emailLayout.getEditText().setText(viewModel.getEmail().getValue());
        }
        if (viewModel.getPassword().getValue() != null) {
            passLayout.getEditText().setText(viewModel.getPassword().getValue());
        }

        MaterialButton btn_signup = view.findViewById(R.id.btn_signup);
        btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // validating certain form checks. the ones from the requirements to the project
                if (!validateRegisterForm(nameLayout, emailLayout, passLayout)) {
                    return; // Не отправляем запрос
                }

                ApiClient.getInstance().register(
                    viewModel.getName().getValue(),
                    viewModel.getEmail().getValue(),
                    viewModel.getPassword().getValue(),
                    new ApiClient.ApiCallback<RegisterResponse>() {
                        public void onFailure(String e) {
                            ViewUtils.toast(view, requireContext(), e);
                        }
                        public void onSuccess(RegisterResponse res) {
                                new PreferencesHelper(requireContext()).saveToken(res.getToken());
                                NavigationHelper.goToMain(requireContext());
                            }
                    },
                    requireContext()
                );
            }
        });

        return view;
    }
}
