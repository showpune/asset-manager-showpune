package com.microsoft.migration.assets.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant uploadedAt;
    private Instant lastModified;

    @PrePersist
    protected void onCreate() {
        uploadedAt = Instant.now();
        lastModified = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = Instant.now();
    }
}