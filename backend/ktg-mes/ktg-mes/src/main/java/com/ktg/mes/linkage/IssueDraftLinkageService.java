package com.ktg.mes.linkage;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.mes.md.domain.MdProductBom;
import com.ktg.mes.md.service.IMdProductBomService;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderBom;
import com.ktg.mes.pro.service.IProWorkorderBomService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.wm.domain.WmIssueHeader;
import com.ktg.mes.wm.domain.WmIssueLine;
import com.ktg.mes.wm.service.IWmIssueHeaderService;
import com.ktg.mes.wm.service.IWmIssueLineService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 排产完成后，按工单 BOM 幂等生成"生产领料草稿"。
 *
 * 设计要点：
 * - 一张工单只生成一张领料单（按 workorderId 查重，重复排产不会重复建单）；
 * - 明细来自 pro_workorder_bom（建工单时已按计划数量乘过用量），
 *   所以这里直接用 BOM 数量，不再乘一次；
 * - 草稿状态 PREPARE，仍需仓管员逐行上架/分配批次后执行，符合"保存≠过账"。
 */
@Service
public class IssueDraftLinkageService {

    private final IWmIssueHeaderService issueHeaderService;
    private final IWmIssueLineService issueLineService;
    private final IProWorkorderService workorderService;
    private final IProWorkorderBomService workorderBomService;
    private final IMdProductBomService productBomService;
    private final AutoCodeUtil autoCodeUtil;
    private final MenuLinkageNoticeService menuNoticeService;

    public IssueDraftLinkageService(IWmIssueHeaderService issueHeaderService,
                                    IWmIssueLineService issueLineService,
                                    IProWorkorderService workorderService,
                                    IProWorkorderBomService workorderBomService,
                                    IMdProductBomService productBomService,
                                    AutoCodeUtil autoCodeUtil,
                                    MenuLinkageNoticeService menuNoticeService) {
        this.issueHeaderService = issueHeaderService;
        this.issueLineService = issueLineService;
        this.workorderService = workorderService;
        this.workorderBomService = workorderBomService;
        this.productBomService = productBomService;
        this.autoCodeUtil = autoCodeUtil;
        this.menuNoticeService = menuNoticeService;
    }

    /**
     * 排产后调用：工单没有领料单且 BOM 有料时，建一张领料草稿并点亮「生产领料」。
     *
     * @return 本次新建的领料单；已存在或无需领料时返回已有单据 / null
     */
    @Transactional
    public WmIssueHeader createDraftIfNeeded(Long workorderId, String operator) {
        if (workorderId == null) {
            return null;
        }
        ProWorkorder workorder = workorderService.selectProWorkorderByWorkorderId(workorderId);
        if (workorder == null) {
            throw new ServiceException("排产对应的生产工单不存在");
        }

        // ① 幂等：同一工单已有领料单就不再新建
        WmIssueHeader query = new WmIssueHeader();
        query.setWorkorderId(workorderId);
        List<WmIssueHeader> existing = issueHeaderService.selectWmIssueHeaderList(query);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        // ② 明细来源：工单 BOM；若工单没有展开 BOM（历史工单/手工建单），
        //    回退用产品 BOM × 计划数量，保证排产后一定能生成可用的领料草稿。
        ProWorkorderBom bomQuery = new ProWorkorderBom();
        bomQuery.setWorkorderId(workorderId);
        List<ProWorkorderBom> boms = workorderBomService.selectProWorkorderBomList(bomQuery);
        boolean useProductBom = (boms == null || boms.isEmpty());

        List<MdProductBom> productBoms = null;
        if (useProductBom) {
            MdProductBom pb = new MdProductBom();
            pb.setItemId(workorder.getProductId());
            productBoms = productBomService.selectMdProductBomList(pb);
            if (productBoms == null || productBoms.isEmpty()) {
                return null;
            }
        }

        // ③ 建单头
        WmIssueHeader header = new WmIssueHeader();
        header.setIssueCode(autoCodeUtil.genSerialCode(UserConstants.ISSUE_CODE, ""));
        header.setIssueName(workorder.getWorkorderName() + "生产领料");
        header.setWorkorderId(workorder.getWorkorderId());
        header.setWorkorderCode(workorder.getWorkorderCode());
        header.setIssueDate(DateUtils.getNowDate());
        header.setRequiredTime(workorder.getRequestDate());
        header.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        header.setAttr1("AUTO:ISSUE:WORKORDER:" + workorderId);
        header.setCreateBy(operator);
        header.setRemark("排产自动生成领料草稿");
        issueHeaderService.insertWmIssueHeader(header);

        // ④ 建明细
        //    - 工单 BOM 已按计划数量展开，直接用
        //    - 回退到产品 BOM 时，需要乘以工单计划数量
        int lineCount = 0;
        BigDecimal planQty = workorder.getQuantity() == null ? BigDecimal.ONE : workorder.getQuantity();
        if (!useProductBom) {
            for (ProWorkorderBom bom : boms) {
                BigDecimal quantity = bom.getQuantity();
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                WmIssueLine line = new WmIssueLine();
                line.setIssueId(header.getIssueId());
                line.setItemId(bom.getItemId());
                line.setItemCode(bom.getItemCode());
                line.setItemName(bom.getItemName());
                line.setSpecification(bom.getItemSpc());
                line.setUnitOfMeasure(bom.getUnitOfMeasure());
                line.setUnitName(bom.getUnitName());
                line.setQuantityIssued(quantity);
                line.setAttr1("AUTO:ISSUE_LINE:WORKORDER_BOM:" + bom.getLineId());
                line.setCreateBy(operator);
                issueLineService.insertWmIssueLine(line);
                lineCount++;
            }
        } else {
            for (MdProductBom bom : productBoms) {
                BigDecimal perUnit = bom.getQuantity();
                if (perUnit == null || perUnit.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                WmIssueLine line = new WmIssueLine();
                line.setIssueId(header.getIssueId());
                line.setItemId(bom.getBomItemId());
                line.setItemCode(bom.getBomItemCode());
                line.setItemName(bom.getBomItemName());
                line.setSpecification(bom.getBomItemSpec());
                line.setUnitOfMeasure(bom.getUnitOfMeasure());
                line.setUnitName(bom.getUnitName());
                line.setQuantityIssued(perUnit.multiply(planQty));
                line.setAttr1("AUTO:ISSUE_LINE:PRODUCT_BOM:" + bom.getBomId());
                line.setCreateBy(operator);
                issueLineService.insertWmIssueLine(line);
                lineCount++;
            }
        }

        if (lineCount == 0) {
            // 全部 BOM 行数量为 0：抛异常触发事务回滚，不会留下"有单无明细"的空草稿
            throw new ServiceException("工单 BOM 用量为空，未生成领料草稿");
        }

        menuNoticeService.markUnread(MenuLinkageNoticePaths.ISSUE);
        return header;
    }
}
