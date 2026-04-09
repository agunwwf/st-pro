<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { Send, Bot, User, Sparkles, Loader2, Target } from 'lucide-vue-next';
import request from '@/utils/request';

const emit = defineEmits(['startQuiz']);

const isChatLoading = ref(false);

const messages = ref([]);
const input = ref('');
const scrollRef = ref(null);

const modules = [
  { id: 'kmeans', name: 'K-Means 聚类' },
  { id: 'linear', name: '线性回归' },
  { id: 'neural', name: '神经网络' }
];

const scrollToBottom = async () => {
  await nextTick();
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
  }
};

// ================= 1. 初始化：拉取历史聊天 =================
onMounted(async () => {
  try {
    const res = await request.get(`/api/ai/chat/history`);
    if (res.data.code === 200 && res.data.data.length > 0) {
      messages.value = res.data.data;
    } else {
      messages.value = [{
        role: 'assistant',
        content: '你好！我是你的专属 AI 算法导师。你可以直接问我问题，或者点击下方的快捷按钮，让我为你诊断特定算法的掌握情况！'
      }];
    }
    scrollToBottom();
  } catch (e) { console.error("读取历史失败", e); }
});

// ================= 2. 核心：点击快捷按钮进行模块诊断 =================
const handleAnalyzeModule = async (moduleId, moduleName) => {
  if (isChatLoading.value) return;


  const userMsg = { role: 'user', content: `导师，请帮我深度分析一下我在【${moduleName}】模块的练习情况。` };
  messages.value.push(userMsg);
  await request.post('/api/ai/chat/save', userMsg);
  scrollToBottom();

  try {

    const res = await request.get(`/api/ai/analysis/source?moduleId=${moduleId}`);
    if (!res.data || !res.data.data) {
      const failMsg = {
        role: 'assistant',
        content: `⚠️ 抱歉，我没有在档案库中找到你【${moduleName}】的测验记录。请先去该模块完成一次测验，再回来找我哦！`,
      };
      messages.value.push(failMsg);
      await request.post('/api/ai/chat/save', failMsg);
      scrollToBottom();
      return;
    }

    const detailData = res.data.data; // 后端返回的 JSON 字符串

    isChatLoading.value = true;
    const runRes = await request.post('/api/ai/analysis/run', { moduleId, moduleName });
    if (runRes.data.code !== 200 || !runRes.data.data) {
      throw new Error(runRes.data.msg || '导师诊断失败');
    }
    const reply = runRes.data.data;
    const aiMsg = { role: 'assistant', content: reply };
    messages.value.push(aiMsg);

    // 5. 存入数据库
    await request.post('/api/ai/chat/save', aiMsg);
    scrollToBottom();

  } catch (e) {
    // 把真实错误展示出来，方便定位（例如：DeepSeek 鉴权失败 / 额度不足 / 后端异常）
    const tip = e?.message ? `诊断失败：${e.message}` : "诊断失败：请检查后端与大模型配置";
    const errorMsg = { role: 'assistant', content: tip };
    messages.value.push(errorMsg);
    scrollToBottom();
  } finally {
    isChatLoading.value = false;
  }
};

