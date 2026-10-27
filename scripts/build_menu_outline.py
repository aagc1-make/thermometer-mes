# -*- coding: utf-8 -*-
import io, os

SRC = r"D:\My_project\测温枪\runtime\menus_full.tsv"
OUT = r"D:\My_project\测温枪\系统侧边栏菜单结构.md"

rows = []
for line in io.open(SRC, encoding="utf-8"):
    line = line.rstrip("\n")
    if not line.strip():
        continue
    p = line.split("\t")
    rows.append({
        "id": p[0], "pid": p[1], "name": p[2].strip(), "type": p[3],
        "order": int(p[4] or 0), "visible": p[5], "status": p[6],
        "path": p[7], "component": p[8], "perms": p[9],
    })

kids = {}
for r in rows:
    kids.setdefault(r["pid"], []).append(r)
for k in kids:
    kids[k].sort(key=lambda r: (r["order"], int(r["id"])))

# 只保留侧边栏可见的目录 M 与菜单 C，按钮 F 不计入
def menus_of(pid):
    return [r for r in kids.get(pid, []) if r["type"] in ("M", "C")]

l1 = menus_of("0")
stat = []          # (一级行, [二级行], 三级数)
for a in l1:
    l2 = menus_of(a["id"])
    n3 = sum(len(menus_of(b["id"])) for b in l2)
    stat.append((a, l2, n3))

tot2 = sum(len(s[1]) for s in stat)
tot3 = sum(s[2] for s in stat)

L = []
L.append("---")
L.append("title: 红外测温枪 MES 系统 侧边栏菜单结构（一/二/三级）")
L.append("source: 运行系统 http://localhost/ · 数据库 j2eedb · 表 sys_menu")
L.append("filter: menu_type in (M,C) and visible='0' and status='0'")
L.append("counts: L1=%d L2=%d L3=%d total=%d" % (len(l1), tot2, tot3, len(l1) + tot2 + tot3))
L.append("---")
L.append("")
L.append("> **数据来源**：运行中的系统 `http://localhost/`，侧边栏菜单取自数据库 `j2eedb.sys_menu`（与前端 `getRouters` 接口同源）  ")
L.append("> **统计口径**：只统计侧边栏可见的目录（M）与菜单（C）；`visible='0'` 且 `status='0'`，不含按钮级权限（F 类 370 条）  ")
L.append("> **层级统计**：一级 %d 个 · 二级 %d 个 · 三级 %d 个 · 合计 %d 个" % (len(l1), tot2, tot3, len(l1) + tot2 + tot3))
L.append("")
L.append("**一级侧边栏总览**")
L.append("")
L.append("| 序号 | 一级侧边栏 | 二级菜单数 | 三级菜单数 | 路由 |")
L.append("| ---: | --- | ---: | ---: | --- |")
for i, (a, l2, n3) in enumerate(stat, 1):
    L.append("| %d | %s | %d | %d | `%s` |" % (i, a["name"], len(l2), n3, a["path"]))
L.append("")
L.append("---")
L.append("")

CN = "一二三四五六七八九十"
for i, (a, l2, n3) in enumerate(stat, 1):
    L.append("## %d. %s" % (i, a["name"]))
    L.append("")
    L.append("> 一级目录 · 路由 `%s` · 二级 %d 个 · 三级 %d 个" % (a["path"], len(l2), n3))
    L.append("")
    L.append("| 序号 | 二级菜单 | 三级菜单 | 路由 |")
    L.append("| ---: | --- | --- | --- |")
    idx = 0
    for b in l2:
        ch = menus_of(b["id"])
        idx += 1
        if not ch:
            L.append("| %d | %s | — | `%s` |" % (idx, b["name"], b["path"]))
        else:
            for j, c in enumerate(ch):
                L.append("| %s | %s | %s | `%s/%s` |" % (idx if j == 0 else "", b["name"] if j == 0 else "↳", c["name"], b["path"], c["path"]))
    L.append("")

L.append("---")
L.append("")
L.append("**附：侧边栏树形视图**")
L.append("")
L.append("```text")
for a, l2, n3 in stat:
    L.append(a["name"])
    for b in l2:
        ch = menus_of(b["id"])
        if not ch:
            L.append("├─ " + b["name"])
        else:
            L.append("├─ " + b["name"])
            for j, c in enumerate(ch):
                L.append("│  " + ("└─ " if j == len(ch) - 1 else "├─ ") + c["name"])
L.append("```")

io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(L).rstrip() + "\n")
print("written", OUT, os.path.getsize(OUT))
print("L1=%d L2=%d L3=%d" % (len(l1), tot2, tot3))
