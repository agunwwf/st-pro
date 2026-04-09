<template>
  <div class="my-exams-container">

    <div class="premium-hero">
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <div class="hero-left">
          <el-tooltip content="返回学习大厅" placement="bottom">
            <el-button
                :icon="ArrowLeft"
                circle
                class="back-btn-dark"
                @click="router.push('/dashboard')"
            />
          </el-tooltip>
          <div class="hero-text">
            <h1 class="hero-title">我的测验任务</h1>
            <p class="hero-subtitle">请在规定时间内完成导师下发的测验。系统已开启防作弊监控，请独立作答。</p>
          </div>
        </div>

        <div class="hero-right">
          <div class="teacher-picker">
            <span class="tp-label">当前导师</span>
            <el-select
              v-model="selectedTeacherId"
              placeholder="请选择导师"
              size="large"
              style="width: 220px"
              :loading="loadingTeachers"
              @change="handleTeacherChange"
            >
              <el-option v-for="t in teacherOptions" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
            </el-select>
          </div>
          <div v-if="teacherHint" class="teacher-hint">{{ teacherHint }}</div>
        </div>
      </div>
    </div>

    <div class="main-content">
      <el-tabs v-model="activeTab" class="premium-tabs">

        <el-tab-pane name="pending">
          <template #label>
            <span class="tab-label"><el-icon><Document /></el-icon> 待完成测验</span>
          </template>

          <div class="grid-container" v-if="pendingExams.length > 0">
            <div class="task-card" v-for="exam in pendingExams" :key="exam.id">
              <div class="card-accent-strip pending-strip"></div>

              <div class="card-body">
                <div class="card-header">
                  <span class="status-tag tag-pending">未作答</span>
                  <span class="time-limit"><el-icon><Timer /></el-icon> {{ exam.timeLimitMinutes }} 分钟</span>
                </div>

                <h3 class="exam-title">{{ exam.publishName }}</h3>

                <div class="exam-meta-list">
                  <div class="meta-item danger">
                    <el-icon><Calendar /></el-icon>
                    <span>截止: {{ formatDateTime(exam.endTime) }}</span>
                  </div>
                </div>

                <div class="card-divider"></div>

                <div class="card-footer">
                  <el-button type="primary" class="action-btn start-btn" @click="startExam(exam.assignmentId || exam.id)">
                    开始作答 <el-icon class="btn-icon-right"><Position /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="empty-state" v-else>
            <div class="empty-icon-wrapper"><el-icon><CircleCheckFilled /></el-icon></div>
            <h3>全部任务已清空</h3>
            <p>太棒了！您当前没有待完成的测验任务。</p>
          </div>
        </el-tab-pane>

        <el-tab-pane name="completed">
          <template #label>
            <span class="tab-label"><el-icon><DataLine /></el-icon> 历史成绩单</span>
          </template>

          <div class="grid-container" v-if="completedExams.length > 0">
            <div class="task-card completed-card" v-for="exam in completedExams" :key="exam.id">
              <div class="card-accent-strip success-strip"></div>

              <div class="card-body">
                <div class="card-header">
                  <span class="status-tag tag-success">已交卷</span>
                </div>

                <h3 class="exam-title">{{ exam.publishName }}</h3>

                <div class="exam-meta-list">
                  <div class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    <span>交卷: {{ formatDateTime(exam.submitTime) }}</span>
                  </div>
                </div>

                <div class="score-display">
                  <div v-if="exam.score !== null">
                    <span class="score-number">{{ exam.score }}</span>
                    <span class="score-unit">分</span>
                  </div>
                  <div v-else class="score-pending">
                    <el-icon class="is-loading"><Loading /></el-icon> 教师批阅中...
                  </div>
                </div>

                <div class="card-divider"></div>

                <div class="card-footer">
                  <el-button plain class="action-btn report-btn" @click="viewReport(exam)">
                    查看成绩分析 <el-icon class="btn-icon-right"><Right /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div class="empty-state" v-else>
            <div class="empty-icon-wrapper muted"><el-icon><DocumentDelete /></el-icon></div>
            <h3>暂无历史记录</h3>
            <p>您还没有完成过任何测验。</p>
          </div>
        </el-tab-pane>

      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 引入全套高级专业图标
