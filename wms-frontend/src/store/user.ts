import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth'
import { resolveAvatarUrl, getRandomDefaultAvatar } from '@/utils/avatar'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('wms_token') || '')
  const userId = ref<string>('')
  const username = ref<string>('')
  const realName = ref<string>('')
  /** 原始头像字段（后端返回的相对路径 /uploads/xxx 或 null） */
  const avatarRaw = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const routers = ref<any[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /** 展示用头像：自动解析后端路径 + 兜底默认卡通头像 */
  const avatar = computed(() => resolveAvatarUrl(avatarRaw.value))

  async function login(loginForm: { username: string; password: string }) {
    const res: any = await loginApi(loginForm)
    token.value = res.data.accessToken
    localStorage.setItem('wms_token', token.value)
    return res
  }

  async function fetchUserInfo() {
    const res: any = await getUserInfoApi()
    const data = res.data
    userId.value = data.userId || data.user?.userId || ''
    username.value = data.username || data.user?.username || ''
    realName.value = data.realName || data.user?.realName || ''
    // 后端 avatar 可能为 null 或空字符串 → 前端给一个随机默认头像（但不回写后端，保持后端数据干净）
    avatarRaw.value = (data.avatar || data.user?.avatar || '') as string
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    routers.value = data.routers || []
    return data
  }

  /**
   * 更新当前用户头像（上传成功或选择默认卡通头像后调用）
   * @param newAvatar 后端返回的相对路径（如 /uploads/avatars/xxx.png）或前端默认头像路径（如 /avatars/avatar-1.svg）
   */
  function setAvatar(newAvatar: string) {
    avatarRaw.value = newAvatar
  }

  function resetState() {
    token.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    avatarRaw.value = ''
    roles.value = []
    permissions.value = []
    routers.value = []
    localStorage.removeItem('wms_token')
  }

  return {
    token, userId, username, realName, avatarRaw, avatar, roles, permissions, routers,
    isLoggedIn, login, fetchUserInfo, setAvatar, resetState
  }
})
