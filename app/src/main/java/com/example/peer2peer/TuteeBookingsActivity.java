package com.example.peer2peer;

// --- Android/Java Imports ---
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Keep if used, or remove if not
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peer2peer.adapters.BookingListAdapter;
// Import your Booking model and AlarmScheduler
import com.example.peer2peer.Booking;
import com.example.peer2peer.AlarmScheduler; // **** ADD THIS IMPORT ****

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
// import com.google.firebase.firestore.FirebaseFirestoreException; // Keep if needed for specific error handling

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
// Removed: import java.util.concurrent.TimeUnit; // AlarmScheduler handles this internal detail

public class TuteeBookingsActivity extends AppCompatActivity
        implements BookingListAdapter.OnBookingInteractionListener {

    private static final String TAG = "TuteeBookingsActivity";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // UI Elements
    private Toolbar toolbar;
    private CalendarView calendarViewBookings;
    private RecyclerView recyclerViewBookings;
    private TextView textViewNoBookings;
    private ProgressBar progressBar;

    // RecyclerView Components
    private BookingListAdapter adapter;
    private List<Booking> displayBookingList;

    // Selected Date
    private Calendar selectedDateCalendar;

    private final ActivityResultLauncher<Intent> submitReviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "Review submitted successfully, refreshing bookings for selected date.");
                    fetchBookingsForSelectedDate();
                } else {
                    Log.d(TAG, "Review submission cancelled or failed (resultCode: " + result.getResultCode() + ")");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_bookings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        toolbar = findViewById(R.id.toolbar_tutee_bookings); // Ensure this ID is in your XML
        calendarViewBookings = findViewById(R.id.calendarView_bookings);
        recyclerViewBookings = findViewById(R.id.recyclerView_tutee_bookings);
        textViewNoBookings = findViewById(R.id.textView_no_bookings);
        progressBar = findViewById(R.id.progressBar_tutee_bookings);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("My Bookings");
        }

        displayBookingList = new ArrayList<>();
        // Ensure BookingListAdapter is in com.example.peer2peer.adapters package
        adapter = new BookingListAdapter(this, displayBookingList, BookingListAdapter.DisplayMode.TUTEE_VIEW, this);
        recyclerViewBookings.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBookings.setAdapter(adapter);

        if (currentUser == null) {
            Log.e(TAG, "User is not logged in! Finishing activity.");
            Toast.makeText(this, "Error: User not logged in.", Toast.LENGTH_SHORT).show();
            finish(); // Or redirect to LoginActivity
            return;
        }
        Log.d(TAG, "Current logged-in user UID: " + currentUser.getUid());

        selectedDateCalendar = Calendar.getInstance(TimeZone.getDefault());
        setupCalendarViewListener();
        Log.d(TAG, "onCreate: Calling initial fetchBookingsForSelectedDate for today.");
        fetchBookingsForSelectedDate();
    }

    private void setupCalendarViewListener() {
        calendarViewBookings.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Log.d(TAG, "CalendarView OnDateChange: year=" + year + ", month=" + month + ", dayOfMonth=" + dayOfMonth);
            selectedDateCalendar.set(year, month, dayOfMonth);
            String selectedDateStr = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(selectedDateCalendar.getTime());
            Log.d(TAG, "Date selected via CalendarView: " + selectedDateStr);
            fetchBookingsForSelectedDate();
        });
    }

    private void fetchBookingsForSelectedDate() {
        if (currentUser == null) {
            Log.e(TAG, "fetchBookingsForSelectedDate: Cannot fetch bookings, user is null.");
            textViewNoBookings.setText("User not logged in. Please log in and try again.");
            textViewNoBookings.setVisibility(View.VISIBLE);
            recyclerViewBookings.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            return;
        }
        if (selectedDateCalendar == null) {
            Log.e(TAG, "fetchBookingsForSelectedDate: selectedDateCalendar is null. Defaulting to today.");
            selectedDateCalendar = Calendar.getInstance(TimeZone.getDefault());
        }

        Calendar startOfDay = (Calendar) selectedDateCalendar.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0);
        Timestamp startTimestamp = new Timestamp(startOfDay.getTime());

        Calendar endOfDay = (Calendar) selectedDateCalendar.clone();
        endOfDay.add(Calendar.DAY_OF_MONTH, 1); // Start of the next day
        endOfDay.set(Calendar.HOUR_OF_DAY, 0); endOfDay.set(Calendar.MINUTE, 0);
        endOfDay.set(Calendar.SECOND, 0); endOfDay.set(Calendar.MILLISECOND, 0);
        Timestamp endTimestamp = new Timestamp(endOfDay.getTime());

        String selectedDateStringForQuery = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDateCalendar.getTime());
        Log.d(TAG, "Querying bookings for tuteeUid: " + currentUser.getUid() + " on " + selectedDateStringForQuery);

        showLoading(true);

        db.collection("bookings")
                .whereEqualTo("tuteeUid", currentUser.getUid())
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        List<Booking> fetchedBookings = new ArrayList<>();
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null) {
                            Log.d(TAG, "Firestore query successful. Documents found: " + snapshots.size());
                            for (QueryDocumentSnapshot document : snapshots) {
                                try {
                                    Booking booking = document.toObject(Booking.class);
                                    if (booking != null) {
                                        booking.setDocumentId(document.getId()); // Set document ID
                                        fetchedBookings.add(booking);

                                        // **** SCHEDULE ALARM FOR UPCOMING, CONFIRMED BOOKINGS ****
                                        if ("confirmed".equalsIgnoreCase(booking.getBookingStatus()) &&
                                                booking.getStartTime() != null &&
                                                booking.getStartTime().toDate().after(new Date())) { // Check if session is in the future

                                            Log.i(TAG, "Attempting to schedule reminder for booking: " + booking.getDocumentId());
                                            AlarmScheduler.scheduleSessionReminder(getApplicationContext(), booking);
                                        }
                                        // *********************************************************

                                    } else {
                                        Log.w(TAG, "Document " + document.getId() + " resulted in a NULL Booking object.");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing booking document ID: " + document.getId(), e);
                                }
                            }
                        } else {
                            Log.w(TAG, "Firestore query successful, but snapshots object was null.");
                        }
                        updateAdapterWithDailyBookings(fetchedBookings);
                    } else {
                        Log.e(TAG, "Error fetching bookings for " + selectedDateStringForQuery + ": ", task.getException());
                        Toast.makeText(TuteeBookingsActivity.this, "Failed to load bookings.", Toast.LENGTH_SHORT).show();
                        updateAdapterWithDailyBookings(new ArrayList<>());
                    }
                });
    }

    private void updateAdapterWithDailyBookings(List<Booking> dailyBookings) {
        List<Booking> validBookings = new ArrayList<>();
        if (dailyBookings != null) {
            for (Booking b : dailyBookings) {
                if (b != null && b.getStartTime() != null) { // Ensure booking and startTime are not null
                    validBookings.add(b);
                } else {
                    Log.w(TAG, "A fetched booking or its startTime was null. Skipping.");
                }
            }
        }
        Log.d(TAG, "Updating adapter with " + validBookings.size() + " valid bookings.");

        displayBookingList.clear();
        displayBookingList.addAll(validBookings);

        Collections.sort(displayBookingList, (b1, b2) -> b1.getStartTime().compareTo(b2.getStartTime()));

        adapter.setBookings(displayBookingList); // Assuming adapter has setBookings method

        if (displayBookingList.isEmpty()) {
            textViewNoBookings.setText("No bookings found for the selected date.");
            textViewNoBookings.setVisibility(View.VISIBLE);
            recyclerViewBookings.setVisibility(View.GONE);
        } else {
            textViewNoBookings.setVisibility(View.GONE);
            recyclerViewBookings.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            if (recyclerViewBookings != null) recyclerViewBookings.setVisibility(View.GONE);
            if (textViewNoBookings != null) textViewNoBookings.setVisibility(View.GONE);
        }
    }

    @Override
    public void onJoinMeetingClick(String meetingLink) {
        Log.d(TAG, "Join Meeting clicked. Link: " + meetingLink);
        if (TextUtils.isEmpty(meetingLink) || "null".equalsIgnoreCase(meetingLink)) {
            Toast.makeText(this, "Meeting link is missing or not yet available.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No application can handle this meeting link.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening meeting link: " + meetingLink, e);
            Toast.makeText(this, "Could not open meeting link.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRateSessionClick(Booking booking) {
        if (booking == null || booking.getDocumentId() == null || booking.getTutorUid() == null) {
            Toast.makeText(this, "Error: Session data incomplete for rating.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Rate Session clicked for booking ID: " + booking.getDocumentId() + ", Tutor ID: " + booking.getTutorUid());
        Intent intent = new Intent(this, SubmitReviewActivity.class);
        intent.putExtra(SubmitReviewActivity.EXTRA_BOOKING_ID, booking.getDocumentId());
        intent.putExtra(SubmitReviewActivity.EXTRA_TUTOR_UID, booking.getTutorUid());
        intent.putExtra(SubmitReviewActivity.EXTRA_TUTOR_NAME, booking.getTutorName());
        submitReviewLauncher.launch(intent);
    }

    @Override
    public void onItemClick(Booking booking) {
        Log.d(TAG, "Booking item clicked: " + (booking != null ? booking.getDocumentId() : "null booking"));
        // You can add navigation to a BookingDetailActivity here if you want
        // For now, it's handled by specific button clicks (Join, Rate)
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Calling fetchBookingsForSelectedDate.");
        fetchBookingsForSelectedDate(); // Refresh bookings when activity resumes
    }

    // Helper to capitalize first letter (if you don't have it in a utility class)
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}