const handleSend = async () => {
  if (!input.value.trim() || isChatLoading.value) return;

  const userText = input.value;
  input.value = '';

  const userMsg = { role: 'user', content: userText };
  messages.value.push(userMsg);
  await request.post('/api/ai/chat/save', userMsg);
  scrollToBottom();

  try {
    isChatLoading.value = true;
    const res = await request.post('/api/ai/chat/complete', { messages: messages.value });
    if (res.data.code !== 200 || !res.data.data) {
      throw new Error(res.data.msg || '对话失败');
    }
    const reply = res.data.data;
    const aiMsg = { role: 'assistant', content: reply };
    messages.value.push(aiMsg);
    await request.post('/api/ai/chat/save', aiMsg);
    scrollToBottom();
  } catch (e) {
    console.error("发送失败", e);
    const tip = e?.message ? `对话失败：${e.message}` : "对话失败：请检查后端与大模型配置";
    messages.value.push({ role: 'assistant', content: tip });
    scrollToBottom();
  } finally { isChatLoading.value = false; }
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
            <button @click="emit('startQuiz')">立即进入强化练习 <Sparkles :size="12" /></button>
          </div>
        </div>
      </div>

      <div v-if="isChatLoading" class="msg-row assistant">
        <div class="avatar"><Bot :size="18" /></div>
        <div class="bubble loading-bubble">
          <Loader2 class="spin" :size="16" /> 导师正在思考中...
        </div>
      </div>
    </div>

    <div class="bottom-panel">
      <div class="quick-actions">
        <span class="action-label"><Target :size="14" /> 快捷诊断:</span>
        <button
            v-for="mod in modules"
            :key="mod.id"
            @click="handleAnalyzeModule(mod.id, mod.name)"
            class="chip-btn"
            :disabled="isChatLoading"
        >
          {{ mod.name }}
        </button>
      </div>

      <div class="input-container">
        <input
            v-model="input"
            @keyup.enter="handleSend"
            placeholder="向导师自由提问..."
            :disabled="isChatLoading"
        />
        <button @click="handleSend" class="send-btn" :disabled="isChatLoading">
          <Send :size="18" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-wrapper {
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  height: 100%;
  background: var(--card-bg); border-radius: 24px;
  border: 1px solid var(--border-color);
  display: flex; flex-direction: column; overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
}

.chat-box { flex: 1; overflow-y: auto; padding: 30px; display: flex; flex-direction: column; gap: 20px; }
.msg-row { display: flex; gap: 12px; max-width: 85%; }
.msg-row.user { flex-direction: row-reverse; align-self: flex-end; }
.avatar { width: 32px; height: 32px; border-radius: 10px; display: flex; align-items: center; justify-content: center; background: #000; color: #fff; flex-shrink: 0;}
.user .avatar { background: #007aff; }

.bubble { padding: 14px 18px; border-radius: 18px; font-size: 14px; background: #f2f2f7; color: #1d1d1f; border-top-left-radius: 0; line-height: 1.6;}
.user .bubble { background: #007aff; color: #fff; border-top-left-radius: 18px; border-top-right-radius: 0;}
.dark .bubble { background: #2c2c2e; color: #f5f5f7; }
pre { white-space: pre-wrap; font-family: inherit; margin: 0; }

.quiz-link { margin-top: 12px; border-top: 1px solid rgba(0,0,0,0.1); padding-top: 12px;}
.dark .quiz-link { border-top-color: rgba(255,255,255,0.1); }
.quiz-link button { background: #000; color: #fff; border: none; padding: 8px 16px; border-radius: 10px; font-size: 12px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: 0.2s; }
.quiz-link button:hover { transform: scale(1.02); }

.loading-bubble { display: flex; align-items: center; gap: 8px; color: #888; font-style: italic;}
.spin { animation: rotate 1s linear infinite; }
@keyframes rotate { from { transform: rotate(0); } to { transform: rotate(360deg); } }


.bottom-panel { padding: 16px 20px; border-top: 1px solid var(--border-color); background: var(--card-bg); }


.quick-actions { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; overflow-x: auto; padding-bottom: 4px; }
.quick-actions::-webkit-scrollbar { display: none; }
.action-label { font-size: 12px; font-weight: 600; color: #86868b; display: flex; align-items: center; gap: 4px; white-space: nowrap;}
.chip-btn { background: transparent; border: 1px solid var(--border-color); padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600; color: var(--text-color); cursor: pointer; transition: 0.2s; white-space: nowrap;}
.chip-btn:hover:not(:disabled) { background: var(--text-color); color: var(--card-bg); }
.chip-btn:disabled { opacity: 0.5; cursor: not-allowed; }


.input-container { position: relative; display: flex; align-items: center; }
.input-container input { flex: 1; padding: 14px 50px 14px 20px; border-radius: 14px; border: 1px solid var(--border-color); background: var(--bg-color); outline: none; font-size: 14px; color: var(--text-color); transition: 0.2s; }
.input-container input:focus { border-color: #007aff; }
.send-btn { position: absolute; right: 6px; width: 36px; height: 36px; border: none; border-radius: 10px; background: #007aff; color: #fff; display: flex; justify-content: center; align-items: center; cursor: pointer; transition: 0.2s;}
.send-btn:hover:not(:disabled) { transform: scale(1.05); }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }
</style>