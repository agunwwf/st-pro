<template>
  <div class="forum-container" ref="scrollContainer" :class="{ dark: isDark }">
    <!-- 高级感头部 -->
    <header class="forum-header" :class="{ 'header-scrolled': isScrolled }">
      <div class="header-left">
        <div class="logo-container" @click="scrollToTop">
          <div class="logo-icon">F</div>
          <span class="logo-text">Forum</span>
        </div>
        <nav class="forum-nav">
          <a
              v-for="item in navItems"
              :key="item.id"
              :href="item.route || '#'"
              class="nav-link"
              :class="{ active: item.route ? (route.path === item.route) : (activeBoard === item.id) }"
              @click.prevent="item.route ? router.push(item.route) : (activeBoard = item.id)"
          >
            {{ item.label }}
          </a>
        </nav>
      </div>

      <div class="header-right">
        <div class="action-icons">
          <el-icon class="header-icon" @click="toggleDark()"><Moon v-if="!isDark"/><Sunny v-else/></el-icon>
          <el-icon class="header-icon"><Search /></el-icon>
          <el-icon class="header-icon"><Bell /></el-icon>
        </div>

        <el-button class="post-btn" round @click="goToCreatePost">
          <el-icon class="mr-2"><Plus /></el-icon> 发帖
        </el-button>

        <div v-if="!currentUser" class="login-btn-wrapper">
          <el-button type="primary" link @click="login">登录</el-button>
        </div>

        <el-dropdown v-else trigger="click" @command="handleCommand">
          <div class="user-avatar-wrapper">
            <el-avatar :size="32" :src="userAvatar" />
          </div>
          <template #dropdown>
            <el-dropdown-menu class="forum-dropdown">
              <el-dropdown-item command="my-records">我的论坛记录</el-dropdown-item>
              <el-dropdown-item command="dashboard">返回后台</el-dropdown-item>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="forum-main">
      <section class="forum-section section-a">
        <div class="section-a-content">
          <div class="section-header-info">
            <h2 class="section-display-title">{{ activeBoardMeta.title }}</h2>
            <p class="section-display-desc">{{ activeBoardMeta.desc }}</p>
          </div>
          <div class="list-feed">
            <article
              v-for="post in currentFeedPosts"
              :key="post.id"
              class="forum-list-card"
              @click="openPostDetail(post)"
            >
              <button
                v-if="isPostAuthor(post)"
                type="button"
                class="post-delete-fab"
                title="删除帖子"
                @click.stop="confirmDeletePost(post)"
              >
                <el-icon><Delete /></el-icon>
              </button>
              <div class="list-title">{{ post.title }}</div>
              <div class="list-meta-row">
                <el-avatar :size="20" :src="post.authorAvatar || userAvatar" />
                <span class="author-name">{{ post.authorName || '匿名用户' }}</span>
                <span class="author-role">{{ boardLabel(post.section) }}</span>
              </div>
              <div class="list-content-row">
                <p class="list-excerpt">{{ previewText(post.content, 120) }}</p>
                <img v-if="post.cover" :src="resolveMediaUrl(post.cover)" class="list-cover" alt="" />
              </div>
              <div class="list-actions">
                <button class="act-btn primary" @click.stop="handleVote(post)"><el-icon><CaretTop /></el-icon>赞同 {{ post.votes || 0 }}</button>
                <button class="act-btn"><el-icon><ChatDotRound /></el-icon>{{ post.comments || 0 }} 条评论</button>
                <button class="act-btn" @click.stop="handleStar(post)"><el-icon><Star /></el-icon>收藏 {{ post.stars || 0 }}</button>
              </div>
            </article>
            <div v-if="postsLoaded && currentFeedPosts.length === 0" class="feed-empty">
              当前板块暂无帖子，去发第一帖吧。
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDark, useToggle } from '@vueuse/core'
import {
  Search, Bell, Plus, Moon, Sunny,
  CaretTop, ChatDotRound, Star, Delete
} from '@element-plus/icons-vue'
import request, { WS_BASE_URL } from '@/utils/request'

