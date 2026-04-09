<template>
  <div class="profile-container">
    <div class="glass-card profile-header">
      <div class="avatar-section">
        <div class="avatar-badge-wrap">
          <el-avatar :size="100" :src="form.avatar" class="profile-avatar" @click="openAvatarPreview" />
          <span v-if="form.isModel === 1 || form.isModel === true" class="model-badge" title="模范学生">★</span>
        </div>
        <el-button class="change-avatar-btn" plain @click="startAvatarChange">更换头像</el-button>
      </div>
      <div class="info-section">
        <h2>{{ form.nickname || form.username || '用户' }}</h2>
        <p class="signature-display">{{ form.signature || '暂无个性签名' }}</p>
      </div>
    </div>

    <el-dialog v-model="avatarPreviewVisible" title="头像预览" width="420px" class="apple-dialog">
      <div class="avatar-preview-modal">
        <el-avatar :size="240" :src="form.avatar" />
      </div>
      <template #footer>
        <el-button @click="avatarPreviewVisible = false" round>关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadVisible" title="更换头像" width="520px" class="apple-dialog">
      <div class="avatar-upload" :class="{ cropping: !!tempAvatar }">
        <el-upload
          v-show="!tempAvatar"
          ref="uploadRef"
          class="avatar-uploader"
          drag
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          :on-change="handleAvatarChange"
        >
          <div class="upload-tip">
            <div style="font-weight: 700; margin-bottom: 6px;">点击或拖拽图片到此处</div>
            <div style="color:#86868b; font-size: 12px;">建议正方形图片，大小不超过 2MB</div>
          </div>
        </el-upload>

        <div class="avatar-crop" v-if="tempAvatar">
          <div class="preview-label">裁剪</div>
          <div class="cropper-box">
            <img ref="cropperImgRef" :src="tempAvatar" alt="avatar" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="uploadVisible = false" round>取消</el-button>
        <el-button v-if="tempAvatar" @click="triggerFilePick" round>重新选择</el-button>
        <el-button type="primary" @click="applyAvatar" round :disabled="!tempAvatar">裁剪并保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="460px" class="apple-dialog" @open="resetPasswordDialog">
      <div v-if="passwordStep === 1">
        <el-form label-position="top" size="large" class="apple-form">
          <el-form-item label="请填写旧密码（校验通过后进入下一步）">
            <el-input
              v-model="verifyForm.oldPassword"
              type="password"
              placeholder="请输入旧密码"
              autocomplete="new-password"
              name="old-password-manual"
            />
          </el-form-item>
        </el-form>
      </div>
      <div v-else>
        <el-form label-position="top" size="large" class="apple-form">
          <el-form-item label="新密码（6-12位）">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              maxlength="12"
              placeholder="请输入新密码"
              autocomplete="new-password"
              name="new-password-manual"
            />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              maxlength="12"
              placeholder="请再次输入新密码"
              autocomplete="new-password"
              name="confirm-password-manual"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="passwordDialogVisible = false" round>取消</el-button>
        <el-button v-if="passwordStep === 1" type="primary" :loading="verifyingOldPassword" @click="verifyOldPasswordAndNext" round>
          下一步
        </el-button>
        <el-button v-else @click="backToVerifyOldPassword" round>上一步</el-button>
        <el-button v-if="passwordStep === 2" type="warning" :loading="changingPassword" @click="changePassword" round>
          确认修改密码
        </el-button>
      </template>
    </el-dialog>

    <div class="glass-card form-section">
      <el-form :model="form" label-position="top" size="large" class="apple-form">
        <div class="section-group">
          <h3 class="section-title">账户基本信息</h3>
          <div class="kv-grid">
            <div class="kv-item">
              <div class="kv-label">账号ID</div>
              <div class="kv-value">{{ form.username || '-' }}</div>
            </div>
            <div class="kv-item">
              <div class="kv-label">IP属地</div>
              <div class="kv-value">{{ form.ipLocation || '-' }}</div>
            </div>
          </div>

          <el-form-item label="修改账号（每月最多5次）">
            <div class="inline-action">
              <el-input v-model="accountForm.newUsername" placeholder="请输入新账号" />
              <el-button type="primary" :loading="changingAccount" @click="changeAccount">确认修改</el-button>
            </div>
          </el-form-item>

          <el-form-item label="公开昵称"><el-input v-model="form.nickname" placeholder="请输入您的昵称" /></el-form-item>
          <el-form-item label="个性签名"><el-input v-model="form.signature" type="textarea" :rows="3" placeholder="用一句话描述自己..." resize="none" /></el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender" placeholder="请选择" style="width: 100%">
              <el-option label="保密" value="保密" />
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="请选择生日" style="width: 100%" />
          </el-form-item>
          <el-form-item label="地区">
            <el-input v-model="form.region" placeholder="如：广东省 深圳市" />
          </el-form-item>
        </div>

        <div class="form-divider"></div>

        <transition name="fade">
          <div v-if="userRole === 'ADMIN'" class="section-group">
            <h3 class="section-title">教师账户信息</h3>
            <el-form-item label="教师工号 / 登录账号">
              <el-input :value="form.username" disabled class="disabled-input" />
            </el-form-item>
          </div>
        </transition>

        <div class="form-actions">
          <el-button type="warning" plain size="large" @click="openPasswordDialog" :loading="changingPassword" round>
            修改密码
          </el-button>
          <el-button type="primary" size="large" @click="save" :loading="saving" class="save-btn" round>保存资料修改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import request from '@/utils/request'
