<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMe, updateMe, uploadAvatar } from '../api/user'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  oldPassword: '',
  newPassword: '',
  avatar: '',
})

// 载入当前用户信息(store 里没有则拉接口)
onMounted(async () => {
  loading.value = true
  try {
    const info = store.userInfo || (await getMe())
    store.setUserInfo(info)
    form.username = info.username || ''
    form.nickname = info.nickname || ''
    form.avatar = info.avatar || ''
  } finally {
    loading.value = false
  }
})

// 头像上传(OSS):传完只拿到 URL,随表单一起保存
async function doUpload(options) {
  uploading.value = true
  try {
    form.avatar = await uploadAvatar(options.file)
    options.onSuccess()
    ElMessage.success('头像已上传，保存后生效')
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

async function handleSave() {
  // 新密码填了但没填旧密码 → 提示(旧密码验证是安全底线)
  if (form.newPassword && !form.oldPassword) {
    ElMessage.warning('修改密码需要填写原密码')
    return
  }
  saving.value = true
  try {
    const payload = {
      username: form.username,
      nickname: form.nickname,
      avatar: form.avatar || null,
    }
    if (form.newPassword) {
      payload.oldPassword = form.oldPassword
      payload.password = form.newPassword
    }
    const info = await updateMe(payload)
    store.setUserInfo(info)
    // 改过密码:提示重新登录(安全习惯)
    if (form.newPassword) {
      ElMessageBox.alert('密码已修改，请重新登录', '提示', { type: 'success' }).then(() => {
        store.logout()
        window.location.href = '/login'
      })
    } else {
      ElMessage.success('保存成功')
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div v-loading="loading" class="profile">
    <h2>个人资料</h2>

    <!-- 头像区 -->
    <div class="avatar-row">
      <el-avatar :size="80" :src="form.avatar || ''" class="avatar-preview">
        <el-icon :size="40"><UserFilled /></el-icon>
      </el-avatar>
      <div class="avatar-upload">
        <el-upload
          :show-file-list="false"
          :http-request="doUpload"
          accept=".jpg,.jpeg,.png,.gif,.webp"
        >
          <el-button size="small" type="primary" :loading="uploading">上传头像</el-button>
        </el-upload>
        <div class="tip">支持 jpg/png/gif/webp，不超过 2MB</div>
      </div>
    </div>

    <el-form label-width="90px" style="max-width: 480px">
      <el-form-item label="用户名">
        <el-input v-model="form.username" maxlength="32" placeholder="3~32 位字母或数字" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="form.nickname" maxlength="32" placeholder="显示名称" />
      </el-form-item>

      <el-divider content-position="left">修改密码（留空则不修改）</el-divider>
      <el-form-item label="原密码">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="验证身份" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6~64 位" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.profile {
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  max-width: 720px;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin: 16px 0 24px;
}
.avatar-preview {
  background: #409eff;
  color: #fff;
  flex-shrink: 0;
}
.tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 6px;
}
</style>
