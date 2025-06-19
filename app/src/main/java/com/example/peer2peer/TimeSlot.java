
package com.example.peer2peer; 

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TimeSlot {
    @Exclude private String documentId;

    private Timestamp startTime;
    private Timestamp endTime;
    private String status;
    private String moduleCode; 

    public TimeSlot() {}

   
    public TimeSlot(Timestamp startTime, Timestamp endTime, String status, String moduleCode) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.moduleCode = moduleCode;
    }

    
    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getModuleCode() { 
        return moduleCode;
    }

    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setModuleCode(String moduleCode) { // <-- NEW SETTER for moduleCode
        this.moduleCode = moduleCode;
    }

    // --- Document ID ---
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    @Exclude
    public String getFormattedTimeRange() {
        if (startTime == null || endTime == null) {
            return "Invalid Time";
        }
        @SuppressWarnings("deprecation")
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        timeFormat.setTimeZone(TimeZone.getDefault());

        String start = timeFormat.format(startTime.toDate());
        String end = timeFormat.format(endTime.toDate());
        return start + " - " + end;
    }

    
    @Exclude
    public String getFormattedTimeRangeWithModule() {
        String timeRange = getFormattedTimeRange();
        if (moduleCode != null && !moduleCode.isEmpty()) {
            return moduleCode + ": " + timeRange;
        }
        return timeRange;
    }
}