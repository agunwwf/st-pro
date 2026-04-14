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
          <div id="quill-toolbar" class="rich-toolbar">
            <span class="ql-formats">
              <select class="ql-header">
                <option value="1">H1</option>
                <option value="2">H2</option>
                <option selected></option>
              </select>
              <select class="ql-size"></select>
            </span>
            <span class="ql-formats">
              <button class="ql-bold"></button>
              <button class="ql-italic"></button>
              <button class="ql-underline"></button>
              <button class="ql-strike"></button>
            </span>
            <span class="ql-formats">
              <button class="ql-blockquote"></button>
              <button class="ql-code-block"></button>
              <button class="ql-list" value="ordered"></button>
              <button class="ql-list" value="bullet"></button>
            </span>
            <span class="ql-formats">
              <button class="ql-link"></button>
              <button class="ql-image"></button>
              <button class="ql-video"></button>
            </span>
            <span class="ql-formats">
              <button class="ql-clean"></button>
            </span>
          </div>
          <div ref="editorRef" class="rich-editor quill-editor"></div>
          <input
            ref="inlineImageInputRef"
            type="file"
            class="cover-file-input"
            accept="image/jpeg,image/jpg,image/png,.jpg,.jpeg,.png"
            @change="onInlineImageSelected"
          />
          <input
            ref="inlineVideoInputRef"
            type="file"
            class="cover-file-input"
            accept="video/mp4,video/webm,video/ogg,.mp4,.webm,.ogg"
            @change="onInlineVideoSelected"
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
          <span class="stat-item">字数：{{ contentWordCount }}</span>
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
        <div class="preview-content" v-if="postContent" v-html="postContent"></div>
        <div class="preview-content" v-else>（暂无正文）</div>
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
import { ref, shallowRef, onMounted, onBeforeUnmount, computed, watch } from 'vue'
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
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

const route = useRoute()
const router = useRouter()
const isDark = useDark()

const SECTION_KEYS = ['knowledge', 'q-a', 'notes']

