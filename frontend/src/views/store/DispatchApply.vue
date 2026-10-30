<template>
  <div class="page">
    <!-- ==================== 页头 ==================== -->
    <div class="page-head">
      <div>
        <h2>网点调车</h2>
        <p class="sub">
          只能申请调出本网点的可租车辆，审批权在平台。审批通过后车辆归属迁移到调入网点，
          本网点车辆列表随之减少。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">发起调车</el-button>
      </div>
    </div>

    <div class="card">
      <!-- ==================== 方向页签 ==================== -->
      <el-tabs v-model="direction" @tab-change="onSearch">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="我调出的" name="OUT" />
        <el-tab-pane label="调入我的" name="IN" />
      </el-tabs>

      <!-- ==================== 筛选 ==================== -->
      <div class="filter-bar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="onSearch">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="CANCELLED" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="申请单号 / 车牌 / 车辆编号"
          clearable
          style="width: 250px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 条</span>
      </div>

      <!-- ==================== 表格 ==================== -->
      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="申请单号" width="160">
          <template #default="{ row }"><code class="code">{{ row.applyNo }}</code></template>
        </el-table-column>

        <el-table-column label="车辆" width="150">
          <template #default="{ row }">
            <div class="plate">{{ row.licensePlate }}</div>
            <div class="account">{{ row.vehicleNo }} · {{ row.modelName || '—' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="调拨流向" min-width="210">
          <template #default="{ row }">
            <div class="flow">
              <span class="store" :class="{ mine: row.fromStoreId === myStoreId }">{{ row.fromStoreName }}</span>
              <el-icon class="arrow"><Right /></el-icon>
              <span class="store" :class="{ mine: row.toStoreId === myStoreId }">{{ row.toStoreName }}</span>
            </div>
            <div class="account">
              {{ row.fromStoreId === myStoreId ? '调出（本网点）' : '调入（本网点）' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="调车原因" min-width="180" show-overflow-tooltip prop="reason" />

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="申请" width="130">
          <template #default="{ row }">
            <div class="name">{{ row.applyUserName || '—' }}</div>
            <div class="account">{{ formatDateTime(row.createTime) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="审批" width="170">
          <template #default="{ row }">
            <template v-if="row.auditTime">
              <div class="name">{{ row.auditUserName || '—' }}</div>
              <div class="account">{{ formatDateTime(row.auditTime) }}</div>
              <div v-if="row.auditRemark" class="account remark">{{ row.auditRemark }}</div>
            </template>
            <span v-else class="account">等待平台处理</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.canCancel"
              link
              type="danger"
              :loading="actingId === row.id"
              @click="onCancel(row)"
            >
              撤回
            </el-button>
            <span v-else class="account">—</span>
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
    </div>

    <!-- ==================== 发起调车 ==================== -->
    <el-dialog v-model="create.visible" title="发起调车申请" width="480px">
      <el-form label-width="90px" size="default">
        <el-form-item label="调出车辆">
          <el-select
            v-model="create.form.vehicleId"
            placeholder="选择本网点可租车辆"
            filterable
            style="width: 100%"
            :loading="create.loading"
          >
            <el-option
              v-for="v in create.vehicles"
              :key="v.id"
              :label="`${v.licensePlate} · ${v.brandName} ${v.modelName}`"
              :value="v.id"
            >
              <span>{{ v.licensePlate }}</span>
              <span class="option-sub">{{ v.brandName }} {{ v.modelName }} · {{ v.vehicleNo }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调入网点">
          <el-select v-model="create.form.toStoreId" placeholder="选择目标网点" style="width: 100%">
            <el-option v-for="s in create.stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调车原因">
          <el-input
            v-model="create.form.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="例如：望京店周末订单积压，需临时补充运力"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-hint">
        仅列出「可租且无未结束订单、也没有待审批申请」的车辆。提交后由平台审批，
        审批通过车辆归属才会迁移。
      </p>
      <template #footer>
        <el-button @click="create.visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Right, Search } from '@element-plus/icons-vue'
import { cancelDispatch, createDispatch, getDispatches, getStoreVehicles, getTargetStores } from '@/api/store'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
/** 当前登录人所属网点，用于区分「调出」和「调入」——登录返回里就带了 storeId */
const myStoreId = computed(() => Number(userStore.userInfo?.storeId || 0))

const loading = ref(false)
const saving = ref(false)
const actingId = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const direction = ref('')
const keyword = ref('')

const create = reactive({
  visible: false,
  loading: false,
  vehicles: [],
  stores: [],
  form: { vehicleId: null, toStoreId: null, reason: '' }
})

const STATUS_TYPE = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }

function statusType(s) {
  return STATUS_TYPE[s] || 'info'
}

function formatDateTime(v) {
  if (!v) return '—'
  const d = new Date(v)
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

async function load() {
  loading.value = true
  try {
    const res = await getDispatches({
      status: status.value,
      direction: direction.value,
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
  status.value = ''
  direction.value = ''
  keyword.value = ''
  page.value = 1
  load()
}

/** 打开弹窗时才拉车辆与网点：多数人只是来看结果的，不该白跑两个请求 */
async function openCreate() {
  create.form.vehicleId = null
  create.form.toStoreId = null
  create.form.reason = ''
  create.visible = true
  create.loading = true
  try {
    const [vehRes, storeRes] = await Promise.all([
      getStoreVehicles({ status: 1, size: 50 }),
      getTargetStores()
    ])
    // 有未结束订单的车后端也会拒绝，这里先过滤掉，少一次白提交
    create.vehicles = ((vehRes.data || {}).records || []).filter((v) => !v.occupied)
    create.stores = storeRes.data || []
  } finally {
    create.loading = false
  }
}

async function submitCreate() {
  if (!create.form.vehicleId) {
    ElMessage.warning('请选择要调出的车辆')
    return
  }
  if (!create.form.toStoreId) {
    ElMessage.warning('请选择调入网点')
    return
  }
  const reason = create.form.reason.trim()
  if (reason.length < 5) {
    ElMessage.warning('调车原因至少 5 个字')
    return
  }

  saving.value = true
  try {
    const res = await createDispatch({
      vehicleId: create.form.vehicleId,
      toStoreId: create.form.toStoreId,
      reason
    })
    ElMessage.success(res.message || '申请已提交')
    create.visible = false
    await load()
  } finally {
    saving.value = false
  }
}

async function onCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认撤回申请 ${row.applyNo}？撤回后该车辆可重新发起调车。`,
      '撤回申请',
      { type: 'warning', confirmButtonText: '确认撤回', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  actingId.value = row.id
  try {
    const res = await cancelDispatch(row.id)
    ElMessage.success(res.message || '已撤回')
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

.head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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

.code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #475669;
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

.remark {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #303133;
}

.store {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.store.mine {
  font-weight: 600;
  color: #3f7ff5;
}

.arrow {
  color: #c0c4cc;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.dialog-hint {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.7;
}

.option-sub {
  float: right;
  margin-left: 12px;
  font-size: 11px;
  color: #a8abb2;
}
</style>
