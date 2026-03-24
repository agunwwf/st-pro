<template>
  <div class="management-container">
    <div class="dashboard-header">
      <div class="header-left">
        <h2>学生管理系统</h2>
        <p>管理学生信息、学习进度及打卡情况</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="showAddStudent = true">添加学生</el-button>
        <el-button type="success" :icon="ChatLineRound" @click="showCreateGroup = true">创建群聊</el-button>
      </div>
    </div>

    <div class="dashboard-content glass-card">
      <el-table :data="students" style="width: 100%" class="student-table" v-loading="loading">
        <el-table-column label="学生" width="250">
          <template #default="scope">
            <div class="student-cell">
              <el-avatar :size="44" :src="scope.row.avatar" shape="square" />
              <div class="student-meta">
                <span class="student-name">{{ scope.row.nickname }}</span>
                <span class="student-id">ID: {{ scope.row.username }}</span>
              </div>
              <el-tag v-if="scope.row.isModel" size="small" type="warning" effect="dark" class="model-tag">模范</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="学习进度">
          <template #default="scope">
            <div class="progress-cell">
              <el-progress :percentage="scope.row.progress" :color="getProgressColor(scope.row.progress)" :stroke-width="10" />
            </div>
          </template>
        </el-table-column>

        <el-table-column label="打卡天数" prop="checkInCount" width="120" align="center">
          <template #default="scope">
            <span class="check-in-text">{{ scope.row.checkInCount }} 天</span>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" width="180">
          <template #default="scope">
            <span class="date-text">{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="action-btns">
              <el-tooltip :content="scope.row.isModel ? '取消模范' : '设为模范'" placement="top">
                <el-button circle :type="scope.row.isModel ? 'warning' : 'info'" :icon="StarFilled" @click="toggleModel(scope.row)" />
              </el-tooltip>
              <el-tooltip content="发送消息" placement="top">
                <el-button circle type="primary" :icon="ChatDotRound" @click="messageStudent(scope.row)" />
              </el-tooltip>
              <el-button type="danger" :icon="Delete" plain @click="confirmDelete(scope.row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 添加学生弹窗：根据用户名添加已注册学生 -->
    <el-dialog v-model="showAddStudent" title="添加学生" width="400px">
      <el-form label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="addForm.username" placeholder="请输入已注册学生的用户名（如 test1）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStudent = false">取消</el-button>
        <el-button type="primary" @click="handleAddStudent">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ChatLineRound, StarFilled, ChatDotRound, Delete } from '@element-plus/icons-vue'
import request from '@/utils/request'
window.axios = request

const router = useRouter()
const students = ref([])
const loading = ref(false)
const showAddStudent = ref(false)
const showCreateGroup = ref(false)
const addForm = ref({
  username: ''
})

const loadStudents = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/api/user/students')
    if (res.data.code === 200) {
      students.value = res.data.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取学生列表失败')
  } finally {
    loading.value = false
  }
}

const getProgressColor = (p) => {
  if (p < 30) return '#ff4d4f'
  if (p < 70) return '#faad14'
  return '#34c759'
}

const formatDate = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString()
}

const handleAddStudent = async () => {
  if (!addForm.value.username) {
    return ElMessage.warning('请输入要添加的学生用户名')
  }
  try {
    // 只允许添加“已经注册过的学生”
    const res = await axios.get(`http://localhost:8080/api/user/search?keyword=${addForm.value.username}`)
    if (res.data.code !== 200 || !Array.isArray(res.data.data) || res.data.data.length === 0) {
      return ElMessage.error('该学生不存在，请先让学生完成注册')
    }

    // 查找精确用户名匹配且角色为 STUDENT 的用户
    const target = res.data.data.find(u => u.username === addForm.value.username && (u.role === 'STUDENT' || !u.role))
    if (!target) {
      return ElMessage.error('只能添加已注册的学生账号')
    }

    ElMessage.success(`已添加学生：${target.nickname || target.username}`)
    showAddStudent.value = false
    addForm.value.username = ''
    // 学生列表本身就是所有学生，这里刷新一下以防有新注册学生
    loadStudents()
  } catch (e) {
    console.error(e)
    ElMessage.error('查询学生失败')
  }
}

const toggleModel = async (student) => {
  try {
    const res = await axios.post('http://localhost:8080/api/user/student/model', {
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

const confirmDelete = (student) => {
  ElMessageBox.confirm(
      `确定要永久删除学生 ${student.nickname} (ID: ${student.username}) 吗？此操作不可撤销。`,
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
  ).then(async () => {
    const res = await axios.delete(`http://localhost:8080/api/user/student/${student.id}`)
    if (res.data.code === 200) {
      ElMessage.success('已删除学生')
      loadStudents()
    }
  }).catch(() => {})
}

const messageStudent = (student) => {
  // 跳转到聊天页面并携带参数
  router.push({
    path: '/chat',
    query: { target: student.username }
  })
}

onMounted(() => {
  loadStudents()
})
</script>

<style scoped lang="scss">
.management-container {
  padding: 32px;
  animation: fadeIn 0.5s ease;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 32px;

  .header-left {
    h2 { font-size: 32px; font-weight: 700; margin: 0 0 8px 0; color: var(--apple-text-primary); }
    p { font-size: 16px; color: var(--apple-text-secondary); margin: 0; }
  }
}

.dashboard-content {
  padding: 24px;
  background: var(--apple-card-bg);
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 12px;

  .student-meta {
    display: flex;
    flex-direction: column;
    .student-name { font-weight: 600; color: var(--apple-text-primary); font-size: 16px; }
    .student-id { font-size: 12px; color: var(--apple-text-secondary); }
  }

  .model-tag { margin-left: 8px; border-radius: 6px; }
}

.progress-cell {
  padding-right: 40px;
}

.check-in-text {
  font-weight: 600;
  color: var(--apple-blue);
}

.date-text {
  color: var(--apple-text-secondary);
  font-size: 14px;
}

.action-btns {
  display: flex;
  gap: 8px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
```

