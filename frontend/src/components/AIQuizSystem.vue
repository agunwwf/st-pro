<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { FileSignature, CodeXml, Target, ListChecks, History, Award, CheckCircle2, XCircle, Sparkles, TrendingUp, Cpu } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const currentStatus = ref('idle') // idle | loading | testing | grading | review
const currentPaper = ref(null)
const currentRecordId = ref(null)
const userAnswers = ref({ mcq: {}, fill: {}, coding: '' })
const currentScore = ref(0)
const isCorrectMap = ref({ mcq: {}, fill: {}, coding: false })
const historyPapers = ref([])
const loadingText = ref('导师正在思考中...')
const historyVisible = ref(false)
let pollTimer = null

const availableModules = [
  { id: 'kmeans', name: 'K-Means 聚类', desc: '聚类原理、质心更新、肘部法则', icon: Target },
  { id: 'linear', name: '线性回归', desc: '最小二乘法、梯度下降、损失函数', icon: TrendingUp },
  { id: 'neural', name: '神经网络', desc: '前向传播、反向传播、激活函数', icon: Cpu }
]

const loadHistory = async () => {
  try {
    const res = await request.get('/api/ai/quiz/list')
    if (res.data.code === 200 && res.data.data) historyPapers.value = res.data.data
  } catch (e) {
    console.error('加载历史记录失败', e)
  }
}

onMounted(async () => {
  await loadHistory()
  pollTimer = window.setInterval(loadHistory, 2500)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})

const createAnswerDraft = (paper) => {
  const draft = { mcq: {}, fill: {}, coding: paper?.coding?.template || '' }
  ;(paper?.mcq || []).forEach(q => { draft.mcq[q.id] = null })
  ;(paper?.fill_in_the_blank || []).forEach(q => { draft.fill[q.id] = '' })
  return draft
}

const generateNewPaper = async (moduleId, moduleName) => {
  currentStatus.value = 'loading'
  loadingText.value = `导师正在翻阅题库，为你定制【${moduleName}】专项试卷...`
  try {
    const res = await request.post('/api/ai/quiz/generate', { moduleId, moduleName })
    if (res.data.code !== 200 || !res.data.data?.id) throw new Error(res.data.msg || '创建失败')
    historyVisible.value = true
    await loadHistory()
    currentStatus.value = 'idle'
  } catch (e) {
    console.error(e)
    ElMessage.error('出题失败，请稍后重试')
    currentStatus.value = 'idle'
  }
}

const openPaper = async (paper) => {
  if (!paper?.id) return
  if (paper.status === 9) return alert('该试卷仍在生成中，请稍后再打开。')
  if (paper.status === -1) return alert(paper.weaknessAnalysis || '该试卷生成失败，请删除后重新生成。')
  try {
    const res = await request.get(`/api/ai/quiz/${paper.id}`)
    if (res.data.code !== 200 || !res.data.data) throw new Error(res.data.msg || '读取失败')
    const detail = res.data.data
    const parsed = JSON.parse(detail.quizJson || '{}')
    currentRecordId.value = detail.id
    currentPaper.value = parsed

    if (Number(detail.status) === 1) {
      userAnswers.value = JSON.parse(detail.userAnswers || '{"mcq":{},"fill":{},"coding":""}')
      currentScore.value = Number(detail.score || 0)
      ;(parsed.mcq || []).forEach(q => {
        isCorrectMap.value.mcq[q.id] = userAnswers.value.mcq?.[q.id] === q.answer
      })
      ;(parsed.fill_in_the_blank || []).forEach(q => {
        const u = (userAnswers.value.fill?.[q.id] || '').toString().trim().toLowerCase()
        const s = (q.answer || '').toString().trim().toLowerCase()
        isCorrectMap.value.fill[q.id] = !!u && u === s
      })
      isCorrectMap.value.coding = !!(userAnswers.value.coding || '').trim()
      currentStatus.value = 'review'
      } else {
      userAnswers.value = createAnswerDraft(parsed)
      currentStatus.value = 'testing'
    }
  } catch (e) {
    console.error(e)
    alert('读取试卷失败')
  }
}

