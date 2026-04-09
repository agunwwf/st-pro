<template>
  <div class="create-post-container" :class="{ 'dark': isDark }">
    <!-- 顶部导航栏 -->
    <header class="create-header">
      <div class="header-left">
        <el-button link @click="goBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="divider"></div>
        <span class="header-title">写文章</span>
      </div>
      <div class="header-right">
        <span class="save-status" :class="{ 'is-pending': savePending }">{{ saveStatusText }}</span>
        <el-button class="draft-btn" link @click="openDraftsDrawer">草稿箱</el-button>
        <el-dropdown trigger="click" @command="onMoreMenuCommand">
          <el-icon class="more-icon"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="preview" class="more-menu-item">预览</el-dropdown-item>
              <el-dropdown-item command="history" class="more-menu-item">历史版本</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 编辑区域 -->
    <main class="editor-main">
      <div class="editor-container">
        <!-- 标题输入 -->
        <div class="title-wrapper">
          <textarea
              v-model="postTitle"
              placeholder="请输入标题（最多 100 个字）"
              class="title-input"
              rows="1"
              @input="autoResizeTitle"
              ref="titleRef"
          ></textarea>
        </div>

        <div class="content-divider"></div>

        <!-- 正文输入 -->
        <div class="content-wrapper">
          <el-input
              v-model="postContent"
              type="textarea"
              :autosize="{ minRows: 15 }"
              placeholder="请输入正文"
              class="content-input"
              resize="none"
          />
        </div>

        <!-- 发布设置区域 (根据截图) -->
        <div class="settings-divider"></div>
        <div class="publish-settings">
          <h3 class="settings-title">发布设置</h3>

          <div class="setting-item">
            <span class="setting-label">发布板块</span>
            <el-select v-model="postSection" placeholder="请选择发布板块" class="section-select">
              <el-option label="问答区域" value="q-a" />
              <el-option label="求助区域" value="help" />
              <el-option label="知识分享区域" value="knowledge" />
              <el-option label="笔记区域" value="notes" />
            </el-select>
          </div>

          <div class="setting-item cover-setting-item">
            <span class="setting-label">添加封面</span>
            <div class="cover-col">
              <input
                ref="coverFileInputRef"
                type="file"
                class="cover-file-input"
                accept="image/jpeg,image/jpg,image/png,.jpg,.jpeg,.png"
                @change="onCoverFileSelected"
              />
              <div
                class="cover-upload-box"
                :class="{
                  'is-dragover': coverDragOver,
                  'is-uploading': coverUploading,
                  'has-cover': !!coverUrl
                }"
                @click="onCoverBoxClick"
                @dragenter.prevent="coverDragOver = true"
                @dragover.prevent="coverDragOver = true"
                @dragleave.prevent="onCoverDragLeave"
                @drop.prevent="onCoverDrop"
              >
                <div v-if="coverUploading" class="cover-upload-progress">
                  <el-progress type="circle" :percentage="coverUploadPercent" :width="76" />
                  <span class="progress-label">上传中…</span>
                </div>
                <template v-else-if="!coverUrl">
                  <div class="upload-placeholder">
                    <el-icon><Plus /></el-icon>
                    <span class="upload-main-text">点击或拖拽图片到此处</span>
                    <span class="upload-sub-text">添加文章封面</span>
                  </div>
                </template>
                <template v-else>
                  <img :src="displayCoverUrl" alt="封面预览" class="cover-preview" />
                  <div class="cover-preview-overlay">
                    <el-button type="primary" round size="small" plain @click.stop="triggerCoverFileSelect">
                      更换
                    </el-button>
                    <el-button type="danger" round size="small" plain @click.stop="removeCover">
                      删除
                    </el-button>
                  </div>
                </template>
              </div>
              <div class="setting-hint">支持 JPEG、JPG、PNG，单张最大 5MB；可不选封面直接发布</div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部工具栏 -->
    <footer class="editor-footer">
      <div class="footer-content">
        <div class="footer-left">
          <span class="stat-item">字数：{{ postContent.length }}</span>
        </div>
        <div class="footer-right">
          <el-button
            class="footer-save-btn"
            :class="{ 'is-enabled': canSaveDraft }"
            :disabled="!canSaveDraft"
            @click="saveCurrentAsDraft"
          >保存</el-button>
          <el-button
              type="primary"
              class="footer-publish-btn"
              :class="{ 'is-active': postTitle && postContent, 'is-disabled': !postTitle || !postContent }"
              @click="handlePublish"
              :loading="publishing"
              :disabled="!postTitle || !postContent"
          >发布</el-button>
        </div>
      </div>
    </footer>

    <!-- 预览 -->
    <el-dialog
      v-model="previewVisible"
      title="预览"
      width="min(720px, 92vw)"
      class="create-post-dialog"
      destroy-on-close
      append-to-body
    >
      <div class="preview-body" :class="{ 'dark': isDark }">
        <div v-if="displayCoverUrl" class="preview-cover-wrap">
          <img :src="displayCoverUrl" alt="" class="preview-cover-img" />
        </div>
        <div class="preview-meta">
          <span class="preview-section-tag">{{ sectionLabel(postSection) }}</span>
        </div>
        <h1 class="preview-title">{{ postTitle.trim() || '（无标题）' }}</h1>
        <div class="preview-content">{{ postContent || '（暂无正文）' }}</div>
      </div>
    </el-dialog>

    <!-- 历史版本 -->
    <el-dialog
      v-model="historyVisible"
      title="历史版本"
      width="min(560px, 92vw)"
      class="create-post-dialog"
      destroy-on-close
      append-to-body
    >
      <p v-if="!historyList.length" class="dialog-empty">暂无历史记录。编辑内容并稍等片刻后会自动记录版本。</p>
      <ul v-else class="history-list">
        <li v-for="h in historyList" :key="h.id" class="history-row">
          <div class="history-row-main">
            <div class="history-time">{{ formatHistoryTime(h.updatedAt) }}</div>
            <div class="history-snippet">{{ historySnippet(h) }}</div>
          </div>
          <el-button type="primary" link @click="restoreHistoryVersion(h)">恢复</el-button>
        </li>
      </ul>
    </el-dialog>

    <!-- 草稿箱 -->
    <el-drawer
      v-model="draftsDrawerVisible"
      title="草稿箱"
      direction="rtl"
      size="min(420px, 92vw)"
      class="create-post-drawer"
      append-to-body
    >
      <div class="drafts-toolbar">
        <el-button type="primary" @click="saveCurrentAsDraft" :disabled="!canSaveDraft">
          将当前内容存为草稿
        </el-button>
      </div>
      <p v-if="!draftsList.length" class="dialog-empty">暂无草稿。可先写文章，再点击上方保存到草稿箱。</p>
      <ul v-else class="drafts-list">
        <li v-for="d in draftsList" :key="d.id" class="draft-item">
          <div class="draft-item-body" @click="loadDraft(d)">
            <div class="draft-item-title">{{ d.title || '（无标题）' }}</div>
            <div class="draft-item-meta">{{ sectionLabel(d.section) }} · {{ formatHistoryTime(d.updatedAt) }}</div>
          </div>
          <el-button type="danger" link @click.stop="removeDraft(d.id)">删除</el-button>
        </li>
      </ul>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDark } from '@vueuse/core'
