package com.ktg.miniapp.repo;

import com.ktg.miniapp.common.BizException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 小程序后端数据访问层（JdbcTemplate，演示环境固定 client_id = 1）
 * 询价单复用 mes_inquiry_* 表，其余业务表统一 miniapp_ 前缀。
 */
@Repository
public class MiniAppRepository {

    private static final long CLIENT_ID = 1L;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 允许生成单号的目标表.列（白名单，避免把外部入参拼进 SQL） */
    private static final Set<String> NO_TARGETS = new HashSet<>(Arrays.asList(
            "mes_inquiry_header.inquiry_no",
            "miniapp_order.order_no",
            "miniapp_invoice.invoice_no"));

    private final JdbcTemplate jdbc;

    public MiniAppRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ==================== 产品目录 ====================

    public List<Map<String, Object>> catalogProducts() {
        String sql = "SELECT item_id AS itemId, item_code AS itemCode, item_name AS itemName, "
                + "specification, unit_name AS unitName, unit_of_measure AS unitOfMeasure, "
                + "item_or_product AS itemOrProduct, item_type_name AS itemTypeName, remark, "
                + "attr1, attr2, attr3, attr4 "
                + "FROM md_item WHERE item_code LIKE 'IRG%' AND enable_flag = 'Y' ORDER BY item_code";
        return jdbc.queryForList(sql);
    }

    // ==================== 客户 ====================

    public Map<String, Object> customerProfile() {
        String sql = "SELECT client_id AS clientId, company_name AS company, short_name AS shortName, "
                + "credit_code AS creditCode, level_name AS level, level_desc AS levelDesc, buyer, region, "
                + "sales_name AS salesName, sales_phone AS salesPhone, "
                + "stat_consulting AS statConsulting, stat_pending AS statPending, stat_closed AS statClosed "
                + "FROM miniapp_customer WHERE client_id = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, CLIENT_ID);
        if (rows.isEmpty()) {
            throw new BizException("客户信息不存在");
        }
        Map<String, Object> customer = rows.get(0);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("consulting", customer.remove("statConsulting"));
        stats.put("pending", customer.remove("statPending"));
        stats.put("closed", customer.remove("statClosed"));
        customer.put("stats", stats);
        customer.put("orderCounts", orderCounts());
        customer.put("sales", sales());
        return customer;
    }

