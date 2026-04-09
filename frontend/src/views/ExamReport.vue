<template>
  <div class="report-page" v-loading="loading">
    <header class="report-header">
      <el-button :icon="ArrowLeft" circle class="back-btn" @click="router.push('/my-exams')" />
      <div class="rh-title">
        <h2>{{ examInfo.publishName || '成绩分析' }}</h2>
        <div class="rh-meta">
          <span>卷面：{{ examInfo.paperTitle || '-' }}</span>
          <span v-if="record.submitTime">交卷：{{ formatDateTime(record.submitTime) }}</span>
        </div>
      </div>
      <div class="rh-score">
        <div class="score-box">
          <div class="score-label">得分</div>
          <div class="score-value">{{ record.score ?? '-' }}</div>
        </div>
      </div>
    </header>

    <main class="report-body">
      <el-empty v-if="!loading && questions.length === 0" description="暂无题目数据" />

      <div v-else class="q-list">
        <div class="q-item" v-for="(q, idx) in questions" :key="q.id">
          <div class="q-head">
            <div class="q-no">{{ idx + 1 }}.</div>
            <el-tag size="small" :type="getQTypeTag(q.type)">{{ q.type }}</el-tag>
            <div class="q-score">({{ q.score || 10 }}分)</div>
          </div>
          <div class="q-content">{{ q.content }}</div>

          <div v-if="q.options && q.type === '选择题'" class="q-options">
            <div class="opt" v-for="(opt, oIdx) in parseOptions(q.options)" :key="oIdx">
              <span class="opt-key">{{ getOptionChar(oIdx) }}.</span>
              <span class="opt-val">{{ opt }}</span>
            </div>
          </div>

          <div class="q-answers">
            <div class="ans-row">
              <span class="ans-label">你的答案</span>
              <span class="ans-val" :class="{ wrong: isWrong(q) }">{{ getStudentAnswer(q) }}</span>
            </div>
            <div class="ans-row">
              <span class="ans-label">正确答案</span>
              <span class="ans-val correct">{{ q.standardAnswer ?? '-' }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const examInfo = ref({})
const record = ref({})
const questions = ref([])
const answersMap = ref({})

const parseOptions = (optStr) => {
  try { return JSON.parse(optStr) || [] } catch (e) { return [] }
}

const formatDateTime = (v) => {
  if (!v) return '-'
  const s = String(v).trim()
  // 兼容 2026-04-08T19:32:00 / 2026-04-08 19:32:00 / 带毫秒
  const normalized = s.replace('T', ' ').replace(/\.\d+$/, '')
  // 优先展示到分钟
  return normalized.length >= 16 ? normalized.substring(0, 16) : normalized
}
const getOptionChar = (index) => String.fromCharCode(65 + index)
const getQTypeTag = (type) => {
  if (type === '选择题') return 'primary'
  if (type === '填空题') return 'warning'
  return 'success'
}

const getStudentAnswer = (q) => {
  const v = answersMap.value?.[q.id]
  if (v === undefined || v === null || v === '') return '-'
  return String(v)
}

const normalize = (v) => (v ?? '').toString().trim()
const isWrong = (q) => {
  const a = normalize(getStudentAnswer(q))
  const s = normalize(q.standardAnswer)
  if (!a || a === '-') return true
  return a !== s
}

onMounted(async () => {
  const id = route.params.id
  if (!id) return router.push('/my-exams')
  loading.value = true
  try {
    const res = await request.get('/api/student/exam/report', { params: { assignmentId: id } })
    if (res.data.code !== 200) {
      ElMessage.error(res.data.msg || '获取成绩分析失败')
      return router.push('/my-exams')
    }
    const data = res.data.data || {}
    examInfo.value = data.examInfo || {}
    record.value = data.record || {}
    questions.value = data.questions || []
    const answersJson = record.value.answersJson
    if (answersJson) {
      try { answersMap.value = JSON.parse(answersJson) || {} } catch (e) { answersMap.value = {} }
    }
  } catch (e) {
    ElMessage.error('网络异常，获取成绩分析失败')
    router.push('/my-exams')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.report-page { min-height: 100vh; background: #f4f6fb; }
.report-header {
  position: sticky; top: 0; z-index: 100;
  display: flex; align-items: center; gap: 16px;
  padding: 18px 24px;
  background: rgba(255,255,255,0.7); backdrop-filter: blur(18px);
  border-bottom: 1px solid #e5e5ea;
}
.back-btn { border: none; background: #f5f5f7; }
.rh-title h2 { margin: 0; font-size: 18px; color: #1d1d1f; font-weight: 700; }
.rh-meta { margin-top: 4px; display: flex; gap: 14px; color: #86868b; font-size: 12px; }
.rh-score { margin-left: auto; }
.score-box { background: #e6f2ff; border: 1px solid #cfe6ff; border-radius: 12px; padding: 8px 14px; text-align: center; }
.score-label { font-size: 12px; color: #0071e3; font-weight: 700; }
.score-value { font-size: 20px; color: #0071e3; font-weight: 800; }
.report-body { max-width: 980px; margin: 0 auto; padding: 24px; }
.q-list { display: flex; flex-direction: column; gap: 16px; }
.q-item { background: #fff; border: 1px solid #e5e5ea; border-radius: 14px; padding: 18px; }
.q-head { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.q-no { font-weight: 800; color: #1d1d1f; }
.q-score { color: #86868b; font-size: 12px; }
.q-content { color: #1d1d1f; line-height: 1.7; margin-bottom: 10px; }
.q-options { background: #fbfcfe; border: 1px solid #eef0f5; border-radius: 10px; padding: 12px; display: flex; flex-direction: column; gap: 8px; margin-bottom: 12px; }
.opt-key { font-weight: 800; color: #0071e3; margin-right: 8px; }
.q-answers { display: flex; flex-direction: column; gap: 8px; }
.ans-row { display: flex; gap: 12px; }
.ans-label { width: 80px; color: #86868b; font-weight: 600; }
.ans-val { color: #1d1d1f; font-weight: 700; }
.ans-val.wrong { color: #ff3b30; }
.ans-val.correct { color: #34c759; }
</style>

