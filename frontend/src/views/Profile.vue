<template>
  <div class="profile-container">
    <div class="glass-card profile-header">
      <div class="avatar-section">
        <el-avatar :size="120" :src="form.avatar" class="profile-avatar" />
        <el-button class="change-avatar-btn" round @click="uploadVisible = true">更换头像</el-button>
      </div>
      <div class="info-section">
        <h2>{{ form.nickname }}</h2>
        <p>{{ form.signature || '暂无个性签名' }}</p>
      </div>
    </div>

    <div class="glass-card form-section">
      <h3>基本资料</h3>
      <el-form :model="form" label-position="top" size="large">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入您的昵称" />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="form.signature" type="textarea" :rows="3" placeholder="写一句话介绍自己..." />
        </el-form-item>
        <div class="form-actions">
          <el-button type="primary" size="large" @click="save" :loading="saving" class="save-btn">保存修改</el-button>
        </div>
      </el-form>
    </div>

    <!-- 头像裁剪弹窗 -->
    <el-dialog v-model="uploadVisible" title="更换头像" width="600px" align-center destroy-on-close>
      <div class="upload-container">
        <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/jpeg,image/png,image/jpg"
            :on-change="handleFileChange"
            class="avatar-uploader"
        >
          <el-button type="primary">选择本地图片</el-button>
          <template #tip>
            <div class="el-upload__tip">支持 JPG/PNG 格式，建议尺寸 500x500 以上</div>
          </template>
        </el-upload>

        <div v-if="previewUrl" class="cropper-area">
          <img ref="cropImage" :src="previewUrl" class="image-to-crop" />
        </div>
        <div v-else class="placeholder-area">
          <el-icon :size="48" color="#dcdfe6"><Picture /></el-icon>
          <p>请选择一张图片进行裁剪</p>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="uploadVisible = false">取消</el-button>
          <el-button type="primary" @click="applyAvatar" :disabled="!previewUrl">确认使用</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import axios from 'axios'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const form = reactive({
  id: null,
  nickname: '',
  signature: '',
  avatar: ''
})

const uploadVisible = ref(false)
const previewUrl = ref('')
const cropImage = ref(null)
const saving = ref(false)
let cropper = null

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      form.id = user.id
      form.nickname = user.nickname || '未命名用户'
      form.signature = user.signature || ''
      form.avatar = user.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    } catch (e) {
      console.error(e)
    }
  }
})

const handleFileChange = (file) => {
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('请上传图片文件')
    return
  }

  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }

  previewUrl.value = URL.createObjectURL(file.raw)

  nextTick(() => {
    if (cropper) {
      cropper.destroy()
    }
    if (cropImage.value) {
      cropper = new Cropper(cropImage.value, {
        aspectRatio: 1,
        viewMode: 1,
        dragMode: 'move',
        autoCropArea: 1,
        background: false
      })
    }
  })
}

const applyAvatar = () => {
  if (!cropper) return

  cropper.getCroppedCanvas({
    width: 300,
    height: 300,
    imageSmoothingQuality: 'high'
  }).toBlob((blob) => {
    const reader = new FileReader()
    reader.readAsDataURL(blob)
    reader.onloadend = () => {
      form.avatar = reader.result
      uploadVisible.value = false
      ElMessage.success('头像已裁剪，请点击保存修改')
    }
  }, 'image/jpeg', 0.9)
}

const save = async () => {
  saving.value = true
  try {
    const fullUrl = 'http://localhost:8080/api/user/update'
    await axios.post(fullUrl, form)

    // 更新本地存储
    const userStr = localStorage.getItem('user')
    let user = userStr ? JSON.parse(userStr) : {}
    user = { ...user, ...form }
    localStorage.setItem('user', JSON.stringify(user))

    // 触发更新事件
    window.dispatchEvent(new CustomEvent('user-updated', { detail: user }))

    ElMessage.success('个人资料已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 清理资源
onUnmounted(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  if (cropper) {
    cropper.destroy()
  }
})
</script>

<style scoped>
.profile-container {
  max-width: 1000px; /* 增加宽度 */
  margin: 40px auto;
  display: flex;
  flex-direction: column;
  gap: 32px; /* 增加间距 */
  padding: 0 20px;
}

.glass-card {
  background: var(--apple-card-bg);
  backdrop-filter: blur(20px);
  border: 1px solid var(--apple-glass-border);
  border-radius: 24px; /* 更大的圆角 */
  box-shadow: var(--apple-shadow);
}

.profile-header {
  padding: 48px; /* 增加内边距 */
  display: flex;
  align-items: center;
  gap: 48px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.change-avatar-btn {
  font-weight: 500;
}

.info-section h2 {
  margin: 0 0 12px 0;
  font-size: 32px; /* 更大的标题 */
  font-weight: 700;
  color: var(--apple-text-primary);
}

.info-section p {
  margin: 0;
  font-size: 18px; /* 更大的正文 */
  color: var(--apple-text-secondary);
  line-height: 1.5;
}

.form-section {
  padding: 48px;
}

.form-section h3 {
  margin: 0 0 32px 0;
  font-size: 24px;
  font-weight: 600;
}

.form-actions {
  margin-top: 40px;
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  padding: 12px 40px;
  font-size: 16px;
  border-radius: 999px;
}

/* 裁剪器样式 */
.upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.cropper-area {
  width: 100%;
  height: 400px;
  background: var(--apple-bg);
  border-radius: 12px;
  overflow: hidden;
}

.image-to-crop {
  display: block;
  max-width: 100%;
}

.placeholder-area {
  width: 100%;
  height: 300px;
  background: var(--apple-bg);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--apple-text-secondary);
  gap: 12px;
}

/* 覆盖 Element Plus 样式以匹配 Apple 风格 */
:deep(.el-form-item__label) {
  font-size: 16px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 10px;
}

:deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.1) inset;
}

:deep(.el-input__inner) {
  font-size: 16px;
  height: 40px;
}

:deep(.el-textarea__inner) {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 16px;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.1) inset;
}
</style>