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

    <section class="section section-gray">
      <div class="page-container">
        <div class="section-head">
          <div>
            <h2 class="section-title">热门车型推荐</h2>
            <p class="section-sub">按累计出租次数排序，展示前 9 台</p>
          </div>
          <router-link to="/vehicles" class="more-link">查看全部车辆 →</router-link>
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

    <!-- 推荐出行方案：左侧一张大图 + 右侧四张场景图 -->
    <section class="section">
      <div class="page-container">
        <div class="section-head">
          <div>
            <h2 class="section-title">推荐出行方案</h2>
            <p class="section-sub">按用车场景选车，从日常代步到长途自驾</p>
          </div>
        </div>

        <div class="travel-grid">
          <div
            v-for="plan in travelPlans"
            :key="plan.key"
            class="travel-tile"
            :class="{ 'travel-tile--main': plan.main }"
            @click="goPlan(plan)"
          >
            <img :src="plan.image" :alt="plan.title" loading="lazy" />
            <div class="travel-scrim"></div>
            <div class="travel-text">
              <h3>{{ plan.title }}</h3>
              <p>{{ plan.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section section-gray">
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

    <!-- 租车 Q&A：文案只写系统里真实存在的规则，不承诺发票/保险/客服 -->
    <section class="section">
      <div class="page-container">
        <div class="qa-head">
          <h2 class="section-title">租车 Q&amp;A</h2>
          <p class="section-sub">押金、费用、取消与逾期，六个高频问题一次说清</p>
        </div>

        <div class="qa-grid">
          <div v-for="(item, index) in faqs" :key="item.q" class="qa-card">
            <div class="qa-title">
              <span class="qa-index">Q{{ index + 1 }}</span>
              <h3>{{ item.q }}</h3>
            </div>
            <p class="qa-answer">{{ item.a }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 服务保障条：四段满宽色块，文字与图标都对应真实能力 -->
    <section class="service-band">
      <div
        v-for="item in services"
        :key="item.title"
        class="service-item"
        :style="{ background: item.color }"
      >
        <el-icon class="service-icon" :size="124">
          <component :is="item.icon" />
        </el-icon>
        <div class="service-title">{{ item.title }}</div>
        <div class="service-sub">{{ item.sub }}</div>
        <p class="service-desc">{{ item.desc }}</p>
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

const loadingHot = ref(false)
const hotVehicles = ref([])
const vehicleTotal = ref(0)
const banners = ref([])

const options = reactive({
  priceRange: { minPrice: null, maxPrice: null }
})

// 仅用于首页展示的等级说明，与 credit_rule 默认值一致；真实阈值以数据库配置为准
const creditLevels = [
  { code: 'EXCELLENT', name: '优秀', range: '120 - 150 分', coefficient: 0.8, color: '#67c23a' },
  { code: 'GOOD', name: '良好', range: '100 - 119 分', coefficient: 0.9, color: '#409eff' },
  { code: 'NORMAL', name: '一般', range: '80 - 99 分', coefficient: 1.0, color: '#e6a23c' },
  { code: 'WATCH', name: '待观察', range: '0 - 79 分', coefficient: 1.2, color: '#f56c6c' }
]

/**
 * 推荐出行方案：每个场景对应一个分类筛选（或全部车辆），
 * 文案里的价格/座位/车型都取自 vehicle_model 种子数据，避免和后台改价后对不上。
 */
const travelPlans = [
  {
    key: 'drive',
    title: '自驾旅行',
    desc: '2 个网点、24 台车，支持跨网点异地还车，日租百元起，租期越长折扣越低。',
    image: '/images/travel/travel-drive.jpg',
    link: { path: '/vehicles' },
    main: true
  },
  {
    key: 'commute',
    title: '日常通勤',
    desc: '英朗、轩逸日租 ¥109 起，市区代步省油。',
    image: '/images/travel/travel-commute.jpg',
    link: { path: '/vehicles', query: { category: 'ECONOMY' } }
  },
  {
    key: 'business',
    title: '商务接待',
    desc: 'A6L、5 系、Q5L 豪华型，后排空间与质感都在线。',
    image: '/images/travel/travel-business.jpg',
    link: { path: '/vehicles', query: { category: 'LUXURY' } }
  },
  {
    key: 'family',
    title: '亲子出游',
    desc: '七座途观L、CR-V，一家人行李都装得下。',
    image: '/images/travel/travel-family.jpg',
    link: { path: '/vehicles', query: { category: 'SUV' } }
  },
  {
    key: 'visit',
    title: '探亲访友',
    desc: '行李伴手礼都装得下，支持异地还车。',
    image: '/images/travel/travel-visit.jpg',
    link: { path: '/vehicles', query: { category: 'COMFORT' } }
  }
]

/** 常见问题：只写系统里真实生效的规则，不承诺发票/保险/客服等边界外能力 */
const faqs = [
  {
    q: '租车需要哪些证件？',
    a: '下单时填写驾驶人姓名与手机号；到店取车请出示本人身份证与驾驶证原件（C1 及以上）。系统只做字段录入与格式校验，不做联网核验。'
  },
  {
    q: '押金怎么算，什么时候退？',
    a: '押金 = 车型基准押金 × 信用系数。信用 120 分以上 8 折、100–119 分 9 折、80–99 分原价、80 分以下 1.2 倍。押金与租金分两笔支付，归还结算后原路退回。'
  },
  {
    q: '预订流程是怎样的？',
    a: '① 选择取车/还车网点与租期；② 挑选车型，页面实时算出租金与押金；③ 提交订单并完成模拟支付；④ 到店取车，归还后自动结算。'
  },
  {
    q: '订单可以取消吗？',
    a: '待支付订单可直接取消，不产生费用；已支付未取车的订单取消后租金与押金原路退回，但会记录一次取消行为并影响信用评分；已取车后只能走归还流程。'
  },
  {
    q: '租金里包含哪些费用？',
    a: '日租价 × 租期天数 × 折扣率，押金单独计算。跨网点归还另收异地还车费，下单时按网点组合明码展示；平台不额外收取保险与服务费。'
  },
  {
    q: '逾期归还怎么处理？',
    a: '超过计划还车时间按天计违约金，从押金中扣除，最多扣到押金为 0；同时按天扣信用分（有次数上限），信用等级下降会让下次租车押金系数变高。'
  }
]

/** 服务保障条：四条都对应代码里已经跑通的机制 */
const services = [
  {
    title: '7×24 自助下单',
    sub: '支付成功即锁车',
    desc: '线上下单不受门店营业时间限制，支付成功那一刻车辆即被锁定，避免到店无车。',
    icon: 'Clock',
    color: '#12539e'
  },
  {
    title: '信用减免押金',
    sub: '优秀等级押金 8 折',
    desc: '按时归还、少取消、少逾期都会加分，信用 120 分以上押金系数降到 0.8。',
    icon: 'Wallet',
    color: '#0f8f7f'
  },
  {
    title: '支持自助取还车',
    sub: '到店自助确认取车',
    desc: '在订单页点击「确认取车」即进入在租，归还后系统自动结算押金与违约金。',
    icon: 'Key',
    color: '#d4922a'
  },
  {
    title: '全程价格透明',
    sub: '租金押金分项可见',
    desc: '租金、押金、异地还车费、逾期违约金逐项列明，支付前就能看到最终金额。',
    icon: 'Tickets',
    color: '#1f2d3d'
  }
]

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

function goPlan(plan) {
  router.push(plan.link)
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
  loadingHot.value = true
  try {
    const [optRes, hotRes, bannerRes] = await Promise.all([
      getVehicleOptions(),
      getVehicles({ page: 1, size: 9, sortBy: 'popular' }),
      getBanners({ position: 'HOME' })
    ])
    Object.assign(options, optRes.data || {})
    hotVehicles.value = hotRes.data?.records || []
    vehicleTotal.value = hotRes.data?.total || 0
    banners.value = bannerRes.data || []
  } catch (e) {
    // 错误提示由 request 拦截器统一处理，首页保留静态内容
  } finally {
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
  background: #0b2f5c;
}

.hero-carousel {
  background: #0b2f5c;
}

.slide {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* ---------- 第一屏：信用说明，蓝调旅行实景图打底 ---------- */
.slide-credit {
  position: relative;
  display: flex;
  align-items: center;
  color: #fff;
  background-color: #0b2f5c;
  background-image: url('/images/banners/banner-credit.jpg');
  background-size: cover;
  background-position: center 58%;
  background-repeat: no-repeat;
  padding: 34px 0 44px;
}

/* 蓝色遮罩：左侧压深保证白字可读，右侧逐渐透出桥与车 */
.slide-credit::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(
      96deg,
      rgba(6, 30, 60, 0.93) 0%,
      rgba(7, 36, 71, 0.85) 30%,
      rgba(9, 45, 88, 0.58) 58%,
      rgba(10, 52, 102, 0.28) 100%
    ),
    linear-gradient(0deg, rgba(5, 22, 45, 0.46) 0%, transparent 32%);
}

.hero-inner {
  position: relative;
  z-index: 1;
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
  text-shadow: 0 4px 22px rgba(2, 14, 30, 0.42);
}

.hero-text p {
  font-size: 15px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.86);
  max-width: 560px;
  margin: 0 0 28px;
  text-shadow: 0 2px 12px rgba(2, 14, 30, 0.35);
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
  background: rgba(4, 22, 44, 0.54);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 12px;
  padding: 20px 22px;
  backdrop-filter: blur(12px) saturate(150%);
  box-shadow: 0 18px 42px rgba(3, 16, 34, 0.32);
}

.panel-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
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
  color: rgba(255, 255, 255, 0.6);
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

.hot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  min-height: 120px;
}

/* ---------- 推荐出行方案 ---------- */
.travel-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr 1fr;
  grid-auto-rows: 208px;
  gap: 16px;
}

.travel-tile {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  background: #0f2f52;
  cursor: pointer;
}

.travel-tile--main {
  grid-row: span 2;
}

.travel-tile img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.travel-tile:hover img {
  transform: scale(1.05);
}

.travel-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    0deg,
    rgba(6, 18, 34, 0.86) 0%,
    rgba(6, 18, 34, 0.4) 44%,
    rgba(6, 18, 34, 0.04) 76%
  );
}

