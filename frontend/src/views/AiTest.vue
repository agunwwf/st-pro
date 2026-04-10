<script setup>
import { ref } from 'vue';
import { Sun, Moon, MessageSquare, LayoutDashboard } from 'lucide-vue-next';
import AIChatTutor from '../components/AIChatTutor.vue';
import AIQuizSystem from '../components/AIQuizSystem.vue';

const currentUser = (() => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch (_) { return {} }
})()
const isTeacher = String(currentUser?.role || '').toUpperCase() === 'ADMIN'

const activeTab = ref('tutor');
const isDarkMode = ref(false);

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value;
  // 切换根节点的暗色类名
  if (isDarkMode.value) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
};

const switchToQuiz = () => {
  if (isTeacher) return
  activeTab.value = 'quiz';
};
</script>

<template>
  <div :class="['page-wrapper', isDarkMode ? 'dark' : '']">
    <header class="app-header">
      <div class="header-left"></div>

      <div class="header-right">
        <button @click="toggleTheme" class="icon-btn theme-toggle">
          <Sun v-if="isDarkMode" :size="20" />
          <Moon v-else :size="20" />
        </button>

        <div class="segmented-control">
          <button @click="activeTab = 'tutor'" :class="{ active: activeTab === 'tutor' }">
            <MessageSquare :size="14" /> {{ isTeacher ? '自动组卷' : '导师交流' }}
          </button>
          <button v-if="!isTeacher" @click="activeTab = 'quiz'" :class="{ active: activeTab === 'quiz' }">
            <LayoutDashboard :size="14" /> 强化练习
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <Transition name="fade-slide" mode="out-in">
        <div class="tab-panel" :key="activeTab">
          <AIChatTutor v-if="activeTab === 'tutor'" @startQuiz="switchToQuiz" />
          <AIQuizSystem v-else-if="!isTeacher" />
        </div>
      </Transition>
    </main>
  </div>
</template>

<style scoped>
/* 基础变量 */
.page-wrapper {
  --bg-color: #F5F5F7;
  --text-color: #1d1d1f;
  --header-bg: rgba(255, 255, 255, 0.7);
  --border-color: rgba(0, 0, 0, 0.1);
  --accent-color: #000;
  --card-bg: #fff;

  height: 100%;
  min-height: 100%;
  background-color: var(--bg-color);
  color: var(--text-color);
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica;
  display: flex;
  flex-direction: column;
}

.page-wrapper.dark {
  --bg-color: #000;
  --text-color: #f5f5f7;
  --header-bg: rgba(20, 20, 20, 0.7);
  --border-color: rgba(255, 255, 255, 0.1);
  --accent-color: #fff;
  --card-bg: #1c1c1e;
}

/* Header 样式 */
.app-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 8px 4px 8px;
  border: none;
  background: transparent;
  position: relative;
  z-index: 20;
}

.header-left { min-width: 1px; }
.header-right { display: flex; align-items: center; gap: 12px; margin-left: auto; }

/* 胶囊切换 */
.segmented-control {
  display: flex;
  padding: 4px;
  background: rgba(0,0,0,0.05);
  border-radius: 16px;
}
.dark .segmented-control { background: rgba(255,255,255,0.1); }

.segmented-control button {
  border: none; padding: 8px 20px; border-radius: 12px;
  font-size: 12px; font-weight: 600; color: #86868b;
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: transparent; display: flex; align-items: center; gap: 6px;
}

.segmented-control button.active {
  background: var(--card-bg);
  color: var(--text-color);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  transform: scale(1.05);
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow: hidden; /* 外层不滚动，避免整体框随鼠标滚动跑出 header */
  max-width: none;
  width: 100%;
  margin: 0;
  padding: 0;
  display: flex;
}

.tab-panel {
  flex: 1;
  min-height: 0;
  display: flex;
}

/* 过渡动画 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }

.icon-btn { border: none; background: transparent; cursor: pointer; color: #86868b; }
</style>