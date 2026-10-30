<template>
  <div class="cashier-page">
    <div class="page-container">
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
        <el-breadcrumb-item>收银台</el-breadcrumb-item>
      </el-breadcrumb>

      <el-steps :active="2" align-center class="steps">
        <el-step title="选择车辆" description="已选车型" />
        <el-step title="确认订单" description="已生成订单" />
        <el-step title="完成支付" description="模拟支付" />
      </el-steps>

      <!-- 支付成功 -->
      <div v-if="paidResult" class="card success-card">
        <el-result icon="success" title="支付成功（模拟）" :sub-title="`订单 ${paidResult.orderNo} 已进入「${paidResult.statusLabel}」`">
          <template #extra>
            <div class="paid-amount">
              实付 <b>¥{{ formatMoney(paidResult.paidAmount) }}</b>
            </div>
          </template>
        </el-result>

        <div class="flow-block">
          <h3 class="block-title">本次生成 {{ paidResult.payments?.length || 0 }} 条支付流水</h3>
          <div class="flow-list">
            <div v-for="p in paidResult.payments" :key="p.id" class="flow-item">
              <div class="flow-left">
                <el-tag size="small" :type="p.payType === 'RENT' ? 'success' : 'warning'" effect="plain">
                  {{ p.payTypeLabel }}
                </el-tag>
                <span class="flow-no">{{ p.payNo }}</span>
              </div>
              <div class="flow-right">
                <span class="flow-channel">{{ payMethodLabel(p.payMethod) }}</span>
                <b>¥{{ formatMoney(p.amount) }}</b>
              </div>
            </div>
          </div>
          <p class="flow-note">
            租金计入平台营收，押金为代收待退：归还结算后走退款流程（S3 实现押金退还）。
          </p>
        </div>

        <div class="success-actions">
          <el-button @click="router.push('/vehicles')">继续租车</el-button>
          <el-button type="primary" @click="router.push(`/orders/${paidResult.orderId}`)">查看订单详情</el-button>
        </div>
      </div>

      <!-- 待支付 -->
      <div v-else-if="order" class="grid">
        <section class="main-col">
          <div class="card">
            <div class="order-head">
              <div>
                <h2>订单待支付</h2>
                <p class="order-no">订单号 {{ order.orderNo }}</p>
              </div>
              <el-tag type="warning" effect="dark" size="large">{{ order.statusLabel }}</el-tag>
            </div>

            <el-descriptions :column="2" size="small" border class="order-desc">
              <el-descriptions-item label="车辆">{{ order.vehicleNames }}</el-descriptions-item>
              <el-descriptions-item label="取车网点">{{ order.storeName }}</el-descriptions-item>
              <el-descriptions-item label="租期">
                {{ formatDate(order.startDate) }} 至 {{ formatDate(order.endDate) }}（{{ order.rentDays }} 天）
              </el-descriptions-item>
              <el-descriptions-item label="归还网点">{{ order.returnStoreName || order.storeName }}</el-descriptions-item>
              <el-descriptions-item label="下单时间">{{ formatDateTime(order.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="车辆数量">{{ order.vehicleCount }} 台</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="card">
            <h3 class="card-title">选择支付方式</h3>
            <el-radio-group v-model="payMethod" class="pay-list">
              <el-radio label="MOCK_WECHAT" border class="pay-item">
                <div class="pay-inner">
                  <span class="pay-name">模拟微信支付</span>
                  <span class="pay-desc">演示渠道，不产生真实扣款</span>
                </div>
              </el-radio>
              <el-radio label="MOCK_ALIPAY" border class="pay-item">
                <div class="pay-inner">
                  <span class="pay-name">模拟支付宝</span>
                  <span class="pay-desc">演示渠道，不产生真实扣款</span>
                </div>
              </el-radio>
            </el-radio-group>

            <el-alert
              class="mock-alert"
              type="warning"
              :closable="false"
              show-icon
              title="模拟支付说明"
              description="本系统未接入任何第三方支付 SDK，点击支付即视为支付成功，仅生成 payment_record 流水用于业务闭环演示。"
            />
          </div>

          <div v-if="order.status !== 'WAIT_PAY'" class="card">
            <el-alert
              type="info"
              :closable="false"
              show-icon
              :title="`该订单当前为「${order.statusLabel}」，无需重复支付`"
            />
            <el-button class="mt-12" @click="router.push(`/orders/${order.id}`)">查看订单详情</el-button>
          </div>
        </section>

        <aside class="aside-col">
          <div class="card">
            <h3 class="card-title">应付金额</h3>
            <div class="row">
              <span>租金</span>
              <span>¥{{ formatMoney(order.rentAmount) }}</span>
            </div>
            <div class="row">
              <span>押金</span>
              <span>¥{{ formatMoney(order.depositAmount) }}</span>
            </div>
            <div class="divider"></div>
            <div class="total-row">
              <span>合计</span>
              <span class="total">¥{{ formatMoney(order.totalAmount) }}</span>
            </div>
          </div>

          <div class="card credit-card">
            <div class="credit-head">
              <span>下单时信用快照</span>
              <el-tag size="small" effect="dark" :color="levelColor(order.creditLevelSnapshot)">
                {{ order.creditLevelLabel || order.creditLevelSnapshot }}
              </el-tag>
            </div>
            <div class="credit-row">
              <span>信用分</span><b>{{ order.creditScoreSnapshot }} 分</b>
            </div>
            <div class="credit-row">
              <span>押金系数</span><b>×{{ order.depositCoefficient }}</b>
            </div>
            <div v-if="Number(order.depositSaved) > 0" class="credit-row saved">
              <span>信用减免</span><b>¥{{ formatMoney(order.depositSaved) }}</b>
            </div>
            <p class="credit-note">信用快照在下单时固定，后续信用变化不影响本单押金</p>
          </div>

          <el-button
            type="primary"
            size="large"
            class="pay-btn"
            :loading="paying"
            :disabled="order.status !== 'WAIT_PAY'"
            @click="onPay"
          >
            确认支付 ¥{{ formatMoney(order.totalAmount) }}
          </el-button>
          <el-button
            v-if="order.status === 'WAIT_PAY'"
            text
            class="cancel-btn"
            @click="onCancel"
          >
            取消订单
          </el-button>
        </aside>
      </div>

      <el-empty v-else-if="!loading" description="订单不存在">
        <el-button type="primary" @click="router.push('/orders')">返回我的订单</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrderDetail, payOrder } from '@/api/order'
import { CREDIT_LEVELS, formatDate, formatDateTime, formatMoney } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const orderId = route.params.orderId

const order = ref(null)
const loading = ref(false)
const paying = ref(false)
const payMethod = ref('MOCK_WECHAT')
const paidResult = ref(null)

function levelColor(code) {
  return CREDIT_LEVELS[code]?.color || '#909399'
}

function payMethodLabel(method) {
  if (method === 'MOCK_ALIPAY') return '模拟支付宝'
  if (method === 'MOCK_WECHAT') return '模拟微信支付'
  return method
}

async function load() {
  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    order.value = res.data
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

async function onPay() {
  paying.value = true
  try {
    const res = await payOrder(orderId, { payMethod: payMethod.value })
    paidResult.value = res.data
    ElMessage.success('模拟支付成功，订单已进入待取车')
    await load()
  } catch (e) {
    // 拦截器已提示（如车辆被抢、重复支付、状态不允许）
    await load()
  } finally {
    paying.value = false
  }
}

async function onCancel() {
  try {
    await ElMessageBox.confirm(
      '取消后订单不可恢复，需要重新下单。确定取消吗？',
      '取消订单',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  try {
    await cancelOrder(orderId, { reason: '用户在收银台取消' })
    ElMessage.success('订单已取消')
    router.replace('/orders')
  } catch (e) {
    await load()
  }
}

onMounted(load)
</script>

<style scoped>
.cashier-page {
  padding: 20px 0 60px;
}

.breadcrumb {
  font-size: 13px;
  margin-bottom: 16px;
}

.steps {
  margin: 6px 0 22px;
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

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
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

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.order-head h2 {
  margin: 0;
  font-size: 20px;
}

.order-no {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.pay-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.pay-item {
  width: 100%;
  height: auto;
  margin: 0;
  padding: 14px 16px;
}

.pay-inner {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 4px;
}

.pay-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.pay-desc {
  font-size: 12px;
  color: #a8abb2;
}

.mock-alert {
  margin-top: 16px;
}

.row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
  padding: 7px 0;
}

.divider {
  height: 1px;
  background: #ebeef5;
  margin: 10px 0;
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
  margin-bottom: 10px;
}

.credit-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  padding: 4px 0;
}

.credit-row b {
  color: #303133;
}

.credit-row.saved b {
  color: #67c23a;
}

.credit-note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.6;
}

.pay-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}

.cancel-btn {
  width: 100%;
  color: #909399;
}

.success-card {
  max-width: 860px;
  margin: 0 auto;
}

.paid-amount {
  font-size: 15px;
  color: #606266;
}

.paid-amount b {
  color: #f56c6c;
  font-size: 26px;
}

.flow-block {
  margin-top: 8px;
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafcff;
  flex-wrap: wrap;
}

.flow-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.flow-no {
  font-size: 12px;
  color: #606266;
  font-family: ui-monospace, Menlo, Consolas, monospace;
  word-break: break-all;
}

.flow-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.flow-channel {
  font-size: 12px;
  color: #909399;
}

.flow-right b {
  font-size: 16px;
  color: #303133;
}

.block-title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.flow-note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.7;
}

.success-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}

.mt-12 {
  margin-top: 12px;
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
