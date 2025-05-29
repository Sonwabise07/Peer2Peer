package com.example.peer2peer;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class DirectMessage {

    private String messageId;       // To store the Firestore document ID
    private String senderId;        // UID of the user who sent the message
    private String receiverId;      // UID of the user who should receive the message
    private String senderName;      // Display name of the sender
    private String messageText;     // The actual content of the message
    private Date timestamp;         // Timestamp of when the message was sent

    // Required empty public constructor for Firestore deserialization
    public DirectMessage() {
    }

    // Constructor for creating a new message before sending to Firestore
    public DirectMessage(String senderId, String receiverId, String senderName, String messageText) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.messageText = messageText;
        // Timestamp will be set by Firestore using @ServerTimestamp
    }

    // Getters
    // For messageId, Firestore can automatically populate this if you retrieve the document ID.
    // Alternatively, you can use @DocumentId annotation on a String field if you prefer.
    // For simplicity, we'll set it manually if needed after fetching.
    public String getMessageId() {
        return messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessageText() {
        return messageText;
    }

    @ServerTimestamp // Annotation to tell Firestore to populate this with the server's timestamp
    public Date getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // Helper to check who sent the message, useful for UI differentiation
    @Exclude // Exclude this from being written to Firestore as it's a helper method
    public boolean isSentBy(String currentUserId) {
        return senderId != null && senderId.equals(currentUserId);
    }
}