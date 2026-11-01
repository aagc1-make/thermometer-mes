import { readdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const testDir = join(root, 'scripts', 'tests')
const tests = [
  join(root, 'scripts', 'fixed-layout.test.mjs'),
  ...readdirSync(testDir).filter(name => name.endsWith('.test.cjs')).sort().map(name => join(testDir, name))
]
const result = spawnSync(process.execPath, ['--test', ...tests], { cwd: root, stdio: 'inherit' })
if (result.error) console.error(result.error.message)
process.exit(result.status ?? 1)
