<script setup>
import { ref } from 'vue';
import { Loader2, BrainCircuit, CheckCircle2, Circle, Wand2, ArrowRight } from 'lucide-vue-next';
import { useDeepSeek } from '../composables/useDeepSeek.js';

const { isChatLoading, sendMessage } = useDeepSeek();
const quizList = ref([]);
const userAnswers = ref([]);
const isSubmitted = ref(false);

const handleGenerate = async () => {
  quizList.value = [];
  isSubmitted.value = false;
  const prompt = `你是一个专家。请针对'KMeans质心初始化'生成2道选择题。返回纯JSON数组：[{"q":"..","options":["A","B","C","D"],"ans":1,"explanation":".."}]`;

  try {
    const reply = await sendMessage([{ role: 'user', content: prompt }]);
    const cleanJson = reply.replace(/```json/g, '').replace(/```/g, '').trim();
    quizList.value = JSON.parse(cleanJson);
    userAnswers.value = new Array(quizList.value.length).fill(null);
  } catch (e) {
    alert("生成失败！");
  }
};

const handleSubmit = () => {
  if (userAnswers.value.includes(null)) { alert("还没做完呢！"); return; }
  isSubmitted.value = true;
};
</script>

<template>
  <div class="quiz-wrapper">
    <div v-if="quizList.length === 0 && !isChatLoading" class="empty-box">
      <div class="icon-circle"><BrainCircuit :size="40" /></div>
      <h2>开启 AI 强化挑战</h2>
      <p style="margin-bottom: 20px">AI 将根据诊断结果为您即时编撰题目</p>
      <button @click="handleGenerate" class="primary-btn"><Wand2 :size="15" /> 立即出题</button>
    </div>

    <div v-else-if="isChatLoading" class="loading-box">
      <Loader2 class="spin" :size="40" />
      <p>DEEPSEEK IS WRITING...</p>
    </div>

    <div v-else class="quiz-list">
      <div v-for="(q, index) in quizList" :key="index" class="quiz-card">
        <div class="card-header">
          <span class="index-num">{{ index + 1 }}</span>
          <h3>{{ q.q }}</h3>
        </div>

        <div class="options-grid">
          <button v-for="(opt, idx) in q.options" :key="idx"
                  @click="!isSubmitted && (userAnswers[index] = idx)"
                  :class="['opt-btn',
              userAnswers[index] === idx ? 'selected' : '',
              isSubmitted && idx === q.ans ? 'correct' : '',
              isSubmitted && userAnswers[index] === idx && idx !== q.ans ? 'wrong' : ''
            ]">
            <span class="opt-text">{{ String.fromCharCode(65 + idx) }}. {{ opt }}</span>
            <CheckCircle2 v-if="userAnswers[index] === idx" :size="16" />
            <Circle v-else :size="16" />
          </button>
        </div>

        <div v-if="isSubmitted" class="explanation-box">
          💡 解析：{{ q.explanation }}
        </div>
      </div>

      <div class="footer-actions">
        <button v-if="!isSubmitted" @click="handleSubmit" class="primary-btn">提交测试 <ArrowRight :size="18" /></button>
        <button v-else @click="handleGenerate" class="outline-btn">再来一组</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.quiz-wrapper { max-width: 800px; margin: 0 auto; }

.empty-box, .loading-box {
  padding: 80px; text-align: center; background: var(--card-bg);
  border-radius: 40px; border: 1px dashed var(--border-color);
}
.icon-circle { width: 80px; height: 80px; background: var(--bg-color); border-radius: 24px; margin: 0 auto 24px; display: flex; align-items: center; justify-content: center; color: #d1d1d6; }

.primary-btn {
  padding: 6px 32px; border-radius: 30px; border: none; background: var(--accent-color);
  color: var(--bg-color); font-weight: 700; cursor: pointer; display: flex; align-items: center; gap: 8px; margin: 0 auto;
}

.quiz-card {
  background: var(--card-bg); border-radius: 32px; padding: 40px;
  border: 1px solid var(--border-color); margin-bottom: 32px;
}
.card-header { display: flex; gap: 20px; margin-bottom: 30px; }
.index-num { width: 36px; height: 36px; background: #f2f2f7; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 900; }
.dark .index-num { background: #2c2c2e; }

.options-grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.opt-btn {
  display: flex; align-items: center; justify-content: space-between; padding: 18px 24px;
  border-radius: 16px; border: 1px solid var(--border-color); background: var(--bg-color);
  cursor: pointer; transition: all 0.2s; text-align: left;
}
.opt-btn.selected { background: #1d1d1f; color: #fff; border-color: #1d1d1f; }
.opt-btn.correct { border-color: #34c759; box-shadow: 0 0 0 4px rgba(52, 199, 89, 0.1); }
.opt-btn.wrong { border-color: #ff3b30; box-shadow: 0 0 0 4px rgba(255, 59, 48, 0.1); }

.explanation-box { margin-top: 24px; padding: 20px; background: #f2f2f7; border-radius: 16px; font-size: 13px; color: #86868b; line-height: 1.6; }
.dark .explanation-box { background: #2c2c2e; }

.spin { animation: rotate 1s linear infinite; }
@keyframes rotate { from { transform: rotate(0); } to { transform: rotate(360deg); } }
</style>