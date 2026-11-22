# -*- coding: utf-8 -*-
"""生成知识库演示文档（Word / Markdown / CSV）。

用法：
    python scripts/gen-demo-kb-docs.py

输出目录：docs/demo-kb/
所有物料、工序、检验项编码均取自 sql/thermometer_init.sql 的演示数据，
因此文档里的编码可以在「数据问答」中查到真实记录。
"""
from __future__ import annotations

import csv
import os
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

OUT = Path(__file__).resolve().parent.parent / "docs" / "demo-kb"
CN_FONT = "微软雅黑"
BODY_FONT = "宋体"


# ---------------------------------------------------------------- 基础排版工具
def base_document(title: str, subtitle: str = "", footer_left: str = "") -> Document:
    doc = Document()
    section = doc.sections[0]
    section.page_width, section.page_height = Cm(21.0), Cm(29.7)
    section.top_margin = Cm(2.4)
    section.bottom_margin = Cm(2.2)
    section.left_margin = Cm(2.6)
    section.right_margin = Cm(2.2)

    normal = doc.styles["Normal"]
    normal.font.name = BODY_FONT
    normal.font.size = Pt(10.5)
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)

    for name, size in (("Heading 1", 15), ("Heading 2", 12.5), ("Heading 3", 11.5)):
        style = doc.styles[name]
        style.font.name = CN_FONT
        style.font.size = Pt(size)
        style.font.bold = True
        style.font.color.rgb = RGBColor(0x1B, 0x2B, 0x45)
        style.element.rPr.rFonts.set(qn("w:eastAsia"), CN_FONT)

    head = doc.add_paragraph()
    head.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = head.add_run(title)
    run.font.name = CN_FONT
    run._element.rPr.rFonts.set(qn("w:eastAsia"), CN_FONT)
    run.font.size = Pt(19)
    run.font.bold = True

    if subtitle:
        sub = doc.add_paragraph()
        sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = sub.add_run(subtitle)
        run.font.name = CN_FONT
        run._element.rPr.rFonts.set(qn("w:eastAsia"), CN_FONT)
        run.font.size = Pt(10.5)
        run.font.color.rgb = RGBColor(0x60, 0x6A, 0x78)

    if footer_left:
        add_footer(doc, footer_left)
    return doc


def add_footer(doc: Document, left: str) -> None:
    """页脚：左侧文件编号与版本，右侧“第 N 页”域。"""
    footer = doc.sections[0].footer
    paragraph = footer.paragraphs[0]
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    for text in (left, "　　第 "):
        run = paragraph.add_run(text)
        run.font.size = Pt(8.5)
        run.font.color.rgb = RGBColor(0x70, 0x78, 0x86)
        run.font.name = BODY_FONT
        run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)

    run = paragraph.add_run()
    run.font.size = Pt(8.5)
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = "PAGE"
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.append(begin)
    run._r.append(instr)
    run._r.append(end)

    run = paragraph.add_run(" 页")
    run.font.size = Pt(8.5)
    run.font.color.rgb = RGBColor(0x70, 0x78, 0x86)
    run.font.name = BODY_FONT
    run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)


def meta_table(doc: Document, rows: list[tuple[str, str]]) -> None:
    table = doc.add_table(rows=0, cols=2)
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for left, right in rows:
        cells = table.add_row().cells
        cells[0].width = Cm(3.6)
        for index, text in ((0, left), (1, right)):
            paragraph = cells[index].paragraphs[0]
            run = paragraph.add_run(text)
            run.font.size = Pt(9.5)
            run.font.bold = index == 0
            run.font.name = BODY_FONT
            run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    doc.add_paragraph()


def heading(doc: Document, text: str, level: int = 1) -> None:
    doc.add_heading(text, level=level)


def para(doc: Document, text: str, bold_prefix: str = "") -> None:
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.space_after = Pt(6)
    paragraph.paragraph_format.line_spacing = 1.35
    if bold_prefix:
        run = paragraph.add_run(bold_prefix)
        run.font.bold = True
        run.font.name = BODY_FONT
        run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    run = paragraph.add_run(text)
    run.font.name = BODY_FONT
    run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)


