package com.ktg.mes.wm.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.common.utils.DateUtils;
import com.ktg.common.utils.StringUtils;
import com.ktg.mes.linkage.SalesNoticeLinkageService;
import com.ktg.mes.md.domain.MdProductBom;
import com.ktg.mes.md.service.IMdProductBomService;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.pro.domain.ProWorkorderBom;
import com.ktg.mes.pro.service.IProWorkorderBomService;
import com.ktg.mes.pro.service.IProWorkorderLineService;
import com.ktg.mes.pro.service.IProWorkorderService;
import com.ktg.mes.qc.domain.QcOqc;
import com.ktg.mes.qc.domain.QcTemplateProduct;
import com.ktg.mes.qc.service.IQcOqcService;
import com.ktg.mes.qc.service.IQcTemplateProductService;
import com.ktg.mes.wm.domain.WmSalesNotice;
import com.ktg.mes.wm.domain.WmSalesNoticeLine;
import com.ktg.mes.wm.domain.WmSalesOrder;
import com.ktg.mes.wm.domain.WmSalesOrderLine;
import com.ktg.mes.wm.mapper.WmSalesOrderLineMapper;
import com.ktg.mes.wm.mapper.WmSalesOrderMapper;
import com.ktg.mes.wm.service.IWmSalesNoticeLineService;
import com.ktg.mes.wm.service.IWmSalesNoticeService;
import com.ktg.mes.wm.service.IWmSalesOrderService;
import com.ktg.mes.wm.utils.WmBarCodeUtil;
import com.ktg.system.service.CodeSequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户销售订单Service业务层处理
 *
 * <p>订单是整条业务链路的源头：确认下单后可一键生成生产工单、出货检验单、发货通知单；
 * 下游单据通过订单号（order_code / so_code）与订单行回写的工单号、检验单号关联，
 * 发货通知确认后再自动生成销售出库草稿，实现「订单 → 工单 → 质检 → 发货」全链路可追溯。</p>
 *
 * @author ktg
 * @date 2026-11-14
 */
@Service
public class WmSalesOrderServiceImpl implements IWmSalesOrderService
{
    /** 来源单据类型：客户销售订单 */
    public static final String SOURCE_DOC_TYPE_SALES_ORDER = "SALES_ORDER";

    @Autowired
    private WmSalesOrderMapper wmSalesOrderMapper;

    @Autowired
    private WmSalesOrderLineMapper wmSalesOrderLineMapper;

    @Autowired
    private CodeSequenceService codeSequenceService;

    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private IProWorkorderLineService proWorkorderLineService;

    @Autowired
    private IProWorkorderBomService proWorkorderBomService;

    @Autowired
    private IMdProductBomService mdProductBomService;

    @Autowired
    private IWmSalesNoticeService wmSalesNoticeService;

    @Autowired
    private IWmSalesNoticeLineService wmSalesNoticeLineService;

    @Autowired
    private SalesNoticeLinkageService salesNoticeLinkageService;

    @Autowired
    private IQcOqcService qcOqcService;

    @Autowired
    private IQcTemplateProductService qcTemplateProductService;

    @Autowired
    private WmBarCodeUtil wmBarCodeUtil;

    @Override
    public WmSalesOrder selectWmSalesOrderByOrderId(Long orderId)
    {
        WmSalesOrder order = wmSalesOrderMapper.selectWmSalesOrderByOrderId(orderId);
        if (order != null)
        {
            WmSalesOrderLine param = new WmSalesOrderLine();
            param.setOrderId(orderId);
            order.setLines(wmSalesOrderLineMapper.selectWmSalesOrderLineList(param));
        }
        return order;
    }

    @Override
    public List<WmSalesOrder> selectWmSalesOrderList(WmSalesOrder wmSalesOrder)
    {
        return wmSalesOrderMapper.selectWmSalesOrderList(wmSalesOrder);
    }

