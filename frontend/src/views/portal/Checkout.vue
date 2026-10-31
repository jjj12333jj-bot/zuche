<template>
  <div class="checkout-page">
    <div class="page-container">
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/vehicles' }">车辆租赁</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: `/vehicles/${vehicleId}` }">车辆详情</el-breadcrumb-item>
        <el-breadcrumb-item>确认订单</el-breadcrumb-item>
      </el-breadcrumb>

      <el-steps :active="1" align-center class="steps">
        <el-step title="选择车辆" description="已选车型" />
        <el-step title="确认订单" description="填写租期与网点" />
        <el-step title="完成支付" description="在线支付" />
      </el-steps>

      <div v-loading="loading" class="grid">
        <section class="main-col">
          <div v-if="vehicle" class="card vehicle-card">
            <img :src="resolveImage(vehicle.coverImage)" :alt="vehicle.modelName" @error="onImageError" />
            <div class="vehicle-info">
              <h2>{{ vehicle.brandName }} {{ vehicle.modelName }}</h2>
              <p class="plate">{{ vehicle.licensePlate }} · {{ vehicle.color }} · {{ vehicle.seats }} 座</p>
              <div class="tag-row">
                <el-tag size="small" effect="plain" :style="{ color: vehicle.categoryColor, borderColor: vehicle.categoryColor }">
                  {{ vehicle.categoryName }}
                </el-tag>
                <el-tag size="small" effect="plain" type="info">{{ vehicle.gearboxName }}</el-tag>
                <el-tag size="small" effect="plain" type="info">{{ vehicle.fuelTypeName }}</el-tag>
              </div>
            </div>
            <div class="vehicle-price">
              <span class="price">¥{{ formatMoney(vehicle.finalPrice) }}</span>
              <span class="unit">/天</span>
            </div>
          </div>

          <el-alert
            v-if="vehicle && vehicle.status !== 1"
            class="lock-tip"
            type="warning"
            :closable="false"
            show-icon
            :title="`该车辆当前状态为「${vehicle.statusName}」，暂时无法下单`"
          />

          <div class="card">
            <h3 class="card-title">租期与取还车</h3>
            <el-form label-width="88px" label-position="left">
              <el-form-item label="租期">
                <el-date-picker
                  v-model="dateRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  range-separator="至"
                  start-placeholder="取车日期"
                  end-placeholder="还车日期"
                  :disabled-date="disabledDate"
                  size="large"
                  class="date-picker"
                />
              </el-form-item>

              <el-form-item label="取车网点">
                <div class="static-field">
                  <el-icon><OfficeBuilding /></el-icon>
                  <span>{{ vehicle?.storeName || '-' }}</span>
                  <span class="text-muted">{{ vehicle?.storeAddress }}</span>
                </div>
              </el-form-item>

              <el-form-item label="归还网点">
                <el-select v-model="returnStoreId" clearable placeholder="默认在取车网点归还" size="large" class="full">
                  <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
                <div class="field-tip">支持异地还车，可在下方选择归还网点</div>
              </el-form-item>

              <el-form-item label="备注">
                <el-input
                  v-model="remark"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="如需要儿童座椅、送车上门等，请在此说明（选填，不影响金额）"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="card rule-card">
            <h3 class="card-title">租赁与信用规则</h3>
            <ul class="rule-list">
              <li><b>租金</b> = 日租金 × 租期天数 × 折扣，本车折扣 {{ coefficientText(vehicle?.discountRate) }}</li>
              <li><b>押金</b> = 标准押金 × 信用押金系数，信用越好系数越低，押金越少</li>
              <li>订单提交后进入<b>待支付</b>，支付成功才会锁定车辆并进入<b>待取车</b></li>
              <li>押金按信用等级折扣收取，还车结算后原路退回</li>
            </ul>
          </div>
        </section>

        <aside class="aside-col">
          <div class="card summary">
            <h3 class="card-title">费用明细</h3>

            <template v-if="quote">
              <div class="row">
                <span>日租金 × {{ quote.rentDays }} 天</span>
                <span>¥{{ formatMoney(quote.finalDailyPrice * quote.rentDays) }}</span>
              </div>
              <div class="row">
                <span>租金小计</span>
                <span>¥{{ formatMoney(quote.rentAmount) }}</span>
              </div>
              <div class="row">
                <span>
                  押金
                  <em class="muted">（标准 ¥{{ formatMoney(quote.depositStandard) }} × {{ quote.depositCoefficient }}）</em>
                </span>
                <span>¥{{ formatMoney(quote.depositAmount) }}</span>
              </div>
              <div v-if="Number(quote.depositSaved) > 0" class="row saved">
                <span>信用减免</span>
                <span>-¥{{ formatMoney(quote.depositSaved) }}</span>
              </div>
              <div v-else-if="Number(quote.depositSaved) < 0" class="row extra">
                <span>信用加收</span>
                <span>+¥{{ formatMoney(Math.abs(quote.depositSaved)) }}</span>
              </div>

              <div class="divider"></div>

              <div class="total-row">
                <span>应付合计</span>
                <span class="total">¥{{ formatMoney(quote.totalAmount) }}</span>
              </div>
              <p class="total-note">含押金 ¥{{ formatMoney(quote.depositAmount) }}，归还结算后退还</p>
            </template>

            <el-empty v-else-if="!quoteLoading" description="请选择租期后查看价格" :image-size="70" />
          </div>

          <div v-if="quote?.credit" class="card credit-card">
            <div class="credit-head">
              <span>我的信用</span>
              <el-tag
                size="small"
                effect="dark"
                :color="quote.credit.levelColor || CREDIT_LEVELS[quote.credit.creditLevel]?.color"
              >
                {{ quote.credit.levelLabel || CREDIT_LEVELS[quote.credit.creditLevel]?.label }}
              </el-tag>
            </div>
            <div class="credit-body">
              <div class="credit-score">
                <b>{{ quote.credit.creditScore }}</b><span>分</span>
              </div>
              <div class="credit-coeff">
                押金系数 <b>×{{ quote.credit.depositCoefficient }}</b>
              </div>
            </div>
            <p class="credit-desc">{{ quote.credit.levelDescription || '按时归还、订单完成可提升信用分' }}</p>
          </div>

          <div v-if="quote" class="card pay-method-card">
            <h3 class="card-title">支付方式</h3>
            <div class="pay-mock">
              <el-icon><Wallet /></el-icon>
              <div>
                <div class="pay-name">在线支付（微信 / 支付宝）</div>
                <div class="pay-desc">提交后进入收银台选择支付渠道</div>
              </div>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="!quote || !canSubmit"
            @click="onSubmit"
          >
            提交订单
          </el-button>
          <p class="submit-note">提交后生成<span>待支付</span>订单，进入收银台完成支付</p>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVehicleDetail, getVehicleOptions } from '@/api/vehicle'
