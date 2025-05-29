package com.example.peer2peer.fragments; // Ensure this matches your package

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.DialogInterface;

import com.example.peer2peer.R;
import com.example.peer2peer.viewmodels.TutorProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;

import java.util.ArrayList; // Import ArrayList
import java.util.Arrays; // Import Arrays
import java.util.Collections; // Import Collections
import java.util.List; // Import List

public class TutorProfileStep3Fragment extends Fragment {

    private static final String TAG = "TutorProfileStep3Frag";

    // UI Elements
    private TextView textSelectLanguages;
    private TextInputEditText editTextBio;
    private TextInputEditText editTextRate;

    // ViewModel
    private TutorProfileViewModel viewModel;

    // Dialog related variables
    private String[] allLanguagesArray;
    private boolean[] selectedLanguagesBooleanArray;
    private List<String> currentSelectedLanguages = new ArrayList<>(); // Initialize here

    public TutorProfileStep3Fragment() {} // Required empty public constructor

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TutorProfileViewModel.class);

        // Load language array safely
        try {
            List<String> languageList = new ArrayList<>(Arrays.asList(requireContext().getResources().getStringArray(R.array.language_array)));
            // Remove prompt if it exists
            if (!languageList.isEmpty() && languageList.get(0).toLowerCase().contains("select")) {
                languageList.remove(0);
            }
            allLanguagesArray = languageList.toArray(new String[0]);
            selectedLanguagesBooleanArray = new boolean[allLanguagesArray.length];
            Log.d(TAG, "Languages loaded for dialog: " + Arrays.toString(allLanguagesArray));
        } catch (Exception e) {
            Log.e(TAG, "Error loading language array from resources", e);
            Toast.makeText(requireContext(), "Error loading languages", Toast.LENGTH_SHORT).show();
            allLanguagesArray = new String[0];
            selectedLanguagesBooleanArray = new boolean[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_profile_step3_tutoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textSelectLanguages = view.findViewById(R.id.text_select_languages);
        editTextBio = view.findViewById(R.id.edit_text_profile_bio);
        editTextRate = view.findViewById(R.id.edit_text_profile_rate);

        setupObservers();
        setupInputListeners();

        // Initialize display in case observer doesn't fire immediately (e.g., no data yet)
        updateLanguagePromptText();
    }

    private void setupObservers() {
        // --- CORRECTED: Observer uses getTutoringLanguages() ---
        viewModel.getTutoringLanguages().observe(getViewLifecycleOwner(), languages -> {
            Log.d(TAG, "Observer received languages: " + languages);
            currentSelectedLanguages = (languages != null) ? new ArrayList<>(languages) : new ArrayList<>(); // Use new list
            updateLanguagePromptText();
            updateSelectedBooleans(); // Sync boolean array after currentSelectedLanguages is updated
        });

        viewModel.getBio().observe(getViewLifecycleOwner(), bio -> {
            if (!TextUtils.equals(editTextBio.getText(), bio)) {
                editTextBio.setText(bio);
            }
        });
        viewModel.getHourlyRate().observe(getViewLifecycleOwner(), rate -> {
            if (!TextUtils.equals(editTextRate.getText(), rate)) {
                editTextRate.setText(rate);
            }
        });
    }

    private void setupInputListeners() {
        textSelectLanguages.setOnClickListener(v -> showLanguageSelectionDialog());

        editTextBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { viewModel.setBio(s.toString()); }
        });

        editTextRate.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { viewModel.setHourlyRate(s.toString()); }
        });
    }

    private void showLanguageSelectionDialog() {
        if (allLanguagesArray == null || allLanguagesArray.length == 0) {
            Toast.makeText(requireContext(), "Language list not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        updateSelectedBooleans(); // Sync before showing

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.dialog_title_languages);
        builder.setCancelable(false);

        builder.setMultiChoiceItems(allLanguagesArray, selectedLanguagesBooleanArray, (dialog, which, isChecked) -> {
            if (which >= 0 && which < selectedLanguagesBooleanArray.length) {
                selectedLanguagesBooleanArray[which] = isChecked;
            } else {
                Log.w(TAG, "Invalid index in multi-choice listener: " + which);
            }
        });

        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            List<String> newlySelectedLanguages = new ArrayList<>(); // Explicit type
            for (int i = 0; i < allLanguagesArray.length; i++) {
                if (selectedLanguagesBooleanArray[i]) {
                    newlySelectedLanguages.add(allLanguagesArray[i]);
                }
            }
            currentSelectedLanguages = newlySelectedLanguages; // Update local list
            // --- CORRECTED: Update ViewModel using setTutoringLanguages ---
            viewModel.setTutoringLanguages(new ArrayList<>(currentSelectedLanguages)); // Pass a new list
            Log.d(TAG, "Dialog OK. ViewModel updated with: " + currentSelectedLanguages);
        });

        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
            dialog.dismiss();
            Log.d(TAG, "Dialog Cancel.");
        });

        // Use getString() for the button text
        builder.setNeutralButton(requireContext().getString(R.string.dialog_clear_all), (dialogInterface, i) -> {
            Arrays.fill(selectedLanguagesBooleanArray, false);
            currentSelectedLanguages.clear();
            // --- CORRECTED: Update ViewModel using setTutoringLanguages ---
            viewModel.setTutoringLanguages(new ArrayList<String>()); // Explicit type <String>
            Log.d(TAG, "Dialog Clear All. ViewModel updated.");
            // Dialog dismisses automatically
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Helper to update the display TextView
    private void updateLanguagePromptText() {
        // Use requireContext().getString()
        if (currentSelectedLanguages == null || currentSelectedLanguages.isEmpty()) {
            textSelectLanguages.setText(requireContext().getString(R.string.languages_select_prompt));
        } else {
            Collections.sort(currentSelectedLanguages);
            textSelectLanguages.setText(TextUtils.join(", ", currentSelectedLanguages));
        }
    }

    // Helper to sync the boolean array with the current list
    private void updateSelectedBooleans() {
        if (allLanguagesArray == null || selectedLanguagesBooleanArray == null || currentSelectedLanguages == null) {
            Log.w(TAG,"Cannot update booleans, array(s) are null during sync.");
            return; // Exit if arrays are null
        }
        // Ensure lengths match before proceeding
        if (allLanguagesArray.length != selectedLanguagesBooleanArray.length) {
            Log.e(TAG, "Array length mismatch! allLanguages=" + allLanguagesArray.length + ", selectedBooleans=" + selectedLanguagesBooleanArray.length);
            // Attempt to re-initialize boolean array if possible
            selectedLanguagesBooleanArray = new boolean[allLanguagesArray.length];
            Log.d(TAG,"Reinitialized boolean array due to length mismatch.");
        }

        Arrays.fill(selectedLanguagesBooleanArray, false); // Reset
        for (int i = 0; i < allLanguagesArray.length; i++) {
            // Ensure index is valid before accessing boolean array
            if (currentSelectedLanguages.contains(allLanguagesArray[i]) && i < selectedLanguagesBooleanArray.length) {
                selectedLanguagesBooleanArray[i] = true;
            }
        }
        Log.d(TAG, "Synced booleans: " + Arrays.toString(selectedLanguagesBooleanArray));
    }

    // TODO: Implement isDataValid() method later
}