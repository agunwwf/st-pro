<template>
  <div class="dashboard">
    <div class="glass-card welcome-banner">
      <h1>{{ greeting }}，{{ displayName }}。</h1>
      <p>今天有 {{ stats.todo }} 个待办，已完成 {{ stats.done }} 个，加油！</p>
    </div>

    <div class="stats-grid">
      <div class="glass-card stat-item" v-for="(item, index) in statItems" :key="index">
        <div class="stat-icon" :style="{ background: item.color }"></div>
        <div class="stat-text">
          <span class="value">{{ item.value }}</span>
          <span class="label">{{ item.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'

const displayName = ref('用户')
const stats = ref({
  todo: 4,
  done: 2,
  score: 98
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '早上好'
  if (hour < 13) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const statItems = computed(() => [
  { label: '今日待办', value: stats.value.todo, color: '#0071E3' },
  { label: '今日已完成', value: stats.value.done, color: '#34C759' },
  { label: '本周任务', value: 12, color: '#FF9F0A' },
  { label: '综合评分', value: stats.value.score, color: '#AF52DE' }
])

onMounted(() => {
  try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      const user = JSON.parse(userStr)
      displayName.value = user.nickname || user.username || '用户'
    }
  } catch {
    displayName.value = '用户'
  }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 24px; }
.welcome-banner { padding: 40px; }
.welcome-banner h1 { margin: 0 0 10px 0; font-size: 32px; }
.welcome-banner p { margin: 0; color: var(--apple-text-secondary); }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.stat-item { padding: 24px; display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; }
.stat-text { display: flex; flex-direction: column; }
.stat-text .value { font-size: 24px; font-weight: 600; }
.stat-text .label { font-size: 13px; color: var(--apple-text-secondary); }
</style>