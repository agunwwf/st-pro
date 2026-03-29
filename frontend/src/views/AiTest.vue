<script setup>
import { ref } from 'vue';
import { Sun, Moon, MessageSquare, BrainCircuit, LayoutDashboard } from 'lucide-vue-next';
import AIChatTutor from '../components/AIChatTutor.vue';
import AIQuizSystem from '../components/AIQuizSystem.vue';

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
  activeTab.value = 'quiz';
};
</script>

<template>
  <div :class="['page-wrapper', isDarkMode ? 'dark' : '']">
    <header class="app-header">
      <div class="header-left">
        <div class="logo-box">
          <BrainCircuit :size="22" />
        </div>
        <div class="title-group">
          <h1>AI 智能测评中心</h1>

        </div>
      </div>

      <div class="header-right">
        <button @click="toggleTheme" class="icon-btn theme-toggle">
          <Sun v-if="isDarkMode" :size="20" />
          <Moon v-else :size="20" />
        </button>

        <div class="segmented-control">
          <button @click="activeTab = 'tutor'" :class="{ active: activeTab === 'tutor' }">
            <MessageSquare :size="14" /> 导师交流
          </button>
          <button @click="activeTab = 'quiz'" :class="{ active: activeTab === 'quiz' }">
            <LayoutDashboard :size="14" /> 强化练习
          </button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <Transition name="fade-slide" mode="out-in">
        <div :key="activeTab">
          <AIChatTutor v-if="activeTab === 'tutor'" @startQuiz="switchToQuiz" />
          <AIQuizSystem v-else />
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

  min-h: 100vh;
  background-color: var(--bg-color);
  color: var(--text-color);
  transition: all 0.3s ease;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica;
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
  align-items: center;
  justify-content: space-between;
  padding: 16px 40px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(20px);
  background-color: var(--header-bg);
}

.header-left { display: flex; align-items: center; gap: 15px; }
.logo-box {
  width: 40px; height: 40px; border-radius: 12px;
  background: var(--accent-color);
  color: var(--bg-color);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.title-group h1 { font-size: 18px; font-weight: 700; margin: 0; }
.subtitle { font-size: 10px; font-weight: 900; color: #86868b; letter-spacing: 2px; margin: 0; }

.header-right { display: flex; align-items: center; gap: 24px; }

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

.main-content { max-width: 1200px; margin: 0 auto; padding: 32px; }

/* 过渡动画 */
.fade-slide-enter-active, .fade-slide-leave-active { transition: all 0.4s ease; }
.fade-slide-enter-from { opacity: 0; transform: translateY(10px); }
.fade-slide-leave-to { opacity: 0; transform: translateY(-10px); }

.icon-btn { border: none; background: transparent; cursor: pointer; color: #86868b; }
</style>