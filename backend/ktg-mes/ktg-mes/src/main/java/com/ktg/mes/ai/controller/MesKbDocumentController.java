package com.ktg.mes.ai.controller;

import com.ktg.common.annotation.Log;
import com.ktg.common.core.domain.AjaxResult;
import com.ktg.common.enums.BusinessType;
import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.service.KbDocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/mes/ai/kb/libraries/{libraryId}/documents")
@PreAuthorize("@ss.hasPermi('mes:ai:query')")
public class MesKbDocumentController {
    private final KbDocumentService documents;

    public MesKbDocumentController(KbDocumentService documents) {
        this.documents = documents;
    }

    @GetMapping
    public AjaxResult list(@PathVariable("libraryId") long libraryId) {
        return AjaxResult.success(documents.list(libraryId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Log(title = "知识库文档上传", businessType = BusinessType.INSERT, isSaveRequestData = false)
    public AjaxResult upload(@PathVariable("libraryId") long libraryId,
                             @RequestParam(value = "docId", required = false) Long docId,
                             @RequestParam(value = "title", required = false) String title,
                             @RequestParam(value = "category", required = false) String category,
                             @RequestParam("version") String version,
                             @RequestParam(value = "graphEnabled", defaultValue = "false") boolean graphEnabled,
                             @RequestParam("file") MultipartFile file) {
        documents.upload(libraryId, docId, title, category, version, graphEnabled, file);
        return AjaxResult.success();
    }

    @GetMapping("/versions/{versionId}/preview")
    public AjaxResult preview(@PathVariable("libraryId") long libraryId, @PathVariable("versionId") long versionId,
                              @RequestParam(value = "page", defaultValue = "1") int page) {
        return AjaxResult.success(documents.preview(libraryId, versionId, page));
    }

    @GetMapping("/versions/{versionId}/original")
    @Log(title = "知识库原件下载", businessType = BusinessType.EXPORT, isSaveResponseData = false)
    public ResponseEntity<FileSystemResource> download(@PathVariable("libraryId") long libraryId,
                                                      @PathVariable("versionId") long versionId) {
        KbDocument.Version version = documents.downloadVersion(libraryId, versionId);
        FileSystemResource original = new FileSystemResource(documents.openOriginal(version));
        String disposition = ContentDisposition.attachment().filename(version.getFilename(), StandardCharsets.UTF_8)
            .build().toString();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .header("X-Content-Type-Options", "nosniff")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(version.getSize())
            .body(original);
    }

    @PostMapping("/versions/{versionId}/retry")
    @Log(title = "知识库解析重试", businessType = BusinessType.UPDATE)
    public AjaxResult retry(@PathVariable("libraryId") long libraryId, @PathVariable("versionId") long versionId) {
        documents.retry(libraryId, versionId);
        return AjaxResult.success();
    }

    @DeleteMapping("/{docId}")
    @Log(title = "知识库文档删除", businessType = BusinessType.DELETE)
    public AjaxResult delete(@PathVariable("libraryId") long libraryId, @PathVariable("docId") long docId) {
        documents.delete(libraryId, docId);
        return AjaxResult.success();
    }
}
