<template>
  <div class="m-login">
    <!-- ===== 多层漂浮光斑背景 ===== -->
    <div class="orb orb-1"></div>
    <div class="orb orb-2"></div>
    <div class="orb orb-3"></div>
    <div class="orb orb-4"></div>

    <!-- 漂浮几何装饰 -->
    <div class="float-shape shape-a"></div>
    <div class="float-shape shape-b"></div>
    <div class="float-shape shape-c"></div>

    <!-- 品牌区 -->
    <div class="brand">
      <div class="brand-logo">
        <div class="logo-ring"></div>
        <van-icon name="shopping-cart-o" size="34" />
      </div>
      <h1 class="brand-title">WMS 仓储管理</h1>
      <p class="brand-sub">仓储作业 · 移动端</p>
    </div>

    <!-- 登录卡片（玻璃拟态） -->
    <div class="login-card">
      <div class="card-glow"></div>
      <div class="card-title">
        <span class="title-bar"></span>账号登录
      </div>

      <van-form @submit="handleLogin">
        <div class="input-wrap">
          <div class="input-icon">
            <van-icon name="manager-o" />
          </div>
          <input
            v-model="form.username"
            class="native-input"
            placeholder="请输入用户名"
            autocomplete="username"
          />
          <div class="input-underline"></div>
        </div>

        <div class="input-wrap">
          <div class="input-icon">
            <van-icon name="lock" />
          </div>
          <input
            v-model="form.password"
            type="password"
            class="native-input"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
          <div class="input-underline"></div>
        </div>

        <button type="submit" class="submit-btn" :disabled="loading">
          <span class="btn-glow"></span>
          <van-loading v-if="loading" size="18" color="#fff" />
          <template v-else>
            <span class="btn-text">登 录</span>
            <van-icon name="arrow" size="16" />
          </template>
        </button>
      </van-form>

      <div class="card-foot">
        <span class="dot-sep"></span>
        企业仓储管理系统
        <span class="dot-sep"></span>
      </div>
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
  background:
    radial-gradient(circle at 20% 15%, rgba(255, 154, 158, 0.35) 0%, transparent 45%),
    radial-gradient(circle at 80% 20%, rgba(129, 196, 253, 0.4) 0%, transparent 50%),
    radial-gradient(circle at 70% 85%, rgba(186, 130, 255, 0.35) 0%, transparent 45%),
    linear-gradient(160deg, #5a67d8 0%, #764ba2 45%, #6a11cb 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 26px;
  padding-top: 14vh;
  box-sizing: border-box;
  overflow: hidden;
}

/* ===== 漂浮光斑（多层模糊圆） ===== */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(50px);
  pointer-events: none;
  animation: orbFloat 14s ease-in-out infinite;
}

.orb-1 {
  width: 240px;
  height: 240px;
  background: rgba(255, 154, 200, 0.55);
  top: -60px;
  right: -50px;
  animation-delay: 0s;
}

.orb-2 {
  width: 200px;
  height: 200px;
  background: rgba(129, 196, 253, 0.5);
  bottom: 140px;
  left: -70px;
  animation-delay: -3.5s;
}

.orb-3 {
  width: 160px;
  height: 160px;
  background: rgba(255, 255, 255, 0.3);
  top: 40%;
  right: -40px;
  animation-delay: -7s;
}

.orb-4 {
  width: 140px;
  height: 140px;
  background: rgba(186, 130, 255, 0.5);
  bottom: 60px;
  right: 20%;
  animation-delay: -10s;
}

@keyframes orbFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33%      { transform: translate(20px, -25px) scale(1.08); }
  66%      { transform: translate(-15px, 20px) scale(0.94); }
}

/* ===== 漂浮几何装饰 ===== */
.float-shape {
  position: absolute;
  pointer-events: none;
  opacity: 0.5;
  animation: shapeFloat 12s ease-in-out infinite;
}

