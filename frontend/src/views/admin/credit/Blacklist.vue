<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>风控名单</h2>
        <p class="sub">
          逾期达到阈值由每日任务自动加入，也可人工加入。<code>限制下单</code> 与 <code>黑名单</code> 会拦截下单，<code>预警</code> 只记录不拦截
        </p>
      </div>
      <div class="head-actions">
        <el-button :loading="scanning" type="warning" plain @click="runScan">
          <el-icon><Timer /></el-icon>
          立即扫描逾期
        </el-button>
        <el-button type="primary" @click="openAdd">手工加入</el-button>
      </div>
    </div>

    <div class="stat-strip">
      <div class="stat">
        <span>逾期订单</span>
        <b class="danger">{{ overview.overdueOrders ?? 0 }}</b>
      </div>
      <div class="stat">
        <span>预警中</span>
        <b class="warn">{{ overview.warningCount ?? 0 }}</b>
      </div>
      <div class="stat">
        <span>限制/黑名单</span>
        <b class="danger">{{ overview.blacklistCount ?? 0 }}</b>
      </div>
      <div class="stat">
        <span>待处理申诉</span>
        <b>{{ overview.pendingAppeals ?? 0 }}</b>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已解除" value="RELEASED" />
        </el-select>
        <el-select v-model="query.listType" placeholder="全部类型" clearable style="width: 140px">
          <el-option label="预警" value="WARNING" />
          <el-option label="限制下单" value="OVERRIDE" />
          <el-option label="黑名单" value="BLACKLIST" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 姓名 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="用户" min-width="150">
          <template #default="{ row }">
            <div class="name">{{ row.realName || row.username }}</div>
            <div class="account">{{ row.username }} · {{ row.phone || '未填手机' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="信用分" width="100">
          <template #default="{ row }">
            <span class="score" :style="{ color: scoreColor(row.creditScore) }">{{ row.creditScore }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.listType)" effect="plain">{{ row.listTypeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="triggerRule" label="触发规则" width="150" show-overflow-tooltip />
        <el-table-column label="生效时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.effectiveTime) }}</template>
        </el-table-column>
        <el-table-column label="到期时间" width="140">
          <template #default="{ row }">
            <span v-if="row.expireTime">{{ formatDateTime(row.expireTime) }}</span>
            <span v-else class="text-muted">长期</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'danger' : 'info'" effect="plain">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'ACTIVE'" link type="primary" @click="onRelease(row)">解除</el-button>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          background
          layout="total, prev, pager, next"
          :total="Number(total)"
          :page-size="query.pageSize"
          @current-change="load"
        />
      </div>
    </div>

    <!-- ==================== 手工加入 ==================== -->
    <el-dialog v-model="addVisible" title="加入风控名单" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="选择用户" required>
          <el-select
            v-model="addForm.userId"
            filterable
            remote
            reserve-keyword
            placeholder="输入用户名 / 手机号搜索"
            :remote-method="searchUsers"
            :loading="searching"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.realName || u.username}（${u.username} · ${u.creditScore} 分）`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名单类型">
          <el-radio-group v-model="addForm.listType">
            <el-radio-button value="WARNING">预警</el-radio-button>
            <el-radio-button value="OVERRIDE">限制下单</el-radio-button>
            <el-radio-button value="BLACKLIST">黑名单</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="加入原因" required>
          <el-input v-model="addForm.reason" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="有效期">
          <el-input-number v-model="addForm.expireDays" :min="1" :max="3650" controls-position="right" style="width: 150px" />
          <span class="form-tip">天；不填表示长期有效</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="submitAdd">确认加入</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 扫描结果 ==================== -->
    <el-dialog v-model="scanVisible" title="逾期扫描结果" width="640px">
      <div class="scan-grid">
        <div class="scan-item">
          <span>候选订单</span>
          <b>{{ scanResult.candidateCount ?? 0 }}</b>
        </div>
        <div class="scan-item">
          <span>新置逾期</span>
          <b class="danger">{{ scanResult.markedCount ?? 0 }}</b>
        </div>
        <div class="scan-item">
          <span>新增预警</span>
          <b class="warn">{{ scanResult.warnedCount ?? 0 }}</b>
        </div>
        <div class="scan-item">
          <span>新增黑名单</span>
          <b class="danger">{{ scanResult.blacklistedCount ?? 0 }}</b>
        </div>
        <div class="scan-item">
          <span>自动解除</span>
          <b>{{ scanResult.releasedCount ?? 0 }}</b>
        </div>
      </div>
      <div v-if="(scanResult.details || []).length" class="scan-log">
        <div v-for="(item, index) in scanResult.details" :key="index" class="scan-line">{{ item }}</div>
      </div>
      <el-empty v-else description="本次没有订单超期，也没有名单变化" :image-size="70" />
      <template #footer>
        <el-button type="primary" @click="scanVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addBlacklist,
  getAdminUsers,
  getBlacklist,
  getRiskOverview,
  releaseBlacklist,
  runOverdueScan
} from '@/api/adminCredit'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const overview = ref({})
const query = reactive({ status: 'ACTIVE', listType: '', keyword: '', current: 1, pageSize: 10 })

const addVisible = ref(false)
const adding = ref(false)
const searching = ref(false)
const userOptions = ref([])
const addForm = reactive({ userId: null, listType: 'WARNING', reason: '', expireDays: 90 })

const scanning = ref(false)
const scanVisible = ref(false)
const scanResult = ref({})

function scoreColor(score) {
  const n = Number(score || 0)
  if (n >= 120) return '#67c23a'
  if (n >= 100) return '#409eff'
  if (n >= 80) return '#e6a23c'
  return '#f56c6c'
}

function typeTag(type) {
  if (type === 'BLACKLIST') return 'danger'
  if (type === 'OVERRIDE') return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getBlacklist(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadOverview() {
  const res = await getRiskOverview()
  overview.value = res.data || {}
}

function onSearch() {
  query.current = 1
  load()
}

function onReset() {
  query.status = 'ACTIVE'
  query.listType = ''
  query.keyword = ''
  query.current = 1
  load()
}

async function openAdd() {
  addForm.userId = null
  addForm.listType = 'WARNING'
  addForm.reason = ''
  addForm.expireDays = 90
  userOptions.value = []
  addVisible.value = true
  searchUsers('')
}

async function searchUsers(keyword) {
  searching.value = true
  try {
    const res = await getAdminUsers({ keyword: keyword || undefined, current: 1, pageSize: 20 })
    userOptions.value = res.data?.records || []
  } finally {
    searching.value = false
  }
}

async function submitAdd() {
  if (!addForm.userId) {
    ElMessage.warning('请选择用户')
    return
  }
  if (!addForm.reason || addForm.reason.trim().length < 4) {
    ElMessage.warning('请填写加入原因（至少 4 个字）')
    return
  }
  adding.value = true
  try {
    await addBlacklist({
      userId: addForm.userId,
      listType: addForm.listType,
      reason: addForm.reason.trim(),
      expireDays: addForm.expireDays || undefined
    })
    ElMessage.success('已加入风控名单')
    addVisible.value = false
    await Promise.all([load(), loadOverview()])
  } finally {
    adding.value = false
  }
}

async function onRelease(row) {
  try {
    await ElMessageBox.confirm(
      `确定解除「${row.realName || row.username}」的${row.listTypeLabel}记录吗？解除后该用户可正常下单。`,
      '解除风控名单',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await releaseBlacklist(row.id)
  ElMessage.success('已解除')
  await Promise.all([load(), loadOverview()])
}

async function runScan() {
  scanning.value = true
  try {
    const res = await runOverdueScan()
    scanResult.value = res.data || {}
    scanVisible.value = true
    await Promise.all([load(), loadOverview()])
  } finally {
    scanning.value = false
  }
}

onMounted(() => {
  load()
  loadOverview()
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
  gap: 16px;
  flex-wrap: wrap;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.sub code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #475669;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.stat-strip {
  display: flex;
  gap: 12px;
}

.stat {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px 20px;
  min-width: 110px;
  text-align: center;
}

.stat span {
  display: block;
  font-size: 12px;
  color: #909399;
}

.stat b {
  font-size: 22px;
  color: #303133;
}

.stat b.danger {
  color: #f56c6c;
}

.stat b.warn {
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
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.name {
  font-size: 13px;
  color: #303133;
}

.account {
  font-size: 11px;
  color: #a8abb2;
}

.score {
  font-size: 16px;
  font-weight: 700;
}

.text-muted {
  color: #a8abb2;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #a8abb2;
}

.scan-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}

.scan-item {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.scan-item span {
  display: block;
  font-size: 12px;
  color: #909399;
}

.scan-item b {
  font-size: 20px;
  color: #303133;
}

.scan-item b.danger {
  color: #f56c6c;
}

.scan-item b.warn {
  color: #e6a23c;
}

.scan-log {
  max-height: 220px;
  overflow: auto;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 14px;
}

.scan-line {
  font-size: 12px;
  color: #606266;
  line-height: 1.9;
  border-bottom: 1px dashed #ebeef5;
}

.scan-line:last-child {
  border-bottom: none;
}

@media (max-width: 900px) {
  .scan-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
