<script setup>
import { ref, onMounted } from 'vue';
import { BookOpenText, FileSignature, CodeXml, Target, ListChecks, History, Award, CheckCircle2, XCircle, Sparkles } from 'lucide-vue-next';


const mockQuizJson = {
  id: "paper_km_001",
  title: "K-Means 算法原理与蓝桥杯真题强化卷",
  mcq: [
    { id: "m1", question: "K-Means 算法中的 'Means' 指的是什么？", options: ["A. 聚类个数 K 的均值", "B. 簇内所有点坐标的几何中心（均值）", "C. 初始质心的随机值", "D. 数据点到质心距离的中位数"], answer: "B", explanation: "K-Means 的核心是不断迭代，将簇内所有点的坐标平均值作为新的质心。", source: "参考：周志华《机器学习》第九章 聚类" },
    { id: "m2", question: "下列关于 K-Means 算法初始质心选择的说法，正确的是：", options: ["A. 初始质心的选择不影响最终聚类结果", "B. 随机选择可能导致算法陷入局部最优", "C. 必须选择数据集中已有的点作为质心", "D. K-Means++ 算法是为了减慢收敛速度"], answer: "B", explanation: "K-Means 是贪心算法，对初始值敏感，不同的初始点可能得到不同的局部最优解。", source: "参考：李航《统计学习方法》 详解" },
    { id: "m3", question: "在实际应用中，'肘部法则' (Elbow Method) 有助于解决什么问题？", options: ["A. 确定最佳的聚类个数 K", "B. 缩短算法运行时间", "C. 处理噪声数据", "D. 衡量簇内的紧密度"], answer: "A", explanation: "通过绘制不同 K 值对应的误差平方和(SSE)曲线，找到下降速度突然变缓的'肘部'点作为最佳 K。", source: "改编自：知名大厂算法岗面试基础题" }
  ],

  fill_in_the_blank: [
    { id: "f1", question: "K-Means 算法是一种____（填：监督/无监督）学习算法。", answer: "无监督", explanation: "K-Means 不需要样本的标签（Label），直接根据数据的分布进行聚类。", source: "出自：某重点大学期末考试题" },
    { id: "f2", question: "为了衡量 K-Means 聚类效果，同一个簇内的数据点越____越好。", answer: "紧凑", explanation: "聚类的目标是组内相似度高（紧凑），组间相似度低（分离）。", source: "参考：LeetCode 聚类专题题目描述" }
  ],

  coding: {
    id: "c1",
    question: "请补全 Python 代码，实现 K-Means 算法中计算两个点 'a' 和 'b' 之间欧几里得距离的函数。",
    language: "python",
    template: "import numpy as np\n\ndef compute_distance(a, b):\n    # a, b 是 numpy 数组，例如 np.array([1, 2])\n    # 请在此处补充一行代码，计算并返回欧氏距离\n    distance = \n    return distance",
    answer: "np.sqrt(np.sum(np.square(a - b)))",
    explanation: "欧氏距离是各维度差值平方和的开方。numpy 提供了简便的向量化操作。",
    source: "改编自：蓝桥杯算法训练营 Python 组练习题"
  }
};


const historyPapers = ref([
  { id: 1, title: 'K-Means 核心强化卷', score: 85, createTime: '2023-10-25 14:30', moduleId: 'kmeans' },
  { id: 2, title: '线性回归基础测验', score: 92, createTime: '2023-10-20 09:15', moduleId: 'linear' },
  { id: 3, title: '神经网络反向传播专项', score: 60, createTime: '2023-10-15 16:00', moduleId: 'neural' },
]);


// 状态流转：'idle' (初始) -> 'loading' (出卷中) -> 'testing' (做题) -> 'review' (查看解析)
const currentStatus = ref('idle');
const currentPaper = ref(null);
const userAnswers = ref({ mcq: {}, fill: {}, coding: '' });
const currentScore = ref(0);
const isCorrectMap = ref({ mcq: {}, fill: {}, coding: false }); // 记录每题对错

// ==================================================================================
// 业务逻辑核心
// ==================================================================================


const generateNewPaper = () => {
  currentStatus.value = 'loading';

  setTimeout(() => {
    currentPaper.value = mockQuizJson; // 塞入假数据
    // 初始化答案结构
    currentPaper.value.mcq.forEach(q => userAnswers.value.mcq[q.id] = null);
    currentPaper.value.fill_in_the_blank.forEach(q => userAnswers.value.fill[q.id] = '');
    userAnswers.value.coding = currentPaper.value.coding.template;
    currentStatus.value = 'testing';
  }, 1500);
};


