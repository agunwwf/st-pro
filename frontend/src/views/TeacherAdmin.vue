<template>
  <div class="teacher-admin-layout">
    <div class="animated-bg"></div>

    <nav class="glass-navbar">
      <div class="nav-left">
        <div class="logo-area">
          <el-icon class="logo-icon"><Monitor /></el-icon>
          <span class="logo-text">教师中心</span>
        </div>
      </div>
      <div class="nav-center">
        <div class="nav-tabs">
          <div class="tab-item" :class="{ active: currentView === 'students' }" @click="currentView = 'students'">
            <el-icon><User /></el-icon> 班级学生
          </div>
          <div class="tab-item" :class="{ active: currentView === 'exams' }" @click="currentView = 'exams'">
            <el-icon><DocumentCopy /></el-icon> 测验中台
          </div>
        </div>
      </div>
      <div class="nav-right">
        <el-button round class="exit-btn" :icon="Back" @click="router.push('/dashboard')">返回大厅</el-button>
      </div>
    </nav>

    <main class="main-content">
      <transition name="fade-slide" mode="out-in">

        <div v-if="currentView === 'students'" class="view-container" key="students">
          <div class="glass-panel">
            <div class="panel-header">
              <h2>班级学生</h2>
              <div style="display:flex; align-items:center; gap: 12px;">
                <el-input v-model="searchStudent" placeholder="搜索姓名/账号..." :prefix-icon="Search" style="width: 250px" />
                <el-button type="primary" :icon="Plus" @click="addStudentVisible = true">添加学生</el-button>
              </div>
            </div>
            <el-tabs v-model="studentTab" class="apple-tabs" style="margin-top: 8px;">
              <el-tab-pane label="学生列表" name="list">
                <el-table :data="filteredStudents" style="width: 100%" class="apple-table" row-class-name="apple-table-row" v-loading="loadingStudents">
              <el-table-column label="学生信息" min-width="250">
                <template #default="scope">
                  <div class="student-cell">
                    <el-avatar :size="44" :src="scope.row.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" shape="square" />
                    <div class="student-meta">
                      <span class="student-name">{{ scope.row.nickname || scope.row.username }}</span>
                      <span class="student-id">ID: {{ scope.row.username }}</span>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="综合进度">
                <template #default="scope">
                  <el-progress :percentage="scope.row.progress || 0" :color="getProgressColor(scope.row.progress)" />
                </template>
              </el-table-column>
              <el-table-column prop="checkInCount" label="打卡天数" align="center" width="120" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <el-button circle type="primary" :icon="ChatDotRound" @click="messageStudent(scope.row)" />
                  <el-button circle type="danger" :icon="Delete" plain @click="confirmDelete(scope.row)" />
                </template>
              </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="入班/换导师申请" name="requests">
                <div style="display:flex; justify-content: flex-end; margin: 8px 0 12px 0;">
                  <el-button @click="loadRequests">刷新申请列表</el-button>
                </div>
                <el-table :data="requests" style="width: 100%" v-loading="loadingRequests">
                  <el-table-column prop="student_id" label="学生ID" width="120" />
                  <el-table-column prop="req_type" label="类型" width="120" />
                  <el-table-column prop="old_teacher_id" label="原导师" width="120" />
                  <el-table-column prop="new_teacher_id" label="新导师" width="120" />
                  <el-table-column prop="approve_old" label="原导师同意" width="120" />
                  <el-table-column prop="approve_new" label="新导师同意" width="120" />
                  <el-table-column prop="create_time" label="申请时间" min-width="180" />
                  <el-table-column label="操作" width="200" fixed="right">
                    <template #default="scope">
                      <el-button type="success" @click="approveRequest(scope.row)">同意</el-button>
                      <el-button type="danger" plain @click="rejectRequest(scope.row)">拒绝</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>

        <div v-else-if="currentView === 'exams'" class="view-container" key="exams">
          <div class="action-banner glass-panel">
            <div class="banner-text">
              <h2>测验与试卷中台</h2>
              <p>组建标准试卷，一键下发至班级学生。</p>
            </div>
            <el-button type="primary" size="large" class="hero-btn" :icon="Plus" @click="openAssembleWorkshop">
              新建试卷模板
            </el-button>
          </div>

          <div class="glass-panel main-tabs">
            <el-tabs v-model="activeExamTab" class="apple-tabs">
              <el-tab-pane label="📚 试卷模板库" name="papers">
                <div class="grid-container">
                  <div class="exam-card exam-card-interactive" v-for="paper in papers" :key="paper.id" @click="openPaperDetail(paper)">
                    <div class="card-badge">{{ paper.category }}</div>
                    <h3>{{ paper.title }}</h3>
                    <p class="meta">题目数: {{ paper.questionCount || paper.question_count || 0 }} | 总分: {{ paper.totalScore || paper.total_score || 0 }}</p>

                    <div class="tags-row">
                      <el-tag size="small" type="info" effect="plain">选择题</el-tag>
                      <el-tag size="small" type="info" effect="plain">填空题</el-tag>
                      <el-tag size="small" type="info" effect="plain">编程题</el-tag>
                    </div>

                    <div class="card-actions">
                      <el-button type="primary" round style="color: #fff; font-weight: bold;" :icon="Promotion" @click.stop="openPublishExam(paper.id)">
                        发布测验
                      </el-button>
                      <el-button type="danger" round plain :icon="Delete" @click.stop="confirmDeletePaper(paper)">删除</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="🚀 考试任务" name="assignments">
                <div style="display: flex; justify-content: flex-end; margin-bottom: 20px;">
                  <el-button type="primary" :icon="Promotion" @click="openPublishExam(null)">发布新考试</el-button>
                </div>
                <div class="grid-container">
                  <div class="exam-card exam-card-interactive" v-for="item in assignments" :key="item.id" @click="goToExamAnalytics(item)">
                    <div class="card-header">
                      <span class="status-badge active">已发布</span>
                      <span class="meta"><el-icon><Timer /></el-icon> {{ item.timeLimitMinutes }} 分钟</span>
                    </div>
                    <h3 style="margin: 10px 0;">{{ item.publishName }}</h3>
                    <p class="meta" style="color:#0071e3">试卷: {{ item.paperTitle }}</p>
                    <p class="meta" style="color:#ff3b30">截止: {{ formatDateTime(item.endTime) }}</p>
                    <p class="meta hint-line">点击查看分析（截止后展示成绩统计）</p>
                    <div class="card-actions">
                      <el-button type="danger" round plain :icon="Delete" @click.stop="confirmDeleteAssignment(item)">删除任务</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>

      </transition>
    </main>

    <el-dialog v-model="assembleWorkshopVisible" fullscreen :show-close="false" class="workshop-dialog">
      <div class="workshop-layout">

        <header class="workshop-header">
          <div class="wh-left">
            <el-button :icon="Back" circle @click="assembleWorkshopVisible = false" />
            <span class="wh-title">{{ previewMode ? '试卷预览模式' : '沉浸式组卷工作台' }}</span>
          </div>
          <div class="wh-center">
            <el-input v-model="paperForm.title" placeholder="请输入试卷大标题..." class="title-input" />
            <el-select v-model="paperForm.category" size="large" style="width: 100%">
              <el-option label="K-Means 聚类" value="kmeans" />
              <el-option label="线性回归" value="linear" />
              <el-option label="逻辑回归" value="logistic" />
              <el-option label="神经网络" value="neural" />
              <el-option label="文本分析与分类 (Bayes)" value="text" />
            </el-select>
          </div>
          <div class="wh-right">
            <span class="q-count-indicator">已选: <strong>{{ paperForm.selectedQuestions.length }}</strong> 题</span>
            <el-button v-if="!previewMode" type="info" plain @click="previewMode = true">预览试卷</el-button>
            <el-button v-else type="info" plain @click="previewMode = false">继续选题</el-button>

            <el-button type="warning" @click="handleCreatePaper" style="font-weight: bold; margin-left: 10px;">
              确认组卷并保存
            </el-button>
          </div>
        </header>

        <div class="workshop-body" v-if="!previewMode">
          <aside class="ws-sidebar">
            <div class="filter-group">
              <h4>按题型筛选</h4>
              <div
                  class="filter-item"
                  :class="{ active: currentQuestionType === 'ALL' }"
                  @click="currentQuestionType = 'ALL'">全部题型</div>
              <div
                  class="filter-item"
                  :class="{ active: currentQuestionType === '选择题' }"
                  @click="currentQuestionType = '选择题'">🔘 选择题</div>
              <div
                  class="filter-item"
                  :class="{ active: currentQuestionType === '填空题' }"
                  @click="currentQuestionType = '填空题'">📝 填空题</div>
              <div
                  class="filter-item"
                  :class="{ active: currentQuestionType === '编程题' }"
                  @click="currentQuestionType = '编程题'">💻 编程题</div>
            </div>
          </aside>

          <main class="ws-question-pool">
            <div class="pool-header">
              <div style="display:flex; align-items:center; justify-content: space-between; gap: 16px;">
                <span>正在显示：{{ currentQuestionType === 'ALL' ? '全部题目' : currentQuestionType }}（共 {{ questionTotal }} 题）</span>
                <el-input
                  v-model="questionKeyword"
                  placeholder="搜索题干关键词..."
                  clearable
                  style="max-width: 320px"
                  @clear="() => { questionPage = 1; loadQuestionPage() }"
                  @keyup.enter="() => { questionPage = 1; loadQuestionPage() }"
                />
              </div>
            </div>

            <div v-loading="loadingQuestions">
              <div
                v-for="q in filteredQuestions"
                :key="q.id"
                class="wq-card"
                :class="{ selected: paperForm.selectedQuestions.includes(q.id) }"
                @click="toggleQuestion(q.id)"
              >
                <div class="wq-header">
                  <el-tag size="small" :type="getQTypeTag(q.type)">{{ q.type }}</el-tag>
                  <div class="wq-checkbox">
                    <div class="check-ring"></div>
                  </div>
                </div>
                <div class="wq-content">{{ q.content }}</div>
                <div v-if="q.options && q.type === '选择题'" class="wq-options">
                  <div class="opt-item" v-for="(opt, idx) in parseOptions(q.options)" :key="idx">{{ opt }}</div>
                </div>
              </div>

              <div v-if="questionTotal > 0" style="display:flex; justify-content:center; padding: 16px 0;">
                <el-config-provider :locale="zhCn">
                  <el-pagination
                    background
                    layout="prev, pager, next, sizes, total"
                    :total="questionTotal"
                    v-model:current-page="questionPage"
                    v-model:page-size="questionPageSize"
                    :page-sizes="[10, 20, 50, 100, 200]"
                    @current-change="() => loadQuestionPage()"
                    @size-change="() => { questionPage = 1; loadQuestionPage() }"
                  />
                </el-config-provider>
              </div>
            </div>
          </main>
        </div>

        <div class="workshop-body preview-body" v-else>
          <div class="preview-paper-container">
            <h1 class="preview-title">{{ paperForm.title || '未命名试卷' }}</h1>
            <p class="preview-meta">模块: {{ paperForm.category }} | 总题数: {{ paperForm.selectedQuestions.length }} 题</p>

            <el-divider />

            <div class="preview-q-list" v-if="selectedQuestionDetails.length > 0">
              <div v-for="(q, index) in selectedQuestionDetails" :key="q.id" class="preview-q-item">
                <div class="pq-header">
                  <strong>{{ index + 1 }}. ({{ q.type }})</strong> {{ q.content }}
                </div>
                <div v-if="q.options && q.type === '选择题'" class="pq-options">
                  <div v-for="(opt, idx) in parseOptions(q.options)" :key="idx" class="pq-opt">{{ opt }}</div>
                </div>
                <div v-if="q.type === '填空题'" class="pq-blank">
                  <el-input disabled placeholder="学生作答区" />
                </div>
                <div v-if="q.type === '编程题'" class="pq-coding">
                  <div class="fake-editor"># 请在此处编写 Python 代码...</div>
                </div>
              </div>
            </div>
            <div v-else class="empty-preview">
              <p>您还没有选择任何题目，快去题库挑选吧！</p>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="publishExamVisible" title="🚀 发布考试任务" width="600px" class="apple-dialog">
      <el-form :model="examForm" label-position="top">
        <el-form-item label="考试任务名称"><el-input v-model="examForm.publishName" size="large" /></el-form-item>
        <el-form-item label="引用试卷">
          <el-select v-model="examForm.paperId" size="large" style="width: 100%">
            <el-option v-for="p in papers" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <div style="display: flex; gap: 16px;">
          <el-form-item label="起止时间" style="flex: 2">
            <el-date-picker
              v-model="examForm.dateRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              size="large"
              teleported
              placement="bottom-start"
              :popper-options="datePickerPopperOptions"
              popper-class="exam-date-popper"
            />
          </el-form-item>
          <el-form-item label="限时(分钟)" style="flex: 1">
            <el-input-number v-model="examForm.timeLimit" :min="5" size="large" style="width: 100%" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="publishExamVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handlePublishExam" round>立即发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperDetailVisible" title="试卷模板详情" width="760px" class="apple-dialog" destroy-on-close>
      <div v-loading="paperDetailLoading">
        <template v-if="paperDetail">
          <p class="paper-detail-meta">
            <strong>{{ paperDetail.paper?.title }}</strong>
            <span class="muted"> · 模块 {{ paperDetail.paper?.category }} · 共 {{ (paperDetail.questions || []).length }} 题</span>
          </p>
          <el-divider />
          <div v-for="(q, idx) in paperDetail.questions || []" :key="q.id" class="paper-q-block">
            <div class="pq-head">
              <el-tag size="small" :type="getQTypeTag(q.type)">{{ q.type }}</el-tag>
              <strong>{{ idx + 1 }}.</strong>
              <span>{{ q.content }}</span>
            </div>
            <div v-if="q.options && q.type === '选择题'" class="pq-opts">
              <div v-for="(opt, oi) in parseOptions(q.options)" :key="oi">{{ opt }}</div>
            </div>
            <p class="answer-line"><span class="label">参考答案：</span>{{ q.standardAnswer || '—' }}</p>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button round @click="paperDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addStudentVisible" title="添加学生到我的班级" width="520px" class="apple-dialog">
      <el-form label-position="top">
        <el-form-item label="学生账号（username）">
          <el-input v-model="addStudentUsername" placeholder="请输入已注册学生账号" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addStudentVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handleAddStudent" round :loading="addingStudent">确定添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, DocumentCopy, Plus, Promotion, Search, Timer, Back, User, ChatDotRound, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import request from '@/utils/request'
