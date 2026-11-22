package com.ktg.mes.ai.domain;

import lombok.Data;

@Data
public class KbCitation {
    private Long chunkId;
    private Long libraryId;
    private Long deptId;
    private Long versionId;
    private String version;
    private String title;
    private String filename;
    private int chunkIndex;
    private String sectionLabel;
    private String content;
}
