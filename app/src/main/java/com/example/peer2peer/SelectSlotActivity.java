package com.example.peer2peer;


import android.app.AlarmManager; 
import android.content.Context;    
import android.content.Intent;
import android.os.Build;          
import android.os.Bundle;
import android.provider.Settings;  
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; 
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Firebase Imports
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; 
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

// Your App's Model and Adapter classes
import com.example.peer2peer.adapters.TimeSlotAdapter;
import com.example.peer2peer.TimeSlot;
import com.example.peer2peer.Booking;         
import com.example.peer2peer.AlarmScheduler;  

// Java Util Imports
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

// Stripe Imports
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;


public class SelectSlotActivity extends AppCompatActivity {

    private static final String TAG = "SelectSlotActivity";
    public static final String EXTRA_TUTOR_UID = "TUTOR_UID";
    public static final String EXTRA_TUTOR_NAME = "TUTOR_NAME"; 

    // UI Elements
    private CalendarView calendarView;
    private TextView textViewSelectedDate;
    private Spinner spinnerSelectModuleForBooking;
    private RecyclerView recyclerViewDailySlots;
    private Button buttonBookSelectedSlot;
    private ProgressBar progressBarSlotsLoading;
    private ProgressBar progressBarBookingAction;
    private TextView textViewNoSlotsMessage;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String tutorUid;
    private String tutorNameFromIntent;
    private FirebaseFunctions mFunctions;

    // Date Handling
    private Calendar selectedCalendarDate;
    private SimpleDateFormat dateFormatter; 

    // RecyclerView and Data
    private TimeSlotAdapter timeSlotAdapter;
    private List<TimeSlot> allSlotsForSelectedDate;
    private List<String> availableModulesForDayList;
    private ArrayAdapter<String> moduleForBookingAdapter;

    // Payment & State Variables
    private PaymentSheet paymentSheet;
    private String paymentIntentClientSecret;
    private String currentPaymentIntentId;
    private TimeSlot currentSelectedTimeSlot;

    // --- NEW FOR ALARM SCHEDULING ---
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 101;
    private Booking bookingToScheduleAfterPermission;
    // --- END NEW FOR ALARM SCHEDULING ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_slot);

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        tutorUid = getIntent().getStringExtra(EXTRA_TUTOR_UID);
        tutorNameFromIntent = getIntent().getStringExtra(EXTRA_TUTOR_NAME);

        if (tutorUid == null || tutorUid.isEmpty()) {
            Log.e(TAG, "No Tutor UID provided.");
            Toast.makeText(this, "Error: Tutor info missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (tutorNameFromIntent == null || tutorNameFromIntent.isEmpty()) {
            tutorNameFromIntent = "Selected Tutor"; // Fallback
        }
        Log.d(TAG, "Received Tutor UID: " + tutorUid + ", Name: " + tutorNameFromIntent);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        calendarView = findViewById(R.id.calendar_view_select_slot);
        textViewSelectedDate = findViewById(R.id.text_selected_slot_date);
        spinnerSelectModuleForBooking = findViewById(R.id.spinner_select_module_for_booking);
        recyclerViewDailySlots = findViewById(R.id.recycler_view_daily_slots);
        buttonBookSelectedSlot = findViewById(R.id.button_book_selected_slot);
        progressBarSlotsLoading = findViewById(R.id.progress_bar_slots_loading);
        progressBarBookingAction = findViewById(R.id.progress_bar_booking_action);
        textViewNoSlotsMessage = findViewById(R.id.text_no_slots_message);

        dateFormatter = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()); // Corrected year pattern
        selectedCalendarDate = Calendar.getInstance(TimeZone.getDefault());

