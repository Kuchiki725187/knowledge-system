<script setup>
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

// avatar 为空时用图标头像作为默认头像
function avatarUrl() {
  return store.userInfo?.avatar || ''
}

function handleLogout() {
  store.logout()
  router.push('/login')
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">📚 知识管理系统</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item index="/plaza">知识大厅</el-menu-item>
        <el-menu-item index="/knowledge">我的知识</el-menu-item>
        <el-menu-item index="/category">分类管理</el-menu-item>
        <el-menu-item index="/tag">标签管理</el-menu-item>
        <el-menu-item index="/favorite">我的收藏</el-menu-item>
        <el-menu-item index="/history">最近浏览</el-menu-item>
        <el-menu-item index="/search">搜索</el-menu-item>
        <el-menu-item index="/stats">数据统计</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div></div>
        <div class="right">
          <span class="nickname">{{ store.userInfo?.nickname || store.userInfo?.username }}</span>
          <!-- 点击头像进入个人资料页 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <el-avatar :size="36" :src="avatarUrl()" class="avatar">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}
.right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nickname {
  font-size: 14px;
  color: #606266;
}
.avatar {
  cursor: pointer;
  background: #409eff;
  flex-shrink: 0;
}
.main {
  background: #f5f7fa;
}
</style>
