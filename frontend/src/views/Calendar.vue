<template>
  <div class="calendar-page glass-card">
    <div class="calendar-header">
      <div>
        <h2>{{ isTeacher ? '教学日历（教师）' : '学习日历（学生）' }}</h2>
        <p class="summary" v-if="!isTeacher">
          本月学习质量日
          <strong>{{ qualityDays }}</strong>
          天，待加强
          <strong class="miss">{{ weakDays }}</strong>
          天，任务完成
          <strong>{{ doneExamCount }}</strong>
          个
        </p>
        <p class="summary" v-else>
          今日班级活跃学生
          <strong>{{ teacherTodayActive }}</strong>
          人，已发布考试
          <strong>{{ teacherActiveAssignments.length }}</strong>
          个
        </p>
      </div>
      <el-button v-if="!isTeacher" type="primary" @click="checkinToday">今日打卡</el-button>
    </div>

    <el-calendar v-model="currentDate">
      <template #date-cell="{ data }">
        <div class="date-cell" :class="cellClasses(data)" @click="selectDate(data.day)">
          <span class="day-text">{{ formatDay(data.day) }}</span>
          <div class="marks">
            <span v-if="hasCheckin(data.day)" class="dot success"></span>
            <span v-if="hasActivity(data.day)" class="dot active"></span>
            <span v-if="hasDeadline(data.day)" class="dot deadline"></span>
          </div>
        </div>
      </template>
    </el-calendar>

    <div class="legend">
      <span><span class="dot success"></span>打卡</span>
      <span><span class="dot active"></span>{{ isTeacher ? '班级学习动态' : '学习动态' }}</span>
      <span><span class="dot deadline"></span>{{ isTeacher ? '考试截止日' : '我的考试截止日' }}</span>
    </div>

    <div class="detail-panel">
      <div class="suggestion">
        {{ smartSuggestion }}
      </div>
      <h3>{{ selectedDay }} 动态</h3>
      <div v-if="dayItems.length === 0" class="empty">当天暂无记录</div>
      <div v-for="(item, idx) in dayItems" :key="idx" class="detail-item">
        <div class="title">{{ item.title }}</div>
        <div class="desc">{{ item.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

let currentUser = null
try {
  const rawUser = localStorage.getItem('user')
  currentUser = rawUser ? JSON.parse(rawUser) : null
} catch {
  currentUser = null
}

const isTeacher = computed(() => String(currentUser?.role || '').toUpperCase() === 'ADMIN')
const STORAGE_KEY = currentUser?.id ? `checkin-records:${currentUser.id}` : 'checkin-records:guest'
const currentDate = ref(new Date())
const selectedDay = ref(toLocalDateKey(new Date()))
const checkinMap = ref(loadRecords())
const studentActivities = ref([])
const studentExamTasks = ref([])
const teacherActivities = ref([])
const teacherAssignments = ref([])
const teacherTodayActive = ref(0)

function pad2(n) { return String(n).padStart(2, '0') }
function toLocalDateKey(d) { return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}` }
function formatDay(dayStr) { return dayStr.split('-').slice(1).join('-') }
function loadRecords() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch { return {} }
}
function persistRecords() { localStorage.setItem(STORAGE_KEY, JSON.stringify(checkinMap.value)) }
function selectDate(day) { selectedDay.value = day }
function dateKeyOf(v) { return String(v || '').slice(0, 10) }
function isNotExpired(endTime) {
  if (!endTime) return false
  return new Date(endTime).getTime() >= Date.now()
}
function isBetween(dateStr, start, end) {
  if (!dateStr) return false
  const x = new Date(dateStr)
  return x >= start && x <= end
}

async function loadCalendarData() {
  try {
    if (isTeacher.value) {
      const res = await request.get('/api/thread/calendar/teacher')
      if (res.data.code === 200 && res.data.data) {
        teacherActivities.value = res.data.data.classActivities || []
        teacherAssignments.value = res.data.data.assignments || []
        teacherTodayActive.value = Number(res.data.data.todayActiveStudents || 0)
      }
    } else {
      const res = await request.get('/api/thread/calendar/student')
      if (res.data.code === 200 && res.data.data) {
        studentActivities.value = res.data.data.activities || []
        studentExamTasks.value = res.data.data.examTasks || []
      }
    }
  } catch (e) {
    ElMessage.error('加载日历数据失败')
  }
}

function parseScore(text) {
  const m = String(text || '').match(/得分\s*(\d+)/)
  return m ? Number(m[1]) : null
}

const qualityByDay = computed(() => {
  const map = {}
  for (const item of studentActivities.value) {
    const k = dateKeyOf(item.createTime)
    if (!k) continue
    if (!map[k]) map[k] = { count: 0, scoreSum: 0, scoreCount: 0 }
    map[k].count += 1
    const s = parseScore(item.content)
    if (s != null) {
      map[k].scoreSum += s
      map[k].scoreCount += 1
    }
  }
  return map
})

const qualityDays = computed(() => Object.values(qualityByDay.value).filter(x => x.count >= 1).length)
const weakDays = computed(() => Object.values(qualityByDay.value).filter(x => x.scoreCount > 0 && (x.scoreSum / x.scoreCount) < 70).length)
const doneExamCount = computed(() => studentExamTasks.value.filter(x => Number(x.status) >= 1).length)
const teacherActiveAssignments = computed(() =>
  (teacherAssignments.value || []).filter(x => isNotExpired(x.endTime))
)

function hasCheckin(day) { return checkinMap.value[day] === 'success' }
function hasActivity(day) {
  if (isTeacher.value) return teacherActivities.value.some(x => dateKeyOf(x.createTime) === day)
  return studentActivities.value.some(x => dateKeyOf(x.createTime) === day)
}
function hasDeadline(day) {
  const list = isTeacher.value ? teacherActiveAssignments.value : studentExamTasks.value
  return list.some(x => dateKeyOf(x.endTime) === day)
}

function cellClasses(data) {
  const day = data.day
  const today = toLocalDateKey(new Date())
  const quality = qualityByDay.value[day]
  return {
    'is-today': day === today,
    'is-quality': !isTeacher.value && !!quality && quality.count > 0,
    'is-weak': !isTeacher.value && !!quality && quality.scoreCount > 0 && (quality.scoreSum / quality.scoreCount) < 70
  }
}

const dayItems = computed(() => {
  const day = selectedDay.value
  if (isTeacher.value) {
    const list = teacherActiveAssignments.value || []
    const assignedRows = list
      .filter(x => dateKeyOf(x.startTime) === day)
      .map(x => ({
        title: `已布置考试：${x.publishName || x.paperTitle || '未命名考试'}`,
        desc: `布置时间 ${formatDayWithTime(x.startTime)}，截止 ${formatDayWithTime(x.endTime)}，当前已提交 ${Number(x.submittedCount || 0)} 人`
      }))
    const deadlineRows = list
      .filter(x => dateKeyOf(x.endTime) === day)
      .map(x => ({
        title: `今日截止：${x.publishName || x.paperTitle || '未命名考试'}`,
        desc: `截止时间 ${formatDayWithTime(x.endTime)}，当前已提交 ${Number(x.submittedCount || 0)} 人`
      }))
    const activitySummary = teacherActivities.value.filter(x => dateKeyOf(x.createTime) === day)
    const activityRow = activitySummary.length > 0
      ? [{
        title: `班级学习动态（${activitySummary.length} 条）`,
        desc: '当日班级产生学习行为，可结合考试进度做督促'
      }]
      : []
    return [...assignedRows, ...deadlineRows, ...activityRow]
  }
  const activityRows = studentActivities.value
    .filter(x => dateKeyOf(x.createTime) === day)
    .map(x => ({ title: x.title, desc: x.content || '学习活动' }))
  const deadlineRows = studentExamTasks.value
    .filter(x => dateKeyOf(x.endTime) === day)
    .map(x => ({ title: `考试截止：${x.publishName || x.paperTitle || '未命名试卷'}`, desc: Number(x.status) >= 1 ? '已完成' : '待完成' }))
  return [...deadlineRows, ...activityRows]
})

function formatDayWithTime(v) {
  if (!v) return '-'
  const s = String(v).replace('T', ' ').replace(/\.\d+$/, '')
  return s.length >= 16 ? s.substring(0, 16) : s
}

const smartSuggestion = computed(() => {
  if (isTeacher.value) {
    const upcoming = teacherActiveAssignments.value.filter(x => {
      const d = dateKeyOf(x.endTime)
      return d >= toLocalDateKey(new Date())
    }).length
    if (teacherTodayActive.value === 0) return '今日班级学习活跃度较低，建议发布一个小测提醒并在群里点名跟进。'
    if (upcoming > 0) return `未来待截止考试 ${upcoming} 个，建议重点关注临近截止班级的提交率。`
    return '班级学习节奏稳定，可根据学习动态安排下一轮针对性训练。'
  }
  const weak = weakDays.value
  const pending = studentExamTasks.value.filter(x => Number(x.status || 0) < 1).length
  if (pending > 0) return `你还有 ${pending} 个考试任务待完成，建议先处理截止时间最近的任务。`
  if (weak > 0) return `本月有 ${weak} 天分数偏低，建议优先复习错题最多的模块再做强化练习。`
  return '学习状态很好，建议继续保持并挑战更高难度练习题。'
})

async function checkinToday() {
  const today = toLocalDateKey(new Date())
  if (checkinMap.value[today] === 'success') {
    ElMessage.info('今天已打卡')
    return
  }
  checkinMap.value[today] = 'success'
  persistRecords()
  try {
    const count = Object.entries(checkinMap.value).filter(([k, v]) => v === 'success' && k.startsWith(String(new Date().getFullYear()))).length
    await request.post('/api/user/checkin', { count })
  } catch {
    // ignore
  }
  ElMessage.success('打卡成功')
}

onMounted(() => {
  loadCalendarData()
})
</script>

<style scoped>
.calendar-page {
  padding: 22px;
  border-radius: 16px;
  border: 1px solid rgba(64, 158, 255, 0.12);
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
}
.calendar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.calendar-header h2 { margin: 0; font-size: 24px; color: #1f2d3d; letter-spacing: 0.3px; }
.summary { margin: 6px 0 0 0; font-size: 14px; color: #5d6b7a; }
.summary .miss { color: #ff3b30; }
.date-cell {
  height: 100%;
  padding: 6px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.date-cell:hover {
  background: rgba(64, 158, 255, 0.08);
  transform: translateY(-1px);
}
.date-cell.is-today {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.18), rgba(64, 158, 255, 0.08));
  box-shadow: inset 0 0 0 1px rgba(64, 158, 255, 0.35);
}
.date-cell.is-quality { box-shadow: inset 0 0 0 1px rgba(103, 194, 58, 0.25); }
.date-cell.is-weak { box-shadow: inset 0 0 0 1px rgba(245, 108, 108, 0.35); }
.day-text { font-size: 13px; color: #2c3e50; font-weight: 500; }
.marks { margin-top: 6px; display: flex; gap: 4px; }
.legend {
  margin-top: 14px;
  display: flex;
  gap: 18px;
  font-size: 13px;
  color: #5d6b7a;
  padding: 8px 10px;
  background: #f7faff;
  border-radius: 10px;
}
.dot { display: inline-block; width: 9px; height: 9px; border-radius: 50%; margin-right: 4px; }
.dot.success { background: #67c23a; }
.dot.active { background: #409eff; }
.dot.deadline { background: #e6a23c; }
.detail-panel { margin-top: 16px; border-top: 1px solid #edf2f8; padding-top: 14px; }
.suggestion {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eef6ff 0%, #f7fbff 100%);
  color: #315a85;
  font-size: 13px;
  border: 1px solid rgba(64, 158, 255, 0.18);
}
.detail-panel h3 { margin: 0 0 10px 0; font-size: 16px; color: #1f2d3d; }
.detail-item {
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #edf2f8;
  border-radius: 10px;
  margin-bottom: 8px;
}
.detail-item .title { font-weight: 600; margin-bottom: 3px; color: #1f2d3d; }
.detail-item .desc { color: #5d6b7a; font-size: 13px; }
.empty { color: #9aa4af; padding: 8px 0; }
</style>