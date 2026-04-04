<template>
  <div class="post-detail" :class="{ dark: isDark }">
    <header class="detail-header">
      <button type="button" class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回论坛</span>
      </button>
      <div class="header-actions">
        <button
          v-if="isAuthor"
          type="button"
          class="delete-header-btn"
          @click="confirmDeletePost"
        >
          <el-icon><Delete /></el-icon>
          <span>删除</span>
        </button>
        <el-icon class="theme-toggle" @click="toggleDark()">
          <Moon v-if="!isDark" />
          <Sunny v-else />
        </el-icon>
      </div>
    </header>

    <div v-if="loading" class="state-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="!post" class="state-wrap empty">
      <p>帖子不存在或已删除</p>
      <el-button type="primary" round @click="router.push('/forum')">回首页</el-button>
    </div>

    <article v-else class="article-shell">
      <div
        class="hero"
        :class="{ 'hero--cover-preview': post.cover }"
        :style="heroStyle"
        :role="post.cover ? 'button' : undefined"
        :aria-label="post.cover ? '点击查看完整封面图' : undefined"
        :tabindex="post.cover ? 0 : undefined"
        @click="onHeroClick"
        @keydown.enter.prevent="openCoverPreview"
        @keydown.space.prevent="openCoverPreview"
      >
        <div class="hero-inner">
          <span class="section-pill">{{ sectionLabel(post.section) }}</span>
          <h1 class="title">{{ post.title }}</h1>
          <div class="meta-row">
            <el-avatar :size="40" :src="post.authorAvatar || DEFAULT_AVATAR" />
            <div class="meta-text">
              <span class="author">{{ post.authorName || '匿名用户' }}</span>
              <span class="time">{{ formatPostTime(post.createTime) }}</span>
            </div>
          </div>
        </div>
        <div v-if="post.cover" class="hero-cover-hint">
          <el-icon><ZoomIn /></el-icon>
          <span>点击查看完整图片</span>
        </div>
      </div>

      <div class="content-card">
        <div class="body-text">{{ post.content }}</div>

        <div class="stats-bar">
          <div
            class="stat-chip"
            :class="{ active: voted }"
            role="button"
            tabindex="0"
            @click="onVote"
          >
            <el-icon><CaretTop /></el-icon>
            <span>{{ post.votes ?? 0 }} 赞同</span>
          </div>
          <div
            class="stat-chip"
            :class="{ active: starred }"
            role="button"
            tabindex="0"
            @click="onStar"
          >
            <el-icon><Star /></el-icon>
            <span>{{ post.stars ?? 0 }} 收藏</span>
          </div>
          <a class="stat-chip muted comment-anchor" href="#forum-comments" @click.prevent="scrollToComments">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ displayCommentCount }} 评论</span>
          </a>
        </div>
      </div>

      <section id="forum-comments" class="comments-panel">
        <div class="comments-panel-head">
          <h2 class="comments-heading">
            <el-icon class="heading-icon"><ChatDotRound /></el-icon>
            全部评论
            <span class="comments-count-badge">{{ displayCommentCount }}</span>
          </h2>
        </div>

        <div v-if="currentUser()?.id" class="comment-composer">
          <el-avatar :size="40" :src="currentUser()?.avatar || DEFAULT_AVATAR" class="composer-avatar" />
          <div class="composer-main">
            <el-input
              v-model="commentDraft"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="分享你的想法，文明发言…"
              class="composer-input"
            />
            <div class="composer-actions">
              <el-button type="primary" round :loading="commentSubmitting" @click="submitComment">
                发布评论
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="comment-login-tip">
          <el-icon><ChatDotRound /></el-icon>
          <span>登录后即可发表评论</span>
          <el-button type="primary" link @click="router.push('/login')">去登录</el-button>
        </div>

        <div v-loading="commentsLoading" class="comments-list">
          <div
            v-for="c in commentList"
            :key="c.id"
            class="comment-row"
          >
            <el-avatar :size="44" :src="c.authorAvatar || DEFAULT_AVATAR" />
            <div class="comment-bubble">
              <div class="comment-row-head">
                <span class="comment-author">{{ c.authorName || '用户' }}</span>
                <span class="comment-time">{{ formatPostTime(c.createTime) }}</span>
                <button
                  v-if="isCommentAuthor(c)"
                  type="button"
                  class="comment-delete"
                  @click="confirmDeleteComment(c)"
                >
                  删除
                </button>
              </div>
              <p class="comment-text">{{ c.content }}</p>
            </div>
          </div>
          <el-empty
            v-if="!commentsLoading && commentList.length === 0"
            description="还没有评论，欢迎抢沙发"
            :image-size="72"
          />
        </div>
      </section>
    </article>

    <el-dialog
      v-model="coverPreviewVisible"
      title="封面预览"
      width="min(96vw, 960px)"
      align-center
      destroy-on-close
      append-to-body
      class="cover-lightbox-dialog"
      :close-on-click-modal="true"
    >
      <img v-if="coverFullUrl" :src="coverFullUrl" alt="帖子封面原图" class="cover-lightbox-img" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDark, useToggle } from '@vueuse/core'
