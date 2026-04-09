<template>
  <div class="my-records" :class="{ dark: isDark }">
    <header class="records-header">
      <button type="button" class="back-btn" @click="router.push('/forum')">
        <el-icon><ArrowLeft /></el-icon>
        <span>论坛首页</span>
      </button>
      <h1 class="page-title">我的论坛记录</h1>
      <el-icon class="theme-toggle" @click="toggleDark()">
        <Moon v-if="!isDark" />
        <Sunny v-else />
      </el-icon>
    </header>

    <div class="records-body">
      <p class="subtitle">发帖、点赞、收藏与浏览记录均来自数据库；点击卡片进入详情</p>

      <el-tabs v-model="activeTab" class="record-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我的发帖" name="posts">
          <div v-loading="loading.posts" class="card-grid">
            <template v-if="lists.posts.length">
              <div
                v-for="p in lists.posts"
                :key="'p-' + p.id"
                class="record-card-wrap"
              >
                <button type="button" class="record-card" @click="goDetail(p.id)">
                  <span class="pill">{{ sectionLabel(p.section) }}</span>
                  <h3 class="card-title">{{ p.title }}</h3>
                  <p class="excerpt">{{ excerpt(p.content) }}</p>
                  <div class="card-foot">
                    <span>{{ formatPostTime(p.createTime) }}</span>
                    <span class="stats">
                      <el-icon><CaretTop /></el-icon> {{ p.votes ?? 0 }}
                      <el-icon><Star /></el-icon> {{ p.stars ?? 0 }}
                    </span>
                  </div>
                </button>
                <el-button
                  type="danger"
                  link
                  class="card-delete-btn"
                  @click.stop="confirmDeleteMyPost(p)"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </template>
            <el-empty v-else-if="!loading.posts" description="还没有发过帖，去写一篇吧" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="赞过" name="voted">
          <div v-loading="loading.voted" class="card-grid">
            <template v-if="lists.voted.length">
              <button
                v-for="p in lists.voted"
                :key="'v-' + p.id"
                type="button"
                class="record-card accent-vote"
                @click="goDetail(p.id)"
              >
                <span class="pill">{{ sectionLabel(p.section) }}</span>
                <h3 class="card-title">{{ p.title }}</h3>
                <p class="excerpt">{{ excerpt(p.content) }}</p>
                <div class="card-foot">
                  <span class="author">{{ p.authorName || '匿名' }}</span>
                  <span class="stats">
                    <el-icon><CaretTop /></el-icon> {{ p.votes ?? 0 }}
                  </span>
                </div>
              </button>
            </template>
            <el-empty v-else-if="!loading.voted" description="暂无点赞记录" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="收藏" name="starred">
          <div v-loading="loading.starred" class="card-grid">
            <template v-if="lists.starred.length">
              <button
                v-for="p in lists.starred"
                :key="'s-' + p.id"
                type="button"
                class="record-card accent-star"
                @click="goDetail(p.id)"
              >
                <span class="pill">{{ sectionLabel(p.section) }}</span>
                <h3 class="card-title">{{ p.title }}</h3>
                <p class="excerpt">{{ excerpt(p.content) }}</p>
                <div class="card-foot">
                  <span class="author">{{ p.authorName || '匿名' }}</span>
                  <span class="stats">
                    <el-icon><Star /></el-icon> {{ p.stars ?? 0 }}
                  </span>
                </div>
              </button>
            </template>
            <el-empty v-else-if="!loading.starred" description="暂无收藏" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="浏览记录" name="viewed">
          <div v-loading="loading.viewed" class="card-grid">
            <template v-if="lists.viewed.length">
              <button
                v-for="p in lists.viewed"
                :key="'vwd-' + p.id"
                type="button"
                class="record-card accent-view"
                @click="goDetail(p.id)"
              >
                <span class="pill">{{ sectionLabel(p.section) }}</span>
                <h3 class="card-title">{{ p.title }}</h3>
                <p class="excerpt">{{ excerpt(p.content) }}</p>
                <div class="card-foot">
                  <span>最近浏览 {{ formatPostTime(p.lastViewTime) }}</span>
                  <span class="stats">
                    <el-icon><View /></el-icon>
                  </span>
                </div>
              </button>
            </template>
            <el-empty v-else-if="!loading.viewed" description="打开过帖子详情后，浏览记录会出现在这里" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useDark, useToggle } from '@vueuse/core'
import { ArrowLeft, Moon, Sunny, CaretTop, Star, View, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { sectionLabel, formatPostTime } from './forumUtils'

const router = useRouter()
const isDark = useDark()
const toggleDark = useToggle(isDark)

const activeTab = ref('posts')
const lists = reactive({
  posts: [],
  voted: [],
  starred: [],
  viewed: []
})
const loading = reactive({
  posts: false,
  voted: false,
  starred: false,
  viewed: false
})

const userId = () => {
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    return u.id
  } catch {
    return null
  }
}

const excerpt = (text) => {
  if (!text) return ''
  const t = text.replace(/\s+/g, ' ').trim()
  return t.length > 120 ? t.slice(0, 120) + '…' : t
}

const goDetail = (id) => {
  router.push({ name: 'ForumPostDetail', params: { id: String(id) } })
}

