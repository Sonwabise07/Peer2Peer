package com.example.peer2peer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp; 

public class Review {
    private float rating;
    private String comment;
    private String tuteeUid;
    private String tuteeName;
    private String bookingId;
    private String tutorUid;
    private Timestamp timestamp; 

    
    public Review() {}

    
    public Review(float rating, String comment, String tuteeUid, String tuteeName, String bookingId, String tutorUid) {
        this.rating = rating;
        this.comment = comment;
        this.tuteeUid = tuteeUid;
        this.tuteeName = tuteeName;
        this.bookingId = bookingId;
        this.tutorUid = tutorUid; 
       
    }

    // Getters for all fields (Required by Firestore)
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public String getTuteeUid() { return tuteeUid; }
    public String getTuteeName() { return tuteeName; }
    public String getBookingId() { return bookingId; }
    public String getTutorUid() { return tutorUid; }
    public Timestamp getTimestamp() { return timestamp; } 

   
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    
}