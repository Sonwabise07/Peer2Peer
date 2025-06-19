package com.example.peer2peer; 


import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.peer2peer.adapters.ManageAvailabilityTimeSlotAdapter;
import com.example.peer2peer.TimeSlot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;
import java.util.Locale;

import java.util.TimeZone;


public class ManageAvailabilityActivity extends AppCompatActivity implements ManageAvailabilityTimeSlotAdapter.OnManageAvailabilitySlotListener {

    private static final String TAG = "ManageAvailability";

   
    private CalendarView calendarView;
    private TextView textViewSelectedDate;
    private RecyclerView recyclerViewTimeSlots;
    private Button buttonAddTimeSlot;
    private Spinner spinnerSelectModule;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String tutorUid;

    // Date Handling
    private Calendar selectedDate;
    @SuppressWarnings("deprecation")
    private SimpleDateFormat dateFormatter; 

    
    private ManageAvailabilityTimeSlotAdapter timeSlotAdapter;
    private List<TimeSlot> timeSlotsList; 

    // For Module Spinner
    private List<String> tutorModulesDisplayList; 
    private ArrayAdapter<String> moduleAdapter;   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_availability);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tutorUid = currentUser.getUid();

        calendarView = findViewById(R.id.calendar_view_availability);
        textViewSelectedDate = findViewById(R.id.text_selected_date);
        recyclerViewTimeSlots = findViewById(R.id.recycler_view_time_slots);
        buttonAddTimeSlot = findViewById(R.id.button_add_time_slot);
        spinnerSelectModule = findViewById(R.id.spinner_select_module);

        dateFormatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        selectedDate = Calendar.getInstance(TimeZone.getDefault());

        tutorModulesDisplayList = new ArrayList<>();
        moduleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tutorModulesDisplayList);
        moduleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectModule.setAdapter(moduleAdapter);

        spinnerSelectModule.setEnabled(false);
        buttonAddTimeSlot.setEnabled(false);

        fetchTutorModules();

        setupRecyclerView();
        updateSelectedDateText();
        loadSlotsForDate(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            updateSelectedDateText();
            Log.d(TAG, "Calendar Selected Date: " + dateFormatter.format(selectedDate.getTime()));
            loadSlotsForDate(selectedDate);
        });

        buttonAddTimeSlot.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance(TimeZone.getDefault());
            today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
            Calendar compareSelectedDate = (Calendar) selectedDate.clone();
            compareSelectedDate.set(Calendar.HOUR_OF_DAY, 0); compareSelectedDate.set(Calendar.MINUTE, 0); compareSelectedDate.set(Calendar.SECOND, 0); compareSelectedDate.set(Calendar.MILLISECOND, 0);

            if (compareSelectedDate.before(today)) {
                Toast.makeText(ManageAvailabilityActivity.this, "Cannot add availability for past dates.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spinnerSelectModule.getSelectedItemPosition() == Spinner.INVALID_POSITION || tutorModulesDisplayList.isEmpty()) {
                Toast.makeText(ManageAvailabilityActivity.this, "Please select a module.", Toast.LENGTH_LONG).show();
                return;
            }
            showTimePicker();
        });
    }

    private void fetchTutorModules() {
        if (tutorUid == null) return;
        Log.d(TAG, "Fetching modules for tutor: " + tutorUid);
        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> modules = (List<String>) documentSnapshot.get("modulesToTutor");
                        if (modules != null && !modules.isEmpty()) {
                            Log.d(TAG, "Modules fetched: " + modules);
                            tutorModulesDisplayList.clear();
                            tutorModulesDisplayList.addAll(modules);
                            moduleAdapter.notifyDataSetChanged();
                            spinnerSelectModule.setEnabled(true);
                            buttonAddTimeSlot.setEnabled(true);
                        } else {
                            Log.w(TAG, "Tutor has no 'modulesToTutor' or it's empty.");
                            Toast.makeText(ManageAvailabilityActivity.this, "No teaching modules found in your profile. Please update your profile to add availability.", Toast.LENGTH_LONG).show();
                            tutorModulesDisplayList.clear();
                            moduleAdapter.notifyDataSetChanged();
                            spinnerSelectModule.setEnabled(false);
                            buttonAddTimeSlot.setEnabled(false);
                        }
                    } else {
                        Log.w(TAG, "Tutor document not found for UID: " + tutorUid);
                        Toast.makeText(ManageAvailabilityActivity.this, "Tutor profile not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching tutor modules", e);
                    Toast.makeText(ManageAvailabilityActivity.this, "Could not load your teaching modules.", Toast.LENGTH_SHORT).show();
                    spinnerSelectModule.setEnabled(false);
                    buttonAddTimeSlot.setEnabled(false);
                });
    }

    
    private boolean isDateBefore(Calendar date1, Calendar date2) {
        Calendar cal1 = (Calendar) date1.clone();
        cal1.set(Calendar.HOUR_OF_DAY, 0); cal1.set(Calendar.MINUTE, 0); cal1.set(Calendar.SECOND, 0); cal1.set(Calendar.MILLISECOND, 0);
        Calendar cal2 = (Calendar) date2.clone();
        cal2.set(Calendar.HOUR_OF_DAY, 0); cal2.set(Calendar.MINUTE, 0); cal2.set(Calendar.SECOND, 0); cal2.set(Calendar.MILLISECOND, 0);
        return cal1.before(cal2);
    }

    private void setupRecyclerView() {
        timeSlotsList = new ArrayList<>(); // Initialize the list
        timeSlotAdapter = new ManageAvailabilityTimeSlotAdapter(this, timeSlotsList, this);
        recyclerViewTimeSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTimeSlots.setAdapter(timeSlotAdapter);
    }

    @SuppressWarnings("deprecation")
    private void updateSelectedDateText() {
        if (selectedDate != null) {
            SimpleDateFormat displayDateFormatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            textViewSelectedDate.setText(displayDateFormatter.format(selectedDate.getTime()));
        }
    }

    @SuppressWarnings("deprecation")
    private void loadSlotsForDate(Calendar date) {
        Log.d(TAG, "Loading slots for date: " + dateFormatter.format(date.getTime()));
        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0); startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);
        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.add(Calendar.DAY_OF_YEAR, 1);
        endOfDay.set(Calendar.HOUR_OF_DAY, 0); endOfDay.set(Calendar.MINUTE, 0); endOfDay.set(Calendar.SECOND, 0); endOfDay.set(Calendar.MILLISECOND, 0);
        Timestamp startTimestamp = new Timestamp(startOfDay.getTime());
        Timestamp endTimestamp = new Timestamp(endOfDay.getTime());

        db.collection("users").document(tutorUid)
                .collection("availability")
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<TimeSlot> fetchedSlots = new ArrayList<>();
                        Log.d(TAG, "Found " + task.getResult().size() + " slots for the date.");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                TimeSlot slot = document.toObject(TimeSlot.class);
                                if (slot != null) {
                                    slot.setDocumentId(document.getId());
                                    fetchedSlots.add(slot);
                                } else { Log.w(TAG, "Fetched null slot data for doc ID: " + document.getId()); }
                            } catch (Exception e) { Log.e(TAG, "Error parsing TimeSlot from document: " + document.getId(), e); }
                        }
                 
                        timeSlotsList.clear();
                        timeSlotsList.addAll(fetchedSlots);
                        runOnUiThread(() -> {
                            if (timeSlotAdapter != null) {
                                // The adapter uses its own copy internally, but best practice is to update its data explicitly
                                timeSlotAdapter.updateData(new ArrayList<>(timeSlotsList)); // Pass a new copy
                            }
                        });
                    } else {
                        Log.e(TAG, "Error getting availability documents: ", task.getException());
                        Toast.makeText(ManageAvailabilityActivity.this, "Error loading schedule.", Toast.LENGTH_SHORT).show();
                        timeSlotsList.clear(); // Clear local list on error
                        runOnUiThread(() -> {
                            if (timeSlotAdapter != null) {
                                timeSlotAdapter.updateData(new ArrayList<>(timeSlotsList)); // Update adapter with empty list
                            }
                        });
                    }
                });
    }


    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(9) // Default start hour
                .setMinute(0)
                .setTitleText("Select Start Time")
                .build();

        timePicker.addOnPositiveButtonClickListener(dialog -> {
            int selectedHour = timePicker.getHour();
            int selectedMinute = timePicker.getMinute();

            Calendar proposedStartTimeCal = (Calendar) selectedDate.clone();
            proposedStartTimeCal.set(Calendar.HOUR_OF_DAY, selectedHour);
            proposedStartTimeCal.set(Calendar.MINUTE, selectedMinute);
            proposedStartTimeCal.set(Calendar.SECOND, 0);
            proposedStartTimeCal.set(Calendar.MILLISECOND, 0);

            Calendar now = Calendar.getInstance(TimeZone.getDefault());
            // More robust check for past time on the same day
            Calendar selectedDayOnly = (Calendar) selectedDate.clone();
            selectedDayOnly.set(Calendar.HOUR_OF_DAY, 0); selectedDayOnly.set(Calendar.MINUTE, 0); selectedDayOnly.set(Calendar.SECOND, 0); selectedDayOnly.set(Calendar.MILLISECOND, 0);
            Calendar todayOnly = (Calendar) now.clone();
            todayOnly.set(Calendar.HOUR_OF_DAY, 0); todayOnly.set(Calendar.MINUTE, 0); todayOnly.set(Calendar.SECOND, 0); todayOnly.set(Calendar.MILLISECOND, 0);

            if (selectedDayOnly.equals(todayOnly) && proposedStartTimeCal.before(now)) {
                Toast.makeText(ManageAvailabilityActivity.this, "Cannot add availability for a past time today.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Past date check is handled by buttonAddTimeSlot.setOnClickListener

            saveTimeSlot(selectedHour, selectedMinute);
        });

        timePicker.show(getSupportFragmentManager(), "MATERIAL_TIME_PICKER");
    }

    private void saveTimeSlot(int hour, int minute) {
        String selectedModuleCode;
        if (spinnerSelectModule.getSelectedItemPosition() == Spinner.INVALID_POSITION ||
                spinnerSelectModule.getSelectedItem() == null ||
                tutorModulesDisplayList.isEmpty()) {
            Toast.makeText(this, "Please select a module.", Toast.LENGTH_LONG).show();
            return;
        }
        selectedModuleCode = spinnerSelectModule.getSelectedItem().toString();

        if (selectedModuleCode.trim().isEmpty()) {
            Toast.makeText(this, "Selected module is invalid.", Toast.LENGTH_LONG).show();
            return;
        }

        // --- BEGIN VALIDATION LOGIC ---
        // 'hour' is the hour (0-23) of the new slot the user wants to add.
        // 'timeSlotsList' contains existing TimeSlot objects for the selectedDate, loaded by loadSlotsForDate().
        Log.d(TAG, "Validating new slot at " + hour + ":" + minute + ". Existing slots for date: " + timeSlotsList.size());

        for (TimeSlot existingSlot : timeSlotsList) {
            if (existingSlot.getStartTime() != null) {
                Calendar existingSlotCalendar = Calendar.getInstance(TimeZone.getDefault()); // Use consistent TimeZone
                existingSlotCalendar.setTime(existingSlot.getStartTime().toDate());
                int existingSlotHour = existingSlotCalendar.get(Calendar.HOUR_OF_DAY);

                if (existingSlotHour == hour) {
                    // Conflict found: a slot already exists in the same hour block.
                    String conflictMessage = String.format(Locale.getDefault(),
                            "You already have an availability slot for the %02d:00 - %02d:59 hour. Please select a different hour.",
                            hour, hour); // Corrected to show only the conflicting hour
                    Toast.makeText(ManageAvailabilityActivity.this, conflictMessage, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Validation failed: Slot conflict at hour " + hour);
                    return; // Exit the method, do not save the new slot
                }
            }
        }
        Log.d(TAG, "Validation passed for new slot at " + hour + ":" + minute);
        // --- END VALIDATION LOGIC ---

        // If validation passes, proceed to create and save the new TimeSlot.
        Calendar startTimeCal = (Calendar) selectedDate.clone();
        startTimeCal.set(Calendar.HOUR_OF_DAY, hour);
        startTimeCal.set(Calendar.MINUTE, minute);
        startTimeCal.set(Calendar.SECOND, 0);
        startTimeCal.set(Calendar.MILLISECOND, 0);

        Calendar endTimeCal = (Calendar) startTimeCal.clone();
        endTimeCal.add(Calendar.HOUR_OF_DAY, 1); // Default 1-hour slot

        Timestamp startTimeStamp = new Timestamp(startTimeCal.getTime());
        Timestamp endTimeStamp = new Timestamp(endTimeCal.getTime());

        TimeSlot newTimeSlot = new TimeSlot(startTimeStamp, endTimeStamp, "available", selectedModuleCode);

        db.collection("users").document(tutorUid)
                .collection("availability")
                .add(newTimeSlot)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Availability slot added with ID: " + documentReference.getId() + " for module: " + selectedModuleCode);
                    Toast.makeText(ManageAvailabilityActivity.this, "Availability for " + selectedModuleCode + " added!", Toast.LENGTH_SHORT).show();
                    loadSlotsForDate(selectedDate); // Refresh the list which also updates timeSlotsList
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding availability slot", e);
                    Toast.makeText(ManageAvailabilityActivity.this, "Failed to add availability for " + selectedModuleCode, Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRemoveClick(String documentId) {
        if (documentId == null || documentId.isEmpty()) {
            Log.e(TAG, "Attempted to remove slot with null or empty document ID.");
            Toast.makeText(this, "Error: Cannot remove slot.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Attempting to remove slot with document ID: " + documentId);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove Slot")
                .setMessage("Are you sure you want to remove this availability slot?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("users").document(tutorUid)
                            .collection("availability").document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "DocumentSnapshot successfully deleted!");
                                Toast.makeText(ManageAvailabilityActivity.this, "Slot removed.", Toast.LENGTH_SHORT).show();
                                loadSlotsForDate(selectedDate); // Refresh list, which also updates timeSlotsList
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error deleting document", e);
                                Toast.makeText(ManageAvailabilityActivity.this, "Error removing slot.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}