import { createOrder, leaseQuote } from '@/api/order'
import { useUserStore } from '@/store/user'
import { resolveImage, onImageError } from '@/utils/image'
import { CREDIT_LEVELS, coefficientText, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const vehicleId = route.params.vehicleId
const loading = ref(false)
const quoteLoading = ref(false)
const submitting = ref(false)

const vehicle = ref(null)
const quote = ref(null)
const stores = ref([])

const dateRange = ref(defaultRange())
const returnStoreId = ref(null)
const remark = ref('')

const canSubmit = computed(() => {
  return !!vehicle.value && vehicle.value.status === 1 && dateRange.value?.length === 2
})

function pad(n) {
  return String(n).padStart(2, '0')
}

function dayString(offset) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function defaultRange() {
  return [dayString(0), dayString(1)]
}

/** 今天的 0 点之前不可选 */
function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function loadVehicle() {
  loading.value = true
  try {
    const res = await getVehicleDetail(vehicleId)
    vehicle.value = res.data
  } catch (e) {
    vehicle.value = null
  } finally {
    loading.value = false
  }
}

async function loadStores() {
  try {
    const res = await getVehicleOptions()
    stores.value = res.data?.stores || []
  } catch (e) {
    stores.value = []
  }
}

async function fetchQuote() {
  if (!vehicle.value || dateRange.value?.length !== 2) return
  quoteLoading.value = true
  try {
    const res = await leaseQuote({
      vehicleId: Number(vehicleId),
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      quantity: 1
    })
    quote.value = res.data
  } catch (e) {
    quote.value = null
  } finally {
    quoteLoading.value = false
  }
}

async function onSubmit() {
  if (!userStore.isLogin) {
    ElMessage.info('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!quote.value) {
    ElMessage.warning('请先选择有效租期')
    return
  }
  submitting.value = true
  try {
    const res = await createOrder({
      vehicleId: Number(vehicleId),
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      quantity: 1,
      returnStoreId: returnStoreId.value || null,
      remark: remark.value || null
    })
    ElMessage.success('下单成功，请完成支付')
    router.replace(`/cashier/${res.data.orderId}`)
  } catch (e) {
    // 错误提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!userStore.isLogin) {
    router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  await Promise.all([loadVehicle(), loadStores()])
  await fetchQuote()
})

watch(dateRange, fetchQuote)
</script>

<style scoped>
.checkout-page {
  padding: 20px 0 60px;
}

.breadcrumb {
  font-size: 13px;
  margin-bottom: 16px;
}

.steps {
  margin: 6px 0 22px;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 22px;
  align-items: start;
}

.main-col,
.aside-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.aside-col {
  position: sticky;
  top: 82px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}

.vehicle-card {
  display: flex;
  gap: 16px;
  align-items: center;
}

.vehicle-card img {
  width: 168px;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  border-radius: 10px;
  background: #eef2f6;
  flex-shrink: 0;
}

.vehicle-info h2 {
  margin: 0;
  font-size: 19px;
}

.plate {
  margin: 6px 0 10px;
  font-size: 13px;
  color: #909399;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.vehicle-price {
  margin-left: auto;
  text-align: right;
  flex-shrink: 0;
}

.vehicle-price .price {
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
}

.vehicle-price .unit {
  font-size: 13px;
  color: #909399;
}

.lock-tip {
  border-radius: 10px;
}

.date-picker,
.full {
  width: 100%;
}

.static-field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #303133;
}

.static-field .text-muted {
  font-size: 12px;
}

.field-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.6;
}

