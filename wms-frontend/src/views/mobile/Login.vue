<template>
  <div class="m-login">
    <!-- 背景装饰 -->
    <div class="bg-shape bg-1"></div>
    <div class="bg-shape bg-2"></div>

    <!-- 品牌区 -->
    <div class="brand">
      <div class="brand-logo">
        <van-icon name="shopping-cart-o" size="36" />
      </div>
      <h1 class="brand-title">WMS 仓储管理</h1>
      <p class="brand-sub">仓储作业 · 移动端</p>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-title">账号登录</div>

      <van-form @submit="handleLogin">
        <div class="input-wrap">
          <van-icon name="manager-o" class="input-icon" />
          <input
            v-model="form.username"
            class="native-input"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>

        <div class="input-wrap">
          <van-icon name="lock" class="input-icon" />
          <input
            v-model="form.password"
            type="password"
            class="native-input"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <van-loading v-if="loading" size="18" color="#fff" />
          <span v-else>登 录</span>
        </button>
      </van-form>
    </div>

    <div class="footer-tip">© WMS Warehouse Management System</div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '@/store/user'

defineOptions({ name: 'MobileLogin' })

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

async function handleLogin() {
  if (!form.username?.trim()) {
    showToast('请输入用户名')
    return
  }
  if (!form.password?.trim()) {
    showToast('请输入密码')
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    await userStore.fetchUserInfo()
    showToast({ message: '登录成功', type: 'success' })
    router.replace('/mobile/dashboard')
  } catch (e: any) {
    showToast(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.m-login {
  min-height: 100vh;
  position: relative;
  background: linear-gradient(160deg, #667eea 0%, #764ba2 50%, #6a11cb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 24px;
  padding-top: 15vh;
  box-sizing: border-box;
  overflow: hidden;
}

/* 背景装饰 */
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.3;
  pointer-events: none;
}

.bg-1 {
  width: 280px;
  height: 280px;
  background: #ff9a9e;
  top: -80px;
  right: -60px;
}

.bg-2 {
  width: 220px;
  height: 220px;
  background: #a1c4fd;
  bottom: 120px;
  left: -80px;
}

/* 品牌 */
.brand {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.brand-logo {
  width: 76px;
  height: 76px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  color: #fff;
}

.brand-title {
  color: #fff;
  font-size: 26px;
  font-weight: 600;
  margin: 0 0 6px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.brand-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin: 0;
  letter-spacing: 2px;
}

/* 登录卡片 */
.login-card {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 20px;
  padding: 28px 22px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 1;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 24px;
}

.input-wrap {
  display: flex;
  align-items: center;
  background: #f5f6fa;
  border-radius: 12px;
  padding: 0 14px;
  height: 52px;
  margin-bottom: 14px;
  transition: all 0.2s;
}

.input-wrap:focus-within {
  background: #fff;
  box-shadow: 0 0 0 2px #667eea;
}

.input-icon {
  font-size: 20px;
  color: #999;
  margin-right: 10px;
  flex-shrink: 0;
}

.native-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: #333;
  height: 100%;
}

.native-input::placeholder {
  color: #bbb;
}

.submit-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 4px;
  cursor: pointer;
  margin-top: 12px;
  transition: transform 0.15s, opacity 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn:active {
  transform: scale(0.98);
}

.submit-btn:disabled {
  opacity: 0.7;
}

/* 底部版权 */
.footer-tip {
  position: fixed;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  letter-spacing: 1px;
}
</style>
