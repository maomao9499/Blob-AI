package dev.blob.media.service;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class ImageTypeDetector {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    public DetectedImage detect(byte[] bytes) {
        if (startsWith(bytes, PNG)) {
            return new DetectedImage("image/png", ".png");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return new DetectedImage("image/jpeg", ".jpg");
        }
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            return new DetectedImage("image/gif", ".gif");
        }
        if (bytes.length >= 12
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return new DetectedImage("image/webp", ".webp");
        }
        throw new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                HttpStatus.BAD_REQUEST,
                "无法识别图片格式，仅支持 JPEG、PNG、GIF 和 WebP"
        );
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        return bytes.length >= signature.length
                && Arrays.equals(Arrays.copyOf(bytes, signature.length), signature);
    }

    public record DetectedImage(String mimeType, String extension) {
    }
}