def bullet(doc: Document, text: str) -> None:
    paragraph = doc.add_paragraph(style="List Bullet")
    paragraph.paragraph_format.space_after = Pt(3)
    run = paragraph.add_run(text)
    run.font.name = BODY_FONT
    run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)


def table(doc: Document, header: list[str], rows: list[list[str]], font_size: float = 9) -> None:
    grid = doc.add_table(rows=1, cols=len(header))
    grid.style = "Table Grid"
    grid.alignment = WD_TABLE_ALIGNMENT.CENTER
    for index, text in enumerate(header):
        cell = grid.rows[0].cells[index]
        cell.text = ""
        run = cell.paragraphs[0].add_run(text)
        run.font.bold = True
        run.font.size = Pt(font_size)
        run.font.name = BODY_FONT
        run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
        cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
    for row in rows:
        cells = grid.add_row().cells
        for index, text in enumerate(row):
            cells[index].text = ""
            run = cells[index].paragraphs[0].add_run(text)
            run.font.size = Pt(font_size)
            run.font.name = BODY_FONT
            run._element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    doc.add_paragraph()


def page_break(doc: Document) -> None:
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


# ---------------------------------------------------------------- 文档一：整机检验规范
def doc_whole_inspection(version: str, aql_function: str, has_auto_off: bool) -> Document:
    is_new = version != "V1.0"
    doc = base_document(
        "红外测温枪整机检验规范",
        f"文件编号 Q/TC-JY-001　　版本 {version}　　生效日期 {'2026-11-20' if is_new else '2026-09-01'}",
        footer_left=f"Q/TC-JY-001　{version}",
    )
    meta_table(doc, [
        ("适用产品", "TG-FG-001 红外测温枪整机（TG-300 便携式，32~42.9℃）"),
        ("编制部门", "质量部"),
        ("起草 / 审核 / 批准", "王敏 / 李国栋 / 张启明"),
        ("替代文件", "Q/TC-JY-001 V1.1（作废）" if is_new else "首次发布"),
    ])

    heading(doc, "1　目的与适用范围")
    para(doc, "本规范规定 TG-FG-001 红外测温枪整机（以下简称“整机”）的出货检验项目、抽样方案与判定规则，"
              "适用于 OP80 出货检验工序以及库存抽查、客户退货复检。来料检验见 Q/TC-JY-002，"
              "过程检验见 Q/TC-GY-001 工艺说明中的 OP20、OP40、OP70 规定。")

    heading(doc, "2　引用文件")
    bullet(doc, "GB/T 2828.1-2012　计数抽样检验程序 第 1 部分：按接收质量限（AQL）检索的逐批检验抽样计划")
    bullet(doc, "Q/TC-GY-001　红外测温枪整机装配与校准工艺说明")
    bullet(doc, "Q/TC-JY-002　红外传感器模块来料检验指导书")

    heading(doc, "3　术语与缺陷分级")
    para(doc, "AQL：接收质量限。Ac：合格判定数。Re：不合格判定数。", "3.1　")
    table(doc, ["等级", "定义", "示例"], [
        ["致命缺陷", "可能导致人身伤害、测温结果失效或安全风险", "测温偏差超过 ±0.5℃、显示误报超温"],
        ["主要缺陷", "影响功能或客户正常使用", "按键失灵、蜂鸣器不响、自动关机失效"],
        ["轻微缺陷", "不影响功能，仅影响外观或标识", "丝印轻微偏位、彩盒压痕"],
    ])

    heading(doc, "4　抽样方案")
    para(doc, "采用 GB/T 2828.1 正常检验一次抽样，一般检验水平 II，按批提交。", "4.1　检验水平：")
    para(doc, f"外观 AQL 1.0；功能 AQL {aql_function}；测温精度为关键特性，AQL 0（不允许出现不合格品）。", "4.2　接收质量限：")
    table(doc, ["批量范围（台）", "样本量 n", "外观 AQL 1.0（Ac/Re）", f"功能 AQL {aql_function}（Ac/Re）"], [
        ["91 – 150", "20", "0 / 1", "0 / 1"],
        ["151 – 280", "32", "1 / 2", "0 / 1"],
        ["281 – 500", "50", "1 / 2", "1 / 2"],
        ["501 – 1200", "80", f"{'2 / 3' if aql_function == '1.0' else '1 / 2'}", f"{'2 / 3' if aql_function == '1.0' else '1 / 2'}"],
        ["1201 – 3200", "125", "3 / 4", "2 / 3"],
    ])
    para(doc, "表 4-1 为常用批量段，超出范围时按 GB/T 2828.1 标准表检索。样本从同一批次随机抽取，"
              "不连续取自同一包装箱。")

    heading(doc, "5　检验项目与要求")
    items = [
        ["5.1", "外观", "无划伤、缩水、色差；丝印清晰可辨，划伤长度 ≤0.5mm 且不影响识别", "目视，照度 ≥500lx"],
        ["5.2", "显示", "全段点亮，无缺划、无闪烁；强光下可读", "通电目视"],
        ["5.3", "按键", "上/下/设置三键行程 0.3–0.5mm，回弹正常，无卡滞", "手感确认 + 塞规"],
        ["5.4", "蜂鸣", "提示音清晰，声压级 85±5 dB（距 10cm）", "声级计"],
        ["5.5", "测温精度（关键）", "黑体炉设定 35.0、37.0、42.0℃ 三点，示值误差 ≤±0.2℃", "黑体炉比对，每点 3 次取均值"],
    ]
    if is_new:
        items.append(["5.5a", "重复性（关键）", "同一测点连续 10 次读数极差 ≤0.2℃", "黑体炉 37.0℃"])
    if has_auto_off:
        items.append(["5.6", "自动关机", "无操作 60±5 s 自动关机，按键后恢复显示", "秒表计时"])
        items.append(["5.7", "跌落", "1.2m 高度六面各跌落一次，功能正常", "跌落台"])
        items.append(["5.8", "包装标识", "型号、批号、生产日期、合格证齐全；彩盒无破损", "目视"])
    else:
        items.append(["5.6", "跌落", "1.2m 高度六面各跌落一次，功能正常", "跌落台"])
        items.append(["5.7", "包装标识", "型号、批号、生产日期、合格证齐全；彩盒无破损", "目视"])
    table(doc, ["编号", "检验项目", "技术要求", "检验方法"], items)

    heading(doc, "6　判定规则")
    para(doc, "出现任一致命缺陷，整批判不合格，不允许让步接收。")
    para(doc, "主要与轻微缺陷按表 4-1 的 Ac/Re 判定：不合格数 ≤Ac 判接收，≥Re 判拒收。")
    critical = "5.5、5.5a" if is_new else "5.5"
    para(doc, f"测温精度与重复性（{critical}）按关键特性单独判定，任一样本超差即整批拒收。")

    page_break(doc)
    heading(doc, "7　不合格品处理")
    table(doc, ["判定结果", "处置方式", "审批"], [
        ["拒收", "退回生产线拆检；查明批次范围后扩大抽检", "质量工程师"],
        ["让步接收", "仅限轻微缺陷，且不影响功能与标识", "质量经理 + 生产经理"],
        ["返工后复检", "返工完成重新提交，复检按加严抽样", "质量工程师"],
    ])

    heading(doc, "8　记录与追溯")
    para(doc, "每批检验填写《出货检验单》，记录工单号、批次号、样本量、缺陷项、黑体炉编号、"
              "校准有效期与检验员。记录保存 3 年，客户投诉时按批次反查工单与工序记录。")
    para(doc, "黑体炉须在有效校准期内使用；超出有效期或跌落过的仪器不得用于 5.5 项判定。")

    heading(doc, "9　变更记录")
    table(doc, ["版本", "日期", "变更内容", "变更人"], [
        ["V1.0", "2026-09-01", "首次发布，功能 AQL 1.0", "王敏"],
        ["V1.1", "2026-10-15", "补充 5.5a 重复性要求；明确黑体炉有效期条款", "王敏"],
        ["V1.2", "2026-11-20", f"功能 AQL 由 1.0 收紧至 0.65；新增 5.6 自动关机检验项（顺序后移）", "王敏"],
    ] if is_new else [
        ["V1.0", "2026-09-01", "首次发布，功能 AQL 1.0；未包含自动关机检验项", "王敏"],
    ])
    return doc