const submitPaper = async () => {
  currentStatus.value = 'grading'
  loadingText.value = '导师正在逐题批改你的试卷，请稍候...'
  try {
    let score = 0
    const totalQuestions = (currentPaper.value.mcq || []).length + (currentPaper.value.fill_in_the_blank || []).length + 1
    const pointsPerQuestion = Math.floor(100 / totalQuestions)

    ;(currentPaper.value.mcq || []).forEach(q => {
      const ok = userAnswers.value.mcq[q.id] === q.answer
      isCorrectMap.value.mcq[q.id] = ok
      if (ok) score += pointsPerQuestion
    })

    const evalPrompt = `你现在是一位严厉但公平的阅卷老师。请根据标准答案，评判学生的填空题和编程题作答是否正确。
【判卷标准】：填空题只要核心语义一致即可算对；编程题只要逻辑正确即可算对。
【填空题数据】：
${(currentPaper.value.fill_in_the_blank || []).map(q => `题号:${q.id} | 标准答案:${q.answer} | 学生作答:${userAnswers.value.fill[q.id] || '未作答'}`).join('\n')}
【编程题数据】：
标准实现逻辑：${currentPaper.value.coding?.answer || ''}
学生作答代码：${userAnswers.value.coding || ''}
【输出格式】：{ "fill": { "f1": true }, "coding": true }`

    const evalRes = await request.post('/api/ai/chat/complete', { messages: [{ role: 'user', content: evalPrompt }] })
    const aiGrading = JSON.parse((evalRes.data.data || '').replace(/```json/gi, '').replace(/```/g, '').trim())

    ;(currentPaper.value.fill_in_the_blank || []).forEach(q => {
      const ok = aiGrading.fill[q.id] === true
      isCorrectMap.value.fill[q.id] = ok
      if (ok) score += pointsPerQuestion
    })

    isCorrectMap.value.coding = aiGrading.coding === true
    if (isCorrectMap.value.coding) score = Math.min(100, score + (100 - score))
    currentScore.value = score

    await request.post('/api/ai/quiz/submit', {
      id: currentRecordId.value,
      score: currentScore.value,
      userAnswers: JSON.stringify(userAnswers.value)
    })
    await loadHistory()
    currentStatus.value = 'review'
  } catch (e) {
    console.error(e)
    alert('批改失败，请稍后重试')
    currentStatus.value = 'testing'
  }
}

const renamePaper = async (paper) => {
  const title = window.prompt('请输入新的试卷名称', paper.title || '')
  if (title == null) return
  const next = title.trim()
  if (!next) return alert('名称不能为空')
  const res = await request.post(`/api/ai/quiz/${paper.id}/rename`, { title: next })
  if (res.data.code === 200) await loadHistory()
  else alert(res.data.msg || '重命名失败')
}

const deletePaper = async (paper) => {
  if (!window.confirm(`确定删除「${paper.title || '该记录'}」吗？`)) return
  const res = await request.delete(`/api/ai/quiz/${paper.id}`)
  if (res.data.code === 200) {
    await loadHistory()
    if (currentRecordId.value === paper.id) backToHome()
  } else {
    alert(res.data.msg || '删除失败')
  }
}

const backToHome = () => {
  currentStatus.value = 'idle'
  currentPaper.value = null
  currentRecordId.value = null
}
</script>

