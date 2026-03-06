<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section glass-card">
      <div class="welcome-text">
        <h1>{{ greeting }}, {{ user.nickname || user.username }}</h1>
        <p>今天已在线学习 <strong class="online-time">{{ formatOnlineTime(onlineSeconds) }}</strong>，继续保持！</p>
      </div>
      <div class="welcome-stats">
        <div class="stat-pill">
          <span class="label">连续打卡</span>
          <span class="value">{{ streakCount }} 天</span>
        </div>
      </div>
    </div>

    <!-- 统计网格 -->
    <div class="stats-grid">
      <div class="stat-card glass-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.color }">
          <el-icon><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- 底部网格：学习线程 + 快速操作 -->
    <div class="bottom-grid">
      <div class="learning-thread glass-card">
        <div class="section-header">
          <h3>学习线程</h3>
          <el-button
              type="primary"
              class="update-btn"
              @click="handleOpenUpdate"
          >
            {{ todayThread ? '修改今日进展' : '+ 更新进展' }}
          </el-button>
        </div>

        <div class="thread-timeline">
          <div v-for="(item, index) in threadList" :key="index" class="thread-item">
            <div class="thread-line">
              <div class="dot"></div>
              <div v-if="index !== threadList.length - 1" class="line"></div>
            </div>
            <div class="thread-content">
              <div class="thread-date">{{ formatDate(item.createTime) }}</div>
              <div class="thread-title">{{ item.title }}</div>
              <div class="thread-desc">{{ item.content }}</div>
            </div>
          </div>
          <div v-if="threadList.length === 0" class="empty-thread">
            暂无学习线程，开始记录你的第一步吧！
          </div>
        </div>
      </div>

      <div class="side-column">
        <div class="quick-actions glass-card">
          <h3>快速操作</h3>
          <div class="action-buttons">
            <el-button type="primary" round @click="$router.push('/calendar')">去打卡</el-button>
            <el-button type="success" round @click="$router.push('/chat')">去聊天</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 更新进展弹窗 -->
    <el-dialog v-model="showUpdateDialog" :title="todayThread ? '修改今日进展' : '更新学习进展'" width="500px" align-center class="apple-dialog">
      <el-form :model="updateForm" label-position="top" class="update-form">
        <el-form-item label="进展标题" required>
          <el-input v-model="updateForm.title" placeholder="例如：网站开发、学术分享" size="large" />
        </el-form-item>
        <el-form-item label="详细内容" required>
          <el-input v-model="updateForm.content" type="textarea" :rows="4" placeholder="描述一下你今天完成了什么..." size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showUpdateDialog = false" round size="large">取消</el-button>
          <el-button type="primary" @click="submitUpdate" :loading="submitting" round size="large">发布</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue'
