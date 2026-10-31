<template>
  <div v-loading="loading" class="detail-page">
    <div v-if="detail" class="page-container content">
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/vehicles' }">车辆租赁</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail.brandName }} {{ detail.modelName }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div class="detail-grid">
        <section class="gallery">
          <div class="main-image">
            <img :src="resolveImage(mainImage)" :alt="detail.modelName" @error="onImageError" />
            <span class="status" :class="statusClass">{{ detail.statusName }}</span>
          </div>
          <div v-if="images.length > 1" class="thumb-list">
            <div
              v-for="img in images"
              :key="img.id || img.imageUrl"
              class="thumb"
              :class="{ active: mainImage === img.imageUrl }"
              @click="mainImage = img.imageUrl"
            >
              <img :src="resolveImage(img.imageUrl)" :alt="img.imageType" @error="onImageError" />
            </div>
          </div>
        </section>

        <aside class="booking">
          <div class="title-block">
            <h1>{{ detail.brandName }} {{ detail.modelName }}</h1>
            <p class="subtitle">{{ detail.description }}</p>
          </div>

          <div class="tag-row">
            <el-tag
              size="small"
              effect="plain"
              :style="{ color: detail.categoryColor, borderColor: detail.categoryColor }"
            >
              {{ detail.categoryName }}
            </el-tag>
            <el-tag size="small" effect="plain" type="info">{{ detail.seats }} 座</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ detail.gearboxName }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ detail.fuelTypeName }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ detail.displacement }}</el-tag>
          </div>

          <div class="price-panel">
            <div class="price-line">
              <span class="currency">¥</span>
              <span class="price">{{ formatMoney(detail.finalPrice) }}</span>
              <span class="unit">/天</span>
              <span v-if="hasDiscount" class="origin">原价 ¥{{ formatMoney(detail.dailyPrice) }}</span>
              <el-tag v-if="hasDiscount" size="small" type="danger" effect="dark">
                {{ discountText }}
              </el-tag>
            </div>
            <div class="deposit-line">
              标准押金 <b>¥{{ formatMoney(detail.deposit) }}</b>
              <span class="text-muted">（下单时按信用等级系数计算）</span>
            </div>
          </div>

          <div v-if="userStore.isLogin" class="credit-box">
            <div class="credit-head">
              <span>我的信用</span>
              <el-tag size="small" :color="levelColor" effect="dark">{{ levelLabel }}</el-tag>
            </div>
            <div class="credit-score">
              <b>{{ userStore.userInfo?.creditScore }}</b>
              <span>分</span>
              <span class="text-muted">信用等级越高，押金系数越低</span>
            </div>
          </div>
          <el-alert
            v-else
            class="login-tip"
            type="info"
            :closable="false"
            show-icon
            title="登录后可查看信用等级与专属押金"
          >
            新用户注册即得 100 信用分（良好档），按时归还可持续加分。
          </el-alert>

          <el-button type="primary" size="large" class="rent-btn" @click="onRent">
            立即租赁
          </el-button>
          <p class="rent-note">押金按信用等级折扣收取，还车结算后原路退回</p>

          <div class="meta-list">
            <div class="meta-item">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ detail.storeName }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Location /></el-icon>
              <span>{{ detail.storeAddress }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Phone /></el-icon>
              <span>{{ detail.storePhone }}</span>
            </div>
          </div>
        </aside>
      </div>

      <div class="info-grid">
        <section class="card">
          <h3 class="card-title">车辆参数</h3>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="品牌">{{ detail.brandName }}</el-descriptions-item>
            <el-descriptions-item label="车型">{{ detail.modelName }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.categoryName }}</el-descriptions-item>
            <el-descriptions-item label="座位数">{{ detail.seats }} 座</el-descriptions-item>
            <el-descriptions-item label="变速箱">{{ detail.gearboxName }}</el-descriptions-item>
            <el-descriptions-item label="能源类型">{{ detail.fuelTypeName }}</el-descriptions-item>
            <el-descriptions-item label="排量/电池">{{ detail.displacement }}</el-descriptions-item>
            <el-descriptions-item label="当前里程">{{ detail.currentMileage }} 万公里</el-descriptions-item>
            <el-descriptions-item label="车牌号">{{ detail.licensePlate }}</el-descriptions-item>
            <el-descriptions-item label="车身颜色">{{ detail.color }}</el-descriptions-item>
            <el-descriptions-item label="资产编号">{{ detail.vehicleNo }}</el-descriptions-item>
            <el-descriptions-item label="上牌日期">{{ detail.registerDate }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="card">
          <h3 class="card-title">取车网点</h3>
          <div class="store-name">{{ detail.storeName }}</div>
          <el-descriptions :column="1" size="small">
            <el-descriptions-item label="地址">{{ detail.storeAddress }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ detail.storePhone }}</el-descriptions-item>
            <el-descriptions-item label="营业时间">{{ detail.businessHours }}</el-descriptions-item>
            <el-descriptions-item label="支持">
              <el-tag size="small" effect="plain" type="success">异地还车</el-tag>
              <el-tag size="small" effect="plain" type="info" class="ml-6">多网点调度</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            class="map-note"
            type="info"
            :closable="false"
            title="到店取车请携带本人身份证与驾驶证，网点营业时间内均可办理"
          />
        </section>
      </div>

      <section class="card intro-card">
        <h3 class="card-title">车型介绍</h3>
        <p class="intro-text">{{ detail.modelDescription || detail.description }}</p>
        <div class="intro-tags">
          <span>累计出租 {{ detail.rentCount }} 次</span>
          <span>浏览 {{ detail.viewCount }} 次</span>
          <span>车况：良好</span>
          <span>手续：齐全</span>
        </div>
      </section>
    </div>

    <div v-else-if="!loading" class="empty-wrap">
      <el-empty description="车辆不存在或已下架">
        <el-button type="primary" @click="router.push('/vehicles')">返回车辆列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVehicleDetail } from '@/api/vehicle'
