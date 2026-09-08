package dev.blob.search.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.common.api.PageResponse;
import dev.blob.search.dto.UnifiedSearchQuery;
import dev.blob.search.dto.UnifiedSearchResponse;
import dev.blob.search.service.UnifiedSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/search/all")
public class UnifiedSearchController {
    private final UnifiedSearchService service;

    public UnifiedSearchController(UnifiedSearchService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PageResponse<UnifiedSearchResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "ALL") @Pattern(regexp = "ALL|JOURNAL|KNOWLEDGE") String sourceType,
            @RequestParam(required = false) @Positive Long tagId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(service.search(new UnifiedSearchQuery(keyword, sourceType, tagId, page, pageSize)));
    }
}
