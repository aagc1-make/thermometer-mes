<template>
  <div>
    <el-dialog title="个人知识库" :visible.sync="visible" width="880px" append-to-body custom-class="mes-knowledge-dialog">
      <p class="kb-intro">导入 BOM、工艺说明或检验规范，供当前账号的知识问答和图谱检索。</p>
      <div class="kb-toolbar">
        <el-button type="primary" size="small" icon="el-icon-upload2" aria-label="导入文档" :disabled="busy || !!error" @click="openImport">导入文档</el-button>
        <el-input v-model="filter" size="small" placeholder="搜索名称、原文件名或版本" clearable aria-label="搜索知识文档" />
        <span role="status">{{ activeCount }} 份可检索 · 共 {{ documents.length }} 份文档</span>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon><el-button type="text" @click="loadDocuments">重新加载</el-button></el-alert>
      <el-table v-loading="busy" :data="filteredDocuments" size="small" class="kb-table" empty-text="还没有文档，点击“导入文档”添加第一份资料">
        <el-table-column prop="title" label="文档" min-width="160" />
        <el-table-column prop="original_name" label="文件名" min-width="180" />
        <el-table-column prop="version_no" label="版本" width="80" />
        <el-table-column label="索引状态" width="110">
          <template slot-scope="{ row }">
            <el-tag :type="!row.cleanup_pending && row.index_status === 'READY' ? 'success' : 'info'" size="small" effect="plain">
              {{ row.cleanup_pending ? '待清理' : indexLabel(row.index_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="254">
          <template slot-scope="{ row }">
            <el-button v-if="row.cleanup_pending" type="text" size="mini" :disabled="busy" @click="remove(row)">
              重试清理
            </el-button>
            <template v-else>
              <el-button
                  type="text"
                  size="mini"
                  :disabled="!row.active_version_id || row.index_status !== 'READY'"
                  @click="downloadDocument(row)"
              >
                下载
              </el-button>
              <el-button type="text" size="mini" :disabled="row.index_status === 'PENDING'" @click="openVersionImport(row)">
                更新版本
              </el-button>
              <el-button type="text" size="mini" :loading="rebuildingId === row.document_id"
                :disabled="rebuildingId !== null || !row.active_version_id || row.index_status !== 'READY'" @click="rebuildGraph(row)">
                重建图谱
              </el-button>
              <el-button type="text" size="mini" :disabled="busy || row.index_status === 'PENDING' || rebuildingId === row.document_id" @click="remove(row)">
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <p class="kb-note">上传的新版本在索引成功后生效。删除文档会移除它的所有版本。</p>
      <span slot="footer"><el-button size="small" @click="visible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog :title="updateDocumentId !== null ? '上传新版本' : '导入知识文档'" :visible.sync="importVisible" width="700px" append-to-body :close-on-click-modal="false" :before-close="closeImport" custom-class="mes-knowledge-dialog">
      <el-form label-width="80px" size="small" @submit.native.prevent>
        <el-form-item v-if="updateDocumentId === null" label="导入方式">
          <el-radio-group v-model="importMode" :disabled="importing"><el-radio-button label="file">文件导入</el-radio-button><el-radio-button label="text">粘贴文本</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item v-if="importMode === 'file'" label="选择文件">
          <el-upload :key="uploadKey" action="#" :auto-upload="false" :limit="1" :file-list="files" accept=".txt,.md,.markdown,.csv,.pdf,.docx" :disabled="importing" :on-change="selectFile" :on-remove="clearFile" :on-exceed="fileExceeded">
            <el-button icon="el-icon-document" :loading="parsing">选择文档</el-button>
            <div slot="tip" class="el-upload__tip">支持 TXT、Markdown（.md、.markdown）、CSV、文本型 PDF 和 DOCX，单文件不超过 5 MiB。TXT 和 Markdown 请使用 UTF-8。</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档名称" required><el-input v-model="form.title" maxlength="120" placeholder="例如：红外传感器来料检验规范" :disabled="importing || updateDocumentId !== null" aria-label="知识文档名称" /></el-form-item>
        <el-form-item v-if="importMode === 'text'" label="文档正文"><el-input v-model="pasteText" type="textarea" :rows="9" :maxlength="300000" placeholder="粘贴正文；空行用于区分段落" :disabled="importing" aria-label="知识文档正文" /></el-form-item>
        <el-form-item v-else label="提取预览">
          <p class="kb-note">提取正文 {{ sectionChars.toLocaleString() }} 字</p>
          <pre class="kb-extract">{{ previewText || '选择文件后显示提取的正文。' }}</pre>
        </el-form-item>
      </el-form>
      <el-alert v-if="importError" :title="importError" type="error" :closable="false" show-icon />
      <span slot="footer"><el-button size="small" :disabled="importing" @click="closeImport()">取消</el-button><el-button type="primary" size="small" :loading="importing" :disabled="parsing" @click="saveImport">{{ importing ? '正在索引…' : '确认导入' }}</el-button></span>
    </el-dialog>

    <el-dialog :title="sourceDocument ? sourceDocument.title + ' · ' + sourceDocument.version : '文档原文'" :visible.sync="sourceVisible" width="820px" append-to-body custom-class="mes-knowledge-dialog" @opened="locateSource">
      <template v-if="sourceDocument">
        <p class="kb-note">来源：{{ sourceDocument.filename }} · {{ sourceDocument.version }}</p>
        <div ref="sourceBody" class="kb-source-body">
          <section v-for="(part, index) in sourceDocument.sections" :key="index" :data-section="index" class="kb-source-section" :class="{ 'is-selected': index === sourceIndex }">
            <h4>{{ part.label }}</h4><pre>{{ part.text }}</pre>
          </section>
        </div>
      </template>
      <span slot="footer"><el-button size="small" @click="sourceVisible = false">关闭原文</el-button></span>
    </el-dialog>
  </div>
</template>

<script>
import { readKnowledgeFile } from '@/utils/knowledgeImport'
import request from '@/utils/request'
import { saveAs } from 'file-saver'

export default {
  name: 'PersonalKnowledgeBase',
  props: { userId: { type: [String, Number], default: '' }},
  data() {
    return { visible: false, importVisible: false, sourceVisible: false, documents: [], filter: '', error: '',
      busy: false, importing: false, parsing: false, importError: '', importMode: 'file', pasteText: '', files: [], sections: [],
      form: { title: '' },
      sourceDocument: null, sourceIndex: -1, generation: 0, parseGeneration: 0, uploadKey: 0, rebuildingId: null, updateDocumentId: null}
  },
  computed: {
    activeCount() { return this.documents.filter(doc => !doc.cleanup_pending && doc.active_version_id && doc.index_status === 'READY').length },
    sectionChars() { return this.sections.reduce((sum, section) => sum + section.text.length, 0) },
    previewText() { return this.sections.map(section => section.text).join('\n\n').slice(0, 4000) },
    filteredDocuments() {
      const query = this.filter.trim().toLowerCase()
      return this.documents.filter(doc => `${doc.title || ''} ${doc.original_name || ''} ${doc.version_no || ''}`.toLowerCase().includes(query))
    }
  },
  watch: {
    userId: { immediate: true, handler() {
      this.generation++
      this.parseGeneration++
      this.visible = false; this.importVisible = false; this.sourceVisible = false; this.sourceDocument = null
      this.documents = []; this.error = ''; this.busy = false; this.importing = false; this.rebuildingId = null
    } }
  },
  methods: {
    indexLabel(status) {
      return { READY: '可检索', FAILED: '索引失败', PENDING: '正在索引' }[status] || '未生效'
    },
    open() {
      this.visible = true
      this.loadDocuments()
    },
    openImport() {
      this.clearFile()
      this.form.title = ''
      this.pasteText = ''
      this.importMode = 'file'
      this.importVisible = true
      this.updateDocumentId = null
    },
    openVersionImport(row) {
      this.openImport()
      this.updateDocumentId = row.document_id
      this.form.title = row.title
    },
    async loadDocuments() {
      if (!this.userId) return
      const generation = this.generation
      this.busy = true
      try {
        const { data } = await request({
          url: '/mes/knowledge/documents',
          method: 'get'
        })
        if (generation !== this.generation) return

        this.documents = Array.isArray(data) ? data : []
        this.error = ''
      } catch (error) {
        if (generation === this.generation) {
          this.error = error.message || '知识库列表读取失败'
        }
      } finally {
        if (generation === this.generation) this.busy = false
      }
    },
    closeImport(done) {
      if (this.importing) return
      this.parseGeneration++; this.parsing = false; this.importVisible = false
      if (typeof done === 'function') done()
    },
    fileExceeded() { this.$message.info('请先移除已选文件，再选择新文件') },
    clearFile() { this.parseGeneration++; this.uploadKey++; this.files = []; this.sections = []; this.parsing = false; this.importError = '' },
    async selectFile(file) {
      const turn = ++this.parseGeneration
      this.files = [file]; this.sections = []; this.importError = ''; this.parsing = true
      if (!this.form.title) this.form.title = file.name.replace(/\.[^.]+$/, '').slice(0, 120)
      try {
        const sections = await readKnowledgeFile(file.raw)
        if (turn === this.parseGeneration) this.sections = sections
      } catch (error) { if (turn === this.parseGeneration) this.importError = error.message || '文档提取失败，请换用粘贴文本' } finally { if (turn === this.parseGeneration) this.parsing = false }
    },
    async saveImport() {
      if (this.importing || this.parsing) return

      const title = this.form.title.trim()
      const updating = this.updateDocumentId !== null
      let file = this.files[0] && this.files[0].raw
      if (!updating && this.importMode === 'text') {
        const content = this.pasteText.trim()
        if (!content) {
          this.importError = '请输入文档正文'
          return
        }
        file = new File([content], '粘贴文档.txt', { type: 'text/plain;charset=utf-8' })
      }

      if (!file || !/\.(txt|md|markdown|csv|pdf|docx)$/i.test(file.name)) {
        this.importError = '请选择 TXT、Markdown、CSV、文本型 PDF 或 DOCX 文件'
        return
      }
      if (!updating && !title) {
        this.importError = '请填写标题'
        return
      }

      const url = updating
          ? `/mes/knowledge/documents/${this.updateDocumentId}/versions`
          : '/mes/knowledge/documents'

      const body = new FormData()
      body.append('file', file)
      if (!updating) body.append('title', title)

      const generation = this.generation
      this.importError = ''
      this.importing = true
      try {
        const { data } = await request({
          url,
          method: 'post',
          data: body,
          headers: { repeatSubmit: false },
          timeout: 0
        })
        if (generation !== this.generation) return

        if (!data || data.indexStatus !== 'READY') {
          throw new Error('文档索引尚未就绪')
        }

        this.importVisible = false
        this.$message.success(updating ? '新版本已生效' : '文档已上传，可以提问')
        await this.loadDocuments()
      } catch (error) {
        if (generation === this.generation) {
          this.importError = error.message || '上传或索引失败'
          await this.loadDocuments()
        }
      } finally {
        if (generation === this.generation) this.importing = false
      }
    },
    async remove(row) {
      const generation = this.generation
      try {
        if (!row.cleanup_pending) await this.$confirm(
            `确定删除「${row.title}」及其所有版本吗？`,
            '删除知识库文档',
            { type: 'warning' }
        )
      } catch {
        return
      }

      if (this.busy || generation !== this.generation) return
      this.busy = true
      try {
        const { data, msg } = await request({
          url: `/mes/knowledge/documents/${row.document_id}`,
          method: 'delete',
          timeout: 130000
        })
        if (generation !== this.generation) return

        if (data && data.cleanupComplete) {
          this.$message.success(msg)
        } else {
          this.$message.warning(`${msg}，请点击“重试清理”`)
        }
      } catch (error) {
        // 请求拦截器显示失败原因，随后刷新列表核对服务端状态。
      } finally {
        if (generation === this.generation) await this.loadDocuments()
      }
    },
    showSource(source) {
      this.sourceDocument = {
        title: source.title,
        filename: source.filename,
        version: source.version,
        sections: [{ label: source.sectionLabel, text: source.excerpt }]
      }
      this.sourceIndex = 0
      this.sourceVisible = true
    },
    async rebuildGraph(row) {
      const generation = this.generation
      this.rebuildingId = row.document_id
      try {
        await request({
          url: `/mes/knowledge/documents/${row.document_id}/graph/rebuild`,
          method: 'post',
          timeout: 0
        })
        if (generation === this.generation) this.$message.success('当前文档的图谱已重建')
      } catch (error) {
        // HTTP 错误由请求拦截器提示。
      } finally {
        if (generation === this.generation) this.rebuildingId = null
      }
    },
    async downloadDocument(row) {
      const generation = this.generation
      try {
        const blob = await request({
          url: `/mes/knowledge/documents/${row.document_id}/download`,
          method: 'get',
          responseType: 'blob'
        })
        if (generation !== this.generation) return

        if (!(blob instanceof Blob) || blob.type.includes('application/json')) {
          this.$message.error('未收到原文件，请检查登录状态后重试')
          return
        }

        saveAs(blob, row.original_name)
      } catch (error) {
        // 项目的 request 拦截器会显示 HTTP 错误。
      }
    },
    locateSource() {
      const body = this.$refs.sourceBody
      const target = body && body.querySelector(`[data-section="${this.sourceIndex}"]`)
      if (target) body.scrollTop = Math.max(0, target.offsetTop - body.offsetTop - 8)
      else if (body) body.scrollTop = 0
    }
  }
}
</script>

<style lang="scss" scoped>
.kb-intro { margin: 0 0 14px; color: var(--color-text-secondary); line-height: 1.7; }
.kb-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 18px 0 12px;
  .el-input { width: 250px; } span { color: var(--color-text-secondary); font-size: 12px; }
}
.kb-note { color: var(--color-text-secondary); font-size: 12px; line-height: 1.7; margin: 10px 0; }
.kb-extract { max-height: 180px; overflow: auto; padding: 12px; margin: 0; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-primary-soft); white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.7; }
.kb-source-body { max-height: 58vh; overflow: auto; }
.kb-source-section { padding: 12px 16px; margin-bottom: 8px; border-radius: 8px;
  &.is-selected { background: var(--color-primary-bg); border: 1px solid var(--color-border); }
  h4 { margin: 0 0 6px; color: var(--color-text-secondary); font-size: 12px; }
  pre { margin: 0; font: inherit; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; color: var(--color-text); }
}
</style>

<style>
.mes-knowledge-dialog { max-width: calc(100vw - 28px); }
.mes-knowledge-dialog .el-dialog__body { padding-top: 10px; }
</style>
