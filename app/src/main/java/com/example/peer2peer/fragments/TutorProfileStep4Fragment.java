package com.example.peer2peer.fragments; // Ensure this matches your package

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider
import android.provider.OpenableColumns;
import android.text.TextUtils; // Import TextUtils
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peer2peer.R; // Ensure this matches your R file path
import com.example.peer2peer.viewmodels.TutorProfileViewModel; // Import ViewModel

public class TutorProfileStep4Fragment extends Fragment {

    // Declare UI Elements
    Button buttonSelectId, buttonSelectProof, buttonSelectRecord;
    TextView textIdFilename, textProofFilename, textRecordFilename;

    // --- ViewModel ---
    private TutorProfileViewModel viewModel;

    // ActivityResultLaunchers for each file type
    private ActivityResultLauncher<Intent> idPickerLauncher;
    private ActivityResultLauncher<Intent> proofPickerLauncher;
    private ActivityResultLauncher<Intent> recordPickerLauncher;

    private static final String TAG = "TutorProfileStep4Frag";

    public TutorProfileStep4Fragment() {} // Required empty public constructor

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Get the ViewModel scoped to the Activity ---
        viewModel = new ViewModelProvider(requireActivity()).get(TutorProfileViewModel.class);

        // Initialize launchers
        idPickerLauncher = createActivityResultLauncher(
                uri -> viewModel.setIdDocumentUri(uri),
                filename -> viewModel.setIdDocumentFilename(filename)
        );
        proofPickerLauncher = createActivityResultLauncher(
                uri -> viewModel.setProofRegistrationUri(uri),
                filename -> viewModel.setProofRegistrationFilename(filename)
        );
        recordPickerLauncher = createActivityResultLauncher(
                uri -> viewModel.setAcademicRecordUri(uri),
                filename -> viewModel.setAcademicRecordFilename(filename)
        );
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutor_profile_step4_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find Views
        buttonSelectId = view.findViewById(R.id.button_select_id);
        buttonSelectProof = view.findViewById(R.id.button_select_proof);
        buttonSelectRecord = view.findViewById(R.id.button_select_record);
        textIdFilename = view.findViewById(R.id.text_id_filename);
        textProofFilename = view.findViewById(R.id.text_proof_filename);
        textRecordFilename = view.findViewById(R.id.text_record_filename);

        // Set onClickListeners for buttons
        buttonSelectId.setOnClickListener(v -> openFilePicker(idPickerLauncher));
        buttonSelectProof.setOnClickListener(v -> openFilePicker(proofPickerLauncher));
        buttonSelectRecord.setOnClickListener(v -> openFilePicker(recordPickerLauncher));

        // Observe ViewModel to update displayed filenames
        setupObservers();
    }

    // Helper method to create a reusable ActivityResultLauncher
    // Updated to use two callbacks: one for URI, one for filename
    private ActivityResultLauncher<Intent> createActivityResultLauncher(
            UriSelectionCallback uriCallback, FilenameSelectionCallback filenameCallback) {

        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { // Using lambda for callback
                    Uri selectedUri = null;
                    String selectedFilename = null;

                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            selectedUri = uri;
                            selectedFilename = getFileName(uri);
                            Log.d(TAG, "File selected: " + selectedFilename + " URI: " + selectedUri);
                        } else {
                            Log.w(TAG, "File selection returned null URI.");
                        }
                    } else {
                        Log.d(TAG, "File selection cancelled or failed.");
                        // If cancelled, we keep the existing values in the ViewModel
                        // by not calling the callbacks with null
                        return; // Exit early if no valid selection
                    }
                    // Call callbacks to update ViewModel
                    uriCallback.onUriSelected(selectedUri);
                    filenameCallback.onFilenameSelected(selectedFilename);
                });
    }

    // Functional interfaces for the callbacks
    interface UriSelectionCallback { void onUriSelected(Uri uri); }
    interface FilenameSelectionCallback { void onFilenameSelected(String filename); }


    // Method to open the file picker Intent (remains the same)
    private void openFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow any file type initially
        String[] mimeTypes = {"application/pdf", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            launcher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching file picker", e);
            Toast.makeText(getContext(), "Cannot open file picker.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to get filename from URI (remains the same)
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            if(getActivity() != null) {
                try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting filename from ContentResolver", e);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        // Limit filename length for display
        if (result != null && result.length() > 40) {
            result = result.substring(0, 15) + "..." + result.substring(result.length() - 15);
        }
        return (result != null) ? result : "Selected File";
    }

    // Observe ViewModel -> Update UI Filenames
    private void setupObservers() {
        viewModel.getIdDocumentFilename().observe(getViewLifecycleOwner(), filename -> {
            textIdFilename.setText(TextUtils.isEmpty(filename) ? "" : filename);
        });
        viewModel.getProofRegistrationFilename().observe(getViewLifecycleOwner(), filename -> {
            textProofFilename.setText(TextUtils.isEmpty(filename) ? "" : filename);
        });
        viewModel.getAcademicRecordFilename().observe(getViewLifecycleOwner(), filename -> {
            textRecordFilename.setText(TextUtils.isEmpty(filename) ? "" : filename);
        });
    }

    // --- URIs needed for upload later (Get directly from ViewModel now) ---
    // public Uri getIdDocumentUri() { return viewModel.getIdDocumentUri().getValue(); }
    // public Uri getProofRegistrationUri() { return viewModel.getProofRegistrationUri().getValue(); }
    // public Uri getAcademicRecordUri() { return viewModel.getAcademicRecordUri().getValue(); }

    // TODO: Implement isDataValid() method later (check if all 3 URIs in ViewModel are not null?)
    // public boolean isDataValid() { ... }
}