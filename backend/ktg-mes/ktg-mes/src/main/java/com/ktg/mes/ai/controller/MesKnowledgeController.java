package com.ktg.mes.ai.controller;

import com.ktg.common.core.controller.BaseController;
import com.ktg.common.core.domain.AjaxResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClientException;
import java.io.IOException;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/mes/knowledge")
public class MesKnowledgeController extends BaseController {

    private final JdbcTemplate jdbcTemplate;
    private final MesKnowledgeUploadService uploads;
    private final MesKnowledgeQuestionService questions;

    private final MesKnowledgeGraphViewService graphView;

    public MesKnowledgeController(JdbcTemplate jdbcTemplate,
                                  MesKnowledgeUploadService uploads,
                                  MesKnowledgeQuestionService questions,
                                  MesKnowledgeGraphViewService graphView) {
        this.jdbcTemplate = jdbcTemplate;
        this.uploads = uploads;
        this.questions = questions;
        this.graphView = graphView;
    }

    @GetMapping("/documents")
    public AjaxResult listDocuments() {
        return AjaxResult.success(jdbcTemplate.queryForList(
                "SELECT d.document_id, d.title, d.active_version_id, d.update_time, "
                        + "v.version_no, v.original_name, v.index_status "
                        + "FROM mes_kb_document d "
                        + "LEFT JOIN mes_kb_version v ON v.version_id = d.active_version_id "
                        + "AND v.document_id = d.document_id "
                        + "WHERE d.owner_id = ? AND d.deleted = 0 "
                        + "ORDER BY d.update_time DESC, d.document_id DESC",
                getUserId()
        ));
    }

    @GetMapping("/graph")
    public AjaxResult viewGraph(@RequestParam(value = "factIds", required = false) String factIds) {
        try {
            return AjaxResult.success(graphView.view(getUserId(), factIds));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(400, e.getMessage());
        } catch (RestClientException | IllegalStateException e) {
            logger.error("读取知识图谱失败", e);
            return AjaxResult.error("读取知识图谱失败，请检查 Neo4j 连接、认证配置和后端日志")
                    .put("errorCode", "NEO4J_QUERY_FAILED");
        }
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("id") long id) {
        return uploads.download(getUserId(), id);
    }

    @DeleteMapping("/documents/{id}")
    public AjaxResult deleteDocument(@PathVariable("id") long id) {
        try {
            boolean cleanupComplete = uploads.deleteDocument(getUserId(), id);
            String message = cleanupComplete
                    ? "文档已删除"
                    : "文档已从知识库移除，原文件或向量清理待重试";
            return AjaxResult.success(message,
                    java.util.Collections.singletonMap("cleanupComplete", cleanupComplete));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @PostMapping(value = "/documents", consumes = "multipart/form-data")
    public AjaxResult uploadDocument(@RequestParam("file") MultipartFile file,
                                     @RequestParam("title") String title) {
        try {
            Map<String, Object> result = uploads.upload(getUserId(), title, file);
            if ("FAILED".equals(result.get("indexStatus"))) {
                return AjaxResult.error("文件已保存，但索引失败，请检查 Ollama 和 Qdrant", result);
            }
            return AjaxResult.success("上传并索引成功", result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e) {
            logger.error("保存知识库文件失败", e);
            return AjaxResult.error("保存知识库文件失败");
        }
    }

    @PostMapping(value = "/documents/{id}/versions", consumes = "multipart/form-data")
    public AjaxResult uploadVersion(@PathVariable("id") long id,
                                    @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = uploads.uploadVersion(getUserId(), id, file);
            if ("FAILED".equals(result.get("indexStatus"))) {
                return AjaxResult.error("新版本未生效，请刷新列表后重试", result);
            }
            return AjaxResult.success("新版本已生效", result);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e) {
            logger.error("保存知识库新版本失败", e);
            return AjaxResult.error("保存知识库新版本失败");
        }
    }

    @PostMapping("/documents/{id}/graph/rebuild")
    public AjaxResult rebuildGraph(@PathVariable("id") long id) {
        try {
            return AjaxResult.success("图谱片段已重建", uploads.rebuildGraph(getUserId(), id));
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e) {
            logger.error("读取知识库原文件失败，文档 ID " + id, e);
            return AjaxResult.error("无法读取原文件，请检查知识库文件目录");
        } catch (RestClientException | IllegalStateException e) {
            logger.error("重建知识图谱失败，文档 ID " + id, e);
            return AjaxResult.error("图谱重建失败，请查看后端日志");
        }
    }

    @PostMapping("/ask")
    public AjaxResult ask(@RequestBody JsonNode body) {
        String question = body.path("question").asText("").trim();
        if (!body.path("question").isTextual()
                || question.isEmpty() || question.length() > 500) {
            return AjaxResult.error("请输入 1 到 500 字的问题");
        }

        JsonNode modeNode = body.path("mode");
        String mode = modeNode.isMissingNode() ? "rag" : modeNode.asText("");

        if (!"rag".equals(mode) && !"graph_rag".equals(mode)) {
            return AjaxResult.error("问答模式必须是 rag 或 graph_rag");
        }

        try {
            return AjaxResult.success(questions.ask(getUserId(), question, mode));
        } catch (MesKnowledgeQuestionService.GraphQueryException e) {
            logger.error("图谱问答失败", e);
            return AjaxResult.error(e.getMessage())
                    .put("errorCode", "NEO4J_QUERY_FAILED");
        } catch (MesKnowledgeQuestionService.AnswerCitationException e) {
            logger.warn("知识库回答引用校验失败");
            return AjaxResult.error(e.getMessage())
                    .put("errorCode", "KNOWLEDGE_CITATION_FAILED");
        } catch (RestClientException | IllegalStateException e) {
            logger.error("知识库问答失败", e);
            return AjaxResult.error("知识库问答失败，请检查 Ollama 和 Qdrant 服务");
        }
    }
}
