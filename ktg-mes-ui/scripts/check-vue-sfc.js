/* 只做语法校验：template / script / scss 三段能否解析。
   用法：node scripts/check-vue-sfc.js src/views/dashboard/WorkorderPipeline.vue [...]
*/
const fs = require('fs')
const path = require('path')
const compiler = require('vue-template-compiler')
const babel = require('@babel/parser')
const sass = require('sass')

const files = process.argv.slice(2)
if (!files.length) {
  console.error('usage: node scripts/check-vue-sfc.js <file.vue> [...]')
  process.exit(2)
}

let failed = 0
for (const rel of files) {
  const file = path.resolve(process.cwd(), rel)
  const src = fs.readFileSync(file, 'utf8')
  const sfc = compiler.parseComponent(src)

  if (sfc.template) {
    const res = compiler.compile(sfc.template.content)
    if (res.errors && res.errors.length) {
      failed++
      console.error('[template] ' + rel)
      res.errors.forEach(e => console.error('  ' + (e.msg || e)))
    }
  }

  if (sfc.script) {
    try {
      babel.parse(sfc.script.content, {
        sourceType: 'module',
        plugins: ['objectRestSpread', 'optionalChaining', 'nullishCoalescingOperator', 'dynamicImport', 'classProperties']
      })
    } catch (e) {
      failed++
      console.error('[script] ' + rel + ' -> ' + e.message)
    }
  }

  const styles = sfc.styles || []
  for (const style of styles) {
    try {
      if (sass.renderSync) {
        sass.renderSync({ data: style.content, includePaths: [path.join(process.cwd(), 'src')] })
      } else {
        sass.compileString(style.content, { loadPaths: [path.join(process.cwd(), 'src')] })
      }
    } catch (e) {
      failed++
      console.error('[style] ' + rel + ' -> ' + e.message)
    }
  }

  if (failed === 0) console.log('OK ' + rel)
}

process.exit(failed ? 1 : 0)
