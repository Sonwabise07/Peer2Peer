package com.example.peer2peer;

public class ChatMessage {

    private String messageText;
    private String senderType;
    private long messageTime; 

    
    public static final String SENDER_USER = "user";
    public static final String SENDER_BOT = "bot";

   
    public ChatMessage(String messageText, String senderType) {
        this.messageText = messageText;
        this.senderType = senderType;
        this.messageTime = System.currentTimeMillis(); 

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