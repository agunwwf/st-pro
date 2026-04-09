<template>
  <div class="chat-layout glass-card">
    <!-- 左侧侧边栏 -->
    <aside class="chat-sidebar" :class="{ collapsed: sidebarMode === 'collapsed' }">
      <div class="sidebar-header">
        <h3 class="title-clickable" @click="toggleMessageMode">{{ sidebarMode === 'friends' ? '我的好友' : '消息' }}</h3>
        <div class="header-actions">
          <el-button
            circle
            size="large"
            @click="toggleListMode"
            class="icon-btn"
            :title="sidebarMode === 'friends' ? '切换为消息对话列表' : '切换为好友列表'"
          >
            <el-icon class="list-toggle-icon"><Tickets /></el-icon>
          </el-button>
          <!-- 好友申请提示 -->
          <el-badge :value="pendingRequests.length" :hidden="pendingRequests.length === 0" class="request-badge">
            <el-button circle size="large" @click="showRequests = true" class="icon-btn">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
        </div>
      </div>

      <div class="search-bar" v-if="sidebarMode !== 'collapsed'">
        <el-input
            v-model="friendKeyword"
            size="large"
            placeholder="搜索用户ID添加好友"
            @keyup.enter="searchAndAdd"
            class="search-input"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>

      <div class="friend-list" v-if="sidebarMode !== 'collapsed'">
        <div
            v-for="f in displayedContacts"
            :key="f.id"
            class="friend-item"
            :class="{ active: activeFriend && activeFriend.id === f.id }"
            @click="selectFriend(f)"
            @contextmenu.prevent="openFriendContextMenu($event, f)"
        >
          <div class="avatar-wrapper">
            <el-avatar :size="54" :src="f.friendAvatar || defaultAvatar" shape="square" class="avatar-img" @click.stop="showFriendProfile(f)" />
            <span v-if="f.friendIsModel === 1 || f.friendIsModel === true" class="model-star-badge">★</span>
            <span class="status-badge online"></span>
          </div>
          <div class="friend-info">
            <div class="name-row">
              <span class="nickname">{{ f.friendNickname || f.friendUsername }}</span>
              <el-tag v-if="f.tempSession" size="small" type="info" effect="plain">临时会话</el-tag>
            </div>
            <div class="msg-preview" v-if="!f.friendNickname">
              <span class="account">ID: {{ f.friendUsername }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧聊天区域 -->
    <section class="chat-wrapper">
      <div class="chat-header">
        <div class="contact-info" v-if="activeFriend">
          <div class="header-text">
            <h3>{{ activeFriend.friendNickname || activeFriend.friendUsername }}</h3>
            <span class="status-text">
              <span class="status-dot-small online"></span> 在线
            </span>
          </div>
        </div>
        <div class="chat-header-actions" v-if="activeFriend">
          <el-dropdown trigger="click">
            <el-button circle class="icon-btn" title="会话设置">
              <el-icon><Setting /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="confirmClearMyConversation" class="danger-item">删除所有聊天记录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-else class="contact-info empty">
          <span>选择一个好友开始聊天</span>
        </div>
      </div>

      <div class="messages-area" ref="msgArea">
        <div v-if="!activeFriend" class="empty-state">
          <el-icon :size="80" color="#E5E5EA"><ChatDotRound /></el-icon>
          <p>未选择聊天</p>
        </div>

        <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="{ 'mine': msg.fromUser === currentUser.username }"
        >
          <el-avatar v-if="msg.fromUser !== currentUser.username" :size="44" :src="activeFriend.friendAvatar || defaultAvatar" class="avatar" shape="square" />
          <div class="bubble-container">
            <div class="meta" v-if="msg.fromUser !== currentUser.username">
              <span class="nick">{{ activeFriend.friendNickname }}</span>
            </div>
            <div class="bubble">
              <template v-if="msg.msgType === 'image'">
                <img :src="resolveUploadUrl(msg.content)" alt="图片" class="image-message" />
              </template>
              <template v-else-if="msg.msgType === 'file'">
                <a class="file-link" :href="resolveUploadUrl(msg.content)" :download="msg.fileName" target="_blank">
                  <el-icon class="file-icon"><Folder /></el-icon>
                  <span class="file-name">{{ msg.fileName || '文件附件' }}</span>
                </a>
              </template>
              <template v-else>
                {{ msg.content }}
              </template>
            </div>
            <span class="time">{{ formatTime(msg.createTime) }}</span>
          </div>
        </div>
      </div>

      <div class="input-area" v-if="activeFriend">
        <div class="toolbar">
          <div class="tool-btn" @click="triggerImageSelect" title="发送图片">
            <el-icon><Picture /></el-icon>
          </div>
          <div class="tool-btn" @click="triggerFileSelect" title="发送文件">
            <el-icon><Folder /></el-icon>
          </div>
          <input ref="imageInput" type="file" accept="image/*" class="hidden-input" @change="handleImageSelected" />
          <input ref="fileInput" type="file" class="hidden-input" @change="handleFileSelected" />
        </div>

        <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            @keydown.enter.prevent="handleEnter"
            class="chat-input"
            resize="none"
        />
        <div class="send-bar">
          <span class="tip">按 Enter 发送</span>
          <el-button type="primary" size="large" round @click="send" :disabled="!inputText.trim()">发送</el-button>
        </div>
      </div>
    </section>

    <!-- 好友申请弹窗 -->
    <el-dialog v-model="showRequests" title="好友申请" width="450px" align-center class="custom-dialog">
      <div v-if="pendingRequests.length === 0" class="empty-requests">暂无新的好友申请</div>
      <div v-else class="request-list">
        <div v-for="req in pendingRequests" :key="req.id" class="request-item">
          <el-avatar :size="48" :src="req.friendAvatar || defaultAvatar" />
          <div class="req-info">
            <span class="name">{{ req.friendNickname }}</span>
            <span class="account">ID: {{ req.friendUsername }}</span>
          </div>
          <el-button type="primary" @click="acceptFriend(req)">接受</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 右键好友菜单 -->
    <div
      v-if="contextMenuVisible"
      class="friend-context-menu"
      :style="{ left: `${contextMenuPos.x}px`, top: `${contextMenuPos.y}px` }"
    >
      <div class="ctx-item danger" @click="deleteFriendFromContext">删除好友</div>
    </div>

    <!-- 好友信息小浮窗 -->
    <el-dialog v-model="friendProfileVisible" title="好友信息" width="420px" align-center class="custom-dialog">
      <div v-if="profileFriend" class="friend-profile-box">
        <div class="fp-avatar-wrap">
          <el-avatar :size="72" :src="profileFriend.friendAvatar || defaultAvatar" shape="square" />
          <span v-if="profileFriend.friendIsModel === 1 || profileFriend.friendIsModel === true" class="model-star-badge big">★</span>
        </div>
        <div class="friend-profile-meta">
          <div class="fp-name">{{ profileFriend.friendNickname || profileFriend.friendUsername }}</div>
          <div class="fp-id">账号：{{ profileFriend.friendUsername || '-' }}</div>
          <div class="fp-sign">个人简介：{{ profileFriend.friendSignature || '这个人很神秘，暂未填写简介。' }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, reactive, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ChatDotRound, Bell, Picture, Folder, Tickets, Setting } from '@element-plus/icons-vue'
import request from '@/utils/request'
window.axios = request

// --- 状态管理 ---
const route = useRoute()
const inputText = ref('')
const msgArea = ref(null)
const messages = ref([])
const friends = ref([])
const tempContacts = ref([])
const activeFriend = ref(null)
const friendKeyword = ref('')
const pendingRequests = ref([])
const showRequests = ref(false)
const sidebarMode = ref('messages')
const friendProfileVisible = ref(false)
const profileFriend = ref(null)
const contextMenuVisible = ref(false)
const contextMenuPos = ref({ x: 0, y: 0 })
const contextMenuFriend = ref(null)
let socket = null
let socketConnecting = false
let reconnectTimer = null
let reconnectAttempt = 0

// 默认头像（如果好友没有头像则使用）
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
const allContacts = computed(() => [...tempContacts.value, ...friends.value])
const displayedContacts = computed(() => {
  if (sidebarMode.value === 'friends') return friends.value
  return allContacts.value
})

// 文件上传 Ref
const imageInput = ref(null)
const fileInput = ref(null)

// 当前用户
const currentUser = reactive({
  id: null,
  username: '',
  nickname: '',
  avatar: ''
})

// --- WebSocket 连接 ---
const connectWebSocket = () => {
  // 避免重复连接
  if (socket && socket.readyState === WebSocket.OPEN) return
  if (socketConnecting) return
  socketConnecting = true

  const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${proto}//${window.location.hostname}:8080/ws/chat`
  socket = new WebSocket(wsUrl)
  socket.onopen = () => {
    socketConnecting = false
    reconnectAttempt = 0
    // 发送身份认证包，绑定 Session
    const token = localStorage.getItem('token') || ''
    socket.send(JSON.stringify({ type: 'auth', token }))
  }
  socket.onerror = () => {
    socketConnecting = false
    ElMessage.error('聊天连接异常，请稍后重试或刷新页面')
  }
  socket.onclose = () => {
    socketConnecting = false
    socket = null
    // 自动重连，避免上传/网络抖动导致断线
    if (!reconnectTimer) {
      const delay = Math.min(15000, 1000 * Math.pow(2, reconnectAttempt++))
      reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null
        connectWebSocket()
      }, delay)
    }
  }
  socket.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'message') {
        // 如果正在和这个人聊天，直接追加消息
        if (activeFriend.value && data.from === activeFriend.value.friendUsername) {
          messages.value.push({
            id: Date.now(),
            fromUser: data.from,
            toUser: currentUser.username,
            content: data.content,
            msgType: data.msgType || 'text',
            fileName: data.fileName,
            createTime: new Date()
          })
          scrollToBottom()
        } else {
          ElMessage.info(`收到来自 ${data.nickname || data.from} 的新消息`)
        }
      }
    } catch (e) {
      console.error(e)
    }
  }
}

