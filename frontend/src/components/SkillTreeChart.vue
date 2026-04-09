<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import request from '@/utils/request';

const chartRef = ref(null);
let myChart = null;
const loading = ref(true);

const STATUS_UI = {
  0: { color: '#F2F2F7', borderColor: '#D1D1D6', fontColor: '#8E8E93', label: '🔒 未解锁' },
  1: { color: '#E6F2FF', borderColor: '#0071E3', fontColor: '#0071E3', label: '💡 可学习' },
  2: { color: '#EAFBF0', borderColor: '#34C759', fontColor: '#1D1D1F', label: '✅ 已完成' }
};

const renderChart = (data) => {
  if (!chartRef.value) return;
  if (!myChart) myChart = echarts.init(chartRef.value);

  const nodes = data.map(item => ({
    id: item.moduleId,
    name: item.moduleId.toUpperCase(),
    statusLabel: STATUS_UI[item.status].label,
    symbolSize: 55, // 节点缩小
    itemStyle: {
      color: STATUS_UI[item.status].color,
      borderColor: STATUS_UI[item.status].borderColor,
      borderWidth: 2,
      shadowBlur: item.status === 1 ? 12 : 0,
      shadowColor: 'rgba(0,113,227,0.3)'
    },
    label: {
      show: true,
      formatter: [
        '{title|{b}}',
        `{status|${STATUS_UI[item.status].label}}`
      ].join('\n'),
      rich: {
        title: { fontSize: 12, fontWeight: 'bold', color: STATUS_UI[item.status].fontColor, padding: [0, 0, 4, 0] },
        status: { fontSize: 10, color: STATUS_UI[item.status].fontColor }
      }
    }
  }));

  const links = [];
  data.forEach(node => {
    node.prerequisites.forEach(pre => {
      const isPreCompleted = data.find(n => n.moduleId === pre)?.status === 2;
      links.push({
        source: pre,
        target: node.moduleId,
        lineStyle: {
          color: isPreCompleted ? '#34C759' : '#E5E5EA',
          width: isPreCompleted ? 3 : 1.5,
          curveness: 0.15
        }
      });
    });
  });

  const option = {

    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#E5E5EA',
      padding: [8, 12],
      textStyle: { color: '#1D1D1F', fontSize: 13 },
      formatter: (params) => {
        if (params.dataType === 'edge') return ''; // 鼠标指在连线上不显示提示
        return `<div style="font-weight:700;margin-bottom:4px">${params.data.name}</div>
                <div style="color:#666">当前状态: <span style="font-weight:bold">${params.data.statusLabel}</span></div>`;
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',

      force: { repulsion: 400, edgeLength: 60, gravity: 0.1 },
      draggable: true,
      roam: false, // 关闭缩放
      data: nodes,
      links: links,
      symbol: 'circle',
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8],
    }]
  };

  myChart.setOption(option);
  loading.value = false;
};

const handleResize = () => myChart && myChart.resize();

onMounted(async () => {
  window.addEventListener('resize', handleResize);
  try {
    const res = await request.get('/api/learning/skill-tree');
    if (res.data.code === 200) {
      renderChart(res.data.data);
    }
  } catch (e) {
    loading.value = false;
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (myChart) myChart.dispose();
});
</script>

<template>
  <div class="mini-skill-tree" v-loading="loading">
    <div ref="chartRef" class="chart-canvas"></div>
  </div>
</template>

<style scoped>
.mini-skill-tree {
  width: 100%;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #f2f2f7;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03); /* 悬浮感 */
  margin-bottom: 20px;
  overflow: hidden;
}

.chart-canvas {
  width: 100%;
  height: 280px;
}
</style>