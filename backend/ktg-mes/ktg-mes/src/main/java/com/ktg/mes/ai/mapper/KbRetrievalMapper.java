package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KbRetrievalMapper {
    List<KbCitation> keywordCandidates(@Param("scope") KbDocument.Library scope, @Param("terms") List<String> terms);
    List<KbCitation> activeHits(@Param("scope") KbDocument.Library scope, @Param("ids") List<Long> ids);

    /** 影响路径：授权库内包含指定物料编码的当前生效片段。 */
    List<KbCitation> impactChunks(@Param("scope") KbDocument.Library scope, @Param("code") String code,
        @Param("limit") int limit);

    /** 影响路径：同版本相邻片段，用于补齐标题、表头与父件上下文。 */
    List<KbCitation> impactVersionChunks(@Param("scope") KbDocument.Library scope, @Param("versionId") long versionId,
        @Param("from") int from, @Param("to") int to);

    /** 影响路径：该版本正文里出现过的整机成品编码，用于「整机直属组成」这类无编码标题的唯一绑定。 */
    List<String> impactFinishedCodes(@Param("scope") KbDocument.Library scope, @Param("versionId") long versionId,
        @Param("typeCode") String typeCode, @Param("limit") int limit);

    KbDocument.Version nextIndexVersion();
    KbDocument.Version nextRetiredVersion();
    List<KbCitation> indexChunks(@Param("version") KbDocument.Version version, @Param("after") long after);
    void startIndex(KbDocument.Version version);
    void restartIndex(@Param("taskId") long taskId);
    void removeIndexRecord(@Param("taskId") long taskId);
}
