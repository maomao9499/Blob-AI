package dev.blob.tag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.dto.TagUpdateRequest;
import dev.blob.tag.entity.JournalEntryTagEntity;
import dev.blob.tag.entity.TagEntity;
import dev.blob.tag.mapper.JournalEntryTagMapper;
import dev.blob.tag.mapper.TagMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final JournalEntryTagMapper journalEntryTagMapper;

    public TagServiceImpl(TagMapper tagMapper, JournalEntryTagMapper journalEntryTagMapper) {
        this.tagMapper = tagMapper;
        this.journalEntryTagMapper = journalEntryTagMapper;
    }

    @Override
    public List<TagResponse> list() {
        return tagMapper.selectList(Wrappers.<TagEntity>query().orderByAsc("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public long create(TagCreateRequest request) {
        String name = normalizeName(request.name());
        ensureUniqueName(name, null);

        TagEntity tag = new TagEntity();
        tag.setName(name);
        tag.setColor(normalizeColor(request.color()));
        try {
            tagMapper.insert(tag);
        } catch (DuplicateKeyException exception) {
            throw conflict("标签已存在");
        }
        return tag.getId();
    }

    @Override
    @Transactional
    public void update(long id, TagUpdateRequest request) {
        TagEntity tag = requireTag(id);
        String name = normalizeName(request.name());
        ensureUniqueName(name, id);
        tag.setName(name);
        tag.setColor(normalizeColor(request.color()));
        try {
            tagMapper.updateById(tag);
        } catch (DuplicateKeyException exception) {
            throw conflict("标签已存在");
        }
    }

    @Override
    @Transactional
    public void delete(long id) {
        requireTag(id);
        long associationCount = journalEntryTagMapper.selectCount(
                Wrappers.<JournalEntryTagEntity>query().eq("tag_id", id)
        );
        if (associationCount > 0) {
            throw conflict("标签仍被日志使用，无法删除");
        }
        tagMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void replaceJournalTags(long journalId, List<Long> tagIds) {
        Set<Long> distinctTagIds = tagIds == null
                ? Set.of()
                : tagIds.stream()
                        .filter(id -> id != null && id > 0)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!distinctTagIds.isEmpty()) {
            long existingCount = tagMapper.selectCount(
                    Wrappers.<TagEntity>query().in("id", distinctTagIds)
            );
            if (existingCount != distinctTagIds.size()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "存在无效标签");
            }
        }

        journalEntryTagMapper.delete(
                Wrappers.<JournalEntryTagEntity>query()
                        .eq("journal_entry_id", journalId)
        );
        if (!distinctTagIds.isEmpty()) {
            List<JournalEntryTagEntity> associations = distinctTagIds.stream()
                    .map(tagId -> new JournalEntryTagEntity(journalId, tagId))
                    .toList();
            journalEntryTagMapper.insert(associations);
        }
    }

    @Override
    public Map<Long, List<TagResponse>> findByJournalIds(Collection<Long> journalIds) {
        if (journalIds == null || journalIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> distinctJournalIds = new LinkedHashSet<>(journalIds);
        List<JournalEntryTagEntity> associations = journalEntryTagMapper.selectList(
                Wrappers.<JournalEntryTagEntity>query()
                        .in("journal_entry_id", distinctJournalIds)
        );
        if (associations.isEmpty()) {
            return distinctJournalIds.stream().collect(Collectors.toMap(
                    Function.identity(),
                    ignored -> List.of(),
                    (left, right) -> left,
                    LinkedHashMap::new
            ));
        }

        Set<Long> tagIds = associations.stream()
                .map(JournalEntryTagEntity::getTagId)
                .collect(Collectors.toSet());
        Map<Long, TagResponse> tagsById = tagMapper.selectBatchIds(tagIds).stream()
                .map(this::toResponse)
                .collect(Collectors.toMap(TagResponse::id, Function.identity()));

        Map<Long, List<TagResponse>> result = new LinkedHashMap<>();
        distinctJournalIds.forEach(id -> result.put(id, new ArrayList<>()));
        associations.forEach(association -> {
            TagResponse tag = tagsById.get(association.getTagId());
            if (tag != null) {
                result.get(association.getJournalEntryId()).add(tag);
            }
        });
        result.replaceAll((ignored, tags) -> tags.stream()
                .sorted(Comparator.comparing(TagResponse::name))
                .toList());
        return result;
    }

    private void ensureUniqueName(String name, Long excludedId) {
        var query = Wrappers.<TagEntity>query().eq("name", name);
        if (excludedId != null) {
            query.ne("id", excludedId);
        }
        if (tagMapper.selectCount(query) > 0) {
            throw conflict("标签已存在");
        }
    }

    private TagEntity requireTag(long id) {
        TagEntity tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "标签不存在");
        }
        return tag;
    }

    private String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 50) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "标签名称长度必须在 1 到 50 个字符之间"
            );
        }
        return normalized;
    }

    private String normalizeColor(String color) {
        if (color == null) {
            return null;
        }
        String normalized = color.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private TagResponse toResponse(TagEntity tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }
}
