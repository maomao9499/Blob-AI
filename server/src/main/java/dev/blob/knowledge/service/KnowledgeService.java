package dev.blob.knowledge.service;
import dev.blob.common.api.PageResponse;
import dev.blob.knowledge.dto.*;
import java.util.List;
public interface KnowledgeService {
    long create(KnowledgeWriteRequest request);
    long promote(long journalId, KnowledgeWriteRequest request);
    KnowledgeDetailResponse get(long id);
    void update(long id, KnowledgeWriteRequest request);
    void delete(long id);
    PageResponse<KnowledgeSummaryResponse> search(KnowledgeQuery query);
    List<CategoryResponse> categories();
    long createCategory(CategoryWriteRequest request);
    void updateCategory(long id, CategoryWriteRequest request);
    void deleteCategory(long id);
    long createRelation(RelationCreateRequest request);
    PageResponse<KnowledgeRelationResponse> relations(long id, int page, int pageSize);
    void deleteRelation(long id);
}
