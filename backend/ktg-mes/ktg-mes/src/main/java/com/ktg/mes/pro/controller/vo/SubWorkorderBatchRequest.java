package com.ktg.mes.pro.controller.vo;

import java.util.List;

/**
 * 从父工单的 BOM 行批量生成半成品子工单的请求。
 *
 * <p>由服务端校验父工单、BOM 归属与权限，并用 attr1 中的幂等键 +
 * 数据库唯一约束保证重复点击/重试不会重复生成。</p>
 */
public class SubWorkorderBatchRequest {

    /** 父工单ID */
    private Long parentId;

    /** 父工单的 BOM 行ID集合（必须是该工单自己的 BOM 行） */
    private List<Long> bomLineIds;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<Long> getBomLineIds() {
        return bomLineIds;
    }

    public void setBomLineIds(List<Long> bomLineIds) {
        this.bomLineIds = bomLineIds;
    }
}
