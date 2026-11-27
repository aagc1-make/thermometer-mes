// Positions use the centre of each card. Layout edges are undirected for spacing
// only; the renderer always draws the original, directed relationships.
export const cardWidth = 174
export const cardHeight = 66
const columnGap = 268
const rowGap = 108

export function arrangeGraph(nodes, edges, focus) {
  const neighbors = new Map(nodes.map(node => [node.id, new Set()]))
  edges.forEach(({ source, target }) => {
    if (source !== target && neighbors.has(source) && neighbors.has(target)) {
      neighbors.get(source).add(target)
      neighbors.get(target).add(source)
    }
  })
  const ordered = nodes.slice().sort((a, b) =>
    Number(b.id === focus) - Number(a.id === focus) ||
    neighbors.get(b.id).size - neighbors.get(a.id).size ||
    Number(a.type === 'Document') - Number(b.type === 'Document') ||
    String(a.id).localeCompare(String(b.id))
  )
  const seen = new Set()
  const positions = new Map()
  let top = cardHeight / 2
  for (const root of ordered) {
    if (seen.has(root.id)) continue
    const queue = [{ id: root.id, children: [] }]
    seen.add(root.id)
    for (const parent of queue) {
      for (const id of neighbors.get(parent.id)) {
        if (seen.has(id)) continue
        seen.add(id)
        const child = { id, children: [] }
        parent.children.push(child)
        queue.push(child)
      }
    }
    for (let i = queue.length - 1; i >= 0; i--) {
      queue[i].rows = Math.max(1, queue[i].children.reduce((sum, child) => sum + child.rows, 0))
    }
    const sides = [{ sign: 1, trees: [], rows: 0 }, { sign: -1, trees: [], rows: 0 }]
    queue[0].children.sort((a, b) => b.rows - a.rows).forEach(child => {
      const side = sides[0].rows <= sides[1].rows ? sides[0] : sides[1]
      side.trees.push(child)
      side.rows += child.rows
    })
    const span = Math.max(1, ...sides.map(side => side.rows))
    const middle = top + (span - 1) * rowGap / 2
    positions.set(root.id, { x: 0, y: middle })
    function place(trees, depth, firstRow) {
      let y = firstRow
      trees.forEach(tree => {
        positions.set(tree.id, { x: depth * columnGap, y: y + (tree.rows - 1) * rowGap / 2 })
        place(tree.children, depth + Math.sign(depth), y)
        y += tree.rows * rowGap
      })
    }
    sides.forEach(side => place(side.trees, side.sign, middle - (side.rows - 1) * rowGap / 2))
    top += (span + 0.5) * rowGap
  }
  return { positions, root: ordered.length ? ordered[0].id : '' }
}

export function connectCards(from, to, lane = 0, size = {}) {
  const width = size.width || cardWidth
  const height = size.height || cardHeight
  const dx = to.x - from.x
  const dy = to.y - from.y
  const horizontalGap = Math.abs(dx) - width
  const verticalGap = Math.abs(dy) - height
  const horizontal = horizontalGap >= 0 && horizontalGap >= verticalGap * 0.55
  const sx = horizontal ? Math.sign(dx) * width / 2 : 0
  const sy = horizontal ? 0 : Math.sign(dy) * height / 2
  const a = { x: from.x + sx, y: from.y + sy }
  const b = { x: to.x - sx, y: to.y - sy }
  const midX = (a.x + b.x) / 2
  const midY = (a.y + b.y) / 2
  const c = horizontal ? { x: midX, y: a.y + lane } : { x: a.x + lane, y: midY }
  const d = horizontal ? { x: midX, y: b.y + lane } : { x: b.x + lane, y: midY }
  return {
    path: `M ${a.x} ${a.y} C ${c.x} ${c.y}, ${d.x} ${d.y}, ${b.x} ${b.y}`,
    x: (a.x + 3 * c.x + 3 * d.x + b.x) / 8,
    y: (a.y + 3 * c.y + 3 * d.y + b.y) / 8
  }
}

// 影响路径只有一条从左到右的有序链：按返回的边序排列，卡片之间留出用量标签的位置。
export function arrangeOrderedPath(nodes, size = {}) {
  const width = size.width || cardWidth
  const gap = width + 96
  const positions = new Map()
  nodes.forEach((node, index) => positions.set(node.id, { x: index * gap, y: 0 }))
  return { positions, root: nodes.length ? nodes[0].id : '', gap }
}
