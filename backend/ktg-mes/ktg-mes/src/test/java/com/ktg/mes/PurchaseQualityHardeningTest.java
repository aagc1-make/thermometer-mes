package com.ktg.mes;

import com.ktg.common.constant.UserConstants;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.linkage.FormLinkagePolicy;
import com.ktg.mes.linkage.PurchaseReceiptLinkageService;
import com.ktg.mes.qc.domain.QcIqc;
import com.ktg.mes.qc.service.IQcIqcService;
import com.ktg.mes.qc.service.impl.QcIqcCompletionService;
import com.ktg.mes.wm.domain.WmArrivalNotice;
import com.ktg.mes.wm.domain.WmArrivalNoticeLine;
import com.ktg.mes.wm.domain.WmItemRecpt;
import com.ktg.mes.wm.domain.WmItemRecptLine;
import com.ktg.mes.wm.domain.tx.ItemRecptTxBean;
import com.ktg.mes.wm.mapper.PostingLockMapper;
import com.ktg.mes.wm.mapper.ReceiptGuardMapper;
import com.ktg.mes.wm.service.IStorageCoreService;
import com.ktg.mes.wm.service.IWmArrivalNoticeLineService;
import com.ktg.mes.wm.service.IWmArrivalNoticeService;
import com.ktg.mes.wm.service.IWmBatchService;
import com.ktg.mes.wm.service.IWmItemRecptDetailService;
import com.ktg.mes.wm.service.IWmItemRecptLineService;
import com.ktg.mes.wm.service.IWmItemRecptService;
import com.ktg.mes.wm.service.IWmOutsourceRecptLineService;
import com.ktg.mes.wm.service.IWmOutsourceRecptService;
import com.ktg.mes.wm.service.impl.ItemRecptWorkflowService;
import com.ktg.system.strategy.AutoCodeUtil;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 采购质量链的阻断项回归（不连接数据库）。
 *
 * <p>执行真实 Service/Controller 业务编排，只替换持久化边界，
 * 用于证明：被拒绝的请求不会发生库存过账、不会重复建单，
 * 且来源/数量/检验结论的校验以数据库记录为准。</p>
 */
public class PurchaseQualityHardeningTest {

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static WmItemRecpt receipt(Long recptId, Long noticeId, String status) {
        WmItemRecpt recpt = new WmItemRecpt();
        recpt.setRecptId(recptId);
        recpt.setNoticeId(noticeId);
        recpt.setStatus(status);
        recpt.setRecptCode("IT-RECPT");
        return recpt;
    }

    private static WmItemRecptLine receiptLine(Long lineId, Long noticeLineId, Long itemId, String quantity, String iqcCheck) {
        WmItemRecptLine line = new WmItemRecptLine();
        line.setLineId(lineId);
        line.setRecptId(7L);
        line.setNoticeLineId(noticeLineId);
        line.setItemId(itemId);
        line.setItemName("PCBA");
        line.setIqcCheck(iqcCheck);
        line.setQuantityRecived(new BigDecimal(quantity));
        return line;
    }

    private static WmArrivalNoticeLine arrivalLine(Long lineId, Long noticeId, Long itemId,
                                                   String arrival, String qualified, Long iqcId) {
        WmArrivalNoticeLine line = new WmArrivalNoticeLine();
        line.setLineId(lineId);
        line.setNoticeId(noticeId);
        line.setItemId(itemId);
        line.setIqcCheck(UserConstants.YES);
        line.setQuantityArrival(new BigDecimal(arrival));
        line.setQuantityQuanlified(qualified == null ? null : new BigDecimal(qualified));
        line.setIqcId(iqcId);
        return line;
    }

    private static void expectRejected(Runnable action, String messagePart) {
        try {
            action.run();
            fail("应当被拒绝，但请求成功执行");
        } catch (ServiceException expected) {
            assertTrue("错误信息应包含[" + messagePart + "]，实际: " + expected.getMessage(),
                    expected.getMessage() != null && expected.getMessage().contains(messagePart));
        } catch (RuntimeException other) {
            throw other;
        }
    }

