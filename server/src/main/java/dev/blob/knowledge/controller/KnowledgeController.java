package dev.blob.knowledge.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.common.api.PageResponse;
import dev.blob.knowledge.dto.*;
import dev.blob.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {
    private final KnowledgeService service;
    public KnowledgeController(KnowledgeService service) { this.service = service; }
    @GetMapping
    ApiResponse<PageResponse<KnowledgeSummaryResponse>> list(
            @RequestParam(required=false) String keyword,
            @RequestParam(required=false) @Positive Long categoryId,
            @RequestParam(defaultValue="false") boolean uncategorized,
            @RequestParam(required=false) @Positive Long tagId,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(service.search(new KnowledgeQuery(keyword,categoryId,uncategorized,tagId,page,pageSize)));
    }
    @PostMapping
    ApiResponse<Map<String,Long>> create(@Valid @RequestBody KnowledgeWriteRequest request) {
        return ApiResponse.ok(Map.of("id",service.create(request)));
    }
    @GetMapping("/{id}")
    ApiResponse<KnowledgeDetailResponse> get(@PathVariable @Positive long id) { return ApiResponse.ok(service.get(id)); }
    @PutMapping("/{id}")
    ApiResponse<Void> update(@PathVariable @Positive long id,@Valid @RequestBody KnowledgeWriteRequest request) {
        service.update(id,request); return ApiResponse.ok(null);
    }
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable @Positive long id) { service.delete(id); return ApiResponse.ok(null); }
    @GetMapping("/categories")
    ApiResponse<List<CategoryResponse>> categories() { return ApiResponse.ok(service.categories()); }
    @PostMapping("/categories")
    ApiResponse<Map<String,Long>> createCategory(@Valid @RequestBody CategoryWriteRequest request) {
        return ApiResponse.ok(Map.of("id",service.createCategory(request)));
    }
    @PutMapping("/categories/{id}")
    ApiResponse<Void> updateCategory(@PathVariable @Positive long id,@Valid @RequestBody CategoryWriteRequest request) {
        service.updateCategory(id,request); return ApiResponse.ok(null);
    }
    @DeleteMapping("/categories/{id}")
    ApiResponse<Void> deleteCategory(@PathVariable @Positive long id) { service.deleteCategory(id); return ApiResponse.ok(null); }
    @GetMapping("/{id}/relations")
    ApiResponse<PageResponse<KnowledgeRelationResponse>> relations(@PathVariable @Positive long id,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.ok(service.relations(id,page,pageSize));
    }
    @PostMapping("/relations")
    ApiResponse<Map<String,Long>> createRelation(@Valid @RequestBody RelationCreateRequest request) {
        return ApiResponse.ok(Map.of("id",service.createRelation(request)));
    }
    @DeleteMapping("/relations/{id}")
    ApiResponse<Void> deleteRelation(@PathVariable @Positive long id) { service.deleteRelation(id); return ApiResponse.ok(null); }
}
