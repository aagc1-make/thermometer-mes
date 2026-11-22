package com.ktg.mes.ai.mapper;

import com.ktg.mes.ai.domain.KbLibrarySummary;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface KbLibraryMapper {
    List<KbLibrarySummary> selectVisibleByUserId(@Param("userId") Long userId);

    List<KbLibrarySummary> selectAdminSummaries();

    int insertForActiveDepartment(@Param("deptId") Long deptId, @Param("name") String name);
}
