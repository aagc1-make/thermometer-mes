<template>
  <div class="knowledge-answer">
    <div class="knowledge-answer__label">
      <i class="el-icon-reading" aria-hidden="true" />
      {{ message.mode === 'graph_rag' ? 'GraphRAG 问答' : 'RAG 问答' }}
    </div>
    <p v-if="message.mode === 'graph_rag' && message.graphStatus === 'no_match'" class="knowledge-answer__note">
      本次未找到相关图谱路径
    </p>
    <p>{{ message.text }}</p>
    <section v-if="paths.length" class="knowledge-answer__paths">
      <div class="knowledge-answer__path-header">
        <h4>图谱路径</h4>
        <el-button v-if="hasGraphPath" type="text" size="mini" @click="$emit('graph-path', paths)">查看推理路径</el-button>
      </div>
      <ul class="knowledge-answer__path-list">
        <li v-for="(path, pathIndex) in paths" :key="pathIndex">
          <ol class="knowledge-answer__steps">
            <li v-for="(step, stepIndex) in path" :key="stepIndex">
              {{ step.subject }} — {{ step.predicate }} → {{ step.object }}
              <el-button
                v-if="sourcesByNumber.has(step.sourceNumber)"
                type="text"
                size="mini"
                :aria-label="`查看原文 [${step.sourceNumber}]`"
                @click="showSource(sourcesByNumber.get(step.sourceNumber))"
              >[{{ step.sourceNumber }}]</el-button>
            </li>
          </ol>
        </li>
      </ul>
    </section>
    <article v-for="(source, index) in sources" :key="source.documentId + ':' + source.sectionIndex" class="knowledge-answer__source">
      <div class="knowledge-answer__title">[{{ source.number || index + 1 }}] {{ source.title }} <el-tag size="mini" effect="plain">{{ source.version }}</el-tag></div>
      <blockquote>{{ source.excerpt }}</blockquote>
      <div class="knowledge-answer__foot">
        <span>{{ source.filename }} · {{ source.sectionLabel }}</span>
        <el-button type="text" size="mini" @click="showSource(source)">查看原文 [{{ source.number || index + 1 }}]</el-button>
      </div>
    </article>
    <p v-if="!message.rag" class="knowledge-answer__note">以上为关键词匹配的原文片段，请结合文档版本和适用范围核对。</p>
  </div>
</template>

<script>
export default {
  name: 'PersonalKnowledgeAnswer',
  props: { message: { type: Object, required: true }},
  computed: {
    sources() {
      return this.message.sources || []
    },
    paths() {
      return this.message.mode === 'graph_rag' && Array.isArray(this.message.paths) ? this.message.paths : []
    },
    hasGraphPath() {
      return this.paths.some(path => path.some(step => step.factId))
    },
    sourcesByNumber() {
      return new Map(this.sources.map(source => [source.number, source]))
    }
  },
  methods: {
    showSource(source) {
      this.$emit('source', { ...source, rag: this.message.rag })
    }
  }
}
</script>

<style lang="scss" scoped>
.knowledge-answer { width: 620px; max-width: 100%;
  p { margin: 8px 0; }
  &__label { font-size: 12px; color: var(--color-primary); font-weight: 600; }
  &__paths { margin: 12px 0; padding: 10px 14px; background: var(--color-primary-soft); border-radius: 6px; overflow-wrap: anywhere; }
  &__path-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 6px; }
  &__path-header h4 { margin: 0; font-size: 13px; }
  &__path-list { margin: 0; padding: 0; list-style: none; }
  &__path-list > li + li { margin-top: 6px; padding-top: 6px; border-top: 1px solid var(--color-border); }
  &__steps { margin: 0; padding-left: 20px; }
  &__steps .el-button { padding: 3px; }
  &__source { margin-top: 12px; border-top: 1px solid var(--color-border); padding-top: 12px; }
  &__title { font-weight: 600; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
  blockquote { margin: 10px 0; padding: 10px 14px; background: var(--color-primary-soft); border: 1px solid var(--color-border); border-radius: 0 6px 6px 0; white-space: pre-wrap; overflow-wrap: anywhere; }
  &__foot { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; color: var(--color-text-secondary); font-size: 12px; }
  &__note { color: var(--color-text-secondary); font-size: 11px; }
}
</style>