import request from '@/utils/request'
import {
  ArrowLeft, MoreFilled, Plus
} from '@element-plus/icons-vue'

// 引入样式
import './CreatePost.scss'
import { resolveMediaUrl } from './forumUtils'

const route = useRoute()
const router = useRouter()
const isDark = useDark()

const SECTION_KEYS = ['knowledge', 'q-a', 'help', 'notes']

const SECTION_LABELS = {
  knowledge: '知识分享区域',
  'q-a': '问答区域',
  help: '求助区域',
  notes: '笔记区域'
}

const STORAGE_AUTOSAVE = 'forum_create_autosave_v2'
const STORAGE_DRAFTS = 'forum_create_drafts_v2'
const STORAGE_HISTORY = 'forum_create_history_v2'
const MAX_DRAFTS = 30
const MAX_HISTORY = 25

const postTitle = ref('')
const postContent = ref('')
const postSection = ref('knowledge')
/** 后端返回的相对路径，如 /uploads/xxx.jpg */
const coverUrl = ref('')
const publishing = ref(false)
const titleRef = ref(null)
const coverFileInputRef = ref(null)
const coverUploading = ref(false)
const coverUploadPercent = ref(0)
const coverDragOver = ref(false)

const lastSavedAt = ref(null)
const savePending = ref(false)
const refreshTick = ref(0)
const previewVisible = ref(false)
const historyVisible = ref(false)
const draftsDrawerVisible = ref(false)
const historyList = ref([])
const draftsList = ref([])

