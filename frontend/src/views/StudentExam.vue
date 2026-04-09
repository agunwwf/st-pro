<template>
  <div class="exam-room" v-loading="loading">
    <header class="exam-header glass-card">
      <div class="eh-left">
        <el-tooltip content="返回学习大厅" placement="bottom">
          <el-button
              :icon="ArrowLeft"
              circle
              class="back-home-btn"
              @click="handleBackHome"
          />
        </el-tooltip>
        <div class="title-group">
          <h2>{{ examData.publishName || '正在获取考试信息...' }}</h2>
          <span class="paper-title">引用卷面：{{ examData.paperTitle || '加载中' }}</span>
        </div>
      </div>

      <div class="eh-center">
        <div class="countdown-box" :class="{ 'danger': timeLeft > 0 && timeLeft < 300 }">
          <el-icon class="timer-icon"><Timer /></el-icon>
          <span class="time-text">{{ formatTime(timeLeft) }}</span>
        </div>
      </div>

      <div class="eh-right">
        <span class="progress-text">已答: <strong>{{ answeredCount }}</strong> / {{ questions.length }}</span>
        <el-button type="success" size="large" class="submit-btn" @click="confirmSubmit">交卷</el-button>
      </div>
    </header>

    <main class="exam-content">
      <div class="paper-container glass-card">
        <div class="paper-header">
          <p class="warning-text">
            <el-icon><Warning /></el-icon>
            考试须知：请在规定时间内独立完成测验。
          </p>
        </div>

        <el-divider />

        <div v-if="questions.length > 0">
          <div v-for="(q, index) in questions" :key="q.id" class="question-block" :id="'q-' + index">
            <div class="q-title">
              <span class="q-num">{{ index + 1 }}.</span>
              <el-tag size="small" :type="getQTypeTag(q.type)" class="q-type-tag">{{ q.type }}</el-tag>
              <span class="q-content-text">{{ q.content }}</span>
              <span class="q-score">({{ q.score || 10 }}分)</span>
            </div>

            <div v-if="q.type === '选择题'" class="q-options-area">
              <el-radio-group v-model="answers[q.id]" class="custom-radio-group">
                <el-radio
                    v-for="(opt, oIdx) in parseOptions(q.options)"
                    :key="oIdx"
                    :label="getOptionChar(oIdx)"
                    class="opt-radio"
                >
                  {{ opt }}
                </el-radio>
              </el-radio-group>
            </div>

            <div v-if="q.type === '填空题'" class="q-blank-area">
              <el-input
                  v-model="answers[q.id]"
                  placeholder="请输入您的答案..."
                  size="large"
                  clearable
                  class="blank-input"
              />
            </div>

            <div v-if="q.type === '编程题'" class="q-coding-area">
              <el-input
                  v-model="answers[q.id]"
                  type="textarea"
                  :rows="6"
                  placeholder="# 请在此处编写您的代码..."
                  class="code-input"
              />
            </div>
          </div>
        </div>

        <div v-else style="padding: 60px 0; text-align: center; color: #86868b;">
          正在奋力加载试卷内容...
        </div>

        <div class="paper-footer" v-if="questions.length > 0">
          <el-button type="primary" size="large" @click="confirmSubmit" style="width: 240px; font-weight: bold;">
            完成并提交试卷
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Timer, Warning, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
window.axios = request

const router = useRouter()
const route = useRoute()
const loading = ref(false)

const examData = ref({})
const questions = ref([])
const answers = ref({})
const timeLeft = ref(0)
let timer = null

// --- 工具函数：获取当前登录学生的 ID ---
const getStudentId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id
}

