package dev.blob.tag.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagUpdateRequest;
import dev.blob.journal.service.JournalCacheRepository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import dev.blob.tag.entity.JournalEntryTagEntity;
import dev.blob.tag.entity.TagEntity;
import dev.blob.tag.mapper.JournalEntryTagMapper;
import dev.blob.tag.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    private TagMapper tagMapper;

    @Mock
    private JournalEntryTagMapper journalEntryTagMapper;

    @Mock
    private JournalCacheRepository cacheRepository;

    @Mock
    private dev.blob.tag.mapper.KnowledgeItemTagMapper knowledgeItemTagMapper;

    @InjectMocks
    private TagServiceImpl service;

    @Test
    void duplicateTagNameIgnoringOuterSpacesIsRejected() {
        when(tagMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.create(new TagCreateRequest(" Java ", null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessageContaining("标签已存在");
                });

        verify(tagMapper, never()).insert(any(TagEntity.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void journalTagReplacementDeduplicatesIdsInOriginalOrder() {
        when(tagMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        service.replaceJournalTags(10L, List.of(3L, 3L, 8L));

        ArgumentCaptor<Collection<JournalEntryTagEntity>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(journalEntryTagMapper).insert(captor.capture());
        assertThat(captor.getValue())
                .extracting(JournalEntryTagEntity::getTagId)
                .containsExactly(3L, 8L);
    }

    @Test
    void unknownTagRejectsReplacementBeforeDeletingExistingAssociations() {
        when(tagMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.replaceJournalTags(10L, List.of(3L, 8L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

        verify(journalEntryTagMapper, never()).delete(any(Wrapper.class));
    }

    @Test
    void renamingTagEvictsAssociatedJournalDetailsAfterCommit() {
        TagEntity tag = new TagEntity();
        tag.setId(3L);
        tag.setName("old");
        when(tagMapper.selectById(3L)).thenReturn(tag);
        when(journalEntryTagMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(new JournalEntryTagEntity(42L, 3L)));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.update(3L, new TagUpdateRequest("new", "#ff0000"));
            verify(cacheRepository, never()).evict(42L);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(cacheRepository).evict(42L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