let refreshTimer = null
let autosaveTimer = null
const AUTOSAVE_MS = 800

const displayCoverUrl = computed(() => resolveMediaUrl(coverUrl.value))

const sectionLabel = (key) => SECTION_LABELS[key] || key || '未选择板块'

const pad2 = (n) => String(n).padStart(2, '0')

function formatSaveStatus(ts) {
  if (!ts) return '尚未保存'
  const diff = Date.now() - ts
  if (diff < 8_000) return '最近保存 刚刚'
  if (diff < 60_000) return `最近保存 ${Math.max(1, Math.floor(diff / 1000))} 秒前`
  if (diff < 3_600_000) return `最近保存 ${Math.floor(diff / 60000)} 分钟前`
  const then = new Date(ts)
  const now = new Date()
  if (then.toDateString() === now.toDateString()) {
    return `最近保存 今天 ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
  }
  return `最近保存 ${then.getMonth() + 1}月${then.getDate()}日 ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
}

function formatHistoryTime(ts) {
  if (!ts) return ''
  const then = new Date(ts)
  const now = new Date()
  if (then.toDateString() === now.toDateString()) {
    return `今天 ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
  }
  return `${then.getFullYear()}-${pad2(then.getMonth() + 1)}-${pad2(then.getDate())} ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
}

const saveStatusText = computed(() => {
  refreshTick.value
  return formatSaveStatus(lastSavedAt.value)
})

const canSaveDraft = computed(() => !!(postTitle.value.trim() || postContent.value.trim()))

function readJsonStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw)
  } catch {
    return fallback
  }
}

function writeJsonStorage(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch (e) {
    console.warn('localStorage write failed', e)
    ElMessage.warning('本地存储已满或不可用，草稿可能无法保存')
  }
}

function loadDraftsFromStorage() {
  const arr = readJsonStorage(STORAGE_DRAFTS, [])
  draftsList.value = Array.isArray(arr) ? arr : []
}

function loadHistoryFromStorage() {
  const arr = readJsonStorage(STORAGE_HISTORY, [])
  historyList.value = Array.isArray(arr) ? arr : []
}

function snapshotPayload() {
  return {
    title: postTitle.value,
    content: postContent.value,
    section: postSection.value,
    cover: coverUrl.value || '',
    updatedAt: Date.now()
  }
}

function pushHistoryIfNeeded(payload) {
  if (!String(payload.title || '').trim() && !String(payload.content || '').trim()) return
  const prev = readJsonStorage(STORAGE_HISTORY, [])
  const list = Array.isArray(prev) ? [...prev] : []
  const last = list[0]
  if (
    last &&
    last.title === payload.title &&
    last.content === payload.content &&
    last.section === payload.section &&
    last.cover === payload.cover
  ) {
    return
  }
  list.unshift({
    id: `${payload.updatedAt}-${Math.random().toString(36).slice(2, 9)}`,
    ...payload
  })
  writeJsonStorage(STORAGE_HISTORY, list.slice(0, MAX_HISTORY))
  loadHistoryFromStorage()
}

function persistAutosave() {
  const payload = snapshotPayload()
  writeJsonStorage(STORAGE_AUTOSAVE, payload)
  const hasText = !!(String(payload.title || '').trim() || String(payload.content || '').trim())
  lastSavedAt.value = hasText ? payload.updatedAt : null
  savePending.value = false
  pushHistoryIfNeeded(payload)
}

function scheduleAutosave() {
  if (autosaveTimer) clearTimeout(autosaveTimer)
  autosaveTimer = setTimeout(() => {
    autosaveTimer = null
    persistAutosave()
  }, AUTOSAVE_MS)
}

