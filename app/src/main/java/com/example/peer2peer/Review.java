package com.example.peer2peer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp; // Use this if you prefer server-side timestamp

public class Review {
    private float rating;
    private String comment;
    private String tuteeUid;
    private String tuteeName;
    private String bookingId;
    private String tutorUid; // <-- Added field
    private Timestamp timestamp; // <-- Added field for client-set timestamp

    // No-argument constructor needed for Firestore
    public Review() {}

    // Constructor matching the call in SubmitReviewActivity
    public Review(float rating, String comment, String tuteeUid, String tuteeName, String bookingId, String tutorUid) {
        this.rating = rating;
        this.comment = comment;
        this.tuteeUid = tuteeUid;
        this.tuteeName = tuteeName;
        this.bookingId = bookingId;
        this.tutorUid = tutorUid; // <-- Assign tutorUid
        // Timestamp is now set separately before saving
    }

    // Getters for all fields (Required by Firestore)
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public String getTuteeUid() { return tuteeUid; }
    public String getTuteeName() { return tuteeName; }
    public String getBookingId() { return bookingId; }
    public String getTutorUid() { return tutorUid; } // <-- Added getter
    public Timestamp getTimestamp() { return timestamp; } // <-- Added getter

    // Setter for timestamp (Required by SubmitReviewActivity)
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    // Optional: Setters for other fields if needed elsewhere, but not strictly required for Firestore deserialization if only using getters.
}