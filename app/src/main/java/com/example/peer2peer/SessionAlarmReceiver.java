package com.example.peer2peer; 
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

import com.example.peer2peer.MainActivity; 
import com.example.peer2peer.R; 
import com.example.peer2peer.TuteeBookingsActivity; 

import java.util.Random;

public class SessionAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "SessionAlarmReceiver";
    public static final String SESSION_REMINDER_CHANNEL_ID = "session_reminders_channel";
    public static final String SESSION_REMINDER_CHANNEL_NAME = "Session Reminders";
    public static final String SESSION_REMINDER_CHANNEL_DESC = "Notifications for upcoming session reminders.";

    // Intent extras keys
    public static final String EXTRA_BOOKING_ID = "com.example.peer2peer.EXTRA_BOOKING_ID";
    public static final String EXTRA_SESSION_TITLE = "com.example.peer2peer.EXTRA_SESSION_TITLE"; 
    public static final String EXTRA_SESSION_DESCRIPTION = "com.example.peer2peer.EXTRA_SESSION_DESCRIPTION"; 
    public static final String EXTRA_NOTIFICATION_ID = "com.example.peer2peer.EXTRA_NOTIFICATION_ID";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        String bookingId = intent.getStringExtra(EXTRA_BOOKING_ID);
        String sessionTitle = intent.getStringExtra(EXTRA_SESSION_TITLE);
        String sessionDescription = intent.getStringExtra(EXTRA_SESSION_DESCRIPTION);
        
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, new Random().nextInt());


        if (sessionTitle == null || sessionDescription == null) {
            Log.e(TAG, "Alarm received with missing title or description. Booking ID: " + bookingId);
            sessionTitle = "Upcoming Session Reminder"; 
            sessionDescription = "You have an upcoming session soon!"; 
        }

        Log.d(TAG, "Displaying notification for Booking ID: " + bookingId + ", Title: " + sessionTitle);

        
        Intent tapIntent = new Intent(context, TuteeBookingsActivity.class); 
        if (bookingId != null) {
            tapIntent.putExtra("HIGHLIGHT_BOOKING_ID", bookingId); 
        }
        tapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingTapIntent = PendingIntent.getActivity(context,
                ("tap_" + bookingId).hashCode(), 
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, SESSION_REMINDER_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_notification) 
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

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    SESSION_REMINDER_CHANNEL_ID,
                    SESSION_REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // High importance for reminders
            );
            channel.setDescription(SESSION_REMINDER_CHANNEL_DESC);
            
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(notificationId, notificationBuilder.build());
        Log.d(TAG, "Session reminder notification sent with ID: " + notificationId);
    }
}