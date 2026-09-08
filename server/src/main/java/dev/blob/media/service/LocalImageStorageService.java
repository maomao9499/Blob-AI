package dev.blob.media.service;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.media.dto.ImageUploadResponse;
import dev.blob.media.entity.MediaAssetEntity;
import dev.blob.media.mapper.MediaAssetMapper;
import dev.blob.media.service.ImageTypeDetector.DetectedImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Pattern SAFE_STORED_NAME = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\.(png|jpg|gif|webp)"
    );

    private final Path uploadRoot;
    private final MediaAssetMapper mediaAssetMapper;
    private final ImageTypeDetector imageTypeDetector;

    public LocalImageStorageService(
            @Value("${blob.upload-dir}") String uploadDir,
            MediaAssetMapper mediaAssetMapper,
            ImageTypeDetector imageTypeDetector
    ) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.mediaAssetMapper = mediaAssetMapper;
        this.imageTypeDetector = imageTypeDetector;
    }

    @Override
    public ImageUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw validation("请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw validation("图片不能超过 10 MB");
        }

        byte[] bytes = readBytes(file);
        DetectedImage detected = imageTypeDetector.detect(bytes);
        validateDeclaredType(file.getContentType(), detected.mimeType());

        String storedName = UUID.randomUUID() + detected.extension();
        Path target = safeResolve(storedName);
        Path temporary = safeResolve(storedName + ".tmp");
        writeAtomically(bytes, temporary, target);

        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setOriginalName(safeOriginalName(file.getOriginalFilename()));
        asset.setStoredName(storedName);
        asset.setStoragePath(target.toString());
        asset.setMimeType(detected.mimeType());
        asset.setFileSize(file.getSize());
        asset.setSha256(sha256(bytes));
        try {
            mediaAssetMapper.insert(asset);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }

        String url = "/api/v1/media/images/" + storedName;
        long assetId = asset.getId() == null ? 0L : asset.getId();
        return new ImageUploadResponse(assetId, url, "![](" + url + ")");
    }

    @Override
    public StoredImage load(String storedName) {
        if (storedName == null || !SAFE_STORED_NAME.matcher(storedName).matches()) {
            throw notFound();
        }
        Path path = safeResolve(storedName);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw notFound();
        }
        String extension = storedName.substring(storedName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mimeType = switch (extension) {
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> throw notFound();
        };
        return new StoredImage(new FileSystemResource(path), mimeType, storedName);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "读取图片失败"
            );
        }
    }

    private void validateDeclaredType(String declaredType, String detectedType) {
        String normalized = "image/jpg".equalsIgnoreCase(declaredType) ? "image/jpeg" : declaredType;
        if (normalized == null
                || !SUPPORTED_MIME_TYPES.contains(normalized.toLowerCase(Locale.ROOT))
                || !detectedType.equalsIgnoreCase(normalized)) {
            throw validation("声明的图片类型与实际内容不一致");
        }
    }

    private Path safeResolve(String filename) {
        Path path = uploadRoot.resolve(filename).normalize();
        if (!path.startsWith(uploadRoot)) {
            throw validation("非法图片路径");
        }
        return path;
    }

    private void writeAtomically(byte[] bytes, Path temporary, Path target) {
        try {
            Files.createDirectories(uploadRoot);
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } catch (IOException exception) {
            deleteQuietly(temporary);
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "保存图片失败"
            );
        }
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        Path fileName = Path.of(originalName.replace('\\', '/')).getFileName();
        String safeName = fileName == null ? "image" : fileName.toString();
        return safeName.length() <= 255 ? safeName : safeName.substring(safeName.length() - 255);
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A failed cleanup is non-fatal and the UUID name avoids collisions.
        }
    }

    private BusinessException validation(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, message);
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "图片不存在");
    }
}
