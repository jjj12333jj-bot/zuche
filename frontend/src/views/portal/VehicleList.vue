<template>
  <div class="vehicle-page">
    <section class="page-head">
      <div class="page-container">
        <h1>车辆租赁</h1>
        <p>共 {{ total }} 台车辆 · 租金透明 · 信用等级越高押金越低</p>
      </div>
    </section>

    <div class="page-container content">
      <section class="filter-card">
        <div class="filter-row main-row">
          <el-input
            v-model="filters.keyword"
            class="keyword-input"
            placeholder="搜索车型、品牌、车牌"
            clearable
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>

          <el-radio-group v-model="filters.categoryCode" class="category-group" @change="search">
            <el-radio-button value="">全部车型</el-radio-button>
            <el-radio-button v-for="cat in options.categories" :key="cat.code" :value="cat.code">
              {{ cat.label }}
            </el-radio-button>
          </el-radio-group>

          <el-button type="primary" @click="search">搜索</el-button>
        </div>

        <div class="filter-row sub-row">
          <el-select v-model="filters.brandId" placeholder="全部品牌" clearable class="filter-item" @change="search">
            <el-option v-for="b in options.brands" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>

          <el-select v-model="filters.storeId" placeholder="全部网点" clearable class="filter-item" @change="search">
            <el-option v-for="s in options.stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>

          <div class="price-range">
            <el-input-number
              v-model="filters.minPrice"
              :min="0"
              :max="99999"
              :controls="false"
              placeholder="最低价"
              class="price-input"
            />
            <span class="dash">-</span>
            <el-input-number
              v-model="filters.maxPrice"
              :min="0"
              :max="99999"
              :controls="false"
              placeholder="最高价"
              class="price-input"
            />
            <span class="price-unit">元/天</span>
          </div>

          <el-select v-model="filters.sortBy" class="filter-item" @change="search">
            <el-option label="默认排序（热度）" value="default" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
            <el-option label="最新上架" value="newest" />
            <el-option label="出租次数最多" value="popular" />
          </el-select>

          <el-checkbox v-model="filters.onlyAvailable" @change="search">只看可租</el-checkbox>

          <el-button text type="primary" @click="reset">重置筛选</el-button>
        </div>
      </section>

      <section class="list-section">
        <div class="list-head">
          <div class="result-text">
            找到 <b>{{ total }}</b> 台车辆
            <span v-if="activeFilterText" class="active-filter">· {{ activeFilterText }}</span>
          </div>
          <div class="price-hint" v-if="options.priceRange?.minPrice">
            平台日租价 ¥{{ formatMoney(options.priceRange.minPrice) }} - ¥{{
              formatMoney(options.priceRange.maxPrice)
            }}
          </div>
        </div>

        <div v-loading="loading" class="vehicle-grid">
          <VehicleCard
            v-for="item in list"
            :key="item.id"
            :vehicle="item"
            @click="router.push(`/vehicles/${item.id}`)"
          />
        </div>

        <el-empty v-if="!loading && !list.length" description="没有符合条件的车辆">
          <el-button type="primary" plain @click="reset">清空筛选条件</el-button>
        </el-empty>

        <div v-if="total > filters.size" class="pagination">
          <el-pagination
            v-model:current-page="filters.page"
            background
            layout="prev, pager, next, total"
            :page-size="filters.size"
            :total="total"
            @current-change="loadList"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVehicles, getVehicleOptions } from '@/api/vehicle'
import VehicleCard from '@/components/VehicleCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)

const options = reactive({
  categories: [],
  brands: [],
  stores: [],
  priceRange: { minPrice: null, maxPrice: null }
})

const filters = reactive({
  keyword: '',
  categoryCode: '',
  brandId: null,
  storeId: null,
  minPrice: null,
  maxPrice: null,
  sortBy: 'default',
  onlyAvailable: false,
  page: 1,
  size: 12
})

const activeFilterText = computed(() => {
  const parts = []
  if (filters.keyword) parts.push(`关键词“${filters.keyword}”`)
  const cat = options.categories.find((c) => c.code === filters.categoryCode)
  if (cat) parts.push(cat.label)
  const brand = options.brands.find((b) => b.id === filters.brandId)
  if (brand) parts.push(brand.name)
  if (filters.onlyAvailable) parts.push('仅可租')
  return parts.join(' · ')
})

function formatMoney(value) {
  const n = Number(value || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function buildParams() {
  if (filters.minPrice != null && filters.maxPrice != null && filters.minPrice > filters.maxPrice) {
    const tmp = filters.minPrice
    filters.minPrice = filters.maxPrice
    filters.maxPrice = tmp
  }
  const params = {
    page: filters.page,
    size: filters.size,
    sortBy: filters.sortBy,
    onlyAvailable: filters.onlyAvailable
  }
  if (filters.keyword) params.keyword = filters.keyword.trim()
  if (filters.categoryCode) params.categoryCode = filters.categoryCode
  if (filters.brandId) params.brandId = filters.brandId
  if (filters.storeId) params.storeId = filters.storeId
  if (filters.minPrice != null) params.minPrice = filters.minPrice
  if (filters.maxPrice != null) params.maxPrice = filters.maxPrice
  return params
}

async function loadList() {
  loading.value = true
  try {
    const res = await getVehicles(buildParams())
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  filters.page = 1
  loadList()
}

function reset() {
  Object.assign(filters, {
    keyword: '',
    categoryCode: '',
    brandId: null,
    storeId: null,
    minPrice: null,
    maxPrice: null,
    sortBy: 'default',
    onlyAvailable: false,
    page: 1
  })
  if (route.query.category || route.query.keyword) {
    router.replace({ path: '/vehicles' })
  }
  loadList()
}

async function loadOptions() {
  try {
    const res = await getVehicleOptions()
    Object.assign(options, res.data || {})
  } catch (e) {
    ElMessage.warning('筛选项加载失败，可刷新页面重试')
  }
}

onMounted(async () => {
  if (route.query.category) filters.categoryCode = String(route.query.category)
  if (route.query.keyword) filters.keyword = String(route.query.keyword)
  await loadOptions()
  await loadList()
})
</script>

<style scoped>
.vehicle-page {
  min-height: 70vh;
  padding-bottom: 48px;
}

.page-head {
  background: linear-gradient(120deg, #1f3a5f 0%, #2c5580 70%, #276b5c 100%);
  color: #fff;
  padding: 34px 0 30px;
  margin-bottom: 22px;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
}

.page-head p {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.keyword-input {
  width: 260px;
}

.category-group {
  flex: 1;
  min-width: 280px;
}

.filter-item {
  width: 150px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.price-input {
  width: 92px;
}

.dash {
  color: #c0c4cc;
}

.price-unit {
  font-size: 12px;
  color: #909399;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
  color: #606266;
}

.result-text b {
  color: #f56c6c;
  font-size: 15px;
}

.active-filter {
  color: #909399;
}

.price-hint {
  font-size: 12px;
  color: #909399;
}

.vehicle-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  min-height: 160px;
}

.pagination {
  margin-top: 26px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1000px) {
  .vehicle-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .vehicle-grid {
    grid-template-columns: 1fr;
  }
  .keyword-input,
  .filter-item {
    width: 100%;
  }
  .category-group {
    width: 100%;
  }
}
</style>
