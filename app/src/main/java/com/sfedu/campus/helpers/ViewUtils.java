package com.sfedu.campus.helpers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModel;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.sfedu.campus.R;
import com.sfedu.campus.auth.AuthViewModel;
import com.sfedu.campus.auth.RegViewModel;

public class ViewUtils {
    public static int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Resources.getSystem().getDisplayMetrics()
        );
    }
    public static void setButtonCornerRadius(@NonNull MaterialButton button, float radiusInDp) {
        int radiusInPx = ViewUtils.dpToPx(radiusInDp);
        button.setCornerRadius(radiusInPx);
    }
    public static void toast(View view, Context context, String text) {
//        activity.runOnUiThread(() -> {
//            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
//        });

        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        TextView snackbarText = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);

        Drawable icon = ContextCompat.getDrawable(context, R.drawable.campus_logo);
        if (icon != null) {
            icon.setBounds(0, 0, 58, 58);

            snackbarText.setCompoundDrawables(icon, null, null, null);
            snackbarText.setCompoundDrawablePadding(16);
//            ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BASELINE);

//            spannableString.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//            snackbarText.setText(spannableString);
        }

        snackbar.show();
    }
    public static void setBGColor(@NonNull View el, int clr) {
        // pass Color.WHITE or Color.TRANSPARENT as clr
        el.setBackgroundColor(clr);
    }
    public static void bindTextInputLayoutAuth(TextInputLayout el, AuthViewModel viewModel, String fieldKey) {
        EditText editText = el.getEditText();
        if (editText == null) return;

        // Убираем старый, если есть, чтобы не вешать несколько слушателей
        // (обычно во Фрагменте вызывается один раз).

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 1. Сбрасываем ошибку при вводе
                el.setError(null);

                // 2. Сохраняем данные в ViewModel в зависимости от переданного ключа
                String value = s.toString();
                switch (fieldKey) {
                    case "email":
                        viewModel.setEmail(value);
                        break;
                    case "password":
                        viewModel.setPassword(value);
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    public static void bindTextInputLayoutReg(TextInputLayout el, RegViewModel viewModel, String fieldKey) {
        EditText editText = el.getEditText();
        if (editText == null) return;

        // Убираем старый, если есть, чтобы не вешать несколько слушателей
        // (обычно во Фрагменте вызывается один раз).

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 1. Сбрасываем ошибку при вводе
                el.setError(null);

                // 2. Сохраняем данные в ViewModel в зависимости от переданного ключа
                String value = s.toString();
                switch (fieldKey) {
                    case "email":
                        viewModel.setEmail(value);
                        break;
                    case "password":
                        viewModel.setPassword(value);
                        break;
                    case "name":
                        viewModel.setName(value);
                        break;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
