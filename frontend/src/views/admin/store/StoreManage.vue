<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>网点管理</h2>
        <p class="sub">
          网点是车辆、订单、人员的归属根。<strong>能停用、不轻易删除</strong>：只要名下还有一台车或一笔订单，
          删除接口就会拦下来——否则那些车会挂在一个不存在的网点上，前台按网点筛车永远查不到。
          停用前也会拦一次：<strong>有进行中订单的网点不许停</strong>，客户已经付了钱约好来取车，店却关了门。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增网点</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">网点总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">营业中</div>
        <div class="stat-value success">{{ stats.normal }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已停用</div>
        <div class="stat-value danger">{{ stats.disabled }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">在册车辆</div>
        <div class="stat-value">{{ stats.vehicleTotal }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">本月订单额</div>
        <div class="stat-value">¥{{ formatMoney(stats.monthRevenue) }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="网点名称 / 编码 / 地址 / 电话"
          clearable
          style="width: 250px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="onSearch">
          <el-option label="营业中" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 个</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="网点" min-width="180">
          <template #default="{ row }">
            <div class="cell-main">{{ row.storeName }}</div>
            <div class="cell-sub mono">{{ row.storeCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.address }}</template>
        </el-table-column>
        <el-table-column label="负责人 / 电话" width="160">
          <template #default="{ row }">
            <div class="cell-main">{{ row.managerName || '未指定' }}</div>
            <div class="cell-sub">{{ row.phone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="营业时间" width="118">
          <template #default="{ row }">{{ row.businessHours }}</template>
        </el-table-column>
        <el-table-column label="车辆" width="150" align="center">
          <template #default="{ row }">
            <div class="vehicle-cell">
              <span class="num">{{ row.vehicleTotal }}</span>
              <span class="chip ok">可租 {{ row.vehicleAvailable }}</span>
              <span class="chip busy">在租 {{ row.vehicleRented }}</span>
              <span v-if="row.vehicleMaintenance" class="chip fix">维保 {{ row.vehicleMaintenance }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单" width="96" align="center">
          <template #default="{ row }">
            <div class="cell-main">{{ row.orderTotal }}</div>
            <div v-if="row.activeOrderCount" class="cell-sub warn-text">进行中 {{ row.activeOrderCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="本月订单额" width="118" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.monthRevenue) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="168" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '恢复' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的网点" :image-size="90" />
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑网点' : '新增网点'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <div class="form-grid">
          <el-form-item label="网点编码" prop="storeCode">
            <el-input v-model="form.storeCode" placeholder="如 S005" maxlength="10" :disabled="editing" />
          </el-form-item>
          <el-form-item label="网点名称" prop="storeName">
            <el-input v-model="form.storeName" placeholder="如 西二旗地铁店" maxlength="50" />
          </el-form-item>
        </div>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="福建省泉州市…" maxlength="120" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="010-88886666" maxlength="20" />
          </el-form-item>
          <el-form-item label="营业时间" prop="businessHours">
            <el-input v-model="form.businessHours" placeholder="08:00-21:00" maxlength="50" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="负责人">
            <el-select v-model="form.managerId" placeholder="可不指定" clearable style="width: 100%">
              <el-option
                v-for="u in managers"
                :key="u.id"
                :label="`${u.realName || u.username}（${u.username}）`"
                :value="u.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">营业中</el-radio>
              <el-radio :value="0">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="经度">
            <el-input-number v-model="form.longitude" :precision="6" :step="0.001" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="纬度">
            <el-input-number v-model="form.latitude" :precision="6" :step="0.001" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="hint">经纬度用于网点定位与距离展示。</div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createStore,
  deleteStore,
  getStoreStats,
  getStores,
  updateStore,
  updateStoreStatus
} from '@/api/adminPlatform'
import { getUsers } from '@/api/adminPlatform'
import { formatMoney } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const managers = ref([])

const query = reactive({
  keyword: '',
  status: null,
  page: 1,
  size: 10
})

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  storeCode: '',
  storeName: '',
  address: '',
  phone: '',
  businessHours: '08:00-21:00',
  longitude: null,
  latitude: null,
  managerId: null,
  status: 1
})

const rules = {
  storeCode: [
    { required: true, message: '请填写网点编码', trigger: 'blur' },
    { pattern: /^[A-Z0-9]{2,10}$/, message: '2~10 位大写字母或数字，如 S005', trigger: 'blur' }
  ],
  storeName: [{ required: true, message: '请填写网点名称', trigger: 'blur' }],
  address: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
  phone: [
    { required: true, message: '请填写联系电话', trigger: 'blur' },
    {
      pattern: /^(0\d{2,3}-?\d{7,8}|1[3-9]\d{9})$/,
      message: '座机如 010-88886666，手机如 13800138000',
      trigger: 'blur'
    }
  ],
  businessHours: [{ required: true, message: '请填写营业时间', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await getStores(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getStoreStats()
  stats.value = res.data || {}
}

async function loadManagers() {
  const res = await getUsers({ roleCode: 'STORE_ADMIN', status: 1, page: 1, size: 50 })
  managers.value = res.data?.records || []
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.status = null
  query.page = 1
  load()
}

function reload() {
  load()
  loadStats()
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    storeCode: '',
    storeName: '',
    address: '',
    phone: '',
    businessHours: '08:00-21:00',
    longitude: null,
    latitude: null,
    managerId: null,
    status: 1
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    storeCode: row.storeCode,
    storeName: row.storeName,
    address: row.address,
    phone: row.phone,
    businessHours: row.businessHours,
    longitude: row.longitude,
    latitude: row.latitude,
    managerId: row.managerId,
    status: row.status
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    const res = editing.value
      ? await updateStore(editingId.value, payload)
      : await createStore(payload)
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const disable = row.status === 1
  try {
    await ElMessageBox.confirm(
      disable
        ? `停用后「${row.storeName}」不再接新订单，前台按网点筛选时不再出现。\n若该网点还有进行中的订单，系统会拒绝停用。`
        : `恢复后「${row.storeName}」重新对用户可见，可以接单。`,
      disable ? '停用网点' : '恢复营业',
      { confirmButtonText: disable ? '确认停用' : '确认恢复', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateStoreStatus(row.id, { status: disable ? 0 : 1 })
    ElMessage.success(res.message || '操作成功')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.storeName}」？\n只有从未产生过车辆与订单的空网点才能删除，否则请改用「停用」。`,
      '删除网点',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
    const res = await deleteStore(row.id)
    ElMessage.success(res.message || '已删除')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

onMounted(() => {
  load()
  loadStats()
  loadManagers()
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
  max-width: 880px;
}

.sub strong {
  color: #606266;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
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

.stat-value.success { color: #67c23a; }
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
}

.warn-text {
  color: #e6a23c;
}

.vehicle-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-wrap: wrap;
}

.vehicle-cell .num {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.chip {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  white-space: nowrap;
}

.chip.ok {
  background: #f0f9eb;
  color: #67c23a;
}

.chip.busy {
  background: #ecf5ff;
  color: #409eff;
}

.chip.fix {
  background: #fdf6ec;
  color: #e6a23c;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.hint {
  font-size: 11px;
  color: #a8abb2;
  margin-left: 92px;
  margin-top: -8px;
}

@media (max-width: 1400px) {
  .stat-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
