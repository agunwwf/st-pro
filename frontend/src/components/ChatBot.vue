<script setup>
import { ref, nextTick } from 'vue';
import { Send, Loader2, User, Bot } from 'lucide-vue-next';
import { useDeepSeek } from '../composables/useDeepSeek.js';

const { isChatLoading, sendMessage } = useDeepSeek();
const messages = ref([]);
const input = ref('');
const scrollRef = ref(null);

const scrollToBottom = async () => {
  await nextTick();
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
  }
};

const handleSend = async () => {
  if (!input.value.trim() || isChatLoading.value) return;

  const userMsg = { role: 'user', content: input.value };
  messages.value.push(userMsg);
  const currentInput = input.value;
  input.value = '';
  scrollToBottom();

  try {
    const reply = await sendMessage(messages.value);
    messages.value.push({ role: 'assistant', content: reply || 'AI 暂时没有回应...' });
    scrollToBottom();
  } catch (e) {
    messages.value.push({ role: 'assistant', content: `抱歉，出错了: ${e.message}` });
    scrollToBottom();
  }
};
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-160px)] max-w-4xl mx-auto">
    <!-- 消息区域 -->
    <div ref="scrollRef" class="flex-1 overflow-y-auto px-4 py-8 space-y-8 scrollbar-hide">
      <div v-if="messages.length === 0" class="h-full flex flex-col items-center justify-center text-center space-y-4">
        <div class="w-16 h-16 bg-zinc-100 dark:bg-zinc-900 rounded-3xl flex items-center justify-center">
          <Bot class="text-zinc-400" :size="32" />
        </div>
        <div>
          <h3 class="text-xl font-semibold">开始对话</h3>
          <p class="text-zinc-500 text-sm">我可以帮你写代码、翻译或者只是聊聊天。</p>
        </div>
      </div>

      <div v-for="(msg, i) in messages" :key="i"
           :class="['flex w-full gap-4', msg.role === 'user' ? 'flex-row-reverse' : 'flex-row']">
        <!-- 头像 -->
        <div :class="['w-8 h-8 rounded-full flex items-center justify-center shrink-0',
                     msg.role === 'user' ? 'bg-zinc-900 text-white' : 'bg-zinc-100 text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400']">
          <User v-if="msg.role === 'user'" :size="16" />
          <Bot v-else :size="16" />
        </div>

        <!-- 气泡 -->
        <div :class="[
          'max-w-[80%] px-5 py-3.5 rounded-2xl text-[15px] leading-relaxed shadow-sm',
          msg.role === 'user'
            ? 'bg-zinc-900 text-white rounded-tr-none'
            : 'bg-white border border-zinc-100 dark:bg-zinc-900 dark:border-zinc-800 rounded-tl-none'
        ]">
          <pre class="whitespace-pre-wrap font-sans">{{ msg.content }}</pre>
        </div>
      </div>

      <div v-if="isChatLoading" class="flex gap-4">
        <div class="w-8 h-8 rounded-full bg-zinc-100 dark:bg-zinc-800 flex items-center justify-center">
          <Bot class="text-zinc-400" :size="16" />
        </div>
        <div class="bg-zinc-50 dark:bg-zinc-900/50 px-5 py-3.5 rounded-2xl rounded-tl-none border border-zinc-100 dark:border-zinc-800">
          <div class="flex gap-1">
            <span class="w-1.5 h-1.5 bg-zinc-300 rounded-full animate-bounce"></span>
            <span class="w-1.5 h-1.5 bg-zinc-300 rounded-full animate-bounce [animation-delay:0.2s]"></span>
            <span class="w-1.5 h-1.5 bg-zinc-300 rounded-full animate-bounce [animation-delay:0.4s]"></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="p-6 bg-white/80 dark:bg-zinc-950/80 backdrop-blur-xl border-t border-zinc-100 dark:border-zinc-800">
      <div class="relative group">
        <input
            v-model="input"
            @keyup.enter="handleSend"
            placeholder="给 DeepSeek 发送消息..."
            class="w-full pl-6 pr-14 py-4 bg-zinc-50 dark:bg-zinc-900 border-none rounded-2xl outline-none focus:ring-2 ring-zinc-500/10 transition-all text-[15px]"
        />
        <button
            @click="handleSend"
            :disabled="!input.trim() || isChatLoading"
            class="absolute right-2 top-2 p-2.5 rounded-xl transition-all"
            :class="input.trim() && !isChatLoading ? 'bg-zinc-900 text-white hover:scale-105 active:scale-95' : 'text-zinc-300'"
        >
          <Send :size="20" />
        </button>
      </div>
      <p class="text-[10px] text-center text-zinc-400 mt-3 uppercase tracking-widest font-medium">Powered by DeepSeek-V3</p>
    </div>
  </div>
</template>