/* 大图的房车在右下方，文案改压左上角，遮罩自上而下压暗，车就不会被字挡住 */
.travel-tile--main .travel-scrim {
  background: linear-gradient(
    180deg,
    rgba(6, 18, 34, 0.9) 0%,
    rgba(6, 18, 34, 0.62) 34%,
    rgba(6, 18, 34, 0.12) 64%,
    rgba(6, 18, 34, 0) 88%
  );
}

.travel-tile--main .travel-text {
  top: 0;
  bottom: auto;
  padding-top: 24px;
}

.travel-text {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 22px;
  color: #fff;
}

.travel-text h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.travel-tile--main .travel-text h3 {
  font-size: 24px;
}

.travel-text p {
  margin: 0;
  max-width: 360px;
  font-size: 13px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.82);
}

/* ---------- 租车 Q&A ---------- */
.qa-head {
  text-align: center;
  margin-bottom: 30px;
}

.qa-head .section-sub {
  margin-top: 8px;
}

.qa-grid {
  column-count: 3;
  column-gap: 16px;
}

.qa-card {
  break-inside: avoid;
  margin-bottom: 16px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
}

.qa-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.qa-index {
  flex-shrink: 0;
  margin-top: 1px;
  padding: 1px 7px;
  border-radius: 6px;
  background: #fdf6e3;
  color: #c08b1f;
  font-size: 12px;
  font-weight: 700;
}

