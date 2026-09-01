<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2>注册账号</h2>
      <el-form @submit.prevent="handleSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名（3~32位字母或数字）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码（至少 6 位）"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.nickname" placeholder="昵称（选填）" size="large" />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          style="width: 100%"
          :loading="loading"
          @click="handleSubmit"
        >
          注 册
        </el-button>
        <div class="auth-footer">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.auth-card {
  width: 380px;
  padding: 12px 8px;
}
.auth-card h2 {
  text-align: center;
  margin: 8px 0 24px;
}
.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
}
</style>