    /** 已执行入库单不能被“确认”接口改回可编辑状态。 */
    @Test
    public void completedReceiptCannotBeConfirmedAgain() {
        AtomicInteger transitions = new AtomicInteger();
        PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) -> "FINISHED");
        ReceiptGuardMapper guard = fake(ReceiptGuardMapper.class, (proxy, method, args) -> {
            transitions.incrementAndGet();
            return 1;
        });
        IWmItemRecptLineService lines = fake(IWmItemRecptLineService.class,
                (proxy, method, args) -> Collections.singletonList(receiptLine(1L, null, 101L, "5", "N")));
        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, guard, null, lines,
                null, null, null, null, new FormLinkagePolicy(), null, null, null);

        expectRejected(() -> service.confirm(7L, "warehouse"), "只有草稿状态");
        assertEquals("已执行单据不应发生状态流转", 0, transitions.get());
    }

    /** 已取消/已完成单据不能通过普通编辑回到待执行状态。 */
    @Test
    public void finishedReceiptCannotReturnToExecutableThroughEdit() {
        PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) -> "APPROVED");
        IWmItemRecptLineService lines = fake(IWmItemRecptLineService.class,
                (proxy, method, args) -> Collections.singletonList(receiptLine(1L, null, 101L, "5", "N")));
        IWmItemRecptService receipts = fake(IWmItemRecptService.class,
                (proxy, method, args) -> receipt(7L, 1L, "APPROVED"));
        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, null, receipts, lines,
                null, null, null, null, new FormLinkagePolicy(), null, null, null);

        WmItemRecpt request = receipt(7L, 1L, UserConstants.ORDER_STATUS_PREPARE);
        expectRejected(() -> service.edit(request, "warehouse", false), "不允许把采购入库单从 APPROVED 改为 PREPARE");
    }

    /** 入库数量超过来源行可入库合格量时，不得过账。 */
    @Test
    public void overReceiptBeyondQualifiedQuantityIsBlocked() {
        AtomicInteger postings = new AtomicInteger();
        PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) -> "APPROVED");
        IWmItemRecptService receipts = fake(IWmItemRecptService.class, (proxy, method, args) -> {
            if ("selectWmItemRecptByRecptId".equals(method.getName())) {
                return receipt(7L, 1L, "APPROVED");
            }
            return null;
        });
        IWmItemRecptLineService lines = fake(IWmItemRecptLineService.class, (proxy, method, args) ->
                Collections.singletonList(receiptLine(1L, 11L, 101L, "9", "Y")));
        IWmArrivalNoticeService notices = fake(IWmArrivalNoticeService.class, (proxy, method, args) -> {
            if ("selectWmArrivalNoticeByNoticeId".equals(method.getName())) {
                WmArrivalNotice notice = new WmArrivalNotice();
                notice.setNoticeId(1L);
                return notice;
            }
            return null;
        });
        IWmArrivalNoticeLineService arrivalLines = fake(IWmArrivalNoticeLineService.class, (proxy, method, args) ->
                Collections.singletonList(arrivalLine(11L, 1L, 101L, "10", "8", 55L)));
        ReceiptGuardMapper guard = fake(ReceiptGuardMapper.class, (proxy, method, args) -> {
            if ("countCompletedIqcBoundToArrivalLine".equals(method.getName())) {
                return 1;
            }
            return BigDecimal.ZERO;
        });
        IStorageCoreService storage = fake(IStorageCoreService.class, (proxy, method, args) -> {
            postings.incrementAndGet();
            return null;
        });
        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, guard, receipts, lines,
                null, notices, arrivalLines, storage, new FormLinkagePolicy(), null, null, null);

        expectRejected(() -> service.execute(7L, "warehouse"), "入库数量超过来源可入库数量");
        assertEquals("超量请求不得产生库存过账", 0, postings.get());
    }

    /** 入库行绑定的 IQC 与来源行不一致（伪造/跨单）时必须阻断。 */
    @Test
    public void mismatchedIqcBindingIsBlocked() {
        AtomicInteger postings = new AtomicInteger();
        PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) -> "APPROVED");
        IWmItemRecptService receipts = fake(IWmItemRecptService.class, (proxy, method, args) -> {
            if ("selectWmItemRecptByRecptId".equals(method.getName())) {
                return receipt(7L, 1L, "APPROVED");
            }
            return null;
        });
        WmItemRecptLine line = receiptLine(1L, 11L, 101L, "5", "Y");
        line.setIqcId(99L);
        IWmItemRecptLineService lines = fake(IWmItemRecptLineService.class,
                (proxy, method, args) -> Collections.singletonList(line));
        IWmArrivalNoticeService notices = fake(IWmArrivalNoticeService.class, (proxy, method, args) -> {
            if ("selectWmArrivalNoticeByNoticeId".equals(method.getName())) {
                WmArrivalNotice notice = new WmArrivalNotice();
                notice.setNoticeId(1L);
                return notice;
            }
            return null;
        });
        IWmArrivalNoticeLineService arrivalLines = fake(IWmArrivalNoticeLineService.class, (proxy, method, args) ->
                Collections.singletonList(arrivalLine(11L, 1L, 101L, "10", "8", 55L)));
        ReceiptGuardMapper guard = fake(ReceiptGuardMapper.class, (proxy, method, args) -> 1);
        IStorageCoreService storage = fake(IStorageCoreService.class, (proxy, method, args) -> {
            postings.incrementAndGet();
            return null;
        });
        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, guard, receipts, lines,
                null, notices, arrivalLines, storage, new FormLinkagePolicy(), null, null, null);

        expectRejected(() -> service.execute(7L, "warehouse"), "检验单与来源到货行不一致");
        assertEquals(0, postings.get());
    }

    /** 重复执行（第二次请求）不得再次过账。 */
    @Test
    public void repeatedExecutionDoesNotPostTwice() {
        AtomicInteger postings = new AtomicInteger();
        PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) -> "FINISHED");
        IStorageCoreService storage = fake(IStorageCoreService.class, (proxy, method, args) -> {
            postings.incrementAndGet();
            return null;
        });
        ItemRecptWorkflowService service = new ItemRecptWorkflowService(lock, null, null, null,
                null, null, null, storage, new FormLinkagePolicy(), null, null, null);

        expectRejected(() -> service.execute(7L, "warehouse"), "已执行或尚未到可执行状态");
        assertEquals(0, postings.get());
    }

    /** 全 NG 时不得生成没有可入库行的空采购入库草稿。 */
    @Test
    public void allNgDoesNotCreateEmptyReceiptDraft() {
        AtomicInteger headerInserts = new AtomicInteger();
        IWmItemRecptService receipts = fake(IWmItemRecptService.class, (proxy, method, args) -> {
            if ("selectWmItemRecptList".equals(method.getName())) {
                return Collections.emptyList();
            }
            headerInserts.incrementAndGet();
            return null;
        });
        IWmItemRecptLineService receiptLines = fake(IWmItemRecptLineService.class, (proxy, method, args) -> 1);
        WmArrivalNotice notice = new WmArrivalNotice();
        notice.setNoticeId(1L);
        notice.setNoticeCode("IT-AN-001");

        PurchaseReceiptLinkageService service = new PurchaseReceiptLinkageService(receipts, receiptLines, null,
                null, null, null);
        WmArrivalNoticeLine allNg = arrivalLine(11L, 1L, 101L, "10", "0", 55L);
        assertNull("全 NG 不应建单", service.createDraftWhenReady(notice, Collections.singletonList(allNg), "inspector"));
        assertEquals("全 NG 不应 insert 入库单头", 0, headerInserts.get());
    }

    /** 混合结果：只把合格行写入草稿，NG 行不入库，且只生成一张草稿。 */
    @Test
    public void mixedResultOnlyWritesQualifiedLines() {
        AtomicInteger headerInserts = new AtomicInteger();
        List<WmItemRecptLine> insertedLines = new ArrayList<>();
        IWmItemRecptService receipts = fake(IWmItemRecptService.class, (proxy, method, args) -> {
            if ("selectWmItemRecptList".equals(method.getName())) {
                return Collections.emptyList();
            }
            if ("insertWmItemRecpt".equals(method.getName())) {
                headerInserts.incrementAndGet();
                WmItemRecpt recpt = (WmItemRecpt) args[0];
                recpt.setRecptId(88L);
                return 1;
            }
            return null;
        });
        IWmItemRecptLineService receiptLines = fake(IWmItemRecptLineService.class, (proxy, method, args) -> {
            insertedLines.add((WmItemRecptLine) args[0]);
            return 1;
        });
        IWmBatchService batch = fake(IWmBatchService.class, (proxy, method, args) -> null);
        PurchaseReceiptLinkageService service = new PurchaseReceiptLinkageService(receipts, receiptLines,
                batch, autoCode(), null, null);

        WmArrivalNotice notice = new WmArrivalNotice();
        notice.setNoticeId(1L);
        notice.setNoticeCode("IT-AN-001");
        List<WmArrivalNoticeLine> lines = Arrays.asList(
                arrivalLine(11L, 1L, 101L, "10", "8", 55L),
                arrivalLine(12L, 1L, 102L, "5", "0", 56L));

        WmItemRecpt created = service.createDraftWhenReady(notice, lines, "inspector");
        assertEquals(1, headerInserts.get());
        assertEquals("只允许合格行进入入库草稿", 1, insertedLines.size());
        assertEquals(0, new BigDecimal("8").compareTo(insertedLines.get(0).getQuantityRecived()));
        assertEquals(Long.valueOf(11L), insertedLines.get(0).getNoticeLineId());
        assertEquals("AUTO:ITEM_RECPT_LINE:ARRIVAL_LINE:11", insertedLines.get(0).getAttr1());
        assertEquals(Long.valueOf(88L), created.getRecptId());
    }

    /** 判定结论必须与合格/不合格数量一致。 */
    @Test
    public void iqcResultMustMatchQuantity() {
        assertEquals("ACCEPT", QcIqcCompletionService.deriveResult(new BigDecimal("8")));
        assertEquals("REJECT", QcIqcCompletionService.deriveResult(BigDecimal.ZERO));

        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "10", "0", "10");
        request.setCheckResult("ACCEPT");
        expectRejected(() -> ctx.service().save(request, "inspector"), "检测结论与合格/不合格数量不一致");
    }

    /** 检测数量不得超过本次接收数量。 */
    @Test
    public void iqcCheckQuantityCannotExceedReceived() {
        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "11", "11", "0");
        expectRejected(() -> ctx.service().save(request, "inspector"), "检测数量不得超过来源可处理数量");
    }

    /** 篡改来源行必须拒绝。 */
    @Test
    public void iqcSourceTamperingIsRejected() {
        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "10", "10", "0");
        request.setSourceLineId(99L);
        expectRejected(() -> ctx.service().save(request, "inspector"), "来源单据行不允许通过普通接口修改");
    }

    /** 并发完成：只有一个请求成功，另一个拿不到条件更新行数。 */
    @Test
    public void concurrentIqcCompletionHasSingleWinner() {
        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        ctx.finishRows.set(0);
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "10", "10", "0");
        expectRejected(() -> ctx.service().save(request, "inspector"), "已被其他请求完成");
    }

    /** 正常完成：回写来源行合格量并触发下游草稿。 */
    @Test
    public void completedIqcWritesBackAndCreatesDraft() {
        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "10", "8", "2");
        request.setCheckResult("ACCEPT");
        ctx.service().save(request, "inspector");

        assertEquals("来源行合格数量应回写", 0, new BigDecimal("8").compareTo(ctx.sourceLine.getQuantityQuanlified()));
        assertEquals(Long.valueOf(55L), ctx.sourceLine.getIqcId());
        assertEquals("完成应触发一次下游草稿", 1, ctx.draftCalls.get());
    }

    /** 编码工具桩：固定返回可预测的单号，避免测试触发真实编码规则。 */
    private static AutoCodeUtil autoCode() {
        return new AutoCodeUtil() {
            @Override
            public String genSerialCode(String ruleCode, String inputCharacter) {
                return "IT-" + ruleCode;
            }
        };
    }

    /** 无来源的手工/历史检验单：允许完成，但不得回写或触发任何下游单。 */
    @Test
    public void sourceLessIqcCompletesWithoutDownstream() {
        QcIqcCompletionContext ctx = new QcIqcCompletionContext();
        ctx.sourceType = null;
        QcIqc request = ctx.request(UserConstants.ORDER_STATUS_CONFIRMED, "10", "10", "0");
        ctx.service().save(request, "inspector");
        assertEquals("无来源检验单不应生成下游草稿", 0, ctx.draftCalls.get());
        assertEquals("无来源检验单不写回到货行", null, ctx.sourceLine.getIqcId());
    }

    /** 来料检验完成测试的固定夹具。 */
    private static final class QcIqcCompletionContext {
        final AtomicInteger finishRows = new AtomicInteger(1);
        final AtomicInteger draftCalls = new AtomicInteger();
        final WmArrivalNoticeLine sourceLine = arrivalLine(11L, 1L, 101L, "10", null, null);
        String sourceType = UserConstants.SOURCE_DOC_TYPE_ARRIVALNOTICE;

        QcIqc persisted() {
            QcIqc iqc = new QcIqc();
            iqc.setIqcId(55L);
            iqc.setIqcCode("IT-IQC-001");
            iqc.setSourceDocType(sourceType);
            iqc.setSourceDocId(sourceType == null ? null : 1L);
            iqc.setSourceDocCode(sourceType == null ? null : "IT-AN-001");
            iqc.setSourceLineId(sourceType == null ? null : 11L);
            iqc.setItemId(101L);
            iqc.setQuantityRecived(new BigDecimal("10"));
            iqc.setStatus(UserConstants.ORDER_STATUS_PREPARE);
            return iqc;
        }

        QcIqc request(String status, String received, String qualified, String unqualified) {
            QcIqc request = new QcIqc();
            request.setIqcId(55L);
            request.setStatus(status);
            request.setQuantityRecived(new BigDecimal(received));
            request.setQuantityCheck(new BigDecimal(received));
            request.setQuantityQualified(new BigDecimal(qualified));
            request.setQuantityUnqualified(new BigDecimal(unqualified));
            return request;
        }

        QcIqcCompletionService service() {
            IQcIqcService iqcService = fake(IQcIqcService.class, (proxy, method, args) -> {
                if ("selectQcIqcByIqcId".equals(method.getName())) {
                    return persisted();
                }
                return 1;
            });
            IWmArrivalNoticeService notices = fake(IWmArrivalNoticeService.class, (proxy, method, args) -> {
                if ("selectWmArrivalNoticeByNoticeId".equals(method.getName())) {
                    WmArrivalNotice notice = new WmArrivalNotice();
                    notice.setNoticeId(1L);
                    notice.setStatus(UserConstants.ORDER_STATUS_APPROVING);
                    return notice;
                }
                return null;
            });
            IWmArrivalNoticeLineService arrivalLines = fake(IWmArrivalNoticeLineService.class, (proxy, method, args) -> {
                if ("selectWmArrivalNoticeLineByLineId".equals(method.getName())) {
                    return sourceLine;
                }
                if ("selectWmArrivalNoticeLineList".equals(method.getName())) {
                    return Collections.singletonList(sourceLine);
                }
                return 1;
            });
            IWmOutsourceRecptService outsource = fake(IWmOutsourceRecptService.class, (proxy, method, args) -> null);
            IWmOutsourceRecptLineService outsourceLines = fake(IWmOutsourceRecptLineService.class, (proxy, method, args) -> null);
            PurchaseReceiptLinkageService draft = new PurchaseReceiptLinkageService(null, null, null, null, null, null) {
                @Override
                public WmItemRecpt createDraftWhenReady(WmArrivalNotice notice,
                                                        List<WmArrivalNoticeLine> sourceLines,
                                                        String operator) {
                    draftCalls.incrementAndGet();
                    return null;
                }
            };
            PostingLockMapper lock = fake(PostingLockMapper.class, (proxy, method, args) ->
                    UserConstants.ORDER_STATUS_PREPARE);
            ReceiptGuardMapper guard = fake(ReceiptGuardMapper.class, (proxy, method, args) -> {
                if ("completedCheckQuantityOnArrivalLine".equals(method.getName())
                        || "completedQualifiedQuantityOnArrivalLine".equals(method.getName())) {
                    return BigDecimal.ZERO;
                }
                if ("finishIqc".equals(method.getName())) {
                    return finishRows.get();
                }
                return 1;
            });
            return new QcIqcCompletionService(iqcService, notices, arrivalLines, outsource, outsourceLines,
                    draft, new FormLinkagePolicy(), lock, guard);
        }
    }
}
