package com.ktg.system.mapper;

import com.ktg.system.domain.SysCodeRule;
import org.apache.ibatis.annotations.Param;

/**
 * 编码规则/取号器 Mapper
 *
 * <p>取号依赖 MySQL 的 LAST_INSERT_ID(expr) 会话特性：
 * INSERT ... ON DUPLICATE KEY UPDATE current_no = LAST_INSERT_ID(current_no + step)
 * 会在当前连接上暂存本次发出的流水号，随后同一连接上的 SELECT LAST_INSERT_ID() 读回。
 * 因此这两个语句必须在同一个事务（同一连接）内执行。</p>
 *
 * @author ktg
 */
public interface SysCodeSequenceMapper
{
    /**
     * 按规则编码查询规则定义
     */
    SysCodeRule selectSysCodeRuleByCode(@Param("ruleCode") String ruleCode);

    int reserveCode(@Param("code") String code);

    int countReservedCode(@Param("code") String code);

    /**
     * 原子取号：新周期从 startNo 开始，已存在则 current_no 累加 step。
     *
     * @return 影响行数（1=首次建行，2=已有行累加）
     */
    int takeSequence(@Param("ruleCode") String ruleCode,
                     @Param("periodKey") String periodKey,
                     @Param("startNo") Long startNo,
                     @Param("step") Integer step);

    /**
     * 读取同一连接上最近一次 LAST_INSERT_ID(expr) 的值
     */
    Long selectLastInsertId();

    /**
     * 查询某业务表当天已使用的流水号集合（用于“最小可用号”分配）。
     * 表名/列名只允许来自 CodeSequenceService 内部固定映射，不接受外部输入。
     */
    java.util.List<Integer> selectUsedSerials(@Param("tableName") String tableName,
                                              @Param("columnName") String columnName,
                                              @Param("codePrefix") String codePrefix,
                                              @Param("serialPos") int serialPos);

    /**
     * 统计单号是否已被业务表占用（软删除单据仍占用，保证审计可追溯）。
     */
    int countCode(@Param("tableName") String tableName,
                  @Param("columnName") String columnName,
                  @Param("code") String code);
}
