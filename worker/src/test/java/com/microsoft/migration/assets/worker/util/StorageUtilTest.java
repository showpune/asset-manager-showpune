package com.microsoft.migration.assets.worker.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageUtilTest {

    @Test
    void testGetThumbnailKeyWithExtension() {
        String result = StorageUtil.getThumbnailKey("image.jpg");
        assertEquals("image_thumbnail.jpg", result);
    }

    @Test
    void testGetThumbnailKeyWithMultipleDots() {
        String result = StorageUtil.getThumbnailKey("my.image.png");
        assertEquals("my.image_thumbnail.png", result);
    }

    @Test
    void testGetThumbnailKeyWithoutExtension() {
        String result = StorageUtil.getThumbnailKey("image");
        assertEquals("image_thumbnail", result);
    }

    @Test
    void testGetExtensionWithExtension() {
        String result = StorageUtil.getExtension("image.jpg");
        assertEquals(".jpg", result);
    }

    @Test
    void testGetExtensionWithMultipleDots() {
        String result = StorageUtil.getExtension("my.image.png");
        assertEquals(".png", result);
    }

    @Test
    void testGetExtensionWithoutExtension() {
        String result = StorageUtil.getExtension("image");
        assertEquals("", result);
    }
}
