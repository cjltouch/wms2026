import { useUserStore } from '@/store/user'

/**
 * 检查当前用户是否拥有指定权限
 * 支持通配符：
 *   - 精确匹配：perms 包含 'wms:purchase:add'
 *   - 全局通配：'*' 或 '*:*:*' 拥有所有权限
 *   - 模块通配：'wms:purchase:*' 匹配 wms:purchase:xxx
 *
 * @param authority 权限标识，如 'wms:stock-out:add'
 * @returns true=有权限
 */
export function hasPerm(authority: string): boolean {
  if (!authority) return true
  const userStore = useUserStore()
  const perms: Set<string> = new Set(userStore.permissions || [])
  if (perms.size === 0) return false

  // 精确匹配
  if (perms.has(authority)) return true

  // 通配符匹配
  for (const perm of perms) {
    if (!perm) continue
    if (perm === '*' || perm === '*:*:*') return true
    if (perm.endsWith(':*')) {
      const prefix = perm.substring(0, perm.length - 1)
      if (authority.startsWith(prefix)) return true
    }
  }
  return false
}

/**
 * 检查是否拥有任一权限（OR 语义）
 */
export function hasAnyPerm(authorities: string[]): boolean {
  if (!authorities || authorities.length === 0) return true
  return authorities.some((a) => hasPerm(a))
}

/**
 * 检查是否拥有全部权限（AND 语义）
 */
export function hasAllPerm(authorities: string[]): boolean {
  if (!authorities || authorities.length === 0) return true
  return authorities.every((a) => hasPerm(a))
}
