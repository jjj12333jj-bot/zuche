<template>
  <div class="placeholder-page">
    <div class="panel">
      <el-result
        icon="info"
        :title="title"
        sub-title="该功能正在完善中，可以先返回工作台，或前往车辆租赁浏览可租车辆。"
      >
        <template #extra>
          <el-button type="primary" @click="router.push(homePath)">返回工作台</el-button>
          <el-button plain @click="router.push('/vehicles')">查看前台车辆</el-button>
        </template>
      </el-result>
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

const title = computed(() => route.meta.title || '功能准备中')
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
  padding: 24px 28px 32px;
}
</style>
