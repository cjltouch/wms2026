import type { Directive } from 'vue'
import { hasPerm } from '@/utils/perm'

/**
 * 权限控制指令：无权限时隐藏元素（display:none）
 *
 * 用法：
 *   <el-button v-perm="'wms:stock-out:add'">新增</el-button>
 *   <el-button v-perm="['wms:stock-out:add', 'wms:stock-out:edit']">任一即可</el-button>
 */
export const perm: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    applyPerm(el, binding.value)
  },
  updated(el, binding) {
    applyPerm(el, binding.value)
  }
}

function applyPerm(el: HTMLElement, value: string | string[] | undefined) {
  if (!value) return
  let ok: boolean
  if (Array.isArray(value)) {
    ok = value.some((v) => hasPerm(v))
  } else {
    ok = hasPerm(value)
  }
  if (!ok) {
    el.style.display = 'none'
    // 标记一下，方便调试
    el.setAttribute('data-perm-denied', value as string)
  } else {
    el.style.display = ''
    el.removeAttribute('data-perm-denied')
  }
}
