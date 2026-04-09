<template>
  <div class="analytics-page">
    <header class="top-bar">
      <el-button :icon="ArrowLeft" circle @click="goBack" />
      <div class="titles">
        <h1>{{ pageTitle }}</h1>
        <p v-if="assignmentMeta" class="sub">
          试卷：{{ assignmentMeta.paperTitle || '—' }} · 截止 {{ formatDateTime(assignmentMeta.endTime) }}
        </p>
      </div>
    </header>

    <div v-loading="loading" class="body">
      <div v-if="blocked" class="blocked-wrap">
        <el-alert type="warning" :title="blockMsg" show-icon :closable="false" class="blocked-alert" />
        <el-button type="primary" class="blocked-back" @click="goBack">返回教师中心</el-button>
      </div>

      <template v-else-if="payload">
        <section class="summary-cards">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">班级人数</div>
            <div class="stat-value">{{ payload.summary?.classSize ?? 0 }}</div>
          </el-card>
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">已交卷</div>
            <div class="stat-value ok">{{ payload.summary?.submittedCount ?? 0 }}</div>
          </el-card>
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">未交卷</div>
            <div class="stat-value warn">{{ payload.summary?.notSubmittedCount ?? 0 }}</div>
          </el-card>
          <el-card shadow="hover" class="stat-card highlight">
            <div class="stat-label">平均得分</div>
            <div class="stat-value">{{ payload.summary?.averageScore ?? 0 }}</div>
          </el-card>
        </section>

        <section class="charts-row">
          <el-card class="chart-card" shadow="never">
            <div ref="pieRef" class="chart-box" />
          </el-card>
          <el-card class="chart-card wide" shadow="never">
            <div ref="barRef" class="chart-box" />
          </el-card>
        </section>

        <section class="table-block">
          <h2>学生成绩</h2>
          <el-table :data="payload.students || []" stripe border style="width: 100%">
            <el-table-column prop="nickname" label="姓名" width="140">
              <template #default="{ row }">{{ row.nickname || row.username }}</template>
            </el-table-column>
            <el-table-column prop="username" label="账号" width="140" />
            <el-table-column prop="score" label="得分" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="Number(row.status) >= 1" type="success" size="small">已交卷</el-tag>
                <el-tag v-else type="info" size="small">其他</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="交卷时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
            </el-table-column>
          </el-table>
          <p v-if="!payload.students?.length" class="muted">暂无答卷记录（截止后仍未交卷的学生不会出现在表中）。</p>
        </section>

        <section class="table-block">
          <h2>题目分析</h2>
          <el-table :data="payload.questions || []" border style="width: 100%">
            <el-table-column prop="index" label="#" width="56" />
            <el-table-column prop="type" label="题型" width="100" />
            <el-table-column label="题干" min-width="220">
              <template #default="{ row }">
                <span class="q-preview">{{ row.content }}</span>
              </template>
            </el-table-column>
            <el-table-column label="正确率" width="100">
              <template #default="{ row }">{{ row.correctRate }}%</template>
            </el-table-column>
            <el-table-column prop="correctCount" label="答对" width="72" />
            <el-table-column prop="wrongCount" label="答错" width="72" />
            <el-table-column prop="blankCount" label="未答" width="72" />
            <el-table-column prop="notSubmittedCount" label="整卷未交" width="100" />
          </el-table>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const blocked = ref(false)
const blockMsg = ref('')
const payload = ref(null)

const barRef = ref(null)
const pieRef = ref(null)
let chartBar = null
let chartPie = null

const assignmentMeta = computed(() => {
  const a = payload.value?.assignment || {}
  return {
    publishName: a.publishName,
    endTime: a.endTime,
    paperTitle: a.paperTitle
  }
})

const pageTitle = computed(() => {
  if (blocked.value) return '考试数据分析'
  const n = payload.value?.assignment?.publishName
  return n ? `「${n}」数据分析` : '考试数据分析'
})

const formatDateTime = (v) => {
  if (!v) return '-'
  const s = String(v).trim().replace('T', ' ').replace(/\.\d+$/, '')
  return s.length >= 16 ? s.substring(0, 16) : s
}

const goBack = () => router.push('/management')

const disposeCharts = () => {
  chartBar?.dispose()
  chartBar = null
  chartPie?.dispose()
  chartPie = null
}

const initCharts = () => {
  disposeCharts()
  const qs = payload.value?.questions || []
  const s = payload.value?.summary

  if (barRef.value && qs.length) {
    chartBar = echarts.init(barRef.value)
    chartBar.setOption({
      title: { text: '各题正确率', left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } },
      grid: { left: 48, right: 24, bottom: 56, top: 48 },
      tooltip: { trigger: 'axis', valueFormatter: (v) => `${v}%` },
      xAxis: { type: 'category', data: qs.map((_, i) => `第${i + 1}题`) },
      yAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
      series: [{
        type: 'bar',
        data: qs.map((q) => q.correctRate),
        itemStyle: { color: '#0071e3', borderRadius: [6, 6, 0, 0] },
        barMaxWidth: 44
      }]
    })
  }

  if (pieRef.value && s) {
    chartPie = echarts.init(pieRef.value)
    chartPie.setOption({
      title: { text: '交卷情况', left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['42%', '72%'],
        itemStyle: { borderRadius: 6 },
        label: { formatter: '{b}: {c}' },
        data: [
          { name: '已交卷', value: s.submittedCount || 0 },
          { name: '未交卷', value: s.notSubmittedCount || 0 }
        ]
      }]
    })
  }
}

const onResize = () => {
  chartBar?.resize()
  chartPie?.resize()
}

const load = async () => {
  loading.value = true
  blocked.value = false
  blockMsg.value = ''
  payload.value = null
  try {
    const id = route.params.id
    const res = await request.get(`/api/teacher/lms/assignment/${id}/analytics`)
    if (res.data?.code === 200) {
      payload.value = res.data.data
      await nextTick()
      initCharts()
    } else {
      blocked.value = true
      blockMsg.value = res.data?.msg || '无法加载分析数据'
    }
  } catch (e) {
    blocked.value = true
    blockMsg.value = '网络异常，加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<style scoped>
.analytics-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f4ff 0%, #f4f6fb 40%, #fff 100%);
  padding: 24px 32px 48px;
}

.top-bar {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.titles h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #1d1d1f;
}

.sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #86868b;
}

.body {
  max-width: 1200px;
  margin: 0 auto;
}

.blocked-wrap {
  max-width: 720px;
}

.blocked-alert {
  margin-bottom: 16px;
}

.blocked-back {
  border-radius: 10px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

@media (max-width: 900px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
}

.stat-card {
  border-radius: 14px;
  border: 1px solid #e5e5ea;
}

.stat-card.highlight {
  border-color: #b3d7ff;
  background: linear-gradient(135deg, #f5f9ff 0%, #fff 100%);
}

.stat-label {
  font-size: 12px;
  color: #86868b;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1d1d1f;
}

.stat-value.ok { color: #34c759; }
.stat-value.warn { color: #ff9500; }

.charts-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 16px;
  margin-bottom: 28px;
}

@media (max-width: 900px) {
  .charts-row { grid-template-columns: 1fr; }
}

.chart-card {
  border-radius: 14px;
  border: 1px solid #e5e5ea;
}

.chart-card.wide {
  min-height: 320px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.table-block {
  margin-bottom: 32px;
}

.table-block h2 {
  font-size: 16px;
  font-weight: 700;
  margin: 0 0 12px;
  color: #1d1d1f;
}

.q-preview {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.muted {
  font-size: 13px;
  color: #86868b;
  margin-top: 8px;
}
</style>
