<template>
  <div class="knowledge-answer">
    <div v-if="parts.length" class="knowledge-answer__text"><template v-for="(part, index) in parts"><button v-if="part.source !== undefined"
      :key="index" class="citation" type="button" :aria-label="'查看引用 ' + (part.source + 1)"
      @click="$emit('source', part.source)">[{{ part.source + 1 }}]</button><span v-else :key="index">{{ part.text }}</span></template></div>
    <div v-if="sources.length && !message.streaming" class="answer-sources">
      <button class="sources-button" type="button" @click="$emit('source', 0)">
        <i class="el-icon-reading" aria-hidden="true" /> {{ sources.length }} 处文档引用
      </button>
    </div>
    <div v-if="sources.length && !message.streaming && message.channel" class="answer-follow-ups">
      <button v-for="question in followUps" :key="question" type="button" @click="$emit('follow-up', question)">
        {{ question }} <i class="el-icon-arrow-right" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'KnowledgeAnswer',
  props: { message: { type: Object, required: true }},
  computed: {
    sources() { return this.message.sources || [] },
    parts() {
      const raw = this.message.text || ''
      return raw.split(/(\[\d+\])/g).filter(Boolean).map(token => {
        const marker = token.match(/^\[(\d+)\]$/)
        const source = marker ? Number(marker[1]) - 1 : -1
        return source >= 0 && source < this.sources.length ? { source } : { text: token }
      })
    },
    followUps() {
      const title = this.sources[0].title.slice(0, 30)
      return [`《${title}》有哪些关键要求？`, `按《${title}》需要记录哪些信息？`]
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";
.knowledge-answer__text { white-space: pre-wrap; overflow-wrap: anywhere; }
.citation, .sources-button, .answer-follow-ups button {
  border: 1px solid $mes-border;
  border-radius: 7px;
  background: $mes-primary-soft;
  color: $mes-primary;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
.citation { padding: 0 5px; margin: 0 2px; line-height: 1.5; vertical-align: 1px; }
.answer-sources, .answer-follow-ups { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }
.sources-button, .answer-follow-ups button { padding: 3px 8px; }
.answer-follow-ups button { background: $mes-card; text-align: left; }
.citation:hover, .sources-button:hover, .answer-follow-ups button:hover { border-color: $mes-marker; }
</style>
