<template>
  <div class="dashboard" v-loading="loading">
    <!-- ==================== 页头 ==================== -->
    <section class="page-head">
      <div class="head-left">
        <h2>运营分析</h2>
        <p class="sub">
          数据来自订单、支付、退款与信用记录实时聚合
          <span v-if="data.generatedAt" class="ts">· 更新于 {{ fmtTime(data.generatedAt) }}</span>
        </p>
      </div>
      <div class="head-right">
        <el-radio-group v-model="days" size="default" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
      </div>
    </section>

    <!-- ==================== 指标卡 ==================== -->
    <section class="kpi-grid">
      <div v-for="card in kpiCards" :key="card.label" class="kpi" :class="card.tone">
        <div class="kpi-top">
          <span class="kpi-label">{{ card.label }}</span>
          <el-tooltip :content="card.tip" placement="top">
            <el-icon class="kpi-help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="kpi-value" :class="{ small: card.small }">{{ card.value }}</div>
        <div class="kpi-foot">{{ card.foot }}</div>
      </div>
    </section>

    <!-- ==================== 营收趋势 ==================== -->
    <section class="card">
      <div class="card-head">
        <div>
          <h3>营收趋势</h3>
          <p class="card-desc">柱为当日租金实收，线为当日支付订单数；押金不计入营收</p>
        </div>
        <div class="legend-note">
          <span><i class="dot" style="background: #409eff"></i>租金实收</span>
          <span><i class="dot" style="background: #e6a23c"></i>支付订单数</span>
        </div>
      </div>
      <ChartBox :option="trendOption" height="320px" />
    </section>

    <!-- ==================== 漏斗 + 车辆状态 ==================== -->
    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <div>
            <h3>订单履约漏斗</h3>
            <p class="card-desc">每级取自订单自身时间戳，取消订单已发生的动作不丢台阶</p>
          </div>
        </div>
        <div class="funnel">
          <div v-for="(step, i) in data.funnel" :key="step.stage" class="funnel-row">
            <div class="funnel-meta">
              <span class="funnel-stage">{{ step.stage }}</span>
              <span class="funnel-count">{{ step.count }}<em>单</em></span>
            </div>
            <div class="funnel-bar-wrap">
              <div
                class="funnel-bar"
                :style="{ width: barWidth(step, i) + '%', background: funnelColor(i) }"
              ></div>
              <span class="funnel-rate">{{ fmtRate(step.overallRate) }}</span>
            </div>
            <div class="funnel-drop">
              <template v-if="i > 0 && step.dropRate">
                <el-icon><Bottom /></el-icon> 流失 {{ fmtRate(step.dropRate) }}
              </template>
              <template v-else-if="i === 0">占下单量</template>
              <template v-else>无流失</template>
            </div>
          </div>
        </div>
        <div class="funnel-foot">
          整体转化率 <b>{{ fmtRate(lastOverall) }}</b>　·　
          取消 {{ data.kpi.cancelledOrders ?? 0 }} 单
        </div>
      </section>

      <section class="card">
        <div class="card-head">
          <div>
            <h3>车辆状态</h3>
            <p class="card-desc">在册 {{ data.kpi.vehicleTotal ?? 0 }} 台，出租率 {{ fmtRate(data.kpi.rentRate) }}</p>
          </div>
        </div>
        <ChartBox :option="vehicleOption" height="280px" />
      </section>
    </div>

    <!-- ==================== 网点 + 车型 ==================== -->
    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <div>
            <h3>网点经营</h3>
            <p class="card-desc">单车产出 = 租金实收 ÷ 在册车辆数</p>
          </div>
        </div>
        <el-table :data="data.stores" size="small" class="store-table">
          <el-table-column prop="storeName" label="网点" min-width="120" />
          <el-table-column prop="vehicleCount" label="车辆" width="64" align="center" />
          <el-table-column prop="orderCount" label="订单" width="64" align="center" />
          <el-table-column label="租金实收" width="106" align="right">
            <template #default="{ row }">{{ money(row.rentAmount) }}</template>
          </el-table-column>
          <el-table-column label="单车产出" width="100" align="right">
            <template #default="{ row }">{{ money(row.perVehicle) }}</template>
          </el-table-column>
          <el-table-column label="完成率" width="86" align="center">
            <template #default="{ row }">
              <span :class="rateClass(row.completeRate)">{{ fmtRate(row.completeRate) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="逾期" width="64" align="center">
            <template #default="{ row }">
              <span :class="{ danger: row.overdueCount > 0 }">{{ row.overdueCount }}</span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="card">
        <div class="card-head">
          <div>
            <h3>车型营收 TOP {{ data.models?.length || 0 }}</h3>
            <p class="card-desc">仅统计已支付订单</p>
          </div>
        </div>
        <ChartBox :option="modelOption" height="300px" />
      </section>
    </div>

    <!-- ==================== 信用分布 ==================== -->
    <section class="card">
      <div class="card-head">
        <div>
          <h3>信用等级与履约表现</h3>
          <p class="card-desc">
            左：各等级人数占比；右：各等级逾期率 —— 用数据回答"信用分到底有没有区分度"
          </p>
        </div>
        <el-tag size="small" effect="plain" type="info">逾期率 = 逾期单数 ÷ 该等级订单数</el-tag>
      </div>
      <div class="grid-2 inner">
        <ChartBox :option="creditPieOption" height="260px" />
        <ChartBox :option="creditBarOption" height="260px" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bottom, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import ChartBox from '@/components/ChartBox.vue'
import { getDashboardOverview } from '@/api/dashboard'

const loading = ref(false)
const days = ref(30)
const data = ref({
  kpi: {},
  trend: [],
  funnel: [],
  vehicleStatus: [],
  stores: [],
  models: [],
  creditLevels: []
})

/* ==================== 格式化 ==================== */

const nf = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + nf.format(Number(v))
}

function fmtRate(v) {
  if (v === null || v === undefined) return '—'
  return Number(v).toFixed(1) + '%'
}

function fmtTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const p = (x) => String(x).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function rateClass(v) {
  if (v === null || v === undefined) return ''
  if (v >= 75) return 'good'
  if (v >= 60) return 'warn'
  return 'danger'
}

/* ==================== 指标卡 ==================== */

const kpiCards = computed(() => {
  const k = data.value.kpi || {}
  return [
    {
      label: '累计净营收',
      value: money(k.revenue),
      small: true,
      foot: `近 ${data.value.days || days.value} 天 ${money(k.rangeRevenue)}`,
      tone: 'tone-blue',
      tip: '租金实收 − 租金退款 + 违约金。押金是代收代退，不计入营收。'
    },
    {
      label: '平均客单价',
      value: money(k.avgOrderAmount),
      foot: '累计口径，不含押金',
      tone: 'tone-green',
      tip: '（累计租金实收 − 租金退款）÷ 累计已支付订单数'
    },
    {
      label: '订单总量',
      value: k.orderTotal ?? 0,
      foot: `已支付 ${k.paidOrders ?? 0} · 已取消 ${k.cancelledOrders ?? 0}`,
      tone: 'tone-purple',
      tip: `所选时间范围内新建的订单数`
    },
    {
      label: '订单完成率',
      value: fmtRate(k.completeRate),
      foot: `已完成 ${k.completedOrders ?? 0} 单`,
      tone: 'tone-cyan',
      tip: '已完成订单 ÷ 所选范围内订单总数'
    },
    {
      label: '车辆出租率',
      value: fmtRate(k.rentRate),
      foot: `${k.vehicleRented ?? 0} / ${k.vehicleTotal ?? 0} 台在租`,
      tone: 'tone-orange',
      tip: '当前已租出车辆 ÷ 在册车辆，是瞬时快照，不是时间加权利用率'
    },
    {
      label: '逾期订单',
      value: k.overdueOrders ?? 0,
      foot: `累计违约金 ${money(k.lateFeeTotal)}`,
      tone: 'tone-red',
      tip: '所选范围内曾经逾期的订单数（含已归还的），不等于当前仍处于逾期状态的单量'
    }
  ]
})

/* ==================== 漏斗 ==================== */

const FUNNEL_COLORS = ['#3f7ff5', '#5b9bf8', '#7db6fa', '#a6cdfc', '#cfe4fe']

function funnelColor(i) {
  return FUNNEL_COLORS[i] || FUNNEL_COLORS[FUNNEL_COLORS.length - 1]
}

function barWidth(step, i) {
  const first = data.value.funnel?.[0]?.count || 0
  if (!first) return 0
  const base = i === 0 ? 100 : Math.max((step.count / first) * 100, 8)
  return base
}

const lastOverall = computed(() => {
  const list = data.value.funnel || []
  return list.length ? list[list.length - 1].overallRate : null
})

/* ==================== 图表 option ==================== */

const trendOption = computed(() => {
  const list = data.value.trend || []
  return {
    grid: { left: 8, right: 8, top: 32, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const date = params[0]?.axisValue || ''
        const rows = params
          .map((p) => {
            const unit = p.seriesName === '支付订单数' ? ' 单' : ''
            const val = p.seriesName === '支付订单数' ? p.value : money(p.value)
            return `${p.marker}${p.seriesName}：<b>${val}${unit}</b>`
          })
          .join('<br/>')
        return `${date}<br/>${rows}`
      }
    },
    xAxis: {
      type: 'category',
      data: list.map((x) => String(x.date).slice(5)),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 11, hideOverlap: true },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '租金',
        nameTextStyle: { color: '#a8abb2', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11, formatter: (v) => (v >= 1000 ? v / 1000 + 'k' : v) },
        splitLine: { lineStyle: { color: '#f2f3f5' } }
      },
      {
        type: 'value',
        name: '单',
        nameTextStyle: { color: '#a8abb2', fontSize: 11 },
        axisLabel: { color: '#909399', fontSize: 11 },
        splitLine: { show: false },
        // 留 15% 余量，否则高峰日的折线会顶到画布上沿，看着像被截断
        max: (value) => Math.max(Math.ceil((value.max || 0) * 1.15), 5)
      }
    ],
    series: [
      {
        name: '租金实收',
        type: 'bar',
        barMaxWidth: 22,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#66b1ff' },
              { offset: 1, color: '#a0cfff' }
            ]
          }
        },
        data: list.map((x) => Number(x.rentAmount || 0))
      },
      {
        name: '支付订单数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, color: '#e6a23c' },
        itemStyle: { color: '#e6a23c' },
        data: list.map((x) => Number(x.orderCount || 0))
      }
    ]
  }
})