// 引入外部样式
import './Forum.scss'
import { resolveMediaUrl } from './forumUtils'

const router = useRouter()
const route = useRoute()
const scrollContainer = ref(null)
const isScrolled = ref(false)
const activeBoard = ref('knowledge')

const isDark = useDark()
const toggleDark = useToggle(isDark)

const SECTION_ROUTE_KEYS = ['knowledge', 'q-a', 'notes']

const normalizeSection = (raw) => {
  const v = String(raw || '').trim().toLowerCase()
  if (!v) return 'knowledge'

  if (['knowledge', 'section-a', 'share', 'knowledge-share', 'knowledge_sharing', '知识', '知识分享'].includes(v)) {
    return 'knowledge'
  }
  if (['q-a', 'qa', 'qna', 'help', 'ask', 'question', '问答', '求助', '问答区域', '求助区域'].includes(v)) {
    return 'q-a'
  }
  if (['notes', 'note', 'section-d', '笔记', '笔记区域'].includes(v)) {
    return 'notes'
  }
  // 历史脏数据兜底到知识区，避免“看不到旧帖子”
  return 'knowledge'
}

const goToCreateWithSection = (section) => {
  const s = SECTION_ROUTE_KEYS.includes(section) ? section : 'knowledge'
  router.push({ path: '/forum/create', query: { section: s } })
}

const navItems = [
  { id: 'knowledge', label: '知识分享' },
  { id: 'q-a', label: '问答区域' },
  { id: 'notes', label: '笔记区域' },
  { id: 'my-records', label: '我的记录', route: '/forum/my' }
]

// 实时帖子数据
const realTimePosts = ref([])
const postsLoaded = ref(false)
let socket = null

/**
 * @param {{ silent?: boolean }} options
 * silent：不将 postsLoaded 置为 false，避免列表整块消失造成闪烁（点赞/收藏/WebSocket 等）
 */
const loadPosts = async (options = {}) => {
  const silent = options.silent === true
  try {
    if (!silent) {
      postsLoaded.value = false
    }
    const res = await request.get('/api/forum/posts')
    const data = res.data
    if (data?.code === 200) {
      realTimePosts.value = (data.data || []).map(post => ({
        ...post,
        time: new Date(post.createTime).toLocaleString()
      }))
    }
  } catch (err) {
    console.error('Failed to load posts', err)
  } finally {
    postsLoaded.value = true
  }
}

// 监听 WebSocket 实时更新 (原生 WebSocket)
const setupSocket = () => {
  socket = new WebSocket(`${WS_BASE_URL}/ws/forum`)

  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'new_post' || data.type === 'post_update' || data.type === 'post_deleted' || data.type === 'post_comment') {
        loadPosts({ silent: true })
      }
    } catch (e) {
      console.error('解析消息失败', e)
    }
  }

  socket.onclose = () => {
    console.log('论坛 WebSocket 已关闭')
  }
}

// 过滤不同板块的帖子
const getPostsBySection = (sectionId) => {
  const current = normalizeSection(sectionId)
  return realTimePosts.value.filter(post => normalizeSection(post.section) === current)
}

const currentFeedPosts = computed(() => getPostsBySection(activeBoard.value))
const activeBoardMeta = computed(() => {
  if (activeBoard.value === 'q-a') return { title: '问答区域', desc: '有问题？来这里寻找答案' }
  if (activeBoard.value === 'notes') return { title: '笔记区域', desc: '记录点滴，沉淀知识' }
  return { title: '知识分享区域', desc: '汇聚深度见解，分享实战经验' }
})

