package dev.blob.knowledge.service;

import dev.blob.common.error.BusinessException;
import dev.blob.common.error.ErrorCode;
import dev.blob.journal.service.JournalService;
import dev.blob.knowledge.dto.*;
import dev.blob.knowledge.mapper.*;
import dev.blob.tag.service.TagService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceImplTest {
    @Mock KnowledgeItemMapper items;
    @Mock KnowledgeCategoryMapper categories;
    @Mock KnowledgeRelationMapper relations;
    @Mock TagService tags;
    @Mock JournalService journals;
    @InjectMocks KnowledgeServiceImpl service;

    @Test void rejectsInvalidWritesEvenOutsideController() {
        for (KnowledgeWriteRequest request : List.of(
                new KnowledgeWriteRequest(" ","正文",null,null,List.of()),
                new KnowledgeWriteRequest("x".repeat(201),"正文",null,null,List.of()),
                new KnowledgeWriteRequest("标题"," ",null,null,List.of()),
                new KnowledgeWriteRequest("标题","正文","x".repeat(1001),null,List.of()))) {
            assertThatThrownBy(() -> service.create(request)).isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
    }
    @Test void rejectsSelfRelationsAndInvalidTypesBeforeAnyLookup() {
        for (RelationCreateRequest request : List.of(new RelationCreateRequest(1L,1L,"RELATED"),
                new RelationCreateRequest(1L,2L,"OTHER"),new RelationCreateRequest(null,2L,"RELATED"))) {
            assertThatThrownBy(() -> service.createRelation(request)).isInstanceOfSatisfying(BusinessException.class,
                    e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
    }
    @Test void queryUsesLiteralWildcardsAndRejectsConflictingCategories() {
        assertThat(new KnowledgeQuery("  100%_!\\  ",null,false,null,null,null).keywordPattern())
                .isEqualTo("%100!%!_!!\\%");
        assertThatThrownBy(() -> new KnowledgeQuery(null,1L,true,null,1,20))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
