package com.example.peer2peer.fragments; // Ensure this matches your package

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import java.util.Locale;
// Removed: android.text.Editable; (Not used in this fragment)
// Removed: android.text.TextWatcher; (Not used in this fragment)
import android.text.TextUtils;
import android.util.Log; // Keep for logging if you add any
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// Removed: android.widget.AdapterView; (Not used in this fragment)
// Removed: android.widget.ArrayAdapter; (Not used in this fragment)
// Removed: android.widget.Spinner; (Not used in this fragment)
import android.widget.TextView;


import com.example.peer2peer.R;
import com.example.peer2peer.viewmodels.TutorProfileViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// Removed: import java.util.Objects; // (Not used in this fragment)

public class TutorProfileStep5Fragment extends Fragment {

    private static final String TAG = "TutorProfileStep5Frag";

    // Declare UI Elements
    private TextView textReviewName, textReviewEmail, textReviewStudentId, textReviewGender, textReviewRace;
    private TextView textReviewYear, textReviewQualifications, textReviewModules;
    private TextView textReviewLanguages, textReviewBio, textReviewRate;
    private TextView textReviewIdDocFilename, textReviewProofDocFilename, textReviewRecordDocFilename;

    private TutorProfileViewModel viewModel;

    public TutorProfileStep5Fragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TutorProfileViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_profile_step5_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        findViews(view);
        setupObservers();
    }

    private void findViews(@NonNull View view) {
        textReviewName = view.findViewById(R.id.text_review_name);
        textReviewEmail = view.findViewById(R.id.text_review_email);
        textReviewStudentId = view.findViewById(R.id.text_review_student_id);
        textReviewGender = view.findViewById(R.id.text_review_gender);
        textReviewRace = view.findViewById(R.id.text_review_race);

        textReviewYear = view.findViewById(R.id.text_review_year);
        textReviewQualifications = view.findViewById(R.id.text_review_qualifications);
        textReviewModules = view.findViewById(R.id.text_review_modules); // This is the one

        textReviewLanguages = view.findViewById(R.id.text_review_languages);
        textReviewBio = view.findViewById(R.id.text_review_bio);
        textReviewRate = view.findViewById(R.id.text_review_rate);

        textReviewIdDocFilename = view.findViewById(R.id.text_review_id_doc_filename);
        textReviewProofDocFilename = view.findViewById(R.id.text_review_proof_doc_filename);
        textReviewRecordDocFilename = view.findViewById(R.id.text_review_record_doc_filename);
    }

    private void setupObservers() {
        viewModel.getFirstName().observe(getViewLifecycleOwner(), firstName -> updateFullName());
        viewModel.getSurname().observe(getViewLifecycleOwner(), surname -> updateFullName());
        viewModel.getEmail().observe(getViewLifecycleOwner(), data -> setText(textReviewEmail, data));
        viewModel.getStudentId().observe(getViewLifecycleOwner(), data -> setText(textReviewStudentId, data));
        viewModel.getGender().observe(getViewLifecycleOwner(), data -> setText(textReviewGender, data));
        viewModel.getRace().observe(getViewLifecycleOwner(), data -> setText(textReviewRace, data));

        viewModel.getYearOfStudy().observe(getViewLifecycleOwner(), data -> setText(textReviewYear, data));
        viewModel.getQualifications().observe(getViewLifecycleOwner(), data -> setText(textReviewQualifications, data));

        // --- CORRECTED: Handle List<String> for modulesToTutor ---
        viewModel.getModulesToTutor().observe(getViewLifecycleOwner(), modulesList -> {
            String modulesText = getString(R.string.not_provided); // Default text
            if (modulesList != null && !modulesList.isEmpty()) {
                // Optionally sort if needed, though typically display order matches input order or desired order
                // List<String> sortedModules = new ArrayList<>(modulesList);
                // Collections.sort(sortedModules);
                // modulesText = TextUtils.join(", ", sortedModules);
                modulesText = TextUtils.join(", ", modulesList); // Join the list into a comma-separated string
            }
            setText(textReviewModules, modulesText);
        });
        // --- END CORRECTION ---

        viewModel.getTutoringLanguages().observe(getViewLifecycleOwner(), languagesList -> { // Renamed for clarity
            String languagesText = getString(R.string.not_provided);
            if (languagesList != null && !languagesList.isEmpty()) {
                List<String> sortedLanguages = new ArrayList<>(languagesList);
                Collections.sort(sortedLanguages);
                languagesText = TextUtils.join(", ", sortedLanguages);
            }
            setText(textReviewLanguages, languagesText);
        });
        viewModel.getBio().observe(getViewLifecycleOwner(), data -> setText(textReviewBio, data));
        viewModel.getHourlyRate().observe(getViewLifecycleOwner(), data -> {
            String rateDisplay = getString(R.string.not_provided);
            if (!TextUtils.isEmpty(data)) {
                try {
                    // Ensure rate is parsed as double before formatting
                    double rateValue = Double.parseDouble(data);
                    rateDisplay = String.format(Locale.getDefault(), "R %.2f", rateValue);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse hourly rate for display: " + data, e);
                    rateDisplay = "R " + data + " (Invalid)"; // Indicate it's not a valid number
                }
            }
            setText(textReviewRate, rateDisplay);
        });

        viewModel.getIdDocumentFilename().observe(getViewLifecycleOwner(), data -> setText(textReviewIdDocFilename, data));
        viewModel.getProofRegistrationFilename().observe(getViewLifecycleOwner(), data -> setText(textReviewProofDocFilename, data));
        viewModel.getAcademicRecordFilename().observe(getViewLifecycleOwner(), data -> setText(textReviewRecordDocFilename, data));
    }

    private void updateFullName() {
        String firstName = viewModel.getFirstName().getValue();
        String surname = viewModel.getSurname().getValue();
        String fullName = "";
        if (!TextUtils.isEmpty(firstName)) {
            fullName += firstName;
        }
        if (!TextUtils.isEmpty(surname)) {
            if (!fullName.isEmpty()) {
                fullName += " ";
            }
            fullName += surname;
        }
        setText(textReviewName, TextUtils.isEmpty(fullName) ? getString(R.string.not_provided) : fullName);
    }

    private void setText(TextView textView, String data) {
        if (textView != null) {
            textView.setText(TextUtils.isEmpty(data) ? getString(R.string.not_provided) : data);
        }
    }
}