window.axios = request

const router = useRouter()
const currentView = ref('exams') // 默认进入测验页面

// --- 基础工具函数 ---
const getCurrentTeacherId = () => JSON.parse(localStorage.getItem('user') || '{}').id
const getProgressColor = (p) => { if (p < 30) return '#ff4d4f'; if (p < 70) return '#faad14'; return '#34c759' }

// ================== 学生管理逻辑 ==================
const studentTab = ref('list')
const students = ref([]); const loadingStudents = ref(false); const searchStudent = ref('')
const filteredStudents = computed(() => {
  const kw = (searchStudent.value || '').trim().toLowerCase()
  if (!kw) return students.value
  return (students.value || []).filter(s => {
    const name = (s.nickname || '').toLowerCase()
    const u = (s.username || '').toLowerCase()
    return name.includes(kw) || u.includes(kw)
  })
})

const requests = ref([])
const loadingRequests = ref(false)

const addStudentVisible = ref(false)
const addStudentUsername = ref('')
const addingStudent = ref(false)
const loadStudents = async () => {
  loadingStudents.value = true
  try {
    // 后端以 JWT 为准，禁止前端透传 teacherId（防 IDOR）
    const res = await axios.get(`/api/teacher/my-students`)
    if (res.data.code === 200) students.value = res.data.data
  } catch (e) {} finally { loadingStudents.value = false }
}
const messageStudent = (student) => router.push({ path: '/chat', query: { target: student.username } })
const confirmDelete = (student) => {
  ElMessageBox.confirm(`确定踢出 ${student.nickname || student.username} 吗？踢出后该学生将无法再次申请加入你班级（除非你手动添加）。`, '警告', { type: 'warning' }).then(async () => {
    const res = await axios.post(`/api/teacher/class/students/${student.id}/kick`, { reason: 'kick' })
    if (res.data.code === 200) { ElMessage.success('已踢出并拉黑'); loadStudents() }
    else ElMessage.error(res.data.msg || '操作失败')
  }).catch(() => {})
}