.qa-title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  color: #303133;
}

.qa-answer {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.9;
  color: #7a8290;
}

/* ---------- 服务保障条 ---------- */
.service-band {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.service-item {
  position: relative;
  overflow: hidden;
  min-height: 196px;
  padding: 36px 30px;
  color: #fff;
}

.service-icon {
  position: absolute;
  right: -16px;
  bottom: -26px;
  opacity: 0.11;
  pointer-events: none;
}

.service-title {
  position: relative;
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.service-sub {
  position: relative;
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.service-desc {
  position: relative;
  margin: 12px 0 0;
  max-width: 250px;
  font-size: 12px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.7);
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
  .travel-grid {
    grid-auto-rows: 190px;
  }
  .travel-text {
    padding: 18px;
  }
  .service-item {
    padding: 30px 22px;
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
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .travel-grid {
    grid-template-columns: repeat(2, 1fr);
    grid-auto-rows: 180px;
  }
  /* 两栏时大图横跨两列两行，避免整屏只剩一张图或文字溢出卡片 */
  .travel-tile--main {
    grid-column: span 2;
    grid-row: span 2;
  }
  .qa-grid {
    column-count: 2;
  }
  .service-band {
    grid-template-columns: repeat(2, 1fr);
  }
  .slide-photo {
    background-position: 62% center;
  }
  .slide-credit {
    background-position: 64% center;
  }
}

@media (max-width: 640px) {
  /* 窄屏换竖版裁图，文字压到底部，遮罩也改成从下往上压暗 */
  .slide-photo,
  .slide-credit {
    background-position: center;
    align-items: flex-end;
    padding-bottom: 46px;
  }
  .slide-photo {
    background-image: var(--bg-sm);
  }
  .slide-credit {
    background-image: url('/images/banners/banner-credit-sm.jpg');
  }
  .photo-scrim {
    background: linear-gradient(
      0deg,
      rgba(6, 14, 28, 0.9) 0%,
      rgba(6, 14, 28, 0.62) 42%,
      rgba(6, 14, 28, 0.12) 82%
    );
  }
  .slide-credit::before {
    background: linear-gradient(
      0deg,
      rgba(5, 22, 45, 0.95) 0%,
      rgba(6, 30, 60, 0.74) 46%,
      rgba(9, 45, 88, 0.2) 86%
    );
  }
}

@media (max-width: 560px) {
  .hot-grid {
    grid-template-columns: 1fr;
  }
  .travel-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: 200px;
  }
  .travel-tile--main {
    grid-column: span 1;
    grid-row: span 2;
  }
  .qa-grid {
    column-count: 1;
  }
  .service-band {
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
