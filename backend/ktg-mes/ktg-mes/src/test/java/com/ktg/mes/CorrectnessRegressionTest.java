package com.ktg.mes;

import com.ktg.common.core.domain.AjaxResult;
import com.ktg.mes.wm.controller.WmProductSalesController;
import com.ktg.mes.wm.controller.WmProductRecptController;
import com.ktg.mes.wm.domain.*;
import com.ktg.mes.wm.service.*;
import org.junit.Test;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 不连接运行库；执行真实 Controller，替换持久化边界以验证拒绝时绝不发生过账。 */
public class CorrectnessRegressionTest {
    @Test public void physicalStockChecksAreEnabledByDefault() {
        assertTrue("physical stock must not become negative",new WmTransaction().isStorageCheckFlag());
    }
    @Test public void negativeTransactionQuantityIsRejectedBeforeAnyWrite() {
        WmTransaction tx=new WmTransaction(); tx.setTransactionType("PRODUCT_SALES");
        tx.setTransactionQuantity(BigDecimal.ONE.negate()); tx.setTransactionFlag(-1);
        tx.setSourceDocCode("TEST"); tx.setSourceDocLineId(1L);
        try {
            new com.ktg.mes.wm.service.impl.WmTransactionServiceImpl().processTransaction(tx);
            fail("negative quantity must be rejected");
        } catch (com.ktg.common.exception.BussinessException expected) {
            assertTrue(expected.getMessage().contains("数量"));
        }
    }
    interface Call { Object invoke(String method, Object[] args) throws Throwable; }
    @SuppressWarnings("unchecked")
    static <T> T fake(Class<T> type, Call call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> call.invoke(method.getName(), args));
    }
    static void set(Object obj, String name, Object value) throws Exception {
        Field f=obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj,value);
    }
    // 兼容修复前代码：修复后自动注入真实新增的行锁边界代理。
    static void locks(Object obj, String state) throws Exception {
        try {
            Field f=obj.getClass().getDeclaredField("postingLockMapper"); f.setAccessible(true);
            f.set(obj, fake(f.getType(), (m,a)->state));
        } catch (NoSuchFieldException beforeFix) { }
    }
    @Test public void completedReceiptCannotPostTwice() throws Exception {
        WmProductRecptController c=new WmProductRecptController();
        WmProductRecpt head=new WmProductRecpt(); head.setStatus("FINISHED");
        AtomicInteger writes=new AtomicInteger();
        set(c,"wmProductRecptService",fake(IWmProductRecptService.class,(m,a)-> {
            if(m.startsWith("select"))return head;
            if(m.equals("getTxBean"))return Collections.emptyList();
            return 1;
        }));
        set(c,"wmProductRecptLineService",fake(IWmProductRecptLineService.class,(m,a)->Collections.singletonList(new WmProductRecptLine())));
        set(c,"storageCoreService",fake(IStorageCoreService.class,(m,a)->{writes.incrementAndGet();return null;}));
        locks(c,"FINISHED");
        AjaxResult result=c.execute(1L);
        assertEquals("already posted receipt must not alter stock",0,writes.get());
        assertNotEquals(200,result.get("code"));
    }
    @Test public void pendingInspectionSaleCannotPost() throws Exception {
        WmProductSalesController c=new WmProductSalesController();
        WmProductSales head=new WmProductSales(); head.setStatus("UNCHECK");
        AtomicInteger writes=new AtomicInteger();
        set(c,"wmProductSalesService",fake(IWmProductSalesService.class,(m,a)-> {
            if(m.startsWith("select"))return head;
            if(m.equals("getTxBeans"))return Collections.emptyList();
            return 1;
        }));
        WmProductSalesLine line=new WmProductSalesLine(); line.setQuantitySales(BigDecimal.ONE);
        set(c,"wmProductSalesLineService",fake(IWmProductSalesLineService.class,(m,a)->Collections.singletonList(line)));
        set(c,"storageCoreService",fake(IStorageCoreService.class,(m,a)->{writes.incrementAndGet();return null;}));
        locks(c,"UNCHECK");
        AjaxResult result=c.execute(1L);
        assertEquals("pending inspection must not alter stock",0,writes.get());
        assertNotEquals(200,result.get("code"));
    }
}
