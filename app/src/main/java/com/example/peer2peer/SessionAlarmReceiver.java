package com.example.peer2peer; // Or a more suitable package like .receivers or .utils

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.peer2peer.MainActivity; // Default activity to open, can be changed
import com.example.peer2peer.R; // For R.drawable.ic_stat_notification
import com.example.peer2peer.TuteeBookingsActivity; // Example: Open bookings list

import java.util.Random;

public class SessionAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "SessionAlarmReceiver";
    public static final String SESSION_REMINDER_CHANNEL_ID = "session_reminders_channel";
    public static final String SESSION_REMINDER_CHANNEL_NAME = "Session Reminders";
    public static final String SESSION_REMINDER_CHANNEL_DESC = "Notifications for upcoming session reminders.";

    // Intent extras keys
    public static final String EXTRA_BOOKING_ID = "com.example.peer2peer.EXTRA_BOOKING_ID";
    public static final String EXTRA_SESSION_TITLE = "com.example.peer2peer.EXTRA_SESSION_TITLE"; // e.g., "Session with [Name]"
    public static final String EXTRA_SESSION_DESCRIPTION = "com.example.peer2peer.EXTRA_SESSION_DESCRIPTION"; // e.g., "Your session for [Module] is starting in 10 minutes."
    public static final String EXTRA_NOTIFICATION_ID = "com.example.peer2peer.EXTRA_NOTIFICATION_ID";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        String bookingId = intent.getStringExtra(EXTRA_BOOKING_ID);
        String sessionTitle = intent.getStringExtra(EXTRA_SESSION_TITLE);
        String sessionDescription = intent.getStringExtra(EXTRA_SESSION_DESCRIPTION);
        // Use a consistent notification ID based on bookingId to allow updating/cancelling if needed,
        // or a new one if each alarm trigger should be a new notification.
        // For simplicity, using bookingId hash code.
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, new Random().nextInt());


        if (sessionTitle == null || sessionDescription == null) {
            Log.e(TAG, "Alarm received with missing title or description. Booking ID: " + bookingId);
            sessionTitle = "Upcoming Session Reminder"; // Fallback title
            sessionDescription = "You have an upcoming session soon!"; // Fallback description
        }

        Log.d(TAG, "Displaying notification for Booking ID: " + bookingId + ", Title: " + sessionTitle);

        // Intent to launch when notification is tapped
        // This should ideally take the user to the specific session or their bookings list
        Intent tapIntent = new Intent(context, TuteeBookingsActivity.class); // Or TutorScheduleActivity, or a SessionDetailActivity
        if (bookingId != null) {
            tapIntent.putExtra("HIGHLIGHT_BOOKING_ID", bookingId); // So the activity can highlight/scroll to it
        }
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingTapIntent = PendingIntent.getActivity(context,
                ("tap_" + bookingId).hashCode(), // Unique request code for the tap action
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, SESSION_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_notification) // Ensure this icon exists and is monochrome
                        .setContentTitle(sessionTitle)
                        .setContentText(sessionDescription)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(sessionDescription)) // For longer text
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // Important for reminders
                        .setDefaults(NotificationCompat.DEFAULT_ALL)   // Vibrate, sound, lights
                        .setContentIntent(pendingTapIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SESSION_REMINDER_CHANNEL_ID,
                    SESSION_REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // High importance for reminders
            );
            channel.setDescription(SESSION_REMINDER_CHANNEL_DESC);
            // Configure channel (e.g., lights, vibration pattern)
            // channel.enableLights(true);
            // channel.setLightColor(Color.BLUE);
            // channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(notificationId, notificationBuilder.build());
        Log.d(TAG, "Session reminder notification sent with ID: " + notificationId);
    }
}