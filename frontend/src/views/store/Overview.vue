<template>
  <div class="page" v-loading="loading">
    <!-- ==================== 页头 ==================== -->
    <div class="page-head">
      <div>
        <h2>{{ data.storeName || '网点概览' }}</h2>
        <p class="sub">
          {{ data.address || '地址未配置' }}
          <span v-if="data.phone"> · {{ data.phone }}</span>
          <span v-if="data.businessHours"> · {{ data.businessHours }}</span>
        </p>
      </div>
      <el-button :icon="Refresh" circle :loading="loading" @click="load" />
    </div>

    <!-- ==================== 待办提醒 ==================== -->
    <el-alert v-if="todoCount > 0" type="warning" :closable="false" show-icon class="todo">
      <template #title>
        有 <b>{{ todoCount }}</b> 单待处理 —— 待交付 {{ data.pendingPickup || 0 }} 单 ·
        待归还 {{ data.pendingReturn || 0 }} 单 · 逾期 {{ data.overdue || 0 }} 单
        <el-button link type="primary" class="todo-link" @click="goOrders">去处理 →</el-button>
      </template>
    </el-alert>
    <el-alert v-else type="success" :closable="false" show-icon
              title="当前没有待处理的取车/归还，状态清爽" />

    <!-- ==================== 指标卡 ==================== -->
    <div class="kpi-grid">
      <div v-for="card in cards" :key="card.label" class="kpi" :class="card.tone">
        <div class="kpi-label">{{ card.label }}</div>
        <div class="kpi-value" :class="{ small: card.small }">{{ card.value }}</div>
        <div class="kpi-foot">{{ card.foot }}</div>
      </div>
    </div>

    <!-- ==================== 资产与经营 ==================== -->
    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <h3>车辆资产</h3>
          <span class="card-desc">在册 {{ data.vehicleTotal || 0 }} 台</span>
        </div>

        <div class="bar">
          <div class="bar-seg seg-available" :style="{ width: pct(data.vehicleAvailable) + '%' }"></div>
          <div class="bar-seg seg-rented" :style="{ width: pct(data.vehicleRented) + '%' }"></div>
          <div class="bar-seg seg-maintenance" :style="{ width: pct(data.vehicleMaintenance) + '%' }"></div>
        </div>

        <div class="legend">
          <div class="legend-item">
            <i class="dot dot-available"></i>
            <span class="legend-label">可租</span>
            <span class="legend-value">{{ data.vehicleAvailable || 0 }}</span>
            <span class="legend-pct">{{ fmtRate(pct(data.vehicleAvailable)) }}</span>
          </div>
          <div class="legend-item">
            <i class="dot dot-rented"></i>
            <span class="legend-label">已租出</span>
            <span class="legend-value">{{ data.vehicleRented || 0 }}</span>
            <span class="legend-pct">{{ fmtRate(pct(data.vehicleRented)) }}</span>
          </div>
          <div class="legend-item">
            <i class="dot dot-maintenance"></i>
            <span class="legend-label">维保中</span>
            <span class="legend-value">{{ data.vehicleMaintenance || 0 }}</span>
            <span class="legend-pct">{{ fmtRate(pct(data.vehicleMaintenance)) }}</span>
          </div>
        </div>

        <p class="hint">
          出租率 {{ fmtRate(data.rentRate) }}（已租出 ÷ 在册），是当前瞬时快照，不是时间加权利用率。
        </p>
      </section>

      <section class="card">
        <div class="card-head">
          <h3>今日与本月</h3>
          <span class="card-desc">营收只算租金实收，押金代收代退</span>
        </div>

        <div class="today-grid">
          <div class="today-item">
            <div class="today-label">今日新单</div>
            <div class="today-value">{{ data.todayNewOrders || 0 }}<em>单</em></div>
          </div>
          <div class="today-item">
            <div class="today-label">今日完成归还</div>
            <div class="today-value">{{ data.todayCompleted || 0 }}<em>单</em></div>
          </div>
          <div class="today-item">
            <div class="today-label">本月订单</div>
            <div class="today-value">{{ data.monthOrders || 0 }}<em>单</em></div>
          </div>
          <div class="today-item">
            <div class="today-label">本月租金实收</div>
            <div class="today-value money">{{ money(data.monthRevenue) }}</div>
          </div>
        </div>

        <p class="hint">
          在管押金 {{ money(data.depositInHand) }} —— 未结束订单的押金合计，归还时扣除违约金后退还。
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { getStoreOverview } from '@/api/store'