// 点赞功能：同一个按钮再点一次就是“取消点赞”
const handleVote = async (post) => {
  try {
    if (!currentUser.value || !currentUser.value.id) {
      ElMessage.warning('请先登录再点赞')
      return
    }

    const res = await request.post('/api/forum/action', {
      postId: post.id,
      userId: currentUser.value.id,
      type: 'vote'
    })
    const data = res.data
    if (data?.code !== 200) {
      ElMessage.error(data?.msg || '操作失败')
      return
    }
    await loadPosts({ silent: true })
  } catch (err) {
    console.error('Vote error', err)
  }
}

// 收藏功能：同一个收藏按钮再点一次就是“取消收藏”
const handleStar = async (post) => {
  try {
    if (!currentUser.value || !currentUser.value.id) {
      ElMessage.warning('请先登录再收藏')
      return
    }

    const res = await request.post('/api/forum/action', {
      postId: post.id,
      userId: currentUser.value.id,
      type: 'star'
    })
    const data = res.data
    if (data?.code === 200) {
      ElMessage.success('操作成功')
      await loadPosts({ silent: true })
    } else {
      ElMessage.error(data?.msg)
    }
  } catch (err) {
    console.error('Star error', err)
  }
}

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 用户信息同步：从 localStorage 读取真实登录用户
const currentUser = ref(null)
const userAvatar = computed(() => currentUser.value?.avatar || DEFAULT_AVATAR)

// 滚动处理
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToCreatePost = () => {
  goToCreateWithSection(activeBoard.value)
}

const login = () => {
  router.push('/login')
}

const openPostDetail = (post) => {
  if (!post?.id) return
  router.push({ path: `/forum/post/${post.id}` })
}

const boardLabel = (section) => {
  const s = normalizeSection(section)
  if (s === 'q-a') return '问答区域'
  if (s === 'notes') return '笔记区域'
  return '知识分享'
}

const isPostAuthor = (post) => {
  if (!currentUser.value?.id || post?.userId == null) return false
  return Number(currentUser.value.id) === Number(post.userId)
}

