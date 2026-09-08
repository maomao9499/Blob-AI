package dev.blob.knowledge.controller;

import dev.blob.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({KnowledgeController.class, KnowledgePromotionController.class})
class KnowledgeControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean KnowledgeService service;

    @Test void createAndPromoteReturnIds() throws Exception {
        when(service.create(any())).thenReturn(7L);
        when(service.promote(eq(3L), any())).thenReturn(8L);
        mvc.perform(post("/api/v1/knowledge").contentType("application/json")
                .content("{\"title\":\"知识\",\"contentMd\":\"正文\",\"tagIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(7));
        mvc.perform(post("/api/v1/journals/3/promote-to-knowledge").contentType("application/json")
                .content("{\"title\":\"知识\",\"contentMd\":\"正文\",\"tagIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(8));
    }
    @Test void rejectsInvalidFieldsPaginationAndConflictingCategoryFilter() throws Exception {
        for (String body : new String[]{"{\"title\":\"  \",\"contentMd\":\"正文\"}",
                "{\"title\":\"知识\",\"contentMd\":\" \"}",
                "{\"title\":\"知识\",\"contentMd\":\"正文\",\"tagIds\":[null]}",
                "{\"title\":\"知识\",\"contentMd\":\"正文\",\"categoryId\":0}"}) {
            mvc.perform(post("/api/v1/knowledge").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/v1/knowledge").param("categoryId","1").param("uncategorized","true"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/knowledge").param("pageSize","101"))
                .andExpect(status().isBadRequest());
    }
    @Test void rejectsClientSuppliedSourceAndValidatesTrimmedTitle() throws Exception {
        mvc.perform(post("/api/v1/knowledge").contentType("application/json")
                .content("{\"title\":\"知识\",\"contentMd\":\"正文\",\"sourceJournalId\":1}"))
                .andExpect(status().isBadRequest());
        when(service.create(any())).thenReturn(7L);
        mvc.perform(post("/api/v1/knowledge").contentType("application/json")
                .content("{\"title\":\"  " + "x".repeat(200) + "  \",\"contentMd\":\"正文\"}"))
                .andExpect(status().isOk());
    }
    @Test void rejectsUnsupportedRelationType() throws Exception {
        mvc.perform(post("/api/v1/knowledge/relations").contentType("application/json")
                .content("{\"sourceKnowledgeId\":1,\"targetKnowledgeId\":2,\"relationType\":\"OTHER\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void categoryEndpointsUseSharedEnvelopeAndRejectInvalidNames() throws Exception {
        when(service.createCategory(any())).thenReturn(4L);
        when(service.categories()).thenReturn(java.util.List.of(
                new dev.blob.knowledge.dto.CategoryResponse(4,"分类","描述",null,null)));
        mvc.perform(post("/api/v1/knowledge/categories").contentType("application/json")
                .content("{\"name\":\"分类\",\"description\":\"描述\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(4));
        mvc.perform(get("/api/v1/knowledge/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").value("分类"));
        mvc.perform(put("/api/v1/knowledge/categories/4").contentType("application/json")
                .content("{\"name\":\"新分类\",\"description\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(delete("/api/v1/knowledge/categories/4"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(post("/api/v1/knowledge/categories").contentType("application/json")
                .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void relationEndpointsReturnRelationshipAndPeerIdsSeparately() throws Exception {
        when(service.createRelation(any())).thenReturn(9L);
        when(service.relations(1,1,20)).thenReturn(new dev.blob.common.api.PageResponse<>(
                java.util.List.of(new dev.blob.knowledge.dto.KnowledgeRelationResponse(9,2,"对端")),1,1,20));
        mvc.perform(post("/api/v1/knowledge/relations").contentType("application/json")
                .content("{\"sourceKnowledgeId\":1,\"targetKnowledgeId\":2,\"relationType\":\"RELATED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(9));
        mvc.perform(get("/api/v1/knowledge/1/relations"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].id").value(9))
                .andExpect(jsonPath("$.data.items[0].knowledgeId").value(2))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(delete("/api/v1/knowledge/relations/9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/v1/knowledge/1/relations").param("page","0"))
                .andExpect(status().isBadRequest());
    }
    @Test void getUpdateDeleteAndBusinessErrorsFollowM1Contract() throws Exception {
        when(service.get(7)).thenReturn(new dev.blob.knowledge.dto.KnowledgeDetailResponse(
                7,"知识","原文",null,null,java.util.List.of(),null,"删除前来源",null,null));
        mvc.perform(get("/api/v1/knowledge/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contentMd").value("原文"))
                .andExpect(jsonPath("$.data.sourceJournalId").isEmpty())
                .andExpect(jsonPath("$.data.sourceJournalTitle").value("删除前来源"));
        mvc.perform(put("/api/v1/knowledge/7").contentType("application/json")
                .content("{\"title\":\"新知识\",\"contentMd\":\"新正文\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(delete("/api/v1/knowledge/7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        when(service.get(99)).thenThrow(new dev.blob.common.error.BusinessException(
                dev.blob.common.error.ErrorCode.NOT_FOUND,org.springframework.http.HttpStatus.NOT_FOUND,"知识不存在"));
        mvc.perform(get("/api/v1/knowledge/99"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        org.mockito.Mockito.doThrow(new dev.blob.common.error.BusinessException(
                dev.blob.common.error.ErrorCode.CONFLICT,org.springframework.http.HttpStatus.CONFLICT,"分类被使用"))
                .when(service).deleteCategory(4);
        mvc.perform(delete("/api/v1/knowledge/categories/4"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }

}
