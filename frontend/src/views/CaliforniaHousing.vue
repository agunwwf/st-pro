<template>
  <div class="project-page">
   
    <iframe class="project-frame" :src="runtimeUrl" title="文本分析与分类交互式学习平台" />
  </div>
</template>

<script setup>
const host = (import.meta.env.VITE_ST_HOST || 'http://localhost').replace(/\/$/, '')
const prefix = (import.meta.env.VITE_ST_PREFIX || '').replace(/\/$/, '')
const baseUrl = prefix ? `${host}${prefix}/8501` : `${host}:8501`
const runtimeUrl = `${baseUrl}?project=text`
</script>

<style scoped>
.project-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 130px);
}

.project-header {
  margin-bottom: 10px;
}

.project-header h1 {
  font-size: 1.35rem;
  margin: 0 0 4px 0;
}

.project-header p {
  margin: 0;
  color: #666;
}

.project-frame {
  flex: 1;
  width: 100%;
  border: none;
  border-radius: 12px;
  background: #fff;
}
</style>