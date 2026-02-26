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
          <button class="filter-button active" data-filter="all">全部</button>
          <button class="filter-button" data-filter="project">个人项目</button>
          <button class="filter-button" data-filter="notes">学习笔记</button>
        </div>

        <div class="projects__grid grid">
          <!-- Project Item 1: Project -->
          <div class="project-card" data-category="project">
            <img src="/images/abm48.png" alt="kmeans" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">K-means Teaching Platform</h3>
              <p class="project-card__description">KMeans聚类交互式学习平台</p>
              <p class="project-card__description">最常用的无监督学习算法之一是Kmeans，用于聚类。使用它，可以对常规数据进行聚类，还可以执行其他任务</p>
              <a href="https://kmeans--project.streamlit.app/" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>

          <div class="project-card" data-category="project">
            <img src="/images/tsh.png" alt="逻辑回归" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">逻辑回归交互式学习平台</h3>
              <p class="project-card__description">逻辑回归是一种经典的统计学习方法，主要用于解决分类问题，尤其在二分类场景中应用广泛</p>
              <a href="https://logistic-regression-project.streamlit.app/" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>

          <div class="project-card" data-category="project">
            <img src="/images/project_toolkit.png" alt="神经网络" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">神经网络交互式学习平台</h3>
              <p class="project-card__description">神经网络算法通过模拟人脑处理信息的方式，对输入数据进行学习和建模，以完成分类、回归、模式识别等任务。</p>
              <a href="https://neural-network-project.streamlit.app/" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>

          <!-- Project Item 2: Project -->
          <div class="project-card" data-category="project">
            <img src="/images/screenshot.png" alt="线性回归" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">线性回归交互式学习平台</h3>
              <p class="project-card__description">线性回归就是帮你从一堆杂乱数据中，找到这条“最贴合”的直线（或超平面）的数学方法。</p>
              <a href="https://linear--project.streamlit.app/" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>
          <div class="project-card" data-category="project">
            <img src="/images/time-frequency-analysis.jpg" alt="文本分析" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">文本分析与分类交互式学习平台</h3>
              <p class="project-card__description">文本分析是从非结构化文本数据中提取有价值信息的过程</p>
              <a href="https://text--analysis.streamlit.app/" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>
          <!-- Project Item 2: Notes -->
          <div class="project-card" data-category="notes">
            <img src="/images/complex-analysis.jpg" alt="复分析讲义" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">复分析讲义</h3>
              <p class="project-card__description">复分析讲义（讲完留数）</p>
              <a href="https://gitee.com/albert-chen04/partial-differential-equation" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>

          <div class="project-card" data-category="notes">
            <img src="/images/project_pde.jpg" alt="偏微分方程讲义" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">偏微分方程傻瓜式讲义</h3>
              <p class="project-card__description">偏微分傻瓜式讲义，作业，习题卷（无适定性，无椭圆型方程）</p>
              <a href="https://gitee.com/albert-chen04/partial-differential-equation" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>

          <!-- NEW Project Item 3: Notes -->
          <div class="project-card" data-category="notes">
            <img src="/images/project_fourier.jpg" alt="傅里叶分析学习笔记" class="project-card__img">
            <div class="project-card__content">
              <h3 class="project-card__title">傅里叶分析学习笔记</h3>
              <p class="project-card__description">傅里叶分析学习笔记（零零碎碎）</p>
              <a href="https://gitee.com/albert-chen04/fourier-analysis-lecture-notes" target="_blank" class="project-card__link">查看详情 &rarr;</a>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
<script setup>
const filterContainer = document.querySelector('.projects__filters');
const projectCards = document.querySelectorAll('.project-card');

if (filterContainer) {
  filterContainer.addEventListener('click', (e) => {
    if (e.target.classList.contains('filter-button')) {
      filterContainer.querySelector('.active').classList.remove('active');
      e.target.classList.add('active');

      const filterValue = e.target.getAttribute('data-filter');

      projectCards.forEach(card => {
        const cardCategory = card.getAttribute('data-category');

        if (filterValue === 'all' || cardCategory === filterValue) {
          card.style.display = 'flex';
        } else {
          card.style.display = 'none';
        }
      });
    }
  });
}
</script>
<style scoped>
  @import "../assets/projects.css";
</style>
