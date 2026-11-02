<template>
  <div class="home">
    <section class="hero">
      <el-carousel
        :height="heroHeight"
        :interval="6000"
        :arrow="photoSlides.length ? 'hover' : 'never'"
        indicator-position="inside"
        class="hero-carousel"
      >
        <el-carousel-item v-for="slide in slides" :key="slide.key">
          <!-- 第一屏：信用与押金说明，纯色底 -->
          <div v-if="slide.type === 'credit'" class="slide slide-credit">
            <div class="page-container hero-inner">
              <div class="hero-text">
                <el-tag effect="dark" type="success" round size="small">信用风控 · 动态押金</el-tag>
                <h1>信用越好，押金越低</h1>
                <p>
                  平台将按时归还率、取消率、逾期次数与完成订单数纳入信用评分，
                  信用等级直接决定押金系数：优秀 0.8、良好 0.9、一般 1.0、待观察 1.2。
                </p>
                <div class="hero-actions">
                  <el-button type="primary" size="large" @click="router.push('/vehicles')">
                    立即选车
                  </el-button>
                  <el-button v-if="!userStore.isLogin" size="large" plain @click="router.push('/register')">
                    注册查看我的信用
                  </el-button>
                  <el-button v-else size="large" plain @click="router.push('/credit')">
                    我的信用：{{ userStore.userInfo?.creditScore }} 分
                  </el-button>
                </div>
                <div class="hero-note">
                  日租价格区间
                  <b>¥{{ formatMoney(options.priceRange?.minPrice) }}</b>
                  -
                  <b>¥{{ formatMoney(options.priceRange?.maxPrice) }}</b>
                  /天 · 共 {{ vehicleTotal }} 台车辆
                </div>
              </div>

              <div class="credit-panel">
                <div class="panel-title">信用等级与押金系数</div>
                <div v-for="item in creditLevels" :key="item.code" class="level-row">
                  <span class="level-name" :style="{ color: item.color }">{{ item.name }}</span>
                  <span class="level-range">{{ item.range }}</span>
                  <el-tag :color="item.color" effect="dark" size="small">{{ coefLabel(item.coefficient) }}</el-tag>
                </div>
                <div class="panel-tip">等级阈值与押金系数均可在后台“信用规则”中配置</div>
              </div>
            </div>
          </div>

          <!-- 其余：后台「轮播图管理」配置的旅行风景图 -->
          <div
            v-else
            class="slide slide-photo"
            :class="{ clickable: !!slide.linkUrl }"
            :style="slideStyle(slide)"
            @click="goBanner(slide)"
          >
            <div class="photo-scrim"></div>
            <div class="page-container photo-inner">
              <p class="photo-kicker">旅行灵感</p>
              <h2 class="photo-title">{{ slide.title }}</h2>
              <p v-if="slide.subtitle" class="photo-sub">{{ slide.subtitle }}</p>
              <span v-if="slide.linkUrl" class="photo-cta">
                去看看
                <el-icon><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <section class="section">
      <div class="page-container">
        <div class="section-head">
          <div>
            <h2 class="section-title">按车型选车</h2>
            <p class="section-sub">分类来自数据字典，后台可维护</p>
          </div>
          <router-link to="/vehicles" class="more-link">查看全部车辆 →</router-link>
        </div>

        <div v-loading="loadingOptions" class="category-grid">
          <div
            v-for="cat in options.categories"
            :key="cat.code"
            class="category-card"
            @click="goCategory(cat.code)"
          >
            <div class="category-icon" :style="{ background: `${cat.color}1a`, color: cat.color }">
              <el-icon :size="22">
                <component :is="CATEGORY_ICON[cat.code] || 'Van'" />
              </el-icon>
            </div>
            <div class="category-info">
              <div class="category-name">{{ cat.label }}</div>
              <div class="category-desc">{{ CATEGORY_DESC[cat.code] || '多款车型可选' }}</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section section-gray">
      <div class="page-container">
        <div class="section-head">
          <div>
            <h2 class="section-title">热门车型</h2>
            <p class="section-sub">按累计出租次数排序，数据来自订单与车辆表</p>
          </div>
          <router-link to="/vehicles" class="more-link">更多车型 →</router-link>
        </div>

        <div v-loading="loadingHot" class="hot-grid">
          <VehicleCard
            v-for="item in hotVehicles"
            :key="item.id"
            :vehicle="item"
            @click="router.push(`/vehicles/${item.id}`)"
          />
        </div>

        <el-empty
          v-if="!loadingHot && !hotVehicles.length"
          description="暂未加载到车辆数据，请确认后端服务已启动"
        />
      </div>
    </section>

    <section class="section">
      <div class="page-container">
        <h2 class="section-title">租赁流程</h2>
        <div class="steps">
          <div v-for="(step, index) in steps" :key="step.title" class="step">
            <div class="step-index">{{ index + 1 }}</div>
            <div class="step-title">{{ step.title }}</div>
            <div class="step-desc">{{ step.desc }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getVehicles, getVehicleOptions } from '@/api/vehicle'
