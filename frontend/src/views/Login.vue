<template>
  <div class="login-container">
    <div class="glass-card login-box">
      <div class="logo-area">
        <img src="/favicon.ico" class="app-logo"  alt="aa"/>
        <h2>{{ isLogin ? 'Sign in to Apple Admin' : 'Create your Apple ID' }}</h2>
        <p class="subtitle">{{ isLogin ? 'Welcome back, please login to your account.' : 'Get started with your free account today.' }}</p>
      </div>

      <el-form :model="form" class="login-form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="Apple ID" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="Password" :prefix-icon="Lock" show-password />
        </el-form-item>

        <el-button
          type="primary"
          class="submit-btn"
          :loading="loading"
          native-type="button"
          @click="handleAuth"
        >
          {{ isLogin ? 'Sign In' : 'Continue' }}
        </el-button>
      </el-form>

      <div class="footer-links">
        <span v-if="isLogin">Don't have an Apple ID? <a @click="toggleMode">Create one now.</a></span>
        <span v-else>Already have an Apple ID? <a @click="toggleMode">Sign in here.</a></span>
      </div>
    </div>

    <div class="copyright">
      Copyright © 2024 Apple Admin Inc. All rights reserved.
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const isLogin = ref(true)
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.username = ''
  form.password = ''
}

const handleAuth = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请填写完整账号和密码')

  loading.value = true
  try {
    const url = isLogin.value ? '/api/user/login' : '/api/user/register'
    const fullUrl = `http://localhost:8080${url}`

    const res = await axios.post(fullUrl, form)

    if (res.data.code === 200) {
      if (isLogin.value) {
        ElMessage.success('欢迎回来')
        localStorage.setItem('token', 'mock-token-' + Date.now())
        localStorage.setItem('user', JSON.stringify(res.data.data))
        await router.push('/')
      } else {
        ElMessage.success('注册成功，请使用新账号登录')
        isLogin.value = true
      }
    } else {
      ElMessage.error(res.data.msg || '操作失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('网络异常或服务器不可用')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: #000;
  background-image: radial-gradient(circle at 50% 0%, #2c3e50 0%, #000000 100%);
  position: relative;
  overflow: hidden;
}

.login-box {
  width: 400px;
  padding: 48px;
  text-align: center;
  z-index: 10;
  background: rgba(28, 28, 30, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4);
  backdrop-filter: blur(20px);
}

.logo-area {
  margin-bottom: 40px;
}

.app-logo {
  width: 50px;
  margin-bottom: 16px;
  opacity: 0.9;
}

.logo-area h2 {
  font-weight: 600;
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #fff;
  letter-spacing: -0.5px;
}

.subtitle {
  color: #86868b;
  font-size: 15px;
  margin: 0;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 17px;
  margin-top: 24px;
  background: #0071e3;
  border: none;
  border-radius: 980px;
  transition: all 0.3s;
}

.submit-btn:hover {
  background: #0077ed;
  transform: scale(1.02);
}

.footer-links {
  margin-top: 32px;
  font-size: 14px;
  color: #86868b;
}

.footer-links a {
  color: #2997ff;
  cursor: pointer;
  margin-left: 4px;
}

.footer-links a:hover {
  text-decoration: underline;
}

.copyright {
  position: absolute;
  bottom: 24px;
  color: #424245;
  font-size: 12px;
}

/* 强制覆盖 Element Plus 样式，确保输入框可见 */
:deep(.el-input__wrapper) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  box-shadow: none !important;
  height: 48px;
  padding: 0 16px;
  border-radius: 12px;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #0071e3 !important;
  background-color: rgba(255, 255, 255, 0.15) !important;
}

:deep(.el-input__inner) {
  color: #fff !important;
  height: 48px;
}

:deep(.el-input__inner::placeholder) {
  color: #86868b;
}

:deep(.el-input__icon) {
  color: #86868b;
}
</style>