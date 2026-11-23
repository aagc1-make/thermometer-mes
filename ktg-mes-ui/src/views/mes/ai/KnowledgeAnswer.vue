<template>
  <div class="knowledge-answer">
    <div class="knowledge-answer__label"><i class="el-icon-reading" aria-hidden="true" /> 知识库引用 · 本机资料</div>
    <p>{{ message.text }}</p>
    <article v-for="(source, index) in message.sources" :key="source.documentId + ':' + source.sectionIndex" class="knowledge-answer__source">
      <div class="knowledge-answer__title">[{{ index + 1 }}] {{ source.title }} <el-tag size="mini" effect="plain">{{ source.version }}</el-tag></div>
      <blockquote>{{ source.excerpt }}</blockquote>
      <div class="knowledge-answer__foot"><span>{{ source.filename }} · {{ source.sectionLabel }}</span><el-button type="text" size="mini" @click="$emit('source', source)">查看原文 [{{ index + 1 }}]</el-button></div>
    </article>
    <p class="knowledge-answer__note">以上为关键词匹配的原文片段，请结合文档版本和适用范围核对。</p>
  </div>
</template>

<script>
export default { name: 'KnowledgeAnswer', props: { message: { type: Object, required: true }}}
</script>

<style lang="scss" scoped>
.knowledge-answer { width: 620px; max-width: 100%;
  p { margin: 8px 0; }
  &__label { font-size: 12px; color: #5c7faf; font-weight: 600; }
  &__source { margin-top: 12px; border-top: 1px solid #e5e7eb; padding-top: 12px; }
  &__title { font-weight: 600; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
  blockquote { margin: 10px 0; padding: 10px 14px; background: #f5f8fc; border-left: 3px solid #b8cce8; border-radius: 0 6px 6px 0; white-space: pre-wrap; overflow-wrap: anywhere; }
  &__foot { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; color: #6b7280; font-size: 12px; }
  &__note { color: #6b7280; font-size: 11px; }
}
</style>
