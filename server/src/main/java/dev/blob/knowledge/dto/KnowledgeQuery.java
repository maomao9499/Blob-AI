package dev.blob.knowledge.dto;

public record KnowledgeQuery(String keyword, Long categoryId, boolean uncategorized, Long tagId,
                             Integer page, Integer pageSize) {
    public KnowledgeQuery {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        page = page == null ? 1 : page;
        pageSize = pageSize == null ? 20 : pageSize;
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("分页参数无效");
        if (categoryId != null && uncategorized) throw new IllegalArgumentException("分类与未分类条件不能同时使用");
        if ((categoryId != null && categoryId < 1) || (tagId != null && tagId < 1))
            throw new IllegalArgumentException("分类或标签 ID 无效");
    }
    public String keywordPattern() {
        return keyword == null ? null : "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
    }
}
