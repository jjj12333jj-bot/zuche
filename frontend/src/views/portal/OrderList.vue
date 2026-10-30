<template>
  <div class="orders-page">
    <div class="page-container">
      <div class="page-head">
        <div>
          <h1>我的订单</h1>
          <p class="sub">共 {{ total }} 笔订单 · 支付与退款均为模拟实现</p>
        </div>
        <el-button type="primary" plain @click="router.push('/vehicles')">去租车</el-button>
      </div>

      <el-tabs v-model="status" class="tabs" @tab-change="onTabChange">
        <el-tab-pane v-for="t in TABS" :key="t.value" :label="t.label" :name="t.value" />
      </el-tabs>

      <div v-loading="loading" class="list">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="order-top">
            <span class="order-no">订单号 {{ o.orderNo }}</span>
            <span class="sep">|</span>
            <span class="time">{{ formatDateTime(o.createTime) }}</span>
            <el-tag class="status-tag" size="small" :type="ORDER_STATUS_TAG[o.status]" effect="light">
              {{ o.statusLabel }}
            </el-tag>
          </div>

          <div class="order-body">
            <img :src="resolveImage(o.coverImage)" :alt="o.vehicleNames" @error="onImageError" />
            <div class="info">
              <h3>{{ o.vehicleNames || '车辆信息已失效' }}</h3>
              <p class="meta">
                {{ formatDate(o.startDate) }} 至 {{ formatDate(o.endDate) }} · {{ o.rentDays }} 天 ·
                {{ o.vehicleCount }} 台 · {{ o.storeName }}
              </p>
              <div class="credit-line">
                <el-tag size="small" effect="plain" type="success">
                  下单信用 {{ o.creditLevelLabel || o.creditLevelSnapshot }}
                </el-tag>
                <span class="text-muted">押金系数 ×{{ o.depositCoefficient }}</span>
              </div>
            </div>
            <div class="amount">
              <div class="total">¥{{ formatMoney(o.totalAmount) }}</div>
              <div class="detail">
                租金 ¥{{ formatMoney(o.rentAmount) }} + 押金 ¥{{ formatMoney(o.depositAmount) }}
              </div>
            </div>
          </div>

          <div class="order-actions">
            <el-button size="small" @click="router.push(`/orders/${o.id}`)">查看详情</el-button>
            <el-button
              v-if="o.canCancel && o.status === 'WAIT_PAY'"
              size="small"
              @click="onCancel(o)"
            >
              取消订单
            </el-button>
            <el-button v-if="o.canPay" size="small" type="primary" @click="router.push(`/cashier/${o.id}`)">
              去支付
            </el-button>
          </div>
        </div>

        <el-empty
          v-if="!loading && orders.length === 0"
          description="这里还没有订单"
        >
          <el-button type="primary" @click="router.push('/vehicles')">去挑一台车</el-button>
        </el-empty>
      </div>

      <div v-if="total > 0" class="pager">
        <el-pagination
          v-model:current-page="page"
          background
          layout="prev, pager, next, total"
          :total="Number(total)"
          :page-size="size"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getMyOrders } from '@/api/order'
import { resolveImage, onImageError } from '@/utils/image'
import { formatDate, formatDateTime, formatMoney, ORDER_STATUS_TAG } from '@/utils/format'

const router = useRouter()

const TABS = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'WAIT_PAY' },
  { label: '待取车', value: 'WAIT_PICKUP' },
  { label: '已取车', value: 'IN_USE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const status = ref('')
const orders = ref([])
const total = ref(0)
const page = ref(1)
const size = 5
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getMyOrders({ status: status.value || undefined, page: page.value, size })
    orders.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  load()
}

async function onCancel(order) {
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.orderNo} 吗？未支付订单取消不影响信用分。`,
      '取消订单',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  try {
    await cancelOrder(order.id, { reason: '用户在订单列表取消' })
    ElMessage.success('订单已取消')
    load()
  } catch (e) {
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.orders-page {
  padding: 22px 0 60px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 6px;
}

.page-head h1 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
}

.tabs {
  margin-top: 12px;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.order-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  overflow: hidden;
}

.order-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #fafcff;
  border-bottom: 1px solid #f0f2f5;
  font-size: 12px;
  color: #909399;
}

.order-no {
  color: #606266;
  font-weight: 500;
}

.sep {
  color: #dcdfe6;
}

.status-tag {
  margin-left: auto;
}

.order-body {
  display: flex;
  gap: 16px;
  padding: 18px 20px;
  align-items: center;
}

.order-body img {
  width: 150px;
  aspect-ratio: 16 / 10;
  object-fit: cover;
  border-radius: 8px;
  background: #eef2f6;
  flex-shrink: 0;
}

.info {
  min-width: 0;
}

.info h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.meta {
  margin: 8px 0 10px;
  font-size: 13px;
  color: #606266;
}

.credit-line {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.amount {
  margin-left: auto;
  text-align: right;
  flex-shrink: 0;
}

.amount .total {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.amount .detail {
  margin-top: 6px;
  font-size: 12px;
  color: #a8abb2;
}

.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px 16px;
  border-top: 1px dashed #f0f2f5;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 860px) {
  .order-body {
    flex-wrap: wrap;
  }
  .amount {
    margin-left: 0;
    text-align: left;
  }
}
</style>
