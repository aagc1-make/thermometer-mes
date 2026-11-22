<template>
  <div class="app-container">
    <el-alert
      v-if="!enrollmentEnabled"
      type="warning"
      :closable="false"
      show-icon
      title="人脸录入暂不可用"
      description="通常是未配置人脸模板加密密钥或专用人脸库未就绪；此时所有账号只能使用密码登录。"
    />

    <el-form :model="query" :inline="true" size="small" class="face-query" @submit.native.prevent="load">
      <el-form-item label="账号">
        <el-input v-model.trim="query.keyword" clearable placeholder="登录名或姓名" style="width: 180px" @keyup.enter.native="load" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.filter" clearable placeholder="全部" style="width: 140px">
          <el-option label="已录入" value="enrolled" />
          <el-option label="未录入" value="none" />
          <el-option label="已停用" value="disabled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
        <el-button icon="el-icon-refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" size="small" border>
      <el-table-column label="账号" prop="userName" width="130" show-overflow-tooltip />
      <el-table-column label="姓名" prop="nickName" width="120" show-overflow-tooltip />
      <el-table-column label="部门" prop="deptName" min-width="140" show-overflow-tooltip />
      <el-table-column label="人脸状态" width="220">
        <template slot-scope="scope">
          <el-tag v-if="!scope.row.enrolled" size="mini" type="info">未录入</el-tag>
          <template v-else>
            <el-tag size="mini" type="success">已录入</el-tag>
            <el-tag v-if="!scope.row.enabled" size="mini" type="warning">登录已停用</el-tag>
            <el-tag v-if="scope.row.needReenroll" size="mini" type="danger">算法过期待重录</el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="模板数" width="90" align="center">
        <template slot-scope="scope">{{ templateCount(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="算法" prop="algorithm" width="150" show-overflow-tooltip />
      <el-table-column label="样本/质量" width="120" align="center">
        <template slot-scope="scope">
          <span v-if="scope.row.enrolled">{{ scope.row.samples || 0 }} / {{ formatQuality(scope.row.quality) }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template slot-scope="scope">{{ formatTime(scope.row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.enrolled"
            v-hasPermi="['system:user:face:status']"
            type="text"
            size="small"
            @click="toggle(scope.row)"
          >{{ scope.row.enabled ? '停用登录' : '允许登录' }}</el-button>
          <el-button
            v-if="scope.row.enrolled"
            v-hasPermi="['system:user:face:remove']"
            type="text"
            size="small"
            class="mes-action--destructive"
            @click="resetTemplate(scope.row)"
          >重置模板</el-button>
        </template>
      </el-table-column>
      <template slot="empty">
        <span>{{ loading ? '正在加载…' : '没有匹配的账号' }}</span>
      </template>
    </el-table>

    <p class="face-caption">
      列表只展示录入元数据，不返回任何人脸特征；停用后该账号仍可用密码登录。
    </p>
  </div>
</template>

<script>
import { listFaceTemplates, changeFaceLoginStatus, resetFaceTemplate as resetTemplateApi } from '@/api/face'

export default {
  name: 'FaceAdmin',
  data() {
    return {
      loading: false,
      rows: [],
      enrollmentEnabled: true,
      query: { keyword: '', filter: '' }
    }
  },
  created() {
    this.load()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const response = await listFaceTemplates({ keyword: this.query.keyword, filter: this.query.filter })
        const data = response.data || {}
        this.rows = data.rows || []
        this.enrollmentEnabled = data.enrollmentEnabled !== false
      } catch (error) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    reset() {
      this.query = { keyword: '', filter: '' }
      this.load()
    },
    async toggle(row) {
      const enabled = !row.enabled
      const action = enabled ? '允许该账号使用人脸登录' : '停用该账号的人脸登录'
      try {
        await this.$confirm(action + '？停用后仍可使用密码登录。', '提示', { type: 'warning' })
      } catch (cancelled) {
        return
      }
      try {
        await changeFaceLoginStatus(row.userId, enabled)
        this.$modal.msgSuccess(enabled ? '已允许人脸登录' : '已停用人脸登录')
        this.load()
      } catch (error) {
        // 请求拦截器已提示
      }
    },
    async resetTemplate(row) {
      try {
        await this.$confirm('重置后将删除该账号的人脸特征，需要本人重新录入。是否继续？', '重置人脸模板', { type: 'warning' })
      } catch (cancelled) {
        return
      }
      try {
        await resetTemplateApi(row.userId)
        this.$modal.msgSuccess('人脸模板已重置')
        this.load()
      } catch (error) {
        // 请求拦截器已提示
      }
    },
    templateCount(row) {
      if (!row.enrolled) return 0
      return 1 + (row.altCount || 0)
    },
    formatQuality(value) {
      if (value === null || value === undefined || value === '') return '—'
      return Number(value).toFixed(2)
    },
    formatTime(value) {
      if (!value) return '—'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.face-query {
  margin-bottom: 4px;
}
.face-caption {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
}
</style>
