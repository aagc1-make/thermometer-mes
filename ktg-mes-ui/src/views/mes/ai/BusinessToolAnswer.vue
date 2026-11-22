<template>
  <div class="business-tool-answer">
    <p v-if="answer || fallbackAnswer" class="tool-answer-text">{{ answer || fallbackAnswer }}</p>
    <details v-if="hasRows" class="tool-details">
      <summary>
        <span class="details-closed">展开查询明细</span>
        <span class="details-open">收起查询明细</span>
      </summary>
      <div v-for="(result, index) in results" :key="index" class="tool-result">
        <header>
          <h3>{{ result.label }}</h3>
          <span v-if="result.arguments.from">{{ result.arguments.from }} 至 {{ result.arguments.to }}</span>
          <time v-if="result.queriedAt" :datetime="result.queriedAt">查询于 {{ new Date(result.queriedAt).toLocaleString() }}</time>
        </header>
        <p v-if="result.arguments.query || result.arguments.workorderCode || result.arguments.itemCode" class="tool-filter">
          {{ result.arguments.query || result.arguments.workorderCode || result.arguments.itemCode }}
        </p>
        <el-table v-if="result.rows.length" :data="result.rows" size="mini" class="tool-result-table"
          :max-height="260" aria-label="数据库查询结果">
          <el-table-column v-for="column in result.columns" :key="column.prop" :prop="column.prop"
            :label="column.label" :min-width="textFields.includes(column.prop) ? 130 : 78" show-overflow-tooltip>
            <template slot-scope="{ row }">{{ format(row[column.prop]) }}</template>
          </el-table-column>
        </el-table>
        <p v-else class="tool-filter">本次查询没有匹配记录。</p>
        <p v-if="!page && result.total > result.rows.length" class="tool-filter">本表显示 {{ result.rows.length }} 条，共匹配 {{ result.total }} 条。</p>
      </div>
      <div v-if="page && page.last > 1" class="tool-pagination">
        <span>第 {{ page.offset + 1 }}–{{ page.offset + results[0].rows.length }} 条，共 {{ results[0].total }} 条</span>
        <button type="button" :disabled="busy || page.number <= 1" @click="$emit('page', page.number - 1)">上一页</button>
        <button type="button" :disabled="busy || page.number >= page.last" @click="$emit('page', page.number + 1)">下一页</button>
      </div>
    </details>
    <p v-if="notice" class="tool-filter">{{ notice }}</p>
    <button v-if="offerDetails" type="button" class="tool-next-query" :disabled="busy" @click="$emit('fetch-details')">查看入库明细</button>
  </div>
</template>

<script>
export default {
  name: 'BusinessToolAnswer',
  props: {
    answer: { type: String, default: '' },
    results: { type: Array, default: () => [] },
    notice: { type: String, default: '' },
    offerDetails: Boolean,
    busy: Boolean,
    page: { type: Object, default: null }
  },
  data() { return { textFields: ['itemCode', 'itemName', 'workorderCode', 'workorderName', 'productName', 'specification'] }},
  computed: {
    hasRows() { return this.results.some(result => result.rows.length > 0) },
    fallbackAnswer() {
      if (!this.results.length) return ''
      return this.results.map(result => result.total
        ? `${result.label}共匹配 ${result.total} 条记录`
        : `${result.label}未找到匹配记录`).join('；') + '。'
    }
  },
  methods: {
    format(value) {
      if (value === null || value === undefined || value === '') return '—'
      return typeof value === 'number' ? value.toLocaleString('zh-CN', { maximumFractionDigits: 10 }) : value
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.business-tool-answer { width: 860px; max-width: 100%; }
.tool-answer-text { margin: 0 0 6px; line-height: 1.65; white-space: pre-line; }
.tool-details > summary { color: $mes-primary; cursor: pointer; padding: 6px 0; width: fit-content; }
.tool-details:not([open]) .details-open,
.tool-details[open] .details-closed { display: none; }
.tool-next-query, .tool-pagination button {
  background: $mes-primary-soft;
  border: 1px solid $mes-border;
  border-radius: 6px;
  color: $mes-primary;
  cursor: pointer;
  padding: 6px 10px;
}
.tool-next-query:disabled, .tool-pagination button:disabled { cursor: default; opacity: .55; }
.tool-pagination { align-items: center; display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; color: $mes-text-sub; font-size: 12px; }
.tool-result + .tool-result { margin-top: 16px; }
.tool-result header { display: flex; flex-wrap: wrap; gap: 10px; align-items: baseline; }
.tool-result h3 { margin: 0; font-size: 15px; }
.tool-result header > span, .tool-result header > time, .tool-filter { color: $mes-text-sub; font-size: 12px; }
.tool-filter { margin: 3px 0; }
.tool-result-table { margin-top: 7px; border: 1px solid $mes-border; border-radius: 8px; font-variant-numeric: tabular-nums; }
.tool-result-table ::v-deep .el-table__body-wrapper { min-height: 0; }
</style>
