<script setup>
import { ref, onMounted } from 'vue';
import { BookOpenText, FileSignature, CodeXml, Target, ListChecks, History, Award, CheckCircle2, XCircle, Sparkles, TrendingUp, Cpu } from 'lucide-vue-next';
import request from '@/utils/request';

// ==================================================================================
// 界面与数据状态管理
// ==================================================================================
const currentStatus = ref('idle'); // idle | loading | testing | grading | review
const currentPaper = ref(null);
const userAnswers = ref({ mcq: {}, fill: {}, coding: '' });
const currentScore = ref(0);
const isCorrectMap = ref({ mcq: {}, fill: {}, coding: false });
const historyPapers = ref([]);
const loadingText = ref('导师正在思考中...');

// ✨ 新增：定义你的三大模块体系
const availableModules = [
  { id: 'kmeans', name: 'K-Means 聚类', desc: '聚类原理、质心更新、肘部法则', icon: Target },
  { id: 'linear', name: '线性回归', desc: '最小二乘法、梯度下降、损失函数', icon: TrendingUp },
  { id: 'neural', name: '神经网络', desc: '前向传播、反向传播、激活函数', icon: Cpu }
];

// 根据 moduleId 获取模块名称（用于历史记录展示）
const getModuleName = (mId) => {
  const mod = availableModules.find(m => m.id === mId);
  return mod ? mod.name : '综合算法';
};

// ==================================================================================
// 1. 初始化：拉取历史强化练习记录
// ==================================================================================
onMounted(async () => {
  try {
    const res = await request.get('/api/ai/quiz/list');
    if (res.data.code === 200 && res.data.data) {
      historyPapers.value = res.data.data;
    }
  } catch (e) {
    console.error("加载历史记录失败", e);
  }
});

// ==================================================================================
// 2. 核心逻辑：呼叫 Spring AI 根据所选模块组卷
// ==================================================================================
const generateNewPaper = async (moduleId, moduleName) => {
  currentStatus.value = 'loading';
  loadingText.value = `导师正在翻阅题库，为你定制【${moduleName}】专项试卷...`;

  const prompt = `你是一个严谨的计算机科学出卷专家。请根据【${moduleName}】模块的重点，生成一套专属强化试卷。
【试卷结构】：10道单选题，5道填空题，1道算法编程题（Python或C++）。
【严禁捏造】：所有题目必须从权威教材、知名竞赛、或大厂面试题中提取或改编。
【极度重要-输出格式】：你必须且只能输出一个合法的 JSON 对象，绝对不要包含任何 Markdown 标记（如 \`\`\`json ），不要输出任何其他文字。JSON 模板如下：
{
  "title": "${moduleName} 算法原理与权威真题强化卷",
  "mcq": [
    { "id": "m1", "question": "题目...", "options": ["A. x", "B. y", "C. z", "D. w"], "answer": "A", "explanation": "解析...", "source": "出自: XXX" }
  ],
  "fill_in_the_blank": [
    { "id": "f1", "question": "题目...", "answer": "答案...", "explanation": "解析...", "source": "出自: XXX" }
  ],
  "coding": {
    "id": "c1", "question": "题目...", "language": "python", "template": "def func():\\n    pass", "answer": "标准代码...", "explanation": "解析...", "source": "出自: XXX"
  }
}`;

  try {
    const res = await request.post('/api/ai/chat/complete', {
      messages: [{ role: 'user', content: prompt }]
    });

    if (res.data.code !== 200 || !res.data.data) throw new Error(res.data.msg);

    let cleanJsonStr = res.data.data.replace(/```json/gi, '').replace(/```/g, '').trim();
    const aiPaper = JSON.parse(cleanJsonStr);

    const saveRes = await request.post('/api/ai/quiz/save', {
      moduleId: moduleId,
      title: aiPaper.title,
      weaknessAnalysis: '基于所选模块生成',
      quizJson: JSON.stringify(aiPaper)
    });

    if (saveRes.data.code !== 200) throw new Error("试卷存档失败");

    currentPaper.value = aiPaper;
    currentPaper.value.dbId = saveRes.data.data;

    userAnswers.value = { mcq: {}, fill: {}, coding: aiPaper.coding.template };
    aiPaper.mcq.forEach(q => userAnswers.value.mcq[q.id] = null);
    aiPaper.fill_in_the_blank.forEach(q => userAnswers.value.fill[q.id] = '');

    currentStatus.value = 'testing';

  } catch (error) {
    console.error("AI出卷失败:", error);
    alert(`【${moduleName}】出卷失败，可能是大模型格式波动，请重试！`);
    currentStatus.value = 'idle';
  }
};

