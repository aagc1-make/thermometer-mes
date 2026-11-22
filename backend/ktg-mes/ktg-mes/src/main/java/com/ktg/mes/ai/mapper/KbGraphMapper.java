package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbDocument;
import com.ktg.mes.ai.service.KbGraphExtraction.Entity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KbGraphMapper {
    List<Entity> anchors();
    KbDocument.Version nextVersion(@Param("versionId") Long versionId);
    KbDocument.Version retiredVersion();
    List<Long> visibleVersions(@Param("scope") KbDocument.Library scope);
    int start(@Param("version") KbDocument.Version version);
    int restart(@Param("taskId") long taskId);
    int remove(@Param("taskId") long taskId);
    int retry(@Param("versionId") long versionId);
}