const loadRequests = async () => {
  loadingRequests.value = true
  try {
    const res = await axios.get('/api/teacher/class/requests', { params: { status: 'PENDING' } })
    if (res.data.code === 200) requests.value = res.data.data || []
  } finally {
    loadingRequests.value = false
  }
}

const approveRequest = async (row) => {
  const res = await axios.post(`/api/teacher/class/requests/${row.id}/approve`)
  if (res.data.code === 200) {
    ElMessage.success(res.data.data?.finalized ? '已同意，申请已生效' : '已同意，等待另一方同意')
    loadRequests()
    loadStudents()
  } else ElMessage.error(res.data.msg || '操作失败')
}

const rejectRequest = async (row) => {
  const res = await axios.post(`/api/teacher/class/requests/${row.id}/reject`)
  if (res.data.code === 200) {
    ElMessage.success('已拒绝')
    loadRequests()
  } else ElMessage.error(res.data.msg || '操作失败')
}

const formatDateTime = (v) => {
  if (!v) return '-'
  const s = String(v).trim()
  const normalized = s.replace('T', ' ').replace(/\.\d+$/, '')
  return normalized.length >= 16 ? normalized.substring(0, 16) : normalized
}

const handleAddStudent = async () => {
  const username = (addStudentUsername.value || '').trim()
  if (!username) return ElMessage.warning('请输入学生账号')
  addingStudent.value = true
  try {
    const res = await axios.post('/api/teacher/class/students/add', { username })
    if (res.data.code === 200) {
      ElMessage.success('添加成功')
      addStudentVisible.value = false
      addStudentUsername.value = ''
      loadStudents()
      return
    }
    // 被踢出需要强制添加
    if ((res.data.msg || '').includes('强制')) {
      await ElMessageBox.confirm(res.data.msg, '需要确认', { type: 'warning' })
      const forceRes = await axios.post('/api/teacher/class/students/add', { username, force: true })
      if (forceRes.data.code === 200) {
        ElMessage.success('已强制添加')
        addStudentVisible.value = false
        addStudentUsername.value = ''
        loadStudents()
        return
      }
      ElMessage.error(forceRes.data.msg || '强制添加失败')
      return
    }
    ElMessage.error(res.data.msg || '添加失败')
  } finally {
    addingStudent.value = false
  }
}