import { getBanners } from '@/api/banner'
import { resolveImage } from '@/utils/image'
import VehicleCard from '@/components/VehicleCard.vue'

const router = useRouter()
const userStore = useUserStore()

const loadingOptions = ref(false)
const loadingHot = ref(false)
const hotVehicles = ref([])
const vehicleTotal = ref(0)
const banners = ref([])

const options = reactive({
  categories: [],
  brands: [],
  stores: [],
  priceRange: { minPrice: null, maxPrice: null }
})

// 仅用于首页展示的等级说明，与 credit_rule 默认值一致；真实阈值以数据库配置为准
const creditLevels = [
  { code: 'EXCELLENT', name: '优秀', range: '120 - 150 分', coefficient: 0.8, color: '#67c23a' },
  { code: 'GOOD', name: '良好', range: '100 - 119 分', coefficient: 0.9, color: '#409eff' },
  { code: 'NORMAL', name: '一般', range: '80 - 99 分', coefficient: 1.0, color: '#e6a23c' },
  { code: 'WATCH', name: '待观察', range: '0 - 79 分', coefficient: 1.2, color: '#f56c6c' }
]

const CATEGORY_ICON = {
  ECONOMY: 'Money',
  COMFORT: 'Suitcase',
  SUV: 'Van',
  LUXURY: 'Star',
  EV: 'Lightning'
}

const CATEGORY_DESC = {
  ECONOMY: '通勤代步，经济省油',
  COMFORT: '空间舒适，长途友好',
  SUV: '通过性好，适合家庭',
  LUXURY: '商务接待，品质出行',
  EV: '纯电与混动，用车成本低'
}

const steps = [
  { title: '选车下单', desc: '按分类、品牌、价格筛选，实时看到按信用计算出的应付押金' },
  { title: '在线支付', desc: '租金与押金分两笔入账，押金在还车结算后原路退回' },
  { title: '网点取车', desc: '网点确认交付，或用户自助确认取车，订单进入在租状态' },
  { title: '归还结算', desc: '确认归还后自动计算逾期违约金，押金原路退还并更新信用分' }
]

/** 第一屏固定是信用说明，后面接后台配置的旅行图 */
const slides = computed(() => [
  { type: 'credit', key: 'credit' },
  ...banners.value.map((item) => ({ ...item, type: 'photo', key: `banner-${item.id}` }))
])

const photoSlides = computed(() => slides.value.filter((item) => item.type === 'photo'))

/** 轮播高度要按屏宽给，el-carousel 必须拿到固定高度 */
const heroHeight = ref('420px')

function syncHeroHeight() {
  const w = window.innerWidth
  if (w < 640) heroHeight.value = '470px'
  else if (w < 900) heroHeight.value = '470px'
  else if (w < 1200) heroHeight.value = '452px'
  else heroHeight.value = '420px'
}

function goCategory(code) {
  router.push({ path: '/vehicles', query: { category: code } })
}

function goBanner(slide) {
  const url = slide.linkUrl
  if (!url) return
  if (/^https?:\/\//i.test(url)) {
    window.open(url, '_blank', 'noopener')
  } else {
    router.push(url)
  }
}

/**
 * 本地轮播图额外准备了一张竖版裁剪（-sm），窄屏用它，
 * 否则横图在手机宽度下会被放大到只剩中间一条。
 */
function mobileImage(url) {
  if (!url) return ''
  if (/^\/images\/banners\/[\w-]+\.jpg$/i.test(url)) {
    return url.replace(/\.jpg$/i, '-sm.jpg')
  }
  return url
}

function slideStyle(slide) {
  return {
    '--bg': `url(${resolveImage(slide.imageUrl)})`,
    '--bg-sm': `url(${resolveImage(mobileImage(slide.imageUrl))})`
  }
}

/** 押金系数 0.8 → 8 折；1.0 → 原价；1.2 → 1.2 倍 */
function coefLabel(coefficient) {
  const c = Number(coefficient || 1)
  if (c === 1) return '原价'
  if (c < 1) return `${Math.round(c * 10)} 折`
  return `${c} 倍`
}

function formatMoney(value) {
  const n = Number(value || 0)
  if (!n) return '—'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

async function loadData() {
  loadingOptions.value = true
  loadingHot.value = true
  try {
    const [optRes, hotRes, bannerRes] = await Promise.all([
      getVehicleOptions(),
      getVehicles({ page: 1, size: 4, sortBy: 'popular' }),
      getBanners({ position: 'HOME' })
    ])
    Object.assign(options, optRes.data || {})
    hotVehicles.value = hotRes.data?.records || []
    vehicleTotal.value = hotRes.data?.total || 0
    banners.value = bannerRes.data || []
  } catch (e) {
    // 错误提示由 request 拦截器统一处理，首页保留静态内容
  } finally {
    loadingOptions.value = false
    loadingHot.value = false
  }
}

onMounted(() => {
  syncHeroHeight()
  window.addEventListener('resize', syncHeroHeight)
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', syncHeroHeight)
})
</script>

<style scoped>
.hero {
  position: relative;
  background: #12539e;
}

.hero-carousel {
  background: #12539e;
}

.slide {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ---------- 第一屏：纯蓝信用说明 ---------- */
.slide-credit {
  display: flex;
  align-items: center;
  color: #fff;
  background: #12539e;
  padding: 34px 0 44px;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 40px;
  align-items: center;
  width: 100%;
}

.hero-text h1 {
  font-size: 38px;
  margin: 16px 0 14px;
  font-weight: 600;
  letter-spacing: 1px;
}

.hero-text p {
  font-size: 15px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.82);
  max-width: 560px;
  margin: 0 0 28px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-note {
  margin-top: 22px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.62);
}

.hero-note b {
  color: #ffd591;
  font-size: 15px;
}

.credit-panel {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 20px 22px;
}

.panel-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 14px;
}

