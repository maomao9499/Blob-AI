package dev.blob.knowledge.service;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.dto.JournalCreateRequest;
import dev.blob.journal.dto.JournalUpdateRequest;
import dev.blob.journal.service.JournalService;
import dev.blob.knowledge.dto.*;
import dev.blob.tag.dto.TagCreateRequest;
import dev.blob.tag.dto.TagUpdateRequest;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeServiceIntegrationTest {
    @Autowired KnowledgeService service;
    @Autowired TagService tags;
    @Autowired JournalService journals;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    KnowledgeWriteRequest request(String title, Long category, List<Long> tagIds) {
        return new KnowledgeWriteRequest(title, "# 原文\n![image](/api/v1/media/images/example.png)", "  ", category, tagIds);
    }
    String unique() { return "M2-" + UUID.randomUUID(); }
    void rollback(Runnable action) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            try { action.run(); } finally { status.setRollbackOnly(); }
        });
    }
    @Test void crudClassificationSharedTagsAndBidirectionalRelations() {
        rollback(() -> {
            long category = service.createCategory(new CategoryWriteRequest(unique(), "desc"));
            long tag = tags.create(new TagCreateRequest(unique(), "#111111"));
            long a = service.create(request("  知识甲  ", category, List.of(tag, tag)));
            long b = service.create(request("知识乙", null, List.of()));
            assertThat(service.get(a).title()).isEqualTo("知识甲");
            assertThat(service.get(a).summary()).isNull();
            assertThat(service.get(a).tags()).hasSize(1);
            assertThat(service.get(a).category().id()).isEqualTo(category);
            assertThat(service.search(new KnowledgeQuery("知识",category,false,tag,1,20)).items())
                    .extracting(KnowledgeSummaryResponse::id).containsExactly(a);
            assertThat(service.search(new KnowledgeQuery(null,null,true,null,1,100)).items())
                    .extracting(KnowledgeSummaryResponse::id).contains(b).doesNotContain(a);
            expectCode(() -> service.deleteCategory(category), ErrorCode.CONFLICT);
            expectCode(() -> tags.delete(tag), ErrorCode.CONFLICT);
            tags.update(tag, new TagUpdateRequest(unique(), "#222222"));
            assertThat(service.get(a).tags().getFirst().color()).isEqualTo("#222222");
            long relation = service.createRelation(new RelationCreateRequest(b,a,"RELATED"));
            assertThat(service.relations(a,1,20).items()).containsExactly(new KnowledgeRelationResponse(relation,b,"知识乙"));
            assertThat(service.relations(b,1,20).items()).containsExactly(new KnowledgeRelationResponse(relation,a,"知识甲"));
            expectCode(() -> service.createRelation(new RelationCreateRequest(a,b,"RELATED")), ErrorCode.CONFLICT);
            expectCode(() -> service.createRelation(new RelationCreateRequest(a,a,"RELATED")), ErrorCode.VALIDATION_ERROR);
            expectCode(() -> service.createRelation(new RelationCreateRequest(a,Long.MAX_VALUE,"RELATED")), ErrorCode.NOT_FOUND);
            service.deleteRelation(relation);
            assertThat(service.relations(a,1,20).total()).isZero();
            service.createRelation(new RelationCreateRequest(a,b,"RELATED"));
            service.update(a,request("已更新",null,List.of()));
            assertThat(service.get(a).category()).isNull();
            assertThat(service.get(a).tags()).isEmpty();
            service.deleteCategory(category);
            tags.delete(tag);
            service.delete(a);
            assertThat(service.relations(b,1,20).total()).isZero();
            assertThat(service.get(b).title()).isEqualTo("知识乙");
        });
    }
    @Test void promotionSnapshotsAuthoritativeTitleAndSurvivesDeletedSource() {
        rollback(() -> {
            long source = journals.create(new JournalCreateRequest("旧标题","日志正文","LEARNING", LocalDate.now(),List.of()));
            journals.get(source); // Populate the optional cache before changing the database directly.
            jdbc.update("UPDATE journal_entry SET title='数据库新标题' WHERE id=?",source);
            long a = service.promote(source,request("手动整理",null,List.of()));
            long b = service.promote(source,request("另一篇",null,List.of()));
            assertThat(service.get(a).sourceJournalTitle()).isEqualTo("数据库新标题");
            assertThat(service.get(b).sourceJournalId()).isEqualTo(source);
            journals.update(source,new JournalUpdateRequest("后来标题","后来正文","LEARNING",LocalDate.now(),List.of()));
            assertThat(service.get(a).sourceJournalTitle()).isEqualTo("数据库新标题");
            assertThat(service.get(a).contentMd()).contains("example.png");
            journals.delete(source);
            assertThat(service.get(a).sourceJournalId()).isNull();
            assertThat(service.get(a).sourceJournalTitle()).isEqualTo("数据库新标题");
            expectCode(() -> service.promote(source,request("不可创建",null,List.of())), ErrorCode.NOT_FOUND);
        });
    }
    @Test void invalidTagRollsBackCreateAndUpdateIncludingOldAssociations() {
        long tag = tags.create(new TagCreateRequest(unique(),null));
        long id = service.create(request(unique(),null,List.of(tag)));
        String before = service.get(id).title();
        try {
            int count = jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_item",Integer.class);
            expectCode(() -> service.create(request(unique(),null,List.of(Long.MAX_VALUE))),ErrorCode.NOT_FOUND);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_item",Integer.class)).isEqualTo(count);
            expectCode(() -> service.update(id,request("must rollback",null,List.of(Long.MAX_VALUE))),ErrorCode.NOT_FOUND);
            assertThat(service.get(id).title()).isEqualTo(before);
            assertThat(service.get(id).tags()).extracting(t -> t.id()).containsExactly(tag);
        } finally { service.delete(id); tags.delete(tag); }
    }
    @Test void categoryUniquenessUsesDatabaseCollationAndAllowsClearingDescription() {
        rollback(() -> {
            String name = unique();
            long id = service.createCategory(new CategoryWriteRequest(name,"description"));
            expectCode(() -> service.createCategory(new CategoryWriteRequest("  "+name.toUpperCase()+"  ",null)),ErrorCode.CONFLICT);
            service.updateCategory(id,new CategoryWriteRequest(name," "));
            assertThat(service.categories().stream().filter(c -> c.id()==id).findFirst().orElseThrow().description()).isNull();
        });
    }
    private void expectCode(Runnable action, ErrorCode code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(code));
    }
}