# ---------------------------------------------------------------- 文档二：来料检验指导书
def doc_incoming() -> Document:
    doc = base_document("红外传感器模块来料检验指导书",
                        "文件编号 Q/TC-JY-002　　版本 V2.0　　生效日期 2026-11-10",
                        footer_left="Q/TC-JY-002　V2.0")
    meta_table(doc, [
        ("适用物料", "TG-EL-002 红外传感器模块（MLX90614 兼容，TO-39 封装）"),
        ("编制部门", "质量部"),
        ("起草 / 审核 / 批准", "陈静 / 李国栋 / 张启明"),
        ("替代文件", "Q/TC-JY-002 V1.0（作废）"),
    ])

    heading(doc, "1　适用范围")
    para(doc, "本指导书规定 TG-EL-002 红外传感器模块（以下简称“传感器”）的来料抽样、检验项目、"
              "判定规则与存储要求。该物料为整机测温核心器件，仅装配于 TG-SF-001 测温显示电控预装件，"
              "最终随整机 TG-FG-001 出厂，不允许流入其他产品。")

    heading(doc, "2　抽样方案")
    para(doc, "每来料批随机抽取 5 件（n=5），AQL 0.65；来料不足 5 件时全检。")
    para(doc, "取样时佩戴防静电手环，不得直接徒手接触引脚。")

    heading(doc, "3　检验项目")
    table(doc, ["编号", "项目", "要求", "类别"], [
        ["3.1", "包装", "防静电袋完好，湿度指示卡未变色，标签型号与批次可辨", "一般"],
        ["3.2", "外观", "引脚无氧化、无变形；本体丝印清晰；无机械损伤", "一般"],
        ["3.3", "尺寸", "TO-39 封装外径 9.0±0.2mm，引脚间距符合规格书", "一般"],
        ["3.4", "电性能（关键）", "I2C 通信正常，从机地址 0x5A，上电电流 ≤1.5mA", "关键"],
        ["3.5", "功能（关键）", "黑体炉 37.0℃ 读数偏差 ≤±0.2℃，重复 3 次极差 ≤0.1℃", "关键"],
        ["3.6", "标识", "批次号与送货单一致，可追溯至供应商出厂批", "一般"],
    ])

    heading(doc, "4　判定规则")
    para(doc, "关键项（3.4、3.5）任一取样件不合格，整批退货，不允许挑选后接收。")
    para(doc, "一般项按 Ac=0 / Re=1 判定；两项及以上一般项不合格同样退货。")

    heading(doc, "5　存储与上线要求")
    table(doc, ["项目", "要求"], [
        ["存储温度", "15 – 30℃"],
        ["相对湿度", "≤60%RH，避免结露"],
        ["包装", "原防静电袋密封，禁止散放"],
        ["开封后", "7 天内上线；超期须 120℃ / 2h 烘烤并重新检验 3.4、3.5"],
        ["领用", "按工单领用，剩余物料退回并重新密封"],
    ])

    heading(doc, "6　追溯要求")
    para(doc, "检验单须记录来料批次、供应商、送检数量、抽样数量、实测偏差与检验员。"
              "生产领用时按批次登记工序流转卡，保证从整机批次可反查传感器批次。")

    heading(doc, "7　变更记录")
    table(doc, ["版本", "日期", "变更内容"], [
        ["V1.0", "2026-09-10", "首次发布：抽样 3 件，仅检外观、尺寸、电性能"],
        ["V2.0", "2026-11-10", "抽样由 3 件改为 5 件；新增 3.5 功能项（黑体炉比对）；补充开封 7 天上限"],
    ])
    return doc


