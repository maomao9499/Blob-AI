package dev.blob.knowledge.dto;

import jakarta.validation.constraints.*;

public record RelationCreateRequest(@NotNull @Positive Long sourceKnowledgeId, @NotNull @Positive Long targetKnowledgeId,
        @NotNull @Pattern(regexp = "RELATED") String relationType) {}
