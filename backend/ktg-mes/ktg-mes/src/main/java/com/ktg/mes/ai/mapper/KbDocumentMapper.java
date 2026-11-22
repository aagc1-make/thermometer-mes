package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KbDocumentMapper {
    KbDocument.Library findLibrary(@Param("libraryId") long libraryId, @Param("userId") long userId,
        @Param("admin") boolean admin, @Param("lock") boolean lock);

    List<KbDocument> listDocuments(@Param("scope") KbDocument.Library scope);
    KbDocument findDocument(@Param("scope") KbDocument.Library scope, @Param("docId") long docId);
    KbDocument findByName(@Param("scope") KbDocument.Library scope, @Param("title") String title,
        @Param("category") String category);
    long countDocuments(@Param("scope") KbDocument.Library scope);
    long countCharacters(@Param("scope") KbDocument.Library scope);
    Long lockLibrary(@Param("libraryId") long libraryId, @Param("deptId") long deptId);
    int activeCharacters(@Param("docId") long docId);
    void insertDocument(KbDocument document);
    void insertVersion(KbDocument.Version version);
    void saveOriginal(KbDocument.Version version);
    void enqueueParse(@Param("versionId") long versionId);
    Long nextPendingVersion();
    KbDocument.Version findPending(@Param("versionId") long versionId);
    KbDocument.Version findVersion(@Param("scope") KbDocument.Library scope, @Param("versionId") long versionId);
    List<KbDocument.Chunk> listChunks(@Param("scope") KbDocument.Library scope, @Param("versionId") long versionId,
        @Param("offset") int offset, @Param("limit") int limit);
    long countChunks(@Param("scope") KbDocument.Library scope, @Param("versionId") long versionId);
    void insertChunks(@Param("version") KbDocument.Version version, @Param("chunks") List<KbDocument.Chunk> chunks);
    void deactivateVersions(@Param("docId") long docId);
    void publishVersion(KbDocument.Version version);
    void publishDocument(KbDocument.Version version);
    void finishParse(@Param("taskId") long taskId, @Param("status") String status, @Param("error") String error);
    int retryParse(@Param("versionId") long versionId);
    void deleteChunks(@Param("scope") KbDocument.Library scope, @Param("docId") long docId);
    void deleteTasks(@Param("docId") long docId);
    void deleteVersions(@Param("docId") long docId);
    void deleteDocument(@Param("scope") KbDocument.Library scope, @Param("docId") long docId);
    List<KbDocument.Version> expiredOriginals();
    void clearOriginal(@Param("versionId") long versionId);
}