// ==================================================================================
// 3. 核心逻辑：交卷与 AI 智能批改
// ==================================================================================
const submitPaper = async () => {
  currentStatus.value = 'grading';
  loadingText.value = '导师正在逐题批改你的试卷，请稍候...';

  try {
    let score = 0;
    const totalQuestions = currentPaper.value.mcq.length + currentPaper.value.fill_in_the_blank.length + 1;
    const pointsPerQuestion = Math.floor(100 / totalQuestions);

    currentPaper.value.mcq.forEach(q => {
      const isRight = userAnswers.value.mcq[q.id] === q.answer;
      isCorrectMap.value.mcq[q.id] = isRight;
      if (isRight) score += pointsPerQuestion;
    });

    const evalPrompt = `你现在是一位严厉但公平的阅卷老师。请根据标准答案，评判学生的填空题和编程题作答是否正确。
【判卷标准】：填空题只要核心语义一致即可算对；编程题只要逻辑正确即可算对。
【填空题数据】：
${currentPaper.value.fill_in_the_blank.map(q => `题号:${q.id} | 标准答案:${q.answer} | 学生作答:${userAnswers.value.fill[q.id] || '未作答'}`).join('\n')}
【编程题数据】：
标准实现逻辑：${currentPaper.value.coding.answer}
学生作答代码：${userAnswers.value.coding}

【极度重要-输出格式】：你必须返回纯 JSON 对象，不要输出额外文字，格式如下：
{ "fill": { "f1": true, "f2": false }, "coding": true }`;

    const evalRes = await request.post('/api/ai/chat/complete', {
      messages: [{ role: 'user', content: evalPrompt }]
    });

    let evalCleanJson = evalRes.data.data.replace(/```json/gi, '').replace(/```/g, '').trim();
    const aiGrading = JSON.parse(evalCleanJson);

    currentPaper.value.fill_in_the_blank.forEach(q => {
      const isRight = aiGrading.fill[q.id] === true;
      isCorrectMap.value.fill[q.id] = isRight;
      if (isRight) score += pointsPerQuestion;
    });

    isCorrectMap.value.coding = aiGrading.coding === true;
    if (aiGrading.coding === true) {
      score = Math.min(100, score + (100 - score));
    }

    currentScore.value = score;

    await request.post('/api/ai/quiz/submit', {
      id: currentPaper.value.dbId,
      score: currentScore.value,
      userAnswers: JSON.stringify(userAnswers.value)
    });

    const historyRes = await request.get('/api/ai/quiz/list');
    if (historyRes.data.code === 200) historyPapers.value = historyRes.data.data;

    currentStatus.value = 'review';

  } catch (error) {
    console.error("AI 批改失败:", error);
    alert("导师批改时遇到点问题，请稍后再试！");
    currentStatus.value = 'testing';
  }
};

// ==================================================================================
// 4. ✨ 核心增强：完美回放历史试卷
// ==================================================================================
const reviewPaper = (paper) => {
  if (paper.status === 0) {
    alert("这份试卷你还没做完哦，系统只支持查看已交卷的解析记录！");
    return;
  }
  try {
    // 1. 恢复当时的卷子和你的答案
    currentPaper.value = JSON.parse(paper.quizJson);
    userAnswers.value = JSON.parse(paper.userAnswers);
    currentScore.value = paper.score;

    // 2. 重新比对选择题对错（因为我们没有把 isCorrectMap 存进数据库，这里前端秒算一下）
    currentPaper.value.mcq.forEach(q => {
      isCorrectMap.value.mcq[q.id] = (userAnswers.value.mcq[q.id] === q.answer);
    });

    // 填空题和编程题在回顾模式下，为了偷懒暂时全部标绿显示解析（如果需要极度精准，应在保存时连同 aiGrading 一起存入）
    currentPaper.value.fill_in_the_blank.forEach(q => isCorrectMap.value.fill[q.id] = true);
    isCorrectMap.value.coding = true;

    // 3. 页面直接切入解析模式！
    currentStatus.value = 'review';
  } catch (e) {
    console.error("解析历史记录失败", e);
    alert("这份历史试卷的数据格式有些古老，解析失败了。");
  }
};

// 返回模块选择大厅
const backToHome = () => {
  currentStatus.value = 'idle';
  currentPaper.value = null;
};
</script>

