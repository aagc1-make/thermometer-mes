---
name: 温衡智造 MES
description: 以暖纸色、宋体标题和清晰的操作层级组织生产工作台。
colors:
  clay: "#a2462e"
  clay-hover: "#823923"
  clay-tint: "#f3e4d9"
  canvas: "#f7f5ef"
  surface: "#fffdf8"
  subtle: "#f3efe7"
  rail: "#ede7db"
  rail-hover: "#e7ddce"
  line: "#ded8cc"
  control-line: "#918577"
  ink: "#28251f"
  muted: "#62594e"
  button: "#2e2923"
  button-hover: "#4a4036"
typography:
  display:
    fontFamily: '"MES Newsreader", "MES Song", "Songti SC", "Noto Serif CJK SC", "Source Han Serif SC", SimSun, Georgia, serif'
    fontSize: "clamp(36px, 5vw, 70px)"
    fontWeight: 650
    lineHeight: 1.1
    letterSpacing: "-0.026em"
  title:
    fontFamily: '"MES Newsreader", "MES Song", "Songti SC", "Noto Serif CJK SC", "Source Han Serif SC", SimSun, Georgia, serif'
    fontSize: "21px"
    fontWeight: 650
    lineHeight: 1.3
  body:
    fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif'
    fontSize: "14px"
  label:
    fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif'
    fontSize: "14px"
    fontWeight: 650
rounded:
  control: "8px"
  panel: "12px"
  feature: "14px"
  pill: "999px"
spacing:
  control-gap: "8px"
  compact-inset: "12px"
  section-gap: "16px"
  workspace-inset: "20px"
components:
  button-primary:
    backgroundColor: "{colors.button}"
    textColor: "{colors.surface}"
    rounded: "{rounded.control}"
  button-primary-hover:
    backgroundColor: "{colors.button-hover}"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
  button-text:
    textColor: "{colors.clay}"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
  module-navigation:
    backgroundColor: "{colors.rail}"
    width: "76px"
  status-tag:
    rounded: "{rounded.pill}"
    padding: "0 10px"
    height: "24px"
  business-panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.panel}"
    padding: "{spacing.workspace-inset}"
---

# Design System: 温衡智造 MES

## Overview

**Creative North Star: "暖纸工作台"**

温衡智造的工作区用暖纸底、墨色文字和清楚的细线组织导航、表格与操作。登录和重点页面用中文衬线标题建立阅读顺序，工单和质检数据保持紧凑，方便操作员扫读和比对。

入口的红外测温枪质检照片提供制造场景，工作区把主要空间留给实际数据。深墨按钮指向主操作，陶土色标示链接、焦点和选中状态。

**Key Characteristics**

- 暖白内容面与砂色业务导航
- 衬线标题与便于扫读的无衬线控件
- 细线分区、有限的阴影与清楚的数值对齐

## Colors

### Primary

- **陶土色**用于链接、焦点环、选中标记与交互反馈。悬停可用更深的陶土色，浅陶底用于承托选中项或焦点。

### Neutral

- **暖纸与暖白**分别承载页面底和内容面。浅砂色区分业务导航和次级背景。
- **墨色与灰褐**分别用于主要内容和补充说明。深墨实底用于当前主要操作。
- **暖灰细线**划分页面、表头与容器。输入框使用更深的描边来标明可编辑范围。

**The Action and Status Rule.** 陶土色标记交互状态，业务结果沿用带文字的成功、警告和异常语义配色。

## Typography

- **网页标题**使用 Newsreader 拉丁字形和 Noto Serif SC 中文标题子集，缺少字形时依次采用系统宋体等回退字体。工作台大标题用随视口缩放的字号，驾驶舱区块标题采用紧凑的衬线字。
- **正文与控件**使用 Inter 和系统无衬线字体。中文随系统选用苹方、微软雅黑等字体。
- **数据数值**在表格和驾驶舱采用等宽数字特性。驾驶舱大指标和工作台统计数值使用常规字重的衬线字。

**The Numeric Alignment Rule.** 同列数值用等宽数字特性，保留可阅读的状态名称和计量单位。

## Layout

业务页面使用左侧模块轨道和二级菜单。较窄视口把导航移入可开合的抽屉，并将内容区间距收紧至紧凑值。页面表单和表格之间维持固定分组，使生产、仓储与质检页面都能快速定位筛选与记录。

入口宽屏采用两栏，窄屏在 820px 及以下收为单列登录。工作台标题与摘要在 620px 及以下改为纵向堆叠，驾驶舱图表在 760px 及以下改为单列。

## Elevation & Depth

工作台外壳、侧栏和内容容器主要用底色与细线形成层级。状态和焦点借助底色变化、描边与下划线反馈。浮动菜单通过柔和阴影区分自身与其覆盖的表面。

## Shapes

顶栏及并排导航使用直边，控制区域使用轻圆角，业务容器使用稍大的圆角。胶囊形状用于紧凑的状态标签。入口图片与对话输入区使用较大圆角。

## Components

### Buttons

深墨实底按钮承担提交或首要操作。次按钮用暖白底和细描边，悬停时加深边框。文本按钮使用陶土色，并在悬停或焦点时显示下划线。

### Inputs / Fields

暖白底输入框使用清晰的暖灰描边与可读的占位文字。聚焦时显示陶土色描边和浅陶色环，页面中的原生登录控件使用可见的键盘焦点轮廓。显式字段标签保留在输入框上方。

### Navigation

业务模块轨道使用砂色底，模块项用图标配合标签。当前模块使用较深砂底和陶土色文字，二级菜单通过浅陶色背景标示当前页面。导航切换在窄屏使用顶部按钮。

### Cards / Containers

业务容器以暖白底和细边框排列，连续内容可用分隔线。表头用浅砂底和灰褐文字，指标区与图表共享同一套正文色和细线。

### Chips

状态标签用简短文字配合语义颜色。检索方式的选中状态用浅陶底和陶土描边，按钮组保持可辨识的文字标签。

### 对话输入区

检索模式和知识库操作位于对话上方，居中的输入区同时承载示例问题和发送操作。回答中的来源标记连接到证据，保留引用与原文检查入口。

## Do's and Don'ts

- **Do** 用深墨实底按钮标出当前主操作。
- **Do** 在表格、指标和图谱节点中保留能直接读懂的名称、单位和状态。
- **Do** 在密集业务页面以细线和底色分组，保持键盘焦点清楚可见。
- **Don't** 将陶土强调色用作所有业务状态的统一底色。
- **Don't** 将展示型衬线字号套用到表格、录入字段或菜单项。