    public Map<String, Object> orderCounts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("quoted", 0);
        counts.put("confirmed", 0);
        counts.put("producing", 0);
        counts.put("delivered", 0);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT LOWER(status) AS status, COUNT(*) AS cnt FROM miniapp_order WHERE client_id = ? GROUP BY status",
                CLIENT_ID);
        for (Map<String, Object> row : rows) {
            String status = str(row.get("status"));
            if (status != null && counts.containsKey(status)) {
                counts.put(status, row.get("cnt"));
            }
        }
        return counts;
    }

    public Map<String, Object> sales() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT sales_name AS name, sales_phone AS phone, region FROM miniapp_customer WHERE client_id = ?",
                CLIENT_ID);
        if (rows.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> sales = rows.get(0);
        sales.put("wechat", "ktg-service");
        sales.put("workTime", "周一至周六 08:30 - 18:00");
        sales.put("serviceTel", "400-820-6621");
        return sales;
    }

    // ==================== 收货地址 ====================

    public List<Map<String, Object>> addresses() {
        String sql = "SELECT address_id AS id, contact_name AS contactName, contact_tel AS contactTel, "
                + "region, detail, tag, CASE is_default WHEN 'Y' THEN 1 ELSE 0 END AS isDefault "
                + "FROM miniapp_address WHERE client_id = ? ORDER BY is_default DESC, address_id";
        return jdbc.queryForList(sql, CLIENT_ID);
    }

    @Transactional
    public Map<String, Object> createAddress(Map<String, Object> payload) {
        String contactName = required(payload.get("contactName"), "请填写收货人");
        String contactTel = required(payload.get("contactTel"), "请填写联系电话");
        String region = required(payload.get("region"), "请填写所在地区");
        String detail = required(payload.get("detail"), "请填写详细地址");
        String tag = str(payload.get("tag"));
        boolean makeDefault = bool(payload.get("isDefault"));
        Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM miniapp_address WHERE client_id = ?", Integer.class, CLIENT_ID);
        if (total != null && total == 0) {
            makeDefault = true;
        }
        if (makeDefault) {
            jdbc.update("UPDATE miniapp_address SET is_default = 'N' WHERE client_id = ?", CLIENT_ID);
        }
        jdbc.update("INSERT INTO miniapp_address (client_id, contact_name, contact_tel, region, detail, tag, is_default, create_time, update_time) "
                        + "VALUES (?,?,?,?,?,?,?,NOW(),NOW())",
                CLIENT_ID, contactName, contactTel, region, detail, tag, makeDefault ? "Y" : "N");
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return addressById(id);
    }

    @Transactional
    public Map<String, Object> updateAddress(Long id, Map<String, Object> payload) {
        addressById(id);
        String contactName = required(payload.get("contactName"), "请填写收货人");
        String contactTel = required(payload.get("contactTel"), "请填写联系电话");
        String region = required(payload.get("region"), "请填写所在地区");
        String detail = required(payload.get("detail"), "请填写详细地址");
        String tag = str(payload.get("tag"));
        boolean makeDefault = bool(payload.get("isDefault"));
        if (makeDefault) {
            jdbc.update("UPDATE miniapp_address SET is_default = 'N' WHERE client_id = ?", CLIENT_ID);
        }
        jdbc.update("UPDATE miniapp_address SET contact_name=?, contact_tel=?, region=?, detail=?, tag=?, "
                        + "is_default = CASE WHEN ? = 1 THEN 'Y' ELSE is_default END, update_time=NOW() "
                        + "WHERE address_id=? AND client_id=?",
                contactName, contactTel, region, detail, tag, makeDefault ? 1 : 0, id, CLIENT_ID);
        return addressById(id);
    }

    @Transactional
    public void deleteAddress(Long id) {
        Map<String, Object> address = addressById(id);
        jdbc.update("DELETE FROM miniapp_address WHERE address_id = ? AND client_id = ?", id, CLIENT_ID);
        if ("Y".equalsIgnoreCase(str(address.get("isDefaultRaw"))) || Boolean.TRUE.equals(address.get("isDefault"))) {
            List<Map<String, Object>> rest = addresses();
            if (!rest.isEmpty()) {
                jdbc.update("UPDATE miniapp_address SET is_default = 'Y' WHERE address_id = ?", rest.get(0).get("id"));
            }
        }
    }

    @Transactional
    public Map<String, Object> setDefaultAddress(Long id) {
        addressById(id);
        jdbc.update("UPDATE miniapp_address SET is_default = 'N' WHERE client_id = ?", CLIENT_ID);
        jdbc.update("UPDATE miniapp_address SET is_default = 'Y', update_time = NOW() WHERE address_id = ? AND client_id = ?",
                id, CLIENT_ID);
        return addressById(id);
    }

    private Map<String, Object> addressById(Long id) {
        String sql = "SELECT address_id AS id, contact_name AS contactName, contact_tel AS contactTel, "
                + "region, detail, tag, is_default AS isDefaultRaw, "
                + "CASE is_default WHEN 'Y' THEN 1 ELSE 0 END AS isDefault "
                + "FROM miniapp_address WHERE address_id = ? AND client_id = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id, CLIENT_ID);
        if (rows.isEmpty()) {
            throw new BizException("收货地址不存在");
        }
        return rows.get(0);
    }

    // ==================== 询价单 ====================

    public List<Map<String, Object>> inquiries(String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT inquiry_id AS inquiryId, inquiry_no AS inquiryNo, client_id AS clientId, "
                        + "client_name AS clientName, buyer, contact_tel AS contactTel, LOWER(status) AS status, "
                        + "DATE_FORMAT(expected_date,'%Y-%m-%d') AS expectedDate, "
                        + "delivery_address AS deliveryAddress, invoice_type AS invoiceType, remark, "
                        + "item_count AS itemCount, total_qty AS totalQty, quote_amount AS quoteAmount, "
                        + "DATE_FORMAT(quote_time,'%Y-%m-%d %H:%i') AS quoteTime, "
                        + "sales_name AS salesName, sales_tel AS salesTel, "
                        + "DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') AS createTime "
                        + "FROM mes_inquiry_header WHERE (client_id = ? OR client_id IS NULL) ");
        List<Object> args = new ArrayList<>();
        args.add(CLIENT_ID);
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND status = ? ");
            args.add(status.trim().toUpperCase());
        }
        sql.append("ORDER BY create_time DESC, inquiry_id DESC");
        List<Map<String, Object>> headers = jdbc.queryForList(sql.toString(), args.toArray());
        attachInquiryLines(headers);
        return headers;
    }

    public Map<String, Object> inquiry(String inquiryNo) {
        String sql = "SELECT inquiry_id AS inquiryId, inquiry_no AS inquiryNo, client_id AS clientId, "
                + "client_name AS clientName, buyer, contact_tel AS contactTel, LOWER(status) AS status, "
                + "DATE_FORMAT(expected_date,'%Y-%m-%d') AS expectedDate, "
                + "delivery_address AS deliveryAddress, invoice_type AS invoiceType, remark, "
                + "item_count AS itemCount, total_qty AS totalQty, quote_amount AS quoteAmount, "
                + "DATE_FORMAT(quote_time,'%Y-%m-%d %H:%i') AS quoteTime, "
                + "sales_name AS salesName, sales_tel AS salesTel, "
                + "DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') AS createTime "
                + "FROM mes_inquiry_header WHERE inquiry_no = ? AND (client_id = ? OR client_id IS NULL)";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, inquiryNo, CLIENT_ID);
        if (rows.isEmpty()) {
            throw new BizException("询价单不存在");
        }
        List<Map<String, Object>> headers = new ArrayList<>();
        headers.add(rows.get(0));
        attachInquiryLines(headers);
        return headers.get(0);
    }

    /**
     * 撤销询价单：把状态置为 CANCELED（不删除记录，保留可追溯性）
     * 已报价 / 已确认 / 已下单的询价单不允许撤销
     */
    @Transactional
    public Map<String, Object> cancelInquiry(String inquiryNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT inquiry_id AS inquiryId, UPPER(status) AS status FROM mes_inquiry_header "
                        + "WHERE inquiry_no = ? AND (client_id = ? OR client_id IS NULL)",
                inquiryNo, CLIENT_ID);
        if (rows.isEmpty()) {
            throw new BizException("询价单不存在");
        }
        Map<String, Object> row = rows.get(0);
        String status = str(row.get("status"));
        if ("CANCELED".equalsIgnoreCase(status)) {
            return inquiry(inquiryNo);
        }
        if ("QUOTED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status)
                || "ORDERED".equalsIgnoreCase(status)) {
            throw new BizException("已报价或已下单的询价单不可撤销");
        }
        jdbc.update("UPDATE mes_inquiry_header SET status = 'CANCELED', update_time = NOW() WHERE inquiry_id = ?",
                toLong(row.get("inquiryId")));
        return inquiry(inquiryNo);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> createInquiry(Map<String, Object> payload) {
        Object rawLines = payload.get("lines");
        if (!(rawLines instanceof List) || ((List<?>) rawLines).isEmpty()) {
            throw new BizException("询价单至少需要一条产品明细");
        }
        List<Map<String, Object>> lines = (List<Map<String, Object>>) rawLines;
        String buyer = str(payload.get("buyer"));
        String clientName = str(payload.get("clientName"));
        if (clientName == null) {
            clientName = str(customerCompany());
        }
        String inquiryNo = nextNo("mes_inquiry_header", "inquiry_no", "XJ" + LocalDate.now().format(DAY) + "-");
        int itemCount = lines.size();
        double totalQty = 0;
        for (Map<String, Object> line : lines) {
            totalQty += toDouble(line.get("qty"));
        }
        jdbc.update("INSERT INTO mes_inquiry_header (inquiry_no, client_id, client_name, buyer, contact_tel, status, "
                        + "expected_date, delivery_address, invoice_type, remark, item_count, total_qty, create_by, create_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                inquiryNo, CLIENT_ID, clientName, buyer, str(payload.get("contactTel")), "SUBMITTED",
                dateOrNull(payload.get("expectedDate")), str(payload.get("deliveryAddress")),
                str(payload.get("invoiceType")), str(payload.get("remark")), itemCount, totalQty, "miniapp");

        Long inquiryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        int lineNo = 1;
        for (Map<String, Object> line : lines) {
            jdbc.update("INSERT INTO mes_inquiry_line (inquiry_id, line_no, item_id, item_code, item_name, specification, "
                            + "item_or_product, qty, unit_name, moq, lead_days, create_by, create_time) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    inquiryId, lineNo++, toLong(line.get("itemId")), str(line.get("itemCode")), str(line.get("itemName")),
                    str(line.get("specification")), str(line.get("itemOrProduct")), toDouble(line.get("qty")),
                    str(line.get("unitName")), toDouble(line.get("moq")), toInteger(line.get("leadDays")), "miniapp");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inquiryId", inquiryId);
        result.put("inquiryNo", inquiryNo);
        result.put("status", "submitted");
        result.put("itemCount", itemCount);
        result.put("totalQty", totalQty);
        return result;
    }

    private void attachInquiryLines(List<Map<String, Object>> headers) {
        if (headers.isEmpty()) {
            return;
        }
        String sql = "SELECT l.line_id AS lineId, l.inquiry_id AS inquiryId, l.line_no AS lineNo, l.item_id AS itemId, "
                + "l.item_code AS itemCode, l.item_name AS itemName, l.specification, l.item_or_product AS itemOrProduct, "
                + "l.qty, l.unit_name AS unitName, l.moq, l.lead_days AS leadDays, l.quoted_price AS quotedPrice, "
                + "DATE_FORMAT(l.delivery_date,'%Y-%m-%d') AS deliveryDate, l.line_amount AS lineAmount, "
                + "m.attr2 AS art "
                + "FROM mes_inquiry_line l LEFT JOIN md_item m ON m.item_code = l.item_code "
                + "ORDER BY l.inquiry_id, l.line_no";
        Map<Long, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> line : jdbc.queryForList(sql)) {
            Long inquiryId = toLong(line.get("inquiryId"));
            grouped.computeIfAbsent(inquiryId, k -> new ArrayList<>()).add(line);
        }
        for (Map<String, Object> header : headers) {
            Long inquiryId = toLong(header.get("inquiryId"));
            header.put("lines", grouped.getOrDefault(inquiryId, new ArrayList<>()));
            Object amount = header.get("quoteAmount");
            boolean quoted = "quoted".equalsIgnoreCase(str(header.get("status"))) || "confirmed".equalsIgnoreCase(str(header.get("status")));
            header.put("quoted", quoted || (amount != null && toDouble(amount) > 0));
        }
    }

    // ==================== 订单 ====================

    public List<Map<String, Object>> orders(String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT order_id AS orderId, order_no AS orderNo, inquiry_no AS inquiryNo, LOWER(status) AS status, "
                        + "status_name AS statusName, item_count AS itemCount, total_qty AS totalQty, "
                        + "total_amount AS totalAmount, delivery_address AS deliveryAddress, "
                        + "invoice_type AS invoiceType, DATE_FORMAT(expected_date,'%Y-%m-%d') AS expectedDate, "
                        + "remark, DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') AS createTime, "
                        + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i') AS updateTime "
                        + "FROM miniapp_order WHERE client_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(CLIENT_ID);
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND status = ? ");
            args.add(status.trim().toUpperCase());
        }
        sql.append("ORDER BY create_time DESC, order_id DESC");
        List<Map<String, Object>> list = jdbc.queryForList(sql.toString(), args.toArray());
        attachOrderLines(list);
        return list;
    }

    public Map<String, Object> order(String orderNo) {
        String sql = "SELECT order_id AS orderId, order_no AS orderNo, inquiry_no AS inquiryNo, LOWER(status) AS status, "
                + "status_name AS statusName, item_count AS itemCount, total_qty AS totalQty, "
                + "total_amount AS totalAmount, delivery_address AS deliveryAddress, invoice_type AS invoiceType, "
                + "DATE_FORMAT(expected_date,'%Y-%m-%d') AS expectedDate, remark, "
                + "DATE_FORMAT(create_time,'%Y-%m-%d %H:%i') AS createTime, "
                + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i') AS updateTime "
                + "FROM miniapp_order WHERE order_no = ? AND client_id = ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orderNo, CLIENT_ID);
        if (rows.isEmpty()) {
            throw new BizException("订单不存在");
        }
        attachOrderLines(rows);
        Map<String, Object> order = rows.get(0);
        order.put("delivery", deliveryByOrder(orderNo));
        return order;
    }

    private void attachOrderLines(List<Map<String, Object>> orders) {
        if (orders.isEmpty()) {
            return;
        }
        String sql = "SELECT line_id AS lineId, order_id AS orderId, line_no AS lineNo, item_code AS itemCode, "
                + "item_name AS itemName, specification, qty, unit_name AS unitName, unit_price AS unitPrice, "
                + "amount, art FROM miniapp_order_line ORDER BY order_id, line_no";
        Map<Long, List<Map<String, Object>>> grouped = new HashMap<>();
        for (Map<String, Object> line : jdbc.queryForList(sql)) {
            grouped.computeIfAbsent(toLong(line.get("orderId")), k -> new ArrayList<>()).add(line);
        }
        for (Map<String, Object> order : orders) {
            order.put("lines", grouped.getOrDefault(toLong(order.get("orderId")), new ArrayList<>()));
        }
    }

    private Map<String, Object> deliveryByOrder(String orderNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT delivery_no AS deliveryNo, order_no AS orderNo, LOWER(status) AS status, "
                        + "status_name AS statusName, total_qty AS totalQty, shipped_qty AS shippedQty, "
                        + "DATE_FORMAT(plan_date,'%Y-%m-%d') AS planDate, DATE_FORMAT(ship_date,'%Y-%m-%d') AS shipDate, "
                        + "carrier, tracking_no AS trackingNo, address, progress, "
                        + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i') AS updateTime "
                        + "FROM miniapp_delivery WHERE order_no = ? AND client_id = ?",
                orderNo, CLIENT_ID);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 询价单转订单（幂等：同一询价单只生成一张订单） */
    @Transactional
    public Map<String, Object> createOrderFromInquiry(String inquiryNo) {
        String inquirySql = "SELECT inquiry_id AS inquiryId, inquiry_no AS inquiryNo, LOWER(status) AS status, "
                + "item_count AS itemCount, total_qty AS totalQty, quote_amount AS quoteAmount, "
                + "delivery_address AS deliveryAddress, invoice_type AS invoiceType, remark, "
                + "DATE_FORMAT(expected_date,'%Y-%m-%d') AS expectedDate, sales_name AS salesName, sales_tel AS salesTel "
                + "FROM mes_inquiry_header WHERE inquiry_no = ?";
        List<Map<String, Object>> inquiryRows = jdbc.queryForList(inquirySql, inquiryNo);
        if (inquiryRows.isEmpty()) {
            throw new BizException("询价单不存在");
        }
        Map<String, Object> inquiry = inquiryRows.get(0);
        if ("canceled".equalsIgnoreCase(str(inquiry.get("status")))) {
            throw new BizException("询价单已取消，无法下单");
        }
        List<Map<String, Object>> exists = jdbc.queryForList(
                "SELECT order_no AS orderNo FROM miniapp_order WHERE inquiry_no = ? AND client_id = ?",
                inquiryNo, CLIENT_ID);
        if (!exists.isEmpty()) {
            return order(str(exists.get(0).get("orderNo")));
        }

        Long inquiryId = toLong(inquiry.get("inquiryId"));
        List<Map<String, Object>> lines = jdbc.queryForList(
                "SELECT l.line_no AS lineNo, l.item_code AS itemCode, l.item_name AS itemName, l.specification, "
                        + "l.qty, l.unit_name AS unitName, COALESCE(l.quoted_price, 0) AS unitPrice, "
                        + "COALESCE(l.line_amount, 0) AS amount, m.attr2 AS art "
                        + "FROM mes_inquiry_line l LEFT JOIN md_item m ON m.item_code = l.item_code "
                        + "WHERE l.inquiry_id = ? ORDER BY l.line_no", inquiryId);
        if (lines.isEmpty()) {
            throw new BizException("询价单没有明细，无法下单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        double totalQty = 0;
        for (Map<String, Object> line : lines) {
            totalAmount = totalAmount.add(toDecimal(line.get("amount")));
            totalQty += toDouble(line.get("qty"));
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0 && inquiry.get("quoteAmount") != null) {
            totalAmount = toDecimal(inquiry.get("quoteAmount"));
        }

        String orderNo = nextNo("miniapp_order", "order_no", "SO" + LocalDate.now().format(DAY) + "-");
        jdbc.update("INSERT INTO miniapp_order (order_no, client_id, inquiry_no, status, status_name, item_count, "
                        + "total_qty, total_amount, delivery_address, invoice_type, expected_date, remark, create_time, update_time) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                orderNo, CLIENT_ID, inquiryNo, "CONFIRMED", "待确认", lines.size(), totalQty, totalAmount,
                str(inquiry.get("deliveryAddress")), str(inquiry.get("invoiceType")),
                dateOrNull(inquiry.get("expectedDate")), str(inquiry.get("remark")));
        Long orderId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        int lineNo = 1;
        for (Map<String, Object> line : lines) {
            jdbc.update("INSERT INTO miniapp_order_line (order_id, line_no, item_code, item_name, specification, qty, "
                            + "unit_name, unit_price, amount, art, create_time) VALUES (?,?,?,?,?,?,?,?,?,?,NOW())",
                    orderId, lineNo++, str(line.get("itemCode")), str(line.get("itemName")), str(line.get("specification")),
                    toDouble(line.get("qty")), str(line.get("unitName")), toDecimal(line.get("unitPrice")),
                    toDecimal(line.get("amount")), str(line.get("art")));
        }
        jdbc.update("UPDATE mes_inquiry_header SET status = 'CONFIRMED', update_time = NOW() WHERE inquiry_id = ?", inquiryId);
        return order(orderNo);
    }

    // ==================== 交付 / 对账 / 发票 / 联系人 / FAQ ====================

    public List<Map<String, Object>> deliveries(String orderNo) {
        StringBuilder sql = new StringBuilder(
                "SELECT delivery_id AS id, delivery_no AS deliveryNo, order_no AS orderNo, LOWER(status) AS status, "
                        + "status_name AS statusName, total_qty AS totalQty, shipped_qty AS shippedQty, "
                        + "DATE_FORMAT(plan_date,'%Y-%m-%d') AS planDate, DATE_FORMAT(ship_date,'%Y-%m-%d') AS shipDate, "
                        + "carrier, tracking_no AS trackingNo, address, progress, remark, "
                        + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i') AS updateTime "
                        + "FROM miniapp_delivery WHERE client_id = ? ");
        List<Object> args = new ArrayList<>();
        args.add(CLIENT_ID);
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            sql.append("AND order_no = ? ");
            args.add(orderNo.trim());
        }
        sql.append("ORDER BY status DESC, plan_date ASC");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    public List<Map<String, Object>> statements() {
        return jdbc.queryForList(
                "SELECT statement_id AS id, statement_no AS statementNo, period, "
                        + "total_amount AS totalAmount, paid_amount AS paidAmount, unpaid_amount AS unpaidAmount, "
                        + "LOWER(status) AS status, status_name AS statusName, "
                        + "DATE_FORMAT(bill_date,'%Y-%m-%d') AS billDate, DATE_FORMAT(due_date,'%Y-%m-%d') AS dueDate, remark "
                        + "FROM miniapp_statement WHERE client_id = ? ORDER BY bill_date DESC", CLIENT_ID);
    }

    public List<Map<String, Object>> invoices() {
        return jdbc.queryForList(
                "SELECT invoice_id AS id, invoice_no AS invoiceNo, invoice_title AS invoiceTitle, tax_no AS taxNo, "
                        + "invoice_type AS invoiceType, amount, LOWER(status) AS status, status_name AS statusName, "
                        + "DATE_FORMAT(apply_time,'%Y-%m-%d %H:%i') AS applyTime, "
                        + "DATE_FORMAT(issue_time,'%Y-%m-%d') AS issueTime, email, remark "
                        + "FROM miniapp_invoice WHERE client_id = ? ORDER BY apply_time DESC", CLIENT_ID);
    }

    @Transactional
    public Map<String, Object> createInvoice(Map<String, Object> payload) {
        BigDecimal amount = toDecimal(payload.get("amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("开票金额必须大于 0");
        }
        String title = str(payload.get("invoiceTitle"));
        if (title == null) {
            title = customerCompany();
        }
        String taxNo = str(payload.get("taxNo"));
        if (taxNo == null) {
            taxNo = customerTaxNo();
        }
        String invoiceType = str(payload.get("invoiceType"));
        if (invoiceType == null) {
            invoiceType = "增值税专用发票 13%";
        }
        String invoiceNo = nextNo("miniapp_invoice", "invoice_no", "FP" + LocalDate.now().format(DAY) + "-");
        jdbc.update("INSERT INTO miniapp_invoice (invoice_no, client_id, invoice_title, tax_no, invoice_type, amount, "
                        + "status, status_name, apply_time, email, remark) VALUES (?,?,?,?,?,?,?,?,NOW(),?,?)",
                invoiceNo, CLIENT_ID, title, taxNo, invoiceType, amount, "APPLIED", "开票中",
                str(payload.get("email")), str(payload.get("remark")));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT invoice_id AS id, invoice_no AS invoiceNo, invoice_title AS invoiceTitle, tax_no AS taxNo, "
                        + "invoice_type AS invoiceType, amount, LOWER(status) AS status, status_name AS statusName, "
                        + "DATE_FORMAT(apply_time,'%Y-%m-%d %H:%i') AS applyTime, email, remark "
                        + "FROM miniapp_invoice WHERE invoice_id = ?", id);
        return rows.get(0);
    }

    public List<Map<String, Object>> contacts() {
        return jdbc.queryForList(
                "SELECT contact_id AS id, name, dept, tel, email, "
                        + "CASE is_default WHEN 'Y' THEN 1 ELSE 0 END AS isDefault "
                        + "FROM miniapp_contact WHERE client_id = ? ORDER BY is_default DESC, contact_id", CLIENT_ID);
    }

    public List<Map<String, Object>> faqs() {
        return jdbc.queryForList(
                "SELECT faq_id AS id, category, question, answer FROM miniapp_faq ORDER BY sort_no, faq_id");
    }

    public Map<String, Object> invoiceProfile() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("invoiceTitle", customerCompany());
        profile.put("taxNo", customerTaxNo());
        profile.put("invoiceType", "增值税专用发票 13%");
        profile.put("email", firstValue("SELECT email FROM miniapp_invoice WHERE client_id = ? ORDER BY apply_time DESC LIMIT 1"));
        return profile;
    }

    // ==================== 工具方法 ====================

    private String customerCompany() {
        return firstValue("SELECT company_name FROM miniapp_customer WHERE client_id = ? LIMIT 1");
    }

    private String customerTaxNo() {
        return firstValue("SELECT credit_code FROM miniapp_customer WHERE client_id = ? LIMIT 1");
    }

    private String firstValue(String sql) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, CLIENT_ID);
        if (rows.isEmpty()) {
            return null;
        }
        Object value = rows.get(0).values().iterator().next();
        return value == null ? null : value.toString();
    }

    private String nextNo(String table, String column, String prefix) {
        if (!NO_TARGETS.contains(table + "." + column)) {
            throw new BizException("不支持的单号类型：" + table + "." + column);
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " LIKE ?", Integer.class, prefix + "%");
        int seq = (count == null ? 0 : count) + 1;
        String no = prefix + String.format("%03d", seq);
        while (true) {
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, no);
            if (exists == null || exists == 0) {
                return no;
            }
            seq++;
            no = prefix + String.format("%03d", seq);
        }
    }

    private static String required(Object value, String message) {
        String text = str(value);
        if (text == null) {
            throw new BizException(message);
        }
        return text;
    }

    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static double toDouble(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0d;
        }
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInteger(Object value) {
        Long num = toLong(value);
        return num == null ? null : num.intValue();
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return value != null && ("true".equalsIgnoreCase(value.toString()) || "1".equals(value.toString()));
    }

    private static Object dateOrNull(Object value) {
        String text = str(value);
        return text == null ? null : text;
    }
}
