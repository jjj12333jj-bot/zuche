/**
 * 菜单树工具：后台侧边栏按当前工作台前缀过滤
 */

/** 只保留 path 以 prefix 开头的节点；目录节点若还有子菜单则保留 */
export function filterMenuByPrefix(nodes = [], prefix) {
  const result = []
  for (const node of nodes) {
    const children = filterMenuByPrefix(node.children || [], prefix)
    const matched = node.path && node.path.startsWith(prefix)
    if (matched || children.length) {
      result.push({ ...node, children })
    }
  }
  return result
}

/** 找到目标路由的菜单链路，用于面包屑 */
export function findMenuTrail(nodes = [], path, trail = []) {
  for (const node of nodes) {
    const nextTrail = [...trail, node]
    if (node.path === path) return nextTrail
    if (node.children?.length) {
      const found = findMenuTrail(node.children, path, nextTrail)
      if (found) return found
    }
  }
  return null
}

/** 按 path 精确查找菜单名称 */
export function findMenuName(nodes = [], path) {
  const trail = findMenuTrail(nodes, path)
  return trail?.[trail.length - 1]?.menuName || ''
}
