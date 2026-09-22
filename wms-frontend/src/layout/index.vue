<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon size="28" color="#409eff"><Box /></el-icon>
        <span v-show="!isCollapse" class="logo-text">WMS 仓储系统</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <el-sub-menu v-if="route.children && route.children.length > 1" :index="route.path">
              <template #title>
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children"
                :key="child.path"
                :index="child.path"
              >
                <el-icon><component :is="child.meta?.icon || 'Menu'" /></el-icon>
                <template #title>{{ child.meta?.title }}</template>
              </el-menu-item>
            </el-sub-menu>
            <!-- 只有 1 个子路由：直接展示子路由（用子路由的 title/icon，避免 仪表盘/采购订单 这类菜单名不对） -->
            <el-menu-item
              v-else-if="route.children && route.children.length === 1"
              :index="route.children[0].path"
            >
              <el-icon><component :is="route.children[0].meta?.icon || route.meta?.icon || 'Menu'" /></el-icon>
              <template #title>{{ route.children[0].meta?.title || route.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title">{{ currentRoute.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.avatar" :icon="UserFilled" />
              <span class="username">{{ userStore.realName || userStore.username || 'Admin' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 多标签页导航 -->
      <TagsView />

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <keep-alive :include="tabsStore.cachedViewNames">
            <component :is="Component" :key="route.fullPath" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>

    <!-- 个人中心弹窗 -->
    <el-dialog v-model="profileVisible" title="个人中心" width="560px">
      <!-- 上半区：头像 + 基本信息 -->
      <div class="profile-top">
        <div class="profile-avatar-area">
          <el-avatar :size="96" :src="userStore.avatar" :icon="UserFilled" />
          <div class="avatar-actions">
            <el-button size="small" :loading="avatarLoading" @click="triggerAvatarUpload">
              上传头像
            </el-button>
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp,image/svg+xml"
              style="display:none"
              @change="handleAvatarFileChange"
            />
          </div>
          <div class="avatar-tip">支持 PNG/JPG/GIF/WEBP/SVG，最大 5MB</div>
        </div>
        <div class="profile-info">
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ userStore.username || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userStore.realName || '—' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">角色</span>
            <span class="info-value">{{ (userStore.roles || []).join('、') || '—' }}</span>
          </div>
        </div>
      </div>

      <!-- 默认卡通头像选择 -->
      <div class="profile-section-title">选择默认卡通头像</div>
      <div class="default-avatar-grid">
        <div
          v-for="av in defaultAvatarList"
          :key="av"
          class="default-avatar-item"
          :class="{ active: userStore.avatarRaw === av }"
          @click="selectDefaultAvatar(av)"
        >
          <img :src="av" :alt="av" />
          <el-icon v-if="userStore.avatarRaw === av" class="check-icon"><Check /></el-icon>
        </div>
      </div>

      <!-- 修改密码 -->
      <div class="profile-section-title">修改密码</div>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">关闭</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitChangePwd">修改密码</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useTabsStore } from '@/store/tabs'
import { ElMessageBox, ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled, Check } from '@element-plus/icons-vue'
import { changePassword, uploadAvatar as uploadAvatarApi, updateAvatar as updateAvatarApi } from '@/api/auth'
import { getDefaultAvatarList } from '@/utils/avatar'
import TagsView from './components/TagsView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

// ===== 侧边栏菜单：严格以后端返回的菜单权限树为准（admin 返回全部）=====
// 未分配任何菜单权限时侧边栏为空，绝不降级显示全部静态路由，避免越权菜单暴露
const menuRoutes = computed(() => {
  const routers: any[] = userStore.routers || []
  return routers.map((r: any) => {
    const topPath = r.path?.startsWith('/') ? r.path : '/' + (r.path || '')
    const topMeta = { title: r.meta?.title || '', icon: r.meta?.icon || 'Menu' }
    if (r.children && r.children.length) {
      return {
        path: topPath,
        meta: topMeta,
        children: r.children.map((c: any) => ({
          path: resolveMenuPath(topPath, c.path),
          meta: { title: c.meta?.title || '', icon: c.meta?.icon || topMeta.icon }
        }))
      }
    }
    // 顶级单页面（如仪表盘）：包装成单子路由结构渲染为一个菜单项
    return { path: '', meta: { title: '', icon: '' }, children: [{ path: topPath, meta: topMeta }] }
  })
})

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.resetState()
      tabsStore.reset()
      ElMessage.success('已退出登录')
      router.push('/login')
    })
  } else if (command === 'profile') {
    profileVisible.value = true
  }
}

