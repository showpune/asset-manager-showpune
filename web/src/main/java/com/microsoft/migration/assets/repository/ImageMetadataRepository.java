package com.microsoft.migration.assets.repository;

import com.microsoft.migration.assets.model.ImageMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, String> {
    // Basic CRUD operations are automatically provided by JpaRepository
    
    /**
     * Find metadata by S3 key (blob name)
     */
    Optional<ImageMetadata> findByS3Key(String s3Key);
    
    /**
     * Delete metadata by S3 key (blob name)
     */
    void deleteByS3Key(String s3Key);
}