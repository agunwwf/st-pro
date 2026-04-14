<template>
  <div class="dashboard-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section glass-card">
      <div class="welcome-text">
        <h1>{{ greeting }}, {{ user.nickname || user.username }}</h1>
        <p v-if="!isTeacher">今天已在线学习 <strong class="online-time">{{ formatOnlineTime(onlineSeconds) }}</strong>，继续保持！</p>
        <p v-else>今日教学总览与待办提醒如下，建议优先处理临期任务。</p>
      </div>
      <div v-if="!isTeacher" class="welcome-stats">
        <div class="stat-pill">
          <span class="label">连续打卡</span>
          <span class="value">{{ streakCount }} 天</span>
        </div>
      </div>
    </div>

    <!-- 统计完成 -->
    <div class="stats-grid">
      <div
        class="stat-card glass-card"
        :class="{ clickable: !!stat.clickable }"
        v-for="stat in stats"
        :key="stat.label"
        @click="handleStatClick(stat)"
      >

        <el-popover
            v-if="stat.label === '已完成教学'"
            placement="bottom"
            :width="260"
            trigger="hover"
        >
          <template #reference>
            <div style="display:flex; gap:24px; align-items:center; cursor: pointer; width: 100%;">
              <div class="stat-icon" :style="{ background: stat.color }">
                <el-icon><component :is="stat.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <span class="stat-value">{{ stat.value }}</span>
                <span class="stat-label">{{ stat.label }}</span>
              </div>
            </div>
          </template>

          <div class="completion-details">
            <h4 style="margin: 0 0 12px 0; font-size: 15px; color: #1d1d1f; border-bottom: 1px solid #eee; padding-bottom: 8px;">
              已解锁模块详情
            </h4>
            <div style="display: flex; flex-wrap: wrap; gap: 8px;">
              <el-tag
                  v-for="item in completedItems"
                  :key="item.id"
                  size="default"
                  :type="item.kind === 'demo' ? 'success' : 'primary'"
                  effect="light"
              >
                {{ item.moduleId.toUpperCase() }} ({{ item.kind === 'demo' ? '演示' : '练习' }})
              </el-tag>

              <div v-if="completedItems.length === 0" style="color: #999; font-size: 13px; width: 100%; text-align: center; padding: 10px 0;">
                暂未完成任何教学哦~ 快去学习吧！
              </div>
            </div>
          </div>
        </el-popover>

        <div v-else style="display:flex; gap:24px; align-items:center; width: 100%;">
          <div class="stat-icon" :style="{ background: stat.color }">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stat.value }}</span>
            <span class="stat-label">{{ stat.label }}</span>
          </div>
        </div>

      </div>
    </div>
    <!-- 底部网格：学习线程 + 快速操作 -->
    <div class="bottom-grid" :class="{ 'teacher-bottom-grid': isTeacher }">
      <div class="learning-thread glass-card">
        <div class="section-header">
          <h3>{{ isTeacher ? '教学提醒与待办' : '学习线程' }}</h3>
        </div>

        <div v-if="isTeacher" class="teacher-todo-list">
          <div v-for="(todo, idx) in teacherTodos" :key="idx" class="teacher-todo-item">
            <div class="teacher-todo-title">{{ todo.title }}</div>
            <div class="teacher-todo-desc">{{ todo.desc }}</div>
          </div>
          <div v-if="teacherTodos.length === 0" class="empty-thread">
            当前暂无待办提醒，班级状态良好。
          </div>
        </div>

        <div v-else class="thread-timeline">
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
            暂无学习动态，完成测验或考试后会自动生成记录。
          </div>
        </div>
      </div>

      <div v-if="!isTeacher" class="side-column">
        <!--Tree-->
        <SkillTreeChart />
        <div class="quick-actions glass-card">
          <h3>快速操作</h3>
          <div class="action-buttons">
            <el-button type="primary" round @click="$router.push('/calendar')">去打卡</el-button>
            <el-button type="success" round @click="$router.push('/chat')">去聊天</el-button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Timer, Checked, Star, Reading } from '@element-plus/icons-vue'
import request from '@/utils/request'
window.axios = request
import SkillTreeChart from '@/components/SkillTreeChart.vue';
const router = useRouter()

