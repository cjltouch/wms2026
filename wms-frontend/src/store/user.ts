import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('wms_token') || '')
  const userId = ref<string>('')
  const username = ref<string>('')
  const realName = ref<string>('')
  const avatar = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const routers = ref<any[]>([])

  const isLoggedIn = computed(() => !!token.value)

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
    avatar.value = data.avatar || data.user?.avatar || ''
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    routers.value = data.routers || []
    return data
  }

  function resetState() {
    token.value = ''
    userId.value = ''
    username.value = ''
    realName.value = ''
    avatar.value = ''
    roles.value = []
    permissions.value = []
    routers.value = []
    localStorage.removeItem('wms_token')
  }

  return {
    token, userId, username, realName, avatar, roles, permissions, routers,
    isLoggedIn, login, fetchUserInfo, resetState
  }
})