.shape-a {
  width: 14px;
  height: 14px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 4px;
  top: 22%;
  left: 12%;
  transform: rotate(20deg);
  animation-delay: 0s;
}

.shape-b {
  width: 10px;
  height: 10px;
  border: 2px solid rgba(255, 255, 255, 0.55);
  border-radius: 50%;
  top: 35%;
  right: 14%;
  animation-delay: -4s;
}

.shape-c {
  width: 18px;
  height: 3px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 2px;
  bottom: 28%;
  left: 18%;
  transform: rotate(-30deg);
  animation-delay: -8s;
}

@keyframes shapeFloat {
  0%, 100% { transform: translateY(0) rotate(var(--r, 20deg)); }
  50%      { transform: translateY(-18px) rotate(var(--r, 20deg)); }
}

.shape-a { --r: 20deg; }
.shape-c { --r: -30deg; }

/* ===== 品牌区 ===== */
.brand {
  text-align: center;
  margin-bottom: 36px;
  position: relative;
  z-index: 2;
}

.brand-logo {
  width: 78px;
  height: 78px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  box-shadow:
    0 10px 30px rgba(76, 72, 162, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  position: relative;
}

/* Logo 外圈呼吸光环 */
.logo-ring {
  position: absolute;
  inset: -6px;
  border-radius: 28px;
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  animation: ringPulse 3s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50%      { transform: scale(1.12); opacity: 0; }
}

.brand-title {
  color: #fff;
  font-size: 27px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 1.5px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.18);
}

.brand-sub {
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  margin: 0;
  letter-spacing: 3px;
}

/* ===== 登录卡片（玻璃拟态） ===== */
.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
  border-radius: 24px;
  padding: 28px 22px 20px;
  box-shadow:
    0 20px 60px rgba(45, 35, 110, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.25);
  position: relative;
  z-index: 2;
  overflow: hidden;
}

/* 卡片右上角光晕 */
.card-glow {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.4) 0%, transparent 70%);
  pointer-events: none;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.title-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: linear-gradient(180deg, #fff, rgba(255, 255, 255, 0.4));
}

/* ===== 输入框（玻璃拟态） ===== */
.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 0 14px;
  height: 52px;
  margin-bottom: 14px;
  transition: all 0.25s;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.input-wrap:focus-within {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.15),
    0 4px 16px rgba(102, 126, 234, 0.25);
}

.input-icon {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.75);
  font-size: 18px;
}

.input-wrap:focus-within .input-icon {
  color: #fff;
}

.native-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 15px;
  color: #fff;
  height: 100%;
}

.native-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

/* 输入框底部高亮线（聚焦时展开） */
.input-underline {
  position: absolute;
  bottom: 4px;
  left: 50%;
  width: 0;
  height: 2px;
  border-radius: 1px;
  background: linear-gradient(90deg, transparent, #fff, transparent);
  transition: width 0.3s, left 0.3s;
}

.input-wrap:focus-within .input-underline {
  width: 70%;
  left: 15%;
}

/* ===== 提交按钮 ===== */
.submit-btn {
  position: relative;
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff 0%, #e0e7ff 100%);
  color: #5a67d8;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 4px;
  cursor: pointer;
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  overflow: hidden;
  box-shadow:
    0 8px 24px rgba(102, 126, 234, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  transition: transform 0.15s, box-shadow 0.2s;
}

.submit-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.submit-btn:disabled {
  opacity: 0.7;
}

/* 按钮光泽 */
.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  transition: left 0.6s;
}

.submit-btn:active .btn-glow {
  left: 100%;
}

.btn-text {
  letter-spacing: 4px;
}

/* ===== 卡片底部 ===== */
.card-foot {
  text-align: center;
  margin-top: 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.dot-sep {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
}

/* ===== 底部版权 ===== */
.footer-tip {
  position: fixed;
  bottom: calc(env(safe-area-inset-bottom) + 18px);
  left: 0;
  right: 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  letter-spacing: 1px;
  z-index: 1;
}
</style>
