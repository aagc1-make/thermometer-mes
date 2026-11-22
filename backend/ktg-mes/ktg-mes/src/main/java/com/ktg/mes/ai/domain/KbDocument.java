package com.ktg.mes.ai.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KbDocument {
    private Long docId;
    private Long libraryId;
    private Long deptId;
    private String title;
    private String category;
    private boolean graphEnabled;
    private String currentVersion;
    private Long currentVersionId;
    private Long latestVersionId;
    private String latestVersion;
    private String filename;
    private String parseStatus;
    private String parseError;
    private String embedStatus;
    private String embedError;
    private String graphStatus;
    private String graphError;
    private Integer chars;
    private LocalDateTime updateTime;

    @Data
    public static class Library {
        private Long libraryId;
        private Long deptId;
        private String name;
        private boolean maintain;
    }

    @Data
    public static class Version {
        private Long versionId;
        private Long docId;
        private Long libraryId;
        private Long deptId;
        private String title;
        private String version;
        private String filename;
        private long size;
        private int chars;
        private boolean active;
        private String storagePath;
        private String checksumSha256;
        private Long taskId;
        private String parseStatus;
        private boolean graphEnabled;
    }

    @Data
    public static class Chunk {
        private final int chunkIndex;
        private final Integer pageNo;
        private final String sectionLabel;
        private final String content;
    }

    @Data
    public static class Catalog {
        private final Library library;
        private final List<KbDocument> documents;
        private final long characters;
        private final int documentLimit;
        private final int characterLimit;
    }
}
