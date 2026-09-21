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

/** 上传头像（multipart/form-data），同时后端会自动更新当前用户头像 */
export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request<{ avatar: string }>({
    url: '/api/system/auth/upload-avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 更新当前用户头像（使用指定 URL，如选择默认卡通头像时） */
export function updateAvatar(avatar: string) {
  return request<void>({
    url: '/api/system/auth/update-avatar',
    method: 'put',
    data: { avatar }
  })
}
