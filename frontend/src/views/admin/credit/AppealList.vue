<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>信用申诉</h2>
        <p class="sub">
          审核通过后由信用服务补分，落 <code>change_type = APPEAL</code> 日志；同一张申诉单重复审批只会补一次
        </p>
      </div>
      <el-alert
        class="tip"
        type="info"
        :closable="false"
        show-icon
        title="申诉是扣分的唯一救济通道"
        description="用户只能对真实的负分记录发起申诉，不能直接修改自己的分数。"
      />
    </div>

    <div class="card">
      <el-tabs v-model="status" @tab-change="onSearch">
        <el-tab-pane label="待处理" name="PENDING" />
        <el-tab-pane label="已通过" name="APPROVED" />
        <el-tab-pane label="已驳回" name="REJECTED" />
        <el-tab-pane label="全部" name="" />
      </el-tabs>

      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="申诉单号 / 用户名 / 订单号"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="申诉单号" width="170">
          <template #default="{ row }"><code class="code">{{ row.appealNo }}</code></template>
        </el-table-column>
        <el-table-column label="申诉人" width="130">
          <template #default="{ row }">
            <div class="name">{{ row.realName || row.username }}</div>
            <div class="account">{{ row.creditScore }} 分 · {{ row.creditLevel }}</div>
          </template>
        </el-table-column>
        <el-table-column label="被申诉记录" min-width="190">
          <template #default="{ row }">
            <div class="log-action">
              {{ row.actionName || (row.creditLogId ? '扣分记录' : '未关联扣分记录') }}
              <span v-if="row.logScoreChange" class="delta down">{{ row.logScoreChange }}</span>
            </div>
            <div class="log-reason">
              {{ row.logReason || (row.creditLogId ? '—' : '用户对整单处理结果发起申诉') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="appealReason" label="申诉理由" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理结果" width="150">
          <template #default="{ row }">
            <span v-if="row.status === 'PENDING'" class="text-muted">待处理</span>
            <span v-else>
              <b v-if="row.status === 'APPROVED'" class="up">+{{ row.restoreScore }} 分</b>
              <span class="text-muted">{{ row.handleRemark || '—' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openHandle(row)">处理</el-button>
            <span v-else class="text-muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="current"
          background
          layout="total, prev, pager, next"
          :total="Number(total)"
          :page-size="pageSize"
          @current-change="load"
        />
      </div>
    </div>

    <!-- ==================== 处理弹窗 ==================== -->
    <el-dialog v-model="handleVisible" title="处理信用申诉" width="600px" destroy-on-close>
      <div class="detail">
        <div class="detail-row">
          <span class="label">申诉人</span>
          <b>{{ detail.realName || detail.username }}</b>
          <span class="muted">当前 {{ detail.creditScore }} 分</span>
        </div>
        <div class="detail-row">
          <span class="label">申诉理由</span>
          <p class="paragraph">{{ detail.appealReason }}</p>
        </div>
        <div class="detail-row">
          <span class="label">被申诉记录</span>
          <p class="paragraph">
            {{ detail.actionName || (detail.creditLogId ? '扣分记录' : '未关联扣分记录') }}
            <span v-if="detail.logScoreChange" class="delta down">{{ detail.logScoreChange }} 分</span>
            <br />
            <span class="muted">{{ detail.logReason || (detail.creditLogId ? '无说明' : '用户对整单处理结果发起申诉') }}</span>
            <span v-if="detail.orderNo" class="muted"> · 订单 {{ detail.orderNo }}</span>
          </p>
        </div>
      </div>

      <el-form label-width="90px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.approved">
            <el-radio-button :value="true">通过并补分</el-radio-button>
            <el-radio-button :value="false">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="handleForm.approved" label="补回分值">
          <el-input-number v-model="handleForm.restoreScore" :min="1" :max="50" controls-position="right" style="width: 150px" />
          <span class="form-tip">默认取原扣分绝对值，最多 50 分</span>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input
            v-model="handleForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            :placeholder="handleForm.approved ? '例如：核实网点记录，车辆故障属实，补回扣分' : '例如：逾期事实成立，维持原判'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAppeals, handleAppeal } from '@/api/adminCredit'
import { formatDateTime } from '@/utils/format'

const status = ref('PENDING')
const keyword = ref('')
const rows = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = 10
const loading = ref(false)

const handleVisible = ref(false)
const submitting = ref(false)
const detail = ref({})
const handleForm = reactive({ approved: true, restoreScore: 3, remark: '' })

function statusType(value) {
  if (value === 'APPROVED') return 'success'
  if (value === 'REJECTED') return 'danger'
  return 'warning'
}

async function load() {
  loading.value = true
  try {
    const res = await getAppeals({
      status: status.value || undefined,
      keyword: keyword.value || undefined,
      current: current.value,
      pageSize
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  current.value = 1
  load()
}

function openHandle(row) {
  detail.value = row
  const abs = Math.min(Math.abs(Number(row.logScoreChange || 3)), 50) || 3
  handleForm.approved = true
  handleForm.restoreScore = abs
  handleForm.remark = ''
  handleVisible.value = true
}

async function submit() {
  submitting.value = true
  try {
    const res = await handleAppeal(detail.value.id, {
      approved: handleForm.approved,
      restoreScore: handleForm.approved ? handleForm.restoreScore : undefined,
      remark: handleForm.remark || undefined
    })
    ElMessage.success(res.message || '处理完成')
    handleVisible.value = false
    load()
  } finally {
    submitting.value = false
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
  gap: 20px;
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

.tip {
  max-width: 330px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 8px 18px 16px;
}

.card :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.name {
  font-size: 13px;
  color: #303133;
}

.account,
.log-reason {
  font-size: 11px;
  color: #a8abb2;
  line-height: 1.5;
}

.log-action {
  font-size: 13px;
  color: #303133;
}

.delta {
  margin-left: 4px;
  font-weight: 600;
}

.delta.down {
  color: #f56c6c;
}

.up {
  color: #67c23a;
}

.text-muted {
  color: #a8abb2;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.detail {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 16px;
}

.detail-row {
  display: flex;
  gap: 12px;
  padding: 6px 0;
  font-size: 13px;
  color: #303133;
}

.detail-row .label {
  width: 82px;
  flex-shrink: 0;
  color: #909399;
}

.paragraph {
  margin: 0;
  line-height: 1.7;
}

.muted {
  color: #a8abb2;
  font-weight: 400;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #a8abb2;
}
</style>
