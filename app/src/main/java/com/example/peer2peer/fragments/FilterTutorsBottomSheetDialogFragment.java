package com.example.peer2peer.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar; // Import RatingBar
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.peer2peer.R; // Make sure R is imported
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

public class FilterTutorsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public static final String TAG = "FilterTutorsDialog";

    // Updated interface
    public interface FilterListener {
        // Removed module, added rating
        void onFiltersApplied(Float maxRate, String language, Float minRating);
        void onFiltersReset();
    }

    private FilterListener listener;

    // UI Elements - Removed module, added ratingBar
    private Slider sliderRate;
    private TextInputEditText editTextLanguage;
    private RatingBar ratingBarFilter; // Added
    private Button buttonApply;
    private Button buttonReset;

    // Store initial/current values - Removed module, added rating
    private float initialMaxRate = 500f;
    private String initialLanguage = "";
    private float initialMinRating = 0f; // Added

    // Updated newInstance method
    public static FilterTutorsBottomSheetDialogFragment newInstance(float currentMaxRate, String currentLanguage, float currentMinRating) {
        FilterTutorsBottomSheetDialogFragment fragment = new FilterTutorsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        // Clamp rate to valid slider range before putting in args
        args.putFloat("maxRate", Math.max(0f, Math.min(500f, currentMaxRate)));
        args.putString("language", currentLanguage);
        args.putFloat("minRating", currentMinRating); // Store rating
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_filter_tutors, container, false);

        // Find views - Removed module, added ratingBar
        sliderRate = view.findViewById(R.id.sliderRateFilter);
        editTextLanguage = view.findViewById(R.id.editTextLanguageFilter);
        ratingBarFilter = view.findViewById(R.id.ratingBarFilter); // Find RatingBar
        buttonApply = view.findViewById(R.id.buttonApplyFilters);
        buttonReset = view.findViewById(R.id.buttonResetFilters);

        // Retrieve initial values if passed - Removed module, added rating
        if (getArguments() != null) {
            initialMaxRate = getArguments().getFloat("maxRate", 500f);
            initialLanguage = getArguments().getString("language", "");
            initialMinRating = getArguments().getFloat("minRating", 0f); // Get rating
        }

        // Set initial values in the UI - Removed module, added ratingBar
        if (sliderRate != null) sliderRate.setValue(initialMaxRate);
        editTextLanguage.setText(initialLanguage);
        if (ratingBarFilter != null) ratingBarFilter.setRating(initialMinRating); // Set initial rating

        // Set listeners
        buttonApply.setOnClickListener(v -> applyFilters());
        buttonReset.setOnClickListener(v -> resetFilters());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set listener
        try {
            if (getParentFragment() != null) {
                listener = (FilterListener) getParentFragment();
            } else {
                listener = (FilterListener) getActivity();
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "Calling Activity or Parent Fragment must implement FilterListener", e);
        } catch (Exception e) {
            Log.e(TAG, "Error setting listener", e);
        }
    }

    // Updated applyFilters
    private void applyFilters() {
        if (sliderRate == null || editTextLanguage == null || ratingBarFilter == null) { // Check ratingBar
            Log.e(TAG, "UI elements not initialized in applyFilters");
            dismissSafely();
            return;
        }

        float maxRate = sliderRate.getValue();
        String language = editTextLanguage.getText().toString().trim();
        float minRating = ratingBarFilter.getRating(); // Get rating

        if (listener != null) {
            // Pass updated filters, pass null if string is empty or float is default/max
            listener.onFiltersApplied(
                    (maxRate < 500f) ? maxRate : null, // Pass null if it's the max value
                    language.isEmpty() ? null : language,
                    (minRating > 0f) ? minRating : null // Pass null if rating is 0
            );
        } else {
            Log.w(TAG, "FilterListener is null in applyFilters");
        }
        dismissSafely();
    }

    // Updated resetFilters
    private void resetFilters() {
        if (sliderRate == null || editTextLanguage == null || ratingBarFilter == null) { // Check ratingBar
            Log.e(TAG, "UI elements not initialized in resetFilters");
            dismissSafely();
            return;
        }
        sliderRate.setValue(500f); // Reset to default max
        editTextLanguage.setText("");
        ratingBarFilter.setRating(0f); // Reset rating

        if (listener != null) {
            listener.onFiltersReset();
        } else {
            Log.w(TAG, "FilterListener is null in resetFilters");
        }
        dismissSafely();
    }

    // Helper to dismiss safely
    private void dismissSafely() {
        if (isAdded() && getActivity() != null) {
            dismiss();
        }
    }
}