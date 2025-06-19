
package com.example.peer2peer;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class Resource {
    @DocumentId
    private String documentId; // Firestore document ID

    private String uploaderUid; // UID of the tutor who uploaded
    private String title;
    private String description;
    private String type; // "file" or "link"
    private String url; // Download URL for files, or the direct link for links
    private String fileName; // Original file name, if type is "file"
    private String filePath; // Path in Firebase Storage, if type is "file"
    private String mimeType; // MIME type, if type is "file"
    private String moduleCode; // The module this resource is for

    @ServerTimestamp
    private Timestamp uploadedAt;

    // No-argument constructor for Firestore
    public Resource() {}

    // Constructor (optional, but good for manual creation)
    public Resource(String uploaderUid, String title, String description, String type, String url,
                    String fileName, String filePath, String mimeType, String moduleCode) {
        this.uploaderUid = uploaderUid;
        this.title = title;
        this.description = description;
        this.type = type;
        this.url = url;
        this.fileName = fileName;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.moduleCode = moduleCode;
        // uploadedAt will be set by @ServerTimestamp or manually if needed
    }

    // --- Getters ---
    public String getDocumentId() { return documentId; }
    public String getUploaderUid() { return uploaderUid; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getUrl() { return url; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getMimeType() { return mimeType; }
    public String getModuleCode() { return moduleCode; }
    public Timestamp getUploadedAt() { return uploadedAt; }

    // --- Setters (needed by Firestore for deserialization) ---
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setUploaderUid(String uploaderUid) { this.uploaderUid = uploaderUid; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setUrl(String url) { this.url = url; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
}