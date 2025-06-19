package com.example.peer2peer;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ChatRoomSummary {

    private String chatRoomId;
    private String otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantProfileImageUrl; 
    private String lastMessageText;
    private Date lastMessageTimestamp;
    

    
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

    
    public Date getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    
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

   
}