const submitPaper = () => {
  currentStatus.value = 'loading';

  setTimeout(() => {
    let score = 0;
    const totalQuestions = currentPaper.value.mcq.length + currentPaper.value.fill_in_the_blank.length + 1; // +1 是代码题
    const pointsPerQuestion = Math.floor(100 / totalQuestions);


    currentPaper.value.mcq.forEach(q => {
      const isRight = userAnswers.value.mcq[q.id] === q.answer;
      isCorrectMap.value.mcq[q.id] = isRight;
      if (isRight) score += pointsPerQuestion;
    });


    currentPaper.value.fill_in_the_blank.forEach(q => {

      const isRight = userAnswers.value.fill[q.id].trim() === q.answer;
      isCorrectMap.value.fill[q.id] = isRight;
      if (isRight) score += pointsPerQuestion;
    });


    const codingAns = userAnswers.value.coding;
    const isCodingRight = codingAns.includes('np.sqrt') && codingAns.includes('sum') && codingAns.includes('square');
    isCorrectMap.value.coding = isCodingRight;
    if (isCodingRight) score = Math.min(100, score + (100 - score)); // 假设代码题权重很大，答对直接补满或加重分

    currentScore.value = score;
    currentStatus.value = 'review'; // 切换到解析模式

    historyPapers.value.unshift({
      id: Date.now(), title: currentPaper.value.title, score: currentScore.value, createTime: '刚刚', moduleId: 'kmeans'
    });

  }, 2000);
};