window.axios = request

const userRole = ref('STUDENT')
const uploadVisible = ref(false)
const tempAvatar = ref('')
const cropperImgRef = ref(null)
let cropper = null
const uploadRef = ref(null)
const avatarPreviewVisible = ref(false)

const openAvatarPreview = () => {
  avatarPreviewVisible.value = true
}

const triggerFilePick = async () => {
  await nextTick()
  const el = uploadRef.value?.$el
  const input = el ? el.querySelector('input[type="file"]') : null
  if (input) input.click()
  else ElMessage.error('未找到文件选择器，请刷新重试')
}

const startAvatarChange = async () => {
  tempAvatar.value = ''
  uploadVisible.value = true
  await triggerFilePick()
}

const form = reactive({
  id: null,
  username: '',
  nickname: '',
  signature: '',
  avatar: '',
  gender: '保密',
  birthday: '',
  region: '',
  ipLocation: '',
  isModel: 0
})
const saving = ref(false)
const changingAccount = ref(false)
const changingPassword = ref(false)
const verifyingOldPassword = ref(false)
const passwordDialogVisible = ref(false)
const passwordStep = ref(1)
const accountForm = reactive({
  newUsername: ''
})
const verifyForm = reactive({
  oldPassword: ''
})
const passwordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

const handleAvatarChange = async (uploadFile) => {
  const file = uploadFile?.raw
  if (!file) {
    ElMessage.error('未获取到图片文件')
    return
  }
  const maxBytes = 2 * 1024 * 1024
  if (file.size > maxBytes) {
    ElMessage.error('图片过大，请选择不超过 2MB 的图片')
    return
  }
  try {
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
    tempAvatar.value = String(dataUrl)
  } catch (e) {
    ElMessage.error('读取图片失败')
  }
}

const applyAvatar = () => {
  if (!tempAvatar.value) return
  if (!cropper) return ElMessage.error('裁剪器未就绪')
  try {
    const canvas = cropper.getCroppedCanvas({ width: 256, height: 256, imageSmoothingEnabled: true })
    const dataUrl = canvas.toDataURL('image/png')
    form.avatar = dataUrl
    uploadVisible.value = false
    // 直接保存到后端，避免刷新回默认头像
    save()
  } catch (e) {
    ElMessage.error('裁剪失败')
  }
}