watch(
  [postTitle, postContent, postSection, coverUrl],
  () => {
    savePending.value = true
    scheduleAutosave()
  },
  { flush: 'post' }
)

function clearAutosaveAndHistory() {
  localStorage.removeItem(STORAGE_AUTOSAVE)
  localStorage.removeItem(STORAGE_HISTORY)
  lastSavedAt.value = null
  historyList.value = []
}

function onMoreMenuCommand(cmd) {
  if (cmd === 'preview') {
    previewVisible.value = true
  } else if (cmd === 'history') {
    loadHistoryFromStorage()
    historyVisible.value = true
  }
}

function openDraftsDrawer() {
  loadDraftsFromStorage()
  draftsDrawerVisible.value = true
}

function historySnippet(h) {
  const t = (h.title || '').trim()
  const c = (h.content || '').replace(/\s+/g, ' ').trim()
  const head = t ? `标题：${t.slice(0, 24)}${t.length > 24 ? '…' : ''}` : ''
  const body = c ? `正文：${c.slice(0, 48)}${c.length > 48 ? '…' : ''}` : ''
  return [head, body].filter(Boolean).join(' · ') || '（空内容）'
}

function applyEditorPayload(p) {
  postTitle.value = p.title ?? ''
  postContent.value = p.content ?? ''
  postSection.value = SECTION_KEYS.includes(p.section) ? p.section : 'knowledge'
  coverUrl.value = p.cover ?? ''
  nextTickResizeTitle()
}

function nextTickResizeTitle() {
  requestAnimationFrame(() => {
    autoResizeTitle()
  })
}

async function restoreHistoryVersion(h) {
  try {
    await ElMessageBox.confirm(
      '将用该版本替换当前编辑区内容，未自动保存的修改可能丢失。是否继续？',
      '恢复历史版本',
      { type: 'warning', confirmButtonText: '恢复', cancelButtonText: '取消' }
    )
    applyEditorPayload(h)
    persistAutosave()
    historyVisible.value = false
    ElMessage.success('已恢复该版本')
  } catch {
    /* cancel */
  }
}