// --- 初始化与 API ---

const initData = async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const u = JSON.parse(userStr)
    Object.assign(currentUser, u)
    await loadFriends()
    await loadRequests()
    connectWebSocket()


    const targetFromRoute = route.query.target ? String(route.query.target) : ''
    const targetFromStorage = localStorage.getItem('chat_active_friend') || ''
    const targetUsername = targetFromRoute || targetFromStorage
    if (targetUsername) {
      // 禁止自己和自己会话
      if (targetUsername === currentUser.username) {
        localStorage.removeItem('chat_active_friend')
        ElMessage.warning('不能和自己聊天')
        return
      }
      let target = friends.value.find(f => f.friendUsername === targetUsername)
      const allowTempSession =
        targetFromRoute &&
        route.query.temp === '1' &&
        route.query.source === 'teacher-admin' &&
        currentUser.role === 'ADMIN'
      if (!target && allowTempSession) {
        const temp = {
          id: `temp-${targetUsername}`,
          friendId: null,
          friendUsername: targetUsername,
          friendNickname: route.query.targetNick || targetUsername,
          friendAvatar: route.query.targetAvatar || defaultAvatar,
          tempSession: true
        }
        tempContacts.value = [temp]
        target = temp
      }
      if (!target && targetFromStorage) {
        localStorage.removeItem('chat_active_friend')
      }
      if (target) {
        selectFriend(target)
      }
    }
  } else {
    ElMessage.error('请先登录')
  }
}

