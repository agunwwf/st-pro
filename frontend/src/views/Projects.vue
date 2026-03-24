<template>
  <div class="projects-container">
    <div class="header-actions">
      <h2>My Projects</h2>
      <el-button type="primary" size="large" round>+ New Project</el-button>
    </div>
    <section id="projects" class="projects section">
      <h2 class="section__title">项目 & 笔记</h2>
      <div class="projects__container container">
        <!-- Project Filters -->
        <div class="projects__filters">
          <button
              class="filter-button"
              :class="{ active: activeFilter === 'all' }"
              @click="setFilter('all')"
          >全部</button>
          <button
              class="filter-button"
              :class="{ active: activeFilter === 'project' }"
              @click="setFilter('project')"
          >个人项目</button>
          <button
              class="filter-button"
              :class="{ active: activeFilter === 'notes' }"
              @click="setFilter('notes')"
          >学习笔记</button>
        </div>

        <div class="projects__grid grid">
          <div
              v-for="(item, index) in filteredProjects"
              :key="index"
              class="project-card"
          >
            <img :src="item.image" :alt="item.title" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">{{ item.title }}</h3>
              <p class="project-card__description">{{ item.description }}</p>
              <router-link
                v-if="item.category === 'project'"
                :to="item.route"
                class="project-card__link"
              >
                查看详情 &rarr;
              </router-link>
              <a
                v-else
                :href="item.link"
                target="_blank"
                rel="noopener noreferrer"
                class="project-card__link"
              >
                查看详情 &rarr;
              </a>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const activeFilter = ref('all')

const projects = [
  {
    title: 'K-means Teaching Platform',
    description: 'KMeans聚类交互式学习平台。最常用的无监督学习算法之一是Kmeans，用于聚类。',
    image: '/images/abm48.png',
    link: 'https://kmeans--project.streamlit.app/',
    category: 'project',
    route: '/projects/bayes-text-classification'
  },
  {
    title: '逻辑回归交互式学习平台',
    description: '逻辑回归是一种经典的统计学习方法，主要用于解决分类问题，尤其在二分类场景中应用广泛',
    image: '/images/tsh.png',
    link: 'https://logistic-regression-project.streamlit.app/',
    category: 'project',
    route: '/projects/wine-clustering'
  },
  {
    title: '神经网络交互式学习平台',
    description: '神经网络算法通过模拟人脑处理信息的方式，对输入数据进行学习和建模。',
    image: '/images/project_toolkit.png',
    link: 'https://neural-network-project.streamlit.app/',
    category: 'project',
    route: '/projects/diabetes-analysis'
  },
  {
    title: '线性回归交互式学习平台',
    description: '线性回归就是帮你从一堆杂乱数据中，找到这条“最贴合”的直线（或超平面）的数学方法。',
    image: '/images/screenshot.png',
    link: 'https://linear--project.streamlit.app/',
    category: 'project',
    route: '/projects/breast-cancer-prediction'
  },
  {
    title: '文本分析与分类交互式学习平台',
    description: '文本分析是从非结构化文本数据中提取有价值信息的过程',
    image: '/images/time-frequency-analysis.jpg',
    link: 'https://text--analysis.streamlit.app/',
    category: 'project',
    route: '/projects/california-housing'
  },
  {
    title: '复分析讲义',
    description: '复分析讲义（讲完留数）',
    image: '/images/complex-analysis.jpg',
    link: 'https://gitee.com/albert-chen04/partial-differential-equation',
    category: 'notes'
  },
  {
    title: '偏微分方程傻瓜式讲义',
    description: '偏微分傻瓜式讲义，作业，习题卷（无适定性，无椭圆型方程）',
    image: '/images/project_pde.jpg',
    link: 'https://gitee.com/albert-chen04/partial-differential-equation',
    category: 'notes'
  },
  {
    title: '傅里叶分析学习笔记',
    description: '傅里叶分析学习笔记（零零碎碎）',
    image: '/images/project_fourier.jpg',
    link: 'https://gitee.com/albert-chen04/fourier-analysis-lecture-notes',
    category: 'notes'
  }
]

const setFilter = (filter) => {
  activeFilter.value = filter
}

const filteredProjects = computed(() => {
  if (activeFilter.value === 'all') {
    return projects
  }
  return projects.filter(p => p.category === activeFilter.value)
})
</script>

<style scoped>
.projects-container {
  --normal-font-size: 1rem;
  --text-color: #495057;
  --border-radius: 0.75rem;
  --transition: all 0.3s ease-in-out;
  --primary-color: #007bff;
  --container-bg-color: rgba(255, 255, 255, 0.85);
  --box-shadow: 0 5px 20px rgba(0, 0, 0, 0.07);
  padding: 20px;
}

.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

/* Projects Section Styles */
.projects__filters {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 3rem;
}

.filter-button {
  background: transparent;
  border: none;
  color: #495057;
  font-weight: 600;
  font-size: var(--normal-font-size);
  cursor: pointer;
  padding: 0.5rem 1rem;
  border-radius: var(--border-radius);
  transition: var(--transition);
}

.filter-button:hover {
  background-color: #f0efff;
  color: var(--primary-color);
}

.filter-button.active {
  background-color: var(--primary-color);
  color: #fff;
}

.projects__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.project-card {
  background-color: var(--container-bg-color);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  overflow: hidden;
  transition: var(--transition);
  display: flex;
  flex-direction: column;
}

.project-card:hover {
  transform: translateY(-5px);
}

.project-card__img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.project-card__content {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.project-card__title {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.project-card__description {
  margin-top: 0.7rem;
  margin-bottom: 1.1rem;
  flex-grow: 1;
  color: var(--text-color);
  line-height: 1.5;
}

.project-card__link {
  font-weight: 600;
  align-self: flex-start;
  color: var(--primary-color);
  text-decoration: none;
}

.project-card__link:hover {
  text-decoration: underline;
}

.section {
  padding: 2rem 0;
}

.section__title {
  text-align: center;
  font-size: 2rem;
  margin-bottom: 2rem;
}
</style>