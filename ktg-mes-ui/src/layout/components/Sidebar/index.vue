<template>
    <div :class="{'has-logo':showLogo}" class="sidebar-light">
        <logo v-if="showLogo" :collapse="false" />
        <el-scrollbar class="theme-light" wrap-class="scrollbar-wrapper">
            <el-menu
                :default-active="activeMenu"
                :collapse="false"
                :default-openeds="openeds"
                background-color="#ffffff"
                text-color="#303133"
                :unique-opened="false"
                active-text-color="#2563eb"
                :collapse-transition="false"
                mode="vertical"
                role="navigation"
                aria-label="业务导航菜单"
            >
                <sidebar-item
                    v-for="(route, index) in groupedRouters"
                    :key="route.path  + index"
                    :item="route"
                    :base-path="route.path"
                />
            </el-menu>
        </el-scrollbar>
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import Logo from "./Logo";
import SidebarItem from "./SidebarItem";
import variables from "@/assets/styles/variables.scss";
import { findDomainByPath, isMenuHidden } from "../../rail-config";

export default {
    components: { SidebarItem, Logo },
    computed: {
        ...mapState(["settings"]),
        ...mapGetters(["sidebarRouters", "sidebar", "device"]),
        activeMenu() {
            const route = this.$route;
            const { meta, path } = route;
            // if set path, the sidebar will highlight the path you set
            if (meta.activeMenu) {
                return meta.activeMenu;
            }
            return path;
        },
        showLogo() {
            return this.$store.state.settings.sidebarLogo;
        },
        // 只展示当前域（左栏选中项）下的菜单，形成「左总右分」结构
        visibleRouters() {
            const domain = findDomainByPath(this.$route.path);
            return (this.sidebarRouters || []).filter(r =>
                domain.roots.some(p => r.path === p || r.path.indexOf(p + "/") === 0)
            );
        },
        /**
         * 按 rail-config 中该域的 groups，把菜单重组成「母目录 + 子菜单」两段结构。
         * 只在前端渲染层重组，不改 sys_menu / 权限标识。
         */
        groupedRouters() {
            const domain = findDomainByPath(this.$route.path);
            if (!domain.groups || !domain.groups.length) {
                return this.visibleRouters;
            }
            const leaves = [];
            const join = (base, p) => {
                if (!p) return base || '/';
                if (String(p).charAt(0) === '/') return p;
                return (base === '/' ? '' : base || '') + '/' + p;
            };
            // 收集叶子菜单并计算「绝对路径」，否则外层合成节点会污染 base-path 导致 404
            const collect = (list, base) => {
                (list || []).forEach(r => {
                    if (r.hidden) return;
                    const full = join(base, r.path);
                    if (r.children && r.children.length) {
                        collect(r.children, full);
                    } else if (r.meta && r.meta.title && !isMenuHidden(full)) {
                        leaves.push({ full, route: r });
                    }
                });
            };
            collect(this.visibleRouters, '');

            const used = new Set();
            const groups = [];
            domain.groups.forEach((g, gi) => {
                const name = g[0];
                const keys = g[1] || [];
                const matched = leaves.filter(l => keys.indexOf(String(l.full).split("/").pop()) > -1);
                if (!matched.length) return;
                matched.forEach(m => used.add(m.full));
                groups.push({
                    path: "/_grp_" + domain.key + "_" + gi,
                    meta: { title: name, icon: "" },
                    children: matched.map(m => ({ ...m.route, path: m.full }))
                });
            });
            const rest = leaves.filter(l => !used.has(l.full));
            if (rest.length) {
                groups.push({
                    path: "/_grp_" + domain.key + "_rest",
                    meta: { title: "其他", icon: "" },
                    children: rest.map(m => ({ ...m.route, path: m.full }))
                });
            }
            return groups;
        },
        variables() {
            return variables;
        },
        isCollapse() {
            return this.device === 'mobile' ? !this.sidebar.opened : false;
        },
        // 分组样式：默认展开当前域下的全部母目录（渲染后的分组节点），呈现「分组标题 + 组内菜单」
        openeds() {
            return (this.groupedRouters || [])
                .filter(r => r.children && r.children.length > 0)
                .map(r => r.path);
        }
    }
};
</script>