# ---------------------------------------------------------------- 文档三：工艺说明
def doc_process() -> Document:
    doc = base_document("红外测温枪整机装配与校准工艺说明",
                        "文件编号 Q/TC-GY-001　　版本 V1.1　　生效日期 2026-11-05",
                        footer_left="Q/TC-GY-001　V1.1")
    meta_table(doc, [
        ("适用产品", "TG-FG-001 红外测温枪整机；半成品 TG-SF-001、TG-SF-002"),
        ("编制部门", "生产技术部"),
        ("起草 / 审核 / 批准", "赵磊 / 周海 / 张启明"),
        ("替代文件", "Q/TC-GY-001 V1.0（作废）"),
    ])

    heading(doc, "1　适用范围")
    para(doc, "本说明规定整机及其半成品的工艺路线、关键工序参数、首件确认与异常处理要求，"
              "适用于试产与量产。检验判定标准见 Q/TC-JY-001（整机）与 Q/TC-JY-002（来料）。")

    heading(doc, "2　工艺路线")
    table(doc, ["工序", "工序名称", "产出", "是否过程检验"], [
        ["TG-OP10", "电控件预装", "TG-SF-001 测温显示电控预装件", "否"],
        ["TG-OP20", "电控件通电测试", "测试合格的电控件", "是"],
        ["TG-OP30", "手柄件预装", "TG-SF-002 扳机与侧键手柄预装件", "否"],
        ["TG-OP40", "手柄件功能测试", "测试合格的手柄件", "是"],
        ["TG-OP50", "整机总装", "整机半成品", "否"],
        ["TG-OP60", "整机功能测试", "功能合格整机", "否"],
        ["TG-OP70", "温度校准", "校准合格整机（关键工序）", "是"],
        ["TG-OP80", "出货检验", "合格待包装整机", "是（OQC）"],
        ["TG-OP90", "包装入库", "成品入库", "否"],
    ])

    heading(doc, "3　关键工序参数")
    heading(doc, "3.1　TG-OP10 电控件预装", 2)
    para(doc, "装配顺序：主控PCBA（TG-EL-001）→ 红外传感器模块（TG-EL-002）→ 显示组件（TG-EL-003）"
              "→ 蜂鸣器（TG-EL-005）→ 红外透镜（TG-ST-004）→ 防震泡棉（TG-ST-006）。")
    para(doc, "静电防护：手环阻值 1MΩ（每班点检），工位接地电阻 <4Ω，作业区离子风机常开。"
              "传感器引脚不得弯折，焊接或插装前确认方向标识。")

    heading(doc, "3.2　TG-OP20 电控件通电测试", 2)
    para(doc, "上电电流 ≤35mA；显示全段点亮无缺划；蜂鸣器可闻；连续上电 3 次无死机。")
    para(doc, "不良品隔离至红色周转箱，不得流入 TG-OP50。")

    heading(doc, "3.3　TG-OP50 整机总装", 2)
    para(doc, "螺丝锁付扭矩 0.25±0.05 N·m，按对角顺序锁付 6 颗（TG-ST-005 螺丝包 1 包/台）；"
              "上下壳（TG-ST-001、TG-ST-002）合模后无可见缝隙，卡扣不得强行压入。")

    heading(doc, "3.4　TG-OP70 温度校准（关键工序）", 2)
    table(doc, ["参数", "设定值", "要求"], [
        ["黑体炉温度点", "35.0℃ / 37.0℃ / 42.0℃", "每点稳定 ≥5 分钟后测量"],
        ["采集次数", "每点 3 次", "取算术平均作为该点示值"],
        ["允许偏差", "≤±0.2℃", "超差须调整后重新测量全部三点"],
        ["记录", "黑体炉编号、校准有效期、三点实测值", "填《校准记录》，随工单归档"],
    ])
    para(doc, "校准前确认黑体炉在校准有效期内；校准后贴上校准合格标识，标识注明日期与设备编号。")

    heading(doc, "3.5　TG-OP80 出货检验与 TG-OP90 包装入库", 2)
    para(doc, "出货检验按 Q/TC-JY-001 抽样；包装按每台 1 份说明书（TG-PK-001）、1 个彩盒（TG-PK-002），"
              "装箱后扫码入库成品仓。")

    page_break(doc)
    heading(doc, "4　首件确认")
    para(doc, "每班开机、更换物料批次、调整设备参数后，须完成首件 3 台全项检验，"
              "由班组长与 IPQC 双签确认后方可批量生产。首件记录保留至该工单结案。")

    heading(doc, "5　异常处理")
    table(doc, ["情形", "处置"], [
        ["OP20 或 OP40 连续 3 件不良", "停机，隔离当班在制品，通知工艺与设备到场分析"],
        ["OP70 校准超差且调整无效", "更换传感器后复测；连续 5 台超差按批次问题上报质量部"],
        ["物料批次混用", "立即停止，按工单核对批次，混用批次整批返工并记录"],
        ["设备扭矩漂移", "停机校验扭矩扳手，校准记录随设备点检表归档"],
    ])

    heading(doc, "6　记录")
    para(doc, "工序流转卡、首件确认单、设备点检表、校准记录、异常处理单，按工单归档，保存 3 年。")

    heading(doc, "7　变更记录")
    table(doc, ["版本", "日期", "变更内容"], [
        ["V1.0", "2026-09-05", "首次发布"],
        ["V1.1", "2026-11-05", "补充 OP70 三点校准与超差复测规则；明确首件 3 台双签要求"],
    ])
    return doc