.rule-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 2;
  color: #606266;
}

.rule-list b {
  color: #303133;
}

.summary .row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 14px;
  color: #606266;
  padding: 7px 0;
}

.summary .row .muted {
  font-style: normal;
  font-size: 12px;
  color: #a8abb2;
}

.row.saved {
  color: #67c23a;
}

.row.extra {
  color: #f56c6c;
}

.divider {
  height: 1px;
  background: #ebeef5;
  margin: 12px 0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 15px;
  font-weight: 600;
}

.total {
  color: #f56c6c;
  font-size: 26px;
  font-weight: 700;
}

.total-note {
  margin: 6px 0 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: right;
}

.credit-card {
  background: linear-gradient(135deg, #f0f9eb 0%, #ffffff 70%);
  border-color: #e1f3d8;
}

.credit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #529b2e;
}

.credit-body {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-top: 8px;
}

.credit-score b {
  font-size: 28px;
  color: #529b2e;
  line-height: 1;
}

.credit-score span {
  font-size: 12px;
  color: #529b2e;
  margin-left: 2px;
}

.credit-coeff {
  font-size: 13px;
  color: #606266;
}

.credit-coeff b {
  color: #529b2e;
  font-size: 15px;
}

.credit-desc {
  margin: 8px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.pay-mock {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  font-size: 13px;
  color: #606266;
}

.pay-mock .el-icon {
  color: #409eff;
  margin-top: 2px;
}

.pay-name {
  font-weight: 600;
  color: #303133;
}

.pay-desc {
  margin-top: 3px;
  font-size: 12px;
  color: #a8abb2;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.submit-note {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
  text-align: center;
}

.submit-note span {
  color: #e6a23c;
}

@media (max-width: 1080px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .aside-col {
    position: static;
  }
}
</style>
