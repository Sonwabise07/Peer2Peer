package com.example.peer2peer.fragments; // Ensure this matches your package

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.peer2peer.R;
import com.example.peer2peer.viewmodels.TutorProfileViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList; // For creating list from array
import java.util.Arrays;    // For Arrays.asList
import java.util.List;      // For List interface
import java.util.stream.Collectors; // For joining list (API 24+) or use TextUtils.join

public class TutorProfileStep2Fragment extends Fragment {

    private static final String TAG = "TutorProfileStep2Frag"; // For logging

    private Spinner spinnerYearOfStudy;
    private TextInputEditText editTextQualifications;
    private TextInputEditText editTextModules;

    private TutorProfileViewModel viewModel;
    private boolean isUpdatingModulesFromVM = false; // Flag to prevent TextWatcher loop

    public TutorProfileStep2Fragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TutorProfileViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_profile_step2_academic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerYearOfStudy = view.findViewById(R.id.spinner_year_of_study);
        editTextQualifications = view.findViewById(R.id.edit_text_profile_qualifications);
        editTextModules = view.findViewById(R.id.edit_text_profile_modules);

        setupSpinners();
        setupObservers();
        setupInputListeners();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> yearAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.year_of_study_array, android.R.layout.simple_spinner_item);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYearOfStudy.setAdapter(yearAdapter);
    }

    private void setupObservers() {
        viewModel.getYearOfStudy().observe(getViewLifecycleOwner(), year -> {
            setSpinnerSelection(spinnerYearOfStudy, year);
        });
        viewModel.getQualifications().observe(getViewLifecycleOwner(), qualifications -> {
            if (!TextUtils.equals(editTextQualifications.getText(), qualifications)) {
                editTextQualifications.setText(qualifications);
            }
        });

        // --- MODIFIED: Observe List<String> and convert to comma-separated String for EditText ---
        viewModel.getModulesToTutor().observe(getViewLifecycleOwner(), modulesList -> {
            isUpdatingModulesFromVM = true; // Set flag
            String modulesString = "";
            if (modulesList != null && !modulesList.isEmpty()) {
                // For API 24+
                // modulesString = modulesList.stream().collect(Collectors.joining(", "));
                // For compatibility (or if you prefer TextUtils):
                modulesString = TextUtils.join(", ", modulesList);
            }
            if (!TextUtils.equals(editTextModules.getText().toString(), modulesString)) {
                editTextModules.setText(modulesString);
                // Optionally move cursor to the end
                editTextModules.setSelection(modulesString.length());
            }
            // A short delay before resetting flag, to ensure TextWatcher doesn't react to this programmatic change
            editTextModules.postDelayed(() -> isUpdatingModulesFromVM = false, 100);
        });
    }

    private void setupInputListeners() {
        spinnerYearOfStudy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    viewModel.setYearOfStudy(parent.getItemAtPosition(position).toString());
                } else {
                    viewModel.setYearOfStudy(null);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { viewModel.setYearOfStudy(null); }
        });

        editTextQualifications.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { viewModel.setQualifications(s.toString()); }
        });

        // --- MODIFIED: Convert comma-separated String from EditText to List<String> for ViewModel ---
        editTextModules.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingModulesFromVM) { // If change is from ViewModel, ignore
                    return;
                }
                String modulesInputString = s.toString().trim();
                List<String> modulesList = new ArrayList<>();
                if (!TextUtils.isEmpty(modulesInputString)) {
                    // Split by comma, trim whitespace from each module, and convert to uppercase
                    String[] modulesArray = modulesInputString.split("\\s*,\\s*");
                    for (String module : modulesArray) {
                        if (!module.trim().isEmpty()) {
                            modulesList.add(module.trim().toUpperCase()); // Standardize
                        }
                    }
                }
                // Only update ViewModel if the list content has actually changed
                // This requires comparing lists, which can be tricky. A simpler approach is to always set,
                // but LiveData will only notify observers if the new value is actually different.
                // For robust list comparison, you might need to check if `viewModel.getModulesToTutor().getValue()`
                // is different from `modulesList`.
                viewModel.setModulesToTutor(modulesList);
                Log.d(TAG, "editTextModules changed, ViewModel updated with list: " + modulesList);
            }
        });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner == null || spinner.getAdapter() == null) return;
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        int positionToSelect = 0;
        if (value != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (TextUtils.equals(adapter.getItem(i), value)) {
                    positionToSelect = i;
                    break;
                }
            }
        }
        if (spinner.getSelectedItemPosition() != positionToSelect) {
            spinner.setSelection(positionToSelect);
        }
    }
}