const router = useRouter()
const loading = ref(false)
const data = ref({})

const nf = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + nf.format(Number(v))
}

function fmtRate(v) {
  if (v === null || v === undefined) return '—'
  return Number(v).toFixed(1) + '%'
}

/** 占比：分母为 0 时返回 0，避免 NaN 把条形撑坏 */
function pct(v) {
  const total = Number(data.value.vehicleTotal || 0)
  if (!total) return 0
  return (Number(v || 0) * 100) / total
}

const todoCount = computed(() => Number(data.value.todoCount || 0))

const cards = computed(() => {
  const d = data.value
  return [
    {
      label: '在册车辆',
      value: d.vehicleTotal ?? 0,
      foot: `可租 ${d.vehicleAvailable ?? 0} · 已租 ${d.vehicleRented ?? 0} · 维保 ${d.vehicleMaintenance ?? 0}`,
      tone: 'tone-blue'
    },
    {
      label: '车辆出租率',
      value: fmtRate(d.rentRate),
      foot: `${d.vehicleRented ?? 0} / ${d.vehicleTotal ?? 0} 台在租`,
      tone: 'tone-cyan'
    },
    {
      label: '在租订单',
      value: d.inUse ?? 0,
      foot: `待交付 ${d.pendingPickup ?? 0} 单`,
      tone: 'tone-green'
    },
    {
      label: '待办合计',
      value: d.todoCount ?? 0,
      foot: `待归还 ${d.pendingReturn ?? 0} · 逾期 ${d.overdue ?? 0}`,
      tone: 'tone-orange'
    },
    {
      label: '本月租金实收',
      value: money(d.monthRevenue),
      small: true,
      foot: `本月 ${d.monthOrders ?? 0} 单`,
      tone: 'tone-purple'
    },
    {
      label: '在管押金',
      value: money(d.depositInHand),
      small: true,
      foot: '代收代退，不计入营收',
      tone: 'tone-red'
    }
  ]
})

function goOrders() {
  router.push('/store/orders')
}

async function load() {
  loading.value = true
  try {
    const res = await getStoreOverview()
    data.value = res.data || {}
  } finally {
    loading.value = false
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
  gap: 16px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2937;
}

.sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.todo {
  border-radius: 8px;
}

.todo-link {
  margin-left: 8px;
  font-size: 13px;
}

/* ---------- 指标卡 ---------- */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.kpi {
  position: relative;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px 12px;
  overflow: hidden;
}

.kpi::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--tone, #409eff);
}

.kpi.tone-blue { --tone: #3f7ff5; }
.kpi.tone-green { --tone: #67c23a; }
.kpi.tone-purple { --tone: #8b5cf6; }
.kpi.tone-cyan { --tone: #06b6d4; }
.kpi.tone-orange { --tone: #e6a23c; }
.kpi.tone-red { --tone: #f56c6c; }

.kpi-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.kpi-value {
  margin: 8px 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.kpi-value.small {
  font-size: 19px;
}

.kpi-foot {
  font-size: 11px;
  color: #b1b3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 卡片 ---------- */
.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 18px 14px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.card-desc {
  font-size: 12px;
  color: #a8abb2;
}

/* ---------- 资产条 ---------- */
.bar {
  display: flex;
  height: 14px;
  border-radius: 7px;
  overflow: hidden;
  background: #f2f3f5;
}

.bar-seg {
  height: 100%;
  transition: width 0.4s ease;
}

.seg-available { background: #67c23a; }
.seg-rented { background: #3f7ff5; }
.seg-maintenance { background: #e6a23c; }

.legend {
  display: flex;
  gap: 22px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  display: inline-block;
}

.dot-available { background: #67c23a; }
.dot-rented { background: #3f7ff5; }
.dot-maintenance { background: #e6a23c; }

.legend-label { color: #909399; }
.legend-value { font-weight: 700; color: #1f2937; font-size: 13px; }
.legend-pct { color: #b1b3b8; }

/* ---------- 今日 ---------- */
.today-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.today-item {
  border-left: 2px solid #f2f3f5;
  padding-left: 10px;
}

.today-label {
  font-size: 12px;
  color: #909399;
}

.today-value {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
}

.today-value em {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: #b1b3b8;
  margin-left: 2px;
}

.today-value.money {
  font-size: 17px;
}

.hint {
  margin: 14px 0 0;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.6;
}

@media (max-width: 1400px) {
  .kpi-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 1100px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
