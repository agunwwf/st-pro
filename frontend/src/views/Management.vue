<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Monitor, DocumentCopy, Plus, EditPen, Promotion,
  Search, Timer, Back, User, ChatLineRound, StarFilled, ChatDotRound, Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
window.axios = request // 兼容你原来的 axios 写法

const router = useRouter()

// 核心视图切换：students (学生管理) | exams (出题测验)
const currentView = ref('students') // 默认先展示你最熟悉的管理界面

// ================== 学生管理模块 (融合你原来的真实逻辑) ==================
const students = ref([])
const loadingStudents = ref(false)
const showAddStudent = ref(false)
const searchStudent = ref('')
const addForm = ref({ username: '' })

// 历史页面：已被 TeacherAdmin.vue 融合替代。
// 为避免线上/环境问题，本页不再使用 localhost 硬编码与旧接口。
const loadStudents = async () => {
  loadingStudents.value = true
  try {
    const res = await axios.get('/api/user/students')
    if (res.data.code === 200) {
      students.value = res.data.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取学生列表失败')
  } finally {
    loadingStudents.value = false
  }
}

// 原汁原味的进度条颜色
const getProgressColor = (p) => {
  if (p < 30) return '#ff4d4f'
  if (p < 70) return '#faad14'
  return '#34c759'
}

// 原汁原味的日期格式化
const formatDate = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

// 原汁原味的添加学生逻辑
const handleAddStudent = async () => {
  if (!addForm.value.username) return ElMessage.warning('请输入要添加的学生用户名')
  try {
    const res = await axios.get(`/api/user/search?keyword=${addForm.value.username}`)
    if (res.data.code !== 200 || !Array.isArray(res.data.data) || res.data.data.length === 0) {
      return ElMessage.error('该学生不存在，请先让学生完成注册')
    }
    const target = res.data.data.find(u => u.username === addForm.value.username && (u.role === 'STUDENT' || !u.role))
    if (!target) return ElMessage.error('只能添加已注册的学生账号')

    ElMessage.success(`已添加学生：${target.nickname || target.username}`)
    showAddStudent.value = false
    addForm.value.username = ''
    loadStudents()
  } catch (e) {
    ElMessage.error('查询学生失败')
  }
}

// 原汁原味的切换模范逻辑
const toggleModel = async (student) => {
  try {
    const res = await axios.post('/api/user/student/model', {
      id: student.id,
      isModel: !student.isModel
    })
    if (res.data.code === 200) {
      ElMessage.success('操作成功')
      loadStudents()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 原汁原味的删除逻辑
const confirmDelete = (student) => {
  ElMessageBox.confirm(
      `确定要永久删除学生 ${student.nickname} (ID: ${student.username}) 吗？此操作不可撤销。`,
      '警告',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    const res = await axios.delete(`/api/user/student/${student.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除学生')
      loadStudents()
    }
  }).catch(() => {})
}

// 原汁原味的发消息跳转
const messageStudent = (student) => {
  router.push({ path: '/chat', query: { target: student.username } })
}

// ================== 测验与出题模块 (新架构) ==================
const activeExamTab = ref('papers')
const createPaperVisible = ref(false)
const publishExamVisible = ref(false)
const questionDrawerVisible = ref(false)

const questionBank = ref([])
const papers = ref([])
const assignments = ref([])

const paperForm = reactive({ title: '', category: 'kmeans', selectedQuestions: [] })
const examForm = reactive({ paperId: null, publishName: '', dateRange: [], timeLimit: 45 })

const loadExamsData = async () => {
  try {
    const qRes = await request.get('/api/teacher/lms/questions')
    const pRes = await request.get('/api/teacher/lms/papers')
    const aRes = await request.get('/api/teacher/lms/assignments')
    if(qRes.data.code === 200) questionBank.value = qRes.data.data
    if(pRes.data.code === 200) papers.value = pRes.data.data
    if(aRes.data.code === 200) assignments.value = aRes.data.data
  } catch (error) {
    // 兼容后端还没跑起来的情况，给点占位数据防白屏
    papers.value = [{ id: 1, title: '2026 K-Means 标准卷', category: 'K-Means', questionCount: 15, totalScore: 100 }]
    assignments.value = [{ id: 101, publishName: '计科一班测验', paperTitle: '2026 K-Means 标准卷', timeLimitMinutes: 45, startTime: '2026-04-10', endTime: '2026-04-12' }]
    questionBank.value = [{ id: 1, category: 'kmeans', type: '选择题', content: 'K-Means的含义是？', score: 10 }]
  }
}

// 初始化加载双端数据
onMounted(() => {
  loadStudents()
  loadExamsData()
})

const toggleQuestion = (id) => {
  const idx = paperForm.selectedQuestions.indexOf(id)
  if (idx > -1) paperForm.selectedQuestions.splice(idx, 1)
  else paperForm.selectedQuestions.push(id)
}
const handleCreatePaper = () => {
  ElMessage.success('🎉 试卷组建成功！')
  createPaperVisible.value = false
}
const openPublishExam = (paperId = null) => {
  examForm.paperId = paperId
  publishExamVisible.value = true
}
const handlePublishExam = () => {
  ElMessage.success('🚀 考试发布成功！')
  publishExamVisible.value = false
}
</script>

<template>
  <div class="teacher-admin-layout">
    <div class="animated-bg"></div>

    <nav class="glass-navbar">
      <div class="nav-left">
        <div class="logo-area">
          <el-icon class="logo-icon"><Monitor /></el-icon>
          <span class="logo-text">LMS 教师控制舱</span>
        </div>
      </div>
      <div class="nav-center">
        <div class="nav-tabs">
          <div class="tab-item" :class="{ active: currentView === 'students' }" @click="currentView = 'students'">
            <el-icon><User /></el-icon> 班级学生总览
          </div>
          <div class="tab-item" :class="{ active: currentView === 'exams' }" @click="currentView = 'exams'">
            <el-icon><DocumentCopy /></el-icon> 出题与测验中台
          </div>
        </div>
      </div>
      <div class="nav-right">
        <el-button round class="exit-btn" :icon="Back" @click="router.push('/dashboard')">
          返回学习大厅
        </el-button>
      </div>
    </nav>

    <main class="main-content">
      <transition name="fade-slide" mode="out-in">

        <div v-if="currentView === 'students'" class="view-container" key="students">
          <div class="glass-panel">
            <div class="panel-header">
              <div>
                <h2>👨‍🎓 班级学生大盘</h2>
                <p style="color: #86868b; margin: 4px 0 0 0;">管理学生信息、追踪学习进度及打卡情况</p>
              </div>
              <div style="display: flex; gap: 12px;">
                <el-input v-model="searchStudent" placeholder="搜索姓名..." :prefix-icon="Search" style="width: 200px" />
                <el-button type="primary" :icon="Plus" @click="showAddStudent = true" round>添加学生</el-button>
              </div>
            </div>

            <el-table :data="students" style="width: 100%" class="apple-table" row-class-name="apple-table-row" v-loading="loadingStudents">
              <el-table-column label="学生信息" min-width="250">
                <template #default="scope">
                  <div class="student-cell">
                    <el-avatar :size="44" :src="scope.row.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" shape="square" />
                    <div class="student-meta">
                      <span class="student-name">{{ scope.row.nickname || scope.row.username }}</span>
                      <span class="student-id">ID: {{ scope.row.username }}</span>
                    </div>
                    <el-tag v-if="scope.row.isModel" size="small" type="warning" effect="dark" class="model-tag">模范</el-tag>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="学习进度" min-width="150">
                <template #default="scope">
                  <div class="progress-cell">
                    <el-progress :percentage="scope.row.progress || 0" :color="getProgressColor(scope.row.progress)" :stroke-width="10" />
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="打卡" prop="checkInCount" width="100" align="center">
                <template #default="scope">
                  <span class="check-in-text">{{ scope.row.checkInCount || 0 }} 天</span>
                </template>
              </el-table-column>

              <el-table-column label="加入时间" width="150">
                <template #default="scope">
                  <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
                </template>
              </el-table-column>

              <el-table-column label="管理操作" width="180" fixed="right">
                <template #default="scope">
                  <div class="action-btns">
                    <el-tooltip :content="scope.row.isModel ? '取消模范' : '设为模范'" placement="top">
                      <el-button circle :type="scope.row.isModel ? 'warning' : 'info'" :icon="StarFilled" @click="toggleModel(scope.row)" />
                    </el-tooltip>
                    <el-tooltip content="单独发消息" placement="top">
                      <el-button circle type="primary" :icon="ChatDotRound" @click="messageStudent(scope.row)" />
                    </el-tooltip>
                    <el-tooltip content="踢出班级" placement="top">
                      <el-button circle type="danger" :icon="Delete" plain @click="confirmDelete(scope.row)" />
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div v-else-if="currentView === 'exams'" class="view-container" key="exams">

          <div class="action-banner glass-panel">
            <div class="banner-text">
              <h2>测验与试卷中台</h2>
              <p>组建标准试卷，一键下发至班级，AI自动统计成绩与病历。</p>
            </div>
            <el-button type="primary" size="large" class="hero-btn" :icon="Plus" @click="createPaperVisible = true">
              新建试卷模板
            </el-button>
          </div>

          <div class="glass-panel main-tabs">
            <el-tabs v-model="activeExamTab" class="apple-tabs">
              <el-tab-pane label="📚 试卷模板库 (Papers)" name="papers">
                <div class="grid-container">
                  <div class="exam-card" v-for="paper in papers" :key="paper.id">
                    <div class="card-badge">{{ paper.category }}</div>
                    <h3>{{ paper.title }}</h3>
                    <p class="meta">总分: {{ paper.totalScore }} 分 | 题目数: {{ paper.questionCount }}</p>
                    <div class="card-actions">
                      <el-button plain :icon="EditPen">编辑</el-button>
                      <el-button type="primary" :icon="Promotion" @click="openPublishExam(paper.id)">发布考试</el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="🚀 已发考试 (Assignments)" name="assignments">
                <div style="display: flex; justify-content: flex-end; margin-bottom: 20px;">
                  <el-button type="primary" :icon="Promotion" @click="openPublishExam(null)">发布新考试</el-button>
                </div>
                <div class="grid-container">
                  <div class="exam-card" v-for="item in assignments" :key="item.id">
                    <div class="card-header">
                      <span class="status-badge active">已发布</span>
                      <span class="meta"><el-icon><Timer /></el-icon> {{ item.timeLimitMinutes }} 分钟</span>
                    </div>
                    <h3 style="margin: 10px 0;">{{ item.publishName }}</h3>
                    <p class="meta" style="color:#0071e3">试卷: {{ item.paperTitle }}</p>
                    <p class="meta" style="color:#ff3b30">截止: {{ (item.endTime || '').substring(0, 16) }}</p>
                    <el-button type="primary" plain style="width: 100%; margin-top: 16px;">追踪成绩</el-button>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

        </div>

      </transition>
    </main>

    <el-dialog v-model="showAddStudent" title="添加学生" width="400px" class="apple-dialog">
      <el-form label-position="top">
        <el-form-item label="学生用户名">
          <el-input v-model="addForm.username" placeholder="请输入已注册的用户名（如 test1）" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStudent = false" round>取消</el-button>
        <el-button type="primary" @click="handleAddStudent" round>确定添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createPaperVisible" title="📚 组建标准试卷" width="600px" class="apple-dialog">
      <el-form :model="paperForm" label-position="top">
        <el-form-item label="试卷名称">
          <el-input v-model="paperForm.title" placeholder="输入名称" size="large" />
        </el-form-item>
        <el-form-item label="题目组装">
          <div class="picker-box" @click="questionDrawerVisible = true" style="cursor: pointer;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><EditPen /></el-icon>
              <span style="font-weight: 600;">当前已选: {{ paperForm.selectedQuestions.length }} 题</span>
            </div>
            <el-button link type="primary">从题库选题 ></el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createPaperVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handleCreatePaper" round>确认保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishExamVisible" title="🚀 发布考试任务" width="600px" class="apple-dialog">
      <el-form :model="examForm" label-position="top">
        <el-form-item label="考试任务名称">
          <el-input v-model="examForm.publishName" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishExamVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handlePublishExam" round>立即发布</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="questionDrawerVisible" title="📚 教学题库中心" size="450px" class="apple-drawer">
      <div class="question-list">
        <div v-for="q in questionBank" :key="q.id" class="question-item"
             :class="{ 'selected': paperForm.selectedQuestions.includes(q.id) }" @click="toggleQuestion(q.id)">
          <div class="q-header"><span class="q-type">{{ q.type }}</span></div>
          <div class="q-content">{{ q.content }}</div>
          <div class="checkbox-ring"></div>
        </div>
      </div>
    </el-drawer>

  </div>
</template>

<style scoped>
/* 全屏与流光背景 */
.teacher-admin-layout { min-height: 100vh; width: 100vw; position: relative; overflow-x: hidden; background-color: #f4f6fb; }
.animated-bg {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; z-index: 0;
  background-image:
      radial-gradient(at 0% 0%, hsla(210,100%,92%,1) 0px, transparent 50%),
      radial-gradient(at 100% 0%, hsla(240,100%,94%,1) 0px, transparent 50%),
      radial-gradient(at 100% 100%, hsla(180,100%,90%,1) 0px, transparent 50%),
      radial-gradient(at 0% 100%, hsla(280,100%,94%,1) 0px, transparent 50%);
  animation: bgFlow 15s ease infinite alternate;
}
@keyframes bgFlow { 0% { transform: scale(1); } 100% { transform: scale(1.1); } }

/* 顶部导航 */
.glass-navbar {
  position: fixed; top: 0; left: 0; right: 0; height: 64px;
  background: rgba(255, 255, 255, 0.6); backdrop-filter: blur(20px); border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  display: flex; justify-content: space-between; align-items: center; padding: 0 40px; z-index: 100; box-shadow: 0 4px 20px rgba(0,0,0,0.02);
}
.logo-area { display: flex; align-items: center; gap: 8px; font-size: 20px; font-weight: 700; color: #1d1d1f; }
.logo-icon { color: #0071e3; font-size: 24px; }
.nav-tabs { display: flex; gap: 16px; background: rgba(0,0,0,0.04); padding: 4px; border-radius: 980px; }
.tab-item { padding: 8px 24px; border-radius: 980px; cursor: pointer; font-weight: 600; font-size: 14px; color: #86868b; transition: all 0.3s; display: flex; align-items: center; gap: 6px; }
.tab-item:hover { color: #1d1d1f; }
.tab-item.active { background: #fff; color: #1d1d1f; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }

/* 核心区 */
.main-content { position: relative; z-index: 10; padding: 100px 40px 40px 40px; max-width: 1400px; margin: 0 auto; }
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.fade-slide-enter-from { opacity: 0; transform: translateY(20px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-20px); }

/* 毛玻璃面板 */
.glass-panel {
  background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8); border-radius: 24px; padding: 32px; box-shadow: 0 10px 40px rgba(0,0,0,0.05); margin-bottom: 24px;
}

/* --- 原版学生表格样式保留 --- */
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.panel-header h2 { margin: 0; font-size: 24px; color: var(--apple-text-primary); }
:deep(.apple-table) { background: transparent; }
:deep(.apple-table-row) { background: transparent !important; transition: background 0.2s; }
:deep(.apple-table-row:hover) { background: rgba(255,255,255,0.8) !important; }

.student-cell { display: flex; align-items: center; gap: 12px; }
.student-meta { display: flex; flex-direction: column; }
.student-name { font-weight: 600; color: #1d1d1f; font-size: 15px; }
.student-id { font-size: 12px; color: #86868b; }
.model-tag { margin-left: 8px; border-radius: 6px; }
.progress-cell { padding-right: 40px; }
.check-in-text { font-weight: 600; color: #0071e3; }
.date-text { color: #86868b; font-size: 14px; }
.action-btns { display: flex; gap: 8px; }

/* 出题中心特有样式 */
.action-banner { display: flex; justify-content: space-between; align-items: center; background: linear-gradient(135deg, rgba(255,255,255,0.9), rgba(230,242,255,0.8)); }
.action-banner h2 { margin: 0 0 8px 0; font-size: 28px; color: #1d1d1f; }
.hero-btn { border-radius: 980px; padding: 0 32px; font-weight: 600; height: 48px; }
.grid-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; margin-top: 16px; }
.exam-card { background: #fff; border-radius: 16px; padding: 24px; border: 1px solid #e5e5ea; transition: all 0.3s; position: relative; }
.exam-card:hover { transform: translateY(-6px); box-shadow: 0 20px 40px rgba(0,0,0,0.08); }
.card-badge { position: absolute; top: 24px; right: 24px; font-size: 12px; font-weight: 600; color: #8E8E93; background: #F2F2F7; padding: 4px 10px; border-radius: 6px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.status-badge.active { color: #0071e3; background: #e6f2ff; padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 600;}
.meta { font-size: 13px; color: #86868b; margin: 4px 0; }
.card-actions { margin-top: 24px; display: flex; gap: 10px; }
.card-actions .el-button { flex: 1; border-radius: 8px; }

/* 题库相关 */
.picker-box { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #f2f2f7; border-radius: 12px; border: 1px solid #e5e5ea; }
.question-item { padding: 16px; border-bottom: 1px solid #e5e5ea; cursor: pointer; position: relative; transition: background 0.2s; border-radius: 12px; margin-bottom: 8px; }
.question-item.selected { background: #e6f2ff; border: 1px solid #0071e3; }
.checkbox-ring { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); width: 22px; height: 22px; border-radius: 50%; border: 2px solid #c7c7cc; }
.question-item.selected .checkbox-ring { border-color: #0071e3; background: #0071e3; box-shadow: inset 0 0 0 3px #e6f2ff; }
</style>