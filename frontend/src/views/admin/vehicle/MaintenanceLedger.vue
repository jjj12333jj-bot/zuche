<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>保养维修</h2>
        <p class="sub">
          台账只增不改——写错了补一条更正记录，而不是抹掉历史。
          登记时进场里程大于档案值会自动把车辆里程往前推；
          登记「维修」且车辆当前空闲，会自动置为维保中，避免维修中的车被下单。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">登记台账</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">台账总数</div>
        <div class="stat-value">{{ total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">保养</div>
        <div class="stat-value ok">{{ summary.maintain }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">维修</div>
        <div class="stat-value warn">{{ summary.repair }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">当期费用</div>
        <div class="stat-value">¥{{ formatMoney(summary.cost) }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.storeId" placeholder="全部网点" clearable style="width: 150px" @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="query.maintType" placeholder="全部类型" clearable style="width: 120px" @change="onSearch">
          <el-option label="保养" value="MAINTAIN" />
          <el-option label="维修" value="REPAIR" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="车牌 / 编号 / 作业内容"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="进场日期" width="105">
          <template #default="{ row }"><span class="name">{{ row.maintDate }}</span></template>
        </el-table-column>

        <el-table-column label="车辆" min-width="150">
          <template #default="{ row }">
            <div class="plate">{{ row.licensePlate || '—' }}</div>
            <div class="account">{{ row.vehicleNo }} · {{ row.brandName }} {{ row.modelName }}</div>
          </template>
        </el-table-column>

        <el-table-column label="网点" min-width="120">
          <template #default="{ row }"><span class="name">{{ row.storeName || '—' }}</span></template>
        </el-table-column>

        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.maintType === 'REPAIR' ? 'danger' : 'success'" effect="plain">
              {{ row.maintLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="作业内容" min-width="220" show-overflow-tooltip prop="content" />

        <el-table-column label="里程" width="90" align="right">
          <template #default="{ row }">
            <span class="account">{{ row.mileage != null ? row.mileage + ' 万' : '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="费用" width="100" align="right">
          <template #default="{ row }"><span class="money">¥{{ formatMoney(row.cost) }}</span></template>
        </el-table-column>

        <el-table-column label="下次保养" width="110">
          <template #default="{ row }">
            <span v-if="row.nextDate" :class="['name', nextClass(row.nextDate)]">{{ row.nextDate }}</span>
            <span v-else class="account">—</span>
          </template>
        </el-table-column>

        <el-table-column label="登记" width="120">
          <template #default="{ row }">
            <div class="name">{{ row.operatorName || '—' }}</div>
            <div class="account">{{ formatDateTime(row.createTime) }}</div>
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

    <el-dialog v-model="dialogVisible" title="登记保养 / 维修" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="车辆" prop="vehicleId">
          <el-select
            v-model="form.vehicleId"
            filterable
            placeholder="输入车牌或编号搜索"
            style="width: 100%"
          >
            <el-option
              v-for="v in vehicles"
              :key="v.id"
              :label="`${v.licensePlate} · ${v.brandName} ${v.modelName}`"
              :value="v.id"
            >
              <span>{{ v.licensePlate }}</span>
              <span class="option-sub">{{ v.storeName }} · {{ v.statusLabel }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="maintType">
          <el-radio-group v-model="form.maintType">
            <el-radio-button value="MAINTAIN">保养</el-radio-button>
            <el-radio-button value="REPAIR">维修</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="进场日期" prop="maintDate">
          <el-date-picker v-model="form.maintDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="进场里程">
          <el-input-number v-model="form.mileage" :min="0" :max="999.99" :precision="2" controls-position="right" style="width: 160px" />
          <span class="form-hint">万公里，大于档案值时自动更新车辆里程</span>
        </el-form-item>
        <el-form-item label="费用">
          <el-input-number v-model="form.cost" :min="0" :max="999999" :precision="2" controls-position="right" style="width: 160px" />
          <span class="form-hint">元</span>
        </el-form-item>
        <el-form-item label="作业内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：更换机油机滤、空调滤芯" />
        </el-form-item>
        <el-form-item label="下次保养">
          <el-date-picker v-model="form.nextDate" type="date" value-format="YYYY-MM-DD" placeholder="选填，留空表示无需提醒" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="200" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { formatDateTime, formatMoney } from '@/utils/format'
import {
  addMaintenance,
  getAdminStoreOptions,
  getAdminVehicles,
  getMaintenances,
  getVehicleAssetStats
} from '@/api/adminVehicle'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const stores = ref([])
const vehicles = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, storeId: null, maintType: '', keyword: '' })

const form = reactive({
  vehicleId: null,
  maintType: 'MAINTAIN',
  maintDate: '',
  mileage: null,
  cost: 0,
  content: '',
  nextDate: '',
  remark: ''
})

const rules = {
  vehicleId: [{ required: true, message: '请选择车辆', trigger: 'change' }],
  maintDate: [{ required: true, message: '请选择进场日期', trigger: 'change' }],
  content: [
    { required: true, message: '请填写作业内容', trigger: 'blur' },
    { min: 4, message: '作业内容至少 4 个字', trigger: 'blur' }
  ]
}

/** 顶部数字卡走全量统计，避免「翻页就变」 */
const summary = ref({ maintain: 0, repair: 0, cost: 0 })

async function loadStats() {
  const res = await getVehicleAssetStats()
  const m = res.data?.maintenance || {}
  summary.value = {
    maintain: m.maintain || 0,
    repair: m.repair || 0,
    cost: Number(m.cost || 0)
  }
}

/** 下次保养已过期标红、30 天内标橙 */
function nextClass(date) {
  const days = Math.ceil((new Date(date) - new Date()) / 86400000)
  if (days < 0) return 'danger'
  if (days <= 30) return 'warn'
  return ''
}

async function load() {
  loading.value = true
  try {
    const res = await getMaintenances({
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      maintType: query.maintType,
      keyword: query.keyword.trim()
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
  query.maintType = ''
  query.keyword = ''
  onSearch()
}

async function openCreate() {
  Object.assign(form, {
    vehicleId: null,
    maintType: 'MAINTAIN',
    maintDate: new Date().toISOString().slice(0, 10),
    mileage: null,
    cost: 0,
    content: '',
    nextDate: '',
    remark: ''
  })
  // 登记时给出全量车辆（含维保中），24 台下拉可直接搜索
  const res = await getAdminVehicles({ page: 1, size: 50 })
  vehicles.value = res.data?.records || []
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await addMaintenance(form.vehicleId, {
      maintType: form.maintType,
      maintDate: form.maintDate,
      mileage: form.mileage,
      cost: form.cost,
      content: form.content,
      nextDate: form.nextDate || null,
      remark: form.remark
    })
    ElMessage.success(res.message || '台账已登记')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } finally {
    saving.value = false
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

.stat-value.ok {
  color: #67c23a;
}

.stat-value.warn {
  color: #e6a23c;
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
