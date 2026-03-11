package com.microsoft.migration.assets.worker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing metadata for uploaded images.
 * Note: Field names 's3Key' and 's3Url' are retained from the original AWS S3 implementation
 * for backward compatibility with existing database schema. These fields are now used for
 * Azure Blob Storage keys and URLs. A future database migration could rename these to
 * storage-agnostic names like 'storageKey' and 'storageUrl'.
 */
@Entity
@Data
@NoArgsConstructor
public class ImageMetadata {
    @Id
    private String id;
    private String filename;
    private String contentType;
    private Long size;
    private String s3Key;
    private String s3Url;
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
}