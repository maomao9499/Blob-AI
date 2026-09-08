package dev.blob.media.service;

import dev.blob.common.error.BusinessException;
import dev.blob.media.entity.MediaAssetEntity;
import dev.blob.media.mapper.MediaAssetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalImageStorageServiceTest {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01
    };

    @TempDir
    private Path uploadDir;

    @Mock
    private MediaAssetMapper mediaAssetMapper;

    @Test
    void storesValidPngWithUuidNameInsideUploadRoot() throws Exception {
        LocalImageStorageService service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "../escape.png", "image/png", PNG_BYTES
        );

        var response = service.store(file);

        ArgumentCaptor<MediaAssetEntity> captor = ArgumentCaptor.forClass(MediaAssetEntity.class);
        verify(mediaAssetMapper).insert(captor.capture());
        Path storedPath = Path.of(captor.getValue().getStoragePath());
        assertThat(storedPath).startsWith(uploadDir.toAbsolutePath().normalize());
        assertThat(storedPath.getFileName().toString()).matches("[0-9a-f-]{36}\\.png");
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(PNG_BYTES);
        assertThat(response.markdown()).startsWith("![](/api/v1/media/images/");
    }

    @Test
    void rejectsScriptDisguisedAsPng() {
        var file = new MockMultipartFile(
                "file", "attack.png", "image/png", "<script>alert(1)</script>".getBytes()
        );

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("图片格式");
    }

    @Test
    void rejectsDeclaredAndDetectedTypeMismatch() {
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", PNG_BYTES);

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不一致");
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        var file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[10 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> service().store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    void duplicateOriginalNamesReceiveDifferentStoredNames() {
        LocalImageStorageService service = service();

        var first = service.store(new MockMultipartFile("file", "same.png", "image/png", PNG_BYTES));
        var second = service.store(new MockMultipartFile("file", "same.png", "image/png", PNG_BYTES));

        assertThat(first.url()).isNotEqualTo(second.url());
    }

    @Test
    void refusesSymlinkOutsideUploadRoot() throws Exception {
        Path outside = Files.createTempFile("blob-outside-", ".png");
        try {
            String name = "00000000-0000-4000-8000-000000000000.png";
            Files.createSymbolicLink(uploadDir.resolve(name), outside);
            assertThatThrownBy(() -> service().load(name))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("不存在");
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    private LocalImageStorageService service() {
        return new LocalImageStorageService(uploadDir.toString(), mediaAssetMapper, new ImageTypeDetector());
    }
}
