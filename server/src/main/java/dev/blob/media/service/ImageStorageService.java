package dev.blob.media.service;

import dev.blob.media.dto.ImageUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    ImageUploadResponse store(MultipartFile file);

    StoredImage load(String storedName);

    record StoredImage(Resource resource, String mimeType, String storedName) {
    }
}
