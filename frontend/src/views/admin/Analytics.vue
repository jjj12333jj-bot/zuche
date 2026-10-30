<template>
  <div class="analytics" v-loading="loading">
    <!-- ==================== 页头 ==================== -->
    <section class="page-head">
      <div class="head-left">
        <h2>智能分析</h2>
        <p class="sub">
          从历史下单与营收里找规律：什么时候忙、什么车赚钱、下周大概能做多少
          <span v-if="generatedAt" class="ts">· 更新于 {{ generatedAt }}</span>
        </p>
      </div>
      <div class="head-right">
        <el-radio-group v-model="days" size="default" @change="load">
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
          <el-radio-button :value="180">近 180 天</el-radio-button>
        </el-radio-group>
        <el-select v-model="horizon" size="default" style="width: 118px" @change="load">
          <el-option :value="7" label="预测 7 天" />
          <el-option :value="14" label="预测 14 天" />
        </el-select>
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
      </div>
    </section>

    <!-- ==================== 结论卡 ==================== -->
    <section class="kpi-grid">
      <div v-for="card in insightCards" :key="card.label" class="kpi" :class="card.tone">
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

    <!-- ==================== 下单时段热力图 ==================== -->
    <section class="card">
      <div class="card-head">
        <div>
          <h3>下单时段热力</h3>
          <p class="card-desc">
            星期 × 小时的下单分布。深色格子是用户真正下单的时间，用来排客服班次和挑推送时机
          </p>
        </div>
        <div class="legend-note">
          <span><i class="dot" style="background: #dbe9ff"></i>少</span>
          <span><i class="dot" style="background: #409eff"></i>多</span>
          <span class="peak">
            峰值 {{ weekLabel(heat.peakWeekday) }} {{ pad(heat.peakHour) }}:00 ·
            {{ heat.peakCount ?? 0 }} 单
          </span>
        </div>
      </div>
      <ChartBox :option="heatOption" height="320px" />
      <div class="card-foot">
        <span>窗口内共 <b>{{ heat.totalOrders ?? 0 }}</b> 单，租金 <b>{{ money(heat.totalRent) }}</b></span>
        <span class="sep">·</span>
        <span>按小时看最忙的是 <b>{{ pad(heat.busiestHour) }}:00</b>（{{ heat.hourTotals?.[heat.busiestHour] ?? 0 }} 单）</span>
        <span class="sep">·</span>
        <span>按星期看最忙的是 <b>{{ weekLabel(heat.busiestWeekday) }}</b>（{{ heat.weekdayTotals?.[heat.busiestWeekday - 1] ?? 0 }} 单）</span>
      </div>
    </section>

    <!-- ==================== 车型偏好 ==================== -->
    <div class="grid-2">
      <section class="card">
        <div class="card-head">
          <div>
            <h3>车型偏好矩阵</h3>
            <p class="card-desc">
              横轴订单量、纵轴客单价、气泡大小是营收。右下角是"走量"，左上角是"高客单"
            </p>
          </div>
        </div>
        <ChartBox :option="bubbleOption" height="330px" />
      </section>

      <section class="card">
        <div class="card-head">
          <div>
            <h3>分类经营明细</h3>
            <p class="card-desc">
              单车周产出 = 分类租金 ÷ 在册车辆数 ÷ 周数，用来判断该加车还是该减车
            </p>
          </div>
        </div>
        <el-table :data="models" size="small" class="detail-table">
          <el-table-column label="分类" min-width="86">
            <template #default="{ row }">
              <span class="cat">
                <i class="cat-dot" :style="{ background: row.color }"></i>{{ row.categoryLabel }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单" width="54" align="center" />
          <el-table-column label="客单价" width="82" align="right">
            <template #default="{ row }">{{ money(row.avgOrderAmount) }}</template>
          </el-table-column>
          <el-table-column label="营收占比" width="76" align="center">
            <template #default="{ row }">
              <span class="ratio">{{ fmtRate(row.rentRatio) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="在册" width="48" align="center">
            <template #default="{ row }">{{ row.vehicleCount }}</template>
          </el-table-column>
          <el-table-column label="单车周产出" width="92" align="right">
            <template #default="{ row }">
              <span :class="row.rentPerVehicleWeek >= 600 ? 'good' : ''">
                {{ money(row.rentPerVehicleWeek) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-note">
          订单量按订单去重（一单两车算一单），营收按明细累加（一单两车算两车钱）
        </div>
      </section>
    </div>

    <!-- ==================== 营收预测 ==================== -->
    <section class="card">
      <div class="card-head">
        <div>
          <h3>营收时序预测</h3>
          <p class="card-desc">
            柱为历史租金实收，虚线为未来 {{ forecast.horizon || horizon }} 天预测，浅蓝带是 80% 置信区间
          </p>
        </div>
        <el-tag size="small" effect="plain" :type="confType">
          可信度 {{ forecast.confidenceLabel || '—' }}
        </el-tag>
      </div>
      <ChartBox :option="forecastOption" height="340px" />

      <div class="metric-row">
        <div v-for="m in forecastMetrics" :key="m.label" class="metric">
          <span class="m-label">{{ m.label }}</span>
          <span class="m-value" :class="m.tone">{{ m.value }}</span>
          <span class="m-note">{{ m.note }}</span>
        </div>
      </div>

      <div class="method">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ forecast.method }}</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { InfoFilled, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import ChartBox from '@/components/ChartBox.vue'
import { getModelPreference, getOrderHeatmap, getRevenueForecast } from '@/api/analytics'

const WEEK = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const loading = ref(false)
const days = ref(90)
const horizon = ref(7)

const heat = ref({})
const models = ref([])
const forecast = ref({})
const generatedAt = ref('')

/* ==================== 格式化 ==================== */

const nf = new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const nf0 = new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 })

function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  return '¥' + nf.format(Number(v))
}

function moneyShort(v) {
  if (v === null || v === undefined) return '—'
  const n = Number(v)
  return n >= 10000 ? '¥' + (n / 10000).toFixed(2) + ' 万' : '¥' + nf0.format(n)
}

function fmtRate(v) {
  if (v === null || v === undefined) return '—'
  return Number(v).toFixed(1) + '%'
}

function signed(v) {
  if (v === null || v === undefined) return '—'
  const n = Number(v)
  return (n >= 0 ? '+' : '') + n.toFixed(1) + '%'
}

function pad(v) {
  if (v === null || v === undefined) return '--'
  return String(v).padStart(2, '0')
}

function weekLabel(w) {
  if (!w || w < 1 || w > 7) return '—'
  return WEEK[w - 1]
}

/* ==================== 结论卡 ==================== */

const topModel = computed(() => {
  const list = models.value || []
  if (!list.length) return {}
  return [...list].sort((a, b) => b.orderCount - a.orderCount)[0]
})

const confType = computed(() => {
  const c = forecast.value?.confidence
  if (c === 'HIGH') return 'success'
  if (c === 'MEDIUM') return 'warning'
  return 'danger'
})

const insightCards = computed(() => {
  const h = heat.value || {}
  const f = forecast.value || {}
  const tm = topModel.value || {}
  return [
    {
      label: '最忙时段',
      value: h.busiestHour === null || h.busiestHour === undefined
        ? '—'
        : `${pad(h.busiestHour)}:00`,
      foot: `${weekLabel(h.busiestWeekday)}最忙 · 该小时累计 ${h.hourTotals?.[h.busiestHour] ?? 0} 单`,
      tone: 'tone-blue',
      tip: '按全窗口各小时的下单量合计取最大值。下单时间比取车时间更能反映用户的决策时刻。'
    },
    {
      label: '最热分类',
      value: tm.categoryLabel || '—',
      small: true,
      foot: `订单占比 ${fmtRate(tm.orderRatio)} · 客单价 ${money(tm.avgOrderAmount)}`,
      tone: 'tone-green',
      tip: '按订单量排序，一单两车只算一单。'
    },
    {
      label: '未来营收预测',
      value: moneyShort(f.forecastTotal),
      foot: `${f.horizon || horizon.value} 天合计 · 日均 ${moneyShort(f.forecastDailyAvg)}`,
      tone: 'tone-purple',
      tip: '季节性朴素模型：近 4 周同一星期几的均值 × 趋势修正系数，不是拍脑袋给的增长假设。'
    },
    {
      label: '预测误差（周口径）',
      value: f.weeklyWape === null || f.weeklyWape === undefined ? '—' : fmtRate(f.weeklyWape),
      foot: `回测 ${f.backtestDays ?? 0} 天 · 日粒度 MAE ${moneyShort(f.mae)}`,
      tone: 'tone-orange',
      tip: 'WAPE = Σ|误差| ÷ Σ|实际|，比 MAPE 更抗小分母。数值越小越可信，25% 以内算可用。'
    }
  ]
})

/* ==================== 热力图 ==================== */

const heatOption = computed(() => {
  const cells = heat.value?.cells || []
  const lookup = new Map()
  cells.forEach((c) => lookup.set(`${c.weekday}-${c.hour}`, c))

  const max = Math.max(1, ...cells.map((c) => Number(c.orderCount || 0)))
  // 只放三维 [小时, 星期, 单量]：heatmap 的 visualMap 会按最后一个维度上色，
  // 多塞一个营收维度会让颜色映射跑到营收上，格子大小和颜色就对不上了
  const data = []
  const rentOf = new Map()
  for (let w = 1; w <= 7; w++) {
    for (let h = 0; h < 24; h++) {
      const c = lookup.get(`${w}-${h}`)
      const cnt = c ? Number(c.orderCount || 0) : 0
      data.push([h, w - 1, cnt])
      if (cnt) rentOf.set(`${h}-${w - 1}`, Number(c.rentAmount || 0))
    }
  }

  return {
    tooltip: {
      formatter(p) {
        const [h, w, cnt] = p.value
        if (!cnt) return `${WEEK[w]} ${pad(h)}:00<br/>无下单`
        return `${WEEK[w]} ${pad(h)}:00<br/>下单 <b>${cnt}</b> 单<br/>租金 ${money(rentOf.get(`${h}-${w}`))}`
      }
    },
    grid: { left: 8, right: 16, top: 12, bottom: 46, containLabel: true },
    xAxis: {
      type: 'category',
      data: Array.from({ length: 24 }, (_, i) => pad(i)),
      splitArea: { show: true, areaStyle: { color: ['#fff', '#fcfcfd'] } },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11, interval: 1 }
    },
    yAxis: {
      type: 'category',
      data: WEEK,
      splitArea: { show: true, areaStyle: { color: ['#fff', '#fcfcfd'] } },
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#606266', fontSize: 11 }
    },
    visualMap: {
      min: 0,
      max,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemWidth: 12,
      itemHeight: 90,
      textStyle: { color: '#909399', fontSize: 11 },
      inRange: { color: ['#f4f8ff', '#dbe9ff', '#a0cfff', '#409eff', '#1d5fd0'] }
    },
    series: [
      {
        type: 'heatmap',
        data,
        // 空格子不写数字，否则整张图都是 0，看不清哪里真的有单
        label: { show: true, fontSize: 10, color: '#4b5563', formatter: (p) => (p.value[2] ? p.value[2] : '') },
        itemStyle: { borderColor: '#fff', borderWidth: 1, borderRadius: 2 },
        emphasis: { itemStyle: { shadowBlur: 6, shadowColor: 'rgba(31,41,55,.28)' } }
      }
    ]
  }
})

