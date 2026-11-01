package com.ktg.mes.wm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 工单成品可售清单（销售出库单添加"工单生成的产品"时的来源数据）
 *
 * <p>口径：</p>
 * <ul>
 *   <li>来源 = 已执行（FINISHED）的产品入库单行，按「工单 + 产品」汇总；工单取行上 workorder_id，
 *       历史行没有行级工单时回退到入库单头的 workorder_id；</li>
 *   <li>已开单 = 未取消（非 CANCELED）的销售出库行数量合计，草稿与在途都占用可售量，避免超卖；</li>
 *   <li>可售 = 已入库 − 已开单；批次取该工单最近一次入库的批次，拣货时仍以实际库存批次为准。</li>
 * </ul>
 */
@Data
public class WorkorderProductSaleVO {

    /** 生产工单ID */
    private Long workorderId;

    /** 生产工单编号 */
    private String workorderCode;

    /** 生产工单名称 */
    private String workorderName;

    /** 生产工单状态 */
    private String workorderStatus;

    /** 产品物料ID */
    private Long itemId;

    /** 产品物料编码 */
    private String itemCode;

    /** 产品物料名称 */
    private String itemName;

    /** 规格型号 */
    private String specification;

    /** 单位 */
    private String unitOfMeasure;

    /** 单位名称 */
    private String unitName;

    /** 批次ID（最近一次入库批次） */
    private Long batchId;

    /** 批次号（最近一次入库批次） */
    private String batchCode;

    /** 已入库数量 */
    private BigDecimal receivedQuantity;

    /** 已开单数量（未取消的销售出库行合计） */
    private BigDecimal issuedQuantity;

    /** 可售数量 = 已入库 − 已开单 */
    private BigDecimal availableQuantity;

    /** 最近一次入库单编号 */
    private String recptCode;

    /** 最近一次入库日期 */
    private Date recptDate;

    /** 查询用：工单编号模糊匹配 */
    private String workorderCodeLike;

    /** 查询用：产品编码模糊匹配 */
    private String itemCodeLike;

    /** 查询用：产品名称模糊匹配 */
    private String itemNameLike;

    /** 查询用：是否只看有可售数量的行 */
    private String onlyAvailable;

    /** 查询用：排除的出库行ID（编辑已有行时避免把自己的数量算成已占用） */
    private Long excludeLineId;
}
