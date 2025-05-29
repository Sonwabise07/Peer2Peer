package com.example.peer2peer.adapters;

import android.content.Context;
import android.content.Intent; // ADDED for starting ChatActivity
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; // ADDED for the new message button
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.Booking;
import com.example.peer2peer.R;
// ---- ADD THIS IMPORT ----
import com.example.peer2peer.ChatActivity;
// --------------------------
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth; // ADDED for getting current user
import com.google.firebase.auth.FirebaseUser; // ADDED for FirebaseUser object

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BookingListAdapter extends RecyclerView.Adapter<BookingListAdapter.BookingViewHolder> {

    private static final String TAG = "BookingListAdapter";

    public interface OnBookingInteractionListener {
        void onJoinMeetingClick(String meetingLink);
        void onRateSessionClick(Booking booking);
        void onItemClick(Booking booking);
        // void onMessageTutorClick(String tutorUid, String tutorName); // Optional: if you want to delegate to activity
    }

    private final OnBookingInteractionListener listener;
    private final Context context;
    private final List<Booking> bookingList;
    private final SimpleDateFormat dateFormatterListItem;
    private final SimpleDateFormat timeFormatterListItem;
    private final DisplayMode displayMode;

    public enum DisplayMode {TUTEE_VIEW, TUTOR_VIEW}

    private static final long JOIN_WINDOW_MINUTES_BEFORE = 10;

    public BookingListAdapter(Context context, List<Booking> bookingList, DisplayMode mode, OnBookingInteractionListener listener) {
        this.context = context;
        this.bookingList = bookingList;
        this.displayMode = mode;
        this.listener = listener;
        this.dateFormatterListItem = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        this.timeFormatterListItem = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Log.d(TAG, "Adapter CONSTRUCTOR: Initial bookingList (shared instance) size: " + (this.bookingList != null ? this.bookingList.size() : "null"));
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_booking, parent, false);
        return new BookingViewHolder(view, listener, displayMode, dateFormatterListItem, timeFormatterListItem);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        if (booking == null) {
            Log.e(TAG, "onBindViewHolder: Booking object at position " + position + " is NULL. Skipping bind.");
            // Consider setting default/empty state for the holder's views here if necessary
            return;
        }
        holder.bind(booking, context);
    }

    @Override
    public int getItemCount() {
        return (bookingList == null) ? 0 : bookingList.size();
    }

    public void setBookings(List<Booking> bookingsParameter) {
        Log.d(TAG, "ADAPTER: setBookings called. The shared list (this.bookingList) should already be updated by the Activity.");
        // ... (rest of your setBookings method) ...
        notifyDataSetChanged();
        Log.d(TAG, "ADAPTER: notifyDataSetChanged() called. Final list size for getItemCount: " + this.bookingList.size());
    }


    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView primaryNameLabel, primaryNameValue;
        TextView moduleCodeLabel, moduleCodeValue;
        TextView dateLabel, dateValue, timeLabel, timeValue, statusValue;
        Button joinMeetingButton, rateSessionButton;
        ImageButton buttonMessageTutorFromBooking; // ADDED: Message button

        private final OnBookingInteractionListener listener;
        private final DisplayMode displayMode;
        private final SimpleDateFormat dateFormatter;
        private final SimpleDateFormat timeFormatter;

        public BookingViewHolder(@NonNull View itemView, OnBookingInteractionListener listener, DisplayMode mode,
                                 SimpleDateFormat dateFormatter, SimpleDateFormat timeFormatter) {
            super(itemView);
            this.listener = listener;
            this.displayMode = mode;
            this.dateFormatter = dateFormatter;
            this.timeFormatter = timeFormatter;

            primaryNameLabel = itemView.findViewById(R.id.textView_primary_name_label);
            primaryNameValue = itemView.findViewById(R.id.textView_primary_name_value);
            moduleCodeLabel = itemView.findViewById(R.id.textView_module_code_label);
            moduleCodeValue = itemView.findViewById(R.id.textView_module_code_value);
            dateLabel = itemView.findViewById(R.id.textView_date_label);
            dateValue = itemView.findViewById(R.id.textView_date_value);
            timeLabel = itemView.findViewById(R.id.textView_time_label);
            timeValue = itemView.findViewById(R.id.textView_time_value);
            statusValue = itemView.findViewById(R.id.textView_status_value);
            joinMeetingButton = itemView.findViewById(R.id.button_join_meeting);
            rateSessionButton = itemView.findViewById(R.id.button_rate_session);
            buttonMessageTutorFromBooking = itemView.findViewById(R.id.button_message_tutor_from_booking); // ADDED: Initialize
        }

        void bind(final Booking booking, Context context) {
            if (booking == null) {
                Log.e(TAG, "Binding null booking object in ViewHolder! Setting defaults.");
                primaryNameValue.setText("N/A");
                moduleCodeValue.setText("N/A");
                dateValue.setText("N/A");
                timeValue.setText("N/A");
                statusValue.setText("UNKNOWN");
                joinMeetingButton.setVisibility(View.GONE);
                rateSessionButton.setVisibility(View.GONE);
                buttonMessageTutorFromBooking.setVisibility(View.GONE); // ADDED: Hide for null booking
                if (moduleCodeLabel != null) moduleCodeLabel.setVisibility(View.GONE);
                return;
            }

            final String meetingLink = booking.getMeetingLink();

            if (displayMode == DisplayMode.TUTEE_VIEW) {
                primaryNameLabel.setText("Tutor:");
                primaryNameValue.setText(TextUtils.isEmpty(booking.getTutorName()) ? "N/A" : booking.getTutorName());
            } else { // TUTOR_VIEW
                primaryNameLabel.setText("Tutee:");
                primaryNameValue.setText(TextUtils.isEmpty(booking.getTuteeName()) ? "N/A" : booking.getTuteeName());
            }

            // ... (your existing module, date, time, and status binding logic) ...
            if (booking.getModuleCode() != null && !booking.getModuleCode().isEmpty()) {
                moduleCodeValue.setText(booking.getModuleCode());
                moduleCodeValue.setVisibility(View.VISIBLE);
                if (moduleCodeLabel != null) moduleCodeLabel.setVisibility(View.VISIBLE);
            } else {
                moduleCodeValue.setVisibility(View.GONE);
                if (moduleCodeLabel != null) moduleCodeLabel.setVisibility(View.GONE);
            }

            String formattedDate = "N/A";
            String formattedTimeRange = "N/A";
            Timestamp startTimeStamp = booking.getStartTime();
            Timestamp endTimeStamp = booking.getEndTime();

            if (startTimeStamp != null) {
                Date startDate = startTimeStamp.toDate();
                formattedDate = dateFormatter.format(startDate);
                formattedTimeRange = timeFormatter.format(startDate);
                if (endTimeStamp != null) {
                    Date endDate = endTimeStamp.toDate();
                    formattedTimeRange += " - " + timeFormatter.format(endDate);
                }
            }
            dateValue.setText(formattedDate);
            timeValue.setText(formattedTimeRange);

            Date now = new Date();
            String currentBookingStatusString = booking.getBookingStatus() != null ? booking.getBookingStatus().trim().toLowerCase(Locale.ROOT) : "";
            boolean isSessionEndTimePast = (endTimeStamp != null) && endTimeStamp.toDate().before(now);

            String statusDisplay = "UNKNOWN";
            int statusColor = ContextCompat.getColor(context, R.color.grey_700); // Ensure grey_700 is defined
            itemView.setAlpha(1.0f);

            switch (currentBookingStatusString) {
                case "confirmed":
                case "payment_successful":
                    if (isSessionEndTimePast) {
                        statusDisplay = "SESSION ENDED";
                        statusColor = ContextCompat.getColor(context, R.color.grey_500); // Ensure grey_500 is defined
                        itemView.setAlpha(0.7f);
                    } else {
                        statusDisplay = "CONFIRMED";
                        statusColor = ContextCompat.getColor(context, R.color.green_500); // Ensure green_500 is defined
                    }
                    break;
                // ... (rest of your status switch cases) ...
                case "completed":
                    statusDisplay = "COMPLETED";
                    statusColor = ContextCompat.getColor(context, R.color.blue_500); // Ensure blue_500 is defined
                    itemView.setAlpha(0.7f);
                    break;
                case "cancelled_by_tutor":
                case "cancelled_by_tutee":
                case "cancelled_by_admin":
                case "payment_canceled":
                    statusDisplay = "CANCELLED";
                    statusColor = ContextCompat.getColor(context, R.color.red_500); // Ensure red_500 is defined
                    itemView.setAlpha(0.7f);
                    break;
                case "payment_failed":
                    statusDisplay = "PAYMENT FAILED";
                    statusColor = ContextCompat.getColor(context, R.color.red_500);
                    itemView.setAlpha(0.8f);
                    break;
                case "pending_payment":
                    statusDisplay = "PENDING PAYMENT";
                    statusColor = ContextCompat.getColor(context, R.color.orange_500); // Ensure orange_500 is defined
                    itemView.setAlpha(0.8f);
                    break;
                default:
                    if (TextUtils.isEmpty(currentBookingStatusString)) {
                        statusDisplay = "UNKNOWN";
                    } else {
                        String[] parts = currentBookingStatusString.split("_");
                        StringBuilder sb = new StringBuilder();
                        for (String part : parts) {
                            if (part.length() > 0) {
                                sb.append(Character.toUpperCase(part.charAt(0)))
                                        .append(part.substring(1).toLowerCase(Locale.ROOT))
                                        .append(" ");
                            }
                        }
                        statusDisplay = sb.toString().trim();
                    }
                    statusColor = ContextCompat.getColor(context, R.color.grey_700);
                    break;
            }
            statusValue.setText(statusDisplay);
            statusValue.setTextColor(statusColor);


            if (displayMode == DisplayMode.TUTEE_VIEW) {
                boolean isConfirmedAndNotYetOver = ("confirmed".equals(currentBookingStatusString) || "payment_successful".equals(currentBookingStatusString))
                        && !isSessionEndTimePast;
                // ... (join meeting button logic) ...
                final boolean isJoinWindowActive;
                if (startTimeStamp != null && endTimeStamp != null) {
                    long currentTimeMillis = System.currentTimeMillis();
                    long allowJoinStartTimeMillis = startTimeStamp.toDate().getTime() - TimeUnit.MINUTES.toMillis(JOIN_WINDOW_MINUTES_BEFORE);
                    long sessionActualEndTimeMillis = endTimeStamp.toDate().getTime();
                    isJoinWindowActive = (currentTimeMillis >= allowJoinStartTimeMillis && currentTimeMillis < sessionActualEndTimeMillis);
                } else {
                    isJoinWindowActive = false;
                }

                if (isConfirmedAndNotYetOver && !TextUtils.isEmpty(meetingLink) && !"null".equalsIgnoreCase(meetingLink)) {
                    joinMeetingButton.setVisibility(View.VISIBLE);
                    joinMeetingButton.setEnabled(isJoinWindowActive);
                    joinMeetingButton.setAlpha(isJoinWindowActive ? 1.0f : 0.5f);
                    joinMeetingButton.setOnClickListener(v -> {
                        if (isJoinWindowActive && listener != null) {
                            listener.onJoinMeetingClick(meetingLink);
                        } else if (joinMeetingButton.getVisibility() == View.VISIBLE) {
                            Toast.makeText(context, "Session join window is not active.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    joinMeetingButton.setVisibility(View.GONE);
                }

                // ... (rate session button logic) ...
                boolean canRateSession = false;
                if ( ( "confirmed".equalsIgnoreCase(currentBookingStatusString) ||
                        "payment_successful".equalsIgnoreCase(currentBookingStatusString) ||
                        "completed".equalsIgnoreCase(currentBookingStatusString) ) &&
                        isSessionEndTimePast &&
                        !booking.isRated ) { // Assuming Booking class has an `isRated` boolean field
                    canRateSession = true;
                }


                if (canRateSession) {
                    rateSessionButton.setVisibility(View.VISIBLE);
                    rateSessionButton.setOnClickListener(v -> {
                        if (listener != null) listener.onRateSessionClick(booking);
                    });
                } else {
                    rateSessionButton.setVisibility(View.GONE);
                }
                itemView.setOnClickListener(null);

                // ADDED: Logic for the message tutor button
                if (booking.getTutorUid() != null && !booking.getTutorUid().isEmpty() &&
                        booking.getTutorName() != null && !booking.getTutorName().isEmpty() &&
                        !"cancelled".equals(statusDisplay.toLowerCase())) { // Don't show for cancelled bookings
                    buttonMessageTutorFromBooking.setVisibility(View.VISIBLE);
                    buttonMessageTutorFromBooking.setOnClickListener(v -> {
                        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentFirebaseUser == null) {
                            Toast.makeText(context, "You need to be logged in to send a message.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Now ChatActivity can be resolved due to the import
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_ID, booking.getTutorUid());
                        intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_NAME, booking.getTutorName());
                        context.startActivity(intent);
                    });
                } else {
                    buttonMessageTutorFromBooking.setVisibility(View.GONE);
                }

            } else { // TUTOR_VIEW
                joinMeetingButton.setVisibility(View.GONE);
                rateSessionButton.setVisibility(View.GONE);
                buttonMessageTutorFromBooking.setVisibility(View.GONE); // ADDED: Hide for tutor view as well
                itemView.setClickable(true);
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onItemClick(booking);
                });
            }
        }
    }
}