    @Override
    public String checkOrderCodeUnique(WmSalesOrder wmSalesOrder)
    {
        WmSalesOrder order = wmSalesOrderMapper.checkOrderCodeUnique(wmSalesOrder);
        Long orderId = wmSalesOrder.getOrderId() == null ? -1L : wmSalesOrder.getOrderId();
        if (StringUtils.isNotNull(order) && order.getOrderId().longValue() != orderId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWmSalesOrder(WmSalesOrder wmSalesOrder)
    {
        validateLines(wmSalesOrder);
        wmSalesOrder.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        if (wmSalesOrder.getOrderDate() == null)
        {
            wmSalesOrder.setOrderDate(DateUtils.getNowDate());
        }
        fillTotals(wmSalesOrder);
        wmSalesOrder.setCreateTime(DateUtils.getNowDate());
        int rows = wmSalesOrderMapper.insertWmSalesOrder(wmSalesOrder);
        if (rows > 0)
        {
            saveLines(wmSalesOrder.getOrderId(), wmSalesOrder.getLines(), wmSalesOrder.getCreateBy());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateWmSalesOrder(WmSalesOrder wmSalesOrder)
    {
        WmSalesOrder existing = wmSalesOrderMapper.selectWmSalesOrderByOrderId(wmSalesOrder.getOrderId());
        if (existing == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(existing.getStatus()))
        {
            throw new ServiceException("只有草稿状态的订单可以修改");
        }
        validateLines(wmSalesOrder);
        fillTotals(wmSalesOrder);
        wmSalesOrder.setUpdateTime(DateUtils.getNowDate());
        int rows = wmSalesOrderMapper.updateWmSalesOrder(wmSalesOrder);
        if (rows > 0)
        {
            wmSalesOrderLineMapper.deleteWmSalesOrderLineByOrderId(wmSalesOrder.getOrderId());
            saveLines(wmSalesOrder.getOrderId(), wmSalesOrder.getLines(), wmSalesOrder.getUpdateBy());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteWmSalesOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds)
        {
            WmSalesOrder order = wmSalesOrderMapper.selectWmSalesOrderByOrderId(orderId);
            if (order == null)
            {
                continue;
            }
            if (!UserConstants.ORDER_STATUS_PREPARE.equals(order.getStatus()))
            {
                throw new ServiceException("只能删除草稿状态的订单：" + order.getOrderCode());
            }
            WmSalesOrderLine param = new WmSalesOrderLine();
            param.setOrderId(orderId);
            List<WmSalesOrderLine> lines = wmSalesOrderLineMapper.selectWmSalesOrderLineList(param);
            for (WmSalesOrderLine line : lines)
            {
                if (line.getWorkorderId() != null || line.getOqcId() != null)
                {
                    throw new ServiceException("订单已生成下游单据，不能删除：" + order.getOrderCode());
                }
            }
            wmSalesOrderLineMapper.deleteWmSalesOrderLineByOrderId(orderId);
        }
        return wmSalesOrderMapper.deleteWmSalesOrderByOrderIds(orderIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmOrder(Long orderId, String operator)
    {
        WmSalesOrder order = wmSalesOrderMapper.selectWmSalesOrderByOrderId(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (UserConstants.ORDER_STATUS_CANCELED.equals(order.getStatus())
                || UserConstants.ORDER_STATUS_FINISHED.equals(order.getStatus()))
        {
            throw new ServiceException("当前订单状态不允许确认");
        }
        if (!UserConstants.ORDER_STATUS_PREPARE.equals(order.getStatus()))
        {
            // 已确认的订单重复点击视为成功，保证前端幂等
            return 1;
        }
        WmSalesOrderLine param = new WmSalesOrderLine();
        param.setOrderId(orderId);
        if (wmSalesOrderLineMapper.selectWmSalesOrderLineList(param).isEmpty())
        {
            throw new ServiceException("请先添加订单明细");
        }
        WmSalesOrder update = new WmSalesOrder();
        update.setOrderId(orderId);
        update.setStatus(UserConstants.ORDER_STATUS_CONFIRMED);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = wmSalesOrderMapper.updateWmSalesOrder(update);
        // 管理员确认订单即预建全链单据：工单、OQC、发货通知统一走链号（dd...0004 -> gd/zj/ft...0004）
        generateWorkorders(orderId, operator);
        generateOqc(orderId, operator);
        generateNotice(orderId, operator);
        createSalesDraft(orderId, operator);
        return rows;
    }

    /** 订单确认即预建销售出库草稿（xc+链号），发货通知确认时复用同一张，不再新建。 */
    private void createSalesDraft(Long orderId, String operator)
    {
        WmSalesOrder order = wmSalesOrderMapper.selectWmSalesOrderByOrderId(orderId);
        if (order == null || order.getNoticeId() == null)
        {
            return;
        }
        WmSalesNotice notice = wmSalesNoticeService.selectWmSalesNoticeByNoticeId(order.getNoticeId());
        if (notice == null)
        {
            return;
        }
        WmSalesNoticeLine query = new WmSalesNoticeLine();
        query.setNoticeId(notice.getNoticeId());
        List<WmSalesNoticeLine> lines = wmSalesNoticeLineService.selectWmSalesNoticeLineList(query);
        if (lines == null || lines.isEmpty())
        {
            return;
        }
        salesNoticeLinkageService.createDraft(notice, lines, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateWorkorders(Long orderId, String operator)
    {
        WmSalesOrder order = requireConfirmedOrder(orderId);
        List<WmSalesOrderLine> lines = loadLines(orderId);
        List<String> createdCodes = new ArrayList<>();
        List<String> existedCodes = new ArrayList<>();
        for (WmSalesOrderLine line : lines)
        {
            if (UserConstants.ORDER_STATUS_CANCELED.equals(line.getStatus()))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(line.getWorkorderCode()))
            {
                existedCodes.add(line.getWorkorderCode());
                continue;
            }
            ProWorkorder wo = buildWorkorder(order, line, operator);
            proWorkorderService.insertProWorkorder(wo);
            generateBomLines(wo);
            wmBarCodeUtil.generateBarCode(UserConstants.BARCODE_TYPE_WORKORDER,
                    wo.getWorkorderId(), wo.getWorkorderCode(), wo.getWorkorderName());

            WmSalesOrderLine updateLine = new WmSalesOrderLine();
            updateLine.setLineId(line.getLineId());
            updateLine.setWorkorderId(wo.getWorkorderId());
            updateLine.setWorkorderCode(wo.getWorkorderCode());
            updateLine.setStatus("PRODUCING");
            updateLine.setUpdateBy(operator);
            updateLine.setUpdateTime(DateUtils.getNowDate());
            wmSalesOrderLineMapper.updateWmSalesOrderLine(updateLine);
            createdCodes.add(wo.getWorkorderCode());
        }
        if (!createdCodes.isEmpty())
        {
            WmSalesOrder update = new WmSalesOrder();
            update.setOrderId(orderId);
            update.setStatus("PRODUCING");
            update.setUpdateBy(operator);
            update.setUpdateTime(DateUtils.getNowDate());
            wmSalesOrderMapper.updateWmSalesOrder(update);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", order.getOrderCode());
        data.put("createdCodes", createdCodes);
        data.put("existedCodes", existedCodes);
        data.put("createdCount", createdCodes.size());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateNotice(Long orderId, String operator)
    {
        WmSalesOrder order = requireConfirmedOrder(orderId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", order.getOrderCode());

        if (StringUtils.isNotEmpty(order.getNoticeCode()))
        {
            data.put("noticeCode", order.getNoticeCode());
            data.put("created", false);
            return data;
        }

        List<WmSalesOrderLine> lines = loadLines(orderId);
        if (lines.isEmpty())
        {
            throw new ServiceException("订单没有明细，无法生成发货单");
        }

        WmSalesNotice notice = new WmSalesNotice();
        notice.setNoticeCode(codeSequenceService.generateChainCode(UserConstants.CODE_RULE_SALES_NOTICE, order.getOrderCode()));
        notice.setNoticeName(truncate(order.getOrderName() + "发货通知", 250));
        notice.setSoCode(order.getOrderCode());
        notice.setClientId(order.getClientId());
        notice.setClientCode(order.getClientCode());
        notice.setClientName(order.getClientName());
        notice.setClientNick(order.getClientNick());
        notice.setSalesDate(order.getDeliveryDate() != null ? order.getDeliveryDate() : DateUtils.getNowDate());
        notice.setRecipient(order.getRecipient());
        notice.setTel(order.getTel());
        notice.setAddress(order.getAddress());
        notice.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        notice.setRemark("客户订单 " + order.getOrderCode() + " 自动生成");
        notice.setCreateBy(operator);
        wmSalesNoticeService.insertWmSalesNotice(notice);

        for (WmSalesOrderLine source : lines)
        {
            if (UserConstants.ORDER_STATUS_CANCELED.equals(source.getStatus()))
            {
                continue;
            }
            BigDecimal remaining = nvl(source.getQuantity()).subtract(nvl(source.getQuantityShipped()));
            if (remaining.compareTo(BigDecimal.ZERO) <= 0)
            {
                continue;
            }
            WmSalesNoticeLine line = new WmSalesNoticeLine();
            line.setNoticeId(notice.getNoticeId());
            line.setItemId(source.getItemId());
            line.setItemCode(source.getItemCode());
            line.setItemName(source.getItemName());
            line.setSpecification(source.getSpecification());
            line.setUnitOfMeasure(source.getUnitOfMeasure());
            line.setUnitName(source.getUnitName());
            line.setQuantitySales(remaining);
            line.setOqcCheck("Y");
            line.setRemark("订单行 " + source.getLineId());
            line.setCreateBy(operator);
            wmSalesNoticeLineService.insertWmSalesNoticeLine(line);
        }

        WmSalesOrder update = new WmSalesOrder();
        update.setOrderId(orderId);
        update.setNoticeId(notice.getNoticeId());
        update.setNoticeCode(notice.getNoticeCode());
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        wmSalesOrderMapper.updateWmSalesOrder(update);

        data.put("noticeCode", notice.getNoticeCode());
        data.put("created", true);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateOqc(Long orderId, String operator)
    {
        WmSalesOrder order = requireConfirmedOrder(orderId);
        List<WmSalesOrderLine> lines = loadLines(orderId);
        List<String> createdCodes = new ArrayList<>();
        List<String> existedCodes = new ArrayList<>();
        for (WmSalesOrderLine line : lines)
        {
            if (UserConstants.ORDER_STATUS_CANCELED.equals(line.getStatus()))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(line.getOqcCode()))
            {
                existedCodes.add(line.getOqcCode());
                continue;
            }
            QcTemplateProduct param = new QcTemplateProduct();
            param.setItemId(line.getItemId());
            List<QcTemplateProduct> templates = qcTemplateProductService.selectQcTemplateProductList(param);
            if (templates == null || templates.isEmpty())
            {
                throw new ServiceException("产品[" + line.getItemCode() + " " + line.getItemName() + "]未配置检测模板，无法生成质检单");
            }

            QcOqc oqc = new QcOqc();
            // 链号：订单 dd...0004 -> 首行 zj...0004，多行依次 zj...0004-01/-02
            oqc.setOqcCode(codeSequenceService.generateChainCode(
                    UserConstants.CODE_RULE_OQC, order.getOrderCode()));
            oqc.setOqcName(truncate(order.getOrderName() + "-" + line.getItemName() + "出货检验", 490));
            oqc.setTemplateId(templates.get(0).getTemplateId());
            oqc.setSourceDocId(orderId);
            oqc.setSourceDocType(SOURCE_DOC_TYPE_SALES_ORDER);
            oqc.setSourceDocCode(order.getOrderCode());
            oqc.setSourceLineId(line.getLineId());
            oqc.setClientId(order.getClientId());
            oqc.setClientCode(order.getClientCode());
            oqc.setClientName(order.getClientName());
            oqc.setItemId(line.getItemId());
            oqc.setItemCode(line.getItemCode());
            oqc.setItemName(line.getItemName());
            oqc.setSpecification(line.getSpecification());
            oqc.setUnitOfMeasure(line.getUnitOfMeasure());
            oqc.setQuantityOut(nvl(line.getQuantity()));
            oqc.setQuantityCheck(nvl(line.getQuantity()));
            oqc.setQuantityUnqualified(BigDecimal.ZERO);
            oqc.setQuantityQualified(BigDecimal.ZERO);
            oqc.setOutDate(order.getDeliveryDate() != null ? order.getDeliveryDate() : DateUtils.getNowDate());
            oqc.setStatus(UserConstants.ORDER_STATUS_PREPARE);
            oqc.setRemark("客户订单 " + order.getOrderCode() + " 自动生成");
            oqc.setCreateBy(operator);
            qcOqcService.insertQcOqc(oqc);

            WmSalesOrderLine updateLine = new WmSalesOrderLine();
            updateLine.setLineId(line.getLineId());
            updateLine.setOqcId(oqc.getOqcId());
            updateLine.setOqcCode(oqc.getOqcCode());
            updateLine.setUpdateBy(operator);
            updateLine.setUpdateTime(DateUtils.getNowDate());
            wmSalesOrderLineMapper.updateWmSalesOrderLine(updateLine);
            createdCodes.add(oqc.getOqcCode());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderCode", order.getOrderCode());
        data.put("createdCodes", createdCodes);
        data.put("existedCodes", existedCodes);
        data.put("createdCount", createdCodes.size());
        return data;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private WmSalesOrder requireConfirmedOrder(Long orderId)
    {
        WmSalesOrder order = wmSalesOrderMapper.selectWmSalesOrderByOrderId(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在");
        }
        if (UserConstants.ORDER_STATUS_PREPARE.equals(order.getStatus()))
        {
            throw new ServiceException("请先确认下单，再生成下游单据");
        }
        if (UserConstants.ORDER_STATUS_CANCELED.equals(order.getStatus()))
        {
            throw new ServiceException("已取消的订单不能生成下游单据");
        }
        return order;
    }

    private List<WmSalesOrderLine> loadLines(Long orderId)
    {
        WmSalesOrderLine param = new WmSalesOrderLine();
        param.setOrderId(orderId);
        return wmSalesOrderLineMapper.selectWmSalesOrderLineList(param);
    }

    private void validateLines(WmSalesOrder order)
    {
        if (order.getLines() == null || order.getLines().isEmpty())
        {
            throw new ServiceException("请至少添加一条订单明细");
        }
        for (WmSalesOrderLine line : order.getLines())
        {
            if (line.getItemId() == null)
            {
                throw new ServiceException("订单明细必须选择产品");
            }
            if (nvl(line.getQuantity()).compareTo(BigDecimal.ZERO) <= 0)
            {
                throw new ServiceException("订单明细数量必须大于 0");
            }
        }
    }

    private void saveLines(Long orderId, List<WmSalesOrderLine> lines, String operator)
    {
        if (lines == null)
        {
            return;
        }
        for (WmSalesOrderLine line : lines)
        {
            line.setOrderId(orderId);
            line.setQuantityProduced(BigDecimal.ZERO);
            line.setQuantityShipped(BigDecimal.ZERO);
            line.setAmount(nvl(line.getQuantity()).multiply(nvl(line.getPrice())));
            line.setStatus(UserConstants.ORDER_STATUS_PREPARE);
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            wmSalesOrderLineMapper.insertWmSalesOrderLine(line);
        }
    }

    private void fillTotals(WmSalesOrder order)
    {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (order.getLines() != null)
        {
            for (WmSalesOrderLine line : order.getLines())
            {
                totalQuantity = totalQuantity.add(nvl(line.getQuantity()));
                totalAmount = totalAmount.add(nvl(line.getQuantity()).multiply(nvl(line.getPrice())));
            }
        }
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
    }

    private ProWorkorder buildWorkorder(WmSalesOrder order, WmSalesOrderLine line, String operator)
    {
        ProWorkorder wo = new ProWorkorder();
        // 链号：订单 dd...0004 -> 首行工单 gd...0004，多行依次 gd...0004-01/-02
        wo.setWorkorderCode(codeSequenceService.generateChainCode(
                UserConstants.CODE_RULE_WORKORDER, order.getOrderCode()));
        wo.setWorkorderName(truncate(order.getOrderName() + "-" + line.getItemName(), 250));
        wo.setWorkorderType("SELF");
        wo.setOrderSource(SOURCE_DOC_TYPE_SALES_ORDER);
        wo.setSourceCode(order.getOrderCode());
        wo.setSourceDocType(SOURCE_DOC_TYPE_SALES_ORDER);
        wo.setSourceDocId(order.getOrderId());
        wo.setSourceDocCode(order.getOrderCode());
        wo.setProductId(line.getItemId());
        wo.setProductCode(line.getItemCode());
        wo.setProductName(line.getItemName());
        wo.setProductSpc(line.getSpecification());
        wo.setUnitOfMeasure(line.getUnitOfMeasure());
        wo.setUnitName(line.getUnitName());
        wo.setQuantity(nvl(line.getQuantity()));
        wo.setClientId(order.getClientId());
        wo.setClientCode(order.getClientCode());
        wo.setClientName(order.getClientName());
        wo.setRequestDate(order.getDeliveryDate() != null ? order.getDeliveryDate() : new Date());
        wo.setParentId(0L);
        wo.setAncestors("0");
        wo.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        wo.setAttr1("AUTO:SALES_ORDER:" + order.getOrderId() + ":" + line.getLineId());
        wo.setCreateBy(operator);
        return wo;
    }

    /** 按产品 BOM 生成工单用料，保证排产/领料流水线可继续。 */
    private void generateBomLines(ProWorkorder workorder)
    {
        Long defaultLineId = proWorkorderLineService.getDefaultLineId(workorder.getWorkorderId());
        MdProductBom param = new MdProductBom();
        param.setItemId(workorder.getProductId());
        List<MdProductBom> boms = mdProductBomService.selectMdProductBomList(param);
        if (boms == null || boms.isEmpty())
        {
            return;
        }
        BigDecimal orderQuantity = nvl(workorder.getQuantity());
        for (MdProductBom bom : boms)
        {
            ProWorkorderBom workorderBom = new ProWorkorderBom();
            workorderBom.setWorkorderId(workorder.getWorkorderId());
            workorderBom.setWorkorderLineId(defaultLineId);
            workorderBom.setItemId(bom.getBomItemId());
            workorderBom.setItemCode(bom.getBomItemCode());
            workorderBom.setItemName(bom.getBomItemName());
            workorderBom.setItemSpc(bom.getBomItemSpec());
            workorderBom.setItemOrProduct(bom.getItemOrProduct());
            workorderBom.setUnitOfMeasure(bom.getUnitOfMeasure());
            workorderBom.setUnitName(bom.getUnitName());
            workorderBom.setQuantity(orderQuantity.multiply(nvl(bom.getQuantity())));
            proWorkorderBomService.insertProWorkorderBom(workorderBom);
        }
    }

    private static BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String truncate(String value, int maxLength)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