// ================== LMS 测验中台 ==================
const activeExamTab = ref('papers')
const assembleWorkshopVisible = ref(false) // 全屏组卷工作台开关
const publishExamVisible = ref(false)
const previewMode = ref(false) // 预览模式开关
const currentQuestionType = ref('ALL') // 题型分类器

const questionBank = ref([])
const questionKeyword = ref('')
const questionPage = ref(1)
const questionPageSize = ref(20)
const questionTotal = ref(0)
const loadingQuestions = ref(false)
const papers = ref([])
const assignments = ref([])

const paperDetailVisible = ref(false)
const paperDetailLoading = ref(false)
const paperDetail = ref(null)

const paperForm = reactive({ title: '', category: 'kmeans', selectedQuestions: [] })
const examForm = reactive({ paperId: null, publishName: '', dateRange: [], timeLimit: 45 })

const loadQuestionPage = async () => {
  loadingQuestions.value = true
  try {
    const res = await axios.get('/api/teacher/lms/questions/page', {
      params: {
        page: questionPage.value,
        size: questionPageSize.value,
        category: paperForm.category,
        type: currentQuestionType.value,
        keyword: questionKeyword.value?.trim() || ''
      }
    })
    if (res.data.code === 200) {
      const data = res.data.data || {}
      questionBank.value = data.items || []
      questionTotal.value = data.total || 0
    }
  } catch (e) {
    // 失败时保留旧数据即可
  } finally {
    loadingQuestions.value = false
  }
}

