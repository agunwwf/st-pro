<template>
  <div class="review-page" v-loading="loading">
    <header class="top-bar">
      <el-button :icon="ArrowLeft" circle @click="goBack" />
      <div class="title-wrap">
        <h2>{{ assignmentTitle }}</h2>
        <p>{{ studentName }} · 当前分数 {{ currentScore }}</p>
      </div>
      <el-button type="primary" @click="submitRescore">更新分数</el-button>
    </header>

    <main class="content">
      <el-empty v-if="!loading && questions.length === 0" description="暂无答卷数据" />
      <div v-else class="q-list">
        <div class="q-item" v-for="q in questions" :key="q.id">
          <div class="q-head">
            <span class="idx">{{ q.index }}.</span>
            <el-tag size="small">{{ q.type }}</el-tag>
            <span class="full">满分 {{ q.fullScore }}</span>
          </div>
          <div class="q-content">{{ q.content }}</div>
          <div v-if="q.options && q.type === '选择题'" class="q-options">
            <div v-for="(opt, i) in parseOptions(q.options)" :key="i">{{ String.fromCharCode(65 + i) }}. {{ opt }}</div>
          </div>
          <div class="ans-line">
            <span class="label">学生答案：</span><span>{{ q.studentAnswer || '-' }}</span>
          </div>
          <div class="ans-line">
            <span class="label">参考答案：</span><span class="std">{{ q.standardAnswer || '-' }}</span>
          </div>
          <div class="score-line">
            <template v-if="canManualScore(q)">
              <el-input-number
                v-model="questionScores[q.id]"
                :min="0"
                :max="Number(q.fullScore || 0)"
                controls-position="right"
                size="small"
              />
              <span class="score-tip">本题给分（0 ~ {{ q.fullScore }}）</span>
            </template>
            <template v-else>
              <el-tag type="info" effect="plain">选择题自动判分，不支持手改</el-tag>
            </template>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const payload = ref(null)
const questions = ref([])
const questionScores = ref({})

const assignmentTitle = computed(() => payload.value?.assignment?.publishName || '试卷批改')
const studentName = computed(() => payload.value?.student?.nickname || payload.value?.student?.username || '-')
const currentScore = computed(() => {
  const m = questionScores.value || {}
  return Object.values(m).reduce((s, x) => s + Number(x || 0), 0)
})

const parseOptions = (v) => {
  if (!v) return []
  try { return JSON.parse(v) || [] } catch { return [] }
}
const canManualScore = (q) => q?.type === '填空题' || q?.type === '编程题'

const load = async () => {
  loading.value = true
  try {
    const { id, studentId } = route.params
    const res = await request.get(`/api/teacher/lms/assignment/${id}/submission/${studentId}`)
    if (res.data?.code !== 200) {
      ElMessage.error(res.data?.msg || '加载失败')
      return
    }
    payload.value = res.data.data || {}
    questions.value = payload.value.questions || []
    const init = {}
    for (const q of questions.value) init[q.id] = Number(q.suggestScore || 0)
    questionScores.value = init
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const submitRescore = async () => {
  const { id, studentId } = route.params
  try {
    const res = await request.post(`/api/teacher/lms/assignment/${id}/submission/${studentId}/rescore`, {
      questionScores: questionScores.value
    })
    if (res.data?.code === 200) {
      ElMessage.success(`已更新分数：${res.data.data?.score ?? currentScore.value}`)
      router.push(`/teacher/exam-analytics/${id}`)
    } else {
      ElMessage.error(res.data?.msg || '更新失败')
    }
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

const goBack = () => router.push(`/teacher/exam-analytics/${route.params.id}`)

load()
</script>

<style scoped>
.review-page { min-height: 100vh; background: #f4f6fb; padding: 20px; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.title-wrap h2 { margin: 0; font-size: 20px; }
.title-wrap p { margin: 2px 0 0; font-size: 13px; color: #8e8e93; }
.content { max-width: 980px; margin: 0 auto; }
.q-list { display: flex; flex-direction: column; gap: 14px; }
.q-item { background: #fff; border: 1px solid #e5e5ea; border-radius: 12px; padding: 14px; }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.idx { font-weight: 700; }
.full { margin-left: auto; color: #8e8e93; font-size: 12px; }
.q-content { margin-bottom: 8px; line-height: 1.6; }
.q-options { background: #fafafa; border-radius: 8px; padding: 10px; margin-bottom: 8px; font-size: 13px; }
.ans-line { font-size: 13px; margin-top: 4px; }
.label { color: #8e8e93; }
.std { color: #34c759; font-weight: 600; }
.score-line { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.score-tip { font-size: 12px; color: #8e8e93; }
</style>