/* ==================== 车型偏好气泡 ==================== */

const bubbleOption = computed(() => {
  const list = models.value || []
  const maxRent = Math.max(1, ...list.map((m) => Number(m.rentAmount || 0)))
  // 客单价最低的那一类标签放气泡下方，否则会和正上方的气泡撞在一起
  const floor = Math.min(...list.map((m) => Number(m.avgOrderAmount || 0)))
  return {
    grid: { left: 8, right: 28, top: 34, bottom: 22, containLabel: true },
    tooltip: {
      formatter(p) {
        const m = p.data.raw
        return [
          `<b>${m.categoryLabel}</b>`,
          `订单 ${m.orderCount} 单（占 ${fmtRate(m.orderRatio)}）`,
          `客单价 ${money(m.avgOrderAmount)}`,
          `租金 ${money(m.rentAmount)}（占 ${fmtRate(m.rentRatio)}）`,
          `平均租期 ${m.avgRentDays} 天 · 在册 ${m.vehicleCount} 台`
        ].join('<br/>')
      }
    },
    xAxis: {
      type: 'value',
      name: '订单量（单）',
      nameLocation: 'middle',
      nameGap: 26,
      nameTextStyle: { color: '#a8abb2', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '客单价（元/单）',
      nameTextStyle: { color: '#a8abb2', fontSize: 11 },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#909399', fontSize: 11, formatter: (v) => (v >= 1000 ? v / 1000 + 'k' : v) }
    },
    series: [
      {
        type: 'scatter',
        // 面积映射营收：半径按开方缩放，视觉面积才与数值成正比
        symbolSize: (val) => 18 + Math.sqrt(Number(val[2] || 0) / maxRent) * 34,
        itemStyle: { opacity: 0.82, borderColor: '#fff', borderWidth: 2 },
        label: {
          show: true,
          position: 'top',
          distance: 6,
          fontSize: 11,
          color: '#606266',
          fontWeight: 500,
          formatter: (p) => p.data.raw.categoryLabel
        },
        data: list.map((m) => ({
          name: m.categoryLabel,
          value: [Number(m.orderCount || 0), Number(m.avgOrderAmount || 0), Number(m.rentAmount || 0)],
          raw: m,
          itemStyle: { color: m.color || '#409eff' },
          label: { position: Number(m.avgOrderAmount || 0) === floor ? 'bottom' : 'top' }
        }))
      }
    ]
  }
})

/* ==================== 营收预测图 ==================== */

const forecastOption = computed(() => {
  const f = forecast.value || {}
  const history = f.history || []
  const future = f.forecast || []
  const n = history.length
  if (!n) return {}

  const dates = [...history.map((x) => x.date), ...future.map((x) => x.date)]
  const actual = history.map((x) => Number(x.rentAmount || 0))

  // 预测线从最后一个真实点接出去，否则中间会断一格，看着像少了一天
  const line = new Array(n).fill(null)
  line[n - 1] = actual[n - 1]
  const lower = new Array(n).fill(null)
  const band = new Array(n).fill(null)
  future.forEach((p, i) => {
    const v = Number(p.value || 0)
    const lo = Number(p.lower || 0)
    const up = Number(p.upper || 0)
    line[n + i] = v
    lower[n + i] = lo
    band[n + i] = Math.max(0, up - lo)
  })

  return {
    grid: { left: 8, right: 16, top: 34, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const idx = params[0].dataIndex
        const date = dates[idx]
        if (idx < n - 1) return `${date}<br/>租金实收：<b>${money(actual[idx])}</b>`
        if (idx === n - 1) return `${date}（今天）<br/>租金实收：<b>${money(actual[idx])}</b>`
        const p = future[idx - n]
        return [
          `${date}（${weekLabel(p.weekday)}）预测`,
          `预测值：<b>${money(p.value)}</b>`,
          `80% 区间：${money(p.lower)} ~ ${money(p.upper)}`,
          `同星期基准：${money(p.weekdayBase)}`
        ].join('<br/>')
      }
    },
    xAxis: {
      type: 'category',
      data: dates.map((d) => String(d).slice(5)),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisTick: { show: false },
      axisLabel: { color: '#909399', fontSize: 11, hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      name: '租金（元）',
      nameTextStyle: { color: '#a8abb2', fontSize: 11 },
      axisLabel: { color: '#909399', fontSize: 11, formatter: (v) => (v >= 1000 ? v / 1000 + 'k' : v) },
      splitLine: { lineStyle: { color: '#f2f3f5' } }
    },
    series: [
      {
        name: '租金实收',
        type: 'bar',
        barMaxWidth: 20,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#a8c9f5' },
              { offset: 1, color: '#dbe9ff' }
            ]
          }
        },
        data: actual
      },
      {
        name: '区间下界',
        type: 'line',
        stack: 'ci',
        symbol: 'none',
        lineStyle: { opacity: 0 },
        areaStyle: { opacity: 0 },
        data: lower
      },
      {
        name: '80% 区间',
        type: 'line',
        stack: 'ci',
        symbol: 'none',
        lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(63, 127, 245, 0.16)' },
        data: band
      },
      {
        name: '预测营收',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { width: 2, type: 'dashed', color: '#3f7ff5' },
        itemStyle: { color: '#3f7ff5' },
        data: line,
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dotted', color: '#c8c9cc' },
          label: { formatter: '今天', color: '#a8abb2', fontSize: 11, position: 'end', padding: [0, 0, 2, 0] },
          // 分界线的 xAxis 必须和 xAxis.data 完全一致（这里是 MM-DD），否则匹配不到分类
          data: [{ xAxis: String(dates[n - 1]).slice(5) }]
        }
      }
    ]
  }
})

