import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {ElMessage} from "element-plus"

// 导入新的项目组件
import BayesTextClassification from '../views/BayesTextClassification.vue'
import WineClustering from '../views/WineClustering.vue'
import DiabetesAnalysis from '../views/DiabetesAnalysis.vue'
import BreastCancerPrediction from '../views/BreastCancerPrediction.vue'
import CaliforniaHousing from '../views/CaliforniaHousing.vue';
import Forum from "@/forum/Forum.vue";

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录教学平台', guest: true }
  },
  {
    path: '/forum',
    name: 'Forum',
    component: Forum,
  },
  {
    path: '/my-exams',
    name: 'MyExams',
    component: () => import('../views/MyExams.vue'),
    meta: { title: '我的测验', requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/exam/:id',
    name: 'StudentExam',
    component: () => import('../views/StudentExam.vue'),
    meta: { title: '考试作答', requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/exam-report/:id',
    name: 'ExamReport',
    component: () => import('../views/ExamReport.vue'),
    meta: { title: '成绩分析', requiresAuth: true, role: 'STUDENT' }
  },
  {
    path: '/management',
    name: 'Management',
    component: () => import('../views/TeacherAdmin.vue'), // 指向我们全新的组件
    meta: { title: '教师控制舱 - LMS', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/teacher/exam-analytics/:id',
    name: 'TeacherExamAnalytics',
    component: () => import('../views/TeacherExamAnalytics.vue'),
    meta: { title: '考试数据分析', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/forum/create',
    name: 'CreatePost',
    component: () => import('../forum/CreatePost.vue'),
    meta: { title: '写文章 - Forum', requiresAuth: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '主页', requiresAuth: true }
      },
      {
        path: 'AiTest',
        name: 'AiTest',
        component: () => import('../views/AiTest.vue'),
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('../views/Projects.vue'),
        meta: { title: '项目', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { title: '消息', requiresAuth: true }
      },

      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('../views/Calendar.vue'),
        meta: { title: '打卡日历', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人信息', requiresAuth: true }
      },
      // ========== 新增的五个项目路由 ==========
      {
        path: '/projects/bayes-text-classification',
        name: 'K-means Teaching Platform',
        component: BayesTextClassification,
        meta: {
          requiresAuth: true,
          title: 'K-means Teaching Platform',
          category: 'project'
        }
      },
      {
        path: '/projects/wine-clustering',
        name: '逻辑回归交互式学习平台',
        component: WineClustering,
        meta: {
          requiresAuth: true,
          title: '逻辑回归交互式学习平台',
          category: 'project'
        }
      },
      {
        path: '/projects/diabetes-analysis',
        name: '神经网络交互式学习平台',
        component: DiabetesAnalysis,
        meta: {
          requiresAuth: true,
          title: '神经网络交互式学习平台',
          category: 'project'
        }
      },
      {
        path: '/projects/breast-cancer-prediction',
        name: '线性回归交互式学习平台',
        component: BreastCancerPrediction,
        meta: {
          requiresAuth: true,
          title: '线性回归交互式学习平台',
          category: 'project'
        }
      },
      {
        path: '/projects/california-housing',
        name: '文本分析与分类交互式学习平台',
        component: CaliforniaHousing,
        meta: {
          requiresAuth: true,
          title: '文本分析与分类交互式学习平台',
          category: 'project'
        }
      },
      // =====================================
    ]
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/404.vue'),
    meta: { title: 'Page Not Found' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || { top: 0, behavior: 'smooth' }
  }
})


router.beforeEach((to, from, next) => {
  if (typeof NProgress !== 'undefined') NProgress.start()
  const token = localStorage.getItem('token')

  document.title = to.meta.title || 'Apple Admin'

  // 角色权限校验（忽略大小写）
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  if (to.meta.role) {
    const username = (user.username || '').toString().toLowerCase().trim()
    const userRole = (user.role || '').toString().toUpperCase().trim()
    const needRole = to.meta.role.toString().toUpperCase().trim()


    if (needRole === 'ADMIN' && username === 'admin') {
      return next()
    }

    if (userRole !== needRole) {
      ElMessage.error('Permission denied')
      return next('/')
    }
  }

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.guest && token) {
    next('/')
  } else {
    next()
  }
})

router.afterEach(() => {
  if (typeof NProgress !== 'undefined') NProgress.done()
})

export default router