<template>
  <div class="app-container kb-management">
    <header class="kb-header">
      <div>
        <h1 class="mes-page-heading">知识库管理</h1>
        <p>上传部门资料，查看解析结果并管理文档版本。</p>
      </div>
      <div class="kb-actions">
        <el-button v-if="libraryId" size="small" icon="el-icon-share"
          @click="$router.push({ path: '/mes/ai/graph', query: { libraryId: String(libraryId) } })">浏览图谱</el-button>
        <el-button size="small" icon="el-icon-reading" @click="$refs.local.open()">浏览器资料</el-button>
        <el-button v-if="admin" size="small" icon="el-icon-setting" @click="settingsVisible = true">部门库设置</el-button>
        <el-button v-if="catalog && catalog.library.maintain" size="small" type="primary" icon="el-icon-upload2" @click="openUpload()">上传文档</el-button>
      </div>
    </header>

    <div v-if="admin && libraries.length" class="kb-scope">
      <label for="kb-library-scope">查看部门</label>
      <el-select id="kb-library-scope" v-model="libraryId" size="small" filterable @change="changeLibrary">
        <el-option v-for="library in libraries" :key="library.libraryId" :value="library.libraryId"
          :label="library.deptName + '（' + library.deptId + '）'" :disabled="library.status !== 'enabled'" />
      </el-select>
    </div>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon>
      <el-button type="text" size="small" @click="loadLibraries">重新加载</el-button>
    </el-alert>
    <section v-else-if="catalog" class="kb-catalog" v-loading="loading">
      <div class="kb-metrics" aria-label="按文档状态筛选">
        <button v-for="metric in metrics" :key="metric.key" type="button" class="kb-metric"
          :aria-pressed="filter === metric.key" @click="filter = metric.key; tablePage = 1">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.count }}</strong>
        </button>
      </div>
      <div class="kb-table-card">
        <div class="kb-table-top">
          <h2>{{ catalog.library.name }}</h2>
          <el-input v-model.trim="search" size="small" clearable prefix-icon="el-icon-search"
            placeholder="搜索文档名称或分类" aria-label="搜索文档" @input="tablePage = 1" />
        </div>
        <el-table :data="visibleRows" row-key="docId" :empty-text="catalog.documents.length ? '没有匹配的文档' : '部门库还没有文档'">
          <el-table-column label="文档" min-width="210">
            <template slot-scope="{ row }">
              <strong class="kb-doc-title">{{ row.title }}</strong>
              <span class="kb-filename">{{ row.filename }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="分类" min-width="100" />
          <el-table-column label="版本" min-width="130">
            <template slot-scope="{ row }">
              <span>{{ row.currentVersion || row.latestVersion }}</span>
              <span v-if="row.currentVersion && row.currentVersion !== row.latestVersion" class="kb-filename">
                {{ row.latestVersion }} {{ row.parseStatus === 'failed' ? '解析失败' : '解析中' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="处理进度" min-width="280">
            <template slot-scope="{ row }">
              <div class="kb-stages" :aria-label="row.parseStatus === 'completed' ? '已解析' : '正在处理'">
                <el-tooltip v-if="row.parseStatus === 'failed'" :content="row.parseError || '解析失败'" placement="top">
                  <span class="is-failed" tabindex="0">解析异常</span>
                </el-tooltip>
                <span v-else :class="row.parseStatus === 'completed' ? 'is-completed' : 'is-current'">
                  <i :class="row.parseStatus === 'completed' ? 'el-icon-check' : 'el-icon-loading'" aria-hidden="true" /> 解析
                </span>
                <el-tooltip v-if="row.embedStatus === 'failed'" :content="row.embedError || '向量化暂时失败，系统会重试。'" placement="top">
                  <span class="is-failed" tabindex="0">向量化异常</span>
                </el-tooltip>
                <span v-else :class="{ 'is-completed': row.embedStatus === 'completed', 'is-current': row.embedStatus === 'running' }"
                  :title="row.embedStatus ? '文档向量索引' : '等待嵌入模型和向量库'">
                  <i v-if="row.embedStatus === 'running'" class="el-icon-loading" aria-hidden="true" />
                  <i v-if="row.embedStatus === 'completed'" class="el-icon-check" aria-hidden="true" /> 向量化
                </span>
                <el-tooltip v-if="row.graphStatus === 'failed'" :content="row.graphError || '图谱抽取失败'" placement="top">
                  <span class="is-failed" tabindex="0">图谱异常</span>
                </el-tooltip>
                <span v-else :class="{ 'is-completed': row.graphStatus === 'completed', 'is-current': row.graphStatus === 'running' }"
                  :title="row.graphEnabled && !graphOnline ? '等待图谱服务' : ''">
                  <i v-if="row.graphStatus === 'running'" class="el-icon-loading" aria-hidden="true" />
                  <i v-if="row.graphStatus === 'completed'" class="el-icon-check" aria-hidden="true" />
                  {{ !row.graphEnabled ? '跳过图谱' : row.graphStatus === 'completed' ? '已建图' : '图谱抽取' }}
                </span>
                <span :class="{ 'is-completed': isReady(row) }"
                  :title="isReady(row) ? '可用作关键词与向量检索' : '等待后续入库处理'">就绪</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="248">
            <template slot-scope="{ row }">
              <el-button v-if="row.currentVersionId" type="text" size="small" @click="openPreview(row)">预览</el-button>
              <el-button v-if="row.latestVersionId" type="text" size="small" @click="download(row)">下载原件</el-button>
              <template v-if="catalog.library.maintain">
                <el-button v-if="row.parseStatus === 'failed'" type="text" size="small" @click="retry(row)">重试</el-button>
                <el-button v-if="row.graphStatus === 'failed'" type="text" size="small" @click="retry(row, true)">重试建图</el-button>
                <el-button v-if="row.parseStatus !== 'pending'" type="text" size="small" @click="openUpload(row)">上传新版</el-button>
                <el-button type="text" size="small" class="mes-action--destructive" @click="remove(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="kb-table-footer">
          <span>文档 {{ catalog.documents.length }} / {{ catalog.documentLimit }} · 已发布正文 {{ catalog.characters.toLocaleString() }} / {{ catalog.characterLimit.toLocaleString() }} 字符</span>
          <el-pagination v-if="filteredRows.length > pageSize" layout="prev, pager, next" small
            :total="filteredRows.length" :page-size="pageSize" :current-page.sync="tablePage" />
        </div>
      </div>
    </section>
    <section v-else class="kb-empty" v-loading="loading">
      <h2>{{ loading ? '正在加载部门知识库…' : '暂无可用部门库' }}</h2>
      <p v-if="!loading">{{ admin ? '可以在部门库设置中创建。' : '请联系管理员为本部门启用知识库。' }}</p>
    </section>

    <el-dialog :title="form.docId ? '上传新版本' : '上传文档'" :visible.sync="uploadVisible"
      width="550px" append-to-body custom-class="mes-knowledge-dialog" :close-on-click-modal="false"
      :close-on-press-escape="!uploadBusy" :show-close="!uploadBusy">
      <el-form label-width="104px" size="small" @submit.native.prevent="submit">
        <el-form-item label="原件" for="kb-upload-file" required>
          <input id="kb-upload-file" :key="uploadKey" type="file" accept=".txt,.md,.csv,.pdf,.docx,.xlsx,.html"
            :disabled="uploadBusy" @change="selectFile">
          <p class="kb-caption">TXT、MD、CSV、PDF、DOCX、XLSX、HTML，单文件最多 50 MB。</p>
        </el-form-item>
        <el-form-item label="文档名称" for="kb-upload-title" required>
          <el-input id="kb-upload-title" v-model.trim="form.title" :disabled="!!form.docId || uploadBusy" maxlength="120" />
        </el-form-item>
        <el-form-item label="分类" for="kb-upload-category" required>
          <el-select id="kb-upload-category" v-model="form.category" :disabled="!!form.docId || uploadBusy" @change="categoryChanged">
            <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" for="kb-upload-version" required>
          <el-input id="kb-upload-version" v-model.trim="form.version" placeholder="例如 V2.0" :disabled="uploadBusy" maxlength="40" />
        </el-form-item>
        <el-form-item label="纳入知识图谱" for="kb-upload-graph">
          <el-switch id="kb-upload-graph" v-model="form.graphEnabled" :disabled="!!form.docId || uploadBusy" />
          <p v-if="form.graphEnabled" class="kb-graph-note">解析后抽取 MES 实体和关系，并保留来源分块。</p>
        </el-form-item>
      </el-form>
      <el-alert v-if="uploadError" :title="uploadError" type="error" :closable="false" show-icon />
      <template #footer>
        <el-button size="small" :disabled="uploadBusy" @click="uploadVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="uploadBusy" @click="submit">确认上传</el-button>
      </template>
    </el-dialog>

    <el-drawer :title="preview.title || '正文预览'" :visible.sync="previewVisible" size="min(550px, 100vw)" append-to-body>
      <div class="kb-preview" v-loading="previewBusy">
        <p v-if="preview.error" role="alert">{{ preview.error }}</p>
        <section v-for="chunk in preview.chunks" :key="chunk.chunkIndex" class="kb-preview-chunk">
          <h3>{{ chunk.sectionLabel }} · 片段 {{ chunk.chunkIndex + 1 }}</h3>
          <pre>{{ chunk.content }}</pre>
        </section>
        <el-pagination v-if="preview.total > preview.pageSize" layout="prev, pager, next" small
          :total="preview.total" :page-size="preview.pageSize" :current-page.sync="preview.page" @current-change="loadPreview" />
      </div>
    </el-drawer>

    <el-dialog v-if="admin" title="部门库设置" :visible.sync="settingsVisible" width="560px" append-to-body
      custom-class="mes-knowledge-dialog" @open="openSettings" @closed="loadLibraries">
      <department-libraries ref="settings" />
    </el-dialog>
    <knowledge-base ref="local" :user-id="$store.state.user.id" />
  </div>
</template>

<script>
import { saveAs } from 'file-saver'
import { getEngineStatus } from '@/api/mes/assistant'
import DepartmentLibraries from './DepartmentLibraries'
import KnowledgeBase from './KnowledgeBase'
import {
  listDepartmentLibraries, listAdminDepartmentLibraries, listLibraryDocuments, uploadLibraryDocument,
  previewLibraryDocument, downloadLibraryOriginal, retryLibraryDocument, deleteLibraryDocument, retryLibraryGraph
} from '@/api/mes/knowledgeLibrary'

const categories = ['检验规范', 'BOM 说明', '工艺说明', '操作手册', '其他']

export default {
  name: 'KnowledgeManagement',
  components: { DepartmentLibraries, KnowledgeBase },
  data() {
    return {
      categories, libraries: [], libraryId: null, catalog: null, loading: true, error: '', catalogRequest: 0,
      filter: 'all', search: '', tablePage: 1, pageSize: 10, pollTimer: null, settingsVisible: false, vectorEnabled: false, graphOnline: false,
      uploadVisible: false, uploadBusy: false, uploadError: '', uploadKey: 0, file: null, form: {},
      previewVisible: false, previewBusy: false, previewRequest: 0, preview: { chunks: [] }
    }
  },
  computed: {
    admin() { return this.$auth.hasRole('admin') },
    metrics() {
      const rows = this.catalog.documents
      return [
        { key: 'all', label: '全部文档', count: rows.length },
        { key: 'ready', label: '已入库就绪', count: rows.filter(this.isReady).length },
        { key: 'graph', label: '已纳入图谱', count: rows.filter(row => row.graphStatus === 'completed').length },
        { key: 'failed', label: '解析异常', count: rows.filter(row => row.parseStatus === 'failed').length }
      ]
    },
    filteredRows() {
      const text = this.search.toLowerCase()
      return this.catalog.documents.filter(row => {
        const matches = `${row.title} ${row.category} ${row.filename}`.toLowerCase().includes(text)
        if (!matches) return false
        if (this.filter === 'ready') return this.isReady(row)
        if (this.filter === 'graph') return row.graphStatus === 'completed'
        if (this.filter === 'failed') return row.parseStatus === 'failed'
        return true
      })
    },
    visibleRows() {
      const start = (this.tablePage - 1) * this.pageSize
      return this.filteredRows.slice(start, start + this.pageSize)
    }
  },
  activated() { this.loadLibraries() },
  mounted() { if (!this.$vnode.data.keepAlive) this.loadLibraries() },
  deactivated() { this.stopPolling() },
  beforeDestroy() { this.stopPolling() },
  methods: {
    isReady(row) { return row.embedStatus === 'completed' && (!row.graphEnabled || row.graphStatus === 'completed') },
    async loadLibraries() {
      this.loading = true
      this.error = ''
      try {
        const [{ data }, status] = await Promise.all([
          this.admin ? listAdminDepartmentLibraries() : listDepartmentLibraries(),
          getEngineStatus().catch(() => ({ data: {} }))
        ])
        this.vectorEnabled = status.data.vectorDb === 'online' && status.data.embedding === 'online'
        this.graphOnline = status.data.graphDb === 'online'
        this.libraries = data
        const enabled = data.filter(library => library.status === 'enabled')
        const selected = enabled.find(library => library.libraryId === this.libraryId) || enabled[0]
        this.libraryId = selected ? selected.libraryId : null
        await this.loadCatalog()
      } catch (error) {
        this.error = error.message || '知识库加载失败'
      } finally {
        this.loading = false
      }
    },
    changeLibrary() {
      this.catalog = null
      this.filter = 'all'
      this.search = ''
      this.tablePage = 1
      this.previewVisible = false
      this.uploadVisible = false
      this.loadCatalog()
    },
    stopPolling() {
      clearTimeout(this.pollTimer)
      this.catalogRequest++
    },
    async loadCatalog() {
      this.stopPolling()
      const request = this.catalogRequest
      if (!this.libraryId) {
        this.catalog = null
        return
      }
      this.error = ''
      this.loading = !this.catalog
      try {
        const { data } = await listLibraryDocuments(this.libraryId)
        if (request !== this.catalogRequest) return
        this.catalog = data
        this.tablePage = Math.max(1, Math.min(this.tablePage, Math.ceil(this.filteredRows.length / this.pageSize)))
        const parsing = data.documents.some(row => row.parseStatus === 'pending')
        const indexing = this.vectorEnabled && data.documents.some(row => row.currentVersionId && row.embedStatus !== 'completed')
        const graphing = this.graphOnline && data.documents.some(row => row.currentVersionId && row.graphEnabled
          && row.graphStatus !== 'completed' && row.graphStatus !== 'failed')
        if (parsing || indexing || graphing) {
          this.pollTimer = setTimeout(this.loadCatalog, parsing ? 1500 : 4000)
        }
      } catch (error) {
        if (request === this.catalogRequest) this.error = error.message || '文档列表加载失败'
      } finally {
        if (request === this.catalogRequest) this.loading = false
      }
    },
    openSettings() {
      this.$nextTick(() => this.$refs.settings.loadLibraries())
    },
    openUpload(row) {
      this.form = row ? { docId: row.docId, title: row.title, category: row.category, graphEnabled: row.graphEnabled, version: '' }
        : { title: '', category: '其他', graphEnabled: false, version: 'V1.0' }
      this.file = null
      this.uploadError = ''
      this.uploadKey++
      this.uploadVisible = true
    },
    selectFile(event) {
      this.file = event.target.files[0] || null
      if (this.file && !this.form.title) this.form.title = this.file.name.replace(/\.[^.]+$/, '').slice(0, 120)
    },
    categoryChanged(category) { this.form.graphEnabled = categories.slice(0, 3).includes(category) },
    async submit() {
      if (this.uploadBusy) return
      this.uploadError = ''
      if (!this.file || !this.file.size || this.file.size > 50 * 1024 * 1024 || !this.form.title || !this.form.version) {
        this.uploadError = '请选择不超过 50 MB 的非空文件，并填写名称与版本号。'
        return
      }
      const body = new FormData()
      for (const [key, value] of Object.entries(this.form)) body.append(key, value)
      body.append('file', this.file)
      this.uploadBusy = true
      try {
        await uploadLibraryDocument(this.libraryId, body)
        this.uploadVisible = false
        this.filter = 'all'
        this.search = ''
        this.tablePage = 1
        this.$message.success('原件已上传，正在解析')
        await this.loadCatalog()
      } catch (error) {
        this.uploadError = error.message || '上传失败，请重试'
      } finally {
        this.uploadBusy = false
      }
    },
    openPreview(row) {
      this.preview = { title: row.title + ' · ' + row.currentVersion, versionId: row.currentVersionId, chunks: [], page: 1 }
      this.previewVisible = true
      this.loadPreview()
    },
    async loadPreview() {
      const request = ++this.previewRequest
      this.previewBusy = true
      this.$set(this.preview, 'error', '')
      try {
        const { data } = await previewLibraryDocument(this.libraryId, this.preview.versionId, this.preview.page)
        if (request === this.previewRequest) this.preview = { ...this.preview, ...data }
      } catch (error) {
        if (request === this.previewRequest) this.$set(this.preview, 'error', error.message || '正文加载失败')
      } finally {
        if (request === this.previewRequest) this.previewBusy = false
      }
    },
    async download(row) {
      try {
        const blob = await downloadLibraryOriginal(this.libraryId, row.currentVersionId || row.latestVersionId)
        if (blob.type.includes('json')) {
          throw new Error(JSON.parse(await blob.text()).msg || '下载失败')
        }
        saveAs(blob, row.filename)
      } catch (error) {
        this.$message.error(error.message || '下载失败，请重试')
      }
    },
    async retry(row, graph = false) {
      try {
        if (graph) {
          await retryLibraryGraph(this.libraryId, row.currentVersionId)
          this.graphOnline = (await getEngineStatus()).data.graphDb === 'online'
        } else {
          await retryLibraryDocument(this.libraryId, row.latestVersionId)
        }
        await this.loadCatalog()
      } catch (error) {
        this.$message.error(error.message || '重试失败')
      }
    },
    async remove(row) {
      const libraryId = this.libraryId
      try {
        await this.$confirm(`删除「${row.title}」及其全部版本？`, '删除文档', { type: 'warning' })
      } catch {
        return
      }
      try {
        await deleteLibraryDocument(libraryId, row.docId)
        this.$message.success('已删除文档')
        await this.loadCatalog()
      } catch (error) {
        this.$message.error(error.message || '删除失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";

.kb-management { color: $mes-text; }
.kb-header, .kb-actions, .kb-scope, .kb-table-top, .kb-table-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.kb-header, .kb-table-top, .kb-table-footer { justify-content: space-between; }
.kb-header { margin-bottom: 20px; }
.kb-header p { margin: 6px 0 0; color: $mes-text-sub; }
.kb-actions .el-button { margin-left: 0; }
.kb-scope { margin-bottom: 16px; font-size: 14px; font-weight: 600; }
.kb-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.kb-metric {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 66px;
  padding: 12px 16px;
  background: $mes-card;
  border: 1px solid $mes-border;
  border-radius: 8px;
  color: $mes-text-sub;
  cursor: pointer;
  font: inherit;
  text-align: left;
}
.kb-metric strong { color: $mes-primary; font-size: 24px; font-variant-numeric: tabular-nums; }
.kb-metric[aria-pressed="true"], .kb-metric:hover { background: $mes-primary-soft; border-color: $mes-marker; }
.kb-table-card, .kb-empty { background: $mes-card; border: 1px solid $mes-border; border-radius: 8px; overflow: hidden; }
.kb-table-top, .kb-table-footer { padding: 12px 16px; }
.kb-table-top h2, .kb-empty h2 { font-size: 16px; margin: 0; }
.kb-table-top .el-input { max-width: 260px; }
.kb-table-footer, .kb-caption, .kb-filename { font-size: 12px; color: $mes-text-sub; }
.kb-filename { display: block; overflow-wrap: anywhere; }
.kb-empty { padding: 36px 16px; text-align: center; }
.kb-empty p { color: $mes-text-sub; margin-bottom: 0; }
.kb-stages { display: flex; flex-wrap: wrap; gap: 4px; }
.kb-stages span { background: $mes-table-head; color: $mes-text-sub; border-radius: 6px; padding: 2px 6px; white-space: nowrap; font-size: 12px; }
.kb-stages .is-current { background: $mes-primary-bg; color: $mes-primary; }
.kb-stages .is-completed { background: #e9f2ed; color: #315b46; }
.kb-stages .is-failed { background: #f9ece9; color: $mes-danger; }
.kb-caption { margin: 4px 0 0; line-height: 1.6; }
.kb-graph-note { background: $mes-primary-soft; color: $mes-primary; padding: 6px 10px; margin: 8px 0 0; line-height: 1.5; border-radius: 6px; }
.kb-preview { height: 100%; padding: 0 20px 20px; overflow: auto; }
.kb-preview-chunk { margin-bottom: 12px; padding: 10px 12px; background: $mes-primary-soft; border-left: 3px solid $mes-primary; }
.kb-preview-chunk h3 { font-size: 12px; color: $mes-text-sub; margin: 0 0 7px; }
.kb-preview-chunk pre { white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.65; margin: 0; }
@media (max-width: 680px) { .kb-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
