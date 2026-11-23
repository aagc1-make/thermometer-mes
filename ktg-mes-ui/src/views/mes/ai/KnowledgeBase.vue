<template>
  <div>
    <el-dialog title="我的知识库" :visible.sync="visible" width="880px" append-to-body custom-class="mes-knowledge-dialog">
      <p class="kb-intro">导入 BOM、工艺说明、检验规范等资料，知识问答会检索已启用的版本并引用原文。</p>
      <el-alert title="资料仅保存在当前浏览器、当前账号下。清理本站浏览器数据会移除资料，请保留原文件。" type="info" :closable="false" show-icon />
      <div class="kb-toolbar">
        <el-button type="primary" size="small" icon="el-icon-upload2" aria-label="导入文档" :disabled="busy || !!error" @click="openImport">导入文档</el-button>
        <el-input v-model="filter" size="small" placeholder="搜索文档名称、分类或版本" clearable aria-label="搜索知识文档" />
        <span>{{ activeCount }} 个启用版本 · 共 {{ documents.length }} 个版本</span>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <el-table v-loading="busy" :data="filteredDocuments" size="small" class="kb-table" empty-text="还没有文档，点击“导入文档”添加第一份资料">
        <el-table-column prop="title" label="文档" min-width="160" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="version" label="版本" width="90" show-overflow-tooltip />
        <el-table-column label="正文" width="80"><template slot-scope="scope">{{ scope.row.chars.toLocaleString() }} 字</template></el-table-column>
        <el-table-column label="启用" width="70"><template slot-scope="scope"><el-switch :value="scope.row.active" :disabled="busy" :aria-label="scope.row.title + ' ' + scope.row.version + ' 启用状态'" @change="active => toggle(scope.row, active)" /></template></el-table-column>
        <el-table-column label="操作" width="130"><template slot-scope="scope">
          <el-button type="text" size="small" @click="showSource({ documentId: scope.row.id, version: scope.row.version })">原文</el-button>
          <el-button type="text" size="small" :disabled="busy" @click="remove(scope.row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <p class="kb-note">同名称、同分类的资料作为一份文档管理。导入新版本会停用旧版，也可手动切换；删除当前版本不会自动启用历史版本。</p>
      <span slot="footer"><el-button size="small" @click="visible = false">关闭</el-button></span>
    </el-dialog>

    <el-dialog title="导入知识文档" :visible.sync="importVisible" width="700px" append-to-body :close-on-click-modal="false" :before-close="closeImport" custom-class="mes-knowledge-dialog">
      <el-form label-width="80px" size="small" @submit.native.prevent>
        <el-form-item label="导入方式">
          <el-radio-group v-model="importMode" :disabled="importing"><el-radio-button label="file">文件导入</el-radio-button><el-radio-button label="text">粘贴文本</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item v-if="importMode === 'file'" label="选择文件">
          <el-upload :key="uploadKey" action="#" :auto-upload="false" :limit="1" :file-list="files" accept=".txt,.md,.markdown,.csv,.pdf,.docx" :disabled="importing" :on-change="selectFile" :on-remove="clearFile" :on-exceed="fileExceeded">
            <el-button icon="el-icon-document" :loading="parsing">选择文档</el-button>
            <div slot="tip" class="el-upload__tip">TXT / Markdown / CSV / PDF / DOCX，单文件不超过5 MiB。扫描PDF需先识别文字。</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="文档名称" required><el-input v-model="form.title" maxlength="120" placeholder="例如：红外传感器来料检验规范" :disabled="importing" aria-label="知识文档名称" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="form.category" :disabled="importing" aria-label="知识文档分类"><el-option v-for="category in categories" :key="category" :label="category" :value="category" /></el-select></el-form-item>
        <el-form-item label="版本号" required><el-input v-model="form.version" maxlength="40" placeholder="例如：V1.0 / 2026-11版" :disabled="importing" aria-label="知识文档版本号" /></el-form-item>
        <el-form-item v-if="importMode === 'text'" label="文档正文"><el-input v-model="pasteText" type="textarea" :rows="9" :maxlength="300000" placeholder="粘贴正文；空行用于区分段落" :disabled="importing" aria-label="知识文档正文" /></el-form-item>
        <el-form-item v-else label="提取预览">
          <p class="kb-note">{{ sections.length }} 个段落或页面 · {{ sectionChars.toLocaleString() }} 字；仅提取文字，不保留原文件排版。</p>
          <pre class="kb-extract">{{ previewText || '选择文件后显示提取的正文。' }}</pre>
        </el-form-item>
      </el-form>
      <el-alert v-if="importError" :title="importError" type="error" :closable="false" show-icon />
      <span slot="footer"><el-button size="small" :disabled="importing" @click="closeImport()">取消</el-button><el-button type="primary" size="small" :loading="importing" :disabled="parsing" @click="saveImport">确认导入</el-button></span>
    </el-dialog>

    <el-dialog :title="sourceDocument ? sourceDocument.title + ' · ' + sourceDocument.version : '文档原文'" :visible.sync="sourceVisible" width="820px" append-to-body custom-class="mes-knowledge-dialog" @opened="locateSource">
      <template v-if="sourceDocument">
        <p class="kb-note">来源：{{ sourceDocument.filename }} · {{ sourceDocument.category }} · {{ sourceDocument.active ? '当前启用版本' : '历史停用版本' }}</p>
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
import { createKnowledgeDocument, searchKnowledge } from '@/utils/knowledgeBase'
import { createKnowledgeStore } from '@/utils/knowledgeStorage'
import { readKnowledgeFile, sectionsFromText } from '@/utils/knowledgeImport'

