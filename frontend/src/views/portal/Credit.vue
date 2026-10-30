<template>
  <div class="credit-page">
    <div class="page-container">
      <div class="page-head">
        <div>
          <h1>信用中心</h1>
          <p class="sub">押金折扣由信用等级实时决定 · 每一次加分扣分都有据可查</p>
        </div>
        <el-button plain @click="router.push('/vehicles')">去租车提升信用</el-button>
      </div>

      <div v-loading="loading">
        <div class="hero">
          <div class="score-block">
            <div class="score-line">
              <span class="score">{{ center.creditScore ?? '-' }}</span>
              <span class="score-total">/ 150</span>
            </div>
            <div class="score-meta">
              <el-tag effect="dark" :color="center.levelColor" class="level-tag">
                {{ center.levelLabel || '——' }}
              </el-tag>
              <span v-if="center.scoreToNextLevel" class="next-tip">
                距「{{ center.nextLevelLabel }}」还差 <b>{{ center.scoreToNextLevel }}</b> 分
              </span>
              <span v-else class="next-tip">已是最高信用等级</span>
            </div>
            <el-progress
              class="score-bar"
              :percentage="scorePercent"
              :stroke-width="8"
              :show-text="false"
              :color="center.levelColor || '#409eff'"
            />
          </div>

          <div class="hero-item">
            <span class="label">押金系数</span>
            <div class="big">{{ coefficientText(center.depositCoefficient) }}</div>
            <p class="desc">{{ center.levelDescription || '按当前信用等级计算' }}</p>
          </div>

          <div class="hero-item">
            <span class="label">行为评分</span>
            <div class="big" :class="behaviorClass">{{ center.behaviorScore ?? 0 }}</div>
            <p class="desc">按按时归还率、取消率、逾期次数折算，0 分为满分</p>
          </div>
        </div>

        <div class="stat-grid">
          <div class="stat">
            <span class="stat-label">按时归还率</span>
            <b>{{ formatPercent(stats.onTimeRate) }}</b>
            <span class="stat-note">近 {{ stats.windowSize || 10 }} 单：{{ stats.onTimeCount || 0 }} / {{ stats.finishedCount || 0 }} 单按时</span>
          </div>
          <div class="stat">
            <span class="stat-label">支付后取消率</span>
            <b :class="{ warn: Number(stats.cancelRate) > 0.3 }">{{ formatPercent(stats.cancelRate) }}</b>
            <span class="stat-note">近 {{ stats.windowSize || 10 }} 单：{{ stats.paidCancelCount || 0 }} / {{ stats.paidCount || 0 }} 单取消</span>
          </div>
          <div class="stat">
            <span class="stat-label">逾期订单</span>
            <b :class="{ warn: Number(stats.overdueCount) > 0 }">{{ stats.overdueCount || 0 }}</b>
            <span class="stat-note">近 {{ stats.windowSize || 10 }} 单内，每单每天扣分</span>
          </div>
          <div class="stat">
            <span class="stat-label">已完成订单</span>
            <b>{{ stats.finishedCount || 0 }}</b>
            <span class="stat-note">近 {{ stats.windowSize || 10 }} 单内走完全流程</span>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">
            等级与押金折扣
            <span class="title-note">阈值与系数来自 credit_rule，后台可调</span>
          </h3>
          <div class="levels">
            <div
              v-for="level in center.levels || []"
              :key="level.code"
              class="level-item"
              :class="{ current: level.current }"
              :style="{ borderColor: level.current ? level.color : '#ebeef5' }"
            >
              <div class="lv-head">
                <span class="lv-name" :style="{ color: level.color }">{{ level.name }}</span>
                <el-tag v-if="level.current" size="small" effect="dark" :color="level.color">当前</el-tag>
              </div>
              <div class="lv-range">{{ level.minScore }} - {{ level.maxScore }} 分</div>
              <div class="lv-coef">押金 {{ coefficientText(level.depositCoefficient) }}</div>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="logs-head">
            <h3 class="card-title">
              信用明细
              <span class="title-note">共 {{ total }} 条</span>
            </h3>
            <div class="logs-actions">
              <el-button size="small" plain @click="openMyAppeals">
                我的申诉<span v-if="myAppeals.length">（{{ myAppeals.length }}）</span>
              </el-button>
              <el-radio-group v-model="changeType" size="small" @change="onFilter">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="EVENT">业务事件</el-radio-button>
                <el-radio-button value="BEHAVIOR">行为评分</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <el-table :data="logs" size="small" border empty-text="还没有信用变动记录">
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="事件" min-width="130">
              <template #default="{ row }">
                {{ row.actionName || row.actionCode }}
                <el-tag v-if="row.changeType === 'BEHAVIOR'" size="small" effect="plain" type="info" class="type-tag">
                  行为分校准
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="变动" width="90" align="right">
              <template #default="{ row }">
                <span class="delta" :class="deltaClass(row.scoreChange)">
                  {{ row.scoreChange > 0 ? '+' : '' }}{{ row.scoreChange }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="变动后" width="90" align="right">
              <template #default="{ row }">{{ row.scoreAfter }} 分</template>
            </el-table-column>
            <el-table-column label="关联订单" width="190">
              <template #default="{ row }">
                <el-link
                  v-if="row.orderId"
                  type="primary"
                  :underline="false"
                  @click="router.push(`/orders/${row.orderId}`)"
                >
                  {{ row.orderNo || row.orderId }}
                </el-link>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="88" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="canAppeal(row)"
                  link
                  type="primary"
                  :disabled="pendingLogIds.has(row.id)"
                  @click="openAppeal(row)"
                >
                  {{ pendingLogIds.has(row.id) ? '审核中' : '申诉' }}
                </el-button>
                <span v-else class="text-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="total > size" class="pager">
            <el-pagination
              v-model:current-page="page"
              background
              layout="prev, pager, next"
              :total="Number(total)"
              :page-size="size"
              @current-change="loadLogs"
            />
          </div>
        </div>

        <p class="footnote">
          信用分 = clamp(100 + 全部变动合计, 0, 150)，等级由分数落在哪个区间决定。
          取消已支付订单扣 3 分，按时归还加 5 分，订单完成再加 2 分，逾期每天扣 3 分。
          对扣分有异议可以点明细右侧的「申诉」，审核通过后由管理员补回分数。
        </p>
      </div>
    </div>

    <!-- ==================== 提交申诉 ==================== -->
    <el-dialog v-model="appealVisible" title="提交信用申诉" width="520px" destroy-on-close>
      <div class="appeal-target">
        <div>
          <div class="target-action">{{ appealTarget.actionName || appealTarget.actionCode }}</div>
          <div class="target-time">{{ formatDateTime(appealTarget.createTime) }}</div>
        </div>
        <b class="target-score">{{ appealTarget.scoreChange }}</b>
      </div>
      <p class="appeal-origin">扣分说明：{{ appealTarget.reason || '无' }}</p>

      <el-input
        v-model="appealReason"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="请说明申诉理由（至少 10 个字），例如：还车当天车辆故障，已联系网点救援，不应计入逾期"
      />

      <el-alert
        class="appeal-tip"
        type="info"
        :closable="false"
        show-icon
        title="同一笔扣分只能有一条待处理申诉；申诉期间分数不会冻结，但审核通过后会补回"
      />

      <template #footer>
        <el-button @click="appealVisible = false">取消</el-button>
        <el-button type="primary" :loading="appealSubmitting" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 我的申诉 ==================== -->
    <el-drawer v-model="myAppealsVisible" title="我的申诉" size="600px">
      <el-empty v-if="!myAppeals.length" description="还没有提交过申诉" :image-size="90" />
      <div v-for="item in myAppeals" :key="item.id" class="appeal-item">
        <div class="appeal-head">
          <code class="appeal-no">{{ item.appealNo }}</code>
          <el-tag size="small" :type="appealTagType(item.status)" effect="plain">{{ item.statusLabel }}</el-tag>
        </div>
        <div class="appeal-line">申诉理由：{{ item.appealReason }}</div>
        <div class="appeal-line muted">
          针对：{{ item.actionName || '扣分记录' }}
          <span v-if="item.logScoreChange">（{{ item.logScoreChange }} 分）</span>
          · {{ item.logReason || '' }}
        </div>
        <div v-if="item.handleTime" class="appeal-line muted">
          处理：{{ item.handleRemark }}
          <span v-if="item.status === 'APPROVED'"> · 补回 {{ item.restoreScore }} 分</span>
        </div>
        <div class="appeal-line muted">提交于 {{ formatDateTime(item.createTime) }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAppeal, getCreditCenter, getCreditLogs, getMyAppeals } from '@/api/credit'
import { coefficientText, formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const center = ref({})
const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const changeType = ref('')

const appealVisible = ref(false)
const appealSubmitting = ref(false)
const appealTarget = ref({})
const appealReason = ref('')
const myAppeals = ref([])
const myAppealsVisible = ref(false)

/** 已有待处理申诉的扣分记录，按钮置灰，避免用户重复提交后才被后端拦下 */
const pendingLogIds = computed(
  () => new Set(myAppeals.value.filter((item) => item.status === 'PENDING').map((item) => item.creditLogId))
)

const stats = computed(() => center.value.stats || {})
const scorePercent = computed(() => {
  const score = Number(center.value.creditScore || 0)
  return Math.max(0, Math.min(100, Math.round((score / 150) * 100)))
})
const behaviorClass = computed(() => ({
  neg: Number(center.value.behaviorScore || 0) < 0,
  zero: Number(center.value.behaviorScore || 0) === 0
}))

function formatPercent(rate) {
  if (rate === null || rate === undefined) return '—'
  return `${(Number(rate) * 100).toFixed(0)}%`
}

function deltaClass(value) {
  if (Number(value) > 0) return 'up'
  if (Number(value) < 0) return 'down'
  return 'flat'
}

/** 行为评分是模型折算结果，没有单条事实可复核，只允许对业务事件/人工调整申诉 */
function canAppeal(row) {
  return Number(row.scoreChange) < 0 && row.changeType !== 'BEHAVIOR'
}

async function loadCenter() {
  loading.value = true
  try {
    const res = await getCreditCenter()
    center.value = res.data || {}
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  const res = await getCreditLogs({
    current: page.value,
    pageSize: size,
    changeType: changeType.value || undefined
  })
  logs.value = res.data?.records || []
  total.value = res.data?.total || 0
}

function onFilter() {
  page.value = 1
  loadLogs()
}

async function loadAppeals() {
  const res = await getMyAppeals({ current: 1, pageSize: 50 })
  myAppeals.value = res.data?.records || []
}

function openAppeal(row) {
  appealTarget.value = row
  appealReason.value = ''
  appealVisible.value = true
}

async function submitAppeal() {
  if ((appealReason.value || '').trim().length < 10) {
    ElMessage.warning('申诉理由至少 10 个字，请说明具体情况')
    return
  }
  appealSubmitting.value = true
  try {
    await createAppeal({ creditLogId: appealTarget.value.id, reason: appealReason.value.trim() })
    ElMessage.success('申诉已提交，等待管理员审核')
    appealVisible.value = false
    await loadAppeals()
  } finally {
    appealSubmitting.value = false
  }
}

function openMyAppeals() {
  myAppealsVisible.value = true
}

function appealTagType(status) {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

onMounted(async () => {
  await Promise.all([loadCenter(), loadLogs(), loadAppeals()])
})
</script>

<style scoped>
.credit-page {
  padding: 22px 0 60px;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-head h1 {
  margin: 0;
  font-size: 24px;
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
}

.hero {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.hero > div {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 22px;
}

.score-block {
  background: linear-gradient(135deg, #f4f9ff 0%, #ffffff 70%);
}

.score-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.score {
  font-size: 52px;
  font-weight: 700;
  line-height: 1;
  color: #1f3a5f;
}

.score-total {
  font-size: 14px;
  color: #a8abb2;
}

.score-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.level-tag {
  border: none;
  color: #fff;
}

.next-tip {
  font-size: 12px;
  color: #909399;
}

.next-tip b {
  color: #409eff;
}

.score-bar {
  margin-top: 14px;
}

.hero-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-item .label {
  font-size: 13px;
  color: #909399;
}

.hero-item .big {
  margin: 10px 0 8px;
  font-size: 34px;
  font-weight: 700;
  color: #1f3a5f;
}

.hero-item .big.neg {
  color: #f56c6c;
}

.hero-item .big.zero {
  color: #67c23a;
}

.hero-item .desc {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.6;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat b {
  font-size: 24px;
  color: #303133;
}

.stat b.warn {
  color: #f56c6c;
}

.stat-note {
  font-size: 12px;
  color: #a8abb2;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.card-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
}

.title-note {
  margin-left: 10px;
  font-size: 12px;
  font-weight: 400;
  color: #a8abb2;
}

.levels {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.level-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  background: #fcfcfd;
}

.level-item.current {
  background: #fff;
  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.12);
}

.lv-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.lv-name {
  font-size: 15px;
  font-weight: 600;
}

.lv-range {
  font-size: 12px;
  color: #606266;
}

.lv-coef {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.logs-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.logs-head .card-title {
  margin-bottom: 0;
}

.logs-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.appeal-target {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 16px;
}

.target-action {
  font-size: 14px;
  color: #303133;
}

.target-time {
  margin-top: 4px;
  font-size: 12px;
  color: #a8abb2;
}

.target-score {
  font-size: 24px;
  color: #67c23a;
}

.appeal-origin {
  margin: 12px 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.7;
}

.appeal-tip {
  margin-top: 12px;
}

.appeal-item {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.appeal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.appeal-no {
  font-size: 12px;
  color: #475669;
}

.appeal-line {
  font-size: 13px;
  color: #303133;
  line-height: 1.8;
}

.appeal-line.muted {
  font-size: 12px;
  color: #a8abb2;
}

.type-tag {
  margin-left: 6px;
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

.text-muted {
  color: #c0c4cc;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.footnote {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.8;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }
  .stat-grid,
  .levels {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