watch([uploadVisible, tempAvatar], async () => {
  if (!uploadVisible.value) {
    if (cropper) { cropper.destroy(); cropper = null }
    return
  }
  if (!tempAvatar.value) return
  await nextTick()
  if (!cropperImgRef.value) return
  if (cropper) { cropper.destroy(); cropper = null }
  cropper = new Cropper(cropperImgRef.value, {
    aspectRatio: 1,
    viewMode: 1,
    autoCropArea: 1,
    background: false,
    movable: true,
    zoomable: true,
    scalable: false,
    rotatable: false,
    responsive: true,
  })
})

onMounted(async () => {
  // 以 /api/user/me 为准，保证字段齐全（含 IP属地）
  try {
    const res = await axios.get('/api/user/me')
    if (res.data.code === 200) {
      const u = res.data.data || {}
      form.id = u.id
      form.username = u.username || ''
      form.nickname = u.nickname || ''
      form.signature = u.signature || ''
      form.avatar = u.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
      form.gender = u.gender || '保密'
      form.birthday = u.birthday || ''
      form.region = normalizeRegion(u.region)
      form.ipLocation = u.ipLocation || ''
      form.isModel = u.isModel || 0
      userRole.value = u.role || 'STUDENT'
    }
  } catch (e) {}
})

const normalizeRegion = (val) => {
  const s = String(val || '').trim()
  // 兜底：避免历史脏数据把手机号显示在“地区”
  if (/^\d{7,}$/.test(s)) return ''
  return s
}

