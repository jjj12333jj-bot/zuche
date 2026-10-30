<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>保险记录</h2>
        <p class="sub">
          同车同险种时段不允许交叉，首尾相接可以（旧单到期当天新单生效）。
          到期天数由数据库按 DATEDIFF 算，不在前端做日期减法——
          时区与「今天」的边界放在一处算才靠得住。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">登记保单</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">保单总数</div>
        <div class="stat-value">{{ total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">30 天内到期</div>
        <div class="stat-value warn">{{ summary.soon }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已过期未续</div>
        <div class="stat-value danger">{{ summary.expired }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">当期保费</div>
        <div class="stat-value">¥{{ formatMoney(summary.premium) }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.storeId" placeholder="全部网点" clearable style="width: 150px" @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="query.insuranceType" placeholder="全部险种" clearable style="width: 130px" @change="onSearch">
          <el-option v-for="t in TYPES" :key="t.code" :label="t.label" :value="t.code" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="车牌 / 保单号 / 保险公司"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-checkbox v-model="query.expiringSoon" @change="onSearch">只看 30 天内到期</el-checkbox>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 张</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="车辆" min-width="170">
          <template #default="{ row }">
            <div class="plate">{{ row.licensePlate || '—' }}</div>
            <div class="account">{{ row.vehicleNo }} · {{ row.brandName }} {{ row.modelName }}</div>
          </template>
        </el-table-column>

        <el-table-column label="险种" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.insuranceType)" effect="plain">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="保险公司" min-width="130">
          <template #default="{ row }"><span class="name">{{ row.insurer }}</span></template>
        </el-table-column>

        <el-table-column label="保单号" width="150">
          <template #default="{ row }"><code class="code">{{ row.policyNo }}</code></template>
        </el-table-column>

        <el-table-column label="保障期间" width="190">
          <template #default="{ row }">
            <div class="name">{{ row.startDate }} → {{ row.endDate }}</div>
            <div class="account">{{ row.storeName }}</div>
          </template>
        </el-table-column>

        <el-table-column label="保费" width="100" align="right">
          <template #default="{ row }"><span class="money">¥{{ formatMoney(row.premium) }}</span></template>
        </el-table-column>

        <el-table-column label="到期" width="120">
          <template #default="{ row }">
            <div :class="['name', expireClass(row)]">{{ expireText(row) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑保单' : '登记保单'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="车辆" prop="vehicleId">
          <el-select v-model="form.vehicleId" filterable placeholder="输入车牌搜索" style="width: 100%">
            <el-option
              v-for="v in vehicles"
              :key="v.id"
              :label="`${v.licensePlate} · ${v.brandName} ${v.modelName}`"
              :value="v.id"
            >
              <span>{{ v.licensePlate }}</span>
              <span class="option-sub">{{ v.storeName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="险种" prop="insuranceType">
          <el-radio-group v-model="form.insuranceType">
            <el-radio-button v-for="t in TYPES" :key="t.code" :value="t.code">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="保险公司" prop="insurer">
          <el-input v-model="form.insurer" maxlength="50" placeholder="人保财险(模拟)" />
        </el-form-item>
        <el-form-item label="保单号" prop="policyNo">
          <el-input v-model="form.policyNo" maxlength="50" placeholder="PICC-C-000001" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="生效日期" prop="startDate">
            <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="到期日期" prop="endDate">
            <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="保费" prop="premium">
          <el-input-number v-model="form.premium" :min="0" :max="999999" :precision="2" controls-position="right" style="width: 180px" />
          <span class="form-hint">元</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { formatMoney } from '@/utils/format'
import {
  createInsurance,
  deleteInsurance,
  getAdminStoreOptions,
  getAdminVehicles,
  getInsurances,
  getVehicleAssetStats,
  updateInsurance
} from '@/api/adminVehicle'

const TYPES = [
  { code: 'COMPULSORY', label: '交强险' },
  { code: 'COMMERCIAL', label: '商业险' },
  { code: 'PASSENGER', label: '座位险' }
]

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const stores = ref([])
const vehicles = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({
  page: 1,
  size: 10,
  storeId: null,
  insuranceType: '',
  keyword: '',
  expiringSoon: false
})

const form = reactive({
  id: null,
  vehicleId: null,
  insuranceType: 'COMPULSORY',
  insurer: '',
  policyNo: '',
  startDate: '',
  endDate: '',
  premium: 950
})

const rules = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  insurer: [{ required: true, message: '请填写保险公司', trigger: 'blur' }],
  policyNo: [{ required: true, message: '请填写保单号', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
  premium: [{ required: true, message: '请填写保费', trigger: 'blur' }]
}

/** 顶部数字卡走全量统计：48 张保单分 5 页，按页算会得出一半的结论 */
const summary = ref({ soon: 0, expired: 0, premium: 0 })

async function loadStats() {
  const res = await getVehicleAssetStats()
  const i = res.data?.insurance || {}
  summary.value = {
    soon: i.soon || 0,
    expired: i.expired || 0,
    premium: Number(i.premium || 0)
  }
}

function typeTag(code) {
  return { COMPULSORY: 'primary', COMMERCIAL: 'success', PASSENGER: 'info' }[code] || 'info'
}

function expireText(row) {
  const d = row.daysToExpire
  if (d == null) return '—'
  if (d < 0) return `已过期 ${-d} 天`
  if (d === 0) return '今天到期'
  return `${d} 天后`
}

function expireClass(row) {
  const d = row.daysToExpire
  if (d == null) return ''
  if (d < 0) return 'danger'
  if (d <= 30) return 'warn'
  return ''
}

function statusTag(row) {
  const d = row.daysToExpire
  if (d != null && d < 0) return 'danger'
  if (row.status === 0) return 'info'
  if (d != null && d <= 30) return 'warning'
  return 'success'
}

async function load() {
  loading.value = true
  try {
    const res = await getInsurances({
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      insuranceType: query.insuranceType,
      keyword: query.keyword.trim(),
      expiringSoon: query.expiringSoon || null
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.storeId = null
  query.insuranceType = ''
  query.keyword = ''
  query.expiringSoon = false
  onSearch()
}

async function ensureVehicles() {
  if (vehicles.value.length) return
  const res = await getAdminVehicles({ page: 1, size: 50 })
  vehicles.value = res.data?.records || []
}

async function openCreate() {
  await ensureVehicles()
  Object.assign(form, {
    id: null,
    vehicleId: null,
    insuranceType: 'COMPULSORY',
    insurer: '人保财险(模拟)',
    policyNo: '',
    startDate: new Date().toISOString().slice(0, 10),
    endDate: '',
    premium: 950
  })
  dialogVisible.value = true
}

async function openEdit(row) {
  await ensureVehicles()
  Object.assign(form, {
    id: row.id,
    vehicleId: row.vehicleId,
    insuranceType: row.insuranceType,
    insurer: row.insurer,
    policyNo: row.policyNo,
    startDate: row.startDate,
    endDate: row.endDate,
    premium: Number(row.premium)
  })
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      vehicleId: form.vehicleId,
      insuranceType: form.insuranceType,
      insurer: form.insurer,
      policyNo: form.policyNo,
      startDate: form.startDate,
      endDate: form.endDate,
      premium: form.premium
    }
    const res = form.id ? await updateInsurance(form.id, payload) : await createInsurance(payload)
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除保单「${row.policyNo}」？`, '删除保单', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  const res = await deleteInsurance(row.id)
  ElMessage.success(res.message || '已删除')
  await Promise.all([load(), loadStats()])
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
  max-width: 820px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.stat-value.warn {
  color: #e6a23c;
}

.stat-value.danger {
  color: #f56c6c;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 18px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
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

.code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 11px;
  color: #475669;
}

.money {
  font-size: 13px;
  font-weight: 600;
  color: #f56c6c;
}

.warn {
  color: #e6a23c !important;
}

.danger {
  color: #f56c6c !important;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 12px;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.option-sub {
  float: right;
  margin-left: 12px;
  font-size: 11px;
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
