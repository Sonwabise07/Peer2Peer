package com.example.peer2peer;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName; // Make sure this import is present
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;
// import java.util.ArrayList; // Only if you explicitly use new ArrayList<>() in constructors not shown

public class Tutor implements Parcelable {

    @DocumentId
    private String uid;

    private String userId;
    private String firstName;
    private String surname;
    private String fullName;
    private String gender;
    private String race;
    private String email;
    private String studentId;
    private String yearOfStudy;
    private String qualifications;
    private List<String> modulesToTutor;
    private List<String> tutoringLanguages;
    private String bio;
    private Double hourlyRate;
    private String profileImageUrl;

    private String idDocumentUrl;
    private String proofRegistrationUrl;
    private String academicRecordUrl;

    private String profileStatus;
    private boolean profileComplete;

    @ServerTimestamp
    private Date submissionTimestamp;
    @ServerTimestamp
    private Date accountCreatedTimestamp;

    private String idDocumentFilename;
    private String proofRegistrationFilename;
    private String academicRecordFilename;

    private Double averageRating;
    private Long ratingCount;

    private String rejectionReason;
    private String role;
    private String lowercaseEmail;
    private boolean isBlocked; // Field is present

    private String fcmToken;
    @ServerTimestamp
    private Timestamp fcmTokenLastUpdated;


    public Tutor() {
        // Firestore needs a public no-argument constructor
    }

