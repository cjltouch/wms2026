import request from '@/utils/request'

export function login(data: { username: string; password: string }) {
  return request({ url: '/api/system/auth/login', method: 'post', data })
}

export function getUserInfo() {
  return request({ url: '/api/system/auth/user-info', method: 'get' })
}

export function logout() {
  return request({ url: '/api/system/auth/logout', method: 'post' })
}

export function getCaptcha() {
  return request({ url: '/api/system/auth/captcha', method: 'get' })
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request({ url: '/api/system/auth/change-password', method: 'post', data })
}