const vehicleOption = computed(() => {
  const list = data.value.vehicleStatus || []
  const colorMap = { 可租: '#67c23a', 已租出: '#409eff', 维保中: '#e6a23c' }
  const total = list.reduce((sum, x) => sum + Number(x.value || 0), 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}：<b>{c} 台</b>（{d}%）' },
    title: {
      text: String(total),
      subtext: '台在册',
      left: 'center',
      top: '38%',
      textAlign: 'center',
      textStyle: { fontSize: 22, fontWeight: 700, color: '#1f2937' },
      subtextStyle: { fontSize: 11, color: '#a8abb2' }
    },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12, color: '#606266' } },
    series: [
      {
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{c}', fontSize: 13, fontWeight: 600, color: '#303133' },
        labelLine: { length: 8, length2: 8 },
        data: list.map((x) => ({
          name: x.name,
          value: x.value,
          itemStyle: { color: colorMap[x.name] || '#909399' }
        }))
      }
    ]
  }
})

const modelOption = computed(() => {
  const list = [...(data.value.models || [])].reverse()
  return {
    grid: { left: 8, right: 64, top: 8, bottom: 8, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const i = params[0].dataIndex
        const item = list[i] || {}
        return `${item.brandName} ${item.modelName}<br/>租金实收：<b>${money(item.rentAmount)}</b><br/>订单 ${item.orderCount} 单 · 平均 ${item.avgRentDays} 天`
      }
    },
    xAxis: { type: 'value', show: false },
    yAxis: {
      type: 'category',
      data: list.map((x) => x.modelName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 11, width: 96, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 14,
        itemStyle: {
          borderRadius: [0, 3, 3, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#7db6fa' },
              { offset: 1, color: '#3f7ff5' }
            ]
          }
        },
        label: {
          show: true,
          position: 'right',
          fontSize: 11,
          color: '#606266',
          formatter: (p) => {
            const v = Number(p.value || 0)
            return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v
          }
        },
        data: list.map((x) => Number(x.rentAmount || 0))
      }
    ]
  }
})

