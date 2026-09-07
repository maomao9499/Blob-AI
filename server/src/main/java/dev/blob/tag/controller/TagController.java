package dev.blob.tag.controller;

import dev.blob.common.api.ApiResponse;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.dto.TagUpdateRequest;
import dev.blob.tag.service.TagService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    ApiResponse<List<TagResponse>> list() {
        return ApiResponse.ok(tagService.list());
    }

    @PostMapping
    ApiResponse<Map<String, Long>> create(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.ok(Map.of("id", tagService.create(request)));
    }

    @PutMapping("/{id}")
    ApiResponse<Void> update(
            @PathVariable @Positive long id,
            @Valid @RequestBody TagUpdateRequest request
    ) {
        tagService.update(id, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable @Positive long id) {
        tagService.delete(id);
        return ApiResponse.ok(null);
    }
}
