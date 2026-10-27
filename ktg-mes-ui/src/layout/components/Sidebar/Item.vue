<script>
export default {
  name: 'MenuItem',
  functional: true,
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    },
    showDot: {
      type: Boolean,
      default: false
    }
  },
  render(h, context) {
    const { icon, title, showDot } = context.props
    const vnodes = []

    if (icon) {
      vnodes.push(<svg-icon icon-class={icon}/>)
    }

    if (title) {
      if (title.length > 5) {
        vnodes.push(<span slot='title' title={(title)}>{(title)}</span>)
      } else {
        vnodes.push(<span slot='title'>{(title)}</span>)
      }
    }
    if (showDot) {
      vnodes.push(h('span', {
        class: 'menu-notice-dot',
        attrs: {
          role: 'status',
          'aria-label': '有新关联表单'
        }
      }))
    }
    return vnodes
  }
}
</script>

<style lang="scss">
.menu-notice-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-left: 8px;
  border-radius: 50%;
  background-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.14);
  vertical-align: middle;
}
</style>
