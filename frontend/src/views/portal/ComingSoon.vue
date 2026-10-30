<template>
  <div class="coming-soon page-container">
    <el-result icon="info" :title="title" sub-title="该模块将在后续阶段开放，当前阶段先完成车辆浏览与后台框架">
      <template #extra>
        <el-button type="primary" @click="router.push('/vehicles')">去选车</el-button>
        <el-button plain @click="router.push('/')">返回首页</el-button>
      </template>
      <template #default>
        <div class="hint">
          <p>已开放：注册 / 登录、车辆列表与筛选、车辆详情、后台动态菜单框架。</p>
          <p>后续计划：在线下单与信用押金计算（S2）、订单与信用中心（S3）、运营看板（S4）。</p>
        </div>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const title = computed(() => `${route.meta.title || '功能'} · 建设中`)
</script>

<style scoped>
.coming-soon {
  min-height: 62vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hint {
  max-width: 560px;
  margin: 0 auto;
  text-align: left;
  font-size: 13px;
  line-height: 1.9;
  color: #909399;
}

.hint p {
  margin: 4px 0;
}
</style>
