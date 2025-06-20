package com.microsoft.migration.assets.repository;

import com.microsoft.migration.assets.model.ImageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, String> {
    Optional<ImageMetadata> findByBlobKey(String blobKey);
}