import { Timer, Checked, Star, Reading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const user = ref({})
const threadList = ref([])
const todayThread = ref(null)
const streakCount = ref(0)
const onlineSeconds = ref(0)
const showUpdateDialog = ref(false)
const submitting = ref(false)
const updateForm = reactive({ id: null, title: '', content: '' })

let studyHandler = null

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

function loadStudySeconds(userId) {
  try {
    const raw = localStorage.getItem(getStudyStorageKey(userId))
    const parsed = raw ? JSON.parse(raw) : null
    if (parsed && parsed.date === getTodayKey() && typeof parsed.seconds === 'number') {
      return parsed.seconds
    }
  } catch {
    // ignore
  }
  return 0
}

function getCheckinStorageKey(userId) {
  return `checkin-records:${userId}`
}

function loadCheckinRecords(userId) {
  try {
    const raw = localStorage.getItem(getCheckinStorageKey(userId))
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function getYearSuccessCount(records) {
  const year = String(new Date().getFullYear())
  let count = 0
  for (const [key, value] of Object.entries(records || {})) {
    if (value === 'success' && String(key).startsWith(year)) count++
  }
  return count
}

function getStreakCount(records) {
  // 从今天开始往前数连续 success
  let count = 0
  const cursor = new Date()
  for (;;) {
    const key = toLocalDateKey(cursor)
    if (records && records[key] === 'success') {
      count++
      cursor.setDate(cursor.getDate() - 1)
      continue
    }
    break
  }
  return count
}

async function syncYearCheckinCountToServer() {
  if (!user.value || !user.value.id) return
  const records = loadCheckinRecords(user.value.id)
  const count = getYearSuccessCount(records)
  try {
    await axios.post('http://localhost:8080/api/user/checkin', { id: user.value.id, count })
  } catch (e) {
    console.error('同步打卡天数失败', e)
  }
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 5) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const stats = computed(() => [
  { label: '今日学习', value: formatOnlineTime(onlineSeconds.value), icon: Timer, color: '#0071E3' },
  { label: '已完成', value: '8', icon: Checked, color: '#34C759' },
  { label: '模范学生', value: user.value.isModel ? 'Yes' : 'No', icon: Star, color: '#FF9F0A' },
  { label: '阅读笔记', value: '24', icon: Reading, color: '#AF52DE' }
])

const formatOnlineTime = (seconds) => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  if (h > 0) return `${h}h ${m}m`
  return `${m}m`
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const loadData = async () => {
  if (!user.value.id) return

  // 分开请求，避免一个失败导致全部失败
  try {
    const threadsRes = await axios.get(`http://localhost:8080/api/thread/list?userId=${user.value.id}`)
    if (threadsRes.data.code === 200) threadList.value = threadsRes.data.data
  } catch (e) {
    console.error('加载进展列表失败', e)
  }

  // 连续打卡：直接从日历记录计算，确保与日历同步
  streakCount.value = getStreakCount(loadCheckinRecords(user.value.id))

  try {
    const todayRes = await axios.get(`http://localhost:8080/api/thread/today?userId=${user.value.id}`)
    if (todayRes.data.code === 200) todayThread.value = todayRes.data.data
  } catch (e) {
    console.error('加载今日进展失败', e)
  }
}

const handleOpenUpdate = () => {
  if (todayThread.value) {
    updateForm.id = todayThread.value.id
    updateForm.title = todayThread.value.title
    updateForm.content = todayThread.value.content
  } else {
    updateForm.id = null
    updateForm.title = ''
    updateForm.content = ''
  }
  showUpdateDialog.value = true
}

const submitUpdate = async () => {
  if (!updateForm.title || !updateForm.content) return ElMessage.warning('请填写完整内容')
  submitting.value = true
  try {
    const isEdit = !!updateForm.id
    const url = isEdit ? 'http://localhost:8080/api/thread/update' : 'http://localhost:8080/api/thread/add'

    const res = await axios.post(url, {
      id: updateForm.id,
      userId: user.value.id,
      title: updateForm.title,
      content: updateForm.content,
      duration: Math.floor(onlineSeconds.value / 60)
    })

    if (res.data.code === 200) {
      ElMessage.success(isEdit ? '进展已修改' : '进展已发布')
      showUpdateDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
    // 今日在线学习时间：从 Layout 全局计时的本地持久化读取
    onlineSeconds.value = loadStudySeconds(user.value.id)
    loadData()
    syncYearCheckinCountToServer()

    studyHandler = (e) => {
      if (!e || !e.detail) return
      if (String(e.detail.userId) !== String(user.value.id)) return
      if (e.detail.date !== getTodayKey()) return
      if (typeof e.detail.seconds === 'number') {
        onlineSeconds.value = e.detail.seconds
      }
    }
    window.addEventListener('study-time-updated', studyHandler)
  }
})

onUnmounted(() => {
  if (studyHandler) window.removeEventListener('study-time-updated', studyHandler)
})
</script>

<style scoped lang="scss">
.dashboard-container { display: flex; flex-direction: column; gap: 32px; padding: 32px; }
.welcome-section {
  padding: 48px; display: flex; justify-content: space-between; align-items: center;
  h1 { font-size: 42px; font-weight: 700; margin: 0 0 12px 0; letter-spacing: -1px; }
  p { color: #86868b; margin: 0; font-size: 20px; .online-time { color: #0071e3; font-weight: 600; } }
}
.stat-pill {
  background: rgba(0, 113, 227, 0.1); padding: 16px 32px; border-radius: 999px; display: flex; flex-direction: column; align-items: center;
  .label { font-size: 14px; color: #0071e3; font-weight: 600; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
  .value { font-size: 28px; font-weight: 700; color: #0071e3; }
}
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 32px; }
.stat-card {
  padding: 32px; display: flex; align-items: center; gap: 24px;
  .stat-icon { width: 64px; height: 64px; border-radius: 20px; display: flex; align-items: center; justify-content: center; color: white; font-size: 28px; }
  .stat-info { display: flex; flex-direction: column; .stat-value { font-size: 28px; font-weight: 700; } .stat-label { font-size: 16px; color: #86868b; font-weight: 500; } }
}

.bottom-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 32px; }
.learning-thread {
  padding: 40px;
  .section-header {
    display: flex; justify-content: space-between; align-items: center; margin-bottom: 40px;
    h3 { margin: 0; font-size: 24px; font-weight: 700; }
    .update-btn {
      background-color: #0071e3; color: #fff; border: none; padding: 12px 24px; font-size: 16px; font-weight: 600; border-radius: 999px;
      transition: all 0.3s;
      &:hover { background-color: #0077ed; transform: scale(1.05); }
    }
  }
}

.thread-timeline {
  display: flex; flex-direction: column;
  .thread-item { display: flex; gap: 32px; }
  .thread-line {
    display: flex; flex-direction: column; align-items: center; width: 24px;
    .dot { width: 14px; height: 14px; border: 4px solid #0071e3; border-radius: 50%; background: #fff; z-index: 1; }
    .line { flex: 1; width: 2px; background: #e5e5ea; margin: 6px 0; }
  }
  .thread-content {
    padding-bottom: 40px; flex: 1;
    .thread-date { font-size: 16px; color: #86868b; margin-bottom: 8px; font-weight: 500; }
    .thread-title { font-size: 22px; font-weight: 700; color: #1d1d1f; margin-bottom: 12px; }
    .thread-desc { font-size: 17px; color: #424245; line-height: 1.7; }
  }
}
.empty-thread { text-align: center; color: #86868b; padding: 60px 0; font-size: 18px; }

.side-column { display: flex; flex-direction: column; gap: 32px; }
.quick-actions { padding: 40px; h3 { margin: 0 0 32px 0; font-size: 24px; font-weight: 700; } }
.action-buttons { display: flex; flex-direction: column; gap: 20px; .el-button { margin: 0; width: 100%; height: 56px; font-size: 18px; font-weight: 600; } }

.update-form {
  :deep(.el-form-item__label) { font-size: 18px; font-weight: 600; color: #1d1d1f; margin-bottom: 12px; }
  :deep(.el-input__inner), :deep(.el-textarea__inner) { font-size: 17px; }
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 16px; padding-top: 20px; }
</style>