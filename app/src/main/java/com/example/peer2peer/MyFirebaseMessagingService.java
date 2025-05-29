package com.example.peer2peer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random; // For unique notification ID and request code

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    // Existing Channel for Bookings
    private static final String BOOKING_CHANNEL_ID = "booking_notifications";
    private static final String BOOKING_CHANNEL_NAME = "Booking Notifications";
    private static final String BOOKING_CHANNEL_DESCRIPTION = "Notifications for new bookings and session updates.";

    // New Channel for Chat Messages
    private static final String CHAT_MESSAGE_CHANNEL_ID = "chat_messages_channel";
    private static final String CHAT_MESSAGE_CHANNEL_NAME = "Chat Messages";
    private static final String CHAT_MESSAGE_CHANNEL_DESCRIPTION = "Notifications for new chat messages.";


    /**
     * Called when a message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Message data payload: " + remoteMessage.getData());

        String title = null;
        String body = null;

        // Title and body should primarily come from the 'notification' payload part of the FCM message
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Notification payload: Title: " + title + ", Body: " + body);
        }

        Map<String, String> data = remoteMessage.getData();

        // If title/body were not in notification payload (e.g. data-only message, or for compatibility)
        // try to get them from data payload. Our Cloud Functions for chat and booking WILL send a notification payload.
        if (title == null && data.containsKey("title")) {
            title = data.get("title");
        }
        if (body == null && data.containsKey("body")) {
            body = data.get("body");
        }

        // Determine notification type from data payload
        String messageType = data.get("type"); // For new chat messages ("CHAT_MESSAGE")
        String bookingNotificationType = data.get("notificationType"); // For existing booking notifications (e.g., "NEW_BOOKING_TUTOR")

        if ("CHAT_MESSAGE".equals(messageType)) {
            String chatRoomId = data.get("chatRoomId");
            String senderId = data.get("senderId"); // This is the chatPartnerId for the recipient
            String senderName = data.get("senderName");

            if (title != null && body != null && chatRoomId != null && senderId != null && senderName != null) {
                Log.d(TAG, "Handling CHAT_MESSAGE notification.");
                sendChatNotification(title, body, chatRoomId, senderId, senderName);
            } else {
                Log.w(TAG, "Chat message notification is incomplete. Data: " + data + ", Title: " + title + ", Body: " + body);
            }
        } else if ("NEW_BOOKING_TUTOR".equals(bookingNotificationType) /* Add other booking types if any: || "OTHER_BOOKING_TYPE".equals(bookingNotificationType) */) {
            // Handle your existing booking notifications
            // String bookingId = data.get("bookingId"); // Example
            if (title != null && body != null) {
                Log.d(TAG, "Handling " + bookingNotificationType + " notification.");
                // Using the existing channel for booking. The sendGenericNotification method needs to be aware of this.
                sendGenericNotification(title, body, BOOKING_CHANNEL_ID, BOOKING_CHANNEL_NAME, BOOKING_CHANNEL_DESCRIPTION, null); // Pass null for intent if default is fine
            } else {
                Log.w(TAG, bookingNotificationType + " notification title or body is missing. Data: " + data);
            }
        } else {
            // Generic or unknown notification
            Log.d(TAG, "Received a generic or unknown FCM message type. Data: " + data);
            if (title != null && body != null) {
                // Use a truly generic channel or the booking one as a default for unknown types
                sendGenericNotification(title, body, BOOKING_CHANNEL_ID, "Generic Notifications", "General app notifications", null);
            } else {
                Log.w(TAG, "Generic notification title or body is missing. Data: " + data);
            }
        }
    }

    /**
     * Create and show a specific notification for new chat messages.
     *
     * @param title         Message title.
     * @param body          Message body.
     * @param chatRoomId    ID of the chat room.
     * @param chatPartnerId ID of the user who sent the message.
     * @param chatPartnerName Name of the user who sent the message.
     */
    private void sendChatNotification(String title, String body, String chatRoomId, String chatPartnerId, String chatPartnerName) {
        Intent intent = new Intent(this, ChatActivity.class); // Make sure ChatActivity exists
        intent.putExtra("CHAT_ROOM_ID", chatRoomId);
        intent.putExtra("CHAT_PARTNER_ID", chatPartnerId);
        intent.putExtra("CHAT_PARTNER_NAME", chatPartnerName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Opens or brings to front existing ChatActivity for this chat

        // Use a unique request code for each PendingIntent to ensure they are distinct
        // Or if you want to group notifications for the same chat, use chatRoomId.hashCode()
        int requestCode = new Random().nextInt();

        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // FLAG_UPDATE_CURRENT will update extras if ChatActivity is already open with this pendingIntent

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHAT_MESSAGE_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_notification) // IMPORTANT: Replace with your chat icon if you have one
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // For heads-up notification

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since Android Oreo (API 26) notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHAT_MESSAGE_CHANNEL_ID,
                    CHAT_MESSAGE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHAT_MESSAGE_CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        // Using a unique ID for each notification allows multiple chat message notifications to be shown.
        // You could use chatRoomId.hashCode() if you want new messages in the same chat to update the existing notification.
        int notificationId = new Random().nextInt();
        notificationManager.notify(notificationId, notificationBuilder.build());
        Log.d(TAG, "Chat Notification Sent: " + title);
    }


    /**
     * Create and show a simple generic notification.
     * This can be used for booking notifications or other general messages.
     *
     * @param messageTitle    FCM message title received.
     * @param messageBody     FCM message body received.
     * @param channelId       The channel ID to use for this notification.
     * @param channelName     The user-visible channel name.
     * @param channelDesc     The user-visible channel description.
     * @param customIntent    An optional custom intent to launch. If null, defaults to MainActivity.
     */
    private void sendGenericNotification(String messageTitle, String messageBody, String channelId, String channelName, String channelDesc, Intent customIntent) {
        Intent intent;
        if (customIntent != null) {
            intent = customIntent;
        } else {
            intent = new Intent(this, MainActivity.class); // Default activity
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Using a random request code to ensure pending intents are mostly unique
        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_stat_notification) // IMPORTANT: Ensure this small icon exists
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH);
            if (channelDesc != null) {
                channel.setDescription(channelDesc);
            }
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
        Log.d(TAG, "Generic Notification Sent on channel " + channelId + ": " + messageTitle);
    }


    /**
     * Called if InstanceID token is updated.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        sendRegistrationToServer(token);
    }

    /**
     * Persist token to Firestore.
     */
    private void sendRegistrationToServer(String token) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> tokenUpdate = new HashMap<>();
            tokenUpdate.put("fcmToken", token);
            tokenUpdate.put("fcmTokenLastUpdated", FieldValue.serverTimestamp());

            db.collection("users").document(userId)
                    .set(tokenUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token merged successfully for user: " + userId + ". Token: " + token))
                    .addOnFailureListener(e -> Log.w(TAG, "Error merging FCM token for user: " + userId, e));
        } else {
            Log.w(TAG, "User not logged in. Cannot update FCM token in Firestore during onNewToken.");
        }
    }
}