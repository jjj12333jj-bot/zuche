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

      <section class="card eval-card">
        <h3 class="card-title">
          用户评价
          <span v-if="evaluations.total" class="title-note">共 {{ evaluations.total }} 条</span>
        </h3>

        <template v-if="evaluations.total">
          <div class="eval-summary">
            <div class="score-block">
              <div class="score-big">{{ avgText(evaluations.avgRating) }}<i>/5</i></div>
              <el-rate :model-value="Number(evaluations.avgRating) || 0" disabled size="small" />
              <div class="score-sub">{{ evaluations.total }} 位租客评价</div>
            </div>

            <div class="dim-block">
              <div class="dim-row">
                <span>车辆状况</span>
                <b>{{ avgText(evaluations.avgVehicleRating) }}</b>
              </div>
              <div class="dim-row">
                <span>门店服务</span>
                <b>{{ avgText(evaluations.avgStoreRating) }}</b>
              </div>
              <p class="dim-tip">车况与服务分开统计，避免一个好一个差被平均成一个"还行"</p>
            </div>

            <div class="dist-block">
              <div v-for="s in [5, 4, 3, 2, 1]" :key="s" class="dist-row">
                <span class="dist-label">{{ s }} 星</span>
                <div class="dist-bar"><i :style="{ width: starPct(s) }" /></div>
                <span class="dist-count">{{ starCount(s) }}</span>
              </div>
            </div>
          </div>

          <div class="eval-list">
            <div v-for="item in evaluations.records" :key="item.id" class="eval-item">
              <div class="item-head">
                <span class="item-avatar">{{ (item.customerName || '匿').slice(0, 1) }}</span>
                <div class="item-user">
                  <div class="item-name">{{ item.customerName || '匿名用户' }}</div>
                  <div class="item-meta">{{ formatDate(item.createTime) }}</div>
                </div>
                <el-rate :model-value="item.rating" disabled size="small" />
              </div>
              <div class="item-dims">
                <span>车况 {{ item.vehicleRating }}.0</span>
                <span>服务 {{ item.storeRating }}.0</span>
              </div>
              <p class="item-content">{{ item.content }}</p>
              <div v-if="item.images && item.images.length" class="item-images">
                <el-image
                  v-for="(img, i) in item.images"
                  :key="i"
                  :src="resolveImage(img)"
                  :preview-src-list="item.images.map(resolveImage)"
                  :initial-index="i"
                  fit="cover"
                  class="item-thumb"
                />
              </div>
              <div v-if="item.reply" class="item-reply">
                <span class="item-reply-tag">商家回复</span>{{ item.reply }}
              </div>
            </div>
          </div>

          <div v-if="evaluations.records.length < evaluations.total" class="eval-more">
            <el-button :loading="evalLoading" @click="loadMoreEvaluations">查看更多评价</el-button>
          </div>
        </template>

        <el-empty v-else description="这台车还没有用户评价" :image-size="72" />
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
import { getVehicleDetail, getVehicleEvaluations } from '@/api/vehicle'
import { useUserStore } from '@/store/user'
import { resolveImage, onImageError } from '@/utils/image'
import { formatDate } from '@/utils/format'

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

function onRent() {  if (!userStore.isLogin) {
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

/* ==================== 用户评价 ==================== */

const EVAL_PAGE_SIZE = 5
const evalLoading = ref(false)
const evaluations = ref({
  total: 0,
  avgRating: 0,
  avgVehicleRating: 0,
  avgStoreRating: 0,
  star1: 0,
  star2: 0,
  star3: 0,
  star4: 0,
  star5: 0,
  records: [],
  page: 1,
  size: EVAL_PAGE_SIZE
})

const STAR_FIELD = { 5: 'star5', 4: 'star4', 3: 'star3', 2: 'star2', 1: 'star1' }

function starCount(star) {
  return Number(evaluations.value[STAR_FIELD[star]] || 0)
}

function starPct(star) {
  const total = Number(evaluations.value.total || 0)
  if (!total) return '0%'
  return `${Math.round((starCount(star) / total) * 100)}%`
}

function avgText(value) {
  const n = Number(value || 0)
  return n ? n.toFixed(1) : '—'
}

async function loadEvaluations(pageNo = 1) {
  const id = route.params.id
  if (!id) return
  evalLoading.value = true
  try {
    const res = await getVehicleEvaluations(id, { page: pageNo, size: EVAL_PAGE_SIZE })
    const data = res.data || {}
    const incoming = data.records || []
    evaluations.value =
      pageNo === 1
        ? { ...evaluations.value, ...data, records: incoming }
        : {
            ...evaluations.value,
            ...data,
            records: [...evaluations.value.records, ...incoming]
          }
  } catch (e) {
    // 评价是附加信息，接口挂了也不该让整页报错，给空态即可
    if (pageNo === 1) {
      evaluations.value = { ...evaluations.value, total: 0, records: [] }
    }
  } finally {
    evalLoading.value = false
  }
}

function loadMoreEvaluations() {
  loadEvaluations(Number(evaluations.value.page || 1) + 1)
}

onMounted(() => {
  loadDetail()
  loadEvaluations(1)
})
watch(
  () => route.params.id,
  () => {
    loadDetail()
    loadEvaluations(1)
  }
)
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

/* ==================== 用户评价 ==================== */

.title-note {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: #a8abb2;
}

.eval-summary {
  display: grid;
  grid-template-columns: 180px 1fr 260px;
  gap: 24px;
  padding: 16px 0 20px;
  border-bottom: 1px solid #f0f2f5;
}

.score-block {
  text-align: center;
}

.score-big {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #e6a23c;
}

.score-big i {
  font-size: 15px;
  font-style: normal;
  color: #a8abb2;
}

.score-block .el-rate {
  margin-top: 8px;
}

.score-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #a8abb2;
}

.dim-block {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.dim-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 220px;
  font-size: 13px;
  color: #606266;
}

.dim-row b {
  font-size: 15px;
  color: #e6a23c;
}

.dim-tip {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #a8abb2;
}

.dist-block {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.dist-label {
  width: 32px;
}

.dist-bar {
  flex: 1;
  height: 7px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.dist-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #f7ba2a, #e6a23c);
  border-radius: 4px;
}

.dist-count {
  width: 24px;
  text-align: right;
}

.eval-list {
  display: flex;
  flex-direction: column;
}

.eval-item {
  padding: 18px 0;
  border-bottom: 1px solid #f5f7fa;
}

.eval-item:last-child {
  border-bottom: none;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  font-size: 15px;
  font-weight: 600;
}

.item-user {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: #303133;
}

.item-meta {
  font-size: 12px;
  color: #a8abb2;
}

.item-dims {
  display: flex;
  gap: 14px;
  margin: 10px 0 6px 44px;
  font-size: 12px;
  color: #909399;
}

.item-content {
  margin: 0 0 0 44px;
  font-size: 14px;
  line-height: 1.75;
  color: #303133;
  white-space: pre-wrap;
}

.item-images {
  display: flex;
  gap: 8px;
  margin: 10px 0 0 44px;
}

.item-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.item-reply {
  margin: 12px 0 0 44px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

.item-reply-tag {
  display: inline-block;
  margin-right: 8px;
  padding: 0 6px;
  font-size: 12px;
  color: #409eff;
  border: 1px solid #d9ecff;
  background: #ecf5ff;
  border-radius: 4px;
}

.eval-more {
  padding-top: 16px;
  text-align: center;
}

@media (max-width: 960px) {
  .eval-summary {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  .item-dims,
  .item-content,
  .item-images,
  .item-reply {
    margin-left: 0;
  }
}
</style>