const loadExamsData = async () => {
  try {
    const [pRes, aRes] = await Promise.all([
      axios.get(`/api/teacher/lms/papers`),
      axios.get(`/api/teacher/lms/assignments`)
    ])
    await loadQuestionPage()
    if(pRes.data.code === 200) papers.value = pRes.data.data
    if(aRes.data.code === 200) assignments.value = aRes.data.data
  } catch (error) { console.warn("LMS数据拉取失败") }
}

onMounted(() => { loadStudents(); loadRequests(); loadExamsData() })

// 打开全屏组卷
const openAssembleWorkshop = () => {
  paperForm.title = ''
  paperForm.selectedQuestions = []
  previewMode.value = false
  currentQuestionType.value = 'ALL'
  questionKeyword.value = ''
  questionPage.value = 1
  questionTotal.value = 0
  assembleWorkshopVisible.value = true
  loadQuestionPage()
}

// 动态计算筛选后的题目
const filteredQuestions = computed(() => {
  // 题库筛选已后置到后端，这里直接展示当前页
  return questionBank.value
})

watch(() => paperForm.category, () => {
  if (!assembleWorkshopVisible.value || previewMode.value) return
  questionPage.value = 1
  loadQuestionPage()
})

watch(() => currentQuestionType.value, () => {
  if (!assembleWorkshopVisible.value || previewMode.value) return
  questionPage.value = 1
  loadQuestionPage()
})