const confirmDeleteMyPost = async (p) => {
  if (!p?.id) return
  const uid = userId()
  if (uid == null) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${(p.title || '').slice(0, 40)}」吗？删除后无法恢复。`,
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
  try {
    const res = await request.delete(`/api/forum/posts/${p.id}`, { params: { userId: uid } })
    if (res.data?.code === 200) {
      ElMessage.success('已删除')
      await fetchTab('posts')
    } else {
      ElMessage.error(res.data?.msg || '删除失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('删除失败')
  }
}

const fetchTab = async (name) => {
  const uid = userId()
  if (uid == null) return
  const key = name
  if (key === 'posts') {
    loading.posts = true
    try {
      const res = await request.get(`/api/forum/user/${uid}/posts`)
      if (res.data?.code === 200) lists.posts = res.data.data || []
    } finally {
      loading.posts = false
    }
  } else if (key === 'voted') {
    loading.voted = true
    try {
      const res = await request.get(`/api/forum/user/${uid}/voted`)
      if (res.data?.code === 200) lists.voted = res.data.data || []
    } finally {
      loading.voted = false
    }
  } else if (key === 'starred') {
    loading.starred = true
    try {
      const res = await request.get(`/api/forum/user/${uid}/starred`)
      if (res.data?.code === 200) lists.starred = res.data.data || []
    } finally {
      loading.starred = false
    }
  } else if (key === 'viewed') {
    loading.viewed = true
    try {
      const res = await request.get(`/api/forum/user/${uid}/viewed`)
      if (res.data?.code === 200) lists.viewed = res.data.data || []
    } finally {
      loading.viewed = false
    }
  }
}

const onTabChange = (name) => {
  fetchTab(name)
}

onMounted(() => {
  fetchTab(activeTab.value)
})
</script>

<style scoped lang="scss">
.my-records {
  min-height: 100vh;
  background: linear-gradient(180deg, #fafafa 0%, #ececf0 45%, #e8e8ed 100%);
  color: #1d1d1f;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  transition: background 0.35s, color 0.35s;

  &.dark {
    background: linear-gradient(180deg, #0c0c0e 0%, #121218 50%, #0a0a0c 100%);
    color: #f5f5f7;

    .records-header {
      background: rgba(18, 18, 22, 0.9);
      border-color: rgba(255, 255, 255, 0.06);
    }
    .back-btn {
      color: #f5f5f7;
      border-color: rgba(255, 255, 255, 0.1);
      &:hover {
        background: rgba(255, 255, 255, 0.06);
      }
    }
    .page-title {
      color: #fff;
    }
    .subtitle {
      color: rgba(245, 245, 247, 0.55);
    }
    .record-card {
      background: rgba(28, 28, 32, 0.92);
      border-color: rgba(255, 255, 255, 0.08);
      .card-title {
        color: #fff;
      }
      .excerpt {
        color: rgba(245, 245, 247, 0.65);
      }
      .card-foot {
        color: rgba(245, 245, 247, 0.45);
      }
    }
    :deep(.el-tabs__item) {
      color: rgba(245, 245, 247, 0.55);
      &.is-active {
        color: #fff;
      }
    }
    :deep(.el-tabs__active-bar) {
      background: linear-gradient(90deg, #3b82f6, #a855f7);
    }
  }
}

.records-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 32px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 40;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: rgba(0, 0, 0, 0.04);
  }
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin: 0;
}

.theme-toggle {
  font-size: 22px;
  cursor: pointer;
  padding: 8px;
  border-radius: 10px;
  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }
}

.records-body {
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

.subtitle {
  margin: 0 0 24px;
  font-size: 15px;
  color: #6e6e73;
  font-weight: 500;
}

.record-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 28px;
  }
  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
    background: rgba(0, 0, 0, 0.06);
  }
  :deep(.el-tabs__item) {
    font-size: 16px;
    font-weight: 600;
    padding: 0 24px;
  }
  :deep(.el-tabs__active-bar) {
    height: 3px;
    border-radius: 3px;
    background: linear-gradient(90deg, #2563eb, #9333ea);
  }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.record-card-wrap {
  position: relative;
}

.card-delete-btn {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 2;
  font-weight: 700;
}

.record-card {
  text-align: left;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 20px;
  padding: 22px 22px 18px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  cursor: pointer;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 48px rgba(37, 99, 235, 0.12);
    border-color: rgba(37, 99, 235, 0.25);
  }

  &.accent-vote:hover {
    box-shadow: 0 20px 48px rgba(59, 130, 246, 0.15);
    border-color: rgba(59, 130, 246, 0.3);
  }
  &.accent-star:hover {
    box-shadow: 0 20px 48px rgba(234, 179, 8, 0.12);
    border-color: rgba(234, 179, 8, 0.35);
  }
  &.accent-view:hover {
    box-shadow: 0 20px 48px rgba(16, 185, 129, 0.14);
    border-color: rgba(16, 185, 129, 0.35);
  }
}

.pill {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(147, 51, 234, 0.12));
  color: #4f46e5;
  margin-bottom: 12px;
}

.card-title {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.35;
  margin: 0 0 10px;
  color: #1d1d1f;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.excerpt {
  font-size: 14px;
  line-height: 1.55;
  color: #6e6e73;
  margin: 0 0 16px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #86868b;
  .author {
    font-weight: 600;
    color: #515154;
  }
  .stats {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: #1d1d1f;
  }
}
</style>
