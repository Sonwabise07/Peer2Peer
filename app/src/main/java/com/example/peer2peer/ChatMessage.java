package com.example.peer2peer;

public class ChatMessage {

    private String messageText;
    private String senderType;
    private long messageTime; // Optional: for timestamping messages

    // Constants for sender type
    public static final String SENDER_USER = "user";
    public static final String SENDER_BOT = "bot";

    // Constructor
    public ChatMessage(String messageText, String senderType) {
        this.messageText = messageText;
        this.senderType = senderType;
        this.messageTime = System.currentTimeMillis(); // Automatically set message time
    }

    // Getters
    public String getMessageText() {
        return messageText;
    }

    public String getSenderType() {
        return senderType;
    }

    public long getMessageTime() {
        return messageTime;
    }

    // Optional: Setters (if you need to modify messages after creation, though often not needed for simple display)
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }
}