// 动态计算已选中的题目详情 (用于预览)
const selectedQuestionDetails = computed(() => {
  return paperForm.selectedQuestions.map(id => questionBank.value.find(q => q.id === id)).filter(Boolean)
})

// 题库交互函数
const toggleQuestion = (id) => {
  const idx = paperForm.selectedQuestions.indexOf(id)
  if (idx > -1) paperForm.selectedQuestions.splice(idx, 1)
  else paperForm.selectedQuestions.push(id)
}
const parseOptions = (optStr) => {
  if (!optStr) return []
  try { return JSON.parse(optStr) } catch(e) { return [] }
}
const getQTypeTag = (type) => {
  if(type === '选择题') return 'primary'
  if(type === '填空题') return 'warning'
  return 'success'
}

// 确认组卷提交
const handleCreatePaper = async () => {
  if (!paperForm.title || paperForm.selectedQuestions.length === 0) return ElMessage.warning('需填标题并至少选1题')
  try {
    const res = await axios.post('/api/teacher/lms/paper/create', {
      title: paperForm.title, category: paperForm.category, questionIds: paperForm.selectedQuestions
    })
    if (res.data.code === 200) {
      ElMessage.success('🎉 试卷组建成功')
      assembleWorkshopVisible.value = false
      loadExamsData()
    }
  } catch (e) { ElMessage.error('组卷失败') }
}

// 发布考试逻辑
const openPaperDetail = async (paper) => {
  if (!paper?.id) return
  paperDetailVisible.value = true
  paperDetailLoading.value = true
  paperDetail.value = null
  try {
    const res = await axios.get(`/api/teacher/lms/paper/${paper.id}/detail`)
    if (res.data.code === 200) paperDetail.value = res.data.data
    else ElMessage.error(res.data.msg || '加载失败')
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    paperDetailLoading.value = false
  }
}

const confirmDeletePaper = (paper) => {
  ElMessageBox.confirm(
      `确定删除试卷模板「${paper.title}」？此操作不可恢复。若该模板已被考试任务引用，需先删除考试任务。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).then(async () => {
    const res = await axios.delete(`/api/teacher/lms/paper/${paper.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除模板')
      loadExamsData()
    } else ElMessage.error(res.data.msg || '删除失败')
  }).catch(() => {})
}

const goToExamAnalytics = (item) => {
  if (!item?.id) return
  router.push(`/teacher/exam-analytics/${item.id}`)
}

const confirmDeleteAssignment = (item) => {
  ElMessageBox.confirm(
      `确定删除考试任务「${item.publishName}」？将同时删除所有学生在本场考试的答卷记录，且不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  ).then(async () => {
    const res = await axios.delete(`/api/teacher/lms/assignment/${item.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除考试任务')
      loadExamsData()
    } else ElMessage.error(res.data.msg || '删除失败')
  }).catch(() => {})
}

