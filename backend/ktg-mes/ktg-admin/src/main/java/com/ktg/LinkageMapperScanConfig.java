package com.ktg;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 本地环境适配补丁（非分支代码改动）：
 * 全局 MapperScan 只扫 com.ktg.**.mapper，分支的 linkage Mapper 不在该包路径下，
 * 这里补扫 com.ktg.mes.linkage，使菜单蓝点提醒的 Mapper 能注册为 Bean。
 */
@Configuration
@MapperScan("com.ktg.mes.linkage")
public class LinkageMapperScanConfig {
}