# ---------------------------------------------------------------- 文档四：BOM 说明（Markdown）
BOM_MD = """# BOM 与物料对应关系说明

**文件编号** Q/TC-RD-003　　**版本** V1.0　　**生效日期** 2026-11-01　　**编制部门** 研发部

## 1　适用范围

说明 TG-FG-001 红外测温枪整机的三层 BOM 结构、物料用量与关键装配对应关系，
供计划、采购、生产与质量在查询“某物料用在哪个产品”时对照使用。
数量与实时库存以 MES 数据为准，本文件不载明。

## 2　整体结构

整机 TG-FG-001（红外测温枪整机）由 2 个自制半成品与 6 个直接物料构成；
两个半成品分别再展开为电子件与结构件，形成三层结构。

## 3　第一层：整机直属组成

| 物料编码 | 物料名称 | 类型 | 用量 | 说明 |
| --- | --- | --- | --- | --- |
| TG-SF-001 | 测温显示电控预装件 | 半成品 | 1 套/台 | 含主控、传感器、显示、蜂鸣、透镜 |
| TG-SF-002 | 扳机与侧键手柄预装件 | 半成品 | 1 套/台 | 含按键板、电池、扳机组件 |
| TG-ST-001 | 上壳 | 结构件 | 1 个/台 | ABS 白色 |
| TG-ST-002 | 下壳 | 结构件 | 1 个/台 | ABS 白色 |
| TG-ST-006 | 防震泡棉 | 结构件 | 1 片/台 | 电控件缓冲固定 |
| TG-ST-005 | 螺丝包 | 结构件 | 1 包/台 | M2×6 自攻螺丝 6 颗 |
| TG-PK-001 | 说明书 | 包装辅料 | 1 份/台 | 中文版 8 页 |
| TG-PK-002 | 彩盒包装 | 包装辅料 | 1 个/台 | 350×200×80mm |

## 4　第二层：半成品展开

### 4.1　TG-SF-001 测温显示电控预装件

| 物料编码 | 物料名称 | 用量 | 关键说明 |
| --- | --- | --- | --- |
| TG-EL-001 | 主控PCBA | 1 个/套 | TG-MAIN-V1.2，按整体外购，不展开内部器件 |
| TG-EL-002 | 红外传感器模块 | 1 个/套 | MLX90614 兼容 TO-39，测温核心器件 |
| TG-EL-003 | 显示组件 | 1 个/套 | 1.0 寸段码 LCD |
| TG-EL-005 | 蜂鸣器 | 1 个/套 | 5V 有源 |
| TG-ST-004 | 红外透镜 | 1 个/套 | HDPE 聚焦透镜 |
| TG-ST-006 | 防震泡棉 | 1 片/套 | 电控件缓冲 |

### 4.2　TG-SF-002 扳机与侧键手柄预装件

| 物料编码 | 物料名称 | 用量 | 关键说明 |
| --- | --- | --- | --- |
| TG-EL-004 | 三键按键板 | 1 个/套 | 上/下/设置三键 |
| TG-EL-006 | 电池 | 2 节/套 | AAA 1.5V 碱性 |
| TG-ST-003 | 扳机组件 | 1 个/套 | 扳机 + 复位弹簧 |

## 5　关键对应关系

- **TG-EL-002 红外传感器模块** 唯一用于 **TG-SF-001 测温显示电控预装件**，经 **TG-OP10 电控件预装**
  工序装入，最终装配于 **TG-FG-001 红外测温枪整机**，不存在其他用途。
- **TG-EL-004 三键按键板** 与 **TG-EL-006 电池** 用于 **TG-SF-002**，经 **TG-OP30 手柄件预装** 工序装配。
- **TG-SF-001** 经 **TG-OP20 电控件通电测试** 合格后流入 **TG-OP50 整机总装**；
  **TG-SF-002** 经 **TG-OP40 手柄件功能测试** 合格后流入同一工序。
- **TG-EL-001 主控PCBA** 按整体外购，其内部器件不在本 BOM 展开范围内。

## 6　替代与限制

- 当前无可替代物料；如需替代，须走工程变更（ECN）并重新做 OP70 温度校准验证。
- 传感器与主控为配对使用的测温核心件，禁止跨批次混用（见 Q/TC-GY-001 第 5 章）。

## 7　变更记录

| 版本 | 日期 | 变更内容 |
| --- | --- | --- |
| V1.0 | 2026-11-01 | 首次发布，与试产 BOM 一致 |
"""