// ===== 个人中心：自助修改密码 =====
const profileVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: (err?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submitChangePwd() {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      ElMessage.success('密码修改成功，下次登录请使用新密码')
      profileVisible.value = false
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } catch (e) {
      // handled by interceptor
    } finally {
      pwdLoading.value = false
    }
  })
}

// ===== 个人中心：头像管理 =====
const defaultAvatarList = getDefaultAvatarList()
const avatarInputRef = ref<HTMLInputElement | null>(null)
const avatarLoading = ref(false)

/** 点击上传按钮 → 触发隐藏的 file input */
function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

/** 文件选择后 → 校验 + 上传 */
async function handleAvatarFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  // 清理 input，确保下次选择同一文件也能触发 change
  input.value = ''

  // 前端校验
  const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp', 'image/svg+xml']
  if (!allowedTypes.includes(file.type.toLowerCase())) {
    ElMessage.error('仅支持 PNG、JPG、GIF、WEBP、SVG 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过 5MB')
    return
  }

  avatarLoading.value = true
  try {
    const res: any = await uploadAvatarApi(file)
    const url = res.data?.avatar || res.avatar
    if (url) {
      userStore.setAvatar(url)
      ElMessage.success('头像上传成功')
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    avatarLoading.value = false
  }
}

/** 选择默认卡通头像 → 调后端保存 */
async function selectDefaultAvatar(avatarPath: string) {
  try {
    await updateAvatarApi(avatarPath)
    userStore.setAvatar(avatarPath)
    ElMessage.success('头像已更换')
  } catch (e) {
    // handled by interceptor
  }
}

/**
 * 把父路由路径 + 子路由路径拼接成一个干净的绝对路径，
 * 避免 `'/' + 'dashboard'` 拼出 `//dashboard` 双斜杠导致跳转失败。
 */
function resolveMenuPath(parentPath: string, childPath: string): string {
  if (!parentPath) parentPath = '/'
  if (!childPath) return parentPath
  const base = parentPath.endsWith('/') ? parentPath.slice(0, -1) : parentPath
  const child = childPath.startsWith('/') ? childPath : '/' + childPath
  return base + child || '/'
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  display: flex;
  flex-direction: column;
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    flex-shrink: 0;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: #2b2f3a;

    .logo-text {
      color: #fff;
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  // 菜单滚动区：约束在 logo 以下的剩余高度内，菜单过多时可滚动
  :deep(.el-scrollbar) {
    flex: 1;
    min-height: 0;
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      cursor: pointer;
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

.main-content {
  background: #f0f2f5;
  overflow-y: auto;
}

.profile-section-title {
  margin: 16px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303030;

  &::before {
    content: '';
    display: inline-block;
    width: 3px;
    height: 14px;
    background: #409eff;
    margin-right: 6px;
    vertical-align: middle;
  }
}

.profile-top {
  display: flex;
  gap: 24px;
  padding: 8px 0 16px;
  border-bottom: 1px solid #f0f0f0;

  .profile-avatar-area {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    .avatar-tip {
      font-size: 12px;
      color: #999;
    }
  }

  .profile-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;

    .info-row {
      display: flex;
      align-items: center;
      gap: 12px;

      .info-label {
        width: 60px;
        font-size: 13px;
        color: #909399;
        flex-shrink: 0;
      }

      .info-value {
        font-size: 14px;
        color: #303133;
      }
    }
  }
}

.default-avatar-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;

  .default-avatar-item {
    position: relative;
    width: 100%;
    aspect-ratio: 1;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid transparent;
    overflow: visible;
    transition: all 0.2s;

    img {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      border: 2px solid #ebeef5;
      transition: border-color 0.2s;
    }

    &:hover img {
      border-color: #409eff;
    }

    &.active {
      border-color: #409eff;

      img {
        border-color: #409eff;
      }
    }

    .check-icon {
      position: absolute;
      bottom: -2px;
      right: -2px;
      width: 20px;
      height: 20px;
      background: #409eff;
      border-radius: 50%;
      color: #fff;
      padding: 2px;
      border: 2px solid #fff;
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
