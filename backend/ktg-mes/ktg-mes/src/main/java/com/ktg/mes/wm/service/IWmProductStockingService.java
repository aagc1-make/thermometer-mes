package com.ktg.mes.wm.service;

import com.ktg.mes.wm.domain.WmProductRecptDetail;
import com.ktg.mes.wm.domain.WmProductRecptLine;
import com.ktg.mes.wm.domain.vo.StorageAreaOptionVo;
import com.ktg.mes.wm.domain.vo.StorageLocationOptionVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品上架：库区/库位余量、推荐与混放校验。
 */
public interface IWmProductStockingService {

    /** 库区候选（含余量、推荐、混放拦截） */
    List<StorageLocationOptionVo> locationOptions(Long warehouseId, WmProductRecptLine line, BigDecimal requiredQty);

    /** 库位候选（含余量、推荐、混放拦截） */
    List<StorageAreaOptionVo> areaOptions(Long locationId, WmProductRecptLine line, BigDecimal requiredQty);

    /** 上架明细写入前校验（数量、余量、混放），不通过抛 ServiceException */
    void assertDetailAllowed(WmProductRecptDetail detail);

    /** 待上架 → 已上架 的齐套校验（数量必须等于入库行数量，并复校验余量/混放） */
    void assertReadyToStock(Long recptId);

    /** 行剩余可上架数量 */
    BigDecimal remainingQty(WmProductRecptLine line);
}
