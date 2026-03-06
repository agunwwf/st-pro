<template>
  <div class="app-wrapper" :class="{ dark: isDark }">
    <!-- 侧边栏 -->
    <aside class="sidebar glass-card" :class="{ collapsed: isCollapse }">
      <div class="sidebar-header">
        <div class="traffic-lights">
          <span></span><span></span><span></span>
        </div>
      </div>

      <nav class="nav-menu">
        <router-link to="/dashboard" class="nav-item" active-class="active">
          <el-icon><Odometer /></el-icon>
          <span v-if="!isCollapse">Dashboard</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="active">
          <el-icon><Folder /></el-icon>
          <span v-if="!isCollapse">Projects</span>
        </router-link>
        <router-link to="/chat" class="nav-item" active-class="active">
          <el-icon><ChatRound /></el-icon>
          <span v-if="!isCollapse">Messages</span>
        </router-link>
        <!-- Management 菜单始终展示，真正的权限控制交由路由守卫处理 -->
        <router-link to="/management" class="nav-item" active-class="active">
          <el-icon><Monitor /></el-icon>
          <span v-if="!isCollapse">Management</span>
        </router-link>
        <router-link to="/calendar" class="nav-item" active-class="active">
          <el-icon><Calendar /></el-icon>
          <span v-if="!isCollapse">Calendar</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-profile" @click="router.push('/profile')">
          <el-avatar :size="36" :src="userAvatar" />
          <div class="user-info" v-if="!isCollapse">
            <span class="name">{{ userName }}</span>
            <span class="status">Online</span>
          </div>
        </div>
        <div class="actions" v-if="!isCollapse">
          <el-icon @click="toggleDark()" class="action-icon"><Moon v-if="!isDark"/><Sunny v-else/></el-icon>
          <el-icon @click="handleLogout" class="action-icon"><SwitchButton /></el-icon>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <header class="top-bar">
        <div class="breadcrumb">
          <el-icon @click="isCollapse = !isCollapse" class="toggle-icon"><Menu /></el-icon>
          <span>{{ route.meta.title }}</span>
        </div>
      </header>

      <div class="content-scroll">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useDark, useToggle } from '@vueuse/core'
import { Odometer, Folder, ChatRound, Calendar, Moon, Sunny, SwitchButton, Menu, Monitor } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const isDark = useDark()
const toggleDark = useToggle(isDark)

const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
const userName = ref('User')
const userRole = ref('STUDENT')

function pad2(n) {
  return String(n).padStart(2, '0')
}

function toLocalDateKey(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function getTodayKey() {
  return toLocalDateKey(new Date())
}

function getStudyStorageKey(userId) {
  return `study-time:${userId}`
}

function readUser() {
  try {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function loadStudyState(userId) {
  const key = getStudyStorageKey(userId)
  try {
    const raw = localStorage.getItem(key)
    const parsed = raw ? JSON.parse(raw) : null
    if (parsed && typeof parsed === 'object') return parsed
  } catch {
    // ignore
  }
  return { date: getTodayKey(), seconds: 0 }
}

function persistStudyState(userId, state) {
  localStorage.setItem(getStudyStorageKey(userId), JSON.stringify(state))
}

let studyTickTimer = null
let studyFlushTimer = null
let studyDayTimer = null
let studyUserId = null
let studyState = null
let visibilityHandler = null
let beforeUnloadHandler = null

function stopStudyTimers() {
  if (studyTickTimer) clearInterval(studyTickTimer)
  if (studyFlushTimer) clearInterval(studyFlushTimer)
  if (studyDayTimer) clearInterval(studyDayTimer)
  studyTickTimer = null
  studyFlushTimer = null
  studyDayTimer = null

  if (visibilityHandler) document.removeEventListener('visibilitychange', visibilityHandler)
  if (beforeUnloadHandler) window.removeEventListener('beforeunload', beforeUnloadHandler)
  visibilityHandler = null
  beforeUnloadHandler = null
  studyUserId = null
  studyState = null
}

function startStudyTimers() {
  stopStudyTimers()
  const u = readUser()
  if (!u || !u.id) return

  const userId = u.id
  let state = loadStudyState(userId)
  const today = getTodayKey()
  if (state.date !== today) {
    state = { date: today, seconds: 0 }
    persistStudyState(userId, state)
  }

  studyUserId = userId
  studyState = state

  const notify = () => {
    window.dispatchEvent(new CustomEvent('study-time-updated', { detail: { ...studyState, userId: studyUserId } }))
  }

  const canCountNow = () => document.visibilityState === 'visible'

  // 每秒计时（只有页面处于可见状态才算“在线学习”）
  studyTickTimer = setInterval(() => {
    if (!canCountNow()) return
    studyState.seconds += 1
  }, 1000)

  // 定期落盘 + 通知页面（避免刷新/关闭清零）
  studyFlushTimer = setInterval(() => {
    persistStudyState(studyUserId, studyState)
    notify()
  }, 5000)

  // 跨天自动清零（每分钟检查一次）
  studyDayTimer = setInterval(() => {
    const nowKey = getTodayKey()
    if (studyState.date !== nowKey) {
      studyState = { date: nowKey, seconds: 0 }
      persistStudyState(studyUserId, studyState)
      notify()
    }
  }, 60 * 1000)

  // 页面切到后台时，立即落盘一次
  visibilityHandler = () => {
    if (!studyUserId || !studyState) return
    persistStudyState(studyUserId, studyState)
    notify()
  }
  document.addEventListener('visibilitychange', visibilityHandler)

  // 刷新/关闭前落盘
  beforeUnloadHandler = () => {
    if (!studyUserId || !studyState) return
    persistStudyState(studyUserId, studyState)
  }
  window.addEventListener('beforeunload', beforeUnloadHandler)

  // 初始通知一次
  notify()
}

const updateUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      userAvatar.value = user.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      userName.value = user.nickname || user.username || 'User'
      userRole.value = user.role || 'STUDENT'
    } catch (e) {
      console.error(e)
    }
  }
}

