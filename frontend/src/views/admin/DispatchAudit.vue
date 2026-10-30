<template>
  <div class="page">
    <!-- ==================== 页头 ==================== -->
    <div class="page-head">
      <div>
        <h2>调车审批</h2>
        <p class="sub">
          平台视角可见全部网点的调车申请。审批通过后车辆归属立即迁移到调入网点，
          迁移失败（车已被租走或已被调走）整笔审批回滚，不会出现「申请通过但车没动」。
        </p>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="load" />
    </div>

    <!-- ==================== 待办提示 ==================== -->
    <el-alert
      v-if="pendingCount > 0"
      type="warning"
      :closable="false"
      show-icon
      :title="`当前有 ${pendingCount} 条调车申请待审批`"
    />

    <div class="card">
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
        <el-table-column label="申请单号" width="158">
          <template #default="{ row }"><code class="code">{{ row.applyNo }}</code></template>
        </el-table-column>

        <el-table-column label="车辆" width="150">
          <template #default="{ row }">
            <div class="plate">{{ row.licensePlate }}</div>
            <div class="account">{{ row.vehicleNo }} · {{ row.modelName || '—' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="调拨流向" min-width="220">
          <template #default="{ row }">
            <div class="flow">
              <span class="store">{{ row.fromStoreName }}</span>
              <el-icon class="arrow"><Right /></el-icon>
              <span class="store">{{ row.toStoreName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="调车原因" min-width="180" show-overflow-tooltip prop="reason" />

        <el-table-column label="申请" width="130">
          <template #default="{ row }">
            <div class="name">{{ row.applyUserName || '—' }}</div>
            <div class="account">{{ formatDateTime(row.createTime) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="审批结果" width="170">
          <template #default="{ row }">
            <template v-if="row.auditTime">
              <div class="name">{{ row.auditUserName || '—' }}</div>
              <div class="account">{{ formatDateTime(row.auditTime) }}</div>
              <div v-if="row.auditRemark" class="account remark">{{ row.auditRemark }}</div>
            </template>
            <span v-else class="account">—</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.canAudit">
              <el-button link type="primary" :loading="actingId === row.id" @click="onApprove(row)">通过</el-button>
              <el-button link type="danger" :loading="actingId === row.id" @click="onReject(row)">驳回</el-button>
            </template>
            <span v-else class="account">已处理</span>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Right, Search } from '@element-plus/icons-vue'
import { approveDispatch, getAdminDispatches, rejectDispatch } from '@/api/store'

const loading = ref(false)
const actingId = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const keyword = ref('')
const pendingCount = ref(0)

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
    const res = await getAdminDispatches({
      status: status.value,
      keyword: keyword.value.trim(),
      page: page.value,
      size: size.value
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total || 0)
    // 当前页里的待审批数量，用于顶部提示
    pendingCount.value = rows.value.filter((r) => r.status === 'PENDING').length
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
  keyword.value = ''
  page.value = 1
  load()
}

/** 通过：会真实迁移车辆归属，确认信息里把「从哪个店到哪个店」说清楚 */
async function onApprove(row) {
  try {
    await ElMessageBox.confirm(
      `确认通过？车辆 ${row.licensePlate} 的归属将由「${row.fromStoreName}」迁移至「${row.toStoreName}」。` +
        '迁移后该车从调出网点的车辆列表中消失。',
      '审批通过',
      { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }

  actingId.value = row.id
  try {
    const res = await approveDispatch(row.id, {})
    ElMessage.success(res.message || '已通过')
    await load()
  } finally {
    actingId.value = null
  }
}

/** 驳回：原因必填，后端也会拦（不写理由的驳回等于让网点无限重提） */
async function onReject(row) {
  let remark
  try {
    const result = await ElMessageBox.prompt(
      `驳回申请 ${row.applyNo}，请说明原因：`,
      '审批驳回',
      {
        inputType: 'textarea',
        inputPlaceholder: '例如：该车为调出网点重点运力，暂不外调',
        inputValidator: (value) => (value && value.trim().length >= 4) || '驳回原因至少 4 个字',
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消'
      }
    )
    remark = result.value
  } catch (e) {
    return
  }

  actingId.value = row.id
  try {
    const res = await rejectDispatch(row.id, { remark })
    ElMessage.success(res.message || '已驳回')
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
  padding: 16px 18px;
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
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
</style>
