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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    private static final long REMINDER_OFFSET_MINUTES = 10; // 10 minutes before
    private static final String REMINDER_PREFIX = "reminder_";
    private static final String DEFAULT_SESSION_TITLE = "Session Reminder";
    private static final String DEFAULT_MODULE_TEXT = "your lesson";
    private static final String DEFAULT_TUTOR_TEXT = "your tutor/tutee";
    
    // Date formatter for consistent log formatting
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * Schedules a session reminder for the given booking
     * @param context Application context
     * @param booking The booking to schedule a reminder for
     */
    public static void scheduleSessionReminder(Context context, Booking booking) {
        
        if (!isValidBookingData(booking)) {
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

       
        Intent intent = createReminderIntent(context, booking);
        int requestCode = generateRequestCode(booking.getDocumentId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        scheduleAlarmByApiLevel(alarmManager, reminderTimeMillis, pendingIntent, booking.getDocumentId(), context);
    }

    /**
     * Cancels a scheduled session reminder
     * @param context Application context
     * @param documentId The document ID of the booking to cancel reminder for
     */
    public static void cancelSessionReminder(Context context, String documentId) { 
        if (documentId == null || documentId.trim().isEmpty()) {
            Log.e(TAG, "Cannot cancel reminder, documentId is null or empty.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available for cancellation.");
            return;
        }

        Intent intent = new Intent(context, SessionAlarmReceiver.class);
        int requestCode = generateRequestCode(documentId);         

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

    /**
     * Validates if booking data is sufficient for scheduling
     * @param booking The booking to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidBookingData(Booking booking) {
        return booking != null && 
               booking.getStartTime() != null && 
               booking.getDocumentId() != null && 
               !booking.getDocumentId().trim().isEmpty();
    }

    /**
     * Creates an intent for the session reminder with all necessary extras
     * @param context Application context
     * @param booking The booking to create intent for
     * @return Configured intent
     */
    private static Intent createReminderIntent(Context context, Booking booking) {
        Intent intent = new Intent(context, SessionAlarmReceiver.class);
        String sessionTitle = DEFAULT_SESSION_TITLE;
        String sessionDescription = buildSessionDescription(booking);

        intent.putExtra(SessionAlarmReceiver.EXTRA_BOOKING_ID, booking.getDocumentId()); 
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_TITLE, sessionTitle);
        intent.putExtra(SessionAlarmReceiver.EXTRA_SESSION_DESCRIPTION, sessionDescription);
        
        int notificationId = booking.getDocumentId().hashCode();
        intent.putExtra(SessionAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId);

        return intent;
    }

    /**
     * Builds a descriptive session description from booking data
     * @param booking The booking to build description for
     * @return Formatted session description
     */
    private static String buildSessionDescription(Booking booking) {
        StringBuilder description = new StringBuilder("Your session for ");
        
        description.append(booking.getModuleCode() != null ? booking.getModuleCode() : DEFAULT_MODULE_TEXT);
        description.append(" with ");
        description.append(booking.getTutorName() != null ? booking.getTutorName() : DEFAULT_TUTOR_TEXT);
        description.append(" is starting in ").append(REMINDER_OFFSET_MINUTES).append(" minutes.");
        
        return description.toString();
    }

    /**
     * Generates a unique request code for the given document ID
     * @param documentId The document ID to generate code for
     * @return Unique request code
     */
    private static int generateRequestCode(String documentId) {
        return (REMINDER_PREFIX + documentId).hashCode();
    }

    /**
     * Schedules alarm based on API level with appropriate method
     * @param alarmManager The alarm manager instance
     * @param reminderTimeMillis Time to schedule alarm for
     * @param pendingIntent The pending intent to execute
     * @param documentId Document ID for logging
     * @param context Application context for toasts
     */
    private static void scheduleAlarmByApiLevel(AlarmManager alarmManager, long reminderTimeMillis, 
                                              PendingIntent pendingIntent, String documentId, Context context) {
        try {
            String scheduledTime = LOG_DATE_FORMAT.format(new Date(reminderTimeMillis));
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                    Log.i(TAG, "Exact alarm scheduled for booking " + documentId + " at " + scheduledTime);
                } else {
                    Log.w(TAG, "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission for booking: " + documentId);
                    Toast.makeText(context, "Please grant permission to schedule exact alarms for session reminders.", Toast.LENGTH_LONG).show();
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                Log.i(TAG, "Exact alarm scheduled (API M+) for booking " + documentId + " at " + scheduledTime);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                Log.i(TAG, "Exact alarm scheduled (pre-API M) for booking " + documentId + " at " + scheduledTime);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while scheduling alarm for booking " + documentId + ". Missing SCHEDULE_EXACT_ALARM or related permission?", se);
            Toast.makeText(context, "Could not schedule reminder due to permission issues.", Toast.LENGTH_LONG).show();
        }
    }
}