import { useUserStore } from '@/store/user'
import { resolveImage, onImageError } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detail = ref(null)
const mainImage = ref('')

const LEVEL_MAP = {
  EXCELLENT: { label: '优秀', color: '#67c23a' },
  GOOD: { label: '良好', color: '#409eff' },
  NORMAL: { label: '一般', color: '#e6a23c' },
  WATCH: { label: '待观察', color: '#f56c6c' }
}

const levelLabel = computed(() => LEVEL_MAP[userStore.userInfo?.creditLevel]?.label || '未知')
const levelColor = computed(() => LEVEL_MAP[userStore.userInfo?.creditLevel]?.color || '#909399')

const images = computed(() => {
  if (!detail.value) return []
  if (detail.value.images?.length) return detail.value.images
  return detail.value.coverImage ? [{ imageUrl: detail.value.coverImage, imageType: 'EXTERIOR' }] : []
})

const hasDiscount = computed(() => Number(detail.value?.discountRate) < 1)

const discountText = computed(() => {
  const rate = Number(detail.value?.discountRate || 1)
  return `${(rate * 10).toFixed(1)} 折`
})

const statusClass = computed(() => {
  const map = { 1: 'ok', 2: 'busy', 3: 'repair', 0: 'off' }
  return map[detail.value?.status] || 'off'
})

function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  detail.value = null
  try {
    const res = await getVehicleDetail(id)
    detail.value = res.data
    mainImage.value = res.data?.images?.[0]?.imageUrl || res.data?.coverImage || ''
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

function onRent() {
  if (!userStore.isLogin) {
    ElMessage.info('请先登录，登录后可查看专属押金')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (detail.value?.status !== 1) {
    ElMessage.warning(`该车辆当前为「${detail.value?.statusName || '不可租'}」状态，换一台试试`)
    return
  }
  router.push(`/checkout/${detail.value.id}`)
}

onMounted(loadDetail)
watch(() => route.params.id, loadDetail)
</script>

<style scoped>
.detail-page {
  min-height: 70vh;
  padding: 20px 0 52px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.breadcrumb {
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 22px;
  align-items: start;
}

.gallery {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px;
}

.main-image {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #eef2f6;
  border-radius: 10px;
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.status {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 4px 11px;
  border-radius: 12px;
  font-size: 12px;
  color: #fff;
}

.status.ok {
  background: rgba(103, 194, 58, 0.94);
}

.status.busy {
  background: rgba(230, 162, 60, 0.94);
}

.status.repair {
  background: rgba(245, 108, 108, 0.94);
}

.status.off {
  background: rgba(144, 147, 153, 0.94);
}

.thumb-list {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.thumb {
  width: 108px;
  aspect-ratio: 16 / 10;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #eef2f6;
}

.thumb.active {
  border-color: #409eff;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.booking {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 22px;
  position: sticky;
  top: 82px;
}

.title-block h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.subtitle {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 16px 0;
}

.price-panel {
  background: #fdf6f6;
  border: 1px solid #fde2e2;
  border-radius: 10px;
  padding: 14px 16px;
}

.price-line {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.currency {
  color: #f56c6c;
  font-size: 16px;
}

.price {
  color: #f56c6c;
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.unit {
  font-size: 13px;
  color: #909399;
}

.origin {
  margin-left: 8px;
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
}

.deposit-line {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}

.deposit-line b {
  color: #303133;
  font-size: 15px;
}

.credit-box {
  margin-top: 16px;
  border: 1px solid #e1f3d8;
  background: #f0f9eb;
  border-radius: 10px;
  padding: 12px 14px;
}

.credit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #529b2e;
}

.credit-score {
  margin-top: 6px;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.credit-score b {
  font-size: 26px;
  color: #529b2e;
  line-height: 1;
}

.credit-score .text-muted {
  margin-left: auto;
  font-size: 12px;
}

.login-tip {
  margin-top: 16px;
}

.rent-btn {
  width: 100%;
  margin-top: 18px;
  height: 46px;
  font-size: 16px;
}

.rent-note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}

.meta-list {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px dashed #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  color: #606266;
}

.info-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 22px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
}

.store-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.ml-6 {
  margin-left: 6px;
}

.map-note {
  margin-top: 14px;
}

.intro-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.9;
  color: #606266;
}

.intro-tags {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
}

.empty-wrap {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 960px) {
  .detail-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }
  .booking {
    position: static;
  }
}
</style>