const save = async () => {
  saving.value = true
  try {
    // 统一走前端 request（携带 JWT），避免硬编码 localhost 导致线上/CORS 问题
    const res = await axios.post('/api/user/update', form)
    if (res.data.code === 200) {
      // 刷新本地缓存用户信息
      try {
        const me = await axios.get('/api/user/me')
        if (me.data.code === 200) {
          const user = me.data.data || {}
          localStorage.setItem('user', JSON.stringify(user))
          window.dispatchEvent(new CustomEvent('user-updated', { detail: user }))
        }
      } catch (e) {}
      ElMessage.success('资料保存成功')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const changeAccount = async () => {
  const nextName = (accountForm.newUsername || '').trim()
  if (!nextName) return ElMessage.warning('请输入新账号')
  if (nextName === form.username) return ElMessage.warning('新账号不能与当前账号一致')
  changingAccount.value = true
  try {
    const res = await axios.post('/api/user/account/change', { newUsername: nextName })
    if (res.data.code === 200) {
      ElMessage.success('账号修改成功，请重新登录')
      try {
        const me = await axios.get('/api/user/me')
        if (me.data.code === 200) {
          const user = me.data.data || {}
          localStorage.setItem('user', JSON.stringify(user))
          form.username = user.username || form.username
          form.nickname = user.nickname || form.nickname
        }
      } catch (e) {}
      accountForm.newUsername = ''
    } else {
      ElMessage.error(res.data.msg || '账号修改失败')
    }
  } catch (e) {
    ElMessage.error('账号修改失败')
  } finally {
    changingAccount.value = false
  }
}

const openPasswordDialog = () => {
  passwordDialogVisible.value = true
  resetPasswordDialog()
}

const backToVerifyOldPassword = () => {
  passwordStep.value = 1
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const resetPasswordDialog = () => {
  passwordStep.value = 1
  verifyForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const verifyOldPasswordAndNext = async () => {
  if (!verifyForm.oldPassword) return ElMessage.warning('请输入旧密码')
  verifyingOldPassword.value = true
  try {
    const res = await axios.post('/api/user/password/verify', {
      oldPassword: verifyForm.oldPassword
    })
    if (res.data.code === 200) {
      ElMessage.success('旧密码校验通过')
      passwordStep.value = 2
    } else {
      ElMessage.error(res.data.msg || '旧密码错误')
    }
  } catch (e) {
    ElMessage.error('旧密码校验失败')
  } finally {
    verifyingOldPassword.value = false
  }
}

const changePassword = async () => {
  if (!verifyForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    return ElMessage.warning('请完整填写密码信息')
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 12) {
    return ElMessage.warning('新密码必须为6-12位')
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  changingPassword.value = true
  try {
    const res = await axios.post('/api/user/password/change', {
      oldPassword: verifyForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    if (res.data.code === 200) {
      ElMessage.success('密码修改成功')
      verifyForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordDialogVisible.value = false
      passwordStep.value = 1
    } else {
      ElMessage.error(res.data.msg || '密码修改失败')
    }
  } catch (e) {
    ElMessage.error('密码修改失败')
  } finally {
    changingPassword.value = false
  }
}
</script>

<style scoped>
/* 样式保留你的极简纯净风 */
.profile-container { max-width: 800px; margin: 40px auto; display: flex; flex-direction: column; gap: 24px; padding: 0 20px; animation: fadeIn 0.4s ease; }
.glass-card { background: #ffffff; border: 1px solid #e5e5ea; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.03); }
.profile-header { padding: 40px; display: flex; align-items: center; gap: 32px; }
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.avatar-badge-wrap { position: relative; display: inline-flex; }
.profile-avatar { border: 2px solid #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.08); cursor: pointer; }
.model-badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #ffb020;
  color: #fff;
  font-size: 13px;
  line-height: 22px;
  text-align: center;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.change-avatar-btn {
  color: #606266 !important;
  height: 32px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: transparent;
  border: 1px solid #dcdfe6;
  box-shadow: none;
}
.change-avatar-btn:hover { background: #f5f7fa; border-color: #c0c4cc; color: #606266 !important; }
.change-avatar-btn:active { background: #eef2f6; }
.avatar-preview-modal { display: flex; justify-content: center; padding: 8px 0 16px 0; }
.avatar-upload { display: flex; gap: 16px; align-items: flex-start; }
.avatar-upload.cropping { justify-content: center; }
.avatar-uploader { flex: 1; }
.upload-tip { padding: 18px 0; }
.avatar-crop { width: 220px; display: flex; flex-direction: column; align-items: center; gap: 10px; margin: 0 auto; }
.cropper-box { width: 220px; height: 220px; border-radius: 12px; overflow: hidden; border: 1px solid #e5e5ea; background: #f5f5f7; }
.cropper-box img { display: block; max-width: 100%; }
.preview-label { font-size: 12px; color: #86868b; font-weight: 700; }
.kv-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-bottom: 18px; }
.kv-item { background: #fbfcfe; border: 1px solid #e5e5ea; border-radius: 10px; padding: 12px; }
.kv-label { font-size: 12px; color: #86868b; margin-bottom: 4px; }
.kv-value { font-size: 14px; color: #1d1d1f; font-weight: 700; word-break: break-all; }
.info-section h2 { margin: 0 0 6px 0; font-size: 26px; font-weight: 700; color: #1d1d1f; }
.signature-display { margin: 0; font-size: 14px; color: #86868b; line-height: 1.5; }
.form-section { padding: 40px; }
.section-group { margin-bottom: 32px; }
.section-title { margin: 0 0 20px 0; font-size: 17px; font-weight: 600; color: #1d1d1f; }
.form-divider { height: 1px; background: #e5e5ea; margin: 0 0 32px 0; }
.form-actions { margin-top: 40px; display: flex; justify-content: flex-end; gap: 10px; }
.inline-action { display: grid; grid-template-columns: 1fr auto; gap: 10px; width: 100%; }
:deep(.el-form-item__label) { font-size: 14px; color: #1d1d1f; font-weight: 500; margin-bottom: 8px !important; padding: 0 !important; }
:deep(.el-input__wrapper), :deep(.el-textarea__inner) { border-radius: 8px; box-shadow: none !important; background: #fbfcfe; border: 1px solid #d2d2d7; }
:deep(.el-input__wrapper.is-focus), :deep(.el-textarea__inner:focus) { border-color: #0071e3 !important; background: #fff; }
</style>