        allSlotsForSelectedDate = new ArrayList<>();
        availableModulesForDayList = new ArrayList<>();
        moduleForBookingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, availableModulesForDayList);
        moduleForBookingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectModuleForBooking.setAdapter(moduleForBookingAdapter);
        spinnerSelectModuleForBooking.setEnabled(false);

        setupRecyclerView();
        updateSelectedDateText();
        loadSlotsForDateAndPopulateModules(selectedCalendarDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedCalendarDate.set(year, month, dayOfMonth);
            updateSelectedDateText();
            Log.d(TAG, "Selected Date: " + dateFormatter.format(selectedCalendarDate.getTime()));
            loadSlotsForDateAndPopulateModules(selectedCalendarDate);
        });

        spinnerSelectModuleForBooking.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModule = availableModulesForDayList.get(position);
                Log.d(TAG, "Module selected by tutee: " + selectedModule);
                filterAndDisplaySlotsForModule(selectedModule);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (timeSlotAdapter != null) {
                    timeSlotAdapter.updateTimeSlots(new ArrayList<>());
                    timeSlotAdapter.clearSelection();
                }
                buttonBookSelectedSlot.setEnabled(false);
                textViewNoSlotsMessage.setText("Please select a module to see available slots.");
                textViewNoSlotsMessage.setVisibility(View.VISIBLE);
                recyclerViewDailySlots.setVisibility(View.GONE);
            }
        });

        buttonBookSelectedSlot.setOnClickListener(v -> fetchTutorRateAndInitiatePaymentIntent());
        buttonBookSelectedSlot.setEnabled(false);
    }

    private void setupRecyclerView() {
        recyclerViewDailySlots.setLayoutManager(new LinearLayoutManager(this));
        timeSlotAdapter = new TimeSlotAdapter(new ArrayList<>(), selectedTimeSlot -> {
            currentSelectedTimeSlot = selectedTimeSlot;
            buttonBookSelectedSlot.setEnabled(selectedTimeSlot != null);
        });
        recyclerViewDailySlots.setAdapter(timeSlotAdapter);
    }

    private void updateSelectedDateText() {
        if (dateFormatter != null && selectedCalendarDate != null) {
            textViewSelectedDate.setText(dateFormatter.format(selectedCalendarDate.getTime()));
        }
    }

    private void loadSlotsForDateAndPopulateModules(Calendar date) {
        if (tutorUid == null) return;
        Log.d(TAG, "Loading slots and modules for date: " + (dateFormatter != null ? dateFormatter.format(date.getTime()) : date.getTime().toString()) );
        showProgress(true, true);
        textViewNoSlotsMessage.setVisibility(View.GONE);
        recyclerViewDailySlots.setVisibility(View.GONE);
        availableModulesForDayList.clear();
        allSlotsForSelectedDate.clear();
        if (timeSlotAdapter != null) {
            timeSlotAdapter.updateTimeSlots(new ArrayList<>());
            timeSlotAdapter.clearSelection();
        }
        buttonBookSelectedSlot.setEnabled(false);
        spinnerSelectModuleForBooking.setEnabled(false);


        Calendar startOfDay = (Calendar) date.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);

        Calendar endOfDay = (Calendar) date.clone();
        endOfDay.add(Calendar.DAY_OF_YEAR, 1);
        endOfDay.set(Calendar.HOUR_OF_DAY, 0); endOfDay.set(Calendar.MINUTE, 0);
        endOfDay.set(Calendar.SECOND, 0); endOfDay.set(Calendar.MILLISECOND, 0);

        Timestamp startTimestamp = new Timestamp(startOfDay.getTime());
        Timestamp endTimestamp = new Timestamp(endOfDay.getTime());

        db.collection("users").document(tutorUid)
                .collection("availability")
                .whereEqualTo("status", "available")
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showProgress(false, true);
                    Set<String> uniqueModules = new HashSet<>();
                    Date now = new Date();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            try {
                                TimeSlot slot = snapshot.toObject(TimeSlot.class);
                                if (slot != null && slot.getStartTime() != null && slot.getStartTime().toDate().after(now) && slot.getModuleCode() != null && !slot.getModuleCode().isEmpty()) {
                                    slot.setDocumentId(snapshot.getId());
                                    allSlotsForSelectedDate.add(slot);
                                    uniqueModules.add(slot.getModuleCode());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing TimeSlot: " + snapshot.getId(), e);
                            }
                        }
                    }

                    if (uniqueModules.isEmpty()) {
                        textViewNoSlotsMessage.setText("No available slots or modules for this date.");
                        textViewNoSlotsMessage.setVisibility(View.VISIBLE);
                        recyclerViewDailySlots.setVisibility(View.GONE);
                        spinnerSelectModuleForBooking.setEnabled(false);
                        moduleForBookingAdapter.notifyDataSetChanged();
                    } else {
                        availableModulesForDayList.addAll(uniqueModules);
                        Collections.sort(availableModulesForDayList);
                        moduleForBookingAdapter.notifyDataSetChanged();
                        spinnerSelectModuleForBooking.setEnabled(true);
                        textViewNoSlotsMessage.setVisibility(View.GONE);
                        if (!availableModulesForDayList.isEmpty()) {
                            spinnerSelectModuleForBooking.setSelection(0, false);
                            filterAndDisplaySlotsForModule(availableModulesForDayList.get(0));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showProgress(false, true);
                    Log.e(TAG, "Error loading availability or modules", e);
                    Toast.makeText(SelectSlotActivity.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
                    textViewNoSlotsMessage.setText("Error loading schedule.");
                    textViewNoSlotsMessage.setVisibility(View.VISIBLE);
                    recyclerViewDailySlots.setVisibility(View.GONE);
                    availableModulesForDayList.clear();
                    moduleForBookingAdapter.notifyDataSetChanged();
                    spinnerSelectModuleForBooking.setEnabled(false);
                    buttonBookSelectedSlot.setEnabled(false);
                });
    }

    private void filterAndDisplaySlotsForModule(String moduleCode) {
        if (timeSlotAdapter == null) return;

        List<TimeSlot> filteredSlots = new ArrayList<>();
        for (TimeSlot slot : allSlotsForSelectedDate) {
            if (slot.getModuleCode() != null && slot.getModuleCode().equals(moduleCode)) {
                filteredSlots.add(slot);
            }
        }

        timeSlotAdapter.updateTimeSlots(filteredSlots);
        timeSlotAdapter.clearSelection();
        buttonBookSelectedSlot.setEnabled(false);

        if (filteredSlots.isEmpty()) {
            textViewNoSlotsMessage.setText("No slots available for " + moduleCode + " on this date.");
            textViewNoSlotsMessage.setVisibility(View.VISIBLE);
            recyclerViewDailySlots.setVisibility(View.GONE);
        } else {
            textViewNoSlotsMessage.setVisibility(View.GONE);
            recyclerViewDailySlots.setVisibility(View.VISIBLE);
        }
    }

    private void fetchTutorRateAndInitiatePaymentIntent() {
        if (currentSelectedTimeSlot == null || currentSelectedTimeSlot.getDocumentId() == null || currentSelectedTimeSlot.getModuleCode() == null) {
            Toast.makeText(this, "Please select a valid time slot from a module.", Toast.LENGTH_SHORT).show();
            return;
        }
        final String slotId = currentSelectedTimeSlot.getDocumentId();
        showProgress(true, false);

        db.collection("users").document(tutorUid).get()
                .addOnSuccessListener(tutorDoc -> {
                    if (tutorDoc.exists() && tutorDoc.contains("hourlyRate")) {
                        Number rate = tutorDoc.getDouble("hourlyRate");
                        if (rate != null && rate.doubleValue() >= 0) {
                            long amountInCents = Math.round(rate.doubleValue() * 100);
                            if (amountInCents <= 0 && rate.doubleValue() > 0) amountInCents = 1; // Ensure minimum 1 cent if rate > 0
                            else if (amountInCents < 0) amountInCents = 0;

                            callCreatePaymentIntentFunction(amountInCents, tutorUid, slotId);
                        } else {
                            handleFlowFailure("Payment Failed", "Tutor's rate is invalid or not set.");
                        }
                    } else {
                        handleFlowFailure("Payment Failed", "Could not retrieve tutor's rate.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch tutor rate", e);
                    handleFlowFailure("Payment Failed", "Error fetching tutor details: " + e.getMessage());
                });
    }

    private void callCreatePaymentIntentFunction(long amountInCents, String tutorUid, String slotId) {
        Log.d(TAG, "Calling 'createPaymentIntent' function for tutor: " + tutorUid + ", slot: " + slotId + ", amount (cents): " + amountInCents);
        showProgress(true, false);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amountInCents);
        data.put("currency", "zar");
        data.put("tutorUid", tutorUid);
        data.put("slotId", slotId);

        mFunctions.getHttpsCallable("createPaymentIntent")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        Log.e(TAG, "'createPaymentIntent' function call failed.", e);
                        String errorMessage = extractErrorMessage(e);
                        handleFlowFailure("Payment Init Failed", "Could not initiate payment. " + errorMessage);
                        return;
                    }
                    try {
                        HttpsCallableResult result = task.getResult();
                        Map<String, Object> resultData = (Map<String, Object>) result.getData();
                        if (resultData != null && resultData.containsKey("clientSecret") && resultData.containsKey("paymentIntentId")) {
                            String clientSecret = (String) resultData.get("clientSecret");
                            String paymentIntentId = (String) resultData.get("paymentIntentId");

                            if (clientSecret != null && !clientSecret.isEmpty() && paymentIntentId != null && !paymentIntentId.isEmpty()) {
                                Log.i(TAG, "'createPaymentIntent' successful! PI ID: " + paymentIntentId);
                                paymentIntentClientSecret = clientSecret;
                                currentPaymentIntentId = paymentIntentId;
                                launchPaymentSheet();
                            } else {
                                handleFlowFailure("Payment Init Failed", "Invalid response from server (empty keys).");
                            }
                        } else {
                            handleFlowFailure("Payment Init Failed", "Server response missing required data.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse result from 'createPaymentIntent' function.", e);
                        handleFlowFailure("Payment Init Failed", "Error processing server response.");
                    }
                    
    }

    private void launchPaymentSheet() {
        if (paymentIntentClientSecret == null || paymentIntentClientSecret.isEmpty()) {
            handleFlowFailure("Payment Error", "Cannot launch payment (missing client secret).");
            return;
        }
        Log.d(TAG, "Configuring and presenting Payment Sheet for PI ID: " + currentPaymentIntentId);

        final PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Peer2Peer Tutoring")
                .allowsDelayedPaymentMethods(false)
                /
                .build();
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
        
    }

    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        String paymentIdToConfirm = currentPaymentIntentId; 

        
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Log.i(TAG, "PaymentSheet reported Completed. Calling confirmBooking for PI ID: " + paymentIdToConfirm);
            Toast.makeText(this, "Payment Successful! Confirming booking...", Toast.LENGTH_SHORT).show();
            // showProgress(true, false) should already be active or will be by callConfirmBookingFunction
            if (paymentIdToConfirm != null && !paymentIdToConfirm.isEmpty()) {
                callConfirmBookingFunction(paymentIdToConfirm);
            } else {
                Log.e(TAG, "Critical: PaymentIntentId missing after payment success.");
                handleFlowFailure("Booking Failed", "Critical error after payment. Please contact support. (PI_MISSING)");
                paymentIntentClientSecret = null; // Clear now
                currentPaymentIntentId = null;
            }
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            showProgress(false, false);
            Log.w(TAG, "Payment canceled by user.");
            Toast.makeText(this, "Payment Canceled.", Toast.LENGTH_SHORT).show();
            resetUiAfterPaymentInteraction();
            paymentIntentClientSecret = null;
            currentPaymentIntentId = null;
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            showProgress(false, false);
            PaymentSheetResult.Failed failure = (PaymentSheetResult.Failed) paymentSheetResult;
            Log.e(TAG, "Payment failed via PaymentSheet.", failure.getError());
            showErrorAndReset("Payment Failed", failure.getError().getLocalizedMessage());
            resetUiAfterPaymentInteraction();
            paymentIntentClientSecret = null;
            currentPaymentIntentId = null;
        }
    }


    private void callConfirmBookingFunction(String paymentIntentId) {
        Log.d(TAG, "Calling 'confirmBooking' function for PI ID: " + paymentIntentId);
        showProgress(true, false); 
        Map<String, Object> data = new HashMap<>();
        data.put("paymentIntentId", paymentIntentId);

        mFunctions.getHttpsCallable("confirmBooking")
                .call(data)
                .addOnCompleteListener(task -> {
                    
                    paymentIntentClientSecret = null;
                    currentPaymentIntentId = null;

                    if (!task.isSuccessful()) {
                        showProgress(false, false);
                        Exception e = task.getException();
                        Log.e(TAG, "'confirmBooking' function call failed.", e);
                        String errorMessage = extractErrorMessage(e);

                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            if (ffe.getCode() == FirebaseFunctionsException.Code.FAILED_PRECONDITION ||
                                    ffe.getCode() == FirebaseFunctionsException.Code.NOT_FOUND) {
                                errorMessage = "Sorry, this slot was booked just before your payment confirmed. " + (ffe.getMessage() != null ? ffe.getMessage() : "");
                            }
                        }
                        showErrorAndReset("Booking Failed After Payment",
                                "Your payment was successful, but the final booking confirmation failed. Reason: " +
                                        errorMessage +
                                        " Please contact support.");
                        resetUiAfterPaymentInteraction();
                        return;
                    }

                    try {
                        HttpsCallableResult result = task.getResult();
                        Map<String, Object> resultData = (Map<String, Object>) result.getData();
                        if (resultData != null && Boolean.TRUE.equals(resultData.get("success")) && resultData.containsKey("bookingId")) {
                            String newBookingDocumentId = (String) resultData.get("bookingId");
                            Log.i(TAG, "Booking confirmed successfully! Booking ID: " + newBookingDocumentId);
                            Toast.makeText(this, "Booking Confirmed!", Toast.LENGTH_LONG).show();

                            // --- SCHEDULE ALARM FOR THE NEW BOOKING ---
                            if (currentSelectedTimeSlot != null && mAuth.getCurrentUser() != null && newBookingDocumentId != null) {
                                Booking newBooking = new Booking();
                                newBooking.setDocumentId(newBookingDocumentId);
                                newBooking.setTutorUid(tutorUid);
                                newBooking.setTuteeUid(mAuth.getCurrentUser().getUid());
                                newBooking.setAvailabilitySlotId(currentSelectedTimeSlot.getDocumentId()); // From selected slot
                                newBooking.setStartTime(currentSelectedTimeSlot.getStartTime());       // From selected slot
                                newBooking.setEndTime(currentSelectedTimeSlot.getEndTime());           // From selected slot
                                newBooking.setModuleCode(currentSelectedTimeSlot.getModuleCode());     // From selected slot
                                newBooking.setBookingStatus("confirmed"); // Status after successful booking
                                newBooking.setTutorName(tutorNameFromIntent); // Passed via Intent or fetched
                                FirebaseUser fbUser = mAuth.getCurrentUser();
                                newBooking.setTuteeName(fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty() ? fbUser.getDisplayName() : "You");
                            

                                Log.d(TAG, "Attempting to schedule reminder for booking: " + newBooking.getDocumentId());
                                checkAndScheduleReminder(newBooking);
                            } else {
                                Log.e(TAG, "Cannot schedule reminder: currentSelectedTimeSlot, currentUser, or newBookingDocumentId is null after booking confirmation.");
                            }
                            

                            showProgress(false, false); 
                            Intent intent = new Intent(SelectSlotActivity.this, TuteeBookingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                        } else {
                            showProgress(false, false);
                            String serverMessage = resultData != null ? (String) resultData.get("message") : "Unknown server error.";
                            Log.e(TAG, "confirmBooking function returned success=false or missing data. Message: " + serverMessage);
                            handleFlowFailure("Booking Confirmation Failed", "Server could not confirm booking. " + serverMessage);
                            resetUiAfterPaymentInteraction();
                        }
                    } catch (Exception e) {
                        showProgress(false, false);
                        Log.e(TAG, "Failed to parse result from 'confirmBooking' function.", e);
                        handleFlowFailure("Booking Confirmation Failed", "Error processing confirmation response from server.");
                        resetUiAfterPaymentInteraction();
                    }
                });
    }

    private void resetUiAfterPaymentInteraction() {
        if (selectedCalendarDate != null) {
            loadSlotsForDateAndPopulateModules(selectedCalendarDate);
        }
        buttonBookSelectedSlot.setEnabled(false);
        if (timeSlotAdapter != null) {
            timeSlotAdapter.clearSelection();
        }
        currentSelectedTimeSlot = null; 
    }

    private void showProgress(boolean show, boolean forSlotLoading) {
        if (forSlotLoading) {
            progressBarSlotsLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) { // Only adjust RV/NoSlotsMessage visibility when hiding slot loading progress
                recyclerViewDailySlots.setVisibility( (allSlotsForSelectedDate.isEmpty() || (timeSlotAdapter!=null && timeSlotAdapter.getItemCount()==0)) ? View.GONE : View.VISIBLE);
                if (availableModulesForDayList.isEmpty()) {
                    textViewNoSlotsMessage.setText("No available slots or modules for this date.");
                    textViewNoSlotsMessage.setVisibility(View.VISIBLE);
                } else if (timeSlotAdapter!=null && timeSlotAdapter.getItemCount() == 0 && spinnerSelectModuleForBooking.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
                    String selectedMod = spinnerSelectModuleForBooking.getSelectedItem().toString();
                    textViewNoSlotsMessage.setText("No slots available for " + selectedMod + " on this date.");
                    textViewNoSlotsMessage.setVisibility(View.VISIBLE);
                } else if (timeSlotAdapter!=null && timeSlotAdapter.getItemCount() > 0) {
                    textViewNoSlotsMessage.setVisibility(View.GONE);
                }
            } else { // When showing slot loading progress
                recyclerViewDailySlots.setVisibility(View.GONE);
                textViewNoSlotsMessage.setVisibility(View.GONE);
            }
        } else { // For booking/payment actions
            progressBarBookingAction.setVisibility(show ? View.VISIBLE : View.GONE);
            buttonBookSelectedSlot.setEnabled(!show && currentSelectedTimeSlot != null); // Re-enable only if a slot is still validly selected
        }
    }


    private void showErrorAndReset(@Nullable String title, @Nullable String message) {
        // This method is called when a payment or booking fails AFTER payment sheet.
        // Progress for booking action should be hidden by the caller or here.
        progressBarBookingAction.setVisibility(View.GONE);
        buttonBookSelectedSlot.setEnabled(currentSelectedTimeSlot != null); // Re-enable based on slot selection

        // Sensitive payment details are cleared in onPaymentSheetResult or callConfirmBookingFunction's completion

        if (message != null && !message.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(title != null ? title : "Error")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Log.d(TAG, "Resetting UI state without specific error dialog (e.g., payment cancelled).");
        }
        
        resetUiAfterPaymentInteraction();
    }

    private void handleFlowFailure(String title, String message) {
        Log.e(TAG, title + ": " + message);
        // Hide booking action progress specifically
        progressBarBookingAction.setVisibility(View.GONE);
        buttonBookSelectedSlot.setEnabled(currentSelectedTimeSlot != null); // Re-enable button
        // Display error to user
        showErrorAndReset(title, message); // showErrorAndReset already calls resetUiAfterPaymentInteraction
    }


    private String extractErrorMessage(Exception e) {
        String errorMessage = "An unknown error occurred.";
        if (e instanceof FirebaseFunctionsException) {
            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
            errorMessage = ffe.getMessage();
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                errorMessage = "Function error code: " + ffe.getCode();
            }
        } else if (e != null && e.getMessage() != null) {
            errorMessage = e.getMessage();
        }
        return errorMessage;
    }


    // --- NEW METHOD FOR ALARM SCHEDULING PERMISSION CHECK ---
    private void checkAndScheduleReminder(Booking booking) {
        if (booking == null || booking.getStartTime() == null || booking.getDocumentId() == null) {
            Log.e(TAG, "checkAndScheduleReminder: Invalid booking data provided for alarm.");
            return;
        }

        bookingToScheduleAfterPermission = booking; // Store in case we need to go to settings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Log.i(TAG, "SCHEDULE_EXACT_ALARM permission not granted by user. Requesting...");
                Toast.makeText(this, "Please grant 'Alarms & Reminders' permission for session notifications.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                
                try {
                    
                    startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM);
                } catch (Exception e) {
                    Log.e(TAG, "Could not open exact alarm settings activity", e);
                    Toast.makeText(this, "Could not open Alarms & Reminders settings. Please grant manually for session reminders to work.", Toast.LENGTH_LONG).show();
                    
                    AlarmScheduler.scheduleSessionReminder(SelectSlotActivity.this, bookingToScheduleAfterPermission);
                    bookingToScheduleAfterPermission = null; 
                }
                return; 
            }
        }
       
        Log.d(TAG, "Permission to schedule exact alarms is OK or not required. Scheduling reminder for booking: " + booking.getDocumentId());
        AlarmScheduler.scheduleSessionReminder(SelectSlotActivity.this, booking);
        bookingToScheduleAfterPermission = null; 
    }

    // --- HANDLE PERMISSION RESULT from Settings screen ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCHEDULE_EXACT_ALARM) {
            Log.d(TAG, "Returned from ACTION_REQUEST_SCHEDULE_EXACT_ALARM settings screen.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                    Log.i(TAG, "SCHEDULE_EXACT_ALARM permission has been GRANTED after returning from settings.");
                    if (bookingToScheduleAfterPermission != null) {
                        Log.d(TAG, "Scheduling reminder for previously stored booking: " + bookingToScheduleAfterPermission.getDocumentId());
                        AlarmScheduler.scheduleSessionReminder(SelectSlotActivity.this, bookingToScheduleAfterPermission);
                        Toast.makeText(this, "Reminder permission granted. Reminder scheduled!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "SCHEDULE_EXACT_ALARM permission still NOT granted after returning from settings.");
                    Toast.makeText(this, "Reminder permission not granted. Session reminders might not work correctly.", Toast.LENGTH_LONG).show();
                    // If you still want to try scheduling without exact permission (AlarmScheduler will log it)
                    if (bookingToScheduleAfterPermission != null) {
                        Log.d(TAG, "Attempting to schedule reminder anyway for booking: " + bookingToScheduleAfterPermission.getDocumentId());
                        AlarmScheduler.scheduleSessionReminder(SelectSlotActivity.this, bookingToScheduleAfterPermission);
                    }
                }
                bookingToScheduleAfterPermission = null; // Clear the temporarily stored booking
            }
        }
    }
}