    // --- Getters ---
    public String getUid() { return uid; }
    public String getUserId() { return userId; }
    public String getFirstName() { return firstName; }
    public String getSurname() { return surname; }
    public String getFullName() {
        if (fullName != null && !fullName.isEmpty()) {
            return fullName;
        }
        String first = firstName != null ? firstName : "";
        String last = surname != null ? surname : "";
        return (first + " " + last).trim();
    }
    public String getGender() { return gender; }
    public String getRace() { return race; }
    public String getEmail() { return email; }
    public String getStudentId() { return studentId; }
    public String getYearOfStudy() { return yearOfStudy; }
    public String getQualifications() { return qualifications; }
    public List<String> getModulesToTutor() { return modulesToTutor; }
    public List<String> getTutoringLanguages() { return tutoringLanguages; }
    public String getBio() { return bio; }
    public Double getHourlyRate() { return hourlyRate; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getIdDocumentUrl() { return idDocumentUrl; }
    public String getProofRegistrationUrl() { return proofRegistrationUrl; }
    public String getAcademicRecordUrl() { return academicRecordUrl; }
    public String getProfileStatus() { return profileStatus; }
    public boolean isProfileComplete() { return profileComplete; }
    public Date getSubmissionTimestamp() { return submissionTimestamp; }
    public Date getAccountCreatedTimestamp() { return accountCreatedTimestamp; }
    public String getIdDocumentFilename() { return idDocumentFilename; }
    public String getProofRegistrationFilename() { return proofRegistrationFilename; }
    public String getAcademicRecordFilename() { return academicRecordFilename; }
    public Double getAverageRating() { return averageRating; }
    public Long getRatingCount() { return ratingCount; }
    public String getRejectionReason() { return rejectionReason; }
    public String getRole() { return role; }
    public String getLowercaseEmail() { return lowercaseEmail; }

    @PropertyName("isBlocked") // Annotation for Firestore mapping
    public boolean isBlocked() { // Getter for isBlocked
        return isBlocked;
    }

    public String getFcmToken() { return fcmToken; }
    public Timestamp getFcmTokenLastUpdated() { return fcmTokenLastUpdated; }


    // --- Setters ---
    public void setUid(String uid) { this.uid = uid; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setSurname(String surname) { this.surname = surname; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setGender(String gender) { this.gender = gender; }
    public void setRace(String race) { this.race = race; }
    public void setEmail(String email) { this.email = email; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setYearOfStudy(String yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public void setQualifications(String qualifications) { this.qualifications = qualifications; }
    public void setModulesToTutor(List<String> modulesToTutor) { this.modulesToTutor = modulesToTutor; }
    public void setTutoringLanguages(List<String> tutoringLanguages) { this.tutoringLanguages = tutoringLanguages; }
    public void setBio(String bio) { this.bio = bio; }
    public void setHourlyRate(Double hourlyRate) { this.hourlyRate = hourlyRate; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setIdDocumentUrl(String idDocumentUrl) { this.idDocumentUrl = idDocumentUrl; }
    public void setProofRegistrationUrl(String proofRegistrationUrl) { this.proofRegistrationUrl = proofRegistrationUrl; }
    public void setAcademicRecordUrl(String academicRecordUrl) { this.academicRecordUrl = academicRecordUrl; }
    public void setProfileStatus(String profileStatus) { this.profileStatus = profileStatus; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
    public void setSubmissionTimestamp(Date submissionTimestamp) { this.submissionTimestamp = submissionTimestamp; }
    public void setAccountCreatedTimestamp(Date accountCreatedTimestamp) { this.accountCreatedTimestamp = accountCreatedTimestamp; }
    public void setIdDocumentFilename(String idDocumentFilename) { this.idDocumentFilename = idDocumentFilename; }
    public void setProofRegistrationFilename(String proofRegistrationFilename) { this.proofRegistrationFilename = proofRegistrationFilename; }
    public void setAcademicRecordFilename(String academicRecordFilename) { this.academicRecordFilename = academicRecordFilename; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public void setRatingCount(Long ratingCount) { this.ratingCount = ratingCount; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setRole(String role) { this.role = role; }
    public void setLowercaseEmail(String lowercaseEmail) { this.lowercaseEmail = lowercaseEmail; }

    @PropertyName("isBlocked") // Annotation for Firestore mapping
    public void setBlocked(boolean blocked) { // Setter for isBlocked
        isBlocked = blocked;
    }

    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setFcmTokenLastUpdated(Timestamp fcmTokenLastUpdated) { this.fcmTokenLastUpdated = fcmTokenLastUpdated; }


    // --- Parcelable Implementation ---
    protected Tutor(Parcel in) {
        uid = in.readString();
        userId = in.readString();
        firstName = in.readString();
        surname = in.readString();
        fullName = in.readString();
        gender = in.readString();
        race = in.readString();
        email = in.readString();
        studentId = in.readString();
        yearOfStudy = in.readString();
        qualifications = in.readString();
        modulesToTutor = in.createStringArrayList();
        tutoringLanguages = in.createStringArrayList();
        bio = in.readString();
        if (in.readByte() == 0) {
            hourlyRate = null;
        } else {
            hourlyRate = in.readDouble();
        }
        profileImageUrl = in.readString();
        idDocumentUrl = in.readString();
        proofRegistrationUrl = in.readString();
        academicRecordUrl = in.readString();
        profileStatus = in.readString();
        profileComplete = in.readByte() != 0;
        long tmpSubTimestamp = in.readLong();
        submissionTimestamp = tmpSubTimestamp == -1 ? null : new Date(tmpSubTimestamp);
        long tmpAccTimestamp = in.readLong();
        accountCreatedTimestamp = tmpAccTimestamp == -1 ? null : new Date(tmpAccTimestamp);
        idDocumentFilename = in.readString();
        proofRegistrationFilename = in.readString();
        academicRecordFilename = in.readString();
        if (in.readByte() == 0) {
            averageRating = null;
        } else {
            averageRating = in.readDouble();
        }
        if (in.readByte() == 0) {
            ratingCount = null;
        } else {
            ratingCount = in.readLong();
        }
        rejectionReason = in.readString();
        role = in.readString();
        lowercaseEmail = in.readString();
        isBlocked = in.readByte() != 0; // Read isBlocked
        fcmToken = in.readString();
        long tmpFcmTokenLastUpdated = in.readLong();
        fcmTokenLastUpdated = tmpFcmTokenLastUpdated == -1 ? null : new Timestamp(new Date(tmpFcmTokenLastUpdated));
    }

    public static final Creator<Tutor> CREATOR = new Creator<Tutor>() {
        @Override
        public Tutor createFromParcel(Parcel in) {
            return new Tutor(in);
        }

        @Override
        public Tutor[] newArray(int size) {
            return new Tutor[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(userId);
        dest.writeString(firstName);
        dest.writeString(surname);
        dest.writeString(fullName);
        dest.writeString(gender);
        dest.writeString(race);
        dest.writeString(email);
        dest.writeString(studentId);
        dest.writeString(yearOfStudy);
        dest.writeString(qualifications);
        dest.writeStringList(modulesToTutor);
        dest.writeStringList(tutoringLanguages);
        dest.writeString(bio);
        if (hourlyRate == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(hourlyRate);
        }
        dest.writeString(profileImageUrl);
        dest.writeString(idDocumentUrl);
        dest.writeString(proofRegistrationUrl);
        dest.writeString(academicRecordUrl);
        dest.writeString(profileStatus);
        dest.writeByte((byte) (profileComplete ? 1 : 0));
        dest.writeLong(submissionTimestamp != null ? submissionTimestamp.getTime() : -1);
        dest.writeLong(accountCreatedTimestamp != null ? accountCreatedTimestamp.getTime() : -1);
        dest.writeString(idDocumentFilename);
        dest.writeString(proofRegistrationFilename);
        dest.writeString(academicRecordFilename);
        if (averageRating == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(averageRating);
        }
        if (ratingCount == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(ratingCount);
        }
        dest.writeString(rejectionReason);
        dest.writeString(role);
        dest.writeString(lowercaseEmail);
        dest.writeByte((byte) (isBlocked ? 1 : 0)); // Write isBlocked
        dest.writeString(fcmToken);
        dest.writeLong(fcmTokenLastUpdated != null ? fcmTokenLastUpdated.toDate().getTime() : -1);
    }
}