package dev.blob.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.knowledge.dto.KnowledgeWriteRequest;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.mapper.KnowledgeItemTagMapper;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeTagRaceTest {
    @Autowired KnowledgeService knowledge;
    @Autowired TagService tags;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean KnowledgeItemTagMapper associations;

    @Test void foreignKeyRejectsDeletionWhenKnowledgeReferenceCommitsAfterPreflightCount() {
        long tag = tags.create(new TagCreateRequest("race-"+UUID.randomUUID(),null));
        long item = knowledge.create(new KnowledgeWriteRequest("标签竞争","正文",null,null,List.of()));
        AtomicBoolean inserted = new AtomicBoolean();
        try {
            doAnswer(invocation -> {
                // MyBatis mapper methods are abstract; perform the real preflight read
                // on the current transaction connection before committing the competing reference.
                long count = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM knowledge_item_tag WHERE tag_id=?", Long.class, tag);
                if (inserted.compareAndSet(false,true)) {
                    assertThat(count).isZero();
                    var concurrent = new TransactionTemplate(transactionManager);
                    concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    concurrent.executeWithoutResult(status -> jdbc.update(
                            "INSERT INTO knowledge_item_tag(knowledge_item_id,tag_id) VALUES (?,?)",item,tag));
                }
                return count;
            }).when(associations).selectCount(any(Wrapper.class));
            assertThatThrownBy(() -> tags.delete(tag)).isInstanceOfSatisfying(BusinessException.class,
                    error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
            assertThat(inserted).isTrue();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag WHERE id=?",Integer.class,tag)).isEqualTo(1);
            assertThat(knowledge.get(item).tags()).extracting(t -> t.id()).containsExactly(tag);
        } finally {
            reset(associations);
            knowledge.delete(item);
            tags.delete(tag);
        }
    }
}
