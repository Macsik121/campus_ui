package com.sfedu.campus.profile;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

/**
 * Custom MaterialButton that acts as a "Save Changes" button.
 * Starts disabled (gray) and enables only when there are unsaved changes.
 * Shows loading state during API calls.
 */
public class DataEditWaitButton extends MaterialButton {

    private boolean hasChanges = false;
    private boolean isLoading = false;
    private String originalText;

    public DataEditWaitButton(@NonNull Context context) {
        super(context);
        init();
    }

    public DataEditWaitButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DataEditWaitButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        originalText = getText().toString();
        setEnabled(false);
        updateAppearance();
    }

    /**
     * Call when any tracked field changes.
     * @param hasChanges true if there are unsaved changes, false otherwise
     */
    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
        if (!isLoading) {
            setEnabled(hasChanges);
            updateAppearance();
        }
    }

    /**
     * Returns true if there are currently unsaved changes.
     */
    public boolean hasChanges() {
        return hasChanges;
    }

    /**
     * Shows loading state (spinner, disabled).
     */
    public void showLoading() {
        isLoading = true;
        setEnabled(false);
        setText("Сохранение...");
        // Use a simple approach - the button will show "Сохранение..." text
        // For a real spinner, you'd need a custom drawable or ProgressBar overlay
    }

    /**
     * Hides loading state, restores normal appearance.
     * @param hasChanges whether there are still unsaved changes after the operation
     */
    public void hideLoading(boolean hasChanges) {
        isLoading = false;
        this.hasChanges = hasChanges;
        setText(originalText);
        setEnabled(hasChanges);
        updateAppearance();
    }

    private void updateAppearance() {
        // The button style handles enabled/disabled colors automatically via Material theme
        // When disabled (no changes): gray/outlined appearance
        // When enabled (has changes): filled/colored appearance
    }

    /**
     * Reset to initial state (no changes, not loading).
     */
    public void reset() {
        hasChanges = false;
        isLoading = false;
        setEnabled(false);
        setIconResource(0);
        setText(originalText);
        updateAppearance();
    }
}