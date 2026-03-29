<script setup>
import { ref } from 'vue';
import { Sun, Moon, MessageSquare, ImageIcon } from 'lucide-vue-next';
import ChatBot from '../components/ChatBot.vue';
import ImageGenerator from '../components/ImageGenerator.vue';

const activeTab = ref('chatbot');
const isDarkMode = ref(false);

const toggleTheme = () => {
  isDarkMode.value = !isDarkMode.value;
  document.documentElement.classList.toggle('dark');
};
</script>

<template>
  <div :class="['min-h-screen transition-colors duration-300 font-sans', isDarkMode ? 'bg-zinc-950 text-zinc-100' : 'bg-white text-zinc-900']">
    <!-- 顶部导航 -->
    <header class="border-b px-6 py-4 flex items-center justify-between sticky top-0 z-10 backdrop-blur-md border-zinc-200 dark:border-zinc-800 bg-white/80 dark:bg-zinc-950/80">
      <div class="flex flex-col">
        <h1 class="text-xl font-bold tracking-tight">AI 网关入门</h1>
        <p class="text-[10px] uppercase tracking-widest text-zinc-400 font-bold">
          {{ activeTab === 'chatbot' ? 'DeepSeek-V3 Chat' : 'DeepSeek Image Demo' }}
        </p>
      </div>

      <div class="flex items-center gap-4">
        <!-- 主题切换 -->
        <button @click="toggleTheme" class="p-2 rounded-lg hover:bg-zinc-100 dark:hover:bg-zinc-800 transition-colors">
          <Sun v-if="isDarkMode" :size="20" class="text-zinc-400" />
          <Moon v-else :size="20" class="text-zinc-500" />
        </button>

        <!-- 标签页切换 -->
        <div class="flex p-1 rounded-xl bg-zinc-100 dark:bg-zinc-900 border border-zinc-200/50 dark:border-zinc-800">
          <button
              @click="activeTab = 'chatbot'"
              :class="['flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all',
                     activeTab === 'chatbot' ? 'bg-white shadow-sm dark:bg-zinc-800 text-zinc-900 dark:text-white' : 'text-zinc-500 hover:text-zinc-700']"
          >
            <MessageSquare :size="16" /> 聊天
          </button>
          <button
              @click="activeTab = 'image'"
              :class="['flex items-center gap-2 px-4 py-1.5 rounded-lg text-sm font-medium transition-all',
                     activeTab === 'image' ? 'bg-white shadow-sm dark:bg-zinc-800 text-zinc-900 dark:text-white' : 'text-zinc-500 hover:text-zinc-700']"
          >
            <ImageIcon :size="16" /> 生图
          </button>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-6xl mx-auto p-6">
      <Transition name="fade" mode="out-in">
        <component :is="activeTab === 'chatbot' ? ChatBot : ImageGenerator" />
      </Transition>
    </main>
  </div>
</template>

<style>
@tailwind base;
@tailwind components;
@tailwind utilities;

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 隐藏滚动条但保留滚动功能 */
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
