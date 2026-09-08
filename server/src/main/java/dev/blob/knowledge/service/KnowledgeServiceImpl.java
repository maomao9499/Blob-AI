package dev.blob.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.blob.common.api.PageResponse;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.service.JournalService;
import dev.blob.knowledge.dto.*;
import dev.blob.knowledge.entity.*;
import dev.blob.knowledge.mapper.*;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.service.TagService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private final KnowledgeItemMapper items;
    private final KnowledgeCategoryMapper categories;
    private final KnowledgeRelationMapper relations;
    private final TagService tags;
    private final JournalService journals;

    public KnowledgeServiceImpl(KnowledgeItemMapper items, KnowledgeCategoryMapper categories,
            KnowledgeRelationMapper relations, TagService tags, JournalService journals) {
        this.items = items;
        this.categories = categories;
        this.relations = relations;
        this.tags = tags;
        this.journals = journals;
    }

    @Override @Transactional
    public long create(KnowledgeWriteRequest request) { return insert(request, null, null); }

    @Override @Transactional
    public long promote(long journalId, KnowledgeWriteRequest request) {
        // The source must be read from MySQL under this transaction, never from the detail cache.
        var source = journals.getAuthoritative(journalId);
        return insert(request, source.id(), source.title());
    }

    private long insert(KnowledgeWriteRequest request, Long sourceId, String sourceTitle) {
        var item = new KnowledgeItemEntity();
        item.setTitle(required(request.title(), 200, "标题"));
        item.setContentMd(content(request.contentMd()));
        item.setSummary(optional(request.summary(), 1000, "摘要"));
        validateCategory(request.categoryId());
        item.setCategoryId(request.categoryId());
        item.setSourceJournalId(sourceId);
        item.setSourceJournalTitle(sourceTitle);
        try {
            items.insert(item);
            tags.replaceKnowledgeTags(item.getId(), request.tagIds());
        } catch (DataIntegrityViolationException exception) {
            throw missing("分类、标签或来源已不存在，请刷新后重试");
        }
        return item.getId();
    }

    @Override
    public KnowledgeDetailResponse get(long id) {
        var item = requireItem(id);
        var category = item.getCategoryId() == null ? null : categorySummary(requireCategory(item.getCategoryId()));
        return new KnowledgeDetailResponse(item.getId(),item.getTitle(),item.getContentMd(),item.getSummary(),
                category,tags.findByKnowledgeIds(List.of(id)).getOrDefault(id,List.of()),
                item.getSourceJournalId(),item.getSourceJournalTitle(),item.getCreatedAt(),item.getUpdatedAt());
    }

    @Override @Transactional
    public void update(long id, KnowledgeWriteRequest request) {
        requireItem(id);
        String title = required(request.title(),200,"标题");
        String content = content(request.contentMd());
        String summary = optional(request.summary(),1000,"摘要");
        validateCategory(request.categoryId());
        try {
            // Update only editable fields; a concurrent source deletion must never be undone.
            int changed = items.update(null, Wrappers.<KnowledgeItemEntity>update().eq("id",id)
                    .set("title",title).set("content_md",content).set("summary",summary)
                    .set("category_id",request.categoryId()).setSql("updated_at = CURRENT_TIMESTAMP(3)"));
            if (changed == 0) throw missing("知识不存在");
            tags.replaceKnowledgeTags(id,request.tagIds());
        } catch (DataIntegrityViolationException exception) {
            throw missing("分类或标签已不存在，请刷新后重试");
        }
    }

    @Override @Transactional
    public void delete(long id) {
        requireItem(id);
        items.deleteById(id);
    }

    @Override
    public PageResponse<KnowledgeSummaryResponse> search(KnowledgeQuery query) {
        long total = items.countSearch(query);
        var entries = total == 0 ? List.<KnowledgeItemEntity>of()
                : items.search(query,(long)(query.page()-1)*query.pageSize(),query.pageSize());
        var tagsById = tags.findByKnowledgeIds(entries.stream().map(KnowledgeItemEntity::getId).toList());
        Set<Long> categoryIds = entries.stream().map(KnowledgeItemEntity::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, CategorySummaryResponse> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categories.selectBatchIds(categoryIds).stream().map(this::categorySummary)
                .collect(Collectors.toMap(CategorySummaryResponse::id,Function.identity()));
        var result = entries.stream().map(item -> new KnowledgeSummaryResponse(item.getId(),item.getTitle(),
                item.getSummary(),excerpt(item.getContentMd()),item.getCategoryId() == null ? null : categoryMap.get(item.getCategoryId()),
                tagsById.getOrDefault(item.getId(),List.of()),item.getCreatedAt(),item.getUpdatedAt())).toList();
        return new PageResponse<>(result,total,query.page(),query.pageSize());
    }

    @Override
    public List<CategoryResponse> categories() {
        return categories.selectList(Wrappers.<KnowledgeCategoryEntity>query().orderByAsc("name","id"))
                .stream().map(c -> new CategoryResponse(c.getId(),c.getName(),c.getDescription(),
                        c.getCreatedAt(),c.getUpdatedAt())).toList();
    }

    @Override @Transactional
    public long createCategory(CategoryWriteRequest request) {
        var category = new KnowledgeCategoryEntity();
        category.setName(required(request.name(),50,"分类名称"));
        category.setDescription(optional(request.description(),500,"分类描述"));
        ensureUniqueCategory(category.getName(),null);
        try { categories.insert(category); }
        catch (DuplicateKeyException exception) { throw conflict("分类已存在"); }
        return category.getId();
    }

    @Override @Transactional
    public void updateCategory(long id, CategoryWriteRequest request) {
        var category = requireCategory(id);
        category.setName(required(request.name(),50,"分类名称"));
        category.setDescription(optional(request.description(),500,"分类描述"));
        ensureUniqueCategory(category.getName(),id);
        try {
            categories.update(null, Wrappers.<KnowledgeCategoryEntity>update().eq("id",id)
                    .set("name",category.getName()).set("description",category.getDescription())
                    .setSql("updated_at = CURRENT_TIMESTAMP(3)"));
        }
        catch (DuplicateKeyException exception) { throw conflict("分类已存在"); }
    }

    @Override @Transactional
    public void deleteCategory(long id) {
        requireCategory(id);
        if (items.selectCount(Wrappers.<KnowledgeItemEntity>query().eq("category_id",id)) > 0)
            throw conflict("分类仍被知识使用，无法删除");
        try { categories.deleteById(id); }
        catch (DataIntegrityViolationException exception) { throw conflict("分类仍被知识使用，无法删除"); }
    }

    @Override @Transactional
    public long createRelation(RelationCreateRequest request) {
        if (request.sourceKnowledgeId() == null || request.targetKnowledgeId() == null
                || request.sourceKnowledgeId() < 1 || request.targetKnowledgeId() < 1
                || request.sourceKnowledgeId().equals(request.targetKnowledgeId())
                || !"RELATED".equals(request.relationType())) throw invalid("关联必须为两个不同知识之间的 RELATED 关系");
        long source = Math.min(request.sourceKnowledgeId(),request.targetKnowledgeId());
        long target = Math.max(request.sourceKnowledgeId(),request.targetKnowledgeId());
        requireItem(source);
        requireItem(target);
        var relation = new KnowledgeRelationEntity();
        relation.setSourceKnowledgeId(source);
        relation.setTargetKnowledgeId(target);
        relation.setRelationType("RELATED");
        try { relations.insert(relation); }
        catch (DuplicateKeyException exception) { throw conflict("知识关联已存在"); }
        catch (DataIntegrityViolationException exception) { throw missing("关联知识已不存在"); }
        return relation.getId();
    }

    @Override
    public PageResponse<KnowledgeRelationResponse> relations(long id, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw invalid("分页参数无效");
        requireItem(id);
        long total = relations.selectCount(Wrappers.<KnowledgeRelationEntity>query()
                .eq("source_knowledge_id",id).or().eq("target_knowledge_id",id));
        return new PageResponse<>(total == 0 ? List.of() : relations.findRelations(id,(long)(page-1)*pageSize,pageSize),
                total,page,pageSize);
    }

    @Override @Transactional
    public void deleteRelation(long id) {
        if (relations.selectById(id) == null) throw missing("知识关联不存在");
        relations.deleteById(id);
    }

    private KnowledgeItemEntity requireItem(long id) {
        var item = items.selectById(id);
        if (item == null) throw missing("知识不存在");
        return item;
    }
    private KnowledgeCategoryEntity requireCategory(long id) {
        var category = categories.selectById(id);
        if (category == null) throw missing("分类不存在");
        return category;
    }
    private void validateCategory(Long id) { if (id != null) requireCategory(id); }
    private void ensureUniqueCategory(String name, Long excluded) {
        var query = Wrappers.<KnowledgeCategoryEntity>query().eq("name",name);
        if (excluded != null) query.ne("id",excluded);
        if (categories.selectCount(query) > 0) throw conflict("分类已存在");
    }
    private CategorySummaryResponse categorySummary(KnowledgeCategoryEntity c) {
        return new CategorySummaryResponse(c.getId(),c.getName());
    }
    private String required(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.isEmpty() || result.length() > max) throw invalid(label+"长度必须在 1 到 "+max+" 个字符之间");
        return result;
    }
    private String optional(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        if (value.length() > max) throw invalid(label+"不能超过 "+max+" 个字符");
        return value.trim();
    }
    private String content(String value) {
        if (value == null || value.isBlank()) throw invalid("正文不能为空");
        return value;
    }
    private String excerpt(String value) {
        String normalized = value.replaceAll("\\s+"," ").trim();
        return normalized.codePointCount(0,normalized.length()) <= 200 ? normalized
                : normalized.substring(0,normalized.offsetByCodePoints(0,200));
    }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.VALIDATION_ERROR,HttpStatus.BAD_REQUEST,message); }
    private BusinessException missing(String message) { return new BusinessException(ErrorCode.NOT_FOUND,HttpStatus.NOT_FOUND,message); }
    private BusinessException conflict(String message) { return new BusinessException(ErrorCode.CONFLICT,HttpStatus.CONFLICT,message); }
}
