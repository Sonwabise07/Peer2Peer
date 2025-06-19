
package com.example.peer2peer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date; 
import java.text.SimpleDateFormat; 
import java.util.Locale; 

public class Booking {

    @DocumentId
    private String documentId; 

   
    private String tutorUid;
    private String tuteeUid;
    private String availabilitySlotId;
    private Timestamp startTime;
    private Timestamp endTime;
    private String bookingStatus;
    private String moduleCode;
    @ServerTimestamp
    private Timestamp createdAt;
    private String tutorName;
    private String tuteeName;
    private String meetingLink;
    private Double rateCharged;
    private String paymentIntentId;
    private String currency;

   
    public boolean isRated = false; 

    
    public Booking() {}

   
    public Booking(String documentId, String tutorUid, String tuteeUid, String availabilitySlotId,
                   Timestamp startTime, Timestamp endTime, String bookingStatus, String moduleCode,
                   Timestamp createdAt, String tutorName, String tuteeName, String meetingLink,
                   boolean isRated, 
                   Double rateCharged, String paymentIntentId, String currency) {
        this.documentId = documentId;
        this.tutorUid = tutorUid;
        this.tuteeUid = tuteeUid;
        this.availabilitySlotId = availabilitySlotId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bookingStatus = bookingStatus;
        this.moduleCode = moduleCode;
        this.createdAt = createdAt;
        this.tutorName = tutorName;
        this.tuteeName = tuteeName;
        this.meetingLink = meetingLink;
        this.isRated = isRated; 
        this.rateCharged = rateCharged;
        this.paymentIntentId = paymentIntentId;
        this.currency = currency;
    }

    
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTutorUid() { return tutorUid; }
    public void setTutorUid(String tutorUid) { this.tutorUid = tutorUid; }

    public String getTuteeUid() { return tuteeUid; }
    public void setTuteeUid(String tuteeUid) { this.tuteeUid = tuteeUid; }

    public String getAvailabilitySlotId() { return availabilitySlotId; }
    public void setAvailabilitySlotId(String availabilitySlotId) { this.availabilitySlotId = availabilitySlotId; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }

    public String getTuteeName() { return tuteeName; }
    public void setTuteeName(String tuteeName) { this.tuteeName = tuteeName; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public Double getRateCharged() { return rateCharged; }
    public void setRateCharged(Double rateCharged) { this.rateCharged = rateCharged; }

    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    // --- toString() METHOD FOR DEBUGGING ---
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String startTimeStr = (startTime != null) ? sdf.format(startTime.toDate()) : "null";
        String endTimeStr = (endTime != null) ? sdf.format(endTime.toDate()) : "null";
        String createdAtStr = (createdAt != null) ? sdf.format(createdAt.toDate()) : "null";

        return "Booking{" +
                "documentId='" + documentId + '\'' +
                ", tuteeUid='" + tuteeUid + '\'' +
                ", tutorUid='" + tutorUid + '\'' +
                ", tutorName='" + tutorName + '\'' +
                ", moduleCode='" + moduleCode + '\'' +
                ", startTime=" + startTimeStr +
                ", endTime=" + endTimeStr +
                ", bookingStatus='" + bookingStatus + '\'' +
                ", isRated=" + isRated + 
                ", meetingLink='" + meetingLink + '\'' +
                ", rateCharged=" + rateCharged +
                ", paymentIntentId='" + paymentIntentId + '\'' +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAtStr +
                '}';
    }
}