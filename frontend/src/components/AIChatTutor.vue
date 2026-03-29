<script setup>
import { ref, nextTick } from 'vue';
import { Send, Bot, User, BarChart3, Sparkles, Loader2 } from 'lucide-vue-next';
import { useDeepSeek } from '../composables/useDeepSeek.js';

const emit = defineEmits(['startQuiz']);
const { isChatLoading, sendMessage } = useDeepSeek();

const messages = ref([
  { role: 'assistant', content: '你好！我是你的 AI 学习导师。我可以根据你的错题记录提供个性化建议。' }
]);
const input = ref('');
const scrollRef = ref(null);

const scrollToBottom = async () => {
  await nextTick();
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
};

const handleAnalyze = async () => {
  if (isChatLoading.value) return;
  const analysisPrompt = `诊断我的学习数据并给出50字内建议。数据：用户在KMeans质心初始化环节错误较多。`;
  messages.value.push({ role: 'user', content: '请帮我诊断一下最近的学习弱点。' });
  scrollToBottom();

  try {
    const reply = await sendMessage([{ role: 'user', content: analysisPrompt }]);
    messages.value.push({
      role: 'assistant',
      content: reply + "\n\n我已经为你准备了对应的强化练习，现在就开始吗？"
    });
    scrollToBottom();
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '诊断失败。' });
  }
};

const handleSend = async () => {
  if (!input.value.trim() || isChatLoading.value) return;
  messages.value.push({ role: 'user', content: input.value });
  const history = [...messages.value];
  input.value = '';
  scrollToBottom();

  try {
    const reply = await sendMessage(history);
    messages.value.push({ role: 'assistant', content: reply || 'AI 暂时没有回应...' });
    scrollToBottom();
  } catch (e) {
    messages.value.push({ role: 'assistant', content: `错误: ${e.message}` });
  }
};
</script>

<template>
  <div class="chat-wrapper">
    <div class="chat-box" ref="scrollRef">
      <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
        <div class="avatar">
          <User v-if="msg.role === 'user'" :size="18" />
          <Bot v-else :size="18" />
        </div>
        <div class="bubble">
          <pre>{{ msg.content }}</pre>
          <div v-if="msg.content.includes('强化练习')" class="quiz-link">
            <button @click="emit('startQuiz')">
              前往强化练习区域 <Sparkles :size="12" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <button @click="handleAnalyze" class="diag-btn" :disabled="isChatLoading">
             错题深度诊断
      </button>
      <div class="input-container">
        <input v-model="input" @keyup.enter="handleSend" placeholder="询问 AI 导师..." />
        <button @click="handleSend" class="send-btn">
          <Loader2 v-if="isChatLoading" class="spin" :size="18" />
          <Send  :size="18" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-wrapper {
  max-width: 800px; margin: 0 auto; height: 600px;
  background: var(--card-bg); border-radius: 32px;
  border: 1px solid var(--border-color); display: flex; flex-direction: column; overflow: hidden;
}

.chat-box { flex: 1; overflow-y: auto; padding: 40px; display: flex; flex-direction: column; gap: 30px; }
.chat-box::-webkit-scrollbar { width: 0; }

.msg-row { display: flex; gap: 16px; max-width: 85%; }
.msg-row.user { flex-direction: row-reverse; align-self: flex-end; }

.avatar {
  width: 36px; height: 36px; border-radius: 12px; background: #f2f2f7;
  display: flex; align-items: center; justify-content: center; color: #86868b;
}
.user .avatar { background: #1d1d1f; color: #fff; }

.bubble {
  padding: 16px 20px; border-radius: 20px; font-size: 14px; line-height: 1.6;
  background: #f2f2f7; color: #1d1d1f; border-top-left-radius: 0;
}
.user .bubble { background: #1d1d1f; color: #fff; border-top-left-radius: 20px; border-top-right-radius: 0; }
.dark .bubble { background: #2c2c2e; color: #f5f5f7; }

pre { white-space: pre-wrap; font-family: inherit; margin: 0; }

.quiz-link { margin-top: 12px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,0.05); }
.quiz-link button {
  background: transparent; border: none; color: #0071e3;
  font-weight: 700; font-size: 12px; cursor: pointer; display: flex; align-items: center; gap: 4px;
}

.input-area { padding: 24px; border-top: 1px solid var(--border-color); }
.diag-btn {
  margin-bottom: 12px; padding: 8px 16px; border-radius: 20px; border: 1px solid var(--border-color);
  background: var(--card-bg); font-size: 15px; font-weight: 700; color: #86868b; cursor: pointer;
}
.diag-btn:hover { background: #000; color: #fff; }

.input-container { position: relative; }
.input-container input {
  width: 100%; padding: 16px 60px 16px 24px; border-radius: 16px;
  border: 1px solid var(--border-color); background: var(--bg-color);
  font-size: 14px; outline: none; box-sizing: border-box;
}

.send-btn {
  position: absolute; right: 8px; top: 8px; width: 40px; height: 40px;
  display: flex;            /* 开启弹性布局 */
  justify-content: center;  /* 水平居中 */
  align-items: center;      /* 垂直居中 */
  padding: 0;
  border: none; border-radius: 12px; background: #1d1d1f; color: #fff; cursor: pointer;
}
.dark .send-btn { background: #fff; color: #000; }

.spin { animation: rotate 1s linear infinite; }
@keyframes rotate { from { transform: rotate(0); } to { transform: rotate(360deg); } }
</style>