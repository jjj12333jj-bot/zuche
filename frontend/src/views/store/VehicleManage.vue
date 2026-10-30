<template>
  <div class="page">
    <!-- ==================== 页头 ==================== -->
    <div class="page-head">
      <div>
        <h2>网点车辆</h2>
        <p class="sub">
          只看本网点车辆。价格改动只影响新订单（已成交订单按下单时快照结算）；
          上下架与进维保需该车没有未结束订单。
        </p>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="load" />
    </div>

    <div class="card">
      <el-tabs v-model="tab" @tab-change="onTabChange">
        <!-- ==================== 车辆档案 ==================== -->
        <el-tab-pane label="车辆档案" name="vehicles">
          <div class="filter-bar">
            <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="onSearch">
              <el-option label="可租" :value="1" />
              <el-option label="已租出" :value="2" />
              <el-option label="维保中" :value="3" />
              <el-option label="已下架" :value="0" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="车牌 / 车辆编号 / 品牌 / 车型"
              clearable
              style="width: 260px"
              @keyup.enter="onSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="reset">重置</el-button>
            <span class="count-hint">共 {{ total }} 台</span>
          </div>

          <el-table :data="rows" v-loading="loading" size="small" border>
            <el-table-column label="车辆" width="150">
              <template #default="{ row }">
                <div class="plate">{{ row.licensePlate }}</div>
                <code class="code">{{ row.vehicleNo }}</code>
              </template>
            </el-table-column>

            <el-table-column label="品牌车型" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="name">{{ row.brandName }} {{ row.modelName }}</div>
                <div class="account">{{ row.color || '—' }}</div>
              </template>
            </el-table-column>

            <el-table-column label="日租金 / 押金" width="150" align="right">
              <template #default="{ row }">
                <div class="money">{{ money(row.dailyPrice) }}</div>
                <div class="account">押金 {{ money(row.deposit) }}</div>
              </template>
            </el-table-column>

            <el-table-column label="里程" width="100" align="right">
              <template #default="{ row }">
                <div class="date">{{ row.currentMileage ?? '—' }}</div>
                <div class="account">万公里</div>
              </template>
            </el-table-column>

            <el-table-column label="台账" width="140">
              <template #default="{ row }">
                <div class="account">{{ row.maintCount || 0 }} 条 · 最近 {{ row.lastMaintDate || '无' }}</div>
                <el-tag v-if="dueMaintenance(row.nextMaintDate)" size="small" type="warning" effect="plain">
                  该保养了（{{ row.nextMaintDate }}）
                </el-tag>
                <div v-else class="account">下次 {{ row.nextMaintDate || '未排期' }}</div>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="96" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)" effect="plain">
                  {{ row.statusLabel }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="当前订单" width="140">
              <template #default="{ row }">
                <template v-if="row.occupied">
                  <code class="code">{{ row.activeOrderNo }}</code>
                  <div class="account">{{ row.activeOrderStatusLabel }}</div>
                </template>
                <span v-else class="account">空闲</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="196" align="center" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPrice(row)">调价</el-button>
                <el-button
                  link
                  type="warning"
                  :disabled="row.occupied"
                  @click="openStatus(row)"
                >
                  改状态
                </el-button>
                <el-button link type="info" @click="openLedger(row)">台账</el-button>
              </template>
            </el-table-column>
          </el-table>

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
        </el-tab-pane>

        <!-- ==================== 维保台账 ==================== -->
        <el-tab-pane label="维保台账" name="maintenances">
          <div class="filter-bar">
            <el-select v-model="mType" placeholder="全部类型" clearable style="width: 140px" @change="onMaintenanceSearch">
              <el-option label="保养" value="MAINTAIN" />
              <el-option label="维修" value="REPAIR" />
            </el-select>
            <el-input
              v-model="mKeyword"
              placeholder="车牌 / 车辆编号 / 作业内容"
              clearable
              style="width: 260px"
              @keyup.enter="onMaintenanceSearch"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="onMaintenanceSearch">查询</el-button>
            <el-button @click="resetMaintenance">重置</el-button>
            <span class="count-hint">共 {{ mTotal }} 条</span>
          </div>

          <el-table :data="mRows" v-loading="mLoading" size="small" border>
            <el-table-column label="进场日期" width="110" prop="maintDate" />
            <el-table-column label="车辆" width="150">
              <template #default="{ row }">
                <div class="plate">{{ row.licensePlate }}</div>
                <code class="code">{{ row.vehicleNo }}</code>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.maintType === 'REPAIR' ? 'warning' : 'primary'" effect="plain">
                  {{ row.maintTypeLabel }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="作业内容" min-width="240" show-overflow-tooltip prop="content" />
            <el-table-column label="里程" width="100" align="right">
              <template #default="{ row }">
                <span class="date">{{ row.mileage ?? '—' }}</span>
                <span class="account"> 万公里</span>
              </template>
            </el-table-column>
            <el-table-column label="费用" width="110" align="right">
              <template #default="{ row }"><span class="money">{{ money(row.cost) }}</span></template>
            </el-table-column>
            <el-table-column label="下次保养" width="110">
              <template #default="{ row }">{{ row.nextDate || '—' }}</template>
            </el-table-column>
            <el-table-column label="登记人" width="90">
              <template #default="{ row }">{{ row.operatorName || '—' }}</template>
            </el-table-column>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="mPage"
              v-model:page-size="mSize"
              :total="mTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadMaintenances"
              @size-change="onMaintenanceSearch"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ==================== 调价 ==================== -->
    <el-dialog v-model="price.visible" title="车辆调价" width="440px">
      <div class="dialog-vehicle">{{ price.row?.licensePlate }} · {{ price.row?.brandName }} {{ price.row?.modelName }}</div>
      <el-form label-width="88px" size="default">
        <el-form-item label="日租金">
          <el-input-number v-model="price.form.dailyPrice" :controls="false" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="押金">
          <el-input-number v-model="price.form.deposit" :controls="false" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="改价原因">
          <el-input v-model="price.form.remark" maxlength="200" placeholder="选填，例如：旺季调价" />
        </el-form-item>
      </el-form>
      <p class="dialog-hint">
        当前：日租金 {{ money(price.row?.dailyPrice) }} · 押金 {{ money(price.row?.deposit) }}。
        已成交订单按下单时的价格快照结算，不受本次调价影响。
      </p>
      <template #footer>
        <el-button @click="price.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPrice">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 改状态 ==================== -->
    <el-dialog v-model="statusDlg.visible" title="调整车辆状态" width="440px">
      <div class="dialog-vehicle">
        {{ statusDlg.row?.licensePlate }} · 当前「{{ statusDlg.row?.statusLabel }}」
      </div>
      <el-form label-width="88px" size="default">
        <el-form-item label="目标状态">
          <el-radio-group v-model="statusDlg.form.status">
            <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="statusDlg.form.reason" maxlength="200" placeholder="选填，例如：右前大灯需更换" />
        </el-form-item>
      </el-form>
      <p class="dialog-hint">
        「已租出」由系统按订单自动维护，不能手工设置。下架后该车不再出现在用户端列表。
      </p>
      <template #footer>
        <el-button @click="statusDlg.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitStatus">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 台账抽屉 ==================== -->
    <el-drawer v-model="drawer.visible" size="560px" title="车辆台账">
      <template #header>
        <div class="drawer-head">
          <div class="drawer-plate">{{ drawer.row?.licensePlate }}</div>
          <div class="account">
            {{ drawer.row?.vehicleNo }} · {{ drawer.row?.brandName }} {{ drawer.row?.modelName }}
          </div>
          <div class="account">
            日租金 {{ money(drawer.row?.dailyPrice) }} · 押金 {{ money(drawer.row?.deposit) }}
            · 里程 {{ drawer.row?.currentMileage ?? '—' }} 万公里
            · 状态 {{ drawer.row?.statusLabel }}
          </div>
        </div>
      </template>

      <div class="drawer-body">
        <h4 class="section-title">登记新的保养 / 维修</h4>
        <el-form label-width="86px" size="small">
          <el-form-item label="类型">
            <el-radio-group v-model="mForm.maintType">
              <el-radio-button value="MAINTAIN">保养</el-radio-button>
              <el-radio-button value="REPAIR">维修</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="进场日期">
            <el-date-picker
              v-model="mForm.maintDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="进场里程">
            <el-input-number v-model="mForm.mileage" :controls="false" :precision="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="费用（元）">
            <el-input-number v-model="mForm.cost" :controls="false" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="作业内容">
            <el-input
              v-model="mForm.content"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="例如：更换机油机滤、空调滤芯"
            />
          </el-form-item>
          <el-form-item label="下次保养">
            <el-date-picker
              v-model="mForm.nextDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              placeholder="选填"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="mForm.remark" maxlength="200" placeholder="选填" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="submitMaintenance">登记台账</el-button>
          </el-form-item>
        </el-form>

        <el-divider>历史记录（{{ drawer.records.length }} 条）</el-divider>

        <el-empty v-if="!drawer.records.length" description="该车暂无台账记录" :image-size="60" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="rec in drawer.records"
            :key="rec.id"
            :timestamp="rec.maintDate"
            placement="top"
          >
            <div class="rec-head">
              <el-tag size="small" :type="rec.maintType === 'REPAIR' ? 'warning' : 'primary'" effect="plain">
                {{ rec.maintTypeLabel }}
              </el-tag>
              <span class="rec-cost">{{ money(rec.cost) }}</span>
              <span class="account">{{ rec.mileage ?? '—' }} 万公里</span>
            </div>
            <div class="rec-content">{{ rec.content }}</div>
            <div class="account">
              登记人 {{ rec.operatorName || '—' }}
              <span v-if="rec.nextDate"> · 下次保养 {{ rec.nextDate }}</span>
              <span v-if="rec.remark"> · {{ rec.remark }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  addMaintenance,
  getMaintenances,
  getStoreVehicles,
  updateVehiclePrice,
  updateVehicleStatus
} from '@/api/store'

const loading = ref(false)
const saving = ref(false)
const tab = ref('vehicles')

/* ---------- 车辆列表 ---------- */
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref(null)
const keyword = ref('')

/* ---------- 台账列表 ---------- */
const mRows = ref([])
const mTotal = ref(0)
const mPage = ref(1)
const mSize = ref(10)
const mType = ref('')
const mKeyword = ref('')
const mLoading = ref(false)

/* ---------- 弹窗 ---------- */
const price = reactive({ visible: false, row: null, form: { dailyPrice: null, deposit: null, remark: '' } })
const statusDlg = reactive({ visible: false, row: null, form: { status: 1, reason: '' } })
const drawer = reactive({ visible: false, row: null, records: [] })

const today = new Date().toISOString().slice(0, 10)

const mForm = reactive({
  maintType: 'MAINTAIN',
  maintDate: today,
  mileage: null,
  cost: null,
  content: '',
  nextDate: '',
  remark: ''
})

const nf = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + nf.format(Number(v))
}

const STATUS_TYPE = { 0: 'info', 1: 'success', 2: 'primary', 3: 'warning' }
function statusType(s) {
  return STATUS_TYPE[s] || 'info'
}

/** 下次保养日期已到/已过，提醒进厂 */
function dueMaintenance(date) {
  return !!date && date <= today
}

/** 可切换的目标状态：排除当前状态与系统维护的「已租出」 */
const statusOptions = computed(() => {
  const current = statusDlg.row?.status
  return [
    { label: '可租', value: 1 },
    { label: '维保中', value: 3 },
    { label: '已下架', value: 0 }
  ].filter((opt) => opt.value !== current)
})

/* ==================== 加载 ==================== */

async function load() {
  loading.value = true
  try {
    const res = await getStoreVehicles({
      status: status.value === null || status.value === '' ? undefined : status.value,
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

async function loadMaintenances() {
  mLoading.value = true
  try {
    const res = await getMaintenances({
      maintType: mType.value,
      keyword: mKeyword.value.trim(),
      page: mPage.value,
      size: mSize.value
    })
    const data = res.data || {}
    mRows.value = data.records || []
    mTotal.value = Number(data.total || 0)
  } finally {
    mLoading.value = false
  }
}

function onSearch() {
  page.value = 1
  load()
}

function reset() {
  status.value = null
  keyword.value = ''
  page.value = 1
  load()
}

function onMaintenanceSearch() {
  mPage.value = 1
  loadMaintenances()
}

function resetMaintenance() {
  mType.value = ''
  mKeyword.value = ''
  mPage.value = 1
  loadMaintenances()
}

function onTabChange(name) {
  if (name === 'maintenances' && !mRows.value.length) {
    loadMaintenances()
  }
}

/* ==================== 调价 ==================== */

function openPrice(row) {
  price.row = row
  price.form.dailyPrice = row.dailyPrice === null ? null : Number(row.dailyPrice)
  price.form.deposit = row.deposit === null ? null : Number(row.deposit)
  price.form.remark = ''
  price.visible = true
}

/** 价格校验放在提交前：输入框不设 min/max，避免非法值被静默改成边界值 */
async function submitPrice() {
  const daily = Number(price.form.dailyPrice)
  const deposit = Number(price.form.deposit)

  if (!Number.isFinite(daily) || daily <= 0) {
    ElMessage.warning('日租金必须大于 0')
    return
  }
  if (daily > 99999) {
    ElMessage.warning('日租金不能高于 99999 元')
    return
  }
  if (!Number.isFinite(deposit) || deposit < 0) {
    ElMessage.warning('押金不能为负数')
    return
  }
  if (deposit > 999999) {
    ElMessage.warning('押金不能高于 999999 元')
    return
  }
  if (daily === Number(price.row?.dailyPrice) && deposit === Number(price.row?.deposit)) {
    ElMessage.warning('日租金与押金均未变化，无需保存')
    return
  }

  saving.value = true
  try {
    const res = await updateVehiclePrice(price.row.id, {
      dailyPrice: daily,
      deposit,
      remark: price.form.remark
    })
    ElMessage.success(res.message || '已保存')
    price.visible = false
    await load()
    // 抽屉里可能正开着同一台车，同步一下头部的价格显示
    if (drawer.visible && drawer.row && drawer.row.id === price.row.id) {
      drawer.row.dailyPrice = daily
      drawer.row.deposit = deposit
    }
  } finally {
    saving.value = false
  }
}

/* ==================== 改状态 ==================== */

function openStatus(row) {
  statusDlg.row = row
  statusDlg.form.reason = ''
  statusDlg.form.status = row.status === 1 ? 3 : 1
  statusDlg.visible = true
}

async function submitStatus() {
  if (statusDlg.form.status === undefined || statusDlg.form.status === null) {
    ElMessage.warning('请选择目标状态')
    return
  }

  saving.value = true
  try {
    const res = await updateVehicleStatus(statusDlg.row.id, {
      status: statusDlg.form.status,
      reason: statusDlg.form.reason
    })
    ElMessage.success(res.message || '已调整')
    statusDlg.visible = false
    await load()
  } finally {
    saving.value = false
  }
}

/* ==================== 台账 ==================== */

async function openLedger(row) {
  drawer.row = row
  drawer.visible = true
  mForm.maintType = 'MAINTAIN'
  mForm.maintDate = today
  mForm.mileage = row.currentMileage === null || row.currentMileage === undefined ? null : Number(row.currentMileage)
  mForm.cost = null
  mForm.content = ''
  mForm.nextDate = ''
  mForm.remark = ''
  await loadLedger(row.id)
}

async function loadLedger(vehicleId) {
  const res = await getMaintenances({ vehicleId, page: 1, size: 50 })
  drawer.records = (res.data || {}).records || []
}

async function submitMaintenance() {
  if (!mForm.maintDate) {
    ElMessage.warning('请选择进场日期')
    return
  }
  if (!mForm.content || mForm.content.trim().length < 4) {
    ElMessage.warning('作业内容至少 4 个字')
    return
  }
  if (mForm.mileage !== null && mForm.mileage !== undefined && Number(mForm.mileage) < 0) {
    ElMessage.warning('里程不能为负数')
    return
  }
  if (mForm.cost !== null && mForm.cost !== undefined && Number(mForm.cost) < 0) {
    ElMessage.warning('费用不能为负数')
    return
  }
  if (mForm.nextDate && mForm.nextDate <= mForm.maintDate) {
    ElMessage.warning('下次保养日期必须晚于本次进场日期')
    return
  }

  saving.value = true
  try {
    const res = await addMaintenance(drawer.row.id, {
      maintType: mForm.maintType,
      maintDate: mForm.maintDate,
      mileage: mForm.mileage,
      cost: mForm.cost,
      content: mForm.content.trim(),
      nextDate: mForm.nextDate || null,
      remark: mForm.remark
    })
    ElMessage.success(res.message || '台账已登记')
    await loadLedger(drawer.row.id)
    await load()
    if (mRows.value.length) {
      await loadMaintenances()
    }
  } finally {
    saving.value = false
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
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.count-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a8abb2;
}

.plate {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  letter-spacing: 0.5px;
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
  line-height: 1.6;
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

/* ---------- 弹窗 ---------- */
.dialog-vehicle {
  margin-bottom: 14px;
  padding: 8px 12px;
  background: #f7f9fc;
  border-radius: 6px;
  font-size: 13px;
  color: #303133;
}

.dialog-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.7;
}

/* ---------- 抽屉 ---------- */
.drawer-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.drawer-plate {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.drawer-body {
  padding: 0 4px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.rec-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rec-cost {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
}

.rec-content {
  margin: 4px 0 2px;
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}
</style>
