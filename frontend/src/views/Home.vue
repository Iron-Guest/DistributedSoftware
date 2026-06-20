<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const user = ref(null)
const activeMenu = ref('index')

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  } else {
    router.push('/login')
  }
})

watch(() => route.path, (newPath) => {
  if (newPath.includes('/home/index') || newPath.includes('/home/goods')) {
    activeMenu.value = 'index'
  } else if (newPath.includes('/home/orders')) {
    activeMenu.value = 'orders'
  } else if (newPath.includes('/home/admin')) {
    activeMenu.value = 'admin'
  }
}, { immediate: true })

const handleMenuSelect = (index) => {
  if (index === 'index') {
    router.push('/home/index')
  } else if (index === 'orders') {
    router.push('/home/orders')
  } else if (index === 'admin') {
    router.push('/home/admin')
  }
}

const handleLogout = () => {
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<template>
  <div class="home-container">
    <el-container>
      <el-header class="header">
        <span class="logo" @click="router.push('/home/index')">电商秒杀平台</span>
        <div class="header-right">
          <span class="welcome">欢迎，{{ user?.username }}</span>
          <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="sidebar">
          <el-menu
            :default-active="activeMenu"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409eff"
            @select="handleMenuSelect"
          >
            <el-menu-item index="index">
              <el-icon><Goods /></el-icon>
              <span>商品列表</span>
            </el-menu-item>
            <el-menu-item index="orders">
              <el-icon><Tickets /></el-icon>
              <span>我的订单</span>
            </el-menu-item>
            <el-menu-item index="admin">
              <el-icon><Setting /></el-icon>
              <span>后台管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #409eff;
  color: #fff;
  padding: 0 20px;
  height: 60px;
}

.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome {
  font-size: 14px;
}

.sidebar {
  background-color: #304156;
  min-height: calc(100vh - 60px);
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>