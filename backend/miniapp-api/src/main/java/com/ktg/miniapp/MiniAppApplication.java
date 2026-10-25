package com.ktg.miniapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业订购小程序独立后端
 * - 端口 8081，接口前缀 /api/v1
 * - 与 MES 后端（8080）完全隔离，仅共用同一个 MySQL 实例
 */
@SpringBootApplication
public class MiniAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniAppApplication.class, args);
    }
}