const confirmDeletePost = async (post) => {
  if (!post?.id) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${(post.title || '').slice(0, 40)}」吗？删除后无法恢复。`,
      '删除帖子',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }
  if (!currentUser.value?.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await request.delete(`/api/forum/posts/${post.id}`, {
      params: { userId: currentUser.value.id }
    })
    if (res.data?.code === 200) {
      ElMessage.success('已删除')
      await loadPosts({ silent: true })
    } else {
      ElMessage.error(res.data?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const stripHtml = (html) => {
  const div = document.createElement('div')
  div.innerHTML = String(html || '')
  return (div.textContent || div.innerText || '').replace(/\s+/g, ' ').trim()
}

const previewText = (html, n = 100) => {
  const t = stripHtml(html)
  return t.length > n ? `${t.slice(0, n)}...` : t
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 50
}

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
    ElMessage.success('已退出登录')
  } else if (command === 'my-records') {
    router.push('/forum/my')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'dashboard') {
    router.push('/dashboard')
  } else {
    ElMessage.info(`点击了 ${command} (占位)`)
  }
}

onMounted(() => {
  loadPosts()
  setupSocket()
  window.addEventListener('scroll', handleScroll)

  // 同步论坛头部登录信息
  const rawUser = localStorage.getItem('user')
  try {
    currentUser.value = rawUser ? JSON.parse(rawUser) : null
  } catch {
    currentUser.value = null
  }

  const onUserUpdated = (event) => {
    if (event?.detail) {
      currentUser.value = {
        ...(currentUser.value || {}),
        ...event.detail
      }
    } else {
      const u = localStorage.getItem('user')
      try {
        currentUser.value = u ? JSON.parse(u) : null
      } catch {
        currentUser.value = null
      }
    }
  }
  window.addEventListener('user-updated', onUserUpdated)
  window.__forumUserUpdatedHandler = onUserUpdated
})

onUnmounted(() => {
  if (socket) socket.close()
  window.removeEventListener('scroll', handleScroll)

  if (window.__forumUserUpdatedHandler) {
    window.removeEventListener('user-updated', window.__forumUserUpdatedHandler)
    window.__forumUserUpdatedHandler = null
  }
})
</script>

<style scoped lang="scss">
.forum-container {
  min-height: 100vh;
  background-color: #ffffff;
  background-image: radial-gradient(#e5e5e5 1px, transparent 1px);
  background-size: 40px 40px;
  color: #1d1d1f;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  transition: background-color 0.3s ease, color 0.3s ease;

  &.dark {
    background-color: #0a0a0a;
    background-image: radial-gradient(#1a1a1a 1px, transparent 1px);
    color: #ffffff;

    .forum-header {
      background: rgba(10, 10, 10, 0.8);
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      .logo-text, .nav-link, .header-icon { color: #ffffff; }
      .nav-link { color: rgba(255, 255, 255, 0.6); &:hover, &.active { color: #ffffff; } }
      .logo-icon { background: #ffffff; color: #000; }
      .post-btn { background: #ffffff; color: #000; &:hover { background: #e5e5e5; } }
    }

    .section-placeholder { color: #ffffff; }
    .forum-section { border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
    .forum-section.section-b,
    .forum-section.section-d { background: rgba(255,255,255,0.02); }
  }
}

/* 高级感头部 */
.forum-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  z-index: 1000;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: 80px;

    .logo-container {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;

      .logo-icon {
        width: 40px;
        height: 40px;
        background: #14110f;
        color: white;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        font-weight: 900;
        transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      }

      &:hover .logo-icon {
        transform: scale(1.1) rotate(-5deg);
      }

      .logo-text {
        font-size: 22px;
        font-weight: 600;
        color: #1d1d1f;
      }
    }

    .forum-nav {
      display: flex;
      gap: 40px;

      .nav-link {
        color: #515154;
        text-decoration: none;
        font-size: 16px;
        font-weight: 500;
        transition: all 0.2s ease;

        &:hover {
          color: #1d1d1f;
        }

        &.active {
          color: #1d1d1f;
          font-weight: 600;
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 24px;

    .action-icons {
      display: flex;
      gap: 20px;
      color: #1d1d1f;

      .header-icon {
        font-size: 22px;
        cursor: pointer;
        transition: opacity 0.2s;
        &:hover { opacity: 0.7; }
      }
    }

    .post-btn {
      background: #14110f;
      border: none;
      color: white;
      padding: 10px 24px;
      font-weight: 600;
      font-size: 15px;
      height: 40px;

      &:hover {
        background: #2c2c2e;
        color: white;
      }
    }

    .user-avatar-wrapper {
      cursor: pointer;
      transition: transform 0.2s;
      &:hover { transform: scale(1.05); }
    }
  }
}

/* 占位内容区域 */
.forum-main {
  padding-top: 72px;
}

.forum-section {
  min-height: calc(100vh - 72px);
  display: flex;
  justify-content: center;
  padding: 80px 0;
  box-sizing: border-box;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  .section-placeholder {
    font-size: 20vw;
    font-weight: 900;
    opacity: 0.05;
    color: #000;
    user-select: none;
  }

  &.section-a { background: transparent; }
  &.section-b { background: rgba(0,0,0,0.02); }
  &.section-d { background: rgba(0,0,0,0.02); }
}

/* 下拉菜单样式 */
.forum-dropdown {
  border-radius: 12px !important;
  overflow: hidden;
  border: none !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1) !important;
}

.card-interactive {
  position: relative;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  &:hover {
    transform: translateY(-3px);
  }
}

.post-delete-fab {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 5;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: rgba(220, 38, 38, 0.12);
  color: #dc2626;
  transition: background 0.2s ease, transform 0.2s ease;
  &:hover {
    background: rgba(220, 38, 38, 0.22);
    transform: scale(1.06);
  }
}

.forum-container.dark .post-delete-fab {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
  &:hover {
    background: rgba(248, 113, 113, 0.28);
  }
}
</style>
