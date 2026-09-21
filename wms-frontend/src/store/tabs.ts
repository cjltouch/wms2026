import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

/** 已访问标签页结构 */
export interface VisitedView {
  /** 路由完整路径（含 query），作为唯一标识 */
  fullPath: string
  /** 路由 path（不含 query），用于匹配 keep-alive include */
  path: string
  /** 路由 name，用于 keep-alive include */
  name: string
  /** 标签页标题 */
  title: string
  /** 是否固定（不可关闭，如首页） */
  affix: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  /** 已打开的标签页列表 */
  const visitedViews = ref<VisitedView[]>([])
  /** 需要 keep-alive 缓存的组件 name 列表 */
  const cachedViewNames = ref<string[]>([])

  /**
   * 判断路由是否可作为有效标签页：
   * - 有 name（keep-alive 依赖）
   * - meta.title 存在（标签页显示文字）
   * - 非 print 页面（不走 Layout）
   */
  function isValidTab(route: RouteLocationNormalized): boolean {
    if (!route.name || !route.meta?.title) return false
    if (route.path.startsWith('/print/')) return false
    return true
  }

  /**
   * 从路由构造 VisitedView 对象
   */
  function toView(route: RouteLocationNormalized): VisitedView {
    return {
      fullPath: route.fullPath,
      path: route.path,
      name: route.name as string,
      title: (route.meta?.title as string) || route.path,
      affix: route.path === '/dashboard'
    }
  }

  /**
   * 添加一个标签页（路由变化时调用）
   * 如果标签已存在则仅刷新 title；不存在则 push 到末尾
   */
  function addTab(route: RouteLocationNormalized) {
    if (!isValidTab(route)) return
    const view = toView(route)
    const exists = visitedViews.value.find((v) => v.fullPath === view.fullPath)
    if (exists) {
      // 已存在：更新标题（可能动态变化）
      if (exists.title !== view.title) exists.title = view.title
    } else {
      visitedViews.value.push(view)
    }
    // keep-alive 缓存 name
    if (view.name && !cachedViewNames.value.includes(view.name)) {
      cachedViewNames.value.push(view.name)
    }
  }

  /**
   * 关闭指定标签页
   * @returns 关闭后应该跳转到的路由；如果关闭的是当前激活页则返回相邻 tab 的 fullPath
   */
  function closeTab(view: VisitedView, currentFullPath?: string): string | null {
    const idx = visitedViews.value.findIndex((v) => v.fullPath === view.fullPath)
    if (idx < 0) return null
    if (view.affix) return null // 固定页不可关闭

    visitedViews.value.splice(idx, 1)

    // 同步移除缓存
    cachedViewNames.value = cachedViewNames.value.filter((n) => n !== view.name)

    // 如果关闭的是当前页，返回相邻页路径供跳转
    if (currentFullPath === view.fullPath) {
      const next = visitedViews.value[idx] || visitedViews.value[idx - 1]
      return next ? next.fullPath : null
    }
    return null
  }

  /** 关闭其他标签页（保留 affix + 当前选中） */
  function closeOthers(selectedView: VisitedView) {
    const keep = visitedViews.value.filter((v) => v.affix || v.fullPath === selectedView.fullPath)
    visitedViews.value = keep
    cachedViewNames.value = keep.map((v) => v.name).filter(Boolean)
  }

  /** 关闭左侧标签页（保留 affix + selectedView 及其右侧） */
  function closeLeft(selectedView: VisitedView) {
    const idx = visitedViews.value.findIndex((v) => v.fullPath === selectedView.fullPath)
    if (idx < 0) return
    const keep = visitedViews.value.filter((v, i) => v.affix || i >= idx)
    visitedViews.value = keep
    cachedViewNames.value = keep.map((v) => v.name).filter(Boolean)
  }

  /** 关闭右侧标签页（保留 affix + selectedView 及其左侧） */
  function closeRight(selectedView: VisitedView) {
    const idx = visitedViews.value.findIndex((v) => v.fullPath === selectedView.fullPath)
    if (idx < 0) return
    const keep = visitedViews.value.filter((v, i) => v.affix || i <= idx)
    visitedViews.value = keep
    cachedViewNames.value = keep.map((v) => v.name).filter(Boolean)
  }

  /** 关闭所有标签页（只保留 affix） */
  function closeAll(): string | null {
    const affixViews = visitedViews.value.filter((v) => v.affix)
    visitedViews.value = affixViews
    cachedViewNames.value = affixViews.map((v) => v.name).filter(Boolean)
    return affixViews.length ? affixViews[0].fullPath : null
  }

  /** 重置 store（退出登录时调用） */
  function reset() {
    visitedViews.value = []
    cachedViewNames.value = []
  }

  return {
    visitedViews,
    cachedViewNames,
    addTab,
    closeTab,
    closeOthers,
    closeLeft,
    closeRight,
    closeAll,
    reset
  }
})