const toggleListMode = () => {
  sidebarMode.value = sidebarMode.value === 'friends' ? 'messages' : 'friends'
}

const toggleMessageMode = () => {
  sidebarMode.value = sidebarMode.value === 'messages' ? 'collapsed' : 'messages'
}

const loadFriends = async () => {
  try {
    const res = await axios.get(`/api/chat/friends?userId=${currentUser.id}`)
    if (res.data.code === 200) {
      friends.value = (res.data.data || []).filter(f => f.friendUsername)
      if (activeFriend.value && !friends.value.some(f => f.friendId === activeFriend.value.friendId)) {
        activeFriend.value = null
        messages.value = []
      }
    }
  } catch (e) {
    console.error('加载好友失败', e)
  }
}

const loadRequests = async () => {
  try {
    const res = await axios.get(`/api/chat/friend/requests?userId=${currentUser.id}`)
    if (res.data.code === 200) {
      pendingRequests.value = res.data.data
    }
  } catch (e) {
    console.error('加载申请失败', e)
  }
}

const searchAndAdd = async () => {
  if (!friendKeyword.value) return
  if (friendKeyword.value === currentUser.username) return ElMessage.warning("不能添加自己为好友")

  try {
    const searchRes = await axios.get(`/api/chat/search?keyword=${friendKeyword.value}`)
    // 注意：后端现在返回的是 List<User>
    if (searchRes.data.code !== 200 || !searchRes.data.data || searchRes.data.data.length === 0) {
      return ElMessage.error('未找到该用户')
    }

    // 默认取第一个匹配的用户
    const targetUser = searchRes.data.data[0]

    await ElMessageBox.confirm(`确定要添加 ${targetUser.nickname} (ID: ${targetUser.username}) 为好友吗?`, '添加好友', {
      confirmButtonText: '发送申请',
      cancelButtonText: '取消',
      type: 'info'
    })

    const reqRes = await axios.post('/api/chat/friend/request', {
      userId: currentUser.id,
      friendUsername: targetUser.username
    })

    if (reqRes.data.code === 200) {
      ElMessage.success('好友申请已发送')
      friendKeyword.value = ''
    } else {
      ElMessage.error(reqRes.data.msg)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const acceptFriend = async (req) => {
  try {
    const res = await axios.post('/api/chat/friend/accept', {
      // 后端按 Friend 对象接收：userId 为申请发起者，friendId 为接收方
      userId: req.userId,
      friendId: req.friendId
    })
    if (res.data.code === 200) {
      ElMessage.success('已添加好友')
      await loadRequests()
      await loadFriends()
      if (pendingRequests.value.length === 0) showRequests.value = false
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const selectFriend = async (f) => {
  contextMenuVisible.value = false
  if (!f?.friendUsername) return
  if (f.friendUsername === currentUser.username) {
    ElMessage.warning('不能和自己聊天')
    return
  }
  activeFriend.value = f
  localStorage.setItem('chat_active_friend', f.friendUsername)
  try {
    const res = await axios.get(`/api/chat/messages?user1=${currentUser.username}&user2=${f.friendUsername}`)
    if (res.data.code === 200) {
      messages.value = res.data.data
      scrollToBottom()
    }
  } catch (e) {
    console.error(e)
  }
}

const confirmClearMyConversation = async () => {
  if (!activeFriend.value?.friendUsername) return
  try {
    await ElMessageBox.confirm(
      `确定删除你与 ${activeFriend.value.friendNickname || activeFriend.value.friendUsername} 的所有聊天记录吗？`,
      '删除聊天记录确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (_) {
    return
  }
  try {
    const res = await axios.post('/api/chat/messages/clear', {
      peerUsername: activeFriend.value.friendUsername
    })
    if (res.data.code === 200) {
      messages.value = []
      ElMessage.success('已删除你自己的聊天记录')
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

// --- 消息发送逻辑 ---

const send = async () => {
  if (!inputText.value.trim()) return
  await sendMessage('text', inputText.value)
  inputText.value = ''
}

const sendMessage = async (type, content, fileName = null) => {
  if (!activeFriend.value) return
  if (activeFriend.value.friendUsername === currentUser.username) {
    ElMessage.warning('不能给自己发消息')
    return
  }
  // 先落库，保证刷新不丢（WS 只负责实时转发）
  try {
    const saveRes = await axios.post('/api/chat/message/save', {
      toUser: activeFriend.value.friendUsername,
      content,
      msgType: type,
      fileName
    })
    if (saveRes?.data?.code !== 200) {
      ElMessage.error(saveRes?.data?.msg || '消息保存失败')
      return
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('消息保存失败')
    return
  }

  const now = new Date()
  const payload = {
    type: 'message',
    msgType: type,
    from: currentUser.username,
    to: activeFriend.value.friendUsername,
    content: content,
    fileName: fileName,
    nickname: currentUser.nickname,
    avatar: currentUser.avatar
  }

  // WebSocket 发送 (用于实时转发；断线不影响落库)
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
  } else {
    connectWebSocket()
  }

  // 本地直接显示
  messages.value.push({
    id: Date.now(),
    fromUser: currentUser.username,
    toUser: activeFriend.value.friendUsername,
    content: content,
    msgType: type,
    fileName: fileName,
    createTime: now
  })

  scrollToBottom()
}

const handleEnter = (e) => {
  if (!e.shiftKey) send()
}

const showFriendProfile = (f) => {
  profileFriend.value = f
  friendProfileVisible.value = true
}

const openFriendContextMenu = (e, f) => {
  if (f?.tempSession) {
    contextMenuVisible.value = false
    return
  }
  contextMenuFriend.value = f
  contextMenuPos.value = { x: e.clientX, y: e.clientY }
  contextMenuVisible.value = true
}

const deleteFriend = async (f) => {
  if (!f) return
  await ElMessageBox.confirm(
    `确定删除好友 ${f.friendNickname || f.friendUsername} 吗？删除后聊天记录将不再显示。`,
    '删除好友确认',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
  )
  const res = await axios.post('/api/chat/friend/delete', {
    userId: currentUser.id,
    friendId: f.friendId
  })
  if (res.data.code === 200) {
    ElMessage.success('好友已删除')
    if (activeFriend.value?.friendId === f.friendId) {
      activeFriend.value = null
      messages.value = []
    }
    await loadFriends()
  } else {
    ElMessage.error(res.data.msg || '删除失败')
  }
}

const deleteFriendFromContext = async () => {
  const f = contextMenuFriend.value
  contextMenuVisible.value = false
  if (!f) return
  try {
    await deleteFriend(f)
  } catch (_) {
    // 用户取消无需提示
  }
}

const handleGlobalClick = () => {
  if (contextMenuVisible.value) contextMenuVisible.value = false
}

// --- 图片/文件处理 (保留原有逻辑) ---

const triggerImageSelect = () => imageInput.value?.click()
const triggerFileSelect = () => fileInput.value?.click()

const handleImageSelected = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return



  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await axios.post('/api/chat/upload', formData)
    if (res.data.code === 200 && res.data.data) {
      const { url } = res.data.data
      // 发送图片消息，content 仅为图片 URL
      sendMessage('image', url)
    } else {
      ElMessage.error(res.data.msg || '图片上传失败')
    }
  } catch (err) {
    console.error('图片上传失败', err)
    ElMessage.error('图片上传失败')
  }
}

const handleFileSelected = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return



  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await axios.post('/api/chat/upload', formData)
    if (res.data.code === 200 && res.data.data) {
      const { url, fileName } = res.data.data
      // 发送文件消息，content 为下载 URL，fileName 为原文件名
      sendMessage('file', url, fileName)
    } else {
      ElMessage.error(res.data.msg || '文件上传失败')
    }
  } catch (err) {
    console.error('文件上传失败', err)
    ElMessage.error('文件上传失败')
  }
}

// --- 辅助函数 ---

const scrollToBottom = () => {
  nextTick(() => {
    if (msgArea.value) msgArea.value.scrollTop = msgArea.value.scrollHeight
  })
}

const formatTime = (t) => {
  return new Date(t).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const resolveUploadUrl = (content) => {
  if (!content) return ''
  const s = String(content)
  // 已经是绝对 URL 则直接用
  if (/^https?:\/\//i.test(s)) return s
  const base = (axios?.defaults?.baseURL || '').replace(/\/$/, '')
  if (!base) return s
  return `${base}${s.startsWith('/') ? '' : '/'}${s}`
}

onMounted(() => {
  initData()
  window.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  if (socket) socket.close()
  if (reconnectTimer) window.clearTimeout(reconnectTimer)
  window.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
/* 基础布局 - 字体放大 */
.chat-layout {
  display: flex;
  height: 100%;
  background: #fff;
  font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 16px; /* 基础字号放大 */
}

/* 左侧侧边栏 */
.chat-sidebar {
  width: 360px; /* 宽度增加 */
  background: rgba(245, 245, 247, 0.85);
  backdrop-filter: blur(20px);
  border-right: 1px solid rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
}
.chat-sidebar.collapsed { width: 92px; }
.chat-sidebar.collapsed .sidebar-header { justify-content: center; padding: 20px 10px; }
.chat-sidebar.collapsed .sidebar-header h3 { display: none; }
.chat-sidebar.collapsed .header-actions { display: flex; gap: 8px; }

.sidebar-header {
  padding: 24px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 24px; /* 标题放大 */
  font-weight: 700;
  color: #1d1d1f;
}
.title-clickable { cursor: pointer; user-select: none; }
.chat-header-actions { margin-left: auto; }
.danger-item { color: #f56c6c; }
.list-toggle-icon {
  font-size: 20px;
  font-weight: 700;
}
.icon-btn :deep(.el-icon) {
  font-size: 21px; /* 图标整体略放大 */
  transform: scale(1.08);
}
.icon-btn :deep(svg) {
  stroke-width: 2.4; /* 线条更粗一点 */
}

.search-bar { padding: 0 20px 20px; }

.friend-list { flex: 1; overflow-y: auto; padding: 0 12px; }

.friend-item {
  display: flex;
  padding: 16px; /* 增加内边距 */
  border-radius: 14px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: all 0.2s;
}

.friend-item:hover { background: rgba(0,0,0,0.04); }
.friend-item.active { background: #0071e3; }
.friend-item.active .nickname, .friend-item.active .account { color: #fff; }

.avatar-wrapper { position: relative; margin-right: 16px; }
.avatar-img { border: 1px solid rgba(0,0,0,0.05); }
.model-star-badge {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #ffb020;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0,0,0,0.16);
}
.model-star-badge.big {
  width: 20px;
  height: 20px;
  line-height: 20px;
  font-size: 12px;
  right: -5px;
  bottom: -5px;
}

.friend-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.name-row { display: flex; align-items: center; gap: 8px; }
.name-row .el-tag {
  margin-left: auto;
  white-space: nowrap;
  flex-shrink: 0;
}
.name-row .nickname {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nickname { font-weight: 600; font-size: 18px; color: #1d1d1f; } /* 昵称放大 */
.account { font-size: 14px; color: #86868b; }

/* 右侧聊天区 */
.chat-wrapper { flex: 1; display: flex; flex-direction: column; background: #fff; }

.chat-header {
  height: 80px; /* 头部增高 */
  padding: 0 32px;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  display: flex;
  align-items: center;
}

.header-text h3 { margin: 0; font-size: 20px; font-weight: 600; }
.status-text { font-size: 14px; color: #86868b; display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.status-dot-small { width: 8px; height: 8px; border-radius: 50%; background: #34c759; }

.messages-area {
  flex: 1;
  padding: 30px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: #fff;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #c7c7cc;
  font-size: 18px;
}

.message-row { display: flex; gap: 16px; max-width: 75%; }
.message-row.mine { align-self: flex-end; flex-direction: row-reverse; }

.bubble-container { display: flex; flex-direction: column; gap: 6px; }
.message-row.mine .bubble-container { align-items: flex-end; }

.meta { font-size: 14px; color: #8e8e93; margin-left: 4px; }

.bubble {
  padding: 14px 20px; /* 气泡变大 */
  border-radius: 20px;
  font-size: 17px; /* 消息文字放大 */
  line-height: 1.5;
  background: #f2f2f7;
  color: #1d1d1f;
  position: relative;
  word-break: break-word;
}

.message-row.mine .bubble {
  background: #0071e3;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-row:not(.mine) .bubble {
  border-bottom-left-radius: 4px;
}

.time { font-size: 12px; color: #c7c7cc; margin: 0 6px; }

/* 图片消息 */
.image-message { max-width: 300px; border-radius: 12px; display: block; }

/* 输入区域 */
.input-area { padding: 20px 32px; border-top: 1px solid rgba(0,0,0,0.05); }

.toolbar { display: flex; gap: 20px; margin-bottom: 16px; }
.tool-btn {
  cursor: pointer;
  font-size: 24px;
  color: #86868b;
  transition: color 0.2s;
  padding: 4px;
}
.tool-btn:hover { color: #0071e3; }
.hidden-input { display: none; }

.chat-input :deep(.el-textarea__inner) {
  font-size: 17px; /* 输入框文字放大 */
  padding: 0;
  box-shadow: none;
  background: transparent;
}

.send-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.tip { font-size: 14px; color: #c7c7cc; }

/* 弹窗样式 */
.request-list { display: flex; flex-direction: column; gap: 12px; }
.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f5f5f7;
  border-radius: 12px;
}
.req-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.req-info .name { font-weight: 600; font-size: 16px; }
.req-info .account { font-size: 14px; color: #86868b; }
.empty-requests { text-align: center; color: #999; padding: 30px; font-size: 16px; }

/* 管理员仪表盘样式 */
.header-actions { display: flex; gap: 12px; }
.active-btn { background: #0071e3 !important; color: #fff !important; }
.list-label { padding: 12px 20px; font-size: 14px; color: #86868b; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; cursor: pointer; }
.content-wrapper { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.dashboard-wrapper { flex: 1; display: flex; flex-direction: column; background: #f5f5f7; padding: 32px; overflow-y: auto; }
.dashboard-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 32px; }
.header-left h2 { font-size: 32px; font-weight: 700; margin: 0 0 8px 0; color: #1d1d1f; }
.header-left p { font-size: 16px; color: #86868b; margin: 0; }
.dashboard-content { background: #fff; border-radius: 20px; padding: 24px; box-shadow: 0 4px 24px rgba(0,0,0,0.04); }
.student-cell { display: flex; align-items: center; gap: 12px; }
.student-meta { display: flex; flex-direction: column; }
.student-name { font-weight: 600; color: #1d1d1f; font-size: 16px; }
.student-id { font-size: 12px; color: #86868b; }
.model-tag { margin-left: 8px; border-radius: 6px; }
.progress-cell { padding-right: 20px; }
.check-in-text { font-weight: 600; color: #0071e3; }
.date-text { color: #86868b; font-size: 14px; }
.action-btns { display: flex; gap: 8px; }

.friend-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 130px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 10px 26px rgba(0, 0, 0, 0.12);
  border-radius: 10px;
  overflow: hidden;
}

.ctx-item {
  padding: 10px 14px;
  font-size: 14px;
  cursor: pointer;
  color: #374151;
}

.ctx-item:hover { background: #f3f4f6; }
.ctx-item.danger { color: #dc2626; }

.friend-profile-box {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.fp-avatar-wrap { position: relative; }

.friend-profile-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fp-name { font-size: 18px; font-weight: 700; color: #111827; }
.fp-id { font-size: 13px; color: #6b7280; }
.fp-sign { font-size: 14px; color: #374151; line-height: 1.6; }
</style>
