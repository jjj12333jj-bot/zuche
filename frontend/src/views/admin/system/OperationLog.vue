<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>操作日志</h2>
        <p class="sub">
          后台每一个写操作（新增、编辑、删除、发布、冻结、调分、关单…）都会在这里留一条痕迹：
          <strong>谁、在哪个模块、做了什么、带了什么参数、耗时多久、成没成</strong>。
          记录由 AOP 切面统一完成，业务代码里没有一行写日志的代码，新增接口只需加一个注解。
          日志<strong>只读</strong>——能事后编辑的审计记录不算审计。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">日志总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">今日操作</div>
        <div class="stat-value">{{ stats.todayCount }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">失败操作</div>
        <div class="stat-value danger">{{ stats.failCount }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">平均耗时</div>
        <div class="stat-value">{{ stats.avgCost }} <span class="unit">ms</span></div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="操作人 / 操作描述 / 请求地址"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.module" placeholder="全部模块" clearable style="width: 140px" @change="onSearch">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部结果" clearable style="width: 120px" @change="onSearch">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 250px"
          @change="onDateChange"
        />
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 条</span>
      </div>

      <el-table
        :data="rows"
        v-loading="loading"
        size="small"
        border
        :row-class-name="rowClass"
      >
        <el-table-column label="操作时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="110">
          <template #default="{ row }">
            <div class="cell-main">{{ row.username || '—' }}</div>
            <div class="cell-sub">ID {{ row.userId ?? '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="模块" width="112">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <span class="cell-main">{{ row.operation }}</span>
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="230">
          <template #default="{ row }">
            <div class="req-cell">
              <span class="method" :class="row.method?.toLowerCase()">{{ row.method }}</span>
              <span class="uri mono">{{ row.requestUri }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="IP" width="118">
          <template #default="{ row }">
            <span class="mono cell-sub">{{ row.ip || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="86" align="right">
          <template #default="{ row }">
            <span :class="{ slow: row.costMs > 500 }">{{ row.costMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="76" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有符合条件的日志" :image-size="90" />
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

    <el-dialog v-model="detailVisible" title="操作详情" width="720px">
      <div v-if="detail" class="detail">
        <div class="detail-grid">
          <div class="item"><span>操作时间</span><b>{{ formatDateTime(detail.createTime) }}</b></div>
          <div class="item"><span>操作人</span><b>{{ detail.username || '—' }}（ID {{ detail.userId ?? '—' }}）</b></div>
          <div class="item"><span>业务模块</span><b>{{ detail.module }}</b></div>
          <div class="item"><span>操作描述</span><b>{{ detail.operation }}</b></div>
          <div class="item"><span>请求方法</span><b class="mono">{{ detail.method }}</b></div>
          <div class="item"><span>客户端 IP</span><b class="mono">{{ detail.ip || '—' }}</b></div>
          <div class="item wide"><span>请求地址</span><b class="mono">{{ detail.requestUri }}</b></div>
          <div class="item"><span>执行耗时</span><b>{{ detail.costMs }} ms</b></div>
          <div class="item">
            <span>执行结果</span>
            <b>
              <el-tag size="small" :type="detail.status === 1 ? 'success' : 'danger'" effect="light">
                {{ detail.statusLabel }}
              </el-tag>
            </b>
          </div>
        </div>
        <div class="params-title">请求参数（已脱敏、超长截断）</div>
        <pre v-if="detail.params" class="params">{{ prettyParams }}</pre>
        <div v-else class="params empty">该操作没有请求参数</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getLogDetail, getLogModules, getLogStats, getLogs } from '@/api/systemAdmin'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const modules = ref([])
const dateRange = ref([])

const query = reactive({
  keyword: '',
  module: null,
  status: null,
  startDate: null,
  endDate: null,
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const detail = ref(null)

const prettyParams = computed(() => {
  const raw = detail.value?.params
  if (!raw) return ''
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch (e) {
    return raw
  }
})

function rowClass({ row }) {
  return row.status === 0 ? 'row-fail' : ''
}

async function load() {
  loading.value = true
  try {
    const res = await getLogs(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getLogStats()
  stats.value = res.data || {}
}

async function loadModules() {
  const res = await getLogModules()
  modules.value = res.data || []
}

function onSearch() {
  query.page = 1
  load()
}

function onDateChange(value) {
  query.startDate = value?.[0] || null
  query.endDate = value?.[1] || null
  onSearch()
}

function reset() {
  query.keyword = ''
  query.module = null
  query.status = null
  query.startDate = null
  query.endDate = null
  query.page = 1
  dateRange.value = []
  load()
}

function reload() {
  load()
  loadStats()
  loadModules()
}

async function openDetail(row) {
  const res = await getLogDetail(row.id)
  detail.value = res.data
  detailVisible.value = true
}

onMounted(() => {
  load()
  loadStats()
  loadModules()
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
  max-width: 960px;
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

.stat-value.danger { color: #f56c6c; }

.unit {
  font-size: 12px;
  font-weight: 400;
  color: #a8abb2;
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

.req-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
}

.method {
  flex: none;
  font-size: 10px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 4px;
  background: #ecf5ff;
  color: #409eff;
}

.method.post { background: #f0f9eb; color: #67c23a; }
.method.put { background: #fdf6ec; color: #e6a23c; }
.method.delete { background: #fef0f0; color: #f56c6c; }

.uri {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slow {
  color: #e6a23c;
}

:deep(.row-fail) {
  background: #fff8f8;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 24px;
}

.detail-grid .item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.detail-grid .item.wide {
  grid-column: span 2;
}

.detail-grid .item > span {
  flex: none;
  width: 72px;
  color: #a8abb2;
  font-size: 12px;
}

.detail-grid .item > b {
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

.params-title {
  margin: 18px 0 8px;
  font-size: 12px;
  color: #a8abb2;
}

.params {
  margin: 0;
  background: #f7f8fa;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: #4b5563;
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.params.empty {
  color: #c0c4cc;
}

@media (max-width: 1280px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
