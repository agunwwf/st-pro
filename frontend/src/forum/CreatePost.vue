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
        <span class="save-status">最近保存 刚刚</span>
        <el-button class="draft-btn" link>草稿箱</el-button>
        <el-dropdown trigger="click">
          <el-icon class="more-icon"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item style="font-size: 20px">预览</el-dropdown-item>
              <el-dropdown-item style="font-size: 20px">历史版本</el-dropdown-item>
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

          <div class="setting-item">
            <span class="setting-label">添加封面</span>
            <div class="cover-upload-box" @click="handleUploadCover">
              <div class="upload-placeholder" v-if="!coverUrl">
                <el-icon><Plus /></el-icon>
                <span>添加文章封面</span>
              </div>
              <img v-else :src="coverUrl" class="cover-preview" />
            </div>
          </div>
          <div class="setting-hint">图片上传格式支持 JPEG、JPG、PNG</div>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDark } from '@vueuse/core'
import request from '@/utils/request'
import {
  ArrowLeft, MoreFilled, Picture, VideoCamera, Link, ChatLineSquare,
  Plus, ArrowDown, QuestionFilled, CircleCheck
} from '@element-plus/icons-vue'

// 引入样式
import './CreatePost.scss'

const router = useRouter()
const isDark = useDark()

const postTitle = ref('')
const postContent = ref('')
const postSection = ref('knowledge')
const coverUrl = ref('')
const publishing = ref(false)
const titleRef = ref(null)

const goBack = () => {
  router.push('/forum')
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

    const res = await request.post('/api/forum/posts', {
      userId,
      title: postTitle.value,
      content: postContent.value,
      section: postSection.value,
      cover: coverUrl.value || `https://picsum.photos/seed/${Date.now()}/800/450`
    })

    if (res.data?.code === 200) {
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
  // 自动聚焦标题
  titleRef.value?.focus()
  // 初始调整高度
  if (titleRef.value) {
    titleRef.value.style.height = 'auto'
    titleRef.value.style.height = titleRef.value.scrollHeight + 'px'
  }
})
</script>
