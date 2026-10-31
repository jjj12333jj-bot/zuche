<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>车辆档案</h2>
        <p class="sub">
          平台视角可见全部网点的车。品牌由车型反查，不能单独填。
          有未结束订单的车不能改车型、不能改归属网点、不能下架——
          订单是按「网点 + 车型快照」结账的，中途改掉账单和资产就对不上了。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">车辆建档</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.storeId" placeholder="全部网点" clearable style="width: 150px" @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="query.brandId" placeholder="全部品牌" clearable filterable style="width: 140px" @change="onSearch">
          <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="onSearch">
          <el-option label="可租" :value="1" />
          <el-option label="已租出" :value="2" />
          <el-option label="维保中" :value="3" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="车牌 / 编号 / VIN / 车型"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 台</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="车辆" min-width="180">
          <template #default="{ row }">
            <div class="plate">{{ row.licensePlate }}</div>
            <div class="account">{{ row.vehicleNo }} · {{ row.color || '未填色' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="品牌 / 车型" min-width="160">
          <template #default="{ row }">
            <div class="name">{{ row.brandName || '—' }}</div>
            <div class="account">{{ row.modelName || '—' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="归属网点" min-width="130">
          <template #default="{ row }">
            <span class="name">{{ row.storeName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="租金 / 押金" width="130" align="right">
          <template #default="{ row }">
            <div class="money">¥{{ formatMoney(row.dailyPrice) }}</div>
            <div class="account">押 ¥{{ formatMoney(row.deposit) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="里程" width="90" align="right">
          <template #default="{ row }">
            <span class="name">{{ row.currentMileage ?? 0 }} 万</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="当前订单" min-width="150">
          <template #default="{ row }">
            <template v-if="row.activeOrderNo">
              <code class="code">{{ row.activeOrderNo }}</code>
              <div class="account">{{ row.activeStartDate }} → {{ row.activeEndDate }}</div>
            </template>
            <span v-else class="account">空闲</span>
          </template>
        </el-table-column>

        <el-table-column label="保单到期" width="120">
          <template #default="{ row }">
            <div v-if="row.insuranceEndDate" :class="['name', insuranceClass(row.insuranceEndDate)]">
              {{ row.insuranceEndDate }}
            </div>
            <div v-else class="account warn">无有效保单</div>
            <div v-if="row.nextMaintainDate" class="account">保养 {{ row.nextMaintainDate }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(cmd) => onCommand(cmd, row)">
              <el-button link type="primary">状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="1" :disabled="row.status === 1 || row.status === 2">置为可租</el-dropdown-item>
                  <el-dropdown-item command="3" :disabled="row.status === 3 || row.status === 2">置为维保中</el-dropdown-item>
                  <el-dropdown-item command="0" :disabled="row.status === 0 || row.status === 2">下架</el-dropdown-item>
                  <el-dropdown-item command="delete" divided :disabled="!!row.activeOrderNo">删除档案</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑车辆档案' : '车辆建档'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <div class="form-grid">
          <el-form-item label="车辆编号" prop="vehicleNo">
            <el-input v-model="form.vehicleNo" maxlength="32" placeholder="V2024001" />
          </el-form-item>
          <el-form-item label="车牌号" prop="licensePlate">
            <el-input v-model="form.licensePlate" maxlength="12" placeholder="闽C·A12345" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="品牌">
            <el-select v-model="form.brandId" filterable placeholder="选择品牌" style="width: 100%" @change="onBrandChange">
              <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="车型" prop="modelId">
            <el-select v-model="form.modelId" filterable placeholder="先选品牌" style="width: 100%">
              <el-option v-for="m in formModels" :key="m.id" :label="m.modelName" :value="m.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="归属网点" prop="storeId">
            <el-select v-model="form.storeId" filterable placeholder="选择网点" style="width: 100%">
              <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id">
                <span>{{ s.name }}</span>
                <span class="option-sub">{{ s.address }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="车身颜色">
            <el-input v-model="form.color" maxlength="20" placeholder="珍珠白" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="日租金">
            <el-input-number v-model="form.dailyPrice" :min="1" :max="99999" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="标准押金">
            <el-input-number v-model="form.deposit" :min="0" :max="999999" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="当前里程">
            <el-input-number v-model="form.currentMileage" :min="0" :max="999.99" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="租金折扣">
            <el-input-number v-model="form.discountRate" :min="0.5" :max="1" :step="0.05" :precision="2" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="上牌日期">
            <el-date-picker v-model="form.registerDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="车架号">
            <el-input v-model="form.vin" maxlength="32" placeholder="选填，17 位车辆识别代号" />
          </el-form-item>
        </div>
        <el-form-item label="可用状态">
          <el-radio-group v-model="form.status" :disabled="!!form.id && !!form.activeOrderNo">
            <el-radio-button :value="1">可租</el-radio-button>
            <el-radio-button :value="3">维保中</el-radio-button>
            <el-radio-button :value="0">下架</el-radio-button>
          </el-radio-group>
          <span v-if="form.activeOrderNo" class="form-hint warn">该车有未结束订单，状态不可改</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" :title="detail ? detail.licensePlate : '车辆详情'" size="420px">
      <template v-if="detail">
        <div class="detail-block">
          <div class="detail-title">资产信息</div>
          <div class="kv"><span>车辆编号</span><b>{{ detail.vehicleNo }}</b></div>
          <div class="kv"><span>品牌车型</span><b>{{ detail.brandName }} {{ detail.modelName }}</b></div>
          <div class="kv"><span>归属网点</span><b>{{ detail.storeName }}</b></div>
          <div class="kv"><span>上牌日期</span><b>{{ detail.registerDate || '—' }}</b></div>
          <div class="kv"><span>当前里程</span><b>{{ detail.currentMileage ?? 0 }} 万公里</b></div>
          <div class="kv"><span>车架号</span><b class="mono">{{ detail.vin || '—' }}</b></div>
          <div class="kv"><span>颜色</span><b>{{ detail.color || '—' }}</b></div>
        </div>

        <div class="detail-block">
          <div class="detail-title">经营数据</div>
          <div class="kv"><span>日租金 / 押金</span><b>¥{{ formatMoney(detail.dailyPrice) }} / ¥{{ formatMoney(detail.deposit) }}</b></div>
          <div class="kv"><span>折扣系数</span><b>{{ detail.discountRate }}</b></div>
          <div class="kv"><span>累计出租</span><b>{{ detail.rentCount || 0 }} 次</b></div>
          <div class="kv"><span>详情浏览</span><b>{{ detail.viewCount || 0 }} 次</b></div>
          <div class="kv"><span>当前状态</span>
            <el-tag size="small" :type="statusType(detail.status)" effect="plain">{{ detail.statusLabel }}</el-tag>
          </div>
        </div>

        <div class="detail-block">
          <div class="detail-title">合规与维保</div>
          <div class="kv"><span>保单到期</span>
            <b :class="insuranceClass(detail.insuranceEndDate)">{{ detail.insuranceEndDate || '无有效保单' }}</b>
          </div>
          <div class="kv"><span>下次保养</span><b>{{ detail.nextMaintainDate || '未排期' }}</b></div>
        </div>

        <div class="detail-block">
          <div class="detail-title">当前占用订单</div>
          <template v-if="detail.activeOrderNo">
            <div class="kv"><span>订单号</span><b class="mono">{{ detail.activeOrderNo }}</b></div>
            <div class="kv"><span>租期</span><b>{{ detail.activeStartDate }} → {{ detail.activeEndDate }}</b></div>
            <div class="kv"><span>订单状态</span><b>{{ detail.activeOrderStatus }}</b></div>
          </template>
          <div v-else class="account">当前无未结束订单，车辆处于空闲状态</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { formatMoney } from '@/utils/format'
import {
  createAdminVehicle,
  deleteAdminVehicle,
  getAdminStoreOptions,
  getAdminVehicles,
  getBrandOptions,
  getModelOptions,
  updateAdminVehicle,
  updateAdminVehicleStatus
} from '@/api/adminVehicle'

const STATUS_TYPE = { 0: 'info', 1: 'success', 2: 'warning', 3: 'danger' }

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const brands = ref([])
const stores = ref([])
const formModels = ref([])
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const detail = ref(null)
const formRef = ref()

const query = reactive({ page: 1, size: 10, storeId: null, brandId: null, status: null, keyword: '' })

const emptyForm = () => ({
  id: null,
  vehicleNo: '',
  licensePlate: '',
  brandId: null,
  modelId: null,
  storeId: null,
  color: '',
  vin: '',
  currentMileage: 0,
  dailyPrice: null,
  deposit: null,
  discountRate: 1,
  registerDate: '',
  status: 1,
  description: '',
  activeOrderNo: ''
})

const form = reactive(emptyForm())

const rules = {
  vehicleNo: [{ required: true, message: '请输入车辆编号', trigger: 'blur' }],
  licensePlate: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  modelId: [{ required: true, message: '请选择车型', trigger: 'change' }],
  storeId: [{ required: true, message: '请选择归属网点', trigger: 'change' }]
}

function statusType(s) {
  return STATUS_TYPE[s] || 'info'
}

/** 保单到期日越近越显眼：30 天内橙色、已过期红色 */
function insuranceClass(date) {
  if (!date) return 'warn'
  const days = Math.ceil((new Date(date) - new Date()) / 86400000)
  if (days < 0) return 'danger'
  if (days <= 30) return 'warn'
  return ''
}

async function loadRefs() {
  const [b, s] = await Promise.all([getBrandOptions(), getAdminStoreOptions()])
  brands.value = b.data || []
  stores.value = s.data || []
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminVehicles({
      page: query.page,
      size: query.size,
      storeId: query.storeId,
      brandId: query.brandId,
      status: query.status,
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
  query.brandId = null
  query.status = null
  query.keyword = ''
  onSearch()
}

function openCreate() {
  Object.assign(form, emptyForm())
  formModels.value = []
  dialogVisible.value = true
}

async function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    vehicleNo: row.vehicleNo,
    licensePlate: row.licensePlate,
    brandId: row.brandId,
    modelId: row.modelId,
    storeId: row.storeId,
    color: row.color || '',
    vin: row.vin || '',
    currentMileage: Number(row.currentMileage || 0),
    dailyPrice: Number(row.dailyPrice),
    deposit: Number(row.deposit),
    discountRate: Number(row.discountRate || 1),
    registerDate: row.registerDate || '',
    status: row.status === 2 ? 1 : row.status,
    description: row.description || '',
    activeOrderNo: row.activeOrderNo || ''
  })
  await loadModels(row.brandId)
  dialogVisible.value = true
}

function openDetail(row) {
  detail.value = row
  drawerVisible.value = true
}

async function loadModels(brandId) {
  if (!brandId) {
    formModels.value = []
    return
  }
  const res = await getModelOptions(brandId)
  formModels.value = res.data || []
}

async function onBrandChange(brandId) {
  form.modelId = null
  await loadModels(brandId)
}

async function onSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      vehicleNo: form.vehicleNo,
      licensePlate: form.licensePlate,
      modelId: form.modelId,
      storeId: form.storeId,
      color: form.color,
      vin: form.vin,
      currentMileage: form.currentMileage,
      dailyPrice: form.dailyPrice,
      deposit: form.deposit,
      discountRate: form.discountRate,
      registerDate: form.registerDate || null,
      status: form.status,
      description: form.description
    }
    const res = form.id ? await updateAdminVehicle(form.id, payload) : await createAdminVehicle(payload)
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const STATUS_TEXT = { 0: '下架', 1: '可租', 3: '维保中' }

async function onCommand(cmd, row) {
  if (cmd === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确认删除车辆「${row.licensePlate}」的档案？删除后前台不再展示，历史订单仍可追溯。`,
        '删除车辆档案',
        { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
      )
    } catch (e) {
      return
    }
    const res = await deleteAdminVehicle(row.id)
    ElMessage.success(res.message || '已删除')
    await load()
    return
  }

  const target = Number(cmd)
  try {
    await ElMessageBox.confirm(
      `将「${row.licensePlate}」置为「${STATUS_TEXT[target]}」？`,
      '变更车辆状态',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await updateAdminVehicleStatus(row.id, target)
  ElMessage.success(res.message || '已更新')
  await load()
}

onMounted(async () => {
  await loadRefs()
  await load()
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

.account.warn,
.warn {
  color: #e6a23c !important;
}

.danger {
  color: #f56c6c !important;
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

.form-hint.warn {
  color: #e6a23c;
}

.option-sub {
  float: right;
  margin-left: 12px;
  font-size: 11px;
  color: #c0c4cc;
}

.detail-block {
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px dashed #ebeef5;
}

.detail-block:last-child {
  border-bottom: none;
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 10px;
}

.kv {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  line-height: 2;
}

.kv b {
  color: #303133;
  font-weight: 500;
  text-align: right;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
