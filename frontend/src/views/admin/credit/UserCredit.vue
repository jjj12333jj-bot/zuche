<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>用户信用</h2>
        <p class="sub">按用户维度查分、调分、看明细。任何调分都会写一条 <code>change_type = ADMIN</code> 的日志</p>
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
          <span>黑名单</span>
          <b class="danger">{{ overview.blacklistCount ?? 0 }}</b>
        </div>
        <div class="stat">
          <span>待处理申诉</span>
          <b>{{ overview.pendingAppeals ?? 0 }}</b>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 姓名 / 手机号"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.level" placeholder="全部等级" clearable style="width: 130px">
          <el-option label="优秀" value="EXCELLENT" />
          <el-option label="良好" value="GOOD" />
          <el-option label="一般" value="NORMAL" />
          <el-option label="待观察" value="WATCH" />
        </el-select>
        <el-select v-model="query.riskType" placeholder="全部风险状态" clearable style="width: 150px">
          <el-option label="正常" value="NONE" />
          <el-option label="预警" value="WARNING" />
          <el-option label="限制下单" value="OVERRIDE" />
          <el-option label="黑名单" value="BLACKLIST" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="用户" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" class="avatar">{{ (row.realName || row.username || '?').slice(0, 1) }}</el-avatar>
              <div>
                <div class="name">{{ row.realName || row.username }}</div>
                <div class="account">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="信用分" width="110">
          <template #default="{ row }">
            <span class="score" :style="{ color: scoreColor(row.creditScore) }">{{ row.creditScore }}</span>
            <span class="score-max">/150</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :color="row.levelColor" class="level-tag">
              {{ row.levelLabel || row.creditLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.riskType" size="small" :type="riskTagType(row.riskType)" effect="plain">
              {{ row.riskTypeLabel }}
            </el-tag>
            <span v-else class="text-muted">正常</span>
          </template>
        </el-table-column>
        <el-table-column label="履约" width="150">
          <template #header>
            <el-tooltip content="统计全部历史订单；取消仅计入支付后取消（未支付取消不算违约）" placement="top">
              <span class="th-help">履约 <el-icon><QuestionFilled /></el-icon></span>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            <span class="text-muted">完成 {{ row.finishedCount ?? 0 }} · 取消 {{ row.cancelCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openAdjust(row)">调整分数</el-button>
            <el-button link @click="openLogs(row)">明细</el-button>
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

    <!-- ==================== 调分弹窗 ==================== -->
    <el-dialog v-model="adjustVisible" title="人工调整信用分" width="520px" destroy-on-close>
      <div class="adjust-user">
        <div>
          <div class="name">{{ currentUser.realName || currentUser.username }}</div>
          <div class="account">{{ currentUser.username }} · {{ currentUser.phone }}</div>
        </div>
        <div class="adjust-score">
          <span>当前</span>
          <b>{{ currentUser.creditScore }}</b>
          <el-icon><Right /></el-icon>
          <b :class="previewClass">{{ previewScore }}</b>
        </div>
      </div>

      <el-form label-width="90px">
        <el-form-item label="调整分值">
          <el-input-number v-model="adjustForm.scoreChange" :min="-150" :max="150" controls-position="right" style="width: 160px" />
          <span class="form-tip">单次上限 ±50，分数整体限制在 0~150</span>
          <div class="quick">
            <el-button v-for="v in QUICK" :key="v" size="small" text @click="adjustForm.scoreChange = v">
              {{ v > 0 ? '+' + v : v }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="必填，至少 5 个字。例如：线下补充材料证明非本人违约，人工恢复"
          />
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="分数始终限制在 0~150；每次调整生成独立的 admin-adj 幂等键，可追溯、不可重复提交"
      />

      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 明细抽屉 ==================== -->
    <el-drawer v-model="logsVisible" :title="`${logUser.realName || logUser.username} · 信用明细`" size="720px">
      <el-table :data="logs" v-loading="logLoading" size="small" border>
        <el-table-column label="时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="事件" min-width="130">
          <template #default="{ row }">{{ row.actionName || row.actionCode }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="info">{{ row.changeTypeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动" width="80" align="right">
          <template #default="{ row }">
            <span class="delta" :class="row.scoreChange > 0 ? 'up' : (row.scoreChange < 0 ? 'down' : 'flat')">
              {{ row.scoreChange > 0 ? '+' : '' }}{{ row.scoreChange }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动后" width="80" align="right">
          <template #default="{ row }">{{ row.scoreAfter }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip />
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="logPage"
          background
          layout="prev, pager, next"
          :total="Number(logTotal)"
          :page-size="10"
          @current-change="loadLogs"
        />
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adjustCredit, getAdminUsers, getRiskOverview, getUserLogs } from '@/api/adminCredit'
import { formatDateTime } from '@/utils/format'

const QUICK = [-20, -10, -5, 5, 10, 20]

// 从「用户管理」页点「信用明细」跳过来时带上用户名，直接定位到这个人
const route = useRoute()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const overview = ref({})

const query = reactive({
  keyword: '',
  level: '',
  riskType: '',
  current: 1,
  pageSize: 10
})

const adjustVisible = ref(false)
const adjusting = ref(false)
const currentUser = ref({})
const adjustForm = reactive({ scoreChange: 5, reason: '' })

const logsVisible = ref(false)
const logLoading = ref(false)
const logUser = ref({})
const logs = ref([])
const logTotal = ref(0)
const logPage = ref(1)

const previewScore = computed(() => {
  const base = Number(currentUser.value.creditScore || 0)
  return Math.max(0, Math.min(150, base + Number(adjustForm.scoreChange || 0)))
})
const previewClass = computed(() => {
  const base = Number(currentUser.value.creditScore || 0)
  if (previewScore.value > base) return 'up'
  if (previewScore.value < base) return 'down'
  return ''
})

function scoreColor(score) {
  const n = Number(score || 0)
  if (n >= 120) return '#67c23a'
  if (n >= 100) return '#409eff'
  if (n >= 80) return '#e6a23c'
  return '#f56c6c'
}

function riskTagType(type) {
  if (type === 'BLACKLIST') return 'danger'
  if (type === 'OVERRIDE') return 'warning'
  return 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getAdminUsers(query)
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
  query.keyword = ''
  query.level = ''
  query.riskType = ''
  query.current = 1
  load()
}

function openAdjust(row) {
  currentUser.value = row
  adjustForm.scoreChange = 5
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  const delta = Number(adjustForm.scoreChange || 0)
  if (!delta) {
    ElMessage.warning('调整分值不能为 0')
    return
  }
  // 输入框不静默钳制，超范围直接说清楚，别让管理员以为"填 60 生效了 60"
  if (Math.abs(delta) > 50) {
    ElMessage.warning(delta > 0 ? '单次最多加 50 分' : '单次最多扣 50 分')
    return
  }
  const base = Number(currentUser.value.creditScore || 0)
  if (delta > 0 && base >= 150) {
    ElMessage.warning(`该用户当前 ${base} 分，已是上限，无法继续加分`)
    return
  }
  if (delta < 0 && base <= 0) {
    ElMessage.warning(`该用户当前 ${base} 分，已到下限，无法继续扣分`)
    return
  }
  if ((adjustForm.reason || '').trim().length < 5) {
    ElMessage.warning('请填写至少 5 个字的调整原因')
    return
  }
  adjusting.value = true
  try {
    const res = await adjustCredit(currentUser.value.id, {
      scoreChange: adjustForm.scoreChange,
      reason: adjustForm.reason.trim()
    })
    ElMessage.success(res.message || '调整成功')
    // 加/减的分数被 0~150 兜住时，把"实际计入多少"说清楚，避免以为写了 50 就真加了 50
    const applied = res.data?.change?.scoreChange
    if (applied !== undefined && applied !== null && Number(applied) !== delta) {
      ElMessage.warning(`本次实际计入 ${Number(applied) > 0 ? '+' : ''}${applied} 分（受 0~150 上下限约束）`)
    }
    adjustVisible.value = false
    await Promise.all([load(), loadOverview()])
  } finally {
    adjusting.value = false
  }
}

function openLogs(row) {
  logUser.value = row
  logPage.value = 1
  logsVisible.value = true
  loadLogs()
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await getUserLogs(logUser.value.id, { current: logPage.value, pageSize: 10 })
    logs.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } finally {
    logLoading.value = false
  }
}

onMounted(() => {
  const kw = route.query.keyword
  if (kw) {
    query.keyword = String(kw)
  }
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
}

.sub code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #475669;
}

.stat-strip {
  display: flex;
  gap: 10px;
}

.stat {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 8px 16px;
  text-align: center;
  min-width: 82px;
}

.stat span {
  display: block;
  font-size: 12px;
  color: #909399;
}

.stat b {
  font-size: 20px;
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.avatar {
  background: #eaf2fb;
  color: #2c5580;
  font-size: 13px;
  flex-shrink: 0;
}

.name {
  font-size: 13px;
  color: #303133;
  line-height: 1.4;
}

.account {
  font-size: 11px;
  color: #a8abb2;
}

.score {
  font-size: 17px;
  font-weight: 700;
}

.score-max {
  margin-left: 2px;
  font-size: 11px;
  color: #c0c4cc;
}

.level-tag {
  border: none;
  color: #fff;
}

.text-muted {
  color: #a8abb2;
  font-size: 12px;
  white-space: nowrap;
}

.form-tip {
  margin-left: 10px;
  color: #a8abb2;
  font-size: 12px;
}

.th-help {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  cursor: help;
  color: #606266;
}

.th-help .el-icon {
  font-size: 12px;
  color: #b1b3b8;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.adjust-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.adjust-score {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.adjust-score b {
  font-size: 22px;
  color: #1f3a5f;
}

.adjust-score b.up {
  color: #67c23a;
}

.adjust-score b.down {
  color: #67c23a;
}

.quick {
  display: inline-flex;
  margin-left: 8px;
}

.quick .el-button {
  padding: 4px 6px;
}

.delta {
  font-weight: 600;
}

.delta.up {
  color: #67c23a;
}

.delta.down {
  color: #f56c6c;
}

.delta.flat {
  color: #909399;
}
</style>
