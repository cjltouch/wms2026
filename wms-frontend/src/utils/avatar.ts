/** 默认卡通头像路径列表（前端 public/avatars/ 下的 SVG） */
const DEFAULT_AVATARS = [
  '/avatars/avatar-1.svg',
  '/avatars/avatar-2.svg',
  '/avatars/avatar-3.svg',
  '/avatars/avatar-4.svg',
  '/avatars/avatar-5.svg',
  '/avatars/avatar-6.svg'
]

/** 前端默认头像：取第一个卡通头像 */
export const DEFAULT_AVATAR = DEFAULT_AVATARS[0]

/**
 * 获取默认头像列表（用于个人中心选择）
 */
export function getDefaultAvatarList(): string[] {
  return [...DEFAULT_AVATARS]
}

/**
 * 随机选择一个默认卡通头像
 */
export function getRandomDefaultAvatar(): string {
  const idx = Math.floor(Math.random() * DEFAULT_AVATARS.length)
  return DEFAULT_AVATARS[idx]
}

/**
 * 解析头像 URL：
 * - 空值 → 返回随机默认卡通头像
 * - /uploads/xxx → 后端上传的头像，需要拼上 API base (/wms-api) 才能访问
 * - /avatars/xxx → 前端静态默认头像，直接可用
 * - http(s):// → 外部完整 URL，直接返回
 */
export function resolveAvatarUrl(avatar: string | null | undefined): string {
  if (!avatar) return getRandomDefaultAvatar()
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  if (avatar.startsWith('/uploads/')) {
    // 后端 context-path 是 /wms-api，需要拼上
    return '/wms-api' + avatar
  }
  return avatar
}

/**
 * 判断头像是否为后端上传的（需要登录态才能访问的 /uploads/ 路径）
 */
export function isUploadedAvatar(avatar: string | null | undefined): boolean {
  return !!avatar && avatar.startsWith('/uploads/')
}
