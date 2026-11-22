package com.ktg.mes.wm.service.impl;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.pro.domain.ProWorkorder;
import com.ktg.mes.wm.domain.WmMaterialStock;
import com.ktg.mes.wm.domain.WmTransfer;
import com.ktg.mes.wm.domain.WmTransferLine;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan;
import com.ktg.mes.wm.domain.vo.WorkorderShortagePlan.*;
import com.ktg.mes.wm.mapper.WorkorderShortageMapper;
import com.ktg.mes.wm.service.IWmTransferLineService;
import com.ktg.mes.wm.service.IWmTransferService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class WorkorderShortageService {
    private final WorkorderShortageMapper mapper;
    private final IWmTransferService transfers;
    private final IWmTransferLineService lines;
    private final AutoCodeUtil codes;

    public WorkorderShortageService(WorkorderShortageMapper mapper, IWmTransferService transfers,
                                    IWmTransferLineService lines, AutoCodeUtil codes) {
        this.mapper = mapper;
        this.transfers = transfers;
        this.lines = lines;
        this.codes = codes;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public WorkorderShortagePlan analyze(long id) {
        ProWorkorder order = requireOrder(id, false);
        WorkorderShortagePlan plan = calculate(order);
        plan.documents = mapper.documents(tag(id));
        plan.requestId = UUID.randomUUID().toString();
        return plan;
    }

    private WorkorderShortagePlan calculate(ProWorkorder order) {
        WorkorderShortagePlan plan = new WorkorderShortagePlan();
        plan.workorderCode = order.getWorkorderCode();
        plan.closed = "FINISHED".equals(order.getStatus()) || "CANCELED".equals(order.getStatus());
        plan.materials = mapper.materials(order.getWorkorderId(), plan.workorderCode);
        Map<Long, Material> missing = new LinkedHashMap<>();
        for (Material row : plan.materials) {
            row.missing = row.required.subtract(row.issued).subtract(row.onHand).max(BigDecimal.ZERO);
            row.uncovered = row.missing;
            if (row.missing.signum() > 0) missing.put(row.itemId, row);
        }
        if (!missing.isEmpty()) {
            for (WmMaterialStock stock : mapper.sources(new ArrayList<>(missing.keySet()))) {
                Material row = missing.get(stock.getItemId());
                BigDecimal qty = row.uncovered.min(stock.getQuantityOnhand());
                if (qty.signum() <= 0) continue;
                Source source = new Source();
                source.stock = stock;
                source.stockId = stock.getMaterialStockId();
                source.batch = stock.getBatchCode();
                source.warehouse = stock.getWarehouseName();
                source.location = stock.getLocationName();
                source.area = stock.getAreaName();
                source.available = stock.getQuantityOnhand();
                source.quantity = qty;
                // 校验用户看过的业务依据，模型文本不参与确认。
                source.version = DigestUtils.sha256Hex(Arrays.asList(order.getWorkorderId(), row.itemId,
                        row.required, row.issued, row.onHand, source.stockId, stock.getWarehouseId(),
                        stock.getLocationId(), stock.getAreaId(), stock.getBatchId(), source.batch,
                        source.available, qty).toString());
                row.sources.add(source);
                row.uncovered = row.uncovered.subtract(qty);
            }
        }
        return plan;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Document confirm(long id, Confirmation request, String operator) {
        SortedMap<Long, String> selected = selections(request);
        String operation = request.requestId + "/" + DigestUtils.sha256Hex(selected.toString());
        ProWorkorder order = requireOrder(id, true);
        List<Document> documents = mapper.documents(tag(id));
        for (Document doc : documents) {
            if (operation.equals(doc.operation)) return doc;
            if (doc.operation != null && doc.operation.startsWith(request.requestId + "/"))
                throw new ServiceException("本次确认已处理，请刷新分析查看关联调拨单");
        }
        if (documents.stream().anyMatch(Document::isActive))
            throw new ServiceException("本工单已有待处理调拨单，请刷新后查看");
        mapper.lockSources(new ArrayList<>(selected.keySet()));
        WorkorderShortagePlan plan = calculate(order);
        if (plan.closed) throw new ServiceException("已结束的工单不能生成调拨草稿");
        List<Source> approved = new ArrayList<>();
        for (Material row : plan.materials) {
            for (Source source : row.sources) {
                if (source.version.equals(selected.get(source.stockId))) approved.add(source);
            }
        }
        if (approved.size() != selected.size()) throw new ServiceException("缺口、批次或可调数量已变化，请刷新分析后重新勾选");
        WmTransfer header = new WmTransfer();
        // 不同工单同时建单时，让下一个生成器看到本事务已提交的流水号。
        mapper.lockTransferCodeRule();
        header.setTransferCode(codes.genSerialCode(UserConstants.TRANSFER_CODE, ""));
        if (UserConstants.NOT_UNIQUE.equals(transfers.checkUnique(header)))
            throw new ServiceException("调拨单编号已被使用，请核对调拨编码规则");
        header.setTransferName(plan.workorderCode + " 缺料调拨");
        header.setTransferType("INNER");
        header.setDeliveryFlag("N");
        header.setConfirmFlag("N");
        header.setStatus(UserConstants.ORDER_STATUS_PREPARE);
        header.setTransferDate(new Date());
        header.setDestination("WH-RAW");
        header.setRemark("来源工单 " + plan.workorderCode + "，目标原料仓 WH-RAW。提交后请在上架环节选择目标库位。");
        header.setAttr1(tag(id));
        header.setAttr2(operation);
        header.setCreateBy(operator);
        if (transfers.insertWmTransfer(header) != 1 || header.getTransferId() == null)
            throw new ServiceException("调拨草稿创建失败");
        for (Source source : approved) {
            WmTransferLine line = new WmTransferLine();
            BeanUtils.copyProperties(source.stock, line, "attr1", "attr2", "attr3", "attr4", "remark",
                    "createTime", "createBy", "updateTime", "updateBy", "params");
            line.setTransferId(header.getTransferId());
            line.setQuantityTransfer(source.quantity);
            line.setCreateBy(operator);
            if (lines.insertWmTransferLine(line) != 1) throw new ServiceException("调拨明细创建失败");
        }
        Document result = new Document();
        result.transferId = header.getTransferId();
        result.transferCode = header.getTransferCode();
        result.status = header.getStatus();
        return result;
    }

    private SortedMap<Long, String> selections(Confirmation request) {
        if (request == null || request.requestId == null || !request.requestId.matches("[a-f0-9-]{36}")
                || request.lines == null || request.lines.isEmpty() || request.lines.size() > 500)
            throw new ServiceException("每次请选择 1 至 500 条调拨明细");
        SortedMap<Long, String> values = new TreeMap<>();
        for (Selection line : request.lines) {
            if (line == null || line.stockId == null || line.version == null || !line.version.matches("[a-f0-9]{64}")
                    || values.put(line.stockId, line.version) != null)
                throw new ServiceException("调拨选择无效，请刷新分析");
        }
        return values;
    }

    private ProWorkorder requireOrder(long id, boolean lock) {
        ProWorkorder order = mapper.order(id, lock);
        if (order == null) throw new ServiceException("工单不存在，请刷新工单列表");
        return order;
    }

    private String tag(long id) { return "WO-SHORTAGE/" + id; }
}
