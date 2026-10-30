<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>用户管理</h2>
        <p class="sub">
          账号维度：角色、归属网点、账号状态、消费贡献。
          <strong>冻结</strong>只改 status 不删数据，且旧 token 会立刻失效——JWT 是无状态的，
          签出去收不回来，所以拦截器每次请求都会核对一次账号状态（结果缓存 60 秒）。
          <strong>调分</strong>必须写原因，并走信用日志入口，保证「分数 = 100 + Σ 日志」永远成立。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">用户总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">正常账号</div>
        <div class="stat-value success">{{ stats.normal }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已冻结</div>
        <div class="stat-value danger">{{ stats.frozen }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">普通用户</div>
        <div class="stat-value">{{ stats.customers }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">网点管理员</div>
        <div class="stat-value">{{ stats.storeAdmins }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">本月新增</div>
        <div class="stat-value">{{ stats.monthNew }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 姓名 / 手机号 / 邮箱"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.roleCode" placeholder="全部角色" clearable style="width: 140px" @change="onSearch">
          <el-option label="普通用户" value="USER" />
          <el-option label="网点管理员" value="STORE_ADMIN" />
          <el-option label="系统管理员" value="SYS_ADMIN" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="onSearch">
          <el-option label="正常" :value="1" />
          <el-option label="已冻结" :value="0" />
        </el-select>
        <el-select v-model="query.level" placeholder="全部等级" clearable style="width: 120px" @change="onSearch">
          <el-option label="优秀" value="EXCELLENT" />
          <el-option label="良好" value="GOOD" />
          <el-option label="一般" value="NORMAL" />
          <el-option label="待观察" value="WATCH" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 人</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="用户" min-width="170">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="30" :src="row.avatar || undefined" class="avatar">
                {{ (row.realName || row.username || '?').slice(0, 1) }}
              </el-avatar>
              <div>
                <div class="name">
                  {{ row.realName || row.username }}
                  <el-tag v-if="row.status === 0" size="small" type="danger" effect="plain" class="mini-tag">已冻结</el-tag>
                </div>
                <div class="account">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="112">
          <template #default="{ row }">
            <el-tag size="small" :type="roleType(row.roleCode)" effect="plain">{{ row.roleName || row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="124" />
        <el-table-column label="归属网点" width="118" show-overflow-tooltip>
          <template #default="{ row }">{{ row.storeName || '—' }}</template>
        </el-table-column>
        <el-table-column label="信用分" width="104">
          <template #default="{ row }">
            <span class="score" :style="{ color: row.levelColor }">{{ row.creditScore }}</span>
            <span class="score-max">/150</span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :color="row.levelColor" class="level-tag">{{ row.levelLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单" width="92" align="center">
          <template #default="{ row }">
            <div class="cell-main">{{ row.orderCount }}</div>
            <div class="cell-sub">完成 {{ row.finishedCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="累计消费" width="112" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ formatMoney(row.totalSpend) }}</span>
            <div v-if="row.activeCount > 0" class="cell-sub">进行中 {{ row.activeCount }}</div>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="140">
          <template #default="{ row }">{{ row.lastLoginTime ? formatDateTime(row.lastLoginTime) : '从未登录' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAdjust(row)">调分</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '冻结' : '解冻' }}
            </el-button>
            <el-button link size="small" @click="gotoCredit(row)">信用明细</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的用户" :image-size="90" />
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

    <!-- 调分弹窗 -->
    <el-dialog v-model="adjustVisible" title="人工调分" width="470px" destroy-on-close>
      <div v-if="current" class="adjust-head">
        <div>
          <div class="name">{{ current.realName || current.username }}</div>
          <div class="account">{{ current.username }} · {{ current.roleName }}</div>
        </div>
        <div class="score-now">
          <span class="score" :style="{ color: current.levelColor }">{{ current.creditScore }}</span>
          <span class="arrow">→</span>
          <span class="score" :style="{ color: previewColor }">{{ previewScore }}</span>
        </div>
      </div>

      <el-form :model="adjustForm" label-width="76px" class="adjust-form">
        <el-form-item label="调整分值">
          <el-input-number v-model="adjustForm.scoreChange" :min="-50" :max="50" :step="1" controls-position="right" style="width: 140px" />
          <div class="quick">
            <el-button v-for="v in [-10, -5, 5, 10]" :key="v" size="small" text @click="adjustForm.scoreChange = v">
              {{ v > 0 ? '+' + v : v }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="如：门店投诉取车时爽约，经核实属实"
          />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="调分会写一条 change_type = ADMIN 的信用日志，可在「信用明细」里查到这次操作。"
        />
      </el-form>

      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  adjustUserCredit,
  getUserStats,
  getUsers,
  updateUserStatus
} from '@/api/adminPlatform'
import { formatDateTime, formatMoney } from '@/utils/format'

const router = useRouter()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})

const query = reactive({
  keyword: '',
  roleCode: '',
  status: null,
  level: '',
  page: 1,
  size: 10
})

const adjustVisible = ref(false)
const adjusting = ref(false)
const current = ref(null)
const adjustForm = reactive({ scoreChange: 0, reason: '' })

const previewScore = computed(() => {
  if (!current.value) return 0
  const next = (current.value.creditScore || 0) + (adjustForm.scoreChange || 0)
  return Math.min(150, Math.max(0, next))
})

const previewColor = computed(() => {
  const s = previewScore.value
  if (s >= 120) return '#67C23A'
  if (s >= 100) return '#409EFF'
  if (s >= 80) return '#E6A23C'
  return '#F56C6C'
})

function roleType(code) {
  return { SYS_ADMIN: 'danger', STORE_ADMIN: 'warning', USER: 'info' }[code] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getUsers(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getUserStats()
  stats.value = res.data || {}
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.roleCode = ''
  query.status = null
  query.level = ''
  query.page = 1
  load()
}

function reload() {
  load()
  loadStats()
}

async function toggleStatus(row) {
  const freeze = row.status === 1
  try {
    await ElMessageBox.confirm(
      freeze
        ? `冻结后「${row.realName || row.username}」无法登录，已登录的会话也会立即失效。\n订单、信用记录、评价等数据都会保留，解冻后照旧。`
        : `解冻后「${row.realName || row.username}」可以重新登录并下单。`,
      freeze ? '冻结账号' : '解冻账号',
      {
        confirmButtonText: freeze ? '确认冻结' : '确认解冻',
        cancelButtonText: '取消',
        type: freeze ? 'warning' : 'info'
      }
    )
    const res = await updateUserStatus(row.id, { status: freeze ? 0 : 1 })
    ElMessage.success(res.message || '操作成功')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

function openAdjust(row) {
  current.value = row
  adjustForm.scoreChange = 0
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!adjustForm.scoreChange) {
    ElMessage.warning('调整分值不能为 0')
    return
  }
  if (!adjustForm.reason.trim()) {
    ElMessage.warning('请填写调整原因')
    return
  }
  adjusting.value = true
  try {
    const res = await adjustUserCredit(current.value.id, {
      scoreChange: adjustForm.scoreChange,
      reason: adjustForm.reason.trim()
    })
    const d = res.data || {}
    ElMessage.success(`信用分 ${d.scoreBefore} → ${d.scoreAfter}`)
    adjustVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e?.message || '调整失败')
  } finally {
    adjusting.value = false
  }
}

function gotoCredit(row) {
  router.push({ path: '/admin/credit/users', query: { keyword: row.username } })
}

onMounted(() => {
  load()
  loadStats()
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
  grid-template-columns: repeat(6, 1fr);
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.avatar {
  background: #e8f0fe;
  color: #409eff;
  font-size: 12px;
  flex: none;
}

.name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.account {
  font-size: 11px;
  color: #a8abb2;
}

.mini-tag {
  margin-left: 4px;
  transform: scale(0.86);
}

.score {
  font-size: 15px;
  font-weight: 700;
}

.score-max {
  font-size: 10px;
  color: #c0c4cc;
  margin-left: 1px;
}

.level-tag {
  border: none;
  color: #fff;
  font-size: 11px;
}

.cell-main {
  font-size: 13px;
  color: #303133;
}

.cell-sub {
  font-size: 11px;
  color: #a8abb2;
}

.money {
  font-weight: 600;
  color: #1f2937;
}

.adjust-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #fafbfc;
  border-radius: 10px;
  margin-bottom: 14px;
}

.score-now {
  display: flex;
  align-items: center;
  gap: 10px;
}

.score-now .score {
  font-size: 24px;
}

.arrow {
  color: #c0c4cc;
  font-size: 16px;
}

.quick {
  display: inline-flex;
  gap: 2px;
  margin-left: 10px;
}

.adjust-form :deep(.el-alert) {
  margin-top: 4px;
}

@media (max-width: 1400px) {
  .stat-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
