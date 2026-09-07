package dev.blob.journal.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.common.api.PageResponse;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalSummaryResponse;
import dev.blob.journal.service.JournalService;
import dev.blob.journal.service.SearchHistoryRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final JournalService journalService;
    private final SearchHistoryRepository searchHistoryRepository;

    public SearchController(JournalService journalService, SearchHistoryRepository searchHistoryRepository) {
        this.journalService = journalService;
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @GetMapping
    ApiResponse<PageResponse<JournalSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Pattern(regexp = "LEARNING|LIFE") String entryType,
            @RequestParam(required = false) @Positive Long tagId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        return ApiResponse.ok(journalService.search(
                new JournalQuery(keyword, entryType, tagId, startDate, endDate, page, pageSize)
        ));
    }

    @GetMapping("/recent")
    ApiResponse<List<String>> recent(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return ApiResponse.ok(searchHistoryRepository.recent(limit));
    }
}
