# -*- coding: utf-8 -*-
import re, collections, io, os

SRC = r"D:\My_project\测温枪\runtime\spec_export.html"
OUT = r"D:\My_project\测温枪\需求规格说明书_标题大纲.md"

s = open(SRC, "rb").read().decode("gbk", "replace")

items = []
for m in re.finditer(r"<h([1-6])[^>]*>(.*?)</h\1>", s, re.S):
    lvl = int(m.group(1))
    t = re.sub(r"<[^>]+>", "", m.group(2)).replace("&nbsp;", " ")
    t = re.sub(r"\s+", " ", t).strip()
    t = t.strip("\u200b").strip()
    if t:
        items.append((lvl, t))

cnt = collections.Counter(l for l, _ in items)

# 一级标题统计
rows = []
cur1 = None
for lvl, t in items:
    if lvl == 1:
        cur1 = [t, 0, 0]
        rows.append(cur1)
    elif cur1 is not None and lvl == 2:
        cur1[1] += 1
    elif cur1 is not None and lvl == 3:
        cur1[2] += 1

lines = []
lines.append("---")
lines.append("title: 红外测温枪 MES 系统 需求规格说明书 标题大纲")
lines.append("source: 红外测温枪MES系统_软件需求规格说明书1 (1).doc")
lines.append("levels: 一级 / 二级 / 三级")
lines.append("counts: h1=%d h2=%d h3=%d total=%d" % (cnt[1], cnt[2], cnt[3], len(items)))
lines.append("---")
lines.append("")
lines.append("> **来源文档**：`红外测温枪MES系统_软件需求规格说明书1 (1).doc`  ")
lines.append("> **提取范围**：全部一级、二级、三级标题，按文档原始顺序，未做任何改写  ")
lines.append("> **标题统计**：一级 %d 个 · 二级 %d 个 · 三级 %d 个 · 合计 %d 个" % (cnt[1], cnt[2], cnt[3], len(items)))
lines.append("")
lines.append("**一级标题总览**")
lines.append("")
lines.append("| 序号 | 一级标题 | 二级标题数 | 三级标题数 |")
lines.append("| ---: | --- | ---: | ---: |")
for i, (t, n2, n3) in enumerate(rows, 1):
    lines.append("| %d | %s | %d | %d |" % (i, t.replace("|", "\\|"), n2, n3))
lines.append("")
lines.append("---")
lines.append("")

for lvl, t in items:
    lines.append("#" * lvl + " " + t)
    lines.append("")

open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(lines).rstrip() + "\n")
print("written", OUT)
print("h1=%d h2=%d h3=%d other=%d" % (cnt[1], cnt[2], cnt[3], sum(v for k, v in cnt.items() if k > 3)))
print("bytes", os.path.getsize(OUT))
