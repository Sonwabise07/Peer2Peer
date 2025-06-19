package com.example.peer2peer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.adapters.MyResourcesAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import com.google.firebase.Timestamp;



import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddResourceActivity extends AppCompatActivity implements MyResourcesAdapter.OnResourceActionsListener {

    private static final String TAG = "AddResourceActivity";

    
    private Toolbar toolbar;
    private TextInputEditText editTextResourceTitle, editTextResourceDescription, editTextResourceLinkUrl;
    private Spinner spinnerResourceModuleCode;
    private RadioGroup radioGroupResourceType;
    private RadioButton radioButtonFile, radioButtonLink;
    private LinearLayout layoutFileUploadDetails;
    private Button buttonChooseFile, buttonSaveResource;
    private TextView textViewSelectedFileName;
    private ProgressBar progressBarSaveResource;

    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage; // Instance of FirebaseStorage
    private String currentTutorUid;

    // For Module Spinner
    private List<String> tutorModulesDisplayList;
    private ArrayAdapter<String> moduleAdapter;

    // For File Picker
    private Uri selectedFileUri;
    private String selectedFileNameForUpload;
    private String selectedFileMimeType;

    private ActivityResultLauncher<Intent> filePickerLauncher;

   
    private RecyclerView recyclerViewMyResources;
    private TextView textViewNoResources;
    private MyResourcesAdapter myResourcesAdapter;
    private List<Resource> myResourceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_resource);

        toolbar = findViewById(R.id.toolbar_add_resource);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manage Resources");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(); 

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentTutorUid = currentUser.getUid();

        editTextResourceTitle = findViewById(R.id.edit_text_resource_title);
        editTextResourceDescription = findViewById(R.id.edit_text_resource_description);
        spinnerResourceModuleCode = findViewById(R.id.spinner_resource_module_code);
        radioGroupResourceType = findViewById(R.id.radiogroup_resource_type);
        radioButtonFile = findViewById(R.id.radio_button_file);
        radioButtonLink = findViewById(R.id.radio_button_link);
        layoutFileUploadDetails = findViewById(R.id.layout_file_upload_details);
        buttonChooseFile = findViewById(R.id.button_choose_file);
        textViewSelectedFileName = findViewById(R.id.text_selected_file_name);
        editTextResourceLinkUrl = findViewById(R.id.edit_text_resource_link_url);
        buttonSaveResource = findViewById(R.id.button_save_resource);
        progressBarSaveResource = findViewById(R.id.progress_bar_save_resource);

        recyclerViewMyResources = findViewById(R.id.recycler_view_my_resources);
        textViewNoResources = findViewById(R.id.text_view_no_resources);

        tutorModulesDisplayList = new ArrayList<>();
        moduleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tutorModulesDisplayList);
        moduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerResourceModuleCode.setAdapter(moduleAdapter);
        spinnerResourceModuleCode.setEnabled(false);

        fetchTutorModules();
        setupRadioGroupListener();
        setupFilePicker();

        buttonChooseFile.setOnClickListener(v -> openFilePicker());
        buttonSaveResource.setOnClickListener(v -> validateAndSaveResource());

        myResourceList = new ArrayList<>();
        myResourcesAdapter = new MyResourcesAdapter(this, myResourceList, this);
        recyclerViewMyResources.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMyResources.setAdapter(myResourcesAdapter);
        recyclerViewMyResources.setNestedScrollingEnabled(false);

        loadMyResources();
    }

    private void setupRadioGroupListener() {
        radioGroupResourceType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_button_file) {
                layoutFileUploadDetails.setVisibility(View.VISIBLE);
                findViewById(R.id.til_resource_link_url).setVisibility(View.GONE);
                editTextResourceLinkUrl.setText("");
            } else if (checkedId == R.id.radio_button_link) {
                layoutFileUploadDetails.setVisibility(View.GONE);
                findViewById(R.id.til_resource_link_url).setVisibility(View.VISIBLE);
                clearSelectedFileInfo();
            }
        });
        layoutFileUploadDetails.setVisibility(View.VISIBLE);
        findViewById(R.id.til_resource_link_url).setVisibility(View.GONE);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedFileUri = result.getData().getData();
                        if (selectedFileUri != null) {
                            Cursor returnCursor = getContentResolver().query(selectedFileUri, null, null, null, null);
                            if (returnCursor != null && returnCursor.moveToFirst()) {
                                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                selectedFileNameForUpload = returnCursor.getString(nameIndex);
                                returnCursor.close();
                            } else {
                                selectedFileNameForUpload = selectedFileUri.getLastPathSegment();
                            }
                            selectedFileMimeType = getContentResolver().getType(selectedFileUri);
                            textViewSelectedFileName.setText(selectedFileNameForUpload != null ? selectedFileNameForUpload : "File selected");
                            Log.d(TAG, "File selected: " + selectedFileNameForUpload + ", URI: " + selectedFileUri + ", MIME: " + selectedFileMimeType);
                        }
                    }
                });
    }

    private void fetchTutorModules() {
        if (currentTutorUid == null) return;
        db.collection("users").document(currentTutorUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> modules = (List<String>) documentSnapshot.get("modulesToTutor");
                        if (modules != null && !modules.isEmpty()) {
                            tutorModulesDisplayList.clear();
                            tutorModulesDisplayList.addAll(modules);
                            moduleAdapter.notifyDataSetChanged();
                            spinnerResourceModuleCode.setEnabled(true);
                        } else {
                            Toast.makeText(this, "No teaching modules found in profile. Please update profile.", Toast.LENGTH_LONG).show();
                            spinnerResourceModuleCode.setEnabled(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching modules.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching tutor modules", e);
                });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select a File to Upload"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndSaveResource() {
        String title = Objects.requireNonNull(editTextResourceTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(editTextResourceDescription.getText()).toString().trim();
        String moduleCode = "";
        if (spinnerResourceModuleCode.getSelectedItem() != null && spinnerResourceModuleCode.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
            moduleCode = spinnerResourceModuleCode.getSelectedItem().toString();
        }

        if (TextUtils.isEmpty(title)) {
            editTextResourceTitle.setError("Title is required");
            editTextResourceTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(moduleCode)) {
            Toast.makeText(this, "Please select a module", Toast.LENGTH_SHORT).show();
            if(spinnerResourceModuleCode.getSelectedView() != null) { 
                ((TextView)spinnerResourceModuleCode.getSelectedView()).setError("Module is required");
            }
            return;
        }

        if (radioButtonFile.isChecked()) {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please choose a file to upload", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadFileAndSaveResource(title, description, moduleCode, selectedFileUri, selectedFileNameForUpload, selectedFileMimeType);
        } else if (radioButtonLink.isChecked()) {
            String url = Objects.requireNonNull(editTextResourceLinkUrl.getText()).toString().trim();
            if (TextUtils.isEmpty(url) || !android.util.Patterns.WEB_URL.matcher(url).matches()) {
                editTextResourceLinkUrl.setError("Valid URL is required");
                editTextResourceLinkUrl.requestFocus();
                return;
            }
            saveResourceToFirestore(title, description, moduleCode, "link", url, null, null, null);
        } else {
            Toast.makeText(this, "Please select a resource type", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFileAndSaveResource(String title, String description, String moduleCode,
                                           Uri fileUri, String originalFileName, @Nullable String mimeTypeParam) {
        if (currentTutorUid == null) return;
        showProgress(true);

        String uniqueFileNameInStorage = UUID.randomUUID().toString() + "_" + (originalFileName != null ? originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "file");
        StorageReference storageRef = storage.getReference() 
                .child("tutor_resources")
                .child(currentTutorUid)
                .child(moduleCode)
                .child(uniqueFileNameInStorage);

        UploadTask uploadTask = storageRef.putFile(fileUri);
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressBarSaveResource.setIndeterminate(false);
            progressBarSaveResource.setProgress((int) progress);
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                if (downloadUri != null) {
                    '
                    saveResourceToFirestore(title, description, moduleCode, "file",
                            downloadUri.toString(), originalFileName, storageRef.getPath(), mimeTypeParam);
                } else {
                    handleSaveError(new Exception("Failed to get download URL."));
                }
            } else {
                handleSaveError(task.getException());
            }
        });
    }

    // Parameter names now match your Resource model for clarity when constructing:
    // (uploaderUid, title, description, type, url, fileName, filePath, mimeType, moduleCode)
    private void saveResourceToFirestore(String title, String description, String moduleCode, String type,
                                         String urlForResource, @Nullable String actualFileName,
                                         @Nullable String actualFilePath, @Nullable String actualMimeType) {
        if (currentTutorUid == null) return;
        showProgress(true);

        Resource resource = new Resource(
                currentTutorUid, title, description, type, urlForResource,
                actualFileName, actualFilePath, actualMimeType, moduleCode
        );

        db.collection("users").document(currentTutorUid)
                .collection("resources")
                .add(resource)
                .addOnSuccessListener(documentReference -> {
                    showProgress(false);
                    Toast.makeText(AddResourceActivity.this, "Resource added successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Resource added with ID: " + documentReference.getId());
                    clearInputFields();
                    loadMyResources();
                })
                .addOnFailureListener(e -> {
                    handleSaveError(e);
                    if ("file".equals(type) && actualFilePath != null) {
                        Log.w(TAG, "Firestore save failed after file upload. Orphan file might exist at: " + actualFilePath);
                    }
                });
    }

    private void clearInputFields() {
        editTextResourceTitle.setText("");
        editTextResourceDescription.setText("");
        editTextResourceLinkUrl.setText("");
        clearSelectedFileInfo();
        if (tutorModulesDisplayList != null && !tutorModulesDisplayList.isEmpty()) {
            spinnerResourceModuleCode.setSelection(0); // Or whatever default position is appropriate
        }
        radioButtonFile.setChecked(true); // Resets to default type
        // UI updates for radio button change will be handled by its listener
    }

    private void clearSelectedFileInfo() {
        textViewSelectedFileName.setText("");
        selectedFileUri = null;
        selectedFileNameForUpload = null;
        selectedFileMimeType = null;
    }

    private void showProgress(boolean show) {
        progressBarSaveResource.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonSaveResource.setEnabled(!show);
        editTextResourceTitle.setEnabled(!show);
        editTextResourceDescription.setEnabled(!show);
        spinnerResourceModuleCode.setEnabled(!show);
        for (int i = 0; i < radioGroupResourceType.getChildCount(); i++) {
            ((RadioButton) radioGroupResourceType.getChildAt(i)).setEnabled(!show);
        }
        buttonChooseFile.setEnabled(!show);
        editTextResourceLinkUrl.setEnabled(!show);
    }

    private void handleSaveError(@Nullable Exception e) {
        showProgress(false);
        Log.e(TAG, "Error saving resource", e);
        Toast.makeText(AddResourceActivity.this, "Failed to save resource: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
    }

    private void loadMyResources() {
        if (currentTutorUid == null) return;
        Log.d(TAG, "Loading resources for tutor: " + currentTutorUid);

        db.collection("users").document(currentTutorUid)
                .collection("resources")
                .orderBy("uploadedAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        myResourceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Resource resource = document.toObject(Resource.class);
                            resource.setDocumentId(document.getId());
                            myResourceList.add(resource);
                        }
                        
                        myResourcesAdapter.submitList(new ArrayList<>(myResourceList));
                        updateNoResourcesView();
                        Log.d(TAG, "Loaded " + myResourceList.size() + " resources.");
                    } else {
                        Log.e(TAG, "Error loading resources: ", task.getException());
                        Toast.makeText(AddResourceActivity.this, "Failed to load resources.", Toast.LENGTH_SHORT).show();
                        myResourceList.clear(); 
                        myResourcesAdapter.submitList(new ArrayList<>(myResourceList)); 
                        updateNoResourcesView();
                    }
                });
    }

    private void updateNoResourcesView() {
        if (myResourceList.isEmpty()) {
            textViewNoResources.setVisibility(View.VISIBLE);
            recyclerViewMyResources.setVisibility(View.GONE);
        } else {
            textViewNoResources.setVisibility(View.GONE);
            recyclerViewMyResources.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDeleteClick(Resource resource, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Resource")
                .setMessage("Are you sure you want to delete '" + resource.getTitle() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> deleteResourceInternal(resource, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteResourceInternal(Resource resource, int position) {
        if (resource == null || resource.getDocumentId() == null) {
            Toast.makeText(this, "Error: Resource data is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }
        String docId = resource.getDocumentId();
        Log.d(TAG, "Attempting to delete resource with ID: " + docId);
        showProgress(true);

        db.collection("users").document(currentTutorUid)
                .collection("resources").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Resource document successfully deleted from Firestore: " + docId);
                    // Use getFilePath() from your Resource.java
                    if ("file".equalsIgnoreCase(resource.getType()) && resource.getFilePath() != null && !resource.getFilePath().isEmpty()) {
                        
                        
                        StorageReference fileRef = storage.getReference(resource.getFilePath());
                        fileRef.delete().addOnSuccessListener(aVoid1 -> {
                            Log.d(TAG, "Resource file successfully deleted from Storage: " + resource.getFilePath());
                            finalizeDeletionUI("'" + resource.getTitle() + "' deleted successfully.");
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete resource file from Storage: " + resource.getFilePath(), e);
                            // Even if storage deletion fails, Firestore entry is gone.
                            finalizeDeletionUI("Resource deleted from list, but failed to delete file from storage.");
                        });
                    } else {
                        // If not a file or no storage path, deletion is complete.
                        finalizeDeletionUI("'" + resource.getTitle() + "' (link or no file data) deleted successfully.");
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false); // Hide progress on failure too
                    Log.e(TAG, "Error deleting resource document from Firestore: " + docId, e);
                    Toast.makeText(AddResourceActivity.this, "Failed to delete resource from Firestore.", Toast.LENGTH_SHORT).show();
                });
    }

    private void finalizeDeletionUI(String message) {
        Toast.makeText(AddResourceActivity.this, message, Toast.LENGTH_SHORT).show();
        showProgress(false);
        loadMyResources(); // Refresh list
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}