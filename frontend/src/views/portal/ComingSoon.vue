<template>
  <div class="coming-soon page-container">
    <el-result icon="info" :title="title" sub-title="该功能正在完善中，可以先浏览车辆或返回首页。">
      <template #extra>
        <el-button type="primary" @click="router.push('/vehicles')">去选车</el-button>
        <el-button plain @click="router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const title = computed(() => `${route.meta.title || '功能'} · 即将开放`)
</script>

<style scoped>
.coming-soon {
  min-height: 62vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
