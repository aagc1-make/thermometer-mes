<template>
  <section class="query-bar" aria-label="查询条件">
    <el-form
      ref="form"
      :model="model"
      :size="size"
      :label-width="labelWidth"
      :inline="true"
      class="query-bar__form"
      @submit.native.prevent
    >
      <div class="query-bar__row">
        <slot></slot>
        <el-form-item v-if="advancedFields.length" class="query-bar__more">
          <el-button
            type="text"
            class="query-bar__toggle"
            :aria-expanded="String(open)"
            @click="open = !open"
          >
            更多筛选
            <i :class="open ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
            <span v-if="activeCount" class="query-bar__count">{{ activeCount }}</span>
          </el-button>
        </el-form-item>
        <el-form-item class="query-bar__actions">
          <el-button icon="el-icon-refresh" size="mini" @click="$emit('reset')">重置</el-button>
          <el-button class="mes-action--query" type="primary" icon="el-icon-search" size="mini" @click="$emit('search')">查询</el-button>
        </el-form-item>
      </div>

      <div v-show="open" class="query-bar__advanced">
        <slot name="more"></slot>
      </div>
    </el-form>

    <div v-if="activeCount" class="query-bar__chips">
      <span v-for="chip in activeChips" :key="chip.prop" class="query-bar__chip">
        <span class="query-bar__chip-key">{{ chip.label }}</span>
        <span class="query-bar__chip-value">{{ chip.value }}</span>
        <button
          type="button"
          class="query-bar__chip-close"
          :aria-label="'移除' + chip.label + '条件'"
          @click="clearField(chip.prop)"
        ><i class="el-icon-close"></i></button>
      </span>
      <el-button type="text" size="mini" @click="clearAdvanced">清除全部</el-button>
    </div>
  </section>
</template>

<script>
/**
 * 列表页统一查询区。
 *
 * 默认插槽放一级条件（常显），具名插槽 more 放二级条件（折叠）。
 * 二级条件的字段名与标签直接从插槽里的 el-form-item 读取，页面不用再声明一遍，
 * 已填写的内容会自动回显成 chip，可单条移除或一键清除。
 */
export default {
  name: 'QueryBar',
  props: {
    // 查询对象（页面上的 queryParams）
    model: { type: Object, required: true },
    size: { type: String, default: 'small' },
    labelWidth: { type: String, default: '68px' }
  },
  data() {
    return { open: false }
  },
  computed: {
    /** 二级条件：字段名与标签来自 more 插槽里的 el-form-item 的 prop / label */
    advancedFields() {
      return (this.$slots.more || [])
        .map(vnode => {
          const props = vnode.componentOptions && vnode.componentOptions.propsData
          if (!props || !props.prop) return null
          return { prop: props.prop, label: props.label || props.prop, vnode }
        })
        .filter(Boolean)
    },
    activeChips() {
      return this.advancedFields
        .map(field => ({
          prop: field.prop,
          label: field.label,
          value: this.displayValue(field, this.valueOf(field.prop))
        }))
        .filter(chip => this.filled(chip.value))
    },
    activeCount() {
      return this.activeChips.length
    }
  },
  methods: {
    /** 兼容 RuoYi 的 resetForm('queryForm')：内部转发给 el-form */
    resetFields() {
      if (this.$refs.form) this.$refs.form.resetFields()
    },
    valueOf(prop) {
      return String(prop).split('.').reduce((acc, key) => (acc == null ? acc : acc[key]), this.model)
    },
    filled(value) {
      if (Array.isArray(value)) return value.length > 0
      return value !== null && value !== undefined && String(value).trim() !== ''
    },
    /**
     * 下拉条件存的是字典值，直接显示会是「1」这种编码。
     * 从插槽里的 el-option 反查一次标签，查不到就退回原值。
     */
    displayValue(field, value) {
      if (!this.filled(value)) return value
      const children = (field.vnode.componentOptions && field.vnode.componentOptions.children) || []
      for (const child of children) {
        const options = (child.componentOptions && child.componentOptions.children) || []
        for (const option of options) {
          const props = option.componentOptions && option.componentOptions.propsData
          if (props && String(props.value) === String(value) && props.label) return props.label
        }
      }
      return value
    },
    setField(prop, value) {
      const path = String(prop).split('.')
      const last = path.pop()
      const target = path.reduce((acc, key) => (acc == null ? this.model : acc[key]), this.model)
      if (target) this.$set(target, last, value)
    },
    clearField(prop) {
      this.setField(prop, null)
      this.$emit('search')
    },
    clearAdvanced() {
      this.activeChips.forEach(chip => this.setField(chip.prop, null))
      this.$emit('search')
    }
  }
}
</script>

<style scoped>
.query-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  margin: 0 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-page);
}

.query-bar__form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.query-bar__row,
.query-bar__advanced {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
}

.query-bar__advanced {
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-control);
  background: var(--color-bg);
}

.query-bar ::v-deep .el-form-item {
  margin: 0;
  max-width: 100%;
}

.query-bar ::v-deep .el-form-item__label {
  padding-right: 10px;
  white-space: nowrap;
}

.query-bar .query-bar__actions {
  margin-left: auto;
}

.query-bar__toggle {
  padding: 0 4px;
}

.query-bar__toggle i {
  margin-left: 4px;
}

.query-bar__count {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  margin-left: 2px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  font-size: 11px;
  font-weight: 600;
  text-align: center;
}

.query-bar__chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.query-bar__chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 2px 0 8px;
  border: 1px solid var(--color-primary-bg);
  border-radius: 4px;
  background: var(--color-bg);
  color: var(--color-primary);
  font-size: 12px;
}

.query-bar__chip-key {
  color: var(--color-text-secondary);
}

.query-bar__chip-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
}

.query-bar__chip-close:hover {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}
</style>
