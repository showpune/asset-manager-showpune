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
    
    // AWS S3 fields (for backward compatibility)
    private String s3Key;
    private String s3Url;
    
    // Azure Blob Storage fields
    private String azureBlobKey;
    private String azureBlobUrl;
    
    // Thumbnail fields (storage-agnostic)
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
    
    // Helper methods to get storage key/url regardless of storage type
    public String getStorageKey() {
        return azureBlobKey != null ? azureBlobKey : s3Key;
    }
    
    public String getStorageUrl() {
        return azureBlobUrl != null ? azureBlobUrl : s3Url;
    }
}