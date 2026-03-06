<template>
  <div class="calendar-page glass-card">
    <div class="calendar-header">
      <h2>本月打卡日历</h2>
      <p class="summary">
        本月已打卡
        <strong>{{ stats.success }}</strong>
        天，缺卡
        <strong class="miss">{{ stats.miss }}</strong>
        天
      </p>
    </div>

    <el-calendar v-model="currentDate">
      <template #date-cell="{ data }">
        <div
          class="date-cell"
          :class="cellClasses(data)"
          @click="handleClickDate(data)"
        >
          <span class="day-text">{{ formatDay(data.day) }}</span>
          <span v-if="statusMap[getKey(data.day)] === 'success'" class="mark success">✔</span>
          <span v-else-if="statusMap[getKey(data.day)] === 'fail'" class="mark fail">✘</span>
        </div>
      </template>
    </el-calendar>

    <div class="legend">
      <span><span class="dot success"></span>已打卡</span>
      <span><span class="dot fail"></span>缺卡</span>
      <span><span class="dot future"></span>未来日期</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

// 当前登录用户
let currentUser = null
try {
  const rawUser = localStorage.getItem('user')
  currentUser = rawUser ? JSON.parse(rawUser) : null
} catch {
  currentUser = null
}

// 每个用户单独存一份打卡记录，避免不同账号互相覆盖
const STORAGE_KEY = currentUser && currentUser.id ? `checkin-records:${currentUser.id}` : 'checkin-records:guest'

const currentDate = ref(new Date())
const statusMap = ref(loadRecords())

function pad2(n) {
  return String(n).padStart(2, '0')
}

function toLocalDateKey(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function getTodayKey() {
  return toLocalDateKey(new Date())
}

function getKey(dayStr) {
  return dayStr
}

function formatDay(dayStr) {
  return dayStr.split('-').slice(1).join('-')
}

function compareDay(a, b) {
  return a === b ? 0 : a < b ? -1 : 1
}

function loadRecords() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function persistRecords() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(statusMap.value))
}

function autoFillMissForPastDays() {
  const todayKey = getTodayKey()
  const date = new Date(currentDate.value)
  const year = date.getFullYear()
  const month = date.getMonth()
  const first = new Date(year, month, 1)
  const nextMonth = new Date(year, month + 1, 1)

  for (let d = new Date(first); d < nextMonth; d.setDate(d.getDate() + 1)) {
    const key = toLocalDateKey(d)
    if (compareDay(key, todayKey) < 0 && !statusMap.value[key]) {
      statusMap.value[key] = 'fail'
    }
  }
  persistRecords()
}

autoFillMissForPastDays()

const stats = computed(() => {
  const date = new Date(currentDate.value)
  const year = date.getFullYear()
  const month = date.getMonth()
  const first = new Date(year, month, 1)
  const nextMonth = new Date(year, month + 1, 1)
  let success = 0
  let miss = 0

  for (let d = new Date(first); d < nextMonth; d.setDate(d.getDate() + 1)) {
    const key = toLocalDateKey(d)
    if (statusMap.value[key] === 'success') success++
    if (statusMap.value[key] === 'fail') miss++
  }
  return { success, miss }
})

// 计算本年度已打卡天数
function getYearSuccessCount() {
  const now = new Date()
  const year = now.getFullYear()
  let count = 0
  for (const [key, value] of Object.entries(statusMap.value)) {
    if (value === 'success' && key.startsWith(String(year))) {
      count++
    }
  }
  return count
}

// 首次进入页面时，同步一次已有的年度打卡天数到后台
async function syncCheckinToServer() {
  if (!(currentUser && currentUser.id)) return
  const count = getYearSuccessCount()
  try {
    await axios.post('http://localhost:8080/api/user/checkin', {
      id: currentUser.id,
      count
    })
  } catch (e) {
    console.error('同步打卡次数失败', e)
  }
}

// 进入日历页面就做一次同步（把之前已经打过的卡汇总给后台）
syncCheckinToServer()

watch(currentDate, () => {
  autoFillMissForPastDays()
})

function cellClasses(data) {
  const key = getKey(data.day)
  const today = getTodayKey()
  const cmp = compareDay(key, today)

  return {
    'is-today': cmp === 0,
    'is-future': cmp > 0,
    'is-past': cmp < 0,
  }
}

async function handleClickDate(data) {
  const key = getKey(data.day)
  const today = getTodayKey()
  const cmp = compareDay(key, today)
  const currentStatus = statusMap.value[key]

  if (cmp > 0) {
    ElMessage.info('未来的日期不能打卡')
    return
  }

  if (cmp < 0) {
    ElMessage.info('历史记录不能修改')
    return
  }

  if (currentStatus === 'success') {
    ElMessage.info('今天已经打过卡了')
    return
  }

  if (currentStatus === 'fail') {
    ElMessage.info('今天已被标记为缺卡，不能修改')
    return
  }

  // 本地标记成功
  statusMap.value[key] = 'success'
  persistRecords()

  // 如果已登录，则通知后端累计打卡次数
  if (currentUser && currentUser.id) {
    try {
      const count = getYearSuccessCount()
      await axios.post('http://localhost:8080/api/user/checkin', { id: currentUser.id, count })
    } catch (e) {
      console.error('上报打卡次数失败', e)
    }
  }

  ElMessage.success('打卡成功')
}
</script>

<style scoped>
.calendar-page {
  padding: 20px;
}

.calendar-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.calendar-header h2 {
  margin: 0;
  font-size: 20px;
}

.summary {
  margin: 0;
  font-size: 14px;
}

.summary .miss {
  color: #ff3b30;
}

.date-cell {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px;
  cursor: pointer;
}

.date-cell.is-today {
  border-radius: 6px;
  background-color: rgba(0, 122, 255, 0.1);
}

.date-cell.is-future .day-text {
  color: #c0c4cc;
}

.day-text {
  font-size: 13px;
}

.mark {
  font-size: 14px;
}

.mark.success {
  color: #34c759;
}

.mark.fail {
  color: #ff3b30;
}

.legend {
  margin-top: 12px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 4px;
}

.dot.success {
  background-color: #34c759;
}

.dot.fail {
  background-color: #ff3b30;
}

.dot.future {
  background-color: #c0c4cc;
}
</style>