package com.ktg.mes.wm.mapper;

import com.ktg.mes.wm.domain.vo.StorageAreaOptionVo;
import com.ktg.mes.wm.domain.vo.StorageLocationOptionVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 产品上架：库区/库位余量与混放校验查询。
 *
 * <p>只做数据读取，推荐顺序、余量计算、混放判定统一在 Service 层完成，
 * 便于单元测试与口径统一。</p>
 */
public interface ProductStockingMapper {

    /**
     * 查询仓库下所有可用库区的容量/占用/预占/同物料同批次存量。
     *
     * @param warehouseId    仓库ID
     * @param itemId         入库物料ID
     * @param batchId        批次ID（可空）
     * @param batchCode      批次号（可空）
     * @param excludeDetailId 编辑上架明细时排除自身（可空）
     */
    List<StorageLocationOptionVo> selectLocationRows(@Param("warehouseId") Long warehouseId,
                                                     @Param("itemId") Long itemId,
                                                     @Param("batchId") Long batchId,
                                                     @Param("batchCode") String batchCode,
                                                     @Param("excludeDetailId") Long excludeDetailId);

    /**
     * 查询库区下所有可用库位的容量/占用/预占与混放维度统计。
     */
    List<StorageAreaOptionVo> selectAreaRows(@Param("locationId") Long locationId,
                                             @Param("itemId") Long itemId,
                                             @Param("batchId") Long batchId,
                                             @Param("batchCode") String batchCode,
                                             @Param("excludeDetailId") Long excludeDetailId);

    /**
     * 指定库位上与本次物料/批次冲突的未过账上架明细数量（其它单据）。
     */
    BigDecimal selectPendingOtherItemQty(@Param("areaId") Long areaId,
                                         @Param("itemId") Long itemId,
                                         @Param("excludeDetailId") Long excludeDetailId);

    BigDecimal selectPendingOtherBatchQty(@Param("areaId") Long areaId,
                                          @Param("itemId") Long itemId,
                                          @Param("batchCode") String batchCode,
                                          @Param("excludeDetailId") Long excludeDetailId);

    /**
     * 指定库位在库的其它物料名称（用于混放提示）。
     */
    String selectOtherItemNames(@Param("areaId") Long areaId, @Param("itemId") Long itemId);

    /**
     * 指定库位在库的同物料其它批次号（用于混放提示）。
     */
    String selectOtherBatchCodes(@Param("areaId") Long areaId,
                                 @Param("itemId") Long itemId,
                                 @Param("batchCode") String batchCode);

    /**
     * 明细行已上架数量（不含指定明细）。
     */
    BigDecimal selectStockedQtyByLine(@Param("lineId") Long lineId,
                                      @Param("excludeDetailId") Long excludeDetailId);
}
