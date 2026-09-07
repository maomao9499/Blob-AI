package dev.blob.journal.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.common.api.PageResponse;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalDetailResponse;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalSummaryResponse;
import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.service.JournalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping
    ApiResponse<PageResponse<JournalSummaryResponse>> list(
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

    @PostMapping
    ApiResponse<Map<String, Long>> create(@Valid @RequestBody JournalCreateRequest request) {
        return ApiResponse.ok(Map.of("id", journalService.create(request)));
    }

    @GetMapping("/{id}")
    ApiResponse<JournalDetailResponse> get(@PathVariable @Positive long id) {
        return ApiResponse.ok(journalService.get(id));
    }

    @PutMapping("/{id}")
    ApiResponse<Void> update(
            @PathVariable @Positive long id,
            @Valid @RequestBody JournalUpdateRequest request
    ) {
        journalService.update(id, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable @Positive long id) {
        journalService.delete(id);
        return ApiResponse.ok(null);
    }
}