export default {
  name: 'KnowledgeBase',
  props: { userId: { type: [String, Number], default: '' }},
  data() {
    return { visible: false, importVisible: false, sourceVisible: false, documents: [], filter: '', error: '',
      busy: false, importing: false, parsing: false, importError: '', importMode: 'file', pasteText: '', files: [], sections: [],
      form: { title: '', version: 'V1.0', category: '其他' }, categories: ['BOM', '工艺说明', '检验规范', '操作手册', '其他'],
      sourceDocument: null, sourceIndex: -1, generation: 0, parseGeneration: 0, uploadKey: 0, store: null }
  },
  computed: {
    activeCount() { return this.documents.filter(doc => doc.active).length },
    sectionChars() { return this.sections.reduce((sum, section) => sum + section.text.length, 0) },
    previewText() { return this.sections.map(section => section.text).join('\n\n').slice(0, 4000) },
    filteredDocuments() {
      const query = this.filter.trim().toLowerCase()
      return this.documents.filter(doc => `${doc.title} ${doc.category} ${doc.version}`.toLowerCase().includes(query))
    }
  },
  watch: {
    userId: { immediate: true, handler() {
      this.generation++
      this.parseGeneration++
      this.visible = false; this.importVisible = false; this.sourceVisible = false; this.sourceDocument = null
      this.documents = []; this.store = null; this.error = ''; this.$emit('change', [])
      this.loadDocuments()
    } }
  },
  methods: {
    publish(rows) {
      this.documents = rows.slice().sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      this.$emit('change', this.documents)
    },
    async loadDocuments() {
      const generation = this.generation
      this.busy = true
      try {
        const store = createKnowledgeStore(this.userId); const documents = await store.list()
        if (generation !== this.generation) return
        this.store = store; this.error = ''; this.publish(documents)
      } catch (error) { if (generation === this.generation) this.error = error.message || '本地知识库读取失败' } finally { if (generation === this.generation) this.busy = false }
    },
    open() { this.visible = true; this.loadDocuments() },
    openImport() {
      this.parseGeneration++
      this.uploadKey++
      this.importMode = 'file'; this.pasteText = ''; this.files = []; this.sections = []; this.importError = ''; this.parsing = false
      this.form = { title: '', version: 'V1.0', category: '其他' }; this.importVisible = true
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
      if (this.importing || this.parsing || !this.store) return
      const generation = this.generation
      this.importError = ''; this.importing = true
      try {
        const sections = this.importMode === 'text' ? sectionsFromText(this.pasteText) : this.sections
        const document = createKnowledgeDocument({ ...this.form, sections,
          filename: this.importMode === 'text' ? `${this.form.title}.txt` : this.files[0] && this.files[0].name })
        const documents = await this.store.add(document)
        if (generation !== this.generation) return
        this.publish(documents); this.importVisible = false; this.$message.success('文档已保存在当前浏览器，新版本已启用')
      } catch (error) { if (generation === this.generation) this.importError = error.message || '文档未保存，请重试' } finally { this.importing = false }
    },
    async toggle(document, active) {
      if (this.busy || !this.store) return
      const generation = this.generation
      this.busy = true
      try { const rows = await this.store.activate(document.id, active); if (generation === this.generation) this.publish(rows) } catch (error) { this.$message.error(error.message) } finally { if (generation === this.generation) this.busy = false }
    },
    async remove(document) {
      const generation = this.generation
      try { await this.$confirm(`删除“${document.title} · ${document.version}”？此版本的本地正文会被移除，请确认已保留原文件。`, '删除文档版本', { type: 'warning' }) } catch (error) { return }
      if (generation !== this.generation || !this.store) return
      this.busy = true
      try { const rows = await this.store.remove(document.id); if (generation === this.generation) { this.publish(rows); this.$message.success('文档版本已删除') } } catch (error) { this.$message.error(error.message) } finally { if (generation === this.generation) this.busy = false }
    },
    async search(question) {
      if (!this.store) { await this.loadDocuments(); if (!this.store) throw new Error(this.error || '知识库尚未就绪') }
      const generation = this.generation; const rows = await this.store.list()
      if (generation !== this.generation) throw new Error('登录账号已切换，请重新查询知识库')
      this.publish(rows)
      return { sources: searchKnowledge(rows, question), documentCount: rows.length, activeCount: rows.filter(doc => doc.active).length }
    },
    async showSource(source) {
      try {
        if (!this.store) throw new Error('知识库尚未就绪，请稍后重试')
        const generation = this.generation; const rows = await this.store.list()
        if (generation !== this.generation) return
        const document = rows.find(doc => doc.id === source.documentId && doc.version === source.version)
        if (!document) throw new Error('该引用对应的文档版本已删除；聊天中保留的是当时的引用片段')
        this.sourceDocument = document; this.sourceIndex = Number.isInteger(source.sectionIndex) ? source.sectionIndex : -1; this.sourceVisible = true
      } catch (error) { this.$message.warning(error.message) }
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
.kb-intro { margin: 0 0 14px; color: #4b5563; line-height: 1.7; }
.kb-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin: 18px 0 12px;
  .el-input { width: 250px; } span { color: #6b7280; font-size: 12px; }
}
.kb-note { color: #6b7280; font-size: 12px; line-height: 1.7; margin: 10px 0; }
.kb-extract { max-height: 180px; overflow: auto; padding: 12px; margin: 0; border: 1px solid #e5e7eb; border-radius: 8px; background: #f8fafc; white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.7; }
.kb-source-body { max-height: 58vh; overflow: auto; }
.kb-source-section { padding: 12px 16px; margin-bottom: 8px; border-radius: 8px;
  &.is-selected { background: #edf4ff; border-left: 3px solid #6f95cc; }
  h4 { margin: 0 0 6px; color: #6b7280; font-size: 12px; }
  pre { margin: 0; font: inherit; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; color: #1f2937; }
}
</style>

<style>
.mes-knowledge-dialog { max-width: calc(100vw - 28px); }
.mes-knowledge-dialog .el-dialog__body { padding-top: 10px; }
</style>
