package com.example.peer2peer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

public class User {
    @DocumentId
    private String uid; // Will be auto-populated by Firestore with document ID

    private String email;
    private String role; // "Tutor" or "Tutee"
    private boolean isBlocked = false;
    private boolean profileComplete = false; // Default for new users
    private String fcmToken;
    @ServerTimestamp
    private Timestamp createdAt;
    private String profileStatus; // e.g., "pending_verification", "verified", "rejected" (mainly for tutors)
    private String name; // Common field for display name

    public User() {
        // Firestore no-arg constructor
    }

    // Getters
    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    @PropertyName("isBlocked") // Matches Firestore field if it's "isBlocked"
    public boolean getIsBlocked() { return isBlocked; } // Changed to getIsBlocked for consistency
    @PropertyName("profileComplete") // Matches Firestore field
    public boolean isProfileComplete() { return profileComplete; }
    public String getFcmToken() { return fcmToken; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getProfileStatus() { return profileStatus; }
    public String getName() { return name; }


    // Setters
    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
    @PropertyName("isBlocked")
    public void setIsBlocked(boolean blocked) { isBlocked = blocked; } // Changed to setIsBlocked
    @PropertyName("profileComplete")
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setProfileStatus(String profileStatus) { this.profileStatus = profileStatus; }
    public void setName(String name) { this.name = name; }
}