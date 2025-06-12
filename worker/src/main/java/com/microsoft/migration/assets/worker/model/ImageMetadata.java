package com.microsoft.migration.assets.worker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class ImageMetadata {
    @Id
    private String id;
    private String filename;
    private String contentType;
    private Long size;
    private String blobKey;
    private String blobUrl;
    private String thumbnailKey;
    private String thumbnailUrl;
    private LocalDateTime uploadedAt;
    private LocalDateTime lastModified;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastModified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }
    
    // Backward compatibility getters/setters for S3 names
    @Deprecated
    public String getS3Key() {
        return blobKey;
    }
    
    @Deprecated
    public void setS3Key(String s3Key) {
        this.blobKey = s3Key;
    }
    
    @Deprecated
    public String getS3Url() {
        return blobUrl;
    }
    
    @Deprecated
    public void setS3Url(String s3Url) {
        this.blobUrl = s3Url;
    }
}