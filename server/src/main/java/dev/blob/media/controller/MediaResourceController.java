package dev.blob.media.controller;

import dev.blob.media.service.ImageStorageService;
import dev.blob.media.service.ImageStorageService.StoredImage;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/images")
public class MediaResourceController {

    private final ImageStorageService imageStorageService;

    public MediaResourceController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/{storedName}")
    ResponseEntity<?> load(@PathVariable String storedName) {
        StoredImage image = imageStorageService.load(storedName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(image.storedName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