# ---------------------------------------------------------------- 文档五：检验项 CSV
CSV_ROWS = [
    ["检验项编码", "检验项名称", "所属工序", "判定标准", "缺陷等级", "处理方式", "依据条款"],
    ["QI-01", "外观划伤", "TG-OP80", "划伤 ≤0.5mm 且不影响识别", "轻微", "返工抛光或让步接收", "Q/TC-JY-001 5.1"],
    ["QI-02", "显示缺划", "TG-OP20", "全段点亮无缺划", "主要", "更换显示组件返工", "Q/TC-JY-001 5.2"],
    ["QI-03", "按键卡滞", "TG-OP40", "行程 0.3–0.5mm，回弹正常", "主要", "拆检按键板，必要时更换", "Q/TC-JY-001 5.3"],
    ["QI-04", "蜂鸣异常", "TG-OP60", "85±5 dB（距 10cm）", "主要", "更换蜂鸣器", "Q/TC-JY-001 5.4"],
    ["QI-05", "测温偏差超差", "TG-OP70", "三点示值误差 ≤±0.2℃", "致命", "整批隔离，重新校准或更换传感器", "Q/TC-JY-001 5.5"],
    ["QI-06", "重复性超差", "TG-OP70", "10 次读数极差 ≤0.2℃", "致命", "判不合格，排查传感器与装配", "Q/TC-JY-001 5.5a"],
    ["QI-07", "自动关机失效", "TG-OP60", "60±5 s 自动关机", "主要", "返工固件或更换主控", "Q/TC-JY-001 5.6"],
    ["QI-08", "螺丝扭矩不足", "TG-OP50", "0.25±0.05 N·m", "主要", "重新锁付并记录", "Q/TC-GY-001 3.3"],
    ["QI-09", "引脚氧化", "来料检验", "引脚无氧化、无变形", "主要", "整批退货", "Q/TC-JY-002 3.2"],
    ["QI-10", "包装标识缺失", "TG-OP90", "型号、批号、日期、合格证齐全", "轻微", "补贴标识后放行", "Q/TC-JY-001 5.8"],
]