.level-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.16);
  font-size: 14px;
}

.level-row:last-of-type {
  border-bottom: none;
}

.level-name {
  font-weight: 600;
  min-width: 64px;
}

.level-range {
  flex: 1;
  color: rgba(255, 255, 255, 0.74);
  font-size: 13px;
}

.panel-tip {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.52);
}

/* ---------- 其余屏：旅行实景图 ---------- */
.slide-photo {
  position: relative;
  display: flex;
  align-items: center;
  background-image: var(--bg);
  background-size: cover;
  background-position: center 55%;
  background-repeat: no-repeat;
}

.slide-photo.clickable {
  cursor: pointer;
}

.photo-scrim {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(
      95deg,
      rgba(5, 12, 24, 0.88) 0%,
      rgba(5, 12, 24, 0.72) 30%,
      rgba(5, 12, 24, 0.38) 55%,
      rgba(5, 12, 24, 0.08) 82%
    ),
    linear-gradient(0deg, rgba(5, 12, 24, 0.42) 0%, transparent 26%);
}

.photo-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  color: #fff;
  padding-bottom: 8px;
}

.photo-kicker {
  margin: 0 0 12px;
  font-size: 12px;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.72);
}

.photo-title {
  margin: 0 0 14px;
  font-size: 40px;
  line-height: 1.25;
  font-weight: 600;
  letter-spacing: 1px;
  max-width: 620px;
  text-shadow: 0 4px 24px rgba(0, 0, 0, 0.35);
}

.photo-sub {
  margin: 0 0 26px;
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.86);
  max-width: 520px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

.photo-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 42px;
  padding: 0 22px;
  border-radius: 22px;
  background: #fff;
  color: #12539e;
  font-size: 14px;
  font-weight: 600;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.slide-photo.clickable:hover .photo-cta {
  transform: translateX(3px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.28);
}

/* 轮播指示器收窄成短横线，压在图片上也不抢视线 */
.hero-carousel :deep(.el-carousel__indicator .el-carousel__button) {
  width: 22px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.5);
  opacity: 1;
}

.hero-carousel :deep(.el-carousel__indicator.is-active .el-carousel__button) {
  width: 34px;
  background: #fff;
}

.hero-carousel :deep(.el-carousel__arrow) {
  background: rgba(8, 20, 38, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.hero-carousel :deep(.el-carousel__arrow:hover) {
  background: rgba(8, 20, 38, 0.68);
}

.section {
  padding: 52px 0;
}

.section-gray {
  background: #fafbfc;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}

.section-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.more-link {
  font-size: 13px;
  color: #409eff;
  white-space: nowrap;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  min-height: 84px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.category-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
}

.category-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-name {
  font-size: 15px;
  font-weight: 600;
}

.category-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #909399;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  min-height: 120px;
}

.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.step {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 22px 20px;
}

.step-index {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 14px;
}

.step-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.step-desc {
  font-size: 13px;
  line-height: 1.75;
  color: #909399;
}

@media (max-width: 1100px) {
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .hero-inner,
  .steps {
    grid-template-columns: 1fr;
  }
  .hero-text h1 {
    font-size: 30px;
  }
  /* 窄屏放不下两栏，押金系数表交给 /credit 页看 */
  .credit-panel {
    display: none;
  }
  .photo-title {
    font-size: 28px;
    max-width: 100%;
  }
  .photo-sub {
    font-size: 14px;
  }
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .slide-photo {
    background-position: 62% center;
  }
}

@media (max-width: 640px) {
  /* 窄屏换竖版裁图，文字压到底部，遮罩也改成从下往上压暗 */
  .slide-photo {
    background-image: var(--bg-sm);
    background-position: center;
    align-items: flex-end;
    padding-bottom: 46px;
  }
  .photo-scrim {
    background: linear-gradient(
      0deg,
      rgba(6, 14, 28, 0.9) 0%,
      rgba(6, 14, 28, 0.62) 42%,
      rgba(6, 14, 28, 0.12) 82%
    );
  }
}

@media (max-width: 560px) {
  .hot-grid {
    grid-template-columns: 1fr;
  }
  .hero-text h1 {
    font-size: 26px;
  }
  .hero-text p {
    font-size: 14px;
  }
}
</style>
