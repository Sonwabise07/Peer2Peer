package com.example.peer2peer;

import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Keep this if needed later
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
// Removed duplicate Toast import

// Import the adapter and Booking model
import com.example.peer2peer.adapters.BookingListAdapter;
import com.example.peer2peer.Booking;

// Keep necessary Firebase imports
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
// Removed unused imports like Comparator, OnCompleteListener, Task, Objects

// --- MODIFIED: Implement the single listener interface ---
public class TutorScheduleActivity extends AppCompatActivity implements BookingListAdapter.OnBookingInteractionListener {

    private static final String TAG = "TutorScheduleActivity";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // UI Elements
    private Toolbar toolbar;
    private RecyclerView recyclerViewSchedule;
    private TextView textViewNoSchedule;
    private ProgressBar progressBar;

    // RecyclerView Components
    private BookingListAdapter adapter;
    private List<Booking> displayBookingList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_schedule);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        toolbar = findViewById(R.id.toolbar_tutor_schedule);
        recyclerViewSchedule = findViewById(R.id.recyclerView_tutor_schedule);
        textViewNoSchedule = findViewById(R.id.textView_no_schedule);
        progressBar = findViewById(R.id.progressBar_tutor_schedule);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("My Schedule");
        }

        displayBookingList = new ArrayList<>();
        // --- MODIFIED: Pass 'this' as the 4th argument (the listener) ---
        adapter = new BookingListAdapter(this, displayBookingList, BookingListAdapter.DisplayMode.TUTOR_VIEW, this);
        recyclerViewSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSchedule.setAdapter(adapter);

        // --- REMOVED: adapter.setOnItemClickListener(this); ---

        if (currentUser == null) {
            Log.e(TAG, "Tutor is not logged in!");
            Toast.makeText(this, "Error: Tutor not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchScheduledBookings();
        Log.d(TAG, "Activity created for Tutor: " + currentUser.getUid());
    }

    // --- Implementation of the methods from OnBookingInteractionListener ---

    @Override
    public void onItemClick(Booking booking) {
        // This is the method that gets called for TUTOR_VIEW item clicks
        Log.d(TAG, "Clicked on booking with Tutee: " + (booking.getTuteeName() != null ? booking.getTuteeName() : booking.getTuteeUid()) + ", ID: " + booking.getDocumentId());
        showAddLinkDialog(booking); // Show dialog to add/edit meeting link
    }

    @Override
    public void onJoinMeetingClick(String meetingLink) {
        // Not used in this activity, but must be implemented
        Log.w(TAG, "onJoinMeetingClick called unexpectedly in TutorScheduleActivity");
    }

    @Override
    public void onRateSessionClick(Booking booking) {
        // Not used in this activity, but must be implemented
        Log.w(TAG, "onRateSessionClick called unexpectedly in TutorScheduleActivity");
    }
    // --- End listener method implementations ---


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchScheduledBookings() {
        // Keep your existing fetchScheduledBookings logic
        Log.d(TAG, "Fetching schedule for tutor: " + currentUser.getUid());
        showLoading(true);

        db.collection("bookings")
                .whereEqualTo("tutorUid", currentUser.getUid())
                .orderBy("startTime", Query.Direction.ASCENDING) // Keep ascending for upcoming sort
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Schedule fetched successfully.");
                        List<Booking> fetchedBookings = new ArrayList<>();
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot document : snapshots) {
                                try {
                                    Booking booking = document.toObject(Booking.class);
                                    if (booking != null) {
                                        // ID should be set by @DocumentId in Booking model
                                        // booking.setBookingId(document.getId());
                                        fetchedBookings.add(booking);
                                    } else {
                                        Log.w(TAG, "Booking object null for doc: " + document.getId());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing booking document: " + document.getId(), e);
                                }
                            }
                        } else {
                            Log.d(TAG, "No booking documents found for this tutor.");
                        }
                        processAndDisplayBookings(fetchedBookings);
                    } else {
                        Log.e(TAG, "Error fetching schedule: ", task.getException());
                        Toast.makeText(TutorScheduleActivity.this, "Failed to load schedule.", Toast.LENGTH_SHORT).show();
                        displayBookingList.clear();
                        processAndDisplayBookings(displayBookingList); // Update empty state display
                        textViewNoSchedule.setText("Error loading schedule.");
                    }
                });
    }

    private void processAndDisplayBookings(List<Booking> fetchedBookings) {
        // Keep your existing sorting logic
        List<Booking> upcomingBookings = new ArrayList<>();
        List<Booking> pastOrOtherBookings = new ArrayList<>();
        Date now = new Date();

        for (Booking booking : fetchedBookings) {
            if (booking.getStartTime() != null && booking.getStartTime().toDate().after(now) &&
                    ("confirmed".equalsIgnoreCase(booking.getBookingStatus()) || "payment_successful".equalsIgnoreCase(booking.getBookingStatus()))) {
                upcomingBookings.add(booking);
            } else {
                pastOrOtherBookings.add(booking);
            }
        }

        Collections.sort(pastOrOtherBookings, (b1, b2) -> {
            Timestamp t1 = b1.getStartTime();
            Timestamp t2 = b2.getStartTime();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        displayBookingList.clear();
        displayBookingList.addAll(upcomingBookings);
        displayBookingList.addAll(pastOrOtherBookings);

        Log.d(TAG, "Updating adapter. Final display list size: " + displayBookingList.size());

        if (displayBookingList.isEmpty()) {
            textViewNoSchedule.setText("No schedule found.");
            textViewNoSchedule.setVisibility(View.VISIBLE);
            recyclerViewSchedule.setVisibility(View.GONE);
        } else {
            textViewNoSchedule.setVisibility(View.GONE);
            recyclerViewSchedule.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }


    private void showAddLinkDialog(final Booking booking) {
        // Keep your existing dialog logic
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add/Edit Meeting Link");

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_link, null);
        final EditText editTextLink = dialogView.findViewById(R.id.editText_meeting_link);

        if (booking.getMeetingLink() != null && !booking.getMeetingLink().isEmpty()) {
            editTextLink.setText(booking.getMeetingLink());
        }
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String meetingLink = editTextLink.getText().toString().trim();
            if (booking.getDocumentId() == null || booking.getDocumentId().isEmpty()) {
                Log.e(TAG, "Cannot update meeting link: Booking ID is missing.");
                Toast.makeText(TutorScheduleActivity.this, "Error: Could not identify booking.", Toast.LENGTH_LONG).show();
                return;
            }
            updateMeetingLinkInFirestore(booking, meetingLink, dialog);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateMeetingLinkInFirestore(final Booking booking, String meetingLink, DialogInterface dialog) {
        // Keep your existing update logic
        DocumentReference bookingRef = db.collection("bookings").document(booking.getDocumentId());
        bookingRef.update("meetingLink", meetingLink)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Meeting link updated successfully for booking: " +booking.getDocumentId());
                    Toast.makeText(TutorScheduleActivity.this, "Meeting link saved!", Toast.LENGTH_SHORT).show();
                    // Efficiently update the item in the list
                    int position = displayBookingList.indexOf(booking);
                    if (position != -1) {
                        displayBookingList.get(position).setMeetingLink(meetingLink);
                        adapter.notifyItemChanged(position);
                    } else {
                        fetchScheduledBookings(); // Fallback to full refresh if item not found
                    }
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating meeting link for booking: " + booking.getDocumentId(), e);
                    Toast.makeText(TutorScheduleActivity.this, "Error saving link: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showLoading(boolean isLoading) {
        // Keep your existing loading logic
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewSchedule != null) {
            recyclerViewSchedule.setVisibility(isLoading ? View.GONE : (displayBookingList.isEmpty() ? View.GONE : View.VISIBLE));
        }
        if (textViewNoSchedule != null) {
            textViewNoSchedule.setVisibility(isLoading ? View.GONE : (displayBookingList.isEmpty() ? View.VISIBLE : View.GONE));
        }
    }

} // End of Activity