package com.example.peer2peer;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class DirectMessage {

    private String messageId;       
    private String senderId;        
    private String receiverId;     
    private String senderName;      
    private String messageText;     
    private Date timestamp;   

 
    public DirectMessage() {
    }


    public DirectMessage(String senderId, String receiverId, String senderName, String messageText) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.messageText = messageText;
        
    }

   
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

    @ServerTimestamp 
    public Date getTimestamp() {
        return timestamp;
    }

    
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

    
    @Exclude 
    public boolean isSentBy(String currentUserId) {
        return senderId != null && senderId.equals(currentUserId);
    }
}