function newDraftId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  return `d-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

function saveCurrentAsDraft() {
  if (!canSaveDraft.value) {
    ElMessage.warning('请先填写标题或正文')
    return
  }
  const payload = snapshotPayload()
  const item = { id: newDraftId(), ...payload }
  const list = [item, ...draftsList.value.filter((d) => d.id !== item.id)].slice(0, MAX_DRAFTS)
  draftsList.value = list
  writeJsonStorage(STORAGE_DRAFTS, list)
  persistAutosave()
  ElMessage.success('已同步到草稿箱')
}

async function loadDraft(d) {
  try {
    await ElMessageBox.confirm(
      '将用所选草稿替换当前编辑区内容。是否继续？',
      '打开草稿',
      { type: 'warning', confirmButtonText: '打开', cancelButtonText: '取消' }
    )
    applyEditorPayload(d)
    persistAutosave()
    draftsDrawerVisible.value = false
    ElMessage.success('已载入草稿')
  } catch {
    /* cancel */
  }
}

function removeDraft(id) {
  const list = draftsList.value.filter((d) => d.id !== id)
  draftsList.value = list
  writeJsonStorage(STORAGE_DRAFTS, list)
  ElMessage.success('已删除草稿')
}

const goBack = () => {
  router.push('/forum')
}

const autoResizeTitle = () => {
  const el = titleRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}

const MAX_COVER_BYTES = 5 * 1024 * 1024

const validateCoverFile = (file) => {
  if (!file) return false
  const name = (file.name || '').toLowerCase()
  const okExt = /\.(jpe?g|png)$/i.test(name)
  // 拖拽时 type 常为 application/octet-stream 或空字符串，以扩展名为准
  const okMime =
    !file.type ||
    file.type === 'image/jpeg' ||
    file.type === 'image/jpg' ||
    file.type === 'image/png' ||
    file.type === 'application/octet-stream'
  if (!okExt || !okMime) {
    ElMessage.warning('仅支持 JPEG、JPG、PNG 格式')
    return false
  }
  if (file.size > MAX_COVER_BYTES) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

const uploadCoverFile = async (file) => {
  if (!validateCoverFile(file)) return
  const fd = new FormData()
  fd.append('file', file)
  coverUploading.value = true
  coverUploadPercent.value = 0
  try {
    // 不要手动设置 Content-Type，否则缺少 boundary，服务端无法解析 multipart
    const res = await request.post('/api/file/upload', fd, {
      onUploadProgress: (ev) => {
        if (ev.total) {
          coverUploadPercent.value = Math.min(99, Math.round((ev.loaded * 100) / ev.total))
        }
      }
    })
    if (res.data?.code === 200 && res.data.data?.url) {
      coverUrl.value = res.data.data.url
      coverUploadPercent.value = 100
      ElMessage.success('封面上传成功')
    } else {
      ElMessage.error(res.data?.msg || '上传失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('上传失败，请检查网络或登录状态')
  } finally {
    coverUploading.value = false
    coverUploadPercent.value = 0
    if (coverFileInputRef.value) {
      coverFileInputRef.value.value = ''
    }
  }
}

const triggerCoverFileSelect = () => {
  coverFileInputRef.value?.click()
}

const onCoverBoxClick = () => {
  if (coverUploading.value) return
  if (coverUrl.value) return
  triggerCoverFileSelect()
}

const onCoverFileSelected = (e) => {
  const file = e.target?.files?.[0]
  if (file) uploadCoverFile(file)
}

const onCoverDragLeave = (e) => {
  const box = e.currentTarget
  if (e.relatedTarget && box?.contains(e.relatedTarget)) return
  coverDragOver.value = false
}

const onCoverDrop = (e) => {
  coverDragOver.value = false
  if (coverUploading.value) return
  const file = e.dataTransfer?.files?.[0]
  if (file) uploadCoverFile(file)
}

const removeCover = () => {
  coverUrl.value = ''
  if (coverFileInputRef.value) {
    coverFileInputRef.value.value = ''
  }
}

const handlePublish = async () => {
  if (!postTitle.value.trim()) {
    return ElMessage.warning('请输入标题')
  }
  if (!postContent.value.trim()) {
    return ElMessage.warning('请输入正文内容')
  }
  if (!postSection.value) {
    return ElMessage.warning('请选择发布板块')
  }

  publishing.value = true

  try {
    const userStr = localStorage.getItem('user')
    let userId = 1
    if (userStr) {
      userId = JSON.parse(userStr).id
    }

    const cover = coverUrl.value?.trim()
    const res = await request.post('/api/forum/posts', {
      userId,
      title: postTitle.value.trim(),
      content: postContent.value,
      section: postSection.value,
      cover: cover || null
    })

    if (res.data?.code === 200) {
      clearAutosaveAndHistory()
      ElMessage.success('发布成功！')
      router.push('/forum')
    } else {
      ElMessage.error(res.data?.msg || '发布失败')
    }
  } catch (error) {
    console.error('Publish error:', error)
    ElMessage.error('发布失败，请重试')
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  const q = route.query.section
  const sectionFromQuery = typeof q === 'string' && SECTION_KEYS.includes(q) ? q : null

  const saved = readJsonStorage(STORAGE_AUTOSAVE, null)
  if (saved && typeof saved === 'object' && (saved.title || saved.content)) {
    postTitle.value = saved.title ?? ''
    postContent.value = saved.content ?? ''
    postSection.value = sectionFromQuery || (SECTION_KEYS.includes(saved.section) ? saved.section : 'knowledge')
    coverUrl.value = saved.cover ?? ''
    lastSavedAt.value = saved.updatedAt || null
  } else if (sectionFromQuery) {
    postSection.value = sectionFromQuery
  }

  loadDraftsFromStorage()
  loadHistoryFromStorage()

  refreshTimer = setInterval(() => {
    refreshTick.value++
  }, 10_000)

  titleRef.value?.focus()
  if (titleRef.value) {
    titleRef.value.style.height = 'auto'
    titleRef.value.style.height = titleRef.value.scrollHeight + 'px'
  }
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (autosaveTimer) clearTimeout(autosaveTimer)
  persistAutosave()
})
</script>
