<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '230px'" class="aside">
      <div class="brand" :class="{ mini: isCollapse }">
        <el-icon :size="24"><Van /></el-icon>
        <div v-show="!isCollapse" class="brand-text">
          <b>微租车</b>
          <span>{{ isAdmin ? '系统管理台' : '网点工作台' }}</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          unique-opened
          background-color="#1f2d3d"
          text-color="#bfcbd9"
          active-text-color="#7fd4a8"
        >
          <SidebarItem v-for="menu in menuTree" :key="menu.id" :item="menu" />
        </el-menu>

        <div v-if="!menuTree.length && !isCollapse" class="menu-empty">
          暂无菜单权限，请检查角色授权
        </div>
      </el-scrollbar>

      <div class="aside-foot">
        <el-button text class="back-portal" @click="router.push('/')">
          <el-icon><Back /></el-icon>
          <span v-show="!isCollapse">返回前台</span>
        </el-button>
      </div>
    </el-aside>

    <el-container class="right">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>

          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: homePath }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-for="node in trail" :key="node.id">
              {{ node.menuName }}
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="!trail.length">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag
            size="small"
            effect="plain"
            :type="isAdmin ? 'primary' : 'warning'"
            :title="`当前标签页登录：${userStore.displayName} · ${userStore.roleLabel}（与其他标签页互不影响）`"
          >
            {{ isAdmin ? '系统管理员' : '网点管理员' }}
          </el-tag>

          <el-dropdown @command="onCommand">
            <span class="user-entry" :title="`当前标签页：${userStore.displayName} · ${userStore.roleLabel}`">
              <el-avatar :size="28" :src="resolveImage(userStore.userInfo?.avatar, DEFAULT_AVATAR)">
                {{ userStore.displayName.slice(0, 1) }}
              </el-avatar>
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="portal">前台首页</el-dropdown-item>
                <el-dropdown-item command="refresh">刷新用户信息</el-dropdown-item>
                <el-dropdown-item command="newTab" divided>新标签页登录其他账号</el-dropdown-item>
                <el-dropdown-item command="logout">退出当前账号</el-dropdown-item>
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

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { openLoginInNewTab } from '@/utils/authSession'
import { filterMenuByPrefix, findMenuTrail } from '@/utils/menu'
import { resolveImage, DEFAULT_AVATAR } from '@/utils/image'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)

const isAdmin = computed(() => userStore.isAdmin)
const prefix = computed(() => (isAdmin.value ? '/admin' : '/store'))
const homePath = computed(() => (isAdmin.value ? '/admin/dashboard' : '/store/overview'))

/** 侧边栏只展示当前工作台前缀下的菜单，避免系统管理员看到前台菜单 */
const menuTree = computed(() => filterMenuByPrefix(userStore.menus, prefix.value))

/** 面包屑：按当前路由在菜单树中找链路 */
const trail = computed(() => findMenuTrail(menuTree.value, route.path) || [])

function onCommand(command) {
  if (command === 'portal') {
    router.push('/')
  } else if (command === 'refresh') {
    userStore
      .refresh()
      .then(() => ElMessage.success('用户信息已刷新'))
      .catch(() => {})
  } else if (command === 'newTab') {
    if (!openLoginInNewTab()) {
      ElMessage.warning('浏览器拦截了新窗口，请手动新建标签页访问 /login')
    }
  } else if (command === 'logout') {
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
  }
}

onMounted(() => {
  if (!userStore.menuLoaded) {
    userStore.loadMenus().catch(() => {})
  }
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.aside {
  background: #1f2d3d;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}

.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  background: #1a2634;
  flex-shrink: 0;
  overflow: hidden;
}

.brand.mini {
  justify-content: center;
  padding: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-text b {
  font-size: 16px;
  letter-spacing: 1px;
}

.brand-text span {
  font-size: 11px;
  color: #7f8fa4;
}

.menu-scroll {
  flex: 1;
}

.menu-scroll :deep(.el-menu) {
  border-right: none;
}

.menu-empty {
  padding: 18px;
  font-size: 12px;
  color: #7f8fa4;
  line-height: 1.8;
}

.aside-foot {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 8px 10px;
  flex-shrink: 0;
}

.back-portal {
  width: 100%;
  color: #bfcbd9;
  justify-content: flex-start;
}

.back-portal:hover {
  color: #7fd4a8;
}

.right {
  background: #f5f7fa;
  min-width: 0;
}

.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.breadcrumb {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 13px;
  color: #303133;
}

.main {
  padding: 18px;
  overflow: auto;
}

@media (max-width: 760px) {
  .user-name {
    display: none;
  }
  .breadcrumb {
    display: none;
  }
}
</style>
