package com.example.peer2peer.fragments; // Make sure this package name is correct

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.example.peer2peer.R;
import com.example.peer2peer.viewmodels.TutorProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList; // Needed for ViewModel List typing if applicable later
import java.util.List; // Needed for ViewModel List typing if applicable later


public class TutorProfileStep1Fragment extends Fragment {

    // UI Elements
    private ImageView profileImageView;
    private Button selectImageButton;
    private TextView textViewFirstNameDisplay;
    private TextView textViewSurnameDisplay;
    private TextView textViewStudentNumber;
    private TextView textViewEmail;
    private Spinner spinnerGender;
    private Spinner spinnerRace;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    // ViewModel
    private TutorProfileViewModel viewModel;

    // Launcher
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private static final String TAG = "TutorProfileStep1Frag";

    public TutorProfileStep1Fragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TutorProfileViewModel.class);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "Image selected: " + imageUri);
                            viewModel.setProfileImageUri(imageUri);
                        } else {
                            Log.d(TAG, "Image selection returned null URI.");
                            Toast.makeText(requireContext(), "Could not get image URI", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Image selection cancelled or failed.");
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tutor_profile_step1_personal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // Find Views
        profileImageView = view.findViewById(R.id.image_profile_pic_preview);
        selectImageButton = view.findViewById(R.id.button_select_profile_image);
        textViewFirstNameDisplay = view.findViewById(R.id.text_profile_firstname_display);
        textViewSurnameDisplay = view.findViewById(R.id.text_profile_surname_display);
        textViewStudentNumber = view.findViewById(R.id.text_profile_student_number);
        textViewEmail = view.findViewById(R.id.text_profile_email);
        spinnerGender = view.findViewById(R.id.spinner_gender);
        spinnerRace = view.findViewById(R.id.spinner_race);

        setupSpinners();
        setupObservers(); // Setup observers first
        loadUserDataFromFirestore(); // Then load data
        setupInputListeners(); // Setup spinner listeners AFTER loading initial data
        setupClickListeners();
    }

    private void openImagePicker() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickIntent);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> raceAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.race_array, android.R.layout.simple_spinner_item);
        raceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRace.setAdapter(raceAdapter);
    }

    // Load data initially from Firestore and update ViewModel
    private void loadUserDataFromFirestore() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);
            Log.d(TAG, "Attempting to load user data from Firestore for userId: " + userId);

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!isAdded() || getView() == null || getActivity() == null) {
                    Log.w(TAG, "Firestore data loaded but Fragment/Activity is not attached.");
                    return;
                }

                if (documentSnapshot.exists()) {
                    Log.d(TAG, "User data found in Firestore. Populating ViewModel.");

                    String fetchedFirstName = documentSnapshot.getString("firstName");
                    String fetchedSurname = documentSnapshot.getString("surname");
                    String fetchedStudentId = documentSnapshot.getString("studentId");
                    String fetchedEmail = documentSnapshot.getString("email");
                    String fetchedGender = documentSnapshot.getString("gender");
                    String fetchedRace = documentSnapshot.getString("race");

                    Log.d(TAG, "Fetched firstName: [" + fetchedFirstName + "]");
                    Log.d(TAG, "Fetched surname: [" + fetchedSurname + "]");
                    Log.d(TAG, "Fetched studentId: [" + fetchedStudentId + "]");
                    Log.d(TAG, "Fetched email: [" + fetchedEmail + "]");
                    Log.d(TAG, "Fetched gender: [" + fetchedGender + "]");
                    Log.d(TAG, "Fetched race: [" + fetchedRace + "]");

                    // --- Update ViewModel ---
                    // Always update non-editable fields from Firestore
                    viewModel.setFirstName(fetchedFirstName);
                    viewModel.setSurname(fetchedSurname);
                    viewModel.setStudentId(fetchedStudentId);
                    viewModel.setEmail(fetchedEmail); // Assuming email is stored in Firestore

                    // --- MODIFIED: Only update editable fields if ViewModel is currently null ---
                    // This prevents overwriting user selections made during this wizard session
                    if (viewModel.getGender().getValue() == null) {
                        Log.d(TAG, "ViewModel gender is null, setting from Firestore: " + fetchedGender);
                        viewModel.setGender(fetchedGender);
                    } else {
                        Log.d(TAG, "ViewModel gender already has value, NOT overwriting from Firestore.");
                    }
                    if (viewModel.getRace().getValue() == null) {
                        Log.d(TAG, "ViewModel race is null, setting from Firestore: " + fetchedRace);
                        viewModel.setRace(fetchedRace);
                    } else {
                        Log.d(TAG, "ViewModel race already has value, NOT overwriting from Firestore.");
                    }
                    // --- END MODIFICATION ---

                    // TODO: Load profile image URL if stored

                    Log.d(TAG, "ViewModel population attempt complete.");

                } else {
                    Log.w(TAG, "No user document found in Firestore for userId: " + userId);
                    if(currentUser.getEmail() != null) {
                        viewModel.setEmail(currentUser.getEmail()); // Set email from Auth as fallback
                    }
                    Toast.makeText(requireContext(), "Could not load existing profile details.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                if (!isAdded() || getActivity() == null) return;
                Log.e(TAG, "Error loading user data from Firestore", e);
                Toast.makeText(requireContext(), "Error loading profile data.", Toast.LENGTH_SHORT).show();
                if(currentUser != null && currentUser.getEmail() != null) {
                    viewModel.setEmail(currentUser.getEmail()); // Fallback email
                }
            });
        } else {
            Log.e(TAG, "User not logged in when trying to load data.");
            Toast.makeText(requireContext(), "Error: User not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    // Observe changes in the ViewModel and update UI
    private void setupObservers() {
        viewModel.getProfileImageUri().observe(getViewLifecycleOwner(), uri -> {
            Log.d(TAG, "Observer - profileImageUri updated: [" + uri + "]");
            Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(profileImageView);
        });

        viewModel.getFirstName().observe(getViewLifecycleOwner(), firstName -> {
            Log.d(TAG, "Observer - firstName updated: [" + firstName + "]");
            textViewFirstNameDisplay.setText(firstName != null ? firstName : "");
        });
        viewModel.getSurname().observe(getViewLifecycleOwner(), surname -> {
            Log.d(TAG, "Observer - surname updated: [" + surname + "]");
            textViewSurnameDisplay.setText(surname != null ? surname : "");
        });
        viewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            Log.d(TAG, "Observer - email updated: [" + email + "]");
            textViewEmail.setText(email != null ? email : "");
        });
        viewModel.getStudentId().observe(getViewLifecycleOwner(), studentId -> {
            Log.d(TAG, "Observer - studentId updated: [" + studentId + "]");
            textViewStudentNumber.setText(studentId != null ? studentId : "");
        });

        viewModel.getGender().observe(getViewLifecycleOwner(), gender -> {
            Log.d(TAG, "Observer - gender updated: [" + gender + "]");
            setSpinnerSelection(spinnerGender, gender);
        });
        viewModel.getRace().observe(getViewLifecycleOwner(), race -> {
            Log.d(TAG, "Observer - race updated: [" + race + "]");
            setSpinnerSelection(spinnerRace, race);
        });
    }

    // Separate method for click listeners
    private void setupClickListeners() {
        selectImageButton.setOnClickListener(v -> openImagePicker());
    }

    // Separate method for input listeners (Spinners in this case)
    private void setupInputListeners() {
        spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedGender = null;
                if (position > 0) { // Avoid saving the "Select..." prompt
                    selectedGender = parent.getItemAtPosition(position).toString();
                }
                // --- ADDED Logging ---
                Log.d(TAG, "Spinner Gender selected: [" + selectedGender + "] at position " + position);
                // Only update if value changed
                if (!TextUtils.equals(viewModel.getGender().getValue(), selectedGender)) {
                    Log.d(TAG, "Updating ViewModel gender to: [" + selectedGender + "]");
                    viewModel.setGender(selectedGender);
                } else {
                    Log.d(TAG, "Spinner Gender selection (" + selectedGender + ") hasn't changed, not updating ViewModel.");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner Gender nothing selected.");
                if (viewModel.getGender().getValue() != null) {
                    Log.d(TAG, "Updating ViewModel gender to null.");
                    viewModel.setGender(null);
                }
            }
        });

        spinnerRace.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRace = null;
                if (position > 0) { // Avoid saving the "Select..." prompt
                    selectedRace = parent.getItemAtPosition(position).toString();
                }
                // --- ADDED Logging ---
                Log.d(TAG, "Spinner Race selected: [" + selectedRace + "] at position " + position);
                // Only update if value changed
                if (!TextUtils.equals(viewModel.getRace().getValue(), selectedRace)) {
                    Log.d(TAG, "Updating ViewModel race to: [" + selectedRace + "]");
                    viewModel.setRace(selectedRace);
                } else {
                    Log.d(TAG, "Spinner Race selection (" + selectedRace + ") hasn't changed, not updating ViewModel.");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "Spinner Race nothing selected.");
                if (viewModel.getRace().getValue() != null) {
                    Log.d(TAG, "Updating ViewModel race to null.");
                    viewModel.setRace(null);
                }
            }
        });
    }


    // Helper method to set spinner selection
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
            Log.d(TAG,"Setting spinner selection for value '"+value+"' to position "+positionToSelect);
            spinner.setSelection(positionToSelect);
        }
    }
}