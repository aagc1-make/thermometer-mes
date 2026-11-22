<template>
  <section class="department-libraries" aria-label="部门知识库">
    <div class="department-libraries__header">
      <h3>部门知识库</h3>
      <el-button v-if="canCreate" type="primary" size="small" icon="el-icon-plus" @click="openCreate">
        创建部门库
      </el-button>
    </div>
    <div class="department-libraries__content" aria-live="polite">
      <span v-if="loading">正在加载部门知识库…</span>
      <template v-else-if="loadError">
        <span role="alert">{{ loadError }}</span>
        <el-button type="text" size="small" @click="loadLibraries">重试</el-button>
      </template>
      <span v-else-if="!libraries.length">{{ canCreate ? '暂无已建部门库。' : '当前账号暂无可用的部门知识库。' }}</span>
      <template v-else>
        <div v-if="canCreate" class="department-libraries__scope">
          <label for="department-library-scope">查看部门</label>
          <el-select id="department-library-scope" v-model="selectedDeptId" size="small" filterable>
            <el-option v-for="library in libraries" :key="library.libraryId" :label="formatDepartment(library)" :value="library.deptId" />
          </el-select>
        </div>
        <div v-if="selectedLibrary" class="department-libraries__summary">
          <strong>{{ selectedLibrary.name }}</strong>
          <span>{{ selectedLibrary.deptName }}</span>
          <el-tag v-if="canCreate" size="mini" :type="selectedLibrary.status === 'enabled' ? 'success' : 'info'">
            {{ selectedLibrary.status === 'enabled' ? '已启用' : '未启用' }}
          </el-tag>
        </div>
      </template>
    </div>

    <el-dialog
      title="创建部门知识库"
      :visible.sync="creating"
      width="480px"
      append-to-body
      custom-class="mes-knowledge-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <el-form :model="form" label-width="104px" size="small" @submit.native.prevent="saveLibrary">
        <el-form-item label="归属部门" for="department-library-dept" required>
          <el-select
            id="department-library-dept"
            v-model="form.deptId"
            filterable
            placeholder="选择部门"
            :loading="loadingDepartments"
            :disabled="loadingDepartments || !!departmentError || saving"
            class="department-libraries__select"
          >
            <el-option
              v-for="dept in departments"
              :key="dept.deptId"
              :label="dept.label"
              :value="dept.deptId"
              :disabled="dept.hasLibrary"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库名称" for="department-library-name" required>
          <el-input
            id="department-library-name"
            v-model="form.name"
            maxlength="120"
            placeholder="例如：质量部知识库"
            :disabled="saving"
          />
        </el-form-item>
      </el-form>
      <el-alert v-if="departmentError || createError" :title="departmentError || createError" :closable="false" type="error" show-icon />
      <span slot="footer">
        <el-button v-if="departmentError" size="small" :disabled="loadingDepartments" @click="loadDepartments">重试加载部门</el-button>
        <el-button size="small" :disabled="saving" @click="creating = false">取消</el-button>
        <el-button
          size="small"
          type="primary"
          :loading="saving"
          :disabled="loadingDepartments || !!departmentError"
          @click="saveLibrary"
        >确认创建</el-button>
      </span>
    </el-dialog>
  </section>
</template>

<script>
import { listDepartmentLibraries, listAdminDepartmentLibraries, createDepartmentLibrary } from '@/api/mes/knowledgeLibrary'
import { listDept } from '@/api/system/dept'

export default {
  name: 'DepartmentLibraries',
  data() {
    return {
      libraries: [],
      selectedDeptId: null,
      loading: true,
      loadError: '',
      listRequest: 0,
      creating: false,
      departments: [],
      loadingDepartments: false,
      departmentError: '',
      form: { deptId: null, name: '' },
      saving: false,
      createError: ''
    }
  },
  computed: {
    canCreate() {
      return this.$auth.hasRole('admin')
    },
    selectedLibrary() {
      if (!this.canCreate) return this.libraries[0]
      return this.libraries.find(library => library.deptId === this.selectedDeptId)
    }
  },
  methods: {
    async loadLibraries(preferredDeptId = this.selectedDeptId) {
      const request = ++this.listRequest
      this.loading = true
      this.loadError = ''
      try {
        const { data } = await (this.canCreate ? listAdminDepartmentLibraries() : listDepartmentLibraries())
        if (request === this.listRequest) {
          this.libraries = data
          const selected = data.find(library => library.deptId === preferredDeptId) || data[0]
          this.selectedDeptId = selected ? selected.deptId : null
        }
      } catch {
        if (request === this.listRequest) {
          this.libraries = []
          this.loadError = '部门知识库加载失败，请重试。'
        }
      } finally {
        if (request === this.listRequest) this.loading = false
      }
    },
    formatDepartment(dept) {
      return dept.deptName + '（编号 ' + dept.deptId + '）'
    },
    openCreate() {
      this.form = { deptId: null, name: '' }
      this.createError = ''
      this.creating = true
      return this.loadDepartments()
    },
    async loadDepartments() {
      this.loadingDepartments = true
      this.departmentError = ''
      try {
        const [departmentResponse, libraryResponse] = await Promise.all([listDept(), listAdminDepartmentLibraries()])
        const names = new Map(departmentResponse.data.map(dept => [String(dept.deptId), dept.deptName]))
        const existing = new Set(libraryResponse.data.map(library => library.deptId))
        this.departments = departmentResponse.data.filter(dept => dept.status === '0').map(dept => {
          const path = (dept.ancestors || '').split(',').map(id => names.get(id)).filter(Boolean)
          const hasLibrary = existing.has(dept.deptId)
          const label = [...path, this.formatDepartment(dept)].join(' / ') + (hasLibrary ? '（已建库）' : '')
          return { deptId: dept.deptId, label, hasLibrary }
        })
      } catch {
        this.departments = []
        this.departmentError = '部门列表加载失败，请重试。'
      } finally {
        this.loadingDepartments = false
      }
    },
    async saveLibrary() {
      if (this.saving || this.loadingDepartments || this.departmentError) return
      const { deptId } = this.form
      const name = this.form.name.trim()
      if (!deptId || !name) {
        this.createError = '请选择归属部门并填写知识库名称。'
        return
      }
      this.createError = ''
      this.saving = true
      try {
        await createDepartmentLibrary({ deptId, name })
        this.creating = false
        this.$message.success('部门知识库已创建')
        await this.loadLibraries(deptId)
      } catch (error) {
        this.createError = error.message || '创建失败，请查看系统提示后重试。'
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.department-libraries {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}
.department-libraries__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;

  h3 { margin: 0; color: #1f2937; font-size: 15px; font-weight: 600; }
}
.department-libraries__content {
  color: #4b5563;
  line-height: 1.6;
  .el-button { margin-left: 8px; }
}
.department-libraries__scope,
.department-libraries__summary {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 4px 12px;
  overflow-wrap: anywhere;
  strong { color: #1f2937; font-weight: 600; }
}
.department-libraries__scope { margin-bottom: 8px; }
.department-libraries__select { width: 100%; }
</style>