const handleUserUpdate = (event) => {
  if (event.detail) {
    userAvatar.value = event.detail.avatar || userAvatar.value
    userName.value = event.detail.nickname || userName.value
  } else {
    updateUser()
  }
  // 登录用户可能变化，重启学习计时
  startStudyTimers()
}

const handleLogout = () => {
  stopStudyTimers()
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  updateUser()
  window.addEventListener('user-updated', handleUserUpdate)
  startStudyTimers()
})

onUnmounted(() => {
  window.removeEventListener('user-updated', handleUserUpdate)
  stopStudyTimers()
})
</script>

<style scoped lang="scss">
.app-wrapper {
  display: flex;
  height: 100vh;
  background-color: var(--apple-bg);
  overflow: hidden;
}

.sidebar {
  width: 240px;
  margin: 16px;
  margin-right: 0;
  display: flex;
  flex-direction: column;
  transition: width 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  z-index: 100;

  &.collapsed {
    width: 80px;
    .nav-item span, .user-info, .actions { display: none; }
    .nav-item { justify-content: center; padding: 0; }
  }

  .sidebar-header {
    padding: 24px;
    .traffic-lights {
      display: flex;
      gap: 8px;
      span {
        width: 12px; height: 12px; border-radius: 50%;
        &:nth-child(1) { background: #FF5F57; }
        &:nth-child(2) { background: #FEBC2E; }
        &:nth-child(3) { background: #28C840; }
      }
    }
  }

  .nav-menu {
    flex: 1;
    padding: 0 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .nav-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-radius: 12px;
    color: var(--apple-text-secondary);
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
    height: 44px;

    .el-icon { font-size: 20px; margin-right: 12px; }

    &:hover { background: rgba(0,0,0,0.05); color: var(--apple-text-primary); }
    &.active { background: var(--apple-blue); color: white; }
  }

  .sidebar-footer {
    padding: 20px;
    border-top: 1px solid rgba(0,0,0,0.05);

    .user-profile {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      .user-info {
        display: flex; flex-direction: column;
        .name { font-weight: 600; font-size: 14px; }
        .status { font-size: 12px; color: #28C840; }
      }
    }

    .actions {
      margin-top: 16px;
      display: flex;
      justify-content: space-between;
      .action-icon { cursor: pointer; font-size: 18px; color: var(--apple-text-secondary); }
    }
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

.top-bar {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;

  .breadcrumb {
    display: flex;
    align-items: center;
    font-size: 24px;
    font-weight: 700;
    gap: 16px;
    .toggle-icon { cursor: pointer; font-size: 20px; color: var(--apple-text-secondary); }
  }
}

.content-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  /* 隐藏滚动条但保留功能 */
  &::-webkit-scrollbar { width: 0; }
}

/* 页面切换动画 */
.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}
.fade-slide-enter-from { opacity: 0; transform: translateY(20px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-20px); }
</style>