<template>
  <div class="home">
    <section class="hero">
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getVehicles, getVehicleOptions } from '@/api/vehicle'
import VehicleCard from '@/components/VehicleCard.vue'

const router = useRouter()
const userStore = useUserStore()

const loadingOptions = ref(false)
const loadingHot = ref(false)
const hotVehicles = ref([])
const vehicleTotal = ref(0)

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
  { title: '模拟支付', desc: '租金与押金分两笔流水入账，不接入真实支付渠道' },
  { title: '网点取车', desc: '网点确认交付，或用户自助确认取车，订单进入在租状态' },
  { title: '归还结算', desc: '确认归还后自动计算逾期违约金，押金原路退还并更新信用分' }
]

function goCategory(code) {
  router.push({ path: '/vehicles', query: { category: code } })
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
    const [optRes, hotRes] = await Promise.all([
      getVehicleOptions(),
      getVehicles({ page: 1, size: 4, sortBy: 'popular' })
    ])
    Object.assign(options, optRes.data || {})
    hotVehicles.value = hotRes.data?.records || []
    vehicleTotal.value = hotRes.data?.total || 0
  } catch (e) {
    // 错误提示由 request 拦截器统一处理，首页保留静态内容
  } finally {
    loadingOptions.value = false
    loadingHot.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.hero {
  background: linear-gradient(120deg, #1f3a5f 0%, #2c5580 60%, #276b5c 100%);
  color: #fff;
  padding: 56px 0 60px;
}

.hero-inner {
  display: grid;
  grid-template-columns: 1.25fr 0.75fr;
  gap: 40px;
  align-items: center;
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
  color: rgba(255, 255, 255, 0.8);
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
  color: rgba(255, 255, 255, 0.6);
}

.hero-note b {
  color: #ffd591;
  font-size: 15px;
}

.credit-panel {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 12px;
  padding: 20px 22px;
}

.panel-title {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 14px;
}

.level-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.14);
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
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
}

.panel-tip {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
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

@media (max-width: 1000px) {
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
    font-size: 28px;
  }
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .hot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
