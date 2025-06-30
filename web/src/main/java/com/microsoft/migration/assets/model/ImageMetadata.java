package com.microsoft.migration.assets.model;

import jakarta.persistence.*;
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
    private String storageKey;
    private String storageUrl;
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

    // Legacy getter/setter for backwards compatibility
    @Deprecated
    public String getS3Key() {
        return storageKey;
    }

    @Deprecated
    public void setS3Key(String s3Key) {
        this.storageKey = s3Key;
    }

    @Deprecated
    public String getS3Url() {
        return storageUrl;
    }

    @Deprecated
    public void setS3Url(String s3Url) {
        this.storageUrl = s3Url;
    }
}