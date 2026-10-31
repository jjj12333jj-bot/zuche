<template>
  <div class="portal-layout">
    <header class="header">
      <div class="page-container header-inner">
        <div class="logo" @click="router.push('/')">
          <el-icon :size="22"><Van /></el-icon>
          <span>微租车</span>
        </div>

        <nav class="nav">
          <router-link
            v-for="item in navs"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions">
          <template v-if="userStore.isLogin">
            <el-tag
              v-if="userStore.userInfo?.creditLevel"
              class="credit-tag"
              effect="plain"
              type="success"
              size="small"
              @click="router.push('/credit')"
            >
              信用 {{ userStore.userInfo.creditScore }} · {{ levelLabel }}
            </el-tag>

            <el-button
              v-if="!userStore.isUser"
              size="small"
              plain
              type="primary"
              @click="goWorkspace"
            >
              {{ userStore.isAdmin ? '系统管理台' : '网点工作台' }}
            </el-button>

            <el-dropdown @command="onCommand">
              <span class="user-entry" :title="`当前标签页：${userStore.displayName} · ${userStore.roleLabel}`">
                <el-avatar :size="28" :src="resolveImage(userStore.userInfo?.avatar, DEFAULT_AVATAR)">
                  {{ (userStore.userInfo?.realName || 'U').slice(0, 1) }}
                </el-avatar>
                <span class="user-name">{{ userStore.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="credit">信用中心</el-dropdown-item>
                  <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                  <el-dropdown-item v-if="!userStore.isUser" command="workspace">
                    {{ userStore.isAdmin ? '系统管理台' : '网点工作台' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="newTab" divided>新标签页登录其他账号</el-dropdown-item>
                  <el-dropdown-item command="logout">退出当前账号</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">免费注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <div class="page-container">
        微租车 · 基于信用风控的汽车租赁平台 &nbsp;|&nbsp; 支付、短信、地图均为模拟实现 &nbsp;|&nbsp; 毕业设计演示
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { openLoginInNewTab } from '@/utils/authSession'
import { resolveImage, DEFAULT_AVATAR } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navs = [
  { path: '/', label: '首页' },
  { path: '/vehicles', label: '车辆租赁' },
  { path: '/orders', label: '我的订单' },
  { path: '/credit', label: '信用中心' }
]

const LEVEL_MAP = {
  EXCELLENT: '优秀',
  GOOD: '良好',
  NORMAL: '一般',
  WATCH: '待观察'
}

const levelLabel = computed(() => LEVEL_MAP[userStore.userInfo?.creditLevel] || '未知')

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(`${path}/`)
}

function goWorkspace() {
  router.push(userStore.isAdmin ? '/admin/dashboard' : '/store/overview')
}

function onCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm(
      `将退出「${userStore.displayName}」账号，仅当前标签页受影响，其他标签页保持登录。`,
      '退出登录',
      { type: 'warning' }
    )
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      })
      .catch(() => {})
  } else if (command === 'credit') {
    router.push('/credit')
  } else if (command === 'orders') {
    router.push('/orders')
  } else if (command === 'workspace') {
    goWorkspace()
  } else if (command === 'newTab') {
    if (!openLoginInNewTab()) {
      ElMessage.warning('浏览器拦截了新窗口，请手动新建标签页访问 /login')
    }
  }
}

onMounted(() => {
  // 信用分是实时值，刷新页面后后台同步一次，避免展示旧分数
  if (userStore.token) {
    userStore.refresh().catch(() => {})
  }
})
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-inner {
  height: 62px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f3a5f;
  cursor: pointer;
  white-space: nowrap;
}

.nav {
  display: flex;
  gap: 26px;
  flex: 1;
}

.nav-item {
  font-size: 14px;
  color: #606266;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
}

.nav-item:hover,
.nav-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.credit-tag {
  cursor: pointer;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.main {
  flex: 1;
}

.footer {
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 18px 0;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

@media (max-width: 860px) {
  .header-inner {
    gap: 16px;
  }
  .nav {
    gap: 14px;
  }
  .credit-tag,
  .user-name {
    display: none;
  }
}
</style>