import {
  Timer, Calendar, ArrowLeft, Document, CircleCheckFilled,
  DataLine, Position, Right, Loading, DocumentDelete
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('pending')

const pendingExams = ref([])
const completedExams = ref([])

const teacherOptions = ref([])
const selectedTeacherId = ref(null)
const loadingTeachers = ref(false)
const teacherHint = ref('')

const getStudentId = () => {
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  return user.id
}

onMounted(async () => {
  const studentId = getStudentId()
  if (!studentId) return ElMessage.error("未获取到登录信息")

  try {
    await Promise.all([loadTeacherOptions(), loadCurrentTeacher()])
    await loadExamList()
  } catch (e) {
    ElMessage.error("获取测验列表失败")
  }
})

const loadTeacherOptions = async () => {
  loadingTeachers.value = true
  try {
    const res = await request.get('/api/teacher/list-teachers')
    if (res.data.code === 200) teacherOptions.value = res.data.data || []
  } finally {
    loadingTeachers.value = false
  }
}

const loadCurrentTeacher = async () => {
  try {
    const res = await request.get('/api/student/teacher/current')
    if (res.data.code === 200) selectedTeacherId.value = res.data.data?.teacherId || null
  } catch (e) {}
}

const loadExamList = async () => {
  // A 方案：老师只影响测验列表；后端会校验 teacherId 必须等于当前绑定导师
  const res = await request.get('/api/student/exam/list', { params: { teacherId: selectedTeacherId.value || undefined } })
  if (res.data.code === 200) {
    const allExams = res.data.data || []
    pendingExams.value = allExams.filter(exam => exam.status == null || exam.status === 0)
    completedExams.value = allExams.filter(exam => exam.status === 1 || exam.status === 2)
  } else {
    ElMessage.error(res.data.msg || '获取测验列表失败')
  }
}

const handleTeacherChange = async (nextId) => {
  teacherHint.value = ''
  if (!nextId) return
  try {
    const curRes = await request.get('/api/student/teacher/current')
    const curTeacherId = curRes.data?.data?.teacherId || null

    if (!curTeacherId) {
      const r = await request.post('/api/student/teacher/request-bind', { teacherId: nextId })
      if (r.data.code === 200) {
        teacherHint.value = '已发起入班申请，等待导师同意后生效'
      } else {
        teacherHint.value = r.data.msg || '申请失败'
      }
      await loadCurrentTeacher()
      await loadExamList()
      return
    }

    if (curTeacherId !== nextId) {
      const r = await request.post('/api/student/teacher/request-switch', { newTeacherId: nextId })
      if (r.data.code === 200) {
        teacherHint.value = '已发起换导师申请，需原导师与新导师都同意后生效'
      } else {
        teacherHint.value = r.data.msg || '申请失败'
      }
      await loadCurrentTeacher()
      await loadExamList()
    }
  } catch (e) {
    teacherHint.value = '操作失败，请稍后重试'
  }
}

const formatDateTime = (v) => {
  if (!v) return '-'
  const s = String(v).trim()
  const normalized = s.replace('T', ' ').replace(/\.\d+$/, '')
  return normalized.length >= 16 ? normalized.substring(0, 16) : normalized
}

const startExam = (assignmentId) => {
  router.push(`/exam/${assignmentId}`)
}

const viewReport = (exam) => {
  if (!exam || exam.score == null) return ElMessage.warning('成绩尚未出炉，暂无法查看分析')
  const assignmentId = exam.assignmentId || exam.id
  router.push(`/exam-report/${assignmentId}`)
}
</script>

<style scoped>
/* 基础布局 */
.my-exams-container {
  min-height: 100vh;
  background-color: #f4f6fb; /* 专业后台常用的极浅灰蓝底色 */
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.4s ease;
}

/* ================= 沉浸式高级头图 ================= */
.premium-hero {
  position: relative;
  height: 220px;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%); /* 经典商业深蓝渐变 */
  display: flex;
  align-items: center;
  padding: 0 60px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

/* 增加科技感网格纹理叠加层 */
.hero-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image:
      linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 30px 30px;
  opacity: 0.5;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 10;
  display: flex;
  align-items: flex-end; /* 让右侧“当前导师”与左侧文案底部对齐 */
  gap: 24px;
  width: 100%;
}

.hero-left {
  display: flex;
  align-items: flex-start; /* 返回按钮保持在左侧上方 */
  gap: 24px;
  flex: 1;
  min-width: 0;
}

.back-btn-dark { align-self: flex-start; }

.hero-right {
  margin-left: auto;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  min-width: 260px;
}

