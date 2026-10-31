<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>评价管理</h2>
        <p class="sub">
          两级处置：<strong>隐藏</strong>用于本身没问题、只是不适合挂前台的评价，随时可恢复；
          <strong>删除</strong>只针对辱骂、广告、泄露隐私等违规内容，且仅系统管理员可操作。
          网点作为商家只有解释性回复权，不能删自家差评——让商家既当运动员又当裁判，评分就没人信了。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">评价总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">综合均分</div>
        <div class="stat-value score-value">{{ stats.avgRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">车况均分</div>
        <div class="stat-value score-value">{{ stats.avgVehicleRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">服务均分</div>
        <div class="stat-value score-value">{{ stats.avgStoreRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">五星好评</div>
        <div class="stat-value success">{{ stats.fiveStar }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">三星及以下</div>
        <div class="stat-value danger">{{ stats.lowStar }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">待回复</div>
        <div class="stat-value warn">{{ stats.unreplied }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">回复率</div>
        <div class="stat-value">{{ replyRate }}<i>%</i></div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.rating" placeholder="全部评分" clearable style="width: 120px" @change="onSearch">
          <el-option v-for="r in [5, 4, 3, 2, 1]" :key="r" :label="`${r} 星`" :value="r" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="onSearch">
          <el-option label="已展示" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>
        <el-checkbox v-model="query.unreplied" @change="onSearch">只看待回复</el-checkbox>
        <el-input
          v-model="query.keyword"
          placeholder="订单号 / 客户 / 评价内容"
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
        <el-table-column label="用户 / 订单" width="176">
          <template #default="{ row }">
            <div class="cell-main">{{ row.customerName || row.username || '—' }}</div>
            <div class="cell-sub mono">{{ row.orderNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="车辆 / 网点" min-width="168">
          <template #default="{ row }">
            <div class="cell-main">{{ row.vehicleName }}</div>
            <div class="cell-sub">{{ row.licensePlate }} · {{ row.storeName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
            <div class="cell-sub">
              车况 {{ row.vehicleRating }}.0 · 服务 {{ row.storeRating }}.0
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="260">
          <template #default="{ row }">
            <div class="content">{{ row.content }}</div>
            <div v-if="row.images && row.images.length" class="thumbs">
              <el-image
                v-for="(img, i) in row.images"
                :key="i"
                :src="img"
                :preview-src-list="row.images"
                :initial-index="i"
                fit="cover"
                class="thumb"
                preview-teleported
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="回复" min-width="200">
          <template #default="{ row }">
            <div v-if="row.reply" class="reply">
              {{ row.reply }}
              <div class="cell-sub">{{ formatDateTime(row.replyTime) }}</div>
            </div>
            <el-tag v-else size="small" type="warning" effect="plain">待回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openReply(row)">
              {{ row.reply ? '改回复' : '回复' }}
            </el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '隐藏' : '展示' }}
            </el-button>
            <el-button link type="danger" size="small" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的评价" :image-size="90" />
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

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="520px" destroy-on-close>
      <div v-if="current" class="reply-quote">
        <div class="quote-head">
          <span>{{ current.customerName || current.username }}</span>
          <el-rate :model-value="current.rating" disabled size="small" />
        </div>
        <div class="quote-body">{{ current.content }}</div>
        <div class="cell-sub">{{ current.vehicleName }} · {{ current.orderNo }}</div>
      </div>

      <el-input
        v-model="replyText"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="如：感谢反馈，内饰问题已安排整备，下次给您更好的体验。"
      />

      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteEvaluation,
  getEvaluationStats,
  getEvaluations,
  replyEvaluation,
  updateEvaluationStatus
} from '@/api/adminPlatform'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})

const query = reactive({
  rating: null,
  status: null,
  unreplied: false,
  keyword: '',
  page: 1,
  size: 10
})

const replyVisible = ref(false)
const replying = ref(false)
const current = ref(null)
const replyText = ref('')

const replyRate = computed(() => {
  const t = stats.value.total || 0
  if (!t) return '0.0'
  return (((stats.value.replied || 0) / t) * 100).toFixed(1)
})

async function load() {
  loading.value = true
  try {
    const res = await getEvaluations(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getEvaluationStats()
  stats.value = res.data || {}
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.rating = null
  query.status = null
  query.unreplied = false
  query.keyword = ''
  query.page = 1
  load()
}

function reload() {
  load()
  loadStats()
}

function openReply(row) {
  current.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyText.value.trim()) {
    ElMessage.warning('请填写回复内容')
    return
  }
  replying.value = true
  try {
    await replyEvaluation(current.value.id, { reply: replyText.value.trim() })
    ElMessage.success('回复成功')
    replyVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e?.message || '回复失败')
  } finally {
    replying.value = false
  }
}

async function toggleStatus(row) {
  const hide = row.status === 1
  try {
    await ElMessageBox.confirm(
      hide
        ? '隐藏后该评价不再在前台展示，但原文与图片都会保留，随时可以恢复展示。'
        : '恢复后该评价会重新出现在前台车辆详情页。',
      hide ? '隐藏评价' : '恢复展示',
      { confirmButtonText: hide ? '确认隐藏' : '确认恢复', cancelButtonText: '取消', type: 'warning' }
    )
    await updateEvaluationStatus(row.id, { status: hide ? 0 : 1 })
    ElMessage.success(hide ? '评价已隐藏' : '评价已恢复展示')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

/**
 * 删除违规评价
 *
 * 和「隐藏」分开做，是因为两者的心智完全不同：隐藏是收起来，删除是处置。
 * 删除前要填原因，写进操作日志，事后能回答"这条为什么没了"。
 */
async function removeRow(row) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt(
      `确认删除「${row.customerName || row.username || '该用户'}」对 ${row.vehicleName || '该车辆'} 的评价？\n` +
        '删除后前台与列表都不再显示。仅限辱骂、广告、泄露隐私等违规内容，\n' +
        '普通差评请用「隐藏」，删了就没法复盘了。',
      '删除评价',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '删除原因，如：含广告链接',
        inputValidator: (v) => (v && v.trim() ? true : '请填写删除原因')
      }
    )
    reason = res.value.trim()
  } catch (e) {
    return
  }
  try {
    await deleteEvaluation(row.id, { reason })
    ElMessage.success('评价已删除')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
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
  /* 8 张卡：总数 / 综合 / 车况 / 服务 一行，好评与回复相关一行，4×2 比 6+2 整齐 */
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
  font-size: 21px;
  font-weight: 600;
  color: #1f2937;
}

.stat-value i {
  font-style: normal;
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.stat-value.success { color: #67c23a; }
.stat-value.warn { color: #e6a23c; }
.stat-value.danger { color: #f56c6c; }

.score-value {
  color: #e6a23c;
}

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

.content {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.thumbs {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.thumb {
  width: 44px;
  height: 33px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  cursor: zoom-in;
}

.reply {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  background: #f4f8ff;
  border-radius: 8px;
  padding: 6px 9px;
  border-left: 2px solid #409eff;
}

.reply-quote {
  background: #fafbfc;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
}

.quote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.quote-body {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 6px;
}

@media (max-width: 1400px) {
  .stat-row {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