const reviewPaper = (paper) => {

  currentPaper.value = mockQuizJson;
  currentScore.value = paper.score;

  currentPaper.value.mcq.forEach(q => isCorrectMap.value.mcq[q.id] = Math.random() > 0.3);
  currentPaper.value.fill_in_the_blank.forEach(q => isCorrectMap.value.fill[q.id] = Math.random() > 0.3);
  isCorrectMap.value.coding = paper.score > 60;

  currentStatus.value = 'review';
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
            <h4>{{ paper.title }}</h4>
            <p class="date">{{ paper.createTime }}</p>
          </div>
          <div :class="['score-tag', paper.score >= 80 ? 'high' : (paper.score >= 60 ? 'mid' : 'low')]">
            {{ paper.score }}
          </div>
        </div>
      </div>
    </aside>

    <main class="quiz-main-content">

      <div v-if="currentStatus === 'idle'" class="state-view idle-view">
        <Sparkles :size="60" class="sparkle-icon" />
        <h2>专属强化练习生成器</h2>
        <p>AI 将根据你最近在【K-Means 聚类】模块的错题诊断，从权威教材和历真题中为你组建一套专属强化试卷 (10选+5填+1编程)。准备好了吗？</p>
        <button @click="generateNewPaper" class="btn-primary btn-generate">
          🧠 呼叫导师组卷
        </button>
      </div>

      <div v-else-if="currentStatus === 'loading'" class="state-view loading-view">
        <div class="loader"></div>
        <p>导师正在翻阅《机器学习》和历年蓝桥杯真题库，为你精心出题中...</p>
      </div>

      <div v-else class="paper-view">

        <header class="paper-header">
          <div class="title-area">
            <h1>{{ currentPaper.title }}</h1>
            <div class="tags">
              <span class="tag"><Target :size="12"/> KMeans 专项</span>
              <span class="tag">难度：中等</span>
            </div>
          </div>
          <div v-if="currentStatus === 'review'" class="final-score-box">
            <Award :size="24" />
            <span class="score-num">{{ currentScore }}</span> <span class="score-unit">分</span>
          </div>
        </header>

        <div class="paper-body">

          <section class="question-section mcq-section">
            <div class="sec-header">
              <ListChecks :size="18" class="icon-sec" />
              <h2>一、单项选择题 (每题 5 分)</h2>
            </div>

            <div v-for="(q, index) in currentPaper.mcq" :key="q.id" class="question-item mcq-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.mcq[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>

              <div class="options-list">
                <label
                    v-for="opt in q.options"
                    :key="opt"
                    :class="[
                      'option-label',
                      currentStatus === 'testing' ? 'hover-effect' : '',
                      userAnswers.mcq[q.id] === opt[0] ? 'is-selected' : '',
                      currentStatus === 'review' && opt[0] === q.answer ? 'is-correct' : '',
                      currentStatus === 'review' && userAnswers.mcq[q.id] === opt[0] && opt[0] !== q.answer ? 'is-wrong' : ''
                  ]"
                >
                  <input
                      type="radio"
                      :name="q.id"
                      :value="opt[0]"
                      v-model="userAnswers.mcq[q.id]"
                      :disabled="currentStatus === 'review'"
                  />
                  <span class="opt-text">{{ opt }}</span>
                </label>
              </div>

              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
                <p class="exp-source">📚 题目来源：{{ q.source }}</p>
              </div>
            </div>
          </section>

          <section class="question-section fill-section">
            <div class="sec-header">
              <FileSignature :size="18" class="icon-sec" />
              <h2>二、填空题 (每题 10 分)</h2>
            </div>

            <div v-for="(q, index) in currentPaper.fill_in_the_blank" :key="q.id" class="question-item fill-item">
              <div class="q-title">
                <span class="q-index">{{ index + 1 }}.</span>
                <pre class="q-text">{{ q.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.fill[q.id]" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>

              <div class="ans-area">
                <input
                    type="text"
                    v-model="userAnswers.fill[q.id]"
                    placeholder="在此处填写答案"
                    :disabled="currentStatus === 'review'"
                    :class="[
                            currentStatus === 'review' && isCorrectMap.fill[q.id] ? 'input-right' : '',
                            currentStatus === 'review' && !isCorrectMap.fill[q.id] ? 'input-wrong' : ''
                        ]"
                />
              </div>

              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">标准答案：<span class="highlight">{{ q.answer }}</span></p>
                <p class="exp-text">导师解析：{{ q.explanation }}</p>
                <p class="exp-source">📚 题目来源：{{ q.source }}</p>
              </div>
            </div>
          </section>

          <section class="question-section coding-section">
            <div class="sec-header">
              <CodeXml :size="18" class="icon-sec" />
              <h2>三、算法编程题 (每题 20 分)</h2>
            </div>

            <div class="question-item coding-item">
              <div class="q-title">
                <span class="q-index">1.</span>
                <pre class="q-text">{{ currentPaper.coding.question }}</pre>
                <div v-if="currentStatus === 'review'" class="status-badge">
                  <CheckCircle2 v-if="isCorrectMap.coding" class="icon-right" :size="18"/>
                  <XCircle v-else class="icon-wrong" :size="18"/>
                </div>
              </div>

              <div class="code-editor-wrapper">
                <div class="editor-header">
                  <span class="lang-tag">{{ currentPaper.coding.language }}</span>
                </div>
                <textarea
                    v-model="userAnswers.coding"
                    class="code-textarea"
                    :disabled="currentStatus === 'review'"
                ></textarea>
              </div>

              <div v-if="currentStatus === 'review'" class="explanation-panel">
                <p class="exp-ans">参考实现代码：</p>
                <pre class="code-standard">{{ currentPaper.coding.answer }}</pre>
                <p class="exp-text">导师解析：{{ currentPaper.coding.explanation }}</p>
                <p class="exp-source">📚 题目来源：{{ currentPaper.coding.source }}</p>
              </div>
            </div>
          </section>

        </div>

        <footer v-if="currentStatus === 'testing'" class="paper-footer">
          <button @click="submitPaper" class="btn-primary btn-submit">
            < Award :size="16" /> 交卷并由 AI 导师批改
          </button>
        </footer>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ==================================================================================
   布局与基础变量 (采用 Apple 风格)
   ================================================================================== */
.quiz-system-page {
  --bg-main: #F5F5F7;
  --bg-card: #FFFFFF;
  --text-primary: #1D1D1F;
  --text-secondary: #86868B;
  --accent: #007AFF;
  --success: #28CD41;
  --warning: #FF9500;
  --danger: #FF3B30;
  --border: #D2D2D7;

  display: flex;
  height: calc(100vh - 80px); /* 减去顶部 Navigation 高度 */
  background: var(--bg-main);
  color: var(--text-primary);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica;
  overflow: hidden;
}

/* ==================================================================================
   左侧：历史练习记录
   ================================================================================== */
.history-sidebar {
  width: 280px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-head { color: var(--accent); }
.sidebar-header h3 { font-size: 16px; font-weight: 600; margin: 0; }

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.history-list::-webkit-scrollbar { width: 4px; }
.history-list::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 5px;
}
.history-item:hover { background: var(--bg-main); }
.item-main h4 { font-size: 14px; font-weight: 600; margin: 0 0 5px 0; }
.item-main .date { font-size: 12px; color: var(--text-secondary); margin: 0; }

.score-tag {
  font-size: 14px; font-weight: 700; width: 36px; height: 36px;
  border-radius: 18px; display: flex; align-items: center; justify-content: center;
}
.score-tag.high { background: #EAFBF0; color: var(--success); }
.score-tag.mid { background: #FFF8E6; color: var(--warning); }
.score-tag.low { background: #FFEBEB; color: var(--danger); }

.quiz-main-content { flex: 1; overflow: hidden; position: relative; }

.state-view {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 40px; text-align: center;
}
.state-view h2 { font-size: 24px; font-weight: 700; margin: 20px 0; }
.state-view p { color: var(--text-secondary); max-width: 500px; line-height: 1.6; margin-bottom: 30px;}

.idle-view .sparkle-icon { color: #FFD700; animation: pulse 2s infinite; }
@keyframes pulse { 0% { opacity: 0.6; } 50% { opacity: 1; } 100% { opacity: 0.6; } }

/* 加载动画 */
.loader {
  border: 4px solid #f3f3f3; border-top: 4px solid var(--accent);
  border-radius: 50%; width: 40px; height: 40px;
  animation: spin 1s linear infinite; margin-bottom: 20px;
}
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }


.paper-view {
  height: 100%; display: flex; flex-direction: column; background: #fff;
}

.paper-header {
  padding: 20px 40px; border-bottom: 1px solid var(--border);
  display: flex; justify-content: space-between; align-items: center;
}
.paper-header h1 { font-size: 20px; font-weight: 700; margin: 0 0 8px 0; }
.paper-header .tags { display: flex; gap: 8px; }
.tag { font-size: 11px; color: var(--text-secondary); background: var(--bg-main); padding: 3px 8px; border-radius: 4px; display: flex; align-items: center; gap: 4px;}


.final-score-box {
  display: flex; align-items: baseline; gap: 5px; color: var(--accent);
  padding: 10px 20px; background: #E6F2FF; border-radius: 12px;
}
.score-num { font-size: 32px; font-weight: 800; line-height: 1; }
.score-unit { font-size: 14px; font-weight: 600; }

.paper-body {
  flex: 1; overflow-y: auto; padding: 40px;
}
.paper-body::-webkit-scrollbar { width: 6px; }
.paper-body::-webkit-scrollbar-thumb { background: #ddd; border-radius: 3px; }

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
.option-label {
  display: flex; align-items: center; gap: 12px; padding: 12px 15px;
  border: 1px solid var(--border); border-radius: 8px; cursor: pointer; transition: 0.2s; font-size: 14px;
}
.option-label.hover-effect:hover { background: #E6F2FF; border-color: var(--accent); }
.option-label input { margin: 0; width: 16px; height: 16px; accent-color: var(--accent); }


.option-label.is-selected { border-color: var(--accent); background: #E6F2FF; border-width: 2px; }
.option-label.is-correct { border-color: var(--success); background: #EAFBF0; border-width: 2px; font-weight: 600; color: var(--success);}
.option-label.is-wrong { border-color: var(--danger); background: #FFEBEB; border-width: 2px; text-decoration: line-through; color: var(--danger);}


.ans-area { padding-left: 25px; }
.ans-area input {
  width: 300px; padding: 10px 15px; border-radius: 8px; border: 1px solid var(--border); outline: none; transition: 0.2s;
}
.ans-area input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px rgba(0,122,255,0.1); }
.ans-area input.input-right { border-color: var(--success); background: #EAFBF0; color: var(--success); font-weight: 600;}
.ans-area input.input-wrong { border-color: var(--danger); background: #FFEBEB; color: var(--danger); font-weight: 600;}


.code-editor-wrapper { border: 1px solid var(--border); border-radius: 8px; overflow: hidden; background: #282c34;}
.editor-header { padding: 5px 10px; background: #21252b; border-bottom: 1px solid #181a1f;}
.lang-tag { font-size: 10px; color: var(--text-secondary); text-transform: uppercase; font-weight: 700; letter-spacing: 1px; }
.code-textarea {
  width: 100%; height: 150px; padding: 15px; font-family: 'Courier New', Courier, monospace; font-size: 13px;
  background: transparent; color: #abb2bf; border: none; outline: none; resize: none; line-height: 1.5;
}


.explanation-panel {
  margin-top: 20px; padding: 15px; background: var(--bg-main); border-radius: 8px; border-left: 4px solid var(--border); font-size: 13px; line-height: 1.6;
}

.question-item:has(.icon-wrong) .explanation-panel { border-left-color: var(--danger); background: #FFEBEB; }
.question-item:has(.icon-right) .explanation-panel { border-left-color: var(--success); background: #EAFBF0; }

.exp-ans { font-weight: 600; margin: 0 0 8px 0; }
.highlight { color: var(--success); font-weight: 700; font-size: 15px;}
.wrong-highlight { color: var(--danger); }
.exp-text { color: var(--text-primary); margin: 0 0 8px 0; }
.exp-source { color: var(--accent); margin: 0; font-weight: 600; font-style: italic; display: flex; align-items: center; gap: 4px;}


.code-standard {
  background: #e1e1e8; padding: 10px; border-radius: 4px; font-family: monospace; font-size: 12px; color: #333; overflow-x: auto; margin-bottom: 10px;
}


.paper-footer {
  padding: 20px 40px; border-top: 1px solid var(--border); display: flex; justify-content: flex-end;
}


.btn-primary {
  background: var(--accent); color: #fff; border: none; padding: 12px 24px; border-radius: 10px;
  font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px;
}
.btn-primary:hover { background: #0062CC; transform: translateY(-1px); }
.btn-primary:active { transform: translateY(0); }

.btn-generate { font-size: 16px; padding: 15px 30px; }
</style>