const user = ref({})
const threadList = ref([])
const streakCount = ref(0)
const onlineSeconds = ref(0)
// 教学完成统计：来自后端 /api/learning/summary（Streamlit 里点「已完成」后写入 MySQL）
const learningCompletedTotal = ref(0)
const completedItems = ref([])
const learningCompletedMax = ref(10) // 5 个项目 × 演示+分步 各 1 = 10，与后端 MODULES×KINDS 一致
const teacherOverview = ref({
  classStudentCount: 0,
  weeklyActiveStudents: 0,
  pendingRequests: 0,
  activeAssignments: 0
})

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
    await axios.post('http://localhost:8080/api/user/checkin', { count })
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

const isTeacher = computed(() => String(user.value.role || '').toUpperCase() === 'ADMIN')

// 四个统计卡片；「已完成教学」的分子分母由下面 loadData 里请求 summary 更新
const stats = computed(() =>
  isTeacher.value
    ? [
      { label: '班级学生', value: String(teacherOverview.value.classStudentCount), icon: Star, color: '#0071E3', clickable: true, action: 'goModelStudents' },
      { label: '本周活跃', value: String(teacherOverview.value.weeklyActiveStudents), icon: Timer, color: '#34C759' },
      { label: '待处理申请', value: String(teacherOverview.value.pendingRequests), icon: Checked, color: '#FF9F0A', clickable: true, action: 'goRequests' },
      { label: '进行中考试', value: String(teacherOverview.value.activeAssignments), icon: Reading, color: '#AF52DE', clickable: true, action: 'goAssignments' }
    ]
    : [
      { label: '今日学习', value: formatOnlineTime(onlineSeconds.value), icon: Timer, color: '#0071E3' },
      {
        label: '已完成教学',
        value: `${learningCompletedTotal.value}/${learningCompletedMax.value}`,
        icon: Checked,
        color: '#34C759',
      },
      { label: '模范学生', value: user.value.isModel ? '是' : '否', icon: Star, color: '#FF9F0A' },
      { label: '阅读笔记', value: '24', icon: Reading, color: '#AF52DE' }
    ]
)

const handleStatClick = (stat) => {
  if (!stat?.clickable || !stat?.action) return
  if (stat.action === 'goModelStudents') {
    router.push({ path: '/management', query: { view: 'students' } })
  } else if (stat.action === 'goRequests') {
    router.push({ path: '/management', query: { view: 'students' } })
  } else if (stat.action === 'goAssignments') {
    router.push({ path: '/management', query: { view: 'exams', tab: 'assignments' } })
  }
}

