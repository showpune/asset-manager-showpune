package com.microsoft.migration.assets.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3StorageItem {
    private String key;        // blob name
    private String name;       // display name
    private long size;
    private Instant lastModified;
    private Instant uploadedAt;
    private String url;
}