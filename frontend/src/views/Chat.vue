<template>
  <div class="chat-layout glass-card">
    <!-- 左侧侧边栏 -->
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h3>消息</h3>
        <div class="header-actions">
          <!-- 好友申请提示 -->
          <el-badge :value="pendingRequests.length" :hidden="pendingRequests.length === 0" class="request-badge">
            <el-button circle size="large" @click="showRequests = true" class="icon-btn">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
        </div>
      </div>

      <div class="search-bar">
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

      <div class="friend-list">
        <div
            v-for="f in friends"
            :key="f.id"
            class="friend-item"
            :class="{ active: activeFriend && activeFriend.id === f.id }"
            @click="selectFriend(f)"
        >
          <div class="avatar-wrapper">
            <el-avatar :size="54" :src="f.friendAvatar || defaultAvatar" shape="square" class="avatar-img" />
            <span class="status-badge online"></span>
          </div>
          <div class="friend-info">
            <div class="name-row">
              <span class="nickname">{{ f.friendNickname || f.friendUsername }}</span>
            </div>
            <div class="msg-preview">
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
                <img :src="msg.content" alt="图片" class="image-message" />
              </template>
              <template v-else-if="msg.msgType === 'file'">
                <a class="file-link" :href="msg.content" :download="msg.fileName" target="_blank">
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ChatDotRound, Bell, Picture, Folder } from '@element-plus/icons-vue'
import axios from 'axios'

// --- 状态管理 ---
const route = useRoute()
const inputText = ref('')
const msgArea = ref(null)
const messages = ref([])
const friends = ref([])
const activeFriend = ref(null)
const friendKeyword = ref('')
const pendingRequests = ref([])
const showRequests = ref(false)
let socket = null
let socketConnecting = false

// 默认头像（如果好友没有头像则使用）
const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

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

  socket = new WebSocket('ws://localhost:8080/ws/chat')
  socket.onopen = () => {
    socketConnecting = false
    // 发送身份认证包，绑定 Session
    socket.send(JSON.stringify({ type: 'auth', from: currentUser.username }))
  }
  socket.onerror = () => {
    socketConnecting = false
    ElMessage.error('聊天连接异常，请稍后重试或刷新页面')
  }
  socket.onclose = () => {
    socketConnecting = false
    socket = null
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


    const targetUsername = route.query.target || localStorage.getItem('chat_active_friend')
    if (targetUsername) {
      const target = friends.value.find(f => f.friendUsername === targetUsername)
      if (target) {
        selectFriend(target)
      }
    }
  } else {
    ElMessage.error('请先登录')
  }
}

const loadFriends = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/chat/friends?userId=${currentUser.id}`)
    if (res.data.code === 200) friends.value = res.data.data
  } catch (e) {
    console.error('加载好友失败', e)
  }
}

const loadRequests = async () => {
  try {
    const res = await axios.get(`http://localhost:8080/api/chat/friend/requests?userId=${currentUser.id}`)
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
    const searchRes = await axios.get(`http://localhost:8080/api/chat/search?keyword=${friendKeyword.value}`)
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

    const reqRes = await axios.post('http://localhost:8080/api/chat/friend/request', {
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
    const res = await axios.post('http://localhost:8080/api/chat/friend/accept', {
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
  activeFriend.value = f
  localStorage.setItem('chat_active_friend', f.friendUsername)
  try {
    const res = await axios.get(`http://localhost:8080/api/chat/messages?user1=${currentUser.username}&user2=${f.friendUsername}`)
    if (res.data.code === 200) {
      messages.value = res.data.data
      scrollToBottom()
    }
  } catch (e) {
    console.error(e)
  }
}

// --- 消息发送逻辑 ---

const send = () => {
  if (!inputText.value.trim()) return
  sendMessage('text', inputText.value)
  inputText.value = ''
}

const sendMessage = (type, content, fileName = null) => {
  if (!activeFriend.value) return
  if (!socket || socket.readyState !== WebSocket.OPEN) {
    ElMessage.error('聊天连接已断开，请刷新页面重新进入聊天')
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

  // WebSocket 发送 (用于实时转发)
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
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
    const res = await axios.post('http://localhost:8080/api/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200 && res.data.data) {
      const { url } = res.data.data
      // 发送图片消息，content 仅为图片 URL
      sendMessage('image', `http://localhost:8080${url}`)
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
    const res = await axios.post('http://localhost:8080/api/chat/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.code === 200 && res.data.data) {
      const { url, fileName } = res.data.data
      // 发送文件消息，content 为下载 URL，fileName 为原文件名
      sendMessage('file', `http://localhost:8080${url}`, fileName)
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

onMounted(() => {
  initData()
})

onUnmounted(() => {
  if (socket) socket.close()
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

.friend-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
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
</style>