<template>
  <div class="quiz-system-page">
    <button class="history-toggle-btn" @click="historyVisible = !historyVisible">
      <History :size="16" /> 历次记录
    </button>

    <aside class="history-sidebar" v-if="historyVisible">
      <div class="sidebar-header">
        <History :size="18" class="icon-head" />
        <h3>历次强化练习记录</h3>
      </div>
      <div class="history-list">
        <div v-for="paper in historyPapers" :key="paper.id" class="history-item" @click="openPaper(paper)">
          <div class="item-main">
            <h4>{{ paper.title || '未命名练习' }}</h4>
            <p class="date">{{ new Date(paper.createTime).toLocaleString() }}</p>
          </div>
          <div class="item-right">
            <div v-if="paper.status === 1" :class="['score-tag', paper.score >= 80 ? 'high' : (paper.score >= 60 ? 'mid' : 'low')]">{{ paper.score }}</div>
            <div v-else-if="paper.status === 9" class="status-badge-mini">生成中</div>
            <div v-else-if="paper.status === -1" class="status-badge-mini">失败</div>
            <div v-else class="status-badge-mini">未完</div>
          </div>
          <div class="item-actions" @click.stop>
            <button class="mini-btn" @click="renamePaper(paper)">重命名</button>
            <button class="mini-btn danger" @click="deletePaper(paper)">删除</button>
          </div>
        </div>
      </div>
    </aside>

    <main class="quiz-main-content">
      <div v-if="currentStatus === 'idle'" class="state-view idle-view">
        <Sparkles :size="50" class="sparkle-icon" />
        <h2>选择你要强化的算法模块</h2>
        <p class="sub-title">试卷生成在后端异步执行，离开页面也会继续，生成后会出现在历次记录中。</p>
        <div class="module-cards-container">
          <div v-for="mod in availableModules" :key="mod.id" class="module-card" @click="generateNewPaper(mod.id, mod.name)">
            <div class="card-icon"><component :is="mod.icon" :size="28" /></div>
            <h3>{{ mod.name }}</h3>
            <p>{{ mod.desc }}</p>
            <button class="card-btn">生成专属试卷</button>
          </div>
        </div>
      </div>

      <div v-else-if="currentStatus === 'loading' || currentStatus === 'grading'" class="state-view loading-view">
        <div class="loader"></div>
        <p class="loading-text">{{ loadingText }}</p>
    </div>

      <div v-else class="paper-view">
        <header class="paper-header">
          <div class="title-area"><h1>{{ currentPaper.title }}</h1></div>
          <div class="header-actions">
            <div v-if="currentStatus === 'review'" class="final-score-box"><Award :size="24" /><span class="score-num">{{ currentScore }}</span><span class="score-unit">分</span></div>
            <button @click="backToHome" class="btn-back">返回选题大厅</button>
          </div>
        </header>

        <div class="paper-body">
          <section class="question-section">
            <div class="sec-header"><ListChecks :size="18" class="icon-sec" /><h2>一、单项选择题</h2></div>
            <div v-for="(q,index) in currentPaper.mcq" :key="q.id" class="question-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.mcq[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
        <div class="options-list">
                <label v-for="opt in q.options" :key="opt" :class="['option-label', userAnswers.mcq[q.id] === opt[0] ? 'is-selected' : '', currentStatus === 'review' && opt[0] === q.answer ? 'is-correct' : '', currentStatus === 'review' && userAnswers.mcq[q.id] === opt[0] && opt[0] !== q.answer ? 'is-wrong' : '']">
                  <input type="radio" :name="q.id" :value="opt[0]" v-model="userAnswers.mcq[q.id]" :disabled="currentStatus === 'review'"/>
                  <span class="opt-text">{{ opt }}</span>
                </label>
              </div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
              </div>
            </div>
          </section>

          <section class="question-section">
            <div class="sec-header"><FileSignature :size="18" class="icon-sec" /><h2>二、填空题</h2></div>
            <div v-for="(q,index) in currentPaper.fill_in_the_blank" :key="q.id" class="question-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.fill[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
              <div class="ans-area"><input type="text" v-model="userAnswers.fill[q.id]" placeholder="在此处填写答案" :disabled="currentStatus === 'review'"/></div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">参考答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
              </div>
            </div>
          </section>

          <section class="question-section">
            <div class="sec-header"><CodeXml :size="18" class="icon-sec" /><h2>三、算法编程题</h2></div>
            <div class="question-item">
              <div class="q-title">
                <span class="q-index">1.</span>
                <pre class="q-text">{{ currentPaper.coding.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.coding" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
              <div class="code-editor-wrapper">
                <div class="editor-header"><span class="lang-tag">{{ currentPaper.coding.language }}</span></div>
                <textarea v-model="userAnswers.coding" class="code-textarea" :disabled="currentStatus === 'review'"></textarea>
              </div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准实现代码：</p>
                <pre class="code-standard">{{ currentPaper.coding.answer }}</pre>
                <p class="exp-text">导师解析：{{ currentPaper.coding.explanation }}</p>
              </div>
            </div>
          </section>
        </div>

        <footer v-if="currentStatus === 'testing'" class="paper-footer">
          <button @click="submitPaper" class="btn-primary btn-submit"><Award :size="16" /> 交卷并由 AI 智能批改</button>
        </footer>
      </div>
    </main>
  </div>
</template>

<style scoped>
.quiz-system-page { --bg-main: #F5F5F7; --bg-card: #FFFFFF; --text-primary: #1D1D1F; --text-secondary: #86868B; --accent: #007AFF; --success: #28CD41; --warning: #FF9500; --danger: #FF3B30; --border: #D2D2D7; display: flex; width: 100%; min-width: 0; height: 100%; background: var(--bg-main); color: var(--text-primary); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica; overflow: hidden; position: relative; }
.history-toggle-btn { position: absolute; left: 14px; top: 12px; z-index: 20; border: 1px solid var(--border); background: #fff; border-radius: 10px; padding: 6px 10px; font-size: 12px; display: flex; align-items: center; gap: 6px; cursor: pointer; }
.history-sidebar { width: 320px; background: var(--bg-card); border-right: 1px solid var(--border); display: flex; flex-direction: column; z-index: 10; margin-top: 48px; }
.sidebar-header { padding: 16px; border-bottom: 1px solid var(--border); display: flex; align-items: center; gap: 8px; }
.sidebar-header h3 { font-size: 14px; margin: 0; }
.history-list { flex: 1; overflow-y: auto; padding: 8px; }
.history-item { padding: 10px; border-radius: 10px; margin-bottom: 8px; background: #fafafa; border: 1px solid #eee; cursor: pointer; }
.item-main h4 { margin: 0; font-size: 13px; }
.date { margin: 6px 0 0; color: #888; font-size: 12px; }
.item-right { margin-top: 8px; }
.item-actions { margin-top: 8px; display: flex; gap: 6px; }
.mini-btn { border: 1px solid #ddd; background: #fff; border-radius: 8px; font-size: 12px; padding: 4px 8px; cursor: pointer; }
.mini-btn.danger { color: #dc2626; border-color: #f0b4b4; }
.status-badge-mini { font-size: 11px; padding: 4px 8px; background: #eee; border-radius: 6px; color: #666; display: inline-block; }
.score-tag { font-size: 13px; font-weight: 700; width: 40px; height: 28px; border-radius: 14px; display: flex; align-items: center; justify-content: center; }
.score-tag.high { background: #EAFBF0; color: var(--success); }
.score-tag.mid { background: #FFF8E6; color: var(--warning); }
.score-tag.low { background: #FFEBEB; color: var(--danger); }
.quiz-main-content { flex: 1; width: 100%; min-width: 0; overflow: hidden; position: relative; margin-top: 48px; }
.state-view { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; overflow-y: auto; padding: 24px 40px 40px; text-align: center; background: #fff; box-sizing: border-box; }
.state-view h2 { font-size: 24px; margin: 16px 0 8px; }
.sub-title { color: var(--text-secondary); max-width: 680px; margin-bottom: 30px; }
.module-cards-container { display: flex; gap: 18px; flex-wrap: wrap; justify-content: center; align-content: flex-start; }
.module-card { background: #FBFCFE; border: 1px solid var(--border); border-radius: 18px; padding: 24px 18px; width: 240px; display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: .2s; }
.module-card:hover { transform: translateY(-6px); box-shadow: 0 8px 24px rgba(0,0,0,.08); }
.card-icon { width: 54px; height: 54px; background: #E6F2FF; color: var(--accent); border-radius: 14px; display: flex; align-items: center; justify-content: center; margin-bottom: 16px; }
.module-card h3 { font-size: 17px; margin: 0 0 8px; }
.module-card p { font-size: 12px; color: #666; margin: 0 0 16px; text-align: center; }
.card-btn { background: #1D1D1F; color: #fff; border: none; border-radius: 9px; padding: 8px 10px; width: 100%; font-size: 12px; }
.loader { border: 4px solid #f3f3f3; border-top: 4px solid var(--accent); border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.paper-view { height: 100%; display: flex; flex-direction: column; background: #fff; }
.paper-header { padding: 18px 30px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
.paper-header h1 { font-size: 18px; margin: 0; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.btn-back { background: var(--bg-main); border: 1px solid var(--border); border-radius: 8px; padding: 8px 12px; cursor: pointer; }
.final-score-box { display: flex; align-items: baseline; gap: 4px; color: var(--accent); padding: 8px 12px; background: #E6F2FF; border-radius: 10px; }
.score-num { font-size: 24px; font-weight: 800; line-height: 1; }
.paper-body { flex: 1; overflow-y: auto; padding: 24px 28px; }
.question-section { margin-bottom: 28px; }
.sec-header { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: var(--accent); border-bottom: 2px solid #E6F2FF; padding-bottom: 8px; }
.sec-header h2 { font-size: 15px; margin: 0; }
.question-item { margin-bottom: 18px; padding: 14px; border-radius: 10px; background: #FBFCFE; }
.q-title { display: flex; gap: 10px; align-items: flex-start; margin-bottom: 10px; position: relative; }
.q-index { color: var(--accent); font-weight: 700; }
.q-text { margin: 0; white-space: pre-wrap; line-height: 1.6; flex: 1; font-size: 14px; }
.status-badge { position: absolute; right: -6px; top: -6px; }
.icon-right { color: var(--success); }
.icon-wrong { color: var(--danger); }
.options-list { display: flex; flex-direction: column; gap: 8px; padding-left: 22px; }
.option-label { display: flex; align-items: center; gap: 10px; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; }
.option-label input { margin: 0; }
.option-label.is-selected { border-color: var(--accent); background: #E6F2FF; }
.option-label.is-correct { border-color: var(--success); background: #EAFBF0; }
.option-label.is-wrong { border-color: var(--danger); background: #FFEBEB; }
.ans-area { padding-left: 22px; }
.ans-area input { width: 320px; border: 1px solid var(--border); border-radius: 8px; padding: 9px 12px; }
.code-editor-wrapper { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: #282c34; }
.editor-header { padding: 5px 10px; background: #21252b; border-bottom: 1px solid #181a1f; }
.lang-tag { color: #adb0b8; font-size: 10px; text-transform: uppercase; font-weight: 700; }
.code-textarea { width: 100%; height: 140px; background: transparent; color: #abb2bf; border: none; outline: none; resize: none; padding: 12px; font-family: monospace; }
.explanation-panel { margin-top: 10px; background: #f2f6ff; border-left: 4px solid var(--accent); border-radius: 8px; padding: 10px; font-size: 13px; }
.highlight { color: var(--success); font-weight: 700; }
.code-standard { background: #ececf2; border-radius: 6px; padding: 8px; font-size: 12px; overflow-x: auto; }
.paper-footer { border-top: 1px solid var(--border); padding: 16px 28px; display: flex; justify-content: flex-end; }
.btn-primary { background: var(--accent); color: #fff; border: none; border-radius: 10px; padding: 10px 18px; display: inline-flex; align-items: center; gap: 8px; cursor: pointer; }
</style>
