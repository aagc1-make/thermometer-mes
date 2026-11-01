# 一键业务演示 Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Independent backend batch constraints may be delegated under dispatching-parallel-agents.

**Goal:** 在 MES 智能助手中一键执行现有标准三工单业务主链。
**Architecture:** 独立前端业务编排模块调用当前用户业务接口，复用自动生成单据；执行器保存每个操作的检查点，控制暂停、停止和中断恢复。后端增加可选的报工消耗批次约束，避免演示扣减历史线边库存。
**Tech Stack:** Vue 2.6、Element UI 2.15、Java 8、Spring Boot、MyBatis、Node test。
**Spec:** ../specs/2026-10-31-one-click-business-demo-design.md

## Global Constraints
- 使用真实接口、现有权限与过账校验，不直接修改数据库。
- 默认生产 10 台、销售 6 台、本次批次结存 4 台。
- 不覆盖工作区已有修改，不提交其他任务的变更；在当前目录新分支实现以保留现有运行环境。

## Review Focus
- 写请求超时或刷新后不得自动重复执行；恢复必须先核对原单据。
- 半成品为整机子工单，合格产出直接在线边流转。
- 历史库存不得被本次报工消耗；服务端校验批次来源。
- 暂停和停止必须作用于每个接口请求之间，而非仅阶段之间。
- 自动联动单据不存在或来源不匹配时必须停止。

### Task 1: 报工批次约束
Files: ProFeedbackController.java、WmItemConsumeServiceImpl.java、新增批次来源 mapper/service 与 JUnit 测试。
Interface: PUT /mes/pro/feedback/{recordId}/with-batches，body 为 { itemId: [batchCode] }。
- [ ] 测试来源校验、缺少消耗物料、历史库存排除、数量不足回滚及原接口兼容；先失败再实现。
- [ ] 新接口复用现有 execute 逻辑，只收紧实际消耗批次，不降低任何业务校验。
- [ ] 运行后端相关测试。

### Task 2: 可恢复执行器与真实业务编排
Files: src/utils/businessDemoRunner.js、src/utils/businessDemoFlow.js、scripts/tests/business-demo.test.cjs。
Interfaces: createDemoRunner({ request, storage, userId, onChange, delayMs })；state、start、pause、resume、stop、restore。
- [ ] 先写测试，覆盖重复启动、暂停/停止、中断、失败不继续、操作检查点和批次过滤。
- [ ] 实现执行器；每个写操作先记录意图，再执行，每个操作用查询及唯一编号核对，结果不确定时冻结为 interrupted。
- [ ] 编排主数据预检、采购/IQC/入库、父子工单/排产、领料/报工/IPQC、产品入库、发货/OQC/出库和结果核对。
- [ ] 运行新增测试及已有智能助手回归测试。

### Task 3: 智能助手入口与控制面板
Files: src/views/mes/ai/BusinessDemo.vue、src/views/mes/ai/index.vue。
- [ ] 增加一键入口及对话触发，面板展示阶段讲解、状态、关联单据和失败原因。
- [ ] 控制面板订阅独立执行器，路由切换后状态仍可查看；刷新后显示中断并要求核对恢复。
- [ ] 前端 SFC 编译检查、生产构建；隔离服务真实跑通主链并检查页面。
- [ ] 独立代码审查，修复重要发现，记录验证证据。

## Progress

- 用户已明确“按现有流程直接做”，直接执行，开发者自行决定实现细节。
- Ruling: 使用当前目录的新分支，保留未提交改动和本地依赖，避免新 worktree 遗失当前业务修正。
- Ruling: 交付工作区代码及验证结果，不自动提交包含用户现有变更的文件。
