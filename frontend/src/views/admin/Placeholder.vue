<template>
  <div class="placeholder-page">
    <div class="panel">
      <el-result icon="info" :title="title" :sub-title="subTitle">
        <template #extra>
          <el-button type="primary" @click="router.push(homePath)">返回工作台</el-button>
          <el-button plain @click="router.push('/vehicles')">查看前台车辆</el-button>
        </template>
      </el-result>

      <div class="info">
        <div class="info-row">
          <span>当前路由</span>
          <code>{{ route.path }}</code>
        </div>
        <div class="info-row">
          <span>菜单来源</span>
          <code>sys_menu + sys_role_menu（按当前角色授权）</code>
        </div>
        <div class="info-row">
          <span>组件路径</span>
          <code>{{ component || '尚未实现' }}</code>
        </div>
      </div>

      <el-alert
        class="stage-tip"
        type="info"
        :closable="false"
        show-icon
        :title="stageTip"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const homePath = computed(() => (userStore.isAdmin ? '/admin/dashboard' : '/store/overview'))

const trail = computed(() => {
  const prefix = userStore.isAdmin ? '/admin' : '/store'
  const find = (nodes) => {
    for (const node of nodes) {
      if (node.path === route.path) return node
      if (node.children?.length) {
        const hit = find(node.children)
        if (hit) return hit
      }
    }
    return null
  }
  return find(userStore.menus.filter((m) => m.path?.startsWith(prefix) || m.children?.length)) || null
})

const title = computed(() => trail.value?.menuName || route.meta.title || '模块建设中')

const component = computed(() => trail.value?.component || '')

const homePathLabel = computed(() => (userStore.isAdmin ? '系统管理' : '网点管理'))

const subTitle = computed(() =>
  userStore.isAdmin
    ? '该后台模块尚未在 S1 实现，菜单已由数据库动态下发，页面将在后续阶段替换。'
    : '网点工作台框架已搭好，网点概览、车辆与订单处理将在 S3 阶段实现。'
)

const stageTip = computed(() => {
  const p = route.path
  if (p.includes('/credit')) return '计划 S3：信用规则配置、用户信用调整、信用日志与风控效果对比。'
  if (p.includes('/dispatch')) return '计划 S3：网点发起调车申请，系统管理员审批后变更车辆归属网点。'
  if (p.includes('/orders')) return '计划 S3：订单状态机、网点交付与归还、押金退还处理。'
  if (p.includes('/vehicle') || p.includes('/store/vehicles')) return '计划 S3：车辆资产 CRUD、图片、保养、保险、违章台账。'
  if (p.includes('/store/overview')) return '计划 S3：本网点车辆、订单、待处理与在租统计。'
  return `该模块属于 ${homePathLabel.value} 后续迭代范围，当前仅完成动态菜单与路由占位。`
})
</script>

<style scoped>
.placeholder-page {
  min-height: calc(100vh - 100px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 30px;
}

.panel {
  width: 100%;
  max-width: 720px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 10px 28px 24px;
}

.info {
  background: #fafbfc;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #606266;
}

.info-row span {
  width: 70px;
  color: #909399;
  flex-shrink: 0;
}

.info-row code {
  font-size: 12px;
  color: #1f3a5f;
  background: #eef2f6;
  padding: 2px 8px;
  border-radius: 4px;
  word-break: break-all;
}

.stage-tip {
  margin-top: 16px;
}
</style>