const forecastMetrics = computed(() => {
  const f = forecast.value || {}
  return [
    { label: '预测合计', value: moneyShort(f.forecastTotal), note: `${f.horizon || horizon.value} 天`, tone: '' },
    { label: '预测日均', value: moneyShort(f.forecastDailyAvg), note: `近 28 天日均 ${moneyShort(f.recentDailyAvg)}`, tone: '' },
    {
      label: '相对近期',
      value: signed(f.vsRecentAvg),
      note: '预测日均 vs 近 28 天日均',
      tone: Number(f.vsRecentAvg) >= 0 ? 'up' : 'down'
    },
    { label: '日粒度 MAE', value: moneyShort(f.mae), note: '平均每天差多少', tone: '' },
    { label: '区间覆盖率', value: fmtRate(f.coverage), note: '名义 80%，检验区间标定', tone: '' },
    { label: '周口径 WAPE', value: fmtRate(f.weeklyWape), note: '更适合看整体误差', tone: '' },
    {
      label: '趋势斜率',
      value: (Number(f.trendSlope) >= 0 ? '+' : '') + nf0.format(Number(f.trendSlope || 0)),
      note: '元/天，近 14 天拟合',
      tone: Number(f.trendSlope) >= 0 ? 'up' : 'down'
    }
  ]
})

