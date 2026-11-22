package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbCitation;
import com.ktg.mes.ai.domain.KbDocument;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KbRetrievalMapper {
    List<KbCitation> keywordCandidates(@Param("scope") KbDocument.Library scope, @Param("terms") List<String> terms);
    List<KbCitation> activeHits(@Param("scope") KbDocument.Library scope, @Param("ids") List<Long> ids);
    KbDocument.Version nextIndexVersion();
    KbDocument.Version nextRetiredVersion();
    List<KbCitation> indexChunks(@Param("version") KbDocument.Version version, @Param("after") long after);
    void startIndex(KbDocument.Version version);
    void restartIndex(@Param("taskId") long taskId);
    void removeIndexRecord(@Param("taskId") long taskId);
}
