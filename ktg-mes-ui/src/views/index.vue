<template>
  <div class="dashboard-editor-container">
    <header class="home-heading">
      <p class="home-heading__system">红外测温枪 MES 系统</p>
      <h1>生产工作台</h1>
      <div class="home-heading__links">
        <a href="#home-workorder-pipeline" @click.prevent="focusSection('pipeline')">查看工单流水线</a>
        <a href="#home-confirmed-workorders" @click.prevent="focusSection('confirmed')">查看工单列表</a>
      </div>
    </header>

    <panel-group />
    <section id="home-workorder-pipeline" ref="pipeline" tabindex="-1" aria-label="工单流水线">
      <workorder-pipeline />
    </section>

    <section id="home-confirmed-workorders" ref="confirmed" class="home-section" tabindex="-1" aria-labelledby="home-production-title">
      <el-card class="production-card" shadow="never" :body-style="{ padding: '0' }">
        <div slot="header" class="production-card__header">
          <div>
            <h2 id="home-production-title">生产进度</h2>
            <p>已确认工单</p>
          </div>
          <el-button size="small" :aria-expanded="String(showDetails)" aria-controls="home-production-table" @click="showDetails = !showDetails">
            {{ showDetails ? '收起详细信息' : '展开详细信息' }}
            <i :class="showDetails ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" aria-hidden="true" />
          </el-button>
        </div>

        <el-alert v-if="loadError" class="production-card__error" title="工单加载失败，请重试" type="error" :closable="false" show-icon>
          <el-button type="text" @click="getList">重新加载</el-button>
        </el-alert>
        <div
          v-else
          id="home-production-table"
          class="production-card__scroll"
          role="region"
          aria-label="已确认工单表格"
          tabindex="0"
        >
          <el-table
            v-loading="loading"
            :class="{ 'production-card__table--expanded': showDetails }"
            :data="workorderList"
            :empty-text="loading ? '正在加载工单' : '暂无已确认工单'"
            row-key="workorderId"
            default-expand-all
            :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
          >
            <el-table-column label="工单 / 产品" min-width="190">
              <template slot-scope="{ row }">
                <div class="production-card__identity">
                  <el-button v-if="$auth.hasPermi('mes:pro:workorder:query')" size="mini" type="text" @click="handleView(row)">
                    {{ row.workorderCode }}
                  </el-button>
                  <strong v-else>{{ row.workorderCode }}</strong>
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="生产进度" min-width="122" align="right">
              <template slot-scope="{ row }">
                <div class="production-card__progress">
                  <el-progress :percentage="safeProgress(row.quantityProduced, row.quantity)" :stroke-width="5" :show-text="false" color="var(--color-progress)" />
                  <span>{{ safeProgress(row.quantityProduced, row.quantity) }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="工单数量" min-width="94" align="right" prop="quantity" />
            <el-table-column label="已生产数量" min-width="106" align="right" prop="quantityProduced" />
            <el-table-column label="需求日期" min-width="118">
              <template slot-scope="{ row }">
                {{ parseTime(row.requestDate, '{y}-{m}-{d}') }}
              </template>
            </el-table-column>
            <el-table-column label="单据状态" min-width="96">
              <template slot-scope="{ row }">
                <el-tag v-if="row.status === 'CONFIRMED'" class="production-card__status" size="mini" type="info">已确认</el-tag>
                <span v-else>{{ row.status || '未知' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="showDetails" label="订单编号" min-width="140" prop="sourceCode" show-overflow-tooltip />
            <el-table-column v-if="showDetails" label="工单名称" min-width="150" prop="workorderName" show-overflow-tooltip />
            <el-table-column v-if="showDetails" label="产品编号" width="120" prop="productCode" />
            <el-table-column v-if="showDetails" label="规格型号" width="140" prop="productSpc" show-overflow-tooltip />
            <el-table-column v-if="showDetails" label="单位" width="70" prop="unitOfMeasure" />
            <el-table-column v-if="showDetails" label="工序进度" width="320">
              <template slot-scope="{ row }">
                <div class="production-card__processes">
                  <div v-for="(item, index) in row.routeHomg" :key="index" class="production-card__process">
                    <el-progress :width="56" type="circle" :percentage="safeProgress(item.completeNumber, item.total)" :color="colors" />
                    <el-tooltip effect="dark" :content="item.processName" placement="bottom">
                      <span>{{ item.processName }}</span>
                    </el-tooltip>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <p v-if="workorderList.length" class="production-card__scroll-hint" :class="{ 'is-expanded': showDetails }">
          横向滚动查看全部列
        </p>
      </el-card>
    </section>

    <section v-if="chartList.length" class="home-section" aria-labelledby="home-reports-title">
      <h2 id="home-reports-title" class="home-section__title">报表</h2>
      <div class="home-reports">
        <el-card v-for="(item, index) in chartList" :key="item.chartId || index" shadow="never">
          <h3 slot="header" class="home-reports__title">{{ item.chartName }}</h3>
          <chart :chart-options="item.options" :api="item.api" />
        </el-card>
      </div>
    </section>
  </div>
</template>

<script>
import { getHomeList } from '@/api/mes/pro/workorder'
import { getMyCharts } from '@/api/mes/report/chart'
import Chart from './mes/report/chart/chart.vue'
import PanelGroup from './dashboard/PanelGroup'
import WorkorderPipeline from './dashboard/WorkorderPipeline'

export default {
  name: 'Index',
  components: { Chart, PanelGroup, WorkorderPipeline },
  data() {
    return {
      loading: true,
      loadError: false,
      showDetails: false,
      queryParams: { status: 'CONFIRMED' },
      workorderList: [],
      chartList: [],
      colors: [
        { color: '#4272c7', percentage: 99 },
        { color: '#4d7b64', percentage: 100 }
      ]
    }
  },
  created() {
    this.getList()
    this.getCharts()
  },
  methods: {
    focusSection(key) {
      const section = this.$refs[key]
      section.focus({ preventScroll: true })
      section.scrollIntoView({ block: 'start' })
    },
    getList() {
      this.loading = true
      this.loadError = false
      return getHomeList(this.queryParams).then(response => {
        this.workorderList = Array.isArray(response.data) ? response.data : []
      }).catch(() => {
        this.loadError = true
        this.workorderList = []
      }).finally(() => {
        this.loading = false
      })
    },
    safeProgress(done, total) {
      const n = Number(done)
      const d = Number(total)
      if (!Number.isFinite(n) || !Number.isFinite(d) || d <= 0) return 0
      return Math.min(100, Math.max(0, Math.round(n / d * 10000) / 100))
    },
    handleView(row) {
      if (row && row.workorderId) {
        this.$router.push({ path: '/mes/pro/workorder', query: { openWorkorderId: String(row.workorderId) } }).catch(() => {})
      }
    },
    getCharts() {
      if (!this.$auth.hasPermi('mes:report:chart:list')) return
      getMyCharts().then(response => {
        this.chartList = response.data.map(item => {
          if (typeof item.options !== 'string') return item
          let options
          try {
            options = JSON.parse(item.options)
          } catch (error) {
            options = {}
          }
          return { ...item, options }
        })
      }).catch(() => { this.chartList = [] })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  display: grid;
  gap: 20px;
  min-width: 0;
  padding: 24px clamp(12px, 2.2vw, 32px) 36px;
  color: var(--color-text);
  background: var(--color-bg);
}

.home-heading {
  padding: 4px 0;

  h1 {
    margin: 5px 0 6px;
    font-size: clamp(25px, 2.4vw, 34px);
    line-height: 1.22;
    font-weight: 700;
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 13px;
    line-height: 1.6;
  }

  &__system {
    font-weight: 600;
  }
}

.home-section,
.home-reports > .el-card {
  min-width: 0;
}

.home-section__title,
.production-card__header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
}

.production-card {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;

    p {
      margin: 3px 0 0;
      color: var(--color-text-secondary);
      font-size: 12px;
    }
  }

  &__scroll {
    overflow-x: auto;
  }

  &__scroll:focus-visible {
    outline-offset: -2px;
  }

  .el-table {
    min-width: 726px;
    font-variant-numeric: tabular-nums;
  }

  .production-card__table--expanded {
    min-width: 1666px;
  }

  &__scroll-hint {
    display: none;
    margin: 0;
    padding: 7px 12px;
    border-top: 1px solid var(--color-border);
    color: var(--color-text-secondary);
    font-size: 12px;
  }

  &__identity {
    display: grid;
    gap: 2px;
    min-width: 0;
    text-align: left;

    .el-button,
    strong {
      justify-self: start;
      max-width: 100%;
      padding: 2px 0;
      color: var(--color-primary);
      font-weight: 600;
      overflow-wrap: anywhere;
      white-space: normal;
      text-align: left;
    }

    span {
      color: var(--color-text-secondary);
      font-size: 12px;
      overflow-wrap: anywhere;
    }
  }

  &__progress {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: 6px;
    color: var(--color-text);
    font-size: 12px;
  }

  &__status {
    color: var(--color-primary);
    background: var(--color-primary-bg);
    border-color: transparent;
  }

  &__processes {
    display: flex;
    gap: 12px;
    overflow-x: auto;
  }

  &__process {
    display: grid;
    flex: 0 0 64px;
    justify-items: center;
    gap: 4px;

    span {
      max-width: 64px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__error {
    margin: 12px;
    width: auto;
  }
}

.home-reports {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 16px;
  margin-top: 12px;

  &__title {
    margin: 0;
    font-size: 15px;
    line-height: 1.5;
  }
}

@media (max-width: 1860px) {
  .production-card__scroll-hint.is-expanded {
    display: block;
  }
}

@media (max-width: 800px) {
  .production-card__scroll-hint {
    display: block;
  }
}

@media (max-width: 600px) {
  .dashboard-editor-container {
    gap: 16px;
    padding-top: 16px;
  }

  .production-card ::v-deep .el-card__header,
  .home-reports ::v-deep .el-card__header,
  .home-reports ::v-deep .el-card__body {
    padding: 12px;
  }
}

.home-heading__links { display: flex; flex-wrap: wrap; gap: 8px 18px; margin-top: 12px; }
.home-heading__links a { color: var(--mes-accent); text-decoration: underline; }
#home-workorder-pipeline, #home-confirmed-workorders { scroll-margin-top: calc(var(--mes-header-height) + 50px); }
</style>
