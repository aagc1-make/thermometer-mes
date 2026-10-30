package com.ktg.mes.linkage;

/** 现有侧边栏中可接收关联表单提醒的菜单路径。 */
public final class MenuLinkageNoticePaths {

    public static final String IQC = "/mes/qc/iqc";
    public static final String ITEM_RECPT = "/mes/wm/itemrecpt";
    public static final String IPQC = "/mes/qc/ipqc";
    public static final String PRODUCT_SALES = "/mes/wm/productsales";
    public static final String OQC = "/mes/qc/oqc";
    /** 产品入库：成品从线边仓转到成品仓 */
    public static final String PRODUCT_RECPT = "/mes/wm/productrecpt";
    /** 待检任务：IQC/IPQC/OQC 草稿的统一工作台 */
    public static final String PENDING_INSPECT = "/mes/qc/pendinginspect";
    /** 生产领料：原料入库后提示可以领料 */
    public static final String ISSUE = "/mes/wm/issue";
    /** 生产报工：领料执行后提示可以报工 */
    public static final String FEEDBACK = "/mes/pro/feedback";
    /** 生产排产：工单确认后提示去排产 */
    public static final String SCHEDULE = "/mes/pro/proschedule";

    private MenuLinkageNoticePaths() {
    }
}