const creditPieOption = computed(() => {
  const list = data.value.creditLevels || []
  return {
    title: [
      {
        text: '人数占比',
        left: 'center',
        top: 4,
        textStyle: { fontSize: 12, fontWeight: 400, color: '#909399' }
      },
      {
        // 环形中心的人数合计，让"分布"有个总量参照
        text: `${list.reduce((s, x) => s + Number(x.userCount || 0), 0)} 人`,
        left: 'center',
        top: '42%',
        textAlign: 'center',
        textStyle: { fontSize: 18, fontWeight: 700, color: '#1f2937' }
      }
    ],
    tooltip: { trigger: 'item', formatter: '{b}：<b>{c} 人</b>（{d}%）' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12, color: '#606266' } },
    series: [
      {
        type: 'pie',
        radius: ['44%', '66%'],
        center: ['50%', '50%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{c}', fontSize: 12, fontWeight: 600, color: '#303133' },
        labelLine: { length: 6, length2: 6 },
        data: list.map((x) => ({
          name: x.levelLabel,
          value: x.userCount,
          itemStyle: { color: x.color }
        }))
      }
    ]
  }
})

const creditBarOption = computed(() => {
  const list = data.value.creditLevels || []
  return {
    title: {
      text: '逾期率',
      left: 'center',
      top: 4,
      textStyle: { fontSize: 12, fontWeight: 400, color: '#909399' }
    },
    grid: { left: 8, right: 24, top: 40, bottom: 16, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const item = list[params[0].dataIndex] || {}
        return `${item.levelLabel}<br/>逾期率：<b>${fmtRate(item.overdueRate)}</b><br/>${item.overdueCount} / ${item.orderCount} 单<br/>租金贡献：${money(item.rentAmount)}`
      }
    },
    xAxis: {
      type: 'category',
      data: list.map((x) => x.levelLabel),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      name: '%',
      nameTextStyle: { color: '#a8abb2', fontSize: 11 },
      axisLabel: { color: '#909399', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5' } }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 40,
        // 逾期率为 0 的等级给 3px 底座，否则柱形完全消失，会被误认为没数据
        barMinHeight: 3,
        itemStyle: { borderRadius: [4, 4, 0, 0] },
        label: {
          show: true,
          position: 'top',
          fontSize: 12,
          fontWeight: 600,
          color: '#606266',
          formatter: (p) => fmtRate(p.value)
        },
        data: list.map((x) => ({
          value: Number(x.overdueRate || 0),
          // 柱色跟等级色走，和左边饼图一眼对得上
          itemStyle: { color: x.color }
        }))
      }
    ]
  }
})