// --- 初始化与获取真实考卷数据 ---
onMounted(async () => {
  const assignmentId = route.params.id
  if (!assignmentId) {
    ElMessage.error('非法的测验链接')
    return router.push('/dashboard')
  }

  loading.value = true
  try {
   
    const res = await request.get(`/api/student/exam/detail?assignmentId=${assignmentId}`)
    if (res.data.code === 200) {
      examData.value = res.data.data.examInfo || {}
      questions.value = res.data.data.questions || []

      // 根据后端返回的分钟数，初始化倒计时
      timeLeft.value = (examData.value.timeLimitMinutes || 45) * 60
      startTimer()

      // 开启切屏防作弊监听
      window.addEventListener('blur', handleBlur)
    } else {
      ElMessage.error(res.data.msg || '获取试卷失败')
      router.push('/dashboard')
    }
  } catch (e) {
    ElMessage.error('服务器走神了，获取试卷失败')
    console.error(e)
  } finally {
    loading.value = false
  }
})

// --- 退出与返回主页逻辑 ---
const handleBackHome = () => {
  if (answeredCount.value > 0) {
    ElMessageBox.confirm(
        '考试正在进行中，现在离开将不会保存当前进度，确定要退出吗？',
        '退出确认',
        {
          confirmButtonText: '确定退出',
          cancelButtonText: '继续考试',
          type: 'warning',
          confirmButtonClass: 'el-button--danger',
          roundButton: true
        }
    ).then(() => {
      router.push('/dashboard')
    }).catch(() => {})
  } else {
    // 还没动笔，直接回去
    router.push('/dashboard')
  }
}

// --- 倒计时与防作弊 ---
const startTimer = () => {
  timer = setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      clearInterval(timer)
      forceSubmit('时间到！系统正在为您自动交卷...')
    }
  }, 1000)
}

const handleBlur = () => {
  ElMessage.warning({ message: '警告：检测到您离开了考试页面！', duration: 3000 })
}

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('blur', handleBlur)
})

