<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>客户评价</h2>
        <p class="sub">
          这里只显示<strong>本网点</strong>的评价。您作为商家只有<strong>解释权</strong>：
          客人说车旧、说等待久，可以在下面回复说明已怎么改进，但删除要由平台判定违规后处理。
          自己删差评，评分就不再有人信了。
        </p>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">评价总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">综合均分</div>
        <div class="stat-value score">{{ stats.avgRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">车况均分</div>
        <div class="stat-value score">{{ stats.avgVehicleRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">服务均分</div>
        <div class="stat-value score">{{ stats.avgStoreRating }}<i>/5</i></div>
      </div>
      <div class="stat">
        <div class="stat-label">待回复</div>
        <div class="stat-value warn">{{ stats.unreplied }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select v-model="query.rating" placeholder="全部评分" clearable style="width: 120px" @change="onSearch">
          <el-option v-for="r in [5, 4, 3, 2, 1]" :key="r" :label="`${r} 星`" :value="r" />
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
        <el-table-column label="客户 / 订单" width="176">
          <template #default="{ row }">
            <div class="cell-main">{{ row.customerName || row.username || '—' }}</div>
            <div class="cell-sub mono">{{ row.orderNo }}</div>
          </template>
        </el-table-column>

        <el-table-column label="车辆" min-width="150">
          <template #default="{ row }">
            <div class="cell-main">{{ row.vehicleName }}</div>
            <div class="cell-sub">{{ row.licensePlate }}</div>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate :model-value="row.rating" disabled size="small" />
            <div class="cell-sub">车况 {{ row.vehicleRating }}.0 · 服务 {{ row.storeRating }}.0</div>
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

        <el-table-column label="我的回复" min-width="200">
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

        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openReply(row)">
              {{ row.reply ? '改回复' : '回复' }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="本网点暂无评价" :image-size="90" />
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

    <el-dialog v-model="replyVisible" title="回复客户评价" width="520px" destroy-on-close>
      <div v-if="current" class="quote">
        <div class="quote-head">
          <span>{{ current.customerName || current.username }}</span>
          <el-rate :model-value="current.rating" disabled size="small" />
        </div>
        <div class="quote-dims">
          车况 {{ current.vehicleRating }}.0 分 · 服务 {{ current.storeRating }}.0 分
        </div>
        <div class="quote-body">{{ current.content }}</div>
        <div class="cell-sub">{{ current.vehicleName }} · {{ current.orderNo }}</div>
      </div>

      <el-input
        v-model="replyText"
        type="textarea"
        :rows="4"
        maxlength="200"
        show-word-limit
        placeholder="如：取车等待的问题我们已增加高峰时段人手，给您带来不便非常抱歉。"
      />

      <p class="reply-tip">
        回复会显示在车辆详情页，所有访客都能看到。写清楚"改了什么"，比只说"抱歉"更有用。
      </p>

      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getStoreEvaluationStats, getStoreEvaluations, replyStoreEvaluation } from '@/api/store'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})

const query = reactive({
  rating: null,
  unreplied: false,
  keyword: '',
  page: 1,
  size: 10
})

const replyVisible = ref(false)
const replying = ref(false)
const current = ref(null)
const replyText = ref('')

async function load() {
  loading.value = true
  try {
    const res = await getStoreEvaluations(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getStoreEvaluationStats()
  stats.value = res.data || {}
}

function reload() {
  load()
  loadStats()
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.rating = null
  query.unreplied = false
  query.keyword = ''
  query.page = 1
  load()
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
    await replyStoreEvaluation(current.value.id, { reply: replyText.value.trim() })
    ElMessage.success('回复成功')
    replyVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    ElMessage.error(e?.message || '回复失败')
  } finally {
    replying.value = false
  }
}

onMounted(reload)
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
  max-width: 820px;
  font-size: 13px;
  line-height: 1.7;
  color: #6b7280;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.stat {
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.stat-value i {
  font-size: 13px;
  font-style: normal;
  color: #a8abb2;
}

.stat-value.score {
  color: #e6a23c;
}

.stat-value.warn {
  color: #f56c6c;
}

.card {
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.count-hint {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.cell-main {
  font-size: 13px;
  color: #303133;
}

.cell-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.mono {
  font-family: Consolas, Monaco, monospace;
}

.content {
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
}

.thumbs {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.thumb {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.reply {
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.quote {
  margin-bottom: 14px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quote-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #303133;
}

.quote-dims {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.quote-body {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #303133;
}

.reply-tip {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #a8abb2;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