/* ==================== 加载 ==================== */

async function load() {
  loading.value = true
  try {
    const res = await getDashboardOverview(days.value)
    data.value = res.data || data.value
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.page-head h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}
.sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
.ts {
  color: #b1b3b8;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 10px;
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

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
}
.kpi-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.kpi-help {
  font-size: 12px;
  color: #c8c9cc;
  cursor: help;
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
  font-size: 20px;
}
.kpi-foot {
  font-size: 11px;
  color: #b1b3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 通用卡片 ---------- */
.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px 18px 12px;
}
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.card-head h3 {
  margin: 0 0 2px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}
.card-desc {
  margin: 0;
  font-size: 12px;
  color: #a8abb2;
}
.legend-note {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  margin-right: 5px;
  vertical-align: middle;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.grid-2.inner {
  gap: 8px;
}

/* ---------- 漏斗 ---------- */
.funnel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 6px 0 4px;
}
.funnel-row {
  display: grid;
  grid-template-columns: 92px 1fr 96px;
  align-items: center;
  gap: 10px;
}
.funnel-meta {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.funnel-stage {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
.funnel-count {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
}
.funnel-count em {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: #b1b3b8;
  margin-left: 1px;
}
.funnel-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f7f8fa;
  border-radius: 4px;
  height: 26px;
  padding-right: 8px;
  overflow: hidden;
}
.funnel-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
}
.funnel-rate {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  font-variant-numeric: tabular-nums;
}
.funnel-drop {
  font-size: 11px;
  color: #b1b3b8;
  display: flex;
  align-items: center;
  gap: 2px;
}
.funnel-drop .el-icon {
  font-size: 11px;
  color: #f56c6c;
}
.funnel-foot {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
}
.funnel-foot b {
  color: #3f7ff5;
  font-size: 14px;
}

/* ---------- 网点表 ---------- */
.store-table {
  margin-top: 4px;
}
.store-table :deep(.el-table__header th) {
  background: #fafbfc;
  color: #909399;
  font-weight: 500;
  font-size: 12px;
}
.store-table :deep(.el-table__cell) {
  padding: 7px 0;
  font-size: 12px;
}
.good { color: #67c23a; font-weight: 600; }
.warn { color: #e6a23c; font-weight: 600; }
.danger { color: #f56c6c; font-weight: 600; }

/* ---------- 响应式 ---------- */
@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1100px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .funnel-row {
    grid-template-columns: 80px 1fr;
  }
  .funnel-drop {
    display: none;
  }
}
</style>
