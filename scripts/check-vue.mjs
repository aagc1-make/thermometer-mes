/**
 * 轻量 Vue 单文件组件语法检查（不依赖 webpack / Node 版本）
 * 用法：node scripts/check-vue.mjs [文件...]
 */
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';

const require = createRequire(path.resolve('E:/JavaCode/ktg-mes-ui/package.json'));
const compiler = require('vue-template-compiler');
const babel = require('@babel/core');

const files = process.argv.slice(2);
if (!files.length) {
  console.error('请传入 .vue 文件路径');
  process.exit(2);
}

let failed = 0;
for (const file of files) {
  const source = readFileSync(file, 'utf8');
  const sfc = compiler.parseComponent(source);
  const errors = [];
  if (sfc.template && sfc.template.content) {
    const r = compiler.compile(sfc.template.content, { outputSourceRange: true });
    for (const e of r.errors || []) errors.push('template: ' + (e.msg || e));
  }
  if (sfc.script && sfc.script.content) {
    try {
      babel.parseSync(sfc.script.content, {
        filename: file,
        presets: [],
        plugins: [require.resolve('@babel/plugin-syntax-dynamic-import')]
      });
    } catch (e) {
      errors.push('script: ' + e.message);
    }
  }
  if (errors.length) {
    failed++;
    console.log(`✗ ${file}`);
    errors.forEach(e => console.log('   ' + e));
  } else {
    console.log(`✓ ${file}（template ${sfc.template ? 'ok' : '-'}，script ok）`);
  }
}
process.exit(failed ? 1 : 0);
