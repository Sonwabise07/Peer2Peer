package com.example.peer2peer;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatRoomSummary {

    private String chatRoomId;
    private String otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantProfileImageUrl; // Optional: for displaying profile picture
    private String lastMessageText;
    private Date lastMessageTimestamp;
    // private int unreadCount; // Optional: for unread message badges

    // Required empty public constructor for Firestore or other deserialization if needed,
    // though this class is primarily for client-side use by the adapter.
    public ChatRoomSummary() {
    }

    // Constructor
    public ChatRoomSummary(String chatRoomId, String otherParticipantId, String otherParticipantName,
                           String otherParticipantProfileImageUrl, String lastMessageText, Date lastMessageTimestamp) {
        this.chatRoomId = chatRoomId;
        this.otherParticipantId = otherParticipantId;
        this.otherParticipantName = otherParticipantName;
        this.otherParticipantProfileImageUrl = otherParticipantProfileImageUrl;
        this.lastMessageText = lastMessageText;
        this.lastMessageTimestamp = lastMessageTimestamp;
        // this.unreadCount = 0; // Initialize if using
    }

    // Getters
    public String getChatRoomId() {
        return chatRoomId;
    }

    public String getOtherParticipantId() {
        return otherParticipantId;
    }

    public String getOtherParticipantName() {
        return otherParticipantName;
    }

    public String getOtherParticipantProfileImageUrl() {
        return otherParticipantProfileImageUrl;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    // Firestore might populate this with @ServerTimestamp if this model were directly saved,
    // but here it's more likely to be populated from a Date object read from Firestore.
    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    /*
    public int getUnreadCount() {
        return unreadCount;
    }
    */

    // Setters (can be useful)
    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void setOtherParticipantId(String otherParticipantId) {
        this.otherParticipantId = otherParticipantId;
    }

    public void setOtherParticipantName(String otherParticipantName) {
        this.otherParticipantName = otherParticipantName;
    }

    public void setOtherParticipantProfileImageUrl(String otherParticipantProfileImageUrl) {
        this.otherParticipantProfileImageUrl = otherParticipantProfileImageUrl;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public void setLastMessageTimestamp(Date lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    /*
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    */
}