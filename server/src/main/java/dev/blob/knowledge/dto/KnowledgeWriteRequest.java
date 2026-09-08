package dev.blob.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record KnowledgeWriteRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String contentMd,
        @Size(max = 1000) String summary,
        @Positive Long categoryId,
        List<@NotNull @Positive Long> tagIds
) {
    public KnowledgeWriteRequest {
        title = title == null ? null : title.trim();
        summary = summary == null || summary.isBlank() ? null : summary.trim();
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("知识输入不支持字段: " + name);
    }
}
