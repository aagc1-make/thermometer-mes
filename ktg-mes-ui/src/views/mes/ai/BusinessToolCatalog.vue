<template>
  <el-dialog title="AI 只读工具" :visible.sync="visible" width="630px" append-to-body custom-class="mes-knowledge-dialog">
    <div v-loading="loading" class="tool-catalog">
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <details v-for="tool in tools" :key="tool.code" class="catalog-tool">
        <summary>{{ tool.label }} <i class="el-icon-arrow-down" aria-hidden="true" /></summary>
        <p>{{ tool.description }}</p>
        <p class="catalog-permission">权限 {{ tool.permission }}</p>
        <dl>
          <div v-for="(parameter, field) in tool.schema.properties" :key="field">
            <dt>{{ field }}{{ tool.schema.required.includes(field) ? ' · 必填' : '' }}</dt>
            <dd>{{ parameter.description || (parameter.enum || []).join(' / ') || parameter.type }}
              <span v-if="parameter.maximum">（{{ parameter.minimum }} 至 {{ parameter.maximum }}）</span>
            </dd>
          </div>
        </dl>
      </details>
    </div>
  </el-dialog>
</template>

<script>
import { listBusinessTools } from '@/api/mes/assistant'

export default {
  name: 'BusinessToolCatalog',
  data() { return { visible: false, loading: false, error: '', tools: [] }},
  methods: {
    async open() {
      this.visible = true
      this.loading = true
      this.error = ''
      try {
        const { data } = await listBusinessTools()
        this.tools = data
      } catch (error) {
        this.error = error.message || '工具清单暂时无法读取。'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.tool-catalog { min-height: 36px; max-height: 60vh; overflow: auto; }
.catalog-tool { border-bottom: 1px solid $mes-border; padding: 8px 0; }
.catalog-tool summary { cursor: pointer; color: $mes-primary; font-weight: 600; }
.catalog-tool p { margin: 6px 0; }
.catalog-permission { color: $mes-text-sub; font-size: 12px; word-break: break-word; }
.catalog-tool dl { background: $mes-primary-soft; margin: 6px 0 0; padding: 6px 10px; border-radius: 6px; }
.catalog-tool dl div { display: flex; align-items: baseline; flex-wrap: wrap; gap: 6px; padding: 3px 0; }
.catalog-tool dt { font-weight: 600; font-size: 12px; }
.catalog-tool dd { margin: 0; color: $mes-text-sub; font-size: 12px; }
</style>
