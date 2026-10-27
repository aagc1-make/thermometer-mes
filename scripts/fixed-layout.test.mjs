import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const read = relativePath => readFileSync(new URL(`../${relativePath}`, import.meta.url), 'utf8')

test('desktop sidebar is fixed open while mobile keeps its drawer state', () => {
  const layout = read('ktg-mes-ui/src/layout/index.vue')
  const sidebar = read('ktg-mes-ui/src/layout/components/Sidebar/index.vue')

  assert.match(layout, /hideSidebar:\s*this\.device\s*===\s*['"]mobile['"]\s*&&\s*!this\.sidebar\.opened/)
  assert.match(sidebar, /return\s+this\.device\s*===\s*['"]mobile['"]\s*\?\s*!this\.sidebar\.opened\s*:\s*false/)
})

test('collapse control is available only on mobile', () => {
  const navbar = read('ktg-mes-ui/src/layout/components/Navbar.vue')

  assert.match(navbar, /<hamburger[^>]+v-if="device==='mobile'"/)
})

test('desktop header spans the viewport and fixed navigation starts below it', () => {
  const theme = read('ktg-mes-ui/src/assets/styles/mes-theme.scss')

  assert.match(theme, /\.app-wrapper \.fixed-header\s*\{[^}]*left:\s*0[^}]*width:\s*100%/s)
  assert.match(theme, /\.app-wrapper \.sidebar-container\s*\{[^}]*top:\s*50px[^}]*height:\s*calc\(100%\s*-\s*50px\)/s)
  assert.match(theme, /\.app-wrapper \.tags-view-container\s*\{[^}]*margin-left:\s*272px[^}]*width:\s*calc\(100%\s*-\s*272px\)/s)
})
