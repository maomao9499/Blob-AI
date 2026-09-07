package dev.blob.media.controller;

import dev.blob.media.dto.ImageUploadResponse;
import dev.blob.media.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @Test
    void uploadsImageAndReturnsMarkdown() throws Exception {
        when(imageStorageService.store(any())).thenReturn(new ImageUploadResponse(
                7L,
                "/api/v1/media/images/123.png",
                "![](/api/v1/media/images/123.png)"
        ));
        MockMultipartFile file = new MockMultipartFile(
                "file", "note.png", "image/png", new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/media/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.markdown").value("![](/api/v1/media/images/123.png)"));
    }
}
