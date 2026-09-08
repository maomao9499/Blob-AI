package dev.blob.knowledge.controller;
import dev.blob.common.api.ApiResponse;
import dev.blob.knowledge.dto.KnowledgeWriteRequest;
import dev.blob.knowledge.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Validated
@RestController
public class KnowledgePromotionController {
    private final KnowledgeService service;
    public KnowledgePromotionController(KnowledgeService service) { this.service = service; }
    @PostMapping("/api/v1/journals/{id}/promote-to-knowledge")
    ApiResponse<Map<String,Long>> promote(@PathVariable @Positive long id,
            @Valid @RequestBody KnowledgeWriteRequest request) {
        return ApiResponse.ok(Map.of("id",service.promote(id,request)));
    }
}