const openPublishExam = (paperId = null) => {
  examForm.paperId = paperId; examForm.publishName = ''; examForm.dateRange = []
  publishExamVisible.value = true
}
const handlePublishExam = async () => {
  if (!examForm.publishName || !examForm.paperId || examForm.dateRange.length === 0) return ElMessage.warning('请填写完整')
  try {
    const res = await axios.post('/api/teacher/lms/assignment/publish', {
      paperId: examForm.paperId, publishName: examForm.publishName,
      startTime: examForm.dateRange[0], endTime: examForm.dateRange[1], timeLimitMinutes: examForm.timeLimit
    })
    if (res.data.code === 200) {
      ElMessage.success('🚀 考试发布成功')
      publishExamVisible.value = false
      loadExamsData()
    }
  } catch (e) { ElMessage.error('发布失败') }
}
</script>

<style scoped>

.teacher-admin-layout {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  background-color: #f4f6fb;
}

/* 日期选择器弹层：挂到 body 后用 :global 才能生效（teleported） */
:global(.exam-date-popper) {
  max-height: calc(100vh - 120px);
  overflow: auto;
}

:global(.exam-date-popper .el-picker-panel__body-wrapper) {
  max-height: calc(100vh - 160px);
  overflow: auto;
}
.animated-bg { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 0; background-image: radial-gradient(at 0% 0%, hsla(210,100%,92%,1) 0px, transparent 50%), radial-gradient(at 100% 0%, hsla(240,100%,94%,1) 0px, transparent 50%); animation: bgFlow 15s ease infinite alternate; }
@keyframes bgFlow { 0% { transform: scale(1); } 100% { transform: scale(1.1); } }
.glass-navbar { position: fixed; top: 0; left: 0; right: 0; height: 64px; background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(20px); display: flex; justify-content: space-between; align-items: center; padding: 0 40px; z-index: 100; box-shadow: 0 4px 20px rgba(0,0,0,0.02); }
.logo-area { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; color: #1d1d1f; }
.logo-icon { color: #0071e3; font-size: 24px; }
.nav-tabs { display: flex; gap: 16px; background: rgba(0,0,0,0.04); padding: 4px; border-radius: 980px; }
.tab-item { padding: 8px 24px; border-radius: 980px; cursor: pointer; font-weight: 600; font-size: 14px; color: #86868b; transition: all 0.3s; display: flex; align-items: center; gap: 6px; }
.tab-item.active { background: #fff; color: #1d1d1f; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.main-content { position: relative; z-index: 10; padding: 100px 40px 40px 40px; max-width: 1400px; margin: 0 auto; }
.glass-panel { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px); border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 24px; padding: 32px; box-shadow: 0 10px 40px rgba(0,0,0,0.05); margin-bottom: 24px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.student-cell { display: flex; align-items: center; gap: 12px; }
.student-meta { display: flex; flex-direction: column; }
.student-name { font-weight: 600; color: #1d1d1f; font-size: 15px; }
.student-id { font-size: 12px; color: #86868b; }
.action-banner { display: flex; justify-content: space-between; align-items: center; background: linear-gradient(135deg, rgba(255,255,255,0.9), rgba(230,242,255,0.8)); }
.grid-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; margin-top: 16px; }
.exam-card { background: #fff; border-radius: 16px; padding: 24px; border: 1px solid #e5e5ea; position: relative; }
.exam-card-interactive { cursor: pointer; transition: box-shadow 0.2s ease, border-color 0.2s ease; }
.exam-card-interactive:hover {
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  border-color: #c7ddff;
}
.hint-line { font-size: 12px; color: #aeaeb2; margin-top: 8px; }
.paper-detail-meta { margin: 0 0 8px; font-size: 15px; }
.paper-detail-meta .muted { color: #86868b; font-weight: normal; }
.paper-q-block { margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f2f2f7; }
.paper-q-block:last-child { border-bottom: none; }
.pq-head { display: flex; flex-wrap: wrap; align-items: baseline; gap: 8px; line-height: 1.5; }
.pq-opts { margin: 10px 0 0 12px; color: #636366; font-size: 13px; }
.answer-line { margin: 10px 0 0; font-size: 13px; color: #34c759; }
.answer-line .label { color: #86868b; margin-right: 6px; }
.card-badge { position: absolute; top: 24px; right: 24px; font-size: 12px; font-weight: 600; color: #8E8E93; background: #F2F2F7; padding: 4px 10px; border-radius: 6px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.status-badge.active { color: #0071e3; background: #e6f2ff; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600;}
.meta { font-size: 13px; color: #86868b; margin: 4px 0; }
.card-actions { margin-top: 24px; display: flex; gap: 10px; }

/* 试卷题型标签 */
.tags-row { margin-top: 12px; display: flex; gap: 8px; }

.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.fade-slide-enter-from { opacity: 0; transform: translateY(20px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-20px); }

/* ================== 全屏组卷工作台 (Workshop) 样式 ================== */
:deep(.workshop-dialog) { background: #f4f6fb; margin: 0 !important; }
:deep(.workshop-dialog .el-dialog__header) { display: none; }
:deep(.workshop-dialog .el-dialog__body) { padding: 0; height: 100vh; display: flex; flex-direction: column; }

.workshop-layout { display: flex; flex-direction: column; height: 100vh; }
.workshop-header {
  height: 64px; background: #fff; display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px; border-bottom: 1px solid #e5e5ea; box-shadow: 0 2px 10px rgba(0,0,0,0.02);
}
.wh-left { display: flex; align-items: center; gap: 16px; }
.wh-title { font-size: 18px; font-weight: 700; color: #1d1d1f; }
.wh-center { flex: 1; display: flex; justify-content: center; }
.title-input { width: 300px; }
.wh-right { display: flex; align-items: center; gap: 16px; }
.q-count-indicator { font-size: 14px; color: #86868b; }
.q-count-indicator strong { color: #ff9500; font-size: 18px; }

.workshop-body { flex: 1; display: flex; overflow: hidden; }

/* 左侧分类侧边栏 */
.ws-sidebar { width: 220px; background: #fff; border-right: 1px solid #e5e5ea; padding: 24px; }
.filter-group h4 { margin: 0 0 16px 0; color: #86868b; font-size: 13px; text-transform: uppercase; }
.filter-item {
  padding: 12px 16px; border-radius: 8px; margin-bottom: 8px; cursor: pointer;
  color: #1d1d1f; font-weight: 500; transition: all 0.2s;
}
.filter-item:hover { background: #f5f5f7; }
.filter-item.active { background: #e6f2ff; color: #0071e3; }

/* 右侧题目池 */
.ws-question-pool { flex: 1; padding: 24px 40px; overflow-y: auto; background: #f4f6fb; }
.pool-header { margin-bottom: 20px; font-size: 14px; color: #86868b; }
.wq-card {
  background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px;
  border: 2px solid transparent; cursor: pointer; transition: all 0.2s; box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
.wq-card:hover { border-color: #d1d1d6; }
.wq-card.selected { border-color: #ff9500; background: #fffaf0; box-shadow: 0 4px 12px rgba(255, 149, 0, 0.1); }
.wq-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.check-ring { width: 22px; height: 22px; border-radius: 50%; border: 2px solid #c7c7cc; transition: all 0.2s; }
.wq-card.selected .check-ring { border-color: #ff9500; background: #ff9500; box-shadow: inset 0 0 0 3px #fffaf0; }
.wq-content { font-size: 15px; color: #1d1d1f; line-height: 1.6; }
.wq-options { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.opt-item { background: #f5f5f7; padding: 8px 12px; border-radius: 6px; font-size: 14px; color: #424245; }

/* 预览模式样式 */
.preview-body { justify-content: center; overflow-y: auto; background: #e5e5ea; padding: 40px; }
.preview-paper-container { background: #fff; width: 800px; min-height: 1000px; padding: 60px 80px; box-shadow: 0 10px 40px rgba(0,0,0,0.1); border-radius: 8px; }
.preview-title { text-align: center; font-size: 28px; font-weight: bold; margin-bottom: 10px; }
.preview-meta { text-align: center; color: #86868b; margin-bottom: 30px; }
.preview-q-item { margin-bottom: 32px; }
.pq-header { font-size: 16px; color: #1d1d1f; margin-bottom: 16px; line-height: 1.5; }
.pq-options { display: flex; flex-direction: column; gap: 10px; padding-left: 20px; }
.pq-opt { font-size: 15px; }
.pq-blank { margin-top: 16px; }
.pq-coding .fake-editor { background: #282c34; color: #abb2bf; padding: 16px; border-radius: 8px; font-family: monospace; margin-top: 16px; min-height: 100px; }
.empty-preview { text-align: center; color: #86868b; padding: 100px 0; font-size: 16px; }
</style>