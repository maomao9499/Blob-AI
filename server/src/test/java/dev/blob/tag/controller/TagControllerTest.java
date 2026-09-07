package dev.blob.tag.controller;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TagService tagService;

    @Test
    void listsTags() throws Exception {
        when(tagService.list()).thenReturn(List.of(new TagResponse(1L, "Java", "#409eff")));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    void rejectsBlankTagName() throws Exception {
        mockMvc.perform(post("/api/v1/tags")
                        .contentType("application/json")
                        .content("{\"name\":\"   \",\"color\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createsTag() throws Exception {
        when(tagService.create(any(TagCreateRequest.class))).thenReturn(7L);

        mockMvc.perform(post("/api/v1/tags")
                        .contentType("application/json")
                        .content("{\"name\":\"Java\",\"color\":\"#409eff\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void mapsDuplicateTagToConflict() throws Exception {
        when(tagService.create(any(TagCreateRequest.class))).thenThrow(
                new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "标签已存在")
        );

        mockMvc.perform(post("/api/v1/tags")
                        .contentType("application/json")
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void updatesAndDeletesTag() throws Exception {
        doNothing().when(tagService).delete(7L);

        mockMvc.perform(put("/api/v1/tags/7")
                        .contentType("application/json")
                        .content("{\"name\":\"Java 21\",\"color\":null}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/tags/7"))
                .andExpect(status().isOk());
    }
}
