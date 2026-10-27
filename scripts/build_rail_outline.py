# -*- coding: utf-8 -*-
"""按 Sidebar/index.vue 的真实渲染逻辑，从 sys_menu + rail-config.js 复现侧边栏一/二/三级结构"""
import io, os, json, subprocess, tempfile

BASE = r"D:\My_project\测温枪"
TSV = BASE + r"\runtime\menus_full.tsv"
CFG = BASE + r"\ktg-mes-ui\src\layout\rail-config.js"
OUT = BASE + r"\系统侧边栏菜单结构.md"

# ---- 读取 rail-config.js（用 node 求值，避免手写解析）----
js = io.open(CFG, encoding="utf-8").read() + "\nconsole.log(JSON.stringify(railDomains));"
f = tempfile.NamedTemporaryFile("w", suffix=".js", delete=False, encoding="utf-8")
f.write(js); f.close()
node = r"C:\Users\韩训亚\.workbuddy\binaries\node\versions\22.22.2-2\node.exe"
domains = json.loads(subprocess.check_output([node, f.name]))
os.unlink(f.name)

# ---- 读取 sys_menu（与 getRouters 同源；F 按钮不进路由）----
children = {}
for line in io.open(TSV, encoding="utf-8"):
    line = line.rstrip("\n")
    if not line.strip():
        continue
    p = line.split("\t")
    if p[3] not in ("M", "C"):
        continue
    m = {"id": p[0], "pid": p[1], "name": p[2].strip(), "type": p[3],
         "order": int(p[4] or 0), "visible": p[5], "path": p[7]}
    children.setdefault(m["pid"], []).append(m)
for k in children:
    children[k].sort(key=lambda x: (x["order"], int(x["id"])))

# ---- 复现 Sidebar/index.vue 的 collect()：收集叶子并计算绝对路径 ----
def joinpath(base, p):
    if not p:
        return base or "/"
    if p.startswith("/"):
        return p
    return ("" if base in (None, "/") else base) + "/" + p

def collect(nodes, base, out):
    for r in nodes:
        if r["visible"] != "0":
            continue
        full = joinpath(base, r["path"])
        ch = [c for c in children.get(r["id"], []) if c["visible"] == "0"]
        if ch:
            collect(ch, full, out)
        else:
            out.append({"full": full, "leaf": r})

stat = []
for d in domains:
    if d["key"] == "home":
        stat.append((d, [("首页", [{"full": "/index", "leaf": {"name": "首页"}}], False)]))
        continue
    tops = [m for m in children.get("0", [])
            if any(m["path"] == r.lstrip("/") or ("/" + m["path"]).startswith(r + "/") for r in d["roots"])]
    leaves = []
    for t in tops:
        sub = []
        collect([c for c in children.get(t["id"], []) if c["visible"] == "0"], "/" + t["path"], sub)
        leaves.extend(sub)
    groups, used = [], set()
    for gi, (gname, keys) in enumerate(d["groups"]):
        matched = [l for l in leaves if l["full"].split("/")[-1] in keys]
        if not matched:
            continue
        used.update(m["full"] for m in matched)
        groups.append((gname, matched, False))
    rest = [l for l in leaves if l["full"] not in used]
    if rest:
        groups.append(("其他", rest, True))
    if not groups:
        groups.append(("菜单", leaves, False))
    stat.append((d, groups))

n1 = len(stat)
n2 = sum(len(g) for _, g in stat)
n3 = sum(len(ms) for _, g in stat for _, ms, _ in g)

L = []
L.append("---")
L.append("title: 红外测温枪 MES 系统 侧边栏一二三级结构（实际渲染版）")
L.append("source: 前端 rail-config.js + Sidebar/index.vue 渲染逻辑 + sys_menu（/dev-api/getRouters 同源）")
L.append("levels: 一级=左栏图标域 · 二级=母目录分组 · 三级=组内菜单项")
L.append("counts: L1=%d L2=%d L3=%d total=%d" % (n1, n2, n3, n1 + n2 + n3))
L.append("---")
L.append("")
L.append("> **数据来源**：系统页面左侧实际渲染的侧边栏。一级是左栏 8 个图标域（`rail-config.js`）；二级是域内配置的分组（母目录，页面上的「计划 / 工艺 / 执行」等）；三级是按路由末段匹配进组的菜单项，未匹配的自动归入「其他」组。菜单标题与路由取自 `sys_menu`。  ")
L.append("> **层级统计**：一级域 %d 个 · 二级分组 %d 个 · 三级菜单 %d 个 · 合计 %d 项" % (n1, n2, n3, n1 + n2 + n3))
L.append("")
L.append("**一级域总览**")
L.append("")
L.append("| 序号 | 一级域（左栏图标） | 二级分组数 | 三级菜单数 |")
L.append("| ---: | --- | ---: | ---: |")
for i, (d, gs) in enumerate(stat, 1):
    L.append("| %d | %s | %d | %d |" % (i, d["title"], len(gs), sum(len(ms) for _, ms, _ in gs)))
L.append("")
L.append("---")
L.append("")

for i, (d, gs) in enumerate(stat, 1):
    L.append("## %d. %s" % (i, d["title"]))
    L.append("")
    for gname, ms, is_rest in gs:
        tag = "（未匹配分组，自动归入）" if is_rest else ""
        L.append("**二级分组：%s** %s" % (gname, tag))
        L.append("")
        L.append("| 序号 | 三级菜单 | 完整路由 |")
        L.append("| ---: | --- | --- |")
        for j, m in enumerate(ms, 1):
            L.append("| %d | %s | `%s` |" % (j, m["leaf"]["name"], m["full"]))
        L.append("")

L.append("---")
L.append("")
L.append("**附：侧边栏树形视图**")
L.append("")
L.append("```text")
for d, gs in stat:
    L.append("◆ " + d["title"])
    for gname, ms, is_rest in gs:
        suffix = "（其他）" if is_rest else ""
        L.append("  ▸ " + gname + suffix)
        for m in ms:
            L.append("      · " + m["leaf"]["name"] + "  （" + m["full"] + "）")
L.append("```")

io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(L).rstrip() + "\n")
print("written", OUT, os.path.getsize(OUT))
print("L1=%d L2=%d L3=%d" % (n1, n2, n3))
for d, gs in stat:
    print(d["title"], "->", [(g, len(ms)) for g, ms, _ in gs])