const teacherTodos = computed(() => {
  if (!isTeacher.value) return []
  const todos = []
  if (teacherOverview.value.pendingRequests > 0) {
    todos.push({
      title: `有 ${teacherOverview.value.pendingRequests} 条入班/换导师申请待处理`,
      desc: '建议尽快在班级管理中完成审批，避免影响学生参加考试任务。'
    })
  }
  if (teacherOverview.value.activeAssignments > 0) {
    todos.push({
      title: `当前有 ${teacherOverview.value.activeAssignments} 场进行中考试任务`,
      desc: '请关注临近截止的考试，必要时提醒学生按时提交。'
    })
  }
  if (teacherOverview.value.weeklyActiveStudents < Math.max(1, Math.floor(teacherOverview.value.classStudentCount * 0.6))) {
    todos.push({
      title: '本周活跃学生占比偏低',
      desc: '可通过发布针对性练习卷或班级通知提升学习活跃度。'
    })
  }
  if (todos.length === 0) {
    todos.push({
      title: '当前教学节奏稳定',
      desc: '暂无紧急待办，可继续关注考试分析与个性化组卷。'
    })
  }
  return todos
})

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
  if (isTeacher.value) {
    try {
      const [studentsRes, requestsRes, teacherCalendarRes] = await Promise.all([
        request.get('/api/teacher/my-students'),
        request.get('/api/teacher/class/requests', { params: { status: 'PENDING' } }),
        request.get('/api/thread/calendar/teacher')
      ])

      const studentRows = studentsRes?.data?.code === 200 ? (studentsRes.data.data || []) : []
      const requestRows = requestsRes?.data?.code === 200 ? (requestsRes.data.data || []) : []
      const calendarData = teacherCalendarRes?.data?.code === 200 ? (teacherCalendarRes.data.data || {}) : {}
      const assignments = calendarData.assignments || []
      const classActivities = calendarData.classActivities || []
      const oneWeekAgo = Date.now() - 7 * 24 * 3600 * 1000
      const weeklyActiveSet = new Set(
        classActivities
          .filter(x => {
            const t = new Date(x.createTime || 0).getTime()
            return Number.isFinite(t) && t >= oneWeekAgo
          })
          .map(x => String(x.userId))
      )
      const activeAssignments = assignments.filter(x => {
        const end = new Date(x.endTime || 0).getTime()
        return Number.isFinite(end) && end >= Date.now()
      }).length

      teacherOverview.value = {
        classStudentCount: studentRows.length,
        weeklyActiveStudents: weeklyActiveSet.size,
        pendingRequests: requestRows.length,
        activeAssignments
      }
    } catch (e) {
      console.error('加载老师仪表盘失败', e)
    }
  } else {
    try {
      const threadsRes = await request.get('/api/thread/list')
      if (threadsRes.data.code === 200) threadList.value = threadsRes.data.data
    } catch (e) {
      console.error('加载进展列表失败', e)
    }
  }

  // 连续打卡：直接从日历记录计算，确保与日历同步
  streakCount.value = getStreakCount(loadCheckinRecords(user.value.id))

  // request 已带 baseURL 与 token 头；返回 { totalCount, maxCount, items[] }
  if (!isTeacher.value) {
    try {
      const learnRes = await request.get('/api/learning/summary')
      if (learnRes.data.code === 200 && learnRes.data.data) {
        learningCompletedTotal.value = learnRes.data.data.totalCount ?? 0
        learningCompletedMax.value = learnRes.data.data.maxCount ?? 10

        // 【关键修复】把后端传过来的明细存起来给气泡用！
        completedItems.value = learnRes.data.data.items || []
      }
    } catch (e) {
      console.error('加载教学完成统计失败', e)
    }
  }
}

// 用户从 iframe（Streamlit）切回本站 Tab 时再拉一次数据，无需手动整页刷新即可更新「已完成教学」
function onVisibilityRefresh() {
  if (document.visibilityState === 'visible' && user.value?.id) {
    loadData()
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
    document.addEventListener('visibilitychange', onVisibilityRefresh)

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
  document.removeEventListener('visibilitychange', onVisibilityRefresh)
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
.stat-card.clickable { cursor: pointer; transition: transform 0.2s ease, box-shadow 0.2s ease; }
.stat-card.clickable:hover { transform: translateY(-2px); box-shadow: 0 10px 24px rgba(0, 0, 0, 0.08); }

.bottom-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 32px; }
.teacher-bottom-grid { grid-template-columns: 1fr; }
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
.teacher-todo-list { display: flex; flex-direction: column; gap: 18px; }
.teacher-todo-item {
  border: 1px solid #e5e5ea;
  border-radius: 14px;
  padding: 18px 20px;
  background: #fafcff;
}
.teacher-todo-title { font-size: 20px; font-weight: 700; color: #1d1d1f; margin-bottom: 8px; }
.teacher-todo-desc { font-size: 16px; color: #5a5a60; line-height: 1.7; }

.side-column { display: flex; flex-direction: column; gap: 32px; }
.quick-actions { padding: 40px; h3 { margin: 0 0 32px 0; font-size: 24px; font-weight: 700; } }
.action-buttons { display: flex; flex-direction: column; gap: 20px; .el-button { margin: 0; width: 100%; height: 56px; font-size: 18px; font-weight: 600; } }

.update-form {
  :deep(.el-form-item__label) { font-size: 18px; font-weight: 600; color: #1d1d1f; margin-bottom: 12px; }
  :deep(.el-input__inner), :deep(.el-textarea__inner) { font-size: 17px; }
}
.dialog-footer { display: flex; justify-content: flex-end; gap: 16px; padding-top: 20px; }
</style>