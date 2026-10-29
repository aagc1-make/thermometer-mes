package com.ktg.mes;

import com.ktg.common.exception.ServiceException;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.domain.tx.ItemRecptTxBean;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.service.*;
import com.ktg.mes.wm.service.impl.ItemRecptWorkflowService;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 采购入库：同一到货通知含「合格行 + 全 NG 行」时，合格行必须仍可过账（回归用例）。
 * 不连接数据库，只替换持久化边界。
 */
public class MixedNgReceiptTest {

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static WmArrivalNoticeLine arrivalLine(Long lineId, Long itemId, String arrival, String qualified, Long iqcId) {
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setLineId(lineId);
        line.setNoticeId(1L);
        line.setItemId(itemId);
        line.setItemName("物料" + itemId);
        line.setIqcCheck("Y");
        line.setQuantityArrival(new BigDecimal(arrival));
        line.setQuantityQuanlified(new BigDecimal(qualified));
        line.setIqcId(iqcId);
        return line;
    }

    private static WmItemRecptLine receiptLine(Long lineId, Long noticeLineId, Long itemId, String quantity) {
        WmItemRecptLine line = new WmItemRecptLine();
        line.setLineId(lineId);
        line.setRecptId(7L);
        line.setNoticeLineId(noticeLineId);
        line.setItemId(itemId);
        line.setIqcCheck("Y");
        line.setQuantityRecived(new BigDecimal(quantity));
        return line;
    }

    @Test
    public void qualifiedLinePostsWhileNgLineIsIgnored() {
        AtomicInteger postings = new AtomicInteger();
        PostingLockMapper lock = fake(PostingLockMapper.class, (p, m, a) -> "APPROVED");
        IWmItemRecptService receipts = fake(IWmItemRecptService.class, (p, m, a) -> {
            if ("selectWmItemRecptByRecptId".equals(m.getName())) {
                WmItemRecpt recpt = new WmItemRecpt();
                recpt.setRecptId(7L);
                recpt.setNoticeId(1L);
                recpt.setStatus("APPROVED");
                return recpt;
            }
            // 本单只有合格行（来源于合格 8 的行）
            return Collections.singletonList(receiptLine(1L, 11L, 101L, "8"));
        });
        IWmArrivalNoticeService notices = fake(IWmArrivalNoticeService.class, (p, m, a) -> {
            if ("selectWmArrivalNoticeByNoticeId".equals(m.getName())) {
                WmArrivalNotice notice = new WmArrivalNotice();
                notice.setNoticeId(1L);
                return notice;
            }
            if (m.getReturnType() == int.class) {
                return 1;
            }
            return null;
        });
        // 通知里同时存在合格行(11)与全 NG 行(12)
        IWmArrivalNoticeLineService arrivalLines = fake(IWmArrivalNoticeLineService.class, (p, m, a) ->
                Arrays.asList(arrivalLine(11L, 101L, "10", "8", 55L), arrivalLine(12L, 102L, "5", "0", 56L)));
        ReceiptGuardMapper guard = fake(ReceiptGuardMapper.class, (p, m, a) -> {
            if ("countCompletedIqcBoundToArrivalLine".equals(m.getName())) {
                return 1;
            }
            if ("transitionReceiptStatus".equals(m.getName())) {
                return 1;
            }
            return BigDecimal.ZERO;
        });
        IWmItemRecptDetailService details = fake(IWmItemRecptDetailService.class, (p, m, a) -> "E");
        IStorageCoreService storage = fake(IStorageCoreService.class, (p, m, a) -> {
            postings.incrementAndGet();
            return null;
        });

        IWmItemRecptService receiptService = fake(IWmItemRecptService.class, (p, m, a) -> {
            if ("selectWmItemRecptByRecptId".equals(m.getName())) {
                WmItemRecpt recpt = new WmItemRecpt();
                recpt.setRecptId(7L);
                recpt.setNoticeId(1L);
                recpt.setStatus("APPROVED");
                return recpt;
            }
            if ("getTxBeans".equals(m.getName())) {
                ItemRecptTxBean bean = new ItemRecptTxBean();
                bean.setSourceDocLineId(1L);
                bean.setTransactionQuantity(new BigDecimal("8"));
                return Collections.singletonList(bean);
            }
            return Collections.singletonList(receiptLine(1L, 11L, 101L, "8"));
        });

        IWmItemRecptLineService lineService = fake(IWmItemRecptLineService.class, (p, m, a) ->
                Collections.singletonList(receiptLine(1L, 11L, 101L, "8")));

        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, guard, receiptService,
                lineService, details, notices, arrivalLines, storage, new FormLinkagePolicy(), null, null,
                new com.ktg.mes.linkage.IssueDraftAfterReceiptService(null) {
                    @Override
                    public void prepareAfterReceipt(Long receiptId, String operator) {
                        // 单测不触达领料草稿
                    }
                });
        try {
            service.execute(7L, "warehouse");
        } catch (ServiceException e) {
            fail("合格行应可入库，但被拒绝：" + e.getMessage());
        }
        assertEquals("合格行应恰好过账一次", 1, postings.get());
        assertTrue(true);
    }
}
