package dev.blob.media.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.media.dto.ImageUploadResponse;
import dev.blob.media.service.ImageStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/media/images")
public class MediaController {

    private final ImageStorageService imageStorageService;

    public MediaController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    ApiResponse<ImageUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(imageStorageService.store(file));
    }
}
