<template>
  <div class="assistant-composer" :class="{ 'is-active': focused }">
    <el-input ref="question" :value="value" type="textarea" :autosize="{ minRows: roomy ? 3 : 2, maxRows: 6 }"
      :maxlength="600" :placeholder="placeholder" label="输入问题"
      @input="$emit('input', $event)" @keydown.native="onKeydown"
      @focus="focused = true" @blur="focused = false" />
    <div class="composer-actions">
      <div class="composer-options">
        <el-dropdown ref="modeDropdown" trigger="click" placement="top-start" @command="$emit('mode', $event)">
          <button ref="modeButton" class="composer-mode" type="button" :disabled="busy">
            <i v-if="selected" :class="selected.icon" aria-hidden="true" />
            {{ selected ? selected.label : '请选择查询模式' }} <i class="el-icon-arrow-down" aria-hidden="true" />
          </button>
          <el-dropdown-menu slot="dropdown" class="mes-assistant-mode-menu">
            <el-dropdown-item v-for="item in modes" :key="item.key" :command="item.key" :divided="item.divided">
              <i :class="item.icon" aria-hidden="true" />
              <span>{{ item.label }} <i v-if="mode === item.key" class="el-icon-check" aria-hidden="true" /></span>
              <small>{{ item.description }}</small>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <span class="composer-scope" :title="scope"><i class="el-icon-folder-opened" aria-hidden="true" /> {{ scope }}</span>
      </div>
      <button v-if="streaming" type="button" class="composer-send" title="停止生成" aria-label="停止生成" @click="$emit('stop')">
        <span class="composer-stop" aria-hidden="true" />
      </button>
      <button v-else type="button" class="composer-send composer-send--submit"
        :title="submitLabel" :aria-label="submitLabel" :disabled="!canSubmit" @click="submit">
        <i class="el-icon-top" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<script>
const modes = [
  { key: 'auto', label: '自动路由', icon: 'el-icon-magic-stick', description: '按问题使用部门资料或业务数据' },
  { key: 'rag', label: '仅文档 RAG', icon: 'el-icon-document', description: '检索部门资料并附上原文引用' },
  { key: 'graph', label: '图谱 GraphRAG', icon: 'el-icon-share', description: '查询实体关系和文档依据' },
  { key: 'business', label: '数据问答', icon: 'el-icon-data-analysis', description: '查询库存、工单、质检等实时数据' },
  { key: 'knowledge', label: '浏览器资料', icon: 'el-icon-monitor', description: '查询当前浏览器已启用的文档', divided: true }
]

export default {
  name: 'AssistantComposer',
  props: {
    value: { type: String, default: '' },
    mode: { type: String, required: true },
    scope: { type: String, required: true },
    busy: Boolean,
    streaming: Boolean,
    roomy: Boolean
  },
  data() { return { modes, focused: false } },
  computed: {
    selected() {
      return modes.find(item => item.key === this.mode)
    },
    canSubmit() { return !this.busy && !!this.value.trim() },
    submitLabel() { return this.selected ? '发送' : '请先选择查询模式' },
    placeholder() {
      return { rag: '输入与本部门文档有关的问题…', business: '问库存、工单进度或检验数据…',
        graph: '输入想了解的实体关系…', knowledge: '输入与浏览器资料有关的问题…' }[this.mode] || '有什么需要查的？'
    }
  },
  methods: {
    focus() { this.$refs.question.focus() },
    submit() {
      if (!this.canSubmit) return
      if (!this.selected) {
        this.$refs.modeButton.focus()
        this.$refs.modeDropdown.show()
        return
      }
      this.$emit('send')
    },
    onKeydown(event) {
      if (event.key !== 'Enter' || event.shiftKey || event.isComposing || event.keyCode === 229) return
      event.preventDefault()
      this.submit()
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/assets/styles/mes-palette";

.assistant-composer {
  width: 100%;
  margin: 0 auto;
  padding: 8px 10px 9px;
  background: $mes-card;
  border: 1px solid $mes-border-strong;
  border-radius: 12px;
  box-shadow: 0 3px 10px rgba(37, 44, 53, .04);
}
.assistant-composer.is-active {
  border-color: $mes-marker;
  box-shadow: 0 0 0 3px rgba($mes-marker, .17);
}
.assistant-composer ::v-deep .el-textarea__inner {
  min-height: 54px;
  padding: 9px 7px;
  border: 0;
  resize: none;
  box-shadow: none;
  background: transparent;
  color: $mes-text;
  font: inherit;
  line-height: 1.55;
}
.composer-actions, .composer-options { display: flex; align-items: center; gap: 8px; min-width: 0; }
.composer-actions { justify-content: space-between; }
.composer-options { flex: 1; flex-wrap: wrap; }
.composer-mode, .composer-scope {
  display: inline-block;
  padding: 5px 8px;
  max-width: 100%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  border: 1px solid $mes-border;
  border-radius: 8px;
  font: inherit;
  font-size: 12px;
}
.composer-mode {
  color: $mes-text-sub;
  background: $mes-card;
  cursor: pointer;
  &:hover:enabled { color: $mes-text; border-color: $mes-marker; }
  &:disabled { color: $mes-text-sub; cursor: default; }
}
.composer-scope { max-width: 250px; color: $mes-text-sub; background: $mes-card; }
.composer-send {
  display: grid;
  place-items: center;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: #fff;
  background: $mes-primary;
  cursor: pointer;
  font-size: 18px;
}
.composer-send:hover { background: $mes-primary-hover; }
.composer-send--submit:disabled { color: $mes-text-sub; background: $mes-border; cursor: default; }
.composer-mode:focus-visible,
.composer-send--submit:focus-visible {
  outline: 2px solid #34304b;
  outline-offset: 2px;
}
.composer-stop { width: 11px; height: 11px; background: currentColor; border-radius: 2px; }
</style>

<style lang="scss">
.mes-assistant-mode-menu .el-dropdown-menu__item { padding: 7px 14px; min-width: 198px; line-height: 1.4; }
.mes-assistant-mode-menu small { display: block; padding-left: 22px; margin-top: 3px; color: #55616f; font-size: 12px; }
</style>