# ---------------------------------------------------------------- 文档六：安全库存办法
def doc_safety_stock() -> Document:
    doc = base_document("物料安全库存与缺料预警管理办法",
                        "文件编号 Q/TC-WM-002　　版本 V1.0　　生效日期 2026-11-01",
                        footer_left="Q/TC-WM-002　V1.0")
    meta_table(doc, [
        ("适用范围", "全部生产性物料与包装辅料"),
        ("编制部门", "仓储部"),
        ("起草 / 审核 / 批准", "孙倩 / 周海 / 张启明"),
        ("相关系统", "MES 库存与预警"),
    ])

    heading(doc, "1　目的")
    para(doc, "统一安全库存的设定口径、预警触发条件与响应时限，避免断料停线与过量备货。")

    heading(doc, "2　定义")
    para(doc, "安全库存：为应对需求波动与到货波动而设置的最低可用库存量。", "2.1　")
    para(doc, "可用库存：现存库存减去已分配未出库数量，以 MES 实时数据为准。", "2.2　")
    para(doc, "缺料预警：可用库存低于或等于安全库存时触发。", "2.3　")

    heading(doc, "3　安全库存设定方法")
    para(doc, "安全库存 = 近 3 个月日均消耗量 × 采购提前期（天） × 1.2（安全系数）。", "3.1　")
    para(doc, "试产物料、一次性物料可按单次用量设定，并在工单结案后清零。", "3.2　")
    para(doc, "安全库存每季度复核一次；工艺变更或供应商变更后 5 个工作日内重新计算。", "3.3　")

    heading(doc, "4　预警分级与响应")
    table(doc, ["级别", "触发条件", "响应时限", "责任人"], [
        ["缺料预警", "可用库存 ≤ 安全库存", "24 小时内完成 MRP 运算并提交采购申请", "仓储计划员"],
        ["紧急缺料", "可用库存 ≤ 安全库存的 50%", "4 小时内上报生产经理，48 小时内到货或调整排产", "仓储主管 + 采购"],
        ["停线风险", "可用库存 ≤ 当班计划用量", "立即启动紧急采购或替代方案评审", "生产经理"],
    ])

    heading(doc, "5　特殊情形")
    para(doc, "试产、急件或客户加急订单允许临时低于安全库存，须由生产经理审批并在 MES 备注原因。")
    para(doc, "传感器（TG-EL-002）与主控PCBA（TG-EL-001）为关键件，任何低于安全库存的情形均须同步质量部，"
              "以便评估是否影响已排产工单。")

    heading(doc, "6　数据口径")
    para(doc, "本办法只规定口径与流程，不载明任何物料的具体数量。当前库存数量、批次与结存"
              "一律以 MES 系统实时数据为准，问答或讨论库存时应引用系统查询结果，不得引用历史台账。")

    heading(doc, "7　变更记录")
    table(doc, ["版本", "日期", "变更内容"], [["V1.0", "2026-11-01", "首次发布"]])
    return doc


