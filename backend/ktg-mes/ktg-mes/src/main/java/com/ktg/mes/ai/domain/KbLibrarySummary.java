package com.ktg.mes.ai.domain;

import lombok.Data;

@Data
public class KbLibrarySummary {
    private Long libraryId;
    private Long deptId;
    private String deptName;
    private String name;
    private String status;
}
