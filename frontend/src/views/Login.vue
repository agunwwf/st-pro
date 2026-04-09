<template>
  <div class="login-container">
    <div class="glass-card login-box">
      <div class="logo-area">
        <img src="/favicon.ico" class="app-logo" />
        <h2>{{ isLogin ? '登录教学平台' : '注册新账号' }}</h2>
        <p class="subtitle">{{ isLogin ? '欢迎回来，请登录你的账号。' : '创建账号后即可开始学习。' }}</p>
      </div>

      <el-form :model="form" class="login-form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="请输入账号ID" :prefix-icon="User" class="apple-input" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password class="apple-input" />
        </el-form-item>

        <el-form-item>
          <el-radio-group v-model="form.role" size="large" class="role-selector">
            <el-radio label="STUDENT">学生</el-radio>
            <el-radio label="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleAuth">
          {{ isLogin ? '登录' : '注册并继续' }}
        </el-button>
      </el-form>

      <div class="footer-links">
        <span v-if="isLogin">还没有账号？<a @click="toggleMode">立即注册</a></span>
        <span v-else>已有账号？<a @click="toggleMode">去登录</a></span>
      </div>
    </div>

    <div class="copyright">
      版权所有 © 2024 教学平台
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
const form = reactive({ username: '', password: '', role: 'STUDENT' })

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
        ElMessage.success('登录成功，欢迎回来')
        const { token, user } = res.data.data
        localStorage.setItem('token', token)
        localStorage.setItem('user', JSON.stringify(user))
        await router.push('/')
      } else {
        ElMessage.success('注册成功，请登录')
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.app-logo {
  width: 50px;
  margin-bottom: 16px;
  opacity: 0.9;
  display: block;
  margin-left: auto;
  margin-right: auto;
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