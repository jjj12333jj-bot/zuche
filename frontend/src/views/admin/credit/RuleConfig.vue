<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>信用规则</h2>
        <p class="sub">
          分值、区间、押金系数、违约金标准全部存在 <code>credit_rule</code> 表，改完立即生效，代码里没有硬编码
        </p>
      </div>
      <el-button :loading="recalcing" plain @click="onRecalc">
        <el-icon><Refresh /></el-icon>
        重算全员等级
      </el-button>
    </div>

    <el-tabs v-model="tab" class="tabs" @tab-change="onTabChange">
      <!-- ==================== 等级规则 ==================== -->
      <el-tab-pane name="LEVEL">
        <template #label>
          等级规则
          <el-tag size="small" effect="plain" type="info" class="tab-tag">押金系数</el-tag>
        </template>

        <div class="card">
          <div class="card-head">
            <h3>四档等级区间</h3>
            <div class="head-actions">
              <span class="hint">区间必须从 0 分起、首尾相接；保存后自动重算全部用户等级</span>
              <el-button type="primary" :loading="savingLevels" @click="saveLevels">保存并重算</el-button>
            </div>
          </div>

          <el-table :data="levels" v-loading="loading" size="small" border>
            <el-table-column label="等级" width="190">
              <template #default="{ row }">
                <div class="level-cell">
                  <span class="dot" :style="{ background: row.color || '#909399' }" />
                  <el-input v-model="row.ruleName" size="small" maxlength="10" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="规则码" width="110">
              <template #default="{ row }"><code class="code">{{ row.ruleCode }}</code></template>
            </el-table-column>
            <el-table-column label="分数下限" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.minScore" :min="0" :max="1000" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="分数上限" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.maxScore" :min="0" :max="1000" size="small" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column label="押金系数" width="150">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.depositCoefficient"
                  :min="0.1"
                  :max="5"
                  :step="0.05"
                  :precision="2"
                  size="small"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="折算" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" :color="row.color" class="coef-tag">
                  {{ coefficientText(row.depositCoefficient) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标签色" width="90">
              <template #default="{ row }">
                <el-color-picker v-model="row.color" size="small" :predefine="PRESET_COLORS" />
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="200">
              <template #default="{ row }">
                <el-input v-model="row.description" size="small" maxlength="200" />
              </template>
            </el-table-column>
          </el-table>

          <p class="footnote">
            押金系数直接参与下单试算：0.80 表示按标准押金的 8 折收取，1.20 表示上浮 20%。
            调整后新订单按下单时的系数快照计算，历史订单不受影响。
          </p>
        </div>
      </el-tab-pane>

      <!-- ==================== 事件规则 ==================== -->
      <el-tab-pane name="ACTION">
        <template #label>
          事件规则
          <el-tag size="small" effect="plain" type="info" class="tab-tag">加减分</el-tag>
        </template>

        <div class="card">
          <div class="card-head">
            <h3>业务事件加减分</h3>
            <span class="hint">分值可正可负；单笔订单最多生效次数用于逾期按天扣分封顶</span>
          </div>
          <el-table :data="actions" v-loading="loading" size="small" border>
            <el-table-column label="事件码" width="180">
              <template #default="{ row }"><code class="code">{{ row.ruleCode }}</code></template>
            </el-table-column>
            <el-table-column prop="ruleName" label="事件名称" width="160" />
            <el-table-column label="分值" width="100" align="right">
              <template #default="{ row }">
                <span class="delta" :class="row.scoreValue > 0 ? 'up' : (row.scoreValue < 0 ? 'down' : 'flat')">
                  {{ row.scoreValue > 0 ? '+' : '' }}{{ row.scoreValue }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="最多生效" width="100" align="center">
              <template #default="{ row }">{{ row.maxTimes || 1 }} 次</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ==================== 风控参数 ==================== -->
      <el-tab-pane name="PARAM">
        <template #label>
          风控参数
          <el-tag size="small" effect="plain" type="warning" class="tab-tag">阈值</el-tag>
        </template>

        <div class="card">
          <div class="card-head">
            <h3>逾期与黑名单阈值</h3>
            <span class="hint">定时任务、违约金计算、下单拦截全部读这里的值</span>
          </div>
          <el-table :data="params" v-loading="loading" size="small" border>
            <el-table-column label="参数" width="190">
              <template #default="{ row }">
                <div class="param-name">{{ row.ruleName }}</div>
                <code class="code">{{ row.ruleCode }}</code>
              </template>
            </el-table-column>
            <el-table-column label="当前值" width="130" align="right">
              <template #default="{ row }">
                <b class="param-value">{{ row.scoreValue }}</b>
                <span v-if="row.ruleCode === 'LATE_FEE_DAILY'" class="unit">元/天</span>
                <span v-else-if="row.ruleCode.endsWith('DAYS')" class="unit">天</span>
                <span v-else-if="row.ruleCode === 'BLACKLIST_SCORE'" class="unit">分</span>
              </template>
            </el-table-column>
            <el-table-column label="封顶次数" width="110" align="center">
              <template #default="{ row }">
                {{ row.maxTimes ? row.maxTimes + ' 次' : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="300" show-overflow-tooltip />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">修改</el-button>
              </template>
            </el-table-column>
          </el-table>

          <p class="footnote">
            「低分拉黑阈值」填 0 表示关闭按分数拉黑；「黑名单有效期」到期后由每日扫描自动解除，
            不需要人工干预。违约金累计不超过押金总额。
          </p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ==================== 编辑弹窗（事件 / 参数） ==================== -->
    <el-dialog v-model="editVisible" :title="`修改「${form.ruleName}」`" width="520px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" maxlength="50" />
        </el-form-item>
        <el-form-item :label="form.ruleType === 'PARAM' ? '参数值' : '分值'">
          <el-input-number
            v-model="form.scoreValue"
            :min="form.ruleType === 'PARAM' ? -10000 : -100"
            :max="form.ruleType === 'PARAM' ? 10000 : 100"
            controls-position="right"
            style="width: 180px"
          />
          <span class="form-tip">
            {{ form.ruleType === 'PARAM' ? '阈值/单价，非信用分' : '正数加分，负数扣分' }}
          </span>
        </el-form-item>
        <el-form-item v-if="form.ruleCode === 'LATE_FEE_DAILY'">
          <span class="form-warn">违约金单价填 0 等于关闭违约金，逾期只扣信用分不扣押金</span>
        </el-form-item>
        <el-form-item label="最多生效次数">
          <el-input-number v-model="form.maxTimes" :min="1" :max="365" controls-position="right" style="width: 180px" />
          <span class="form-tip">
            {{ form.ruleType === 'PARAM' ? '违约金最多计费天数' : '同一订单内最多触发几次' }}
          </span>
        </el-form-item>
        <el-form-item label="规则说明">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item v-if="form.ruleType === 'ACTION'" label="启用状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
          <span class="form-tip">停用后该类事件不再产生加减分</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRules, recalcLevels, updateLevelRules, updateRule } from '@/api/adminCredit'
import { coefficientText } from '@/utils/format'

const PRESET_COLORS = ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#909399']

const tab = ref('LEVEL')
const loading = ref(false)
const saving = ref(false)
const savingLevels = ref(false)
const recalcing = ref(false)

const levels = ref([])
const actions = ref([])
const params = ref([])

const editVisible = ref(false)
const form = reactive({
  id: null,
  ruleType: '',
  ruleCode: '',
  ruleName: '',
  scoreValue: 0,
  maxTimes: 1,
  description: '',
  status: 1
})

async function load(type = tab.value) {
  loading.value = true
  try {
    const res = await getRules(type)
    const list = (res.data || []).map((item) => ({ ...item }))
    if (type === 'LEVEL') {
      levels.value = list
    } else if (type === 'ACTION') {
      actions.value = list
    } else {
      params.value = list
    }
  } finally {
    loading.value = false
  }
}

function onTabChange(name) {
  load(name)
}

async function saveLevels() {
  // 前端先做一次基本校验，避免只为了看后端报错而多跑一趟。
  // 提示语与后端保持一致，否则用户看到的是笼统的一句"有空隙或重叠"，不知道改哪一段。
  const sorted = [...levels.value].sort((a, b) => a.minScore - b.minScore)
  if (!sorted.length || Number(sorted[0].minScore) !== 0) {
    ElMessage.warning('最低一档必须从 0 分起')
    return
  }
  for (let i = 0; i < sorted.length; i += 1) {
    const cur = sorted[i]
    if (Number(cur.minScore) > Number(cur.maxScore)) {
      ElMessage.warning(`「${cur.ruleName}」的下限不能大于上限`)
      return
    }
    if (i > 0) {
      const prev = sorted[i - 1]
      const gapStart = Number(prev.maxScore) + 1
      const gapEnd = Number(cur.minScore) - 1
      if (gapEnd >= gapStart) {
        ElMessage.warning(`${gapStart}~${gapEnd} 分没有对应等级，区间必须首尾相接`)
        return
      }
      if (Number(cur.minScore) <= Number(prev.maxScore)) {
        ElMessage.warning(
          `「${prev.ruleName}」上限 ${prev.maxScore} 与「${cur.ruleName}」下限 ${cur.minScore} 重叠，请调整区间`
        )
        return
      }
    }
  }
  savingLevels.value = true
  try {
    await updateLevelRules(
      levels.value.map((row) => ({
        id: row.id,
        ruleName: row.ruleName,
        minScore: row.minScore,
        maxScore: row.maxScore,
        depositCoefficient: row.depositCoefficient,
        color: row.color,
        description: row.description
      }))
    )
    ElMessage.success('等级规则已保存，用户等级已按新区间重算')
    load('LEVEL')
  } finally {
    savingLevels.value = false
  }
}

async function onRecalc() {
  await ElMessageBox.confirm('将按当前等级区间重算全部用户的信用等级，是否继续？', '重算等级', {
    type: 'warning'
  }).catch(() => null).then(async (action) => {
    if (!action) return
    recalcing.value = true
    try {
      const res = await recalcLevels()
      ElMessage.success(`重算完成，变更 ${res.data?.changed ?? 0} 人`)
    } finally {
      recalcing.value = false
    }
  })
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    ruleType: row.ruleType,
    ruleCode: row.ruleCode,
    ruleName: row.ruleName,
    scoreValue: row.scoreValue ?? 0,
    maxTimes: row.maxTimes ?? 1,
    description: row.description || '',
    status: row.status ?? 1
  })
  editVisible.value = true
}

async function saveRule() {
  // 参数类规则允许任何非负数，但负数要在提交前明确拦住：
  // 输入框若用 :min="0" 静默钳制，用户填 -50 会变成 0 并被保存，"校验通过"其实是假象
  if (form.ruleType === 'PARAM' && Number(form.scoreValue) < 0) {
    ElMessage.warning('风控参数不能为负值')
    return
  }
  saving.value = true
  try {
    await updateRule(form.id, {
      ruleName: form.ruleName,
      scoreValue: form.scoreValue,
      maxTimes: form.maxTimes,
      description: form.description,
      status: form.ruleType === 'ACTION' ? form.status : undefined
    })
    ElMessage.success('已保存，新规则立即生效')
    editVisible.value = false
    load(form.ruleType)
  } finally {
    saving.value = false
  }
}

onMounted(() => load('LEVEL'))
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
}

.sub {
  margin: 8px 0 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
}

.sub code,
.code {
  background: #f4f6f9;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #475669;
}

.tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.tab-tag {
  margin-left: 4px;
  transform: scale(0.9);
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 18px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  font-size: 12px;
  color: #a8abb2;
}

.level-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.coef-tag {
  border: none;
  color: #fff;
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

.param-name {
  font-size: 13px;
  color: #303133;
}

.param-value {
  font-size: 15px;
  color: #1f3a5f;
}

.unit {
  margin-left: 3px;
  font-size: 12px;
  color: #a8abb2;
}

.footnote {
  margin: 12px 0 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.8;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #a8abb2;
}

.form-warn {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}
</style>