/* ==================== 加载 ==================== */

async function load() {
  loading.value = true
  try {
    const [h, m, f] = await Promise.all([
      getOrderHeatmap(days.value),
      getModelPreference(days.value),
      getRevenueForecast(45, horizon.value)
    ])
    heat.value = h.data || {}
    models.value = m.data || []
    forecast.value = f.data || {}
    const d = new Date()
    const p = (x) => String(x).padStart(2, '0')
    generatedAt.value = `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.analytics {
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

/* ---------- 结论卡 ---------- */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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
.kpi.tone-orange { --tone: #e6a23c; }

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

/* ---------- 卡片 ---------- */
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
  align-items: center;
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
.peak {
  padding-left: 12px;
  border-left: 1px solid #ebeef5;
  color: #606266;
}
.card-foot {
  margin-top: 6px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.card-foot b {
  color: #3f7ff5;
  font-variant-numeric: tabular-nums;
}
.sep {
  color: #dcdfe6;
}

.grid-2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

/* ---------- 分类表 ---------- */
.detail-table {
  margin-top: 2px;
}
.detail-table :deep(.el-table__header th) {
  background: #fafbfc;
  color: #909399;
  font-weight: 500;
  font-size: 12px;
}
.detail-table :deep(.el-table__cell) {
  padding: 7px 0;
  font-size: 12px;
}
.cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #303133;
}
.cat-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.ratio {
  font-variant-numeric: tabular-nums;
  color: #606266;
}
.table-note {
  margin-top: 8px;
  font-size: 11px;
  color: #b1b3b8;
}
.good {
  color: #67c23a;
  font-weight: 600;
}

/* ---------- 预测指标条 ---------- */
.metric-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 1px;
  background: #ebeef5;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  margin-top: 4px;
}
.metric {
  background: #fafbfc;
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.m-label {
  font-size: 11px;
  color: #909399;
}
.m-value {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  font-variant-numeric: tabular-nums;
}
.m-value.up {
  color: #f56c6c;
}
.m-value.down {
  color: #67c23a;
}
.m-note {
  font-size: 10px;
  color: #b1b3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.method {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
  padding: 9px 12px;
  background: #f7f9fc;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #7a8194;
}
.method .el-icon {
  margin-top: 2px;
  color: #a8abb2;
  flex-shrink: 0;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1400px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .metric-row {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
@media (max-width: 1100px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 720px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .metric-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
