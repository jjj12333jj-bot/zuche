<template>
  <div class="page">
    <!-- ==================== 页头 ==================== -->
    <div class="page-head">
      <div>
        <h2>网点订单</h2>
        <p class="sub">
          只显示本网点订单。交付与归还会直接改动订单状态、车辆状态与客户信用分，
          操作前请核对车况。
        </p>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="load" />
    </div>

    <div class="card">
      <!-- ==================== 状态页签 ==================== -->
      <el-tabs v-model="status" @tab-change="onSearch">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待交付" name="WAIT_PICKUP" />
        <el-tab-pane label="在租" name="IN_USE" />
        <el-tab-pane label="待归还" name="WAIT_RETURN" />
        <el-tab-pane label="逾期" name="OVERDUE" />
        <el-tab-pane label="已完成" name="COMPLETED" />
      </el-tabs>

      <!-- ==================== 搜索 ==================== -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="订单号 / 客户姓名 / 手机号"
          clearable
          style="width: 260px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <!-- ==================== 表格 ==================== -->
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="订单号" width="180">
          <template #default="{ row }">
            <code class="code">{{ row.orderNo }}</code>
          </template>
        </el-table-column>

        <el-table-column label="客户" width="120">
          <template #default="{ row }">
            <div class="name">{{ row.customerName || row.customerUsername || '—' }}</div>
            <div class="account">{{ row.customerPhone || '—' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="车辆" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="vehicle">{{ row.vehicleNames || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="租期" width="180">
          <template #default="{ row }">
            <div class="date">{{ row.startDate }} → {{ row.endDate }}</div>
            <div class="account">{{ row.rentDays }} 天 · {{ row.vehicleCount }} 台</div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="130" align="right">
          <template #default="{ row }">
            <div class="money">{{ money(row.totalAmount) }}</div>
            <div class="account">租金 {{ money(row.rentAmount) }} · 押金 {{ money(row.depositAmount) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="信用" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="levelType(row.creditLevelSnapshot)">
              {{ row.creditLevelLabel || row.creditLevelSnapshot || '—' }}
            </el-tag>
            <div class="account">× {{ row.depositCoefficient }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" width="120">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.canPickup"
              link
              type="primary"
              :loading="actingId === row.id"
              @click="onPickup(row)"
            >
              办理交付
            </el-button>
            <el-button
              v-else-if="row.canConfirmReturn"
              link
              type="warning"
              :loading="actingId === row.id"
              @click="onReturn(row)"
            >
              办理归还
            </el-button>
            <span v-else class="account">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- ==================== 分页 ==================== -->
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getStoreOrders, storePickup, storeConfirmReturn } from '@/api/store'

const loading = ref(false)
const actingId = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const keyword = ref('')

const nf = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + nf.format(Number(v))
}

function formatDateTime(v) {
  if (!v) return '—'
  const d = new Date(v)
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const STATUS_TYPE = {
  WAIT_PAY: 'info',
  WAIT_PICKUP: 'warning',
  IN_USE: 'primary',
  WAIT_RETURN: 'warning',
  OVERDUE: 'danger',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

function statusType(s) {
  return STATUS_TYPE[s] || 'info'
}

const LEVEL_TYPE = { EXCELLENT: 'success', GOOD: 'primary', NORMAL: 'warning', POOR: 'danger' }

function levelType(s) {
  return LEVEL_TYPE[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getStoreOrders({
      status: status.value,
      keyword: keyword.value.trim(),
      page: page.value,
      size: size.value
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
  status.value = ''
  page.value = 1
  load()
}

/** 办理交付：状态不可逆，先确认再提交 */
async function onPickup(row) {
  try {
    await ElMessageBox.confirm(
      `确认为订单 ${row.orderNo} 办理交付？车辆：${row.vehicleNames || '—'}`,
      '办理交付',
      { type: 'warning', confirmButtonText: '确认交付', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  actingId.value = row.id
  try {
    const res = await storePickup(row.id)
    ElMessage.success(res.message || '已办理交付')
    await load()
  } finally {
    actingId.value = null
  }
}

/** 办理归还：会扣违约金、退押金、结算信用分，提示里把金额说清楚 */
async function onReturn(row) {
  try {
    await ElMessageBox.confirm(
      `确认为订单 ${row.orderNo} 办理归还？计划还车日 ${row.endDate}。` +
        '系统将按实际逾期天数从押金中扣除违约金，多余部分退还，并同步结算客户信用分。',
      '办理归还',
      { type: 'warning', confirmButtonText: '确认归还', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  actingId.value = row.id
  try {
    const res = await storeConfirmReturn(row.id)
    ElMessage.success(res.message || '归还完成')
    await load()
  } finally {
    actingId.value = null
  }
}

onMounted(load)
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
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 8px 18px 16px;
}

.card :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #475669;
}

.name {
  font-size: 13px;
  color: #303133;
}

.account {
  font-size: 11px;
  color: #a8abb2;
  line-height: 1.5;
}

.vehicle {
  font-size: 13px;
  color: #303133;
}

.date {
  font-size: 12px;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.money {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
