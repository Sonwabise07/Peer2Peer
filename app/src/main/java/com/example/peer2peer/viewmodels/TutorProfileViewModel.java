package com.example.peer2peer.viewmodels; // Ensure this matches your package

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class TutorProfileViewModel extends ViewModel {

    // --- Step 1: Personal Details ---
    private final MutableLiveData<Uri> profileImageUri = new MutableLiveData<>();
    private final MutableLiveData<String> firstName = new MutableLiveData<>();
    private final MutableLiveData<String> surname = new MutableLiveData<>();
    private final MutableLiveData<String> gender = new MutableLiveData<>();
    private final MutableLiveData<String> race = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> studentId = new MutableLiveData<>();

    // --- Step 2: Academic Background ---
    private final MutableLiveData<String> yearOfStudy = new MutableLiveData<>();
    private final MutableLiveData<String> qualifications = new MutableLiveData<>();
    // --- CORRECTED: Changed to List<String> ---
    private final MutableLiveData<List<String>> modulesToTutor = new MutableLiveData<>(new ArrayList<>()); // Initialize with empty list

    // --- Step 3: Tutoring Details ---
    private final MutableLiveData<List<String>> tutoringLanguages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> bio = new MutableLiveData<>();
    private final MutableLiveData<String> hourlyRate = new MutableLiveData<>();

    // --- Step 4: Verification Documents ---
    private final MutableLiveData<Uri> idDocumentUri = new MutableLiveData<>();
    private final MutableLiveData<String> idDocumentFilename = new MutableLiveData<>();
    private final MutableLiveData<Uri> proofRegistrationUri = new MutableLiveData<>();
    private final MutableLiveData<String> proofRegistrationFilename = new MutableLiveData<>();
    private final MutableLiveData<Uri> academicRecordUri = new MutableLiveData<>();
    private final MutableLiveData<String> academicRecordFilename = new MutableLiveData<>();


    // --- Getters (LiveData - for observing changes) ---

    public LiveData<Uri> getProfileImageUri() { return profileImageUri; }
    public LiveData<String> getFirstName() { return firstName; }
    public LiveData<String> getSurname() { return surname; }
    public LiveData<String> getGender() { return gender; }
    public LiveData<String> getRace() { return race; }
    public LiveData<String> getEmail() { return email; }
    public LiveData<String> getStudentId() { return studentId; }
    public LiveData<String> getYearOfStudy() { return yearOfStudy; }
    public LiveData<String> getQualifications() { return qualifications; }
    // --- CORRECTED: Getter returns LiveData<List<String>> ---
    public LiveData<List<String>> getModulesToTutor() { return modulesToTutor; }
    public LiveData<List<String>> getTutoringLanguages() { return tutoringLanguages; }
    public LiveData<String> getBio() { return bio; }
    public LiveData<String> getHourlyRate() { return hourlyRate; }
    public LiveData<Uri> getIdDocumentUri() { return idDocumentUri; }
    public LiveData<String> getIdDocumentFilename() { return idDocumentFilename; }
    public LiveData<Uri> getProofRegistrationUri() { return proofRegistrationUri; }
    public LiveData<String> getProofRegistrationFilename() { return proofRegistrationFilename; }
    public LiveData<Uri> getAcademicRecordUri() { return academicRecordUri; }
    public LiveData<String> getAcademicRecordFilename() { return academicRecordFilename; }


    // --- Setters ---

    public void setProfileImageUri(Uri uri) { profileImageUri.setValue(uri); }
    public void setFirstName(String value) { firstName.setValue(value); }
    public void setSurname(String value) { surname.setValue(value); }
    public void setGender(String value) { gender.setValue(value); }
    public void setRace(String value) { race.setValue(value); }
    public void setEmail(String value) { email.setValue(value); }
    public void setStudentId(String value) { studentId.setValue(value); }
    public void setYearOfStudy(String value) { yearOfStudy.setValue(value); }
    public void setQualifications(String value) { qualifications.setValue(value); }
    // --- CORRECTED: Setter accepts List<String> ---
    public void setModulesToTutor(List<String> value) { modulesToTutor.setValue(value); }
    public void setTutoringLanguages(List<String> value) { tutoringLanguages.setValue(value); }
    public void setBio(String value) { bio.setValue(value); }
    public void setHourlyRate(String value) { hourlyRate.setValue(value); }
    public void setIdDocumentUri(Uri uri) { idDocumentUri.setValue(uri); }
    public void setIdDocumentFilename(String filename) { idDocumentFilename.setValue(filename); }
    public void setProofRegistrationUri(Uri uri) { proofRegistrationUri.setValue(uri); }
    public void setProofRegistrationFilename(String filename) { proofRegistrationFilename.setValue(filename); }
    public void setAcademicRecordUri(Uri uri) { academicRecordUri.setValue(uri); }
    public void setAcademicRecordFilename(String filename) { academicRecordFilename.setValue(filename); }

}