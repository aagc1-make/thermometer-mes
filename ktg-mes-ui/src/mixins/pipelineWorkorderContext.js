// 流水线进入业务列表时保留工单上下文，兼容页面被 keep-alive 缓存的情况。
export default {
  created() {
    this._pipelineDefaultQuery = { ...this.queryParams }
    // Element 会把首次挂载时的工单编码记作表单初值；退出上下文后再次重置仍需覆盖它。
    const query = this.$route && this.$route.query || {}
    this._pipelineFormInitialized = /^\d+$/.test(String(query.pipelineWorkorderId || ''))
    this.applyPipelineWorkorderContext()
  },
  watch: {
    '$route.query.pipelineWorkorderId'() {
      if (this.applyPipelineWorkorderContext()) this.getList()
    },
    'queryParams.workorderCode'(value) {
      const query = this.$route && this.$route.query || {}
      if (this._pipelineContextApplied && value !== query.pipelineWorkorderCode) this.queryParams.workorderId = null
    }
  },
  methods: {
    applyPipelineWorkorderContext() {
      const query = this.$route && this.$route.query || {}
      const id = query.pipelineWorkorderId
      if (id && /^\d+$/.test(String(id))) {
        if (!this._pipelineContextApplied) this._pipelinePreviousQuery = { ...this.queryParams }
        Object.assign(this.queryParams, this._pipelineDefaultQuery)
        this.$set(this.queryParams, 'workorderId', String(id))
        this.$set(this.queryParams, 'workorderCode', query.pipelineWorkorderCode || null)
        // 排产列表默认仅显示已确认工单；查看历史步骤时也要能找到该已完工工单。
        this.queryParams.status = null
        this.queryParams.pageNum = 1
        this._pipelineContextApplied = true
        return true
      }
      if (this._pipelineContextApplied) {
        this.queryParams.workorderId = null
        this.queryParams.workorderCode = null
        Object.assign(this.queryParams, this._pipelinePreviousQuery)
        this.queryParams.pageNum = 1
        this._pipelineContextApplied = false
        return true
      }
      return false
    },
    clearPipelineWorkorderContext() {
      if (!this._pipelineContextApplied && !this._pipelineFormInitialized) return
      this.queryParams.workorderId = null
      this.queryParams.workorderCode = null
      Object.assign(this.queryParams, this._pipelineDefaultQuery)
      this._pipelineContextApplied = false
      if (this.$router && this.$route.query.pipelineWorkorderId) {
        const query = { ...this.$route.query }
        delete query.pipelineWorkorderId
        delete query.pipelineWorkorderCode
        this.$router.replace({ query }).catch(() => {})
      }
    }
  }
}