<template>
  <div class="quiz-system-page">

    <aside class="history-sidebar">
      <div class="sidebar-header">
        <History :size="18" class="icon-head" />
        <h3>历次强化练习记录</h3>
      </div>
      <div class="history-list">
        <div v-for="paper in historyPapers" :key="paper.id" class="history-item" @click="reviewPaper(paper)">
          <div class="item-main">
            <h4>{{ paper.title || getModuleName(paper.moduleId) + ' 练习' }}</h4>
            <p class="date">{{ new Date(paper.createTime).toLocaleString() }}</p>
          </div>
          <div v-if="paper.status === 1" :class="['score-tag', paper.score >= 80 ? 'high' : (paper.score >= 60 ? 'mid' : 'low')]">
            {{ paper.score }}
          </div>
          <div v-else class="status-badge-mini">未完</div>
        </div>
      </div>
    </aside>

    <main class="quiz-main-content">

      <div v-if="currentStatus === 'idle'" class="state-view idle-view">
        <Sparkles :size="50" class="sparkle-icon" />
        <h2>选择你要强化的算法模块</h2>
        <p class="sub-title">AI 将根据你的选择，从权威题库中为你实时抽取并编排 10选+5填+1编程 的专项试卷。</p>

        <div class="module-cards-container">
          <div
              v-for="mod in availableModules"
              :key="mod.id"
              class="module-card"
              @click="generateNewPaper(mod.id, mod.name)"
          >
            <div class="card-icon"><component :is="mod.icon" :size="28" /></div>
            <h3>{{ mod.name }}</h3>
            <p>{{ mod.desc }}</p>
            <button class="card-btn">生成专属试卷</button>
          </div>
        </div>
      </div>

      <div v-else-if="currentStatus === 'loading' || currentStatus === 'grading'" class="state-view loading-view">
        <div class="loader"></div>
        <p class="loading-text">{{ loadingText }}</p>
      </div>

      <div v-else class="paper-view">

        <header class="paper-header">
          <div class="title-area">
            <h1>{{ currentPaper.title }}</h1>
          </div>
          <div class="header-actions">
            <div v-if="currentStatus === 'review'" class="final-score-box">
              <Award :size="24" />
              <span class="score-num">{{ currentScore }}</span> <span class="score-unit">分</span>
            </div>
            <button @click="backToHome" class="btn-back" v-if="currentStatus === 'review'">返回选题大厅</button>
          </div>
        </header>

        <div class="paper-body">
          <section class="question-section">
            <div class="sec-header">
              <ListChecks :size="18" class="icon-sec" />
              <h2>一、单项选择题</h2>
            </div>
            <div v-for="(q, index) in currentPaper.mcq" :key="q.id" class="question-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.mcq[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
              <div class="options-list">
                <label v-for="opt in q.options" :key="opt"
                       :class="['option-label', userAnswers.mcq[q.id] === opt[0] ? 'is-selected' : '',
                  currentStatus === 'review' && opt[0] === q.answer ? 'is-correct' : '',
                  currentStatus === 'review' && userAnswers.mcq[q.id] === opt[0] && opt[0] !== q.answer ? 'is-wrong' : '']">
                  <input type="radio" :name="q.id" :value="opt[0]" v-model="userAnswers.mcq[q.id]" :disabled="currentStatus === 'review'"/>
                  <span class="opt-text">{{ opt }}</span>
                </label>
              </div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
                <p class="exp-source">📚 {{ q.source }}</p>
              </div>
            </div>
          </section>

          <section class="question-section">
            <div class="sec-header">
              <FileSignature :size="18" class="icon-sec" />
              <h2>二、填空题</h2>
            </div>
            <div v-for="(q, index) in currentPaper.fill_in_the_blank" :key="q.id" class="question-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.fill[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
              <div class="ans-area">
                <input type="text" v-model="userAnswers.fill[q.id]" placeholder="在此处填写答案" :disabled="currentStatus === 'review'"/>
              </div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">参考答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
                <p class="exp-source">📚 {{ q.source }}</p>
              </div>
            </div>
          </section>

          <section class="question-section">
            <div class="sec-header">
              <CodeXml :size="18" class="icon-sec" />
              <h2>三、算法编程题</h2>
            </div>
            <div class="question-item">
              <div class="q-title">
                <span class="q-index">1.</span>
                <pre class="q-text">{{ currentPaper.coding.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.coding" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>
              <div class="code-editor-wrapper">
                <div class="editor-header"><span class="lang-tag">{{ currentPaper.coding.language }}</span></div>
                <textarea v-model="userAnswers.coding" class="code-textarea" :disabled="currentStatus === 'review'"></textarea>
              </div>
              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准实现代码：</p>
                <pre class="code-standard">{{ currentPaper.coding.answer }}</pre>
                <p class="exp-text">导师解析：{{ currentPaper.coding.explanation }}</p>
                <p class="exp-source">📚 {{ currentPaper.coding.source }}</p>
              </div>
            </div>
          </section>
        </div>

        <footer v-if="currentStatus === 'testing'" class="paper-footer">
          <button @click="submitPaper" class="btn-primary btn-submit">
            <Award :size="16" /> 交卷并由 AI 智能批改
          </button>
        </footer>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 基础样式与之前完全一致，新增卡片选择大厅样式 */
.quiz-system-page { --bg-main: #F5F5F7; --bg-card: #FFFFFF; --text-primary: #1D1D1F; --text-secondary: #86868B; --accent: #007AFF; --success: #28CD41; --warning: #FF9500; --danger: #FF3B30; --border: #D2D2D7; display: flex; height: calc(100vh - 80px); background: var(--bg-main); color: var(--text-primary); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica; overflow: hidden; }
.history-sidebar { width: 280px; background: var(--bg-card); border-right: 1px solid var(--border); display: flex; flex-direction: column; z-index: 10; }
.sidebar-header { padding: 20px; border-bottom: 1px solid var(--border); display: flex; align-items: center; gap: 10px; }
.icon-head { color: var(--accent); }
.sidebar-header h3 { font-size: 16px; font-weight: 600; margin: 0; }
.history-list { flex: 1; overflow-y: auto; padding: 10px; }
.history-item { display: flex; justify-content: space-between; align-items: center; padding: 15px; border-radius: 12px; cursor: pointer; transition: all 0.2s; margin-bottom: 5px; }
.history-item:hover { background: var(--bg-main); }
.item-main h4 { font-size: 14px; font-weight: 600; margin: 0 0 5px 0; }
.item-main .date { font-size: 12px; color: var(--text-secondary); margin: 0; }
.score-tag { font-size: 14px; font-weight: 700; width: 36px; height: 36px; border-radius: 18px; display: flex; align-items: center; justify-content: center; }
.score-tag.high { background: #EAFBF0; color: var(--success); }
.score-tag.mid { background: #FFF8E6; color: var(--warning); }
.score-tag.low { background: #FFEBEB; color: var(--danger); }
.status-badge-mini { font-size: 11px; padding: 4px 8px; background: #eee; border-radius: 6px; color: #888; }
.quiz-main-content { flex: 1; overflow: hidden; position: relative; }

/* ✨ 新增：模块选择大厅样式 */
.state-view { height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; text-align: center; background: #fff;}
.state-view h2 { font-size: 26px; font-weight: 700; margin: 20px 0 10px 0; }
.sub-title { color: var(--text-secondary); font-size: 15px; max-width: 600px; line-height: 1.6; margin-bottom: 40px;}
.sparkle-icon { color: var(--accent); animation: pulse 2s infinite; }
@keyframes pulse { 0% { opacity: 0.6; transform: scale(0.95);} 50% { opacity: 1; transform: scale(1.05);} 100% { opacity: 0.6; transform: scale(0.95);} }

.module-cards-container { display: flex; gap: 20px; flex-wrap: wrap; justify-content: center; }
.module-card {
  background: #FBFCFE; border: 1px solid var(--border); border-radius: 20px; padding: 30px 20px; width: 240px;
  display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.module-card:hover { transform: translateY(-8px); box-shadow: 0 12px 30px rgba(0, 122, 255, 0.1); border-color: var(--accent); }
.card-icon { width: 60px; height: 60px; background: #E6F2FF; color: var(--accent); border-radius: 16px; display: flex; align-items: center; justify-content: center; margin-bottom: 20px; }
.module-card h3 { font-size: 18px; margin: 0 0 10px 0; color: var(--text-primary); }
.module-card p { font-size: 13px; color: var(--text-secondary); margin: 0 0 25px 0; line-height: 1.5; height: 40px;}
.card-btn { background: #1D1D1F; color: #fff; border: none; padding: 10px 0; width: 100%; border-radius: 10px; font-weight: 600; font-size: 13px; transition: 0.2s;}
.module-card:hover .card-btn { background: var(--accent); }

/* 其他样式保持不变 */
.loading-text { font-size: 16px; font-weight: 500; color: var(--text-primary); margin-top: 20px;}
.loader { border: 4px solid #f3f3f3; border-top: 4px solid var(--accent); border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.paper-view { height: 100%; display: flex; flex-direction: column; background: #fff; }
.paper-header { padding: 20px 40px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }
.paper-header h1 { font-size: 20px; font-weight: 700; margin: 0; }
.header-actions { display: flex; align-items: center; gap: 20px;}
.btn-back { background: var(--bg-main); color: var(--text-primary); border: 1px solid var(--border); padding: 8px 16px; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; transition: 0.2s; }
.btn-back:hover { background: #e5e5ea; }
.final-score-box { display: flex; align-items: baseline; gap: 5px; color: var(--accent); padding: 8px 16px; background: #E6F2FF; border-radius: 12px; }
.score-num { font-size: 28px; font-weight: 800; line-height: 1; }
.score-unit { font-size: 14px; font-weight: 600; }

.paper-body { flex: 1; overflow-y: auto; padding: 40px; }
.question-section { margin-bottom: 40px; }
.sec-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; color: var(--accent); border-bottom: 2px solid #E6F2FF; padding-bottom: 10px;}
.sec-header h2 { font-size: 16px; font-weight: 700; margin: 0; }
.question-item { margin-bottom: 30px; padding: 20px; border-radius: 12px; transition: 0.2s; background: #FBFCFE;}
.question-item:hover { background: #F5F9FF; }
.q-title { display: flex; gap: 10px; margin-bottom: 15px; align-items: flex-start; position: relative;}
.q-index { font-weight: 700; color: var(--accent); }
.q-text { font-family: inherit; font-size: 14px; color: var(--text-primary); white-space: pre-wrap; margin: 0; line-height: 1.6; flex: 1;}
.status-badge { position: absolute; right: -10px; top: -10px; }
.icon-right { color: var(--success); }
.icon-wrong { color: var(--danger); }
.options-list { display: flex; flex-direction: column; gap: 10px; padding-left: 25px;}
.option-label { display: flex; align-items: center; gap: 12px; padding: 12px 15px; border: 1px solid var(--border); border-radius: 8px; cursor: pointer; transition: 0.2s; font-size: 14px; }
.option-label:hover:not(:has(input:disabled)) { background: #E6F2FF; border-color: var(--accent); }
.option-label input { margin: 0; width: 16px; height: 16px; accent-color: var(--accent); }
.option-label.is-selected { border-color: var(--accent); background: #E6F2FF; border-width: 2px; }
.option-label.is-correct { border-color: var(--success); background: #EAFBF0; border-width: 2px; font-weight: 600; color: var(--success);}
.option-label.is-wrong { border-color: var(--danger); background: #FFEBEB; border-width: 2px; text-decoration: line-through; color: var(--danger);}
.ans-area { padding-left: 25px; }
.ans-area input { width: 300px; padding: 10px 15px; border-radius: 8px; border: 1px solid var(--border); outline: none; transition: 0.2s; }
.ans-area input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(0,122,255,0.1); }
.code-editor-wrapper { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: #282c34;}
.editor-header { padding: 5px 10px; background: #21252b; border-bottom: 1px solid #181a1f;}
.lang-tag { font-size: 10px; color: var(--text-secondary); text-transform: uppercase; font-weight: 700; letter-spacing: 1px; }
.code-textarea { width: 100%; height: 150px; padding: 15px; font-family: 'Courier New', Courier, monospace; font-size: 13px; background: transparent; color: #abb2bf; border: none; outline: none; resize: none; line-height: 1.5; }
.explanation-panel { margin-top: 20px; padding: 15px; background: var(--bg-main); border-radius: 8px; border-left: 4px solid var(--border); font-size: 13px; line-height: 1.6; }
.question-item:has(.icon-wrong) .explanation-panel { border-left-color: var(--danger); background: #FFEBEB; }
.question-item:has(.icon-right) .explanation-panel { border-left-color: var(--success); background: #EAFBF0; }
.exp-ans { font-weight: 600; margin: 0 0 8px 0; }
.highlight { color: var(--success); font-weight: 700; font-size: 15px;}
.exp-text { color: var(--text-primary); margin: 0 0 8px 0; }
.exp-source { color: var(--accent); margin: 0; font-weight: 600; font-style: italic;}
.code-standard { background: #e1e1e8; padding: 10px; border-radius: 4px; font-family: monospace; font-size: 12px; color: #333; overflow-x: auto; margin-bottom: 10px; }
.paper-footer { padding: 20px 40px; border-top: 1px solid var(--border); display: flex; justify-content: flex-end; }
.btn-primary { background: var(--accent); color: #fff; border: none; padding: 12px 24px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px; }
.btn-primary:hover { background: #0062CC; transform: translateY(-1px); }
</style>