import { ArrowLeft, Moon, Sunny, CaretTop, Star, ChatDotRound, Delete, ZoomIn } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { sectionLabel, formatPostTime, resolveMediaUrl } from './forumUtils'

const route = useRoute()
const router = useRouter()
const isDark = useDark()
const toggleDark = useToggle(isDark)

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const loading = ref(true)
const post = ref(null)
const voted = ref(false)
const starred = ref(false)
const commentList = ref([])
const commentsLoading = ref(false)
const commentDraft = ref('')
const commentSubmitting = ref(false)
const coverPreviewVisible = ref(false)
let socket = null

const displayCommentCount = computed(() =>
  commentList.value.length > 0 ? commentList.value.length : (post.value?.comments ?? 0)
)

const currentUser = () => {
  try {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

/** 将浏览记录写入数据库（sys_forum_post_view） */
const recordPostViewToServer = async (postId) => {
  const u = currentUser()
  if (!u?.id || !postId) return
  try {
    await request.post(`/api/forum/posts/${postId}/view`, { userId: u.id })
  } catch (e) {
    console.debug('recordPostView', e)
  }
}

const isAuthor = computed(() => {
  const u = currentUser()
  const p = post.value
  if (!u?.id || !p?.userId) return false
  return Number(u.id) === Number(p.userId)
})

const isCommentAuthor = (c) => {
  const u = currentUser()
  if (!u?.id || !c?.userId) return false
  return Number(u.id) === Number(c.userId)
}

const scrollToComments = () => {
  document.getElementById('forum-comments')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const loadComments = async (postId) => {
  if (!postId) {
    commentList.value = []
    return
  }
  commentsLoading.value = true
  try {
    const res = await request.get(`/api/forum/posts/${postId}/comments`)
    if (res.data?.code === 200) {
      commentList.value = res.data.data || []
    } else {
      commentList.value = []
    }
  } catch (e) {
    console.error(e)
    commentList.value = []
  } finally {
    commentsLoading.value = false
  }
}

const submitComment = async () => {
  const u = currentUser()
  if (!u?.id) {
    ElMessage.warning('请先登录')
    return
  }
  const text = (commentDraft.value || '').trim()
  if (!text) {
    ElMessage.warning('请输入评论内容')
    return
  }
  const pid = post.value?.id
  if (!pid) return
  commentSubmitting.value = true
  try {
    const res = await request.post(`/api/forum/posts/${pid}/comments`, {
      userId: u.id,
      content: text
    })
    if (res.data?.code === 200) {
      commentDraft.value = ''
      ElMessage.success('评论已发布')
      await loadComments(pid)
      await loadPost({ silent: true })
    } else {
      ElMessage.error(res.data?.msg || '发布失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const confirmDeleteComment = async (c) => {
  const u = currentUser()
  if (!u?.id || !c?.id) return
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    const res = await request.delete(`/api/forum/comments/${c.id}`, { params: { userId: u.id } })
    if (res.data?.code === 200) {
      ElMessage.success('已删除')
      await loadComments(post.value.id)
      await loadPost({ silent: true })
    } else {
      ElMessage.error(res.data?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const confirmDeletePost = async () => {
  const p = post.value
  if (!p?.id || !isAuthor.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${(p.title || '').slice(0, 48)}」吗？删除后无法恢复。`,
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
  const u = currentUser()
  if (!u?.id) return
  try {
    const res = await request.delete(`/api/forum/posts/${p.id}`, { params: { userId: u.id } })
    if (res.data?.code === 200) {
      ElMessage.success('已删除')
      router.push('/forum')
    } else {
      ElMessage.error(res.data?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const heroStyle = computed(() => {
  if (post.value?.cover) {
    const coverSrc = resolveMediaUrl(post.value.cover)
    return {
      backgroundImage: `linear-gradient(135deg, rgba(15,15,20,0.88) 0%, rgba(40,35,60,0.75) 100%), url(${coverSrc})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center'
    }
  }
  return {
    background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 100%)'
  }
})

const coverFullUrl = computed(() => {
  const c = post.value?.cover
  if (c == null || !String(c).trim()) return ''
  return resolveMediaUrl(String(c).trim())
})

const openCoverPreview = () => {
  if (!coverFullUrl.value) return
  coverPreviewVisible.value = true
}

const onHeroClick = () => {
  if (!post.value?.cover) return
  openCoverPreview()
}

/**
 * @param {{ silent?: boolean }} options
 * silent：不切换全页骨架、不写浏览记录、不重新拉评论（用于点赞/收藏/WebSocket 等局部刷新，避免整页闪动）
 */
const loadPost = async (options = {}) => {
  const silent = options.silent === true
  const id = route.params.id
  if (!id) {
    if (!silent) loading.value = false
    post.value = null
    return
  }
  const user = currentUser()
  const q = user?.id != null ? `?userId=${user.id}` : ''
  try {
    if (!silent) {
      loading.value = true
    }
    const res = await request.get(`/api/forum/posts/${id}${q}`)
    const data = res.data
    if (data?.code === 200 && data.data) {
      post.value = data.data
      voted.value = !!data.data.votedByMe
      starred.value = !!data.data.starredByMe
      if (!silent) {
        recordPostViewToServer(data.data.id)
        loadComments(data.data.id)
      }
    } else {
      if (!silent) {
        post.value = null
        commentList.value = []
      }
    }
  } catch (e) {
    console.error(e)
    if (!silent) {
      post.value = null
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/forum')
}

const onVote = async () => {
  const u = currentUser()
  if (!u?.id) {
    ElMessage.warning('请先登录再点赞')
    return
  }
  if (!post.value?.id) return
  try {
    const res = await request.post('/api/forum/action', {
      postId: post.value.id,
      userId: u.id,
      type: 'vote'
    })
    if (res.data?.code !== 200) {
      ElMessage.error(res.data?.msg || '操作失败')
      return
    }
    await loadPost({ silent: true })
  } catch (e) {
    console.error(e)
  }
}

const onStar = async () => {
  const u = currentUser()
  if (!u?.id) {
    ElMessage.warning('请先登录再收藏')
    return
  }
  if (!post.value?.id) return
  try {
    const res = await request.post('/api/forum/action', {
      postId: post.value.id,
      userId: u.id,
      type: 'star'
    })
    if (res.data?.code === 200) {
      ElMessage.success('操作成功')
      await loadPost({ silent: true })
    } else {
      ElMessage.error(res.data?.msg || '操作失败')
    }
  } catch (e) {
    console.error(e)
  }
}

const setupSocket = () => {
  socket = new WebSocket('ws://localhost:8080/ws/forum')
  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'post_update' && Number(data.postId) === Number(route.params.id)) {
        loadPost({ silent: true })
      }
      if (data.type === 'post_comment' && Number(data.postId) === Number(route.params.id)) {
        loadComments(Number(route.params.id))
        loadPost({ silent: true })
      }
      if (data.type === 'post_deleted' && Number(data.postId) === Number(route.params.id)) {
        ElMessage.info('该帖已被删除')
        router.push('/forum')
      }
      if (data.type === 'new_post') {
        /* no-op for detail */
      }
    } catch (e) {
      console.error(e)
    }
  }
}

watch(
  () => route.params.id,
  () => {
    loadPost()
  }
)

onMounted(() => {
  loadPost()
  setupSocket()
})

onUnmounted(() => {
  if (socket) socket.close()
})
</script>

<style scoped lang="scss">
.post-detail {
  min-height: 100vh;
  background: #f5f5f7;
  color: #1d1d1f;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  transition: background 0.3s, color 0.3s;

  &.dark {
    background: #0a0a0a;
    color: #f5f5f7;

    .detail-header {
      background: rgba(12, 12, 14, 0.85);
      border-color: rgba(255, 255, 255, 0.08);
    }
    .back-btn {
      color: #f5f5f7;
      border-color: rgba(255, 255, 255, 0.12);
      &:hover {
        background: rgba(255, 255, 255, 0.06);
      }
    }
    .content-card {
      background: #141416;
      border-color: rgba(255, 255, 255, 0.08);
      box-shadow: 0 24px 80px rgba(0, 0, 0, 0.45);
    }
    .body-text {
      color: rgba(245, 245, 247, 0.88);
    }
    .stat-chip {
      background: rgba(255, 255, 255, 0.06);
      color: rgba(245, 245, 247, 0.85);
      &.muted {
        opacity: 0.55;
      }
      &.active {
        background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
        color: #fff;
      }
      &.comment-anchor {
        opacity: 0.9;
      }
    }
    .comments-panel {
      background: #141416;
      border-color: rgba(255, 255, 255, 0.08);
    }
    .comments-heading {
      color: #f5f5f7;
    }
    .comment-composer {
      background: rgba(255, 255, 255, 0.04);
      border-color: rgba(255, 255, 255, 0.08);
    }
    .comment-login-tip {
      background: rgba(255, 255, 255, 0.04);
      color: rgba(245, 245, 247, 0.7);
    }
    .comment-row {
      border-color: rgba(255, 255, 255, 0.06);
    }
    .comment-bubble {
      background: rgba(255, 255, 255, 0.06);
    }
    .comment-author {
      color: #f5f5f7;
    }
    .comment-text {
      color: rgba(245, 245, 247, 0.88);
    }
    .comment-time {
      color: rgba(245, 245, 247, 0.45);
    }
  }
}

.detail-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: rgba(245, 245, 247, 0.82);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: transparent;
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    background: rgba(0, 0, 0, 0.04);
    transform: translateX(-2px);
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.delete-header-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(220, 38, 38, 0.35);
  background: rgba(220, 38, 38, 0.06);
  color: #dc2626;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover {
    background: rgba(220, 38, 38, 0.12);
  }
}

.post-detail.dark .delete-header-btn {
  border-color: rgba(248, 113, 113, 0.45);
  color: #f87171;
  background: rgba(248, 113, 113, 0.1);
  &:hover {
    background: rgba(248, 113, 113, 0.18);
  }
}

.theme-toggle {
  font-size: 22px;
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  transition: background 0.2s;
  &:hover {
    background: rgba(0, 0, 0, 0.06);
  }
}

.state-wrap {
  max-width: 720px;
  margin: 48px auto;
  padding: 0 24px;
  &.empty {
    text-align: center;
    color: #86868b;
    p {
      margin-bottom: 20px;
      font-size: 16px;
    }
  }
}

.article-shell {
  max-width: 820px;
  margin: 0 auto 80px;
  padding: 24px 20px 48px;
}

.hero {
  border-radius: 24px;
  padding: 48px 40px 40px;
  margin-bottom: -32px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}

.hero--cover-preview {
  cursor: zoom-in;
  outline: none;
  &:focus-visible {
    outline: 2px solid #60a5fa;
    outline-offset: 3px;
  }
}

.hero-cover-hint {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(10px);
  pointer-events: none;
  user-select: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
}

.section-pill {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  backdrop-filter: blur(8px);
  margin-bottom: 16px;
}

.title {
  font-size: clamp(26px, 4vw, 36px);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: #fff;
  margin: 0 0 24px;
  text-shadow: 0 2px 24px rgba(0, 0, 0, 0.25);
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.meta-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  .author {
    font-weight: 700;
    font-size: 16px;
    color: #fff;
  }
  .time {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.75);
  }
}

.content-card {
  position: relative;
  z-index: 2;
  background: #fff;
  border-radius: 24px;
  padding: 48px 40px 32px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.08);
}

.body-text {
  font-size: 17px;
  line-height: 1.75;
  color: #3a3a3c;
  white-space: pre-wrap;
  word-break: break-word;
  margin-bottom: 36px;
}

.stats-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 600;
  background: #f5f5f7;
  color: #1d1d1f;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
  &.muted {
    cursor: default;
    opacity: 0.65;
  }
  &:not(.muted):hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
  &.active {
    background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
    color: #fff;
    border-color: transparent;
    box-shadow: 0 8px 28px rgba(37, 99, 235, 0.35);
  }
}

