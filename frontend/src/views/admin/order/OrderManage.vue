<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>订单管理</h2>
        <p class="sub">
          平台视角看全量订单。这里只有「关闭」一个破坏性操作，且只对<strong>待支付 / 待取车</strong>开放——
          已取车的单必须由网点走归还流程，否则押金、违约金、车况都没有结算依据。
          关闭已支付的单会自动全额退款并释放车辆，处理链路与用户自己取消完全一致。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">订单总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">待取车</div>
        <div class="stat-value primary">{{ stats.pendingPickup }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">在租中</div>
        <div class="stat-value success">{{ stats.inUse }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">逾期未还</div>
        <div class="stat-value danger">{{ stats.overdue }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">今日新增</div>
        <div class="stat-value">{{ stats.todayNew }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已结算违约金</div>
        <div class="stat-value warn">¥{{ formatMoney(stats.lateFeeTotal) }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="onSearch">
          <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="query.storeId" placeholder="全部网点" clearable style="width: 150px" @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="取车起"
          end-placeholder="取车止"
          style="width: 240px"
          @change="onSearch"
        />
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 客户 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 笔</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column prop="orderNo" label="订单号" width="196" show-overflow-tooltip />
        <el-table-column label="客户" width="120">
          <template #default="{ row }">
            <div class="cell-main">{{ row.customerName || row.customerUsername || '—' }}</div>
            <div class="cell-sub">{{ row.customerPhone || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="storeName" label="取车网点" width="120" show-overflow-tooltip />
        <el-table-column label="车辆" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.vehicleNames || '—' }}</template>
        </el-table-column>
        <el-table-column label="租期" width="196">
          <template #default="{ row }">
            <span class="mono">{{ row.startDate }} → {{ row.endDate }}</span>
            <div class="cell-sub">{{ row.rentDays }} 天 · {{ row.vehicleCount }} 台</div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="116" align="right">
          <template #default="{ row }">
            <div class="money">¥{{ formatMoney(row.totalAmount) }}</div>
            <div class="cell-sub">押金 ¥{{ formatMoney(row.depositAmount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="light">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row.id)">详情</el-button>
            <el-button
              v-if="row.canCancel"
              link
              type="danger"
              size="small"
              @click="closeOrder(row)"
            >
              关闭
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的订单" :image-size="90" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>

    <!-- 订单详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="640px" destroy-on-close>
      <div v-if="detail" v-loading="detailLoading" class="detail">
        <div class="detail-head">
          <div>
            <div class="detail-no">{{ detail.orderNo }}</div>
            <div class="cell-sub">下单于 {{ formatDateTime(detail.createTime) }}</div>
          </div>
          <el-tag :type="statusType(detail.status)" effect="dark">{{ detail.statusLabel }}</el-tag>
        </div>

        <el-alert
          v-if="detail.overdueDays > 0 && !detail.lateFeeSettled"
          type="warning"
          :closable="false"
          show-icon
          :title="`已逾期 ${detail.overdueDays} 天，预计违约金 ¥${formatMoney(detail.lateFee)}（归还结算时才扣）`"
        />

        <div class="kv-grid">
          <div class="kv"><span>客户</span><b>{{ detail.customerName }}（{{ detail.customerUsername }}）</b></div>
          <div class="kv"><span>手机号</span><b>{{ detail.customerPhone || '—' }}</b></div>
          <div class="kv"><span>取车网点</span><b>{{ detail.storeName }}</b></div>
          <div class="kv"><span>归还网点</span><b>{{ detail.returnStoreName || detail.storeName }}</b></div>
          <div class="kv"><span>租期</span><b>{{ detail.startDate }} → {{ detail.endDate }}（{{ detail.rentDays }} 天）</b></div>
          <div class="kv"><span>信用快照</span><b>{{ detail.creditScoreSnapshot }} / {{ detail.creditLevelLabel }}（系数 {{ coefficientText(detail.depositCoefficient) }}）</b></div>
          <div class="kv"><span>当前信用</span><b>{{ detail.customerCreditScore ?? '—' }}</b></div>
          <div class="kv"><span>押金</span><b>¥{{ formatMoney(detail.depositAmount) }}<i v-if="detail.depositSaved > 0" class="saved">（信用减免 ¥{{ formatMoney(detail.depositSaved) }}）</i></b></div>
          <div class="kv"><span>租金</span><b>¥{{ formatMoney(detail.rentAmount) }}</b></div>
          <div class="kv"><span>订单总额</span><b class="money">¥{{ formatMoney(detail.totalAmount) }}</b></div>
          <div class="kv"><span>押金退还</span><b>{{ depositRefundLabel(detail.depositRefundStatus) }}</b></div>
          <div class="kv"><span>违约金</span><b>{{ detail.lateFeeSettled ? '已结算' : '预估' }} ¥{{ formatMoney(detail.lateFee) }}</b></div>
        </div>

        <div v-if="detail.cancelReason" class="block">
          <div class="block-title">关闭原因</div>
          <div class="cancel-reason">{{ detail.cancelReason }}</div>
        </div>

        <div class="block">
          <div class="block-title">车辆明细</div>
          <el-table :data="detail.items" size="small" border>
            <el-table-column prop="vehicleName" label="车辆" min-width="150" show-overflow-tooltip />
            <el-table-column prop="licensePlate" label="车牌" width="110" />
            <el-table-column label="日租金" width="90" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.dailyPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.rentAmount) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="block">
          <div class="block-title">支付流水</div>
          <el-table :data="detail.payments" size="small" border>
            <el-table-column prop="payNo" label="流水号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="payTypeLabel" label="类型" width="80" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
            </el-table-column>
            <template #empty><span class="empty-text">未支付</span></template>
          </el-table>
        </div>

        <div v-if="detail.refunds && detail.refunds.length" class="block">
          <div class="block-title">退款流水</div>
          <el-table :data="detail.refunds" size="small" border>
            <el-table-column prop="refundNo" label="退款单号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="refundTypeLabel" label="类型" width="80" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ formatDateTime(row.refundTime) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { closeOrder as apiCloseOrder, getOrderDetail, getOrderStats, getOrders } from '@/api/adminPlatform'
import { getAdminStoreOptions } from '@/api/adminVehicle'
import { coefficientText, formatDateTime, formatMoney } from '@/utils/format'

const STATUS_OPTIONS = [
  { value: 'WAIT_PAY', label: '待支付' },
  { value: 'WAIT_PICKUP', label: '待取车' },
  { value: 'IN_USE', label: '已取车/在租' },
  { value: 'WAIT_RETURN', label: '待归还' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'OVERDUE', label: '逾期' }
]

const STATUS_TYPES = {
  WAIT_PAY: 'warning',
  WAIT_PICKUP: 'primary',
  IN_USE: 'success',
  WAIT_RETURN: 'warning',
  COMPLETED: 'info',
  CANCELLED: 'info',
  OVERDUE: 'danger'
}

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const stores = ref([])
const dateRange = ref([])

const query = reactive({
  status: '',
  storeId: null,
  keyword: '',
  startFrom: '',
  startTo: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

function statusType(status) {
  return STATUS_TYPES[status] || 'info'
}

function depositRefundLabel(status) {
  return { NONE: '无需退还', PENDING: '待退还', REFUNDED: '已退还' }[status] || status || '—'
}

async function load() {
  loading.value = true
  try {
    query.startFrom = dateRange.value?.[0] || ''
    query.startTo = dateRange.value?.[1] || ''
    const res = await getOrders(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getOrderStats()
  stats.value = res.data || {}
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.status = ''
  query.storeId = null
  query.keyword = ''
  dateRange.value = []
  query.page = 1
  load()
}

function reload() {
  load()
  loadStats()
}

async function openDetail(id) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getOrderDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

/**
 * 关闭订单：必须填原因。
 * 用 prompt 而不是简单的 confirm——后台关单是「有理由的破坏性操作」，
 * 事后复盘只能靠这句话，不写就没法交代。
 */
async function closeOrder(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `关闭订单 ${row.orderNo}。${row.status === 'WAIT_PICKUP' ? '该单已支付，关闭后会自动全额退款并释放车辆。' : '该单未支付，关闭后仅释放车辆。'}\n请填写关闭原因：`,
      '关闭订单',
      {
        confirmButtonText: '确认关闭',
        cancelButtonText: '取消',
        inputPlaceholder: '如：客户电话要求取消',
        inputValidator: (v) => (v && v.trim().length >= 2 ? true : '请填写至少 2 个字的原因'),
        type: 'warning',
        customClass: 'close-order-box'
      }
    )
    await apiCloseOrder(row.id, { reason: value.trim() })
    ElMessage.success('订单已关闭')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '关闭失败')
    }
  }
}

onMounted(async () => {
  const res = await getAdminStoreOptions()
  stores.value = res.data || []
  await Promise.all([load(), loadStats()])
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
  max-width: 860px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.stat {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 14px 16px;
}

.stat-label {
  font-size: 12px;
  color: #a8abb2;
}

.stat-value {
  margin-top: 6px;
  font-size: 21px;
  font-weight: 600;
  color: #1f2937;
}

.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warn { color: #e6a23c; }
.stat-value.danger { color: #f56c6c; }

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.count-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a8abb2;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.cell-main {
  font-size: 13px;
  color: #303133;
}

.cell-sub {
  font-size: 11px;
  color: #a8abb2;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  color: #606266;
}

.money {
  font-weight: 600;
  color: #1f2937;
}

.empty-text {
  color: #c0c4cc;
  font-size: 12px;
}

/* ---------- 详情 ---------- */
.detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-no {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.kv-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
  padding: 12px 14px;
  background: #fafbfc;
  border-radius: 10px;
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed #ebeef5;
}

.kv span {
  color: #909399;
  flex: none;
}

.kv b {
  color: #303133;
  font-weight: 500;
  text-align: right;
}

.saved {
  font-style: normal;
  color: #67c23a;
  font-size: 11px;
}

.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.cancel-reason {
  font-size: 13px;
  color: #606266;
  background: #fef0f0;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.6;
}

@media (max-width: 1400px) {
  .stat-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
