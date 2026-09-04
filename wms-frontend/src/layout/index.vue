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
              <el-avatar :size="32" :icon="UserFilled" />
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

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 个人中心弹窗 -->
    <el-dialog v-model="profileVisible" title="个人中心" width="440px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="用户名">{{ userStore.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.realName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ (userStore.roles || []).join('、') || '—' }}</el-descriptions-item>
      </el-descriptions>
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
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitChangePwd">修改密码</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox, ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