.teacher-picker {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tp-label {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.teacher-hint {
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  max-width: 320px;
  text-align: right;
  line-height: 1.4;
}

.back-btn-dark {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 20px;
  width: 44px; height: 44px;
  transition: all 0.3s;
}
.back-btn-dark:hover {
  background: #fff;
  color: #1e3c72;
  transform: scale(1.05);
}

.hero-text { color: #fff; }
.hero-title { margin: 0 0 12px 0; font-size: 32px; font-weight: 600; letter-spacing: 1px; }
.hero-subtitle { margin: 0; font-size: 15px; color: rgba(255, 255, 255, 0.8); line-height: 1.6; max-width: 600px; }

/* ================= 内容区与高级 Tabs ================= */
.main-content {
  padding: 30px 60px;
  max-width: 1400px;
  margin: -40px auto 0 auto; /* 负边距实现内容区向上悬浮叠加效果 */
  width: 100%;
  position: relative;
  z-index: 20;
}

:deep(.premium-tabs > .el-tabs__header) {
  background: #fff;
  padding: 0 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.03);
  margin-bottom: 24px;
  border-bottom: none;
}
:deep(.premium-tabs .el-tabs__nav-wrap::after) { display: none; /* 去掉底部灰线 */ }
:deep(.premium-tabs .el-tabs__item) {
  height: 60px;
  line-height: 60px;
  font-size: 16px;
  color: #5c6b77;
  font-weight: 500;
}
:deep(.premium-tabs .el-tabs__item.is-active) { color: #2a5298; font-weight: 600; }
:deep(.premium-tabs .el-tabs__active-bar) { background-color: #2a5298; height: 3px; border-radius: 3px; }

.tab-label { display: flex; align-items: center; gap: 8px; }

/* ================= 企业级任务卡片 ================= */
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.task-card {
  background: #fff;
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  border: 1px solid #eef0f5;
  box-shadow: 0 2px 8px rgba(28, 39, 49, 0.04);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  display: flex;
  flex-direction: column;
}
.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(28, 39, 49, 0.08);
  border-color: #d1d9e6;
}

/* 左侧状态强调条 */
.card-accent-strip { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }
.pending-strip { background-color: #faad14; }
.success-strip { background-color: #52c41a; }

.card-body { padding: 24px; display: flex; flex-direction: column; flex: 1; }

.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.status-tag { padding: 4px 12px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.tag-pending { background: #fffbe6; color: #faad14; border: 1px solid #ffe58f; }
.tag-success { background: #f6ffed; color: #52c41a; border: 1px solid #b7eb8f; }

.time-limit { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #5c6b77; font-weight: 500; }

.exam-title { margin: 0 0 16px 0; font-size: 18px; color: #1c242c; font-weight: 600; line-height: 1.4; }

.exam-meta-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; flex: 1; }
.meta-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #5c6b77; }
.meta-item.danger { color: #f5222d; font-weight: 500; }

/* 成绩展示区 */
.score-display { background: #f8fafc; border-radius: 8px; padding: 16px; text-align: center; margin-bottom: 20px; border: 1px solid #eef0f5; }
.score-number { font-size: 32px; font-weight: 700; color: #2a5298; font-family: 'Helvetica Neue', Arial, sans-serif; }
.score-unit { font-size: 14px; color: #5c6b77; margin-left: 4px; }
.score-pending { color: #faad14; font-size: 14px; font-weight: 500; display: flex; align-items: center; justify-content: center; gap: 8px; }

.card-divider { height: 1px; background: #eef0f5; margin: 0 -24px 20px -24px; }

/* 按钮区 */
.card-footer { margin-top: auto; }
.action-btn { width: 100%; height: 44px; border-radius: 6px; font-size: 15px; font-weight: 600; display: flex; justify-content: center; align-items: center; }
.start-btn { background: #2a5298; border-color: #2a5298; }
.start-btn:hover { background: #1e3c72; border-color: #1e3c72; }
.btn-icon-right { margin-left: 8px; font-size: 16px; }

/* ================= 空状态设计 ================= */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; }
.empty-icon-wrapper { width: 80px; height: 80px; border-radius: 50%; background: #eafbf0; color: #52c41a; display: flex; align-items: center; justify-content: center; font-size: 40px; margin-bottom: 20px; }
.empty-icon-wrapper.muted { background: #f4f6fb; color: #a0aec0; }
.empty-state h3 { margin: 0 0 8px 0; color: #1c242c; font-size: 20px; }
.empty-state p { margin: 0; color: #5c6b77; font-size: 15px; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(15px); } to { opacity: 1; transform: translateY(0); } }
</style>