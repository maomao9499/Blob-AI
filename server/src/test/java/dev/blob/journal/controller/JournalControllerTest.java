package dev.blob.journal.controller;

import dev.blob.common.api.PageResponse;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalDetailResponse;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalSummaryResponse;
import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.service.JournalService;
import dev.blob.journal.service.SearchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest({JournalController.class, SearchController.class})
class JournalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JournalService journalService;

    @MockitoBean
    private SearchHistoryRepository searchHistoryRepository;

    @Test
    void createReturnsIdEnvelope() throws Exception {
        when(journalService.create(any(JournalCreateRequest.class))).thenReturn(42L);

        mockMvc.perform(post("/api/v1/journals")
                        .contentType("application/json")
                        .content("""
                                {"title":"Redis","contentMd":"# Cache","entryType":"LEARNING",
                                 "entryDate":"2026-09-07","tagIds":[1]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(42));
    }

    @Test
    void rejectsInvalidTypeAndEmptyContent() throws Exception {
        mockMvc.perform(post("/api/v1/journals")
                        .contentType("application/json")
                        .content("""
                                {"title":"Redis","contentMd":"","entryType":"OTHER",
                                 "entryDate":"2026-09-07","tagIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void listsJournalsWithPaginationAndGlobalSearchUsesSameContract() throws Exception {
        JournalSummaryResponse summary = new JournalSummaryResponse(
                2L, "Redis", "LEARNING", LocalDate.of(2026, 9, 7), null, List.of(),
                LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 0)
        );
        when(journalService.search(any(JournalQuery.class)))
                .thenReturn(new PageResponse<>(List.of(summary), 1, 1, 20));

        mockMvc.perform(get("/api/v1/journals").param("keyword", "缓存"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Redis"))
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/v1/search").param("keyword", "缓存"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(2));
    }

    @Test
    void getsUpdatesAndDeletesJournal() throws Exception {
        when(journalService.get(2L)).thenReturn(new JournalDetailResponse(
                2L, "Redis", "# Cache", "LEARNING", LocalDate.of(2026, 9, 7), null, List.of(),
                LocalDateTime.of(2026, 9, 7, 9, 0), LocalDateTime.of(2026, 9, 7, 9, 0)
        ));
        doNothing().when(journalService).delete(2L);

        mockMvc.perform(get("/api/v1/journals/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMd").value("# Cache"));

        mockMvc.perform(put("/api/v1/journals/2")
                        .contentType("application/json")
                        .content("""
                                {"title":"Redis 进阶","contentMd":"正文","entryType":"LEARNING",
                                 "entryDate":"2026-09-07","tagIds":[]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/journals/2"))
                .andExpect(status().isOk());
    }

    @Test
    void mapsMissingJournalToNotFound() throws Exception {
        when(journalService.get(99L)).thenThrow(
                new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "日志不存在")
        );

        mockMvc.perform(get("/api/v1/journals/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsInvalidListTypeAndPage() throws Exception {
        mockMvc.perform(get("/api/v1/journals")
                        .param("entryType", "OTHER")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