# ---------------------------------------------------------------- 输出
def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    produced: list[tuple[str, Path]] = []

    pairs = [
        ("01_TG-FG-001整机检验规范_V1.2.docx", doc_whole_inspection("V1.2", "0.65", True)),
        ("01_TG-FG-001整机检验规范_V1.0_旧版.docx", doc_whole_inspection("V1.0", "1.0", False)),
        ("02_TG-EL-002红外传感器模块来料检验指导书_V2.0.docx", doc_incoming()),
        ("03_TG-FG-001整机装配与校准工艺说明_V1.1.docx", doc_process()),
        ("06_物料安全库存与缺料预警管理办法_V1.0.docx", doc_safety_stock()),
    ]
    for name, document in pairs:
        path = OUT / name
        document.save(path)
        produced.append((name, path))

    bom = OUT / "04_BOM与物料对应关系说明_V1.0.md"
    bom.write_text(BOM_MD, encoding="utf-8")
    produced.append((bom.name, bom))

    csv_path = OUT / "05_检验项与缺陷处理对照表_V1.0.csv"
    with csv_path.open("w", encoding="utf-8-sig", newline="") as handle:
        csv.writer(handle).writerows(CSV_ROWS)
    produced.append((csv_path.name, csv_path))

    for name, path in produced:
        print(f"{path.stat().st_size:>8}  {name}")


if __name__ == "__main__":
    main()