// --- 基础数据处理工具 ---
const formatTime = (seconds) => {
  if (seconds <= 0) return '00:00'
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

const parseOptions = (optStr) => {
  try { return JSON.parse(optStr) || [] } catch(e) { return [] }
}

const getOptionChar = (index) => String.fromCharCode(65 + index)

const getQTypeTag = (type) => {
  if (type === '选择题') return 'primary'
  if (type === '填空题') return 'warning'
  return 'success'
}

// 动态计算已作答的数量
const answeredCount = computed(() => {
  return Object.values(answers.value).filter(val => val !== undefined && val !== '').length
})

// --- 交卷逻辑 ---
const confirmSubmit = () => {
  const unAnswered = questions.value.length - answeredCount.value
  const msg = unAnswered > 0
      ? `您还有 ${unAnswered} 道题未作答，确认要交卷吗？`
      : `确认要交卷吗？交卷后将不可修改。`

  ElMessageBox.confirm(msg, '交卷提示', {
    type: 'warning',
    confirmButtonText: '确认交卷',
    cancelButtonText: '再检查一下',
    roundButton: true
  }).then(() => {
    forceSubmit('交卷成功！')
  }).catch(() => {})
}


const forceSubmit = async (msg) => {
  loading.value = true
  const assignmentId = route.params.id

  try {
    const res = await request.post('/api/student/exam/submit', {
      assignmentId: assignmentId,
      answers: JSON.stringify(answers.value) // 将答案对象序列化后存入数据库
    })

    if (res.data.code === 200) {
      ElMessage.success(msg)
      clearInterval(timer) // 停止倒计时
      setTimeout(() => {
        router.push('/my-exams') // 交卷后回到学生的测验大厅
      }, 1500)
    } else {
      ElMessage.error(res.data.msg || '交卷遇到未知错误')
    }
  } catch (e) {
    ElMessage.error('网络异常，交卷失败，请重试')
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.exam-room { min-height: 100vh; background-color: #f4f6fb; display: flex; flex-direction: column; overflow-x: hidden; }
.glass-card { background: #ffffff; border-bottom: 1px solid #e5e5ea; box-shadow: 0 4px 12px rgba(0,0,0,0.03); }

/* 顶部状态栏 */
.exam-header {
  position: sticky; top: 0; z-index: 100; display: flex; justify-content: space-between;
  align-items: center; padding: 0 40px; height: 72px;
}

/* 标题与返回按钮组合 */
.eh-left { display: flex; align-items: center; gap: 20px; }
.back-home-btn {
  border: none; background: #f5f5f7; color: #1d1d1f; font-size: 20px;
  width: 44px; height: 44px; transition: all 0.3s;
}
.back-home-btn:hover { background: #e8e8ed; transform: scale(1.1); color: #0071e3; }
.title-group { display: flex; flex-direction: column; }
.title-group h2 { margin: 0; font-size: 18px; color: #1d1d1f; font-weight: 700; }
.paper-title { font-size: 12px; color: #86868b; margin-top: 4px; }

/* 中间倒计时 */
.eh-center { position: absolute; left: 50%; transform: translateX(-50%); }
.countdown-box {
  display: flex; align-items: center; gap: 8px; background: #e6f2ff; padding: 6px 20px;
  border-radius: 980px; color: #0071e3; font-weight: bold; font-size: 18px;
  border: 2px solid #0071e3; transition: all 0.3s;
}
.countdown-box.danger { background: #fff4e5; color: #ff3b30; border-color: #ff3b30; animation: pulse 1s infinite; }
@keyframes pulse { 0% { opacity: 1; box-shadow: 0 0 0 0 rgba(255, 59, 48, 0.4); } 70% { box-shadow: 0 0 0 10px rgba(255, 59, 48, 0); } 100% { opacity: 1; box-shadow: 0 0 0 0 rgba(255, 59, 48, 0); } }

/* 右侧提交区 */
.eh-right { display: flex; align-items: center; gap: 20px; }
.progress-text { color: #86868b; font-size: 14px; }
.progress-text strong { color: #0071e3; font-size: 16px; }
.submit-btn { border-radius: 980px; padding: 0 32px; font-weight: bold; }

/* 强制覆盖 Element 按钮白字 */
:deep(.el-button--primary), :deep(.el-button--success) { color: #ffffff !important; font-weight: 600; }

/* 考卷主体 */
.exam-content { flex: 1; padding: 40px; display: flex; justify-content: center; }
.paper-container { width: 100%; max-width: 900px; border-radius: 16px; padding: 40px 60px; margin-bottom: 40px;}
.warning-text { color: #ff9500; font-size: 14px; background: #fffaf0; padding: 12px 16px; border-radius: 8px; border: 1px solid #ffe8cc; display: flex; align-items: center; gap: 8px; }

/* 题目样式 */
.question-block { margin-bottom: 48px; }
.q-title { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 20px; line-height: 1.6; }
.q-num { font-size: 18px; font-weight: bold; color: #1d1d1f; }
.q-type-tag { margin-top: 2px; }
.q-content-text { font-size: 16px; color: #1d1d1f; flex: 1; }
.q-score { color: #86868b; font-size: 14px; white-space: nowrap; }

/* 选项与输入区美化 */
.q-options-area { padding-left: 30px; }
.custom-radio-group { display: flex; flex-direction: column; gap: 16px; width: 100%; }
.opt-radio { margin: 0; padding: 16px 20px; border: 1px solid #e5e5ea; border-radius: 8px; transition: all 0.2s; background: #fbfcfe; width: 100%; height: auto; white-space: normal; }
.opt-radio.is-checked { background: #e6f2ff; border-color: #0071e3; }

.q-blank-area { padding-left: 30px; max-width: 400px; }
:deep(.blank-input .el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #d2d2d7 inset; }
:deep(.blank-input .el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #0071e3 inset !important; }

.q-coding-area { padding-left: 30px; }
:deep(.code-input .el-textarea__inner) { background: #282c34; color: #abb2bf; font-family: 'Courier New', Courier, monospace; font-size: 15px; border-radius: 8px; padding: 16px; line-height: 1.5; box-shadow: none; border: 1px solid #1e2227; }
:deep(.code-input .el-textarea__inner:focus) { border-color: #0071e3; outline: none; }

.paper-footer { text-align: center; margin-top: 60px; }
</style>