package dev.blob.search.controller;

import dev.blob.journal.controller.SearchController;
import dev.blob.journal.service.JournalService;
import dev.blob.journal.service.SearchHistoryRepository;
import dev.blob.common.api.PageResponse;
import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchResponse;
import dev.blob.search.service.UnifiedSearchService;
import dev.blob.tag.dto.TagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SearchController.class, UnifiedSearchController.class})
class UnifiedSearchControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JournalService journalService;
    @MockitoBean
    private SearchHistoryRepository history;
    @MockitoBean
    private UnifiedSearchService service;

    @Test
    void unifiedSearchIsAvailableAtItsOwnRoute() throws Exception {
        when(service.search(new UnifiedSearchQuery(null, "ALL", null, 1, 20)))
                .thenReturn(new PageResponse<>(List.of(new UnifiedSearchResponse("KNOWLEDGE", 7L,
                        "中文标题", "正文片段", List.of(new TagResponse(1L, "Java", null)),
                        LocalDateTime.of(2026, 9, 8, 12, 0))), 1, 1, 20));
        mockMvc.perform(get("/api/v1/search/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.items[0].sourceType").value("KNOWLEDGE"))
                .andExpect(jsonPath("$.data.items[0].sourceId").value(7))
                .andExpect(jsonPath("$.data.items[0].tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.items[0].contentMd").doesNotExist());
    }

    @Test
    void acceptsCombinedFilters() throws Exception {
        when(service.search(new UnifiedSearchQuery("中文", "JOURNAL", 3L, 2, 10)))
                .thenReturn(new PageResponse<>(List.of(), 11, 2, 10));
        mockMvc.perform(get("/api/v1/search/all").param("keyword", " 中文 ")
                        .param("sourceType", "JOURNAL").param("tagId", "3")
                        .param("page", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(11))
                .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void rejectsInvalidSearchParameters() throws Exception {
        for (String[] parameter : List.of(new String[]{"sourceType", "OTHER"}, new String[]{"page", "0"},
                new String[]{"pageSize", "101"}, new String[]{"tagId", "-1"}, new String[]{"tagId", "bad"})) {
            mockMvc.perform(get("/api/v1/search/all").param(parameter[0], parameter[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void keepsExistingJournalSearchAndHistoryRoutes() throws Exception {
        when(journalService.search(new dev.blob.journal.dto.JournalQuery(null, null, null, null, null, 1, 20)))
                .thenReturn(new PageResponse<>(List.of(), 0, 1, 20));
        when(history.recent(10)).thenReturn(List.of("Java"));
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/api/v1/search/recent"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0]").value("Java"));
    }
}
