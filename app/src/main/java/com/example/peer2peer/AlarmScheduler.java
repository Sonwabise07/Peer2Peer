package com.example.peer2peer; 
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


import com.example.peer2peer.Booking;

import com.example.peer2peer.SessionAlarmReceiver;

import com.google.firebase.Timestamp;


import java.util.concurrent.TimeUnit;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private static final long REMINDER_OFFSET_MINUTES = 10; // 10 minutes before

    public static void scheduleSessionReminder(Context context, Booking booking) {
        
        if (booking == null || booking.getStartTime() == null || booking.getDocumentId() == null) {
            Log.e(TAG, "Cannot schedule reminder, booking data is invalid (missing start time or documentId).");
            return;
        }

        Timestamp sessionStartTime = booking.getStartTime();
        long sessionStartTimeMillis = sessionStartTime.toDate().getTime();
        long reminderTimeMillis = sessionStartTimeMillis - TimeUnit.MINUTES.toMillis(REMINDER_OFFSET_MINUTES);

        
        if (reminderTimeMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Session reminder time for booking " + booking.getDocumentId() + " is in the past. Not scheduling.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available.");
            return;
        }

       
        Intent intent = new Intent(context, SessionAlarmReceiver.class);
        String sessionTitle = "Session Reminder";
        String sessionDescription = "Your session for " + (booking.getModuleCode() != null ? booking.getModuleCode() : "your lesson");

        sessionDescription += " with " + (booking.getTutorName() != null ? booking.getTutorName() : "your tutor/tutee");
        sessionDescription += " is starting in " + REMINDER_OFFSET_MINUTES + " minutes.";

        intent.putExtra(SessionAlarmReceiver.EXTRA_BOOKING_ID, booking.getDocumentId()); 
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_TITLE, sessionTitle);
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_DESCRIPTION, sessionDescription);

        
        int notificationId = booking.getDocumentId().hashCode();
        intent.putExtra(SessionAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId);


        
        int requestCode = ("reminder_" + booking.getDocumentId()).hashCode(); 

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                    Log.i(TAG, "Exact alarm scheduled for booking " + booking.getDocumentId() + " at " + new java.util.Date(reminderTimeMillis).toString());
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission or user hasn't granted it for booking: " + booking.getDocumentId());
                    Toast.makeText(context, "Please grant permission to schedule exact alarms for session reminders.", Toast.LENGTH_LONG).show();
                    
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                Log.i(TAG, "Exact alarm scheduled (API M+) for booking " + booking.getDocumentId() + " at " + new java.util.Date(reminderTimeMillis).toString());
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                Log.i(TAG, "Exact alarm scheduled (pre-API M) for booking " + booking.getDocumentId() + " at " + new java.util.Date(reminderTimeMillis).toString());
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while scheduling alarm for booking " + booking.getDocumentId() + ". Missing SCHEDULE_EXACT_ALARM or related permission?", se);
            Toast.makeText(context, "Could not schedule reminder due to permission issues.", Toast.LENGTH_LONG).show();
        }
    }

    public static void cancelSessionReminder(Context context, String documentId) { 
        if (documentId == null) {
            Log.e(TAG, "Cannot cancel reminder, documentId is null.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available for cancellation.");
            return;
        }

        Intent intent = new Intent(context, SessionAlarmReceiver.class);
        int requestCode = ("reminder_" + documentId).hashCode();         

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.i(TAG, "Cancelled session reminder for booking document ID: " + documentId);
        } else {
            Log.d(TAG, "No reminder found to cancel for booking document ID: " + documentId + " (PendingIntent was null)");
        }
    }
}