const SECTION_LABELS = {
  knowledge: '知识分享区域',
  'q-a': '问答区域',
  help: '问答区域',
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
const editorRef = shallowRef()
const quillRef = shallowRef(null)
const inlineImageInputRef = ref(null)
const inlineVideoInputRef = ref(null)
const coverUploading = ref(false)
const coverUploadPercent = ref(0)
const coverDragOver = ref(false)

const lastSavedAt = ref(null)
const savePending = ref(false)
const previewVisible = ref(false)
const historyVisible = ref(false)
const draftsDrawerVisible = ref(false)
const historyList = ref([])
const draftsList = ref([])

let autosaveTimer = null
const AUTOSAVE_MS = 800

const displayCoverUrl = computed(() => resolveMediaUrl(coverUrl.value))

let syncingEditor = false

const sectionLabel = (key) => SECTION_LABELS[key] || key || '未选择板块'

const pad2 = (n) => String(n).padStart(2, '0')

function formatHistoryTime(ts) {
  if (!ts) return ''
  const then = new Date(ts)
  const now = new Date()
  if (then.toDateString() === now.toDateString()) {
    return `今天 ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
  }
  return `${then.getFullYear()}-${pad2(then.getMonth() + 1)}-${pad2(then.getDate())} ${pad2(then.getHours())}:${pad2(then.getMinutes())}`
}

const canSaveDraft = computed(() => !!(postTitle.value.trim() || postContent.value.trim()))
const contentWordCount = computed(() => getPlainText(postContent.value).length)

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

watch(
  postContent,
  () => {
    syncEditorFromContent()
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
  const c = getPlainText(h.content || '')
  const head = t ? `标题：${t.slice(0, 24)}${t.length > 24 ? '…' : ''}` : ''
  const body = c ? `正文：${c.slice(0, 48)}${c.length > 48 ? '…' : ''}` : ''
  return [head, body].filter(Boolean).join(' · ') || '（空内容）'
}

function applyEditorPayload(p) {
  postTitle.value = p.title ?? ''
  postContent.value = p.content ?? ''
  postSection.value = p.section === 'help' ? 'q-a' : (SECTION_KEYS.includes(p.section) ? p.section : 'knowledge')
  coverUrl.value = p.cover ?? ''
  nextTickResizeTitle()
}
function getPlainText(html) {
  const div = document.createElement('div')
  div.innerHTML = String(html || '')
  return (div.textContent || div.innerText || '').replace(/\s+/g, ' ').trim()
}

const syncEditorFromContent = () => {
  if (!quillRef.value || syncingEditor) return
  const current = quillRef.value.root.innerHTML
  const next = postContent.value || ''
  if (current === next) return
  syncingEditor = true
  quillRef.value.clipboard.dangerouslyPasteHTML(next || '<p><br></p>')
  syncingEditor = false
}

const uploadInlineMedia = async (file, type) => {
  if (!file || !quillRef.value) return
  const fd = new FormData()
  fd.append('file', file)
  const res = await request.post('/api/file/upload', fd)
  if (res.data?.code !== 200 || !res.data?.data?.url) {
    throw new Error(res.data?.msg || `${type === 'image' ? '图片' : '视频'}上传失败`)
  }
  const url = resolveMediaUrl(res.data.data.url)
  const range = quillRef.value.getSelection(true)
  const index = range?.index ?? quillRef.value.getLength()
  quillRef.value.insertEmbed(index, type, url, 'user')
  quillRef.value.setSelection(index + 1, 0)
}

const onInlineImageSelected = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  try {
    await uploadInlineMedia(file, 'image')
  } catch (err) {
    ElMessage.error(err?.message || '图片上传失败')
  } finally {
    if (inlineImageInputRef.value) inlineImageInputRef.value.value = ''
  }
}

const onInlineVideoSelected = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  try {
    await uploadInlineMedia(file, 'video')
  } catch (err) {
    ElMessage.error(err?.message || '视频上传失败')
  } finally {
    if (inlineVideoInputRef.value) inlineVideoInputRef.value.value = ''
  }
}

const initQuillEditor = () => {
  if (!editorRef.value) return
  quillRef.value = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: '请输入正文（支持图片、视频、标题、列表、引用、代码块）',
    modules: {
      toolbar: '#quill-toolbar'
    }
  })

  const toolbar = quillRef.value.getModule('toolbar')
  toolbar.addHandler('image', () => inlineImageInputRef.value?.click())
  toolbar.addHandler('video', () => inlineVideoInputRef.value?.click())

  const toolbarEl = document.getElementById('quill-toolbar')
  const localizePicker = (rootSelector, mapping, fallbackText) => {
    const picker = toolbarEl?.querySelector(rootSelector)
    if (!picker) return
    const labelEl = picker.querySelector('.ql-picker-label')
    const currentVal = labelEl?.getAttribute('data-value') || ''
    const currentText = mapping[currentVal] || fallbackText
    if (labelEl && currentText) {
      labelEl.setAttribute('data-label', currentText)
    }
    picker.querySelectorAll('.ql-picker-item').forEach((item) => {
      const key = item.getAttribute('data-value') || ''
      const text = mapping[key] || fallbackText
      item.setAttribute('data-label', text)
    })
  }

  localizePicker('.ql-header', { '': '正文', '1': '标题 1', '2': '标题 2' }, '正文')
  localizePicker('.ql-size', { '': '默认', small: '小', large: '大', huge: '超大' }, '默认')

  const setTitle = (selector, title) => toolbarEl?.querySelector(selector)?.setAttribute('title', title)
  setTitle('.ql-bold', '加粗')
  setTitle('.ql-italic', '斜体')
  setTitle('.ql-underline', '下划线')
  setTitle('.ql-strike', '删除线')
  setTitle('.ql-blockquote', '引用')
  setTitle('.ql-code-block', '代码块')
  setTitle('.ql-list[value="ordered"]', '有序列表')
  setTitle('.ql-list[value="bullet"]', '无序列表')
  setTitle('.ql-link', '插入链接')
  setTitle('.ql-image', '上传图片')
  setTitle('.ql-video', '上传视频')
  setTitle('.ql-clean', '清除格式')
  toolbarEl?.querySelector('.ql-header')?.setAttribute('title', '标题级别')
  toolbarEl?.querySelector('.ql-size')?.setAttribute('title', '字号')

  quillRef.value.on('text-change', () => {
    if (syncingEditor) return
    postContent.value = quillRef.value.root.innerHTML
  })
  syncEditorFromContent()
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
  if (!getPlainText(postContent.value).trim()) {
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

  titleRef.value?.focus()
  if (titleRef.value) {
    titleRef.value.style.height = 'auto'
    titleRef.value.style.height = titleRef.value.scrollHeight + 'px'
  }
  initQuillEditor()
})

onBeforeUnmount(() => {
  if (autosaveTimer) clearTimeout(autosaveTimer)
  if (quillRef.value) quillRef.value.off('text-change')
  persistAutosave()
})
</script>
