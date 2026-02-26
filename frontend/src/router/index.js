import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录 - Apple 管理后台', guest: true }
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
        meta: { title: '首页', requiresAuth: true }
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
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/404.vue'),
    meta: { title: '页面未找到' }
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