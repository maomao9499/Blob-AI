package dev.blob.journal.service;

import dev.blob.common.api.PageResponse;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalDetailResponse;
import dev.blob.journal.dto.JournalQuery;
import dev.blob.journal.dto.JournalSummaryResponse;
import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.entity.JournalEntryEntity;
import dev.blob.journal.entity.JournalEntryEntity.EntryType;
import dev.blob.journal.mapper.JournalEntryMapper;
import dev.blob.tag.dto.TagResponse;
import dev.blob.tag.service.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JournalServiceImpl implements JournalService {

    private final JournalEntryMapper journalMapper;
    private final TagService tagService;
    private final JournalCacheRepository cacheRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    public JournalServiceImpl(
            JournalEntryMapper journalMapper,
            TagService tagService,
            JournalCacheRepository cacheRepository,
            SearchHistoryRepository searchHistoryRepository
    ) {
        this.journalMapper = journalMapper;
        this.tagService = tagService;
        this.cacheRepository = cacheRepository;
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Override
    @Transactional
    public long create(JournalCreateRequest request) {
        JournalEntryEntity entry = JournalEntryEntity.create(
                normalizeTitle(request.title()),
                request.contentMd(),
                EntryType.valueOf(request.entryType()),
                request.entryDate()
        );
        journalMapper.insert(entry);
        tagService.replaceJournalTags(entry.getId(), request.tagIds());
        safeCacheEvict(entry.getId());
        return entry.getId();
    }

    @Override
    public JournalDetailResponse get(long id) {
        Optional<JournalDetailResponse> cached = safeCacheGet(id);
        if (cached.isPresent()) {
            return cached.get();
        }
        JournalEntryEntity entry = requireEntry(id);
        List<TagResponse> tags = tagService.findByJournalIds(List.of(id)).getOrDefault(id, List.of());
        JournalDetailResponse detail = toDetail(entry, tags);
        safeCachePut(detail);
        return detail;
    }

    @Override
    @Transactional
    public void update(long id, JournalUpdateRequest request) {
        JournalEntryEntity entry = requireEntry(id);
        entry.setTitle(normalizeTitle(request.title()));
        entry.setContentMd(request.contentMd());
        entry.setEntryType(EntryType.valueOf(request.entryType()));
        entry.setEntryDate(request.entryDate());
        journalMapper.updateById(entry);
        tagService.replaceJournalTags(id, request.tagIds());
        safeCacheEvict(id);
    }

    @Override
    @Transactional
    public void delete(long id) {
        requireEntry(id);
        journalMapper.deleteById(id);
        safeCacheEvict(id);
    }

    @Override
    public PageResponse<JournalSummaryResponse> search(JournalQuery query) {
        long total = journalMapper.countSearch(query);
        long offset = (long) (query.page() - 1) * query.pageSize();
        List<JournalEntryEntity> entries = total == 0
                ? List.of()
                : journalMapper.search(query, offset, query.pageSize());
        Map<Long, List<TagResponse>> tagsByJournalId = tagService.findByJournalIds(
                entries.stream().map(JournalEntryEntity::getId).toList()
        );
        List<JournalSummaryResponse> items = entries.stream()
                .map(entry -> toSummary(entry, tagsByJournalId.getOrDefault(entry.getId(), List.of())))
                .toList();
        if (query.keyword() != null) {
            safeRecordSearch(query.keyword());
        }
        return new PageResponse<>(items, total, query.page(), query.pageSize());
    }

    private Optional<JournalDetailResponse> safeCacheGet(long id) {
        try {
            return cacheRepository.get(id);
        } catch (DataAccessException exception) {
            return Optional.empty();
        }
    }

    private void safeCachePut(JournalDetailResponse detail) {
        try {
            cacheRepository.put(detail);
        } catch (DataAccessException ignored) {
            // Redis is optional; the MySQL result remains authoritative.
        }
    }

    private void safeCacheEvict(long id) {
        try {
            cacheRepository.evict(id);
        } catch (DataAccessException ignored) {
            // Cache entries expire and Redis remains rebuildable.
        }
    }

    private void safeRecordSearch(String keyword) {
        try {
            searchHistoryRepository.record(keyword);
        } catch (DataAccessException ignored) {
            // Search history is auxiliary and must not fail journal search.
        }
    }

    private JournalEntryEntity requireEntry(long id) {
        JournalEntryEntity entry = journalMapper.selectById(id);
        if (entry == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND, "日志不存在");
        }
        return entry;
    }

    private String normalizeTitle(String title) {
        return title == null ? null : title.trim();
    }

    private JournalSummaryResponse toSummary(JournalEntryEntity entry, List<TagResponse> tags) {
        return new JournalSummaryResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getEntryType().getValue(),
                entry.getEntryDate(),
                entry.getAiSummary(),
                tags,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private JournalDetailResponse toDetail(JournalEntryEntity entry, List<TagResponse> tags) {
        return new JournalDetailResponse(
                entry.getId(),
                entry.getTitle(),
                entry.getContentMd(),
                entry.getEntryType().getValue(),
                entry.getEntryDate(),
                entry.getAiSummary(),
                tags,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