.stat-chip.comment-anchor {
  cursor: pointer;
  opacity: 0.92;
  text-decoration: none;
  &:hover {
    opacity: 1;
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.comments-panel {
  margin-top: 28px;
  padding: 28px 28px 32px;
  background: #fff;
  border-radius: 24px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.06);
}

.comments-panel-head {
  margin-bottom: 22px;
}

.comments-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #1d1d1f;
  .heading-icon {
    font-size: 22px;
    color: #6366f1;
  }
}

.comments-count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 26px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  color: #4f46e5;
}

.comment-composer {
  display: flex;
  gap: 16px;
  padding: 18px;
  margin-bottom: 24px;
  border-radius: 16px;
  background: #f5f5f7;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.composer-avatar {
  flex-shrink: 0;
}

.composer-main {
  flex: 1;
  min-width: 0;
}

.composer-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.comment-login-tip {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: #f5f5f7;
  font-size: 15px;
  color: #515154;
}

.comments-list {
  min-height: 80px;
}

.comment-row {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  &:last-child {
    border-bottom: none;
  }
}

.comment-bubble {
  flex: 1;
  min-width: 0;
  padding: 12px 16px;
  border-radius: 14px;
  background: #fafafa;
}

.comment-row-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 700;
  font-size: 15px;
  color: #1d1d1f;
}

.comment-time {
  font-size: 13px;
  color: #86868b;
}

.comment-delete {
  margin-left: auto;
  border: none;
  background: none;
  font-size: 13px;
  font-weight: 600;
  color: #dc2626;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 6px;
  &:hover {
    background: rgba(220, 38, 38, 0.08);
  }
}

.comment-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  color: #3a3a3c;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

<!-- append-to-body 的弹窗不受 scoped 影响 -->
<style lang="scss">
.cover-lightbox-dialog .el-dialog__body {
  padding: 12px 16px 24px;
}

.cover-lightbox-img {
  display: block;
  max-width: 100%;
  max-height: min(85vh, 900px);
  width: auto;
  height: auto;
  margin: 0 auto;
  object-fit: contain;
  border-radius: 8px;
}
</style>
