package com.ktg.mes.linkage;

import java.util.ArrayList;
import java.util.List;

/**
 * 领料草稿对账结果：区分新建/已存在/跳过/失败，并携带工单标识，
 * 用于补偿接口向调用方明确体现“部分失败”。
 */
public class IssueDraftReconcileResult {

    private int created;
    private int existed;
    private int skipped;
    private int failed;

    private final List<Long> createdWorkorderIds = new ArrayList<>();
    private final List<Long> existedWorkorderIds = new ArrayList<>();
    private final List<Long> skippedWorkorderIds = new ArrayList<>();
    private final List<Long> failedWorkorderIds = new ArrayList<>();

    public void addCreated(Long workorderId) {
        created++;
        createdWorkorderIds.add(workorderId);
    }

    public void addExisted(Long workorderId) {
        existed++;
        existedWorkorderIds.add(workorderId);
    }

    public void addSkipped(Long workorderId) {
        skipped++;
        skippedWorkorderIds.add(workorderId);
    }

    public void addFailed(Long workorderId) {
        failed++;
        failedWorkorderIds.add(workorderId);
    }

    public boolean hasFailure() {
        return failed > 0;
    }

    public int getCreated() {
        return created;
    }

    public int getExisted() {
        return existed;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getFailed() {
        return failed;
    }

    public List<Long> getCreatedWorkorderIds() {
        return createdWorkorderIds;
    }

    public List<Long> getExistedWorkorderIds() {
        return existedWorkorderIds;
    }

    public List<Long> getSkippedWorkorderIds() {
        return skippedWorkorderIds;
    }

    public List<Long> getFailedWorkorderIds() {
        return failedWorkorderIds;
    }
}
