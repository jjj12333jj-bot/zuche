<template>
  <div class="order-detail-page">
    <div class="page-container">
      <el-breadcrumb class="breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
        <el-breadcrumb-item>{{ detail?.orderNo || '订单详情' }}</el-breadcrumb-item>
      </el-breadcrumb>

      <div v-loading="loading">
        <template v-if="detail">
          <div class="card status-banner">
            <div class="left">
              <el-tag :type="ORDER_STATUS_TAG[detail.status]" size="large" effect="dark">
                {{ detail.statusLabel }}
              </el-tag>
              <div class="no-block">
                <div class="order-no">订单号 {{ detail.orderNo }}</div>
                <div class="time">下单时间 {{ formatDateTime(detail.createTime) }}</div>
              </div>
            </div>
            <div class="right">
              <el-button v-if="detail.canCancel" :disabled="busy" @click="onCancel">取消订单</el-button>
              <el-button v-if="detail.canPickup" type="success" :loading="busy" @click="onPickup">
                确认取车
              </el-button>
              <el-button v-if="detail.canApplyReturn" :loading="busy" @click="onApplyReturn">
                申请归还
              </el-button>
              <el-button
                v-if="detail.canConfirmReturn"
                type="success"
                :loading="busy"
                @click="onConfirmReturn"
              >
                确认归还并结算
              </el-button>
              <el-button v-if="detail.canPay" type="primary" @click="router.push(`/cashier/${detail.id}`)">
                去支付 ¥{{ formatMoney(detail.totalAmount) }}
              </el-button>
              <el-button
                v-if="detail.canEvaluate"
                type="warning"
                :loading="evalSubmitting"
                @click="openEvaluate"
              >
                发表评价
              </el-button>
            </div>
          </div>

          <section v-if="detail.evaluation" class="card eval-mine">
            <h3 class="card-title">
              我的评价
              <span class="title-note">{{ formatDateTime(detail.evaluation.createTime) }}</span>
            </h3>
            <div class="eval-head">
              <div class="eval-dim">
                <span class="dim-name">车辆状况</span>
                <el-rate :model-value="detail.evaluation.vehicleRating" disabled size="small" />
                <b class="dim-score">{{ detail.evaluation.vehicleRating }}.0</b>
              </div>
              <div class="eval-dim">
                <span class="dim-name">门店服务</span>
                <el-rate :model-value="detail.evaluation.storeRating" disabled size="small" />
                <b class="dim-score">{{ detail.evaluation.storeRating }}.0</b>
              </div>
              <div class="eval-dim total">
                <span class="dim-name">综合</span>
                <b class="dim-score">{{ detail.evaluation.rating }}.0</b>
                <span class="dim-label">{{ detail.evaluation.ratingLabel }}</span>
              </div>
            </div>
            <p class="eval-content">{{ detail.evaluation.content }}</p>
            <div v-if="detail.evaluation.images && detail.evaluation.images.length" class="eval-images">
              <el-image
                v-for="(img, i) in detail.evaluation.images"
                :key="i"
                :src="resolveImage(img)"
                :preview-src-list="detail.evaluation.images.map(resolveImage)"
                :initial-index="i"
                fit="cover"
                class="eval-thumb"
              />
            </div>
            <div v-if="detail.evaluation.reply" class="eval-reply">
              <span class="reply-tag">商家回复</span>
              <span>{{ detail.evaluation.reply }}</span>
            </div>
          </section>

          <div class="info-grid">
            <div class="card">
              <h3 class="card-title">订单信息</h3>
              <el-descriptions :column="2" size="small" border>
                <el-descriptions-item label="取车网点">{{ detail.storeName }}</el-descriptions-item>
                <el-descriptions-item label="归还网点">
                  {{ detail.returnStoreName || detail.storeName }}
                </el-descriptions-item>
                <el-descriptions-item label="租期">
                  {{ formatDate(detail.startDate) }} 至 {{ formatDate(detail.endDate) }}
                </el-descriptions-item>
                <el-descriptions-item label="租期天数">{{ detail.rentDays }} 天</el-descriptions-item>
                <el-descriptions-item label="车辆数量">{{ detail.vehicleCount }} 台</el-descriptions-item>
                <el-descriptions-item label="支付时间">{{ formatDateTime(detail.payTime) }}</el-descriptions-item>
                <el-descriptions-item label="取车时间">{{ formatDateTime(detail.pickupTime) }}</el-descriptions-item>
                <el-descriptions-item label="归还时间">{{ formatDateTime(detail.returnTime) }}</el-descriptions-item>
                <el-descriptions-item label="押金退还">
                  {{ DEPOSIT_REFUND_LABEL[detail.depositRefundStatus] || '未退还' }}
                </el-descriptions-item>
                <el-descriptions-item v-if="Number(detail.overdueDays) > 0" label="逾期天数">
                  <span class="danger-text">{{ detail.overdueDays }} 天</span>
                </el-descriptions-item>
                <el-descriptions-item v-if="Number(detail.lateFee) > 0" :label="lateFeeSettled ? '逾期违约金' : '预估违约金'">
                  <span class="danger-text">¥{{ formatMoney(detail.lateFee) }}</span>
                  <span class="fee-note">{{ lateFeeSettled ? '已从押金中扣除' : '确认归还时扣除，多退少补' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detail.remark || '无' }}</el-descriptions-item>
                <el-descriptions-item v-if="detail.cancelReason" label="取消原因" :span="2">
                  {{ detail.cancelReason }}
                </el-descriptions-item>
              </el-descriptions>

              <el-alert
                v-if="Number(detail.overdueDays) > 0"
                class="overdue-alert"
                type="warning"
                :closable="false"
                show-icon
                :title="overdueText"
              />
            </div>

            <div class="card credit-card">
              <div class="credit-head">
                <h3 class="card-title">信用风控快照</h3>
                <el-tag size="small" effect="dark" :color="levelColor">
                  {{ detail.creditLevelLabel || detail.creditLevelSnapshot }}
                </el-tag>
              </div>
              <div class="credit-row">
                <span>下单时信用分</span><b>{{ detail.creditScoreSnapshot }} 分</b>
              </div>
              <div class="credit-row">
                <span>押金系数</span><b>×{{ detail.depositCoefficient }}</b>
              </div>
              <div class="credit-row">
                <span>标准押金合计</span><b>¥{{ formatMoney(detail.depositStandardTotal) }}</b>
              </div>
              <div class="credit-row">
                <span>应付押金</span><b>¥{{ formatMoney(detail.depositAmount) }}</b>
              </div>
              <div v-if="Number(detail.depositSaved) > 0" class="credit-row saved">
                <span>信用减免</span><b>¥{{ formatMoney(detail.depositSaved) }}</b>
              </div>
              <p class="credit-note">
                押金系数由下单时的信用等级决定并写入订单快照，之后信用分变化不会改变本单金额。
              </p>
            </div>
          </div>

          <div class="card">
            <h3 class="card-title">车辆明细</h3>
            <el-table :data="detail.items" size="small" border>
              <el-table-column prop="vehicleName" label="车辆" min-width="180" />
              <el-table-column prop="licensePlate" label="车牌" width="110" />
              <el-table-column label="日租金" width="100" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.dailyPrice) }}</template>
              </el-table-column>
              <el-table-column prop="rentDays" label="天数" width="70" align="center" />
              <el-table-column prop="quantity" label="数量" width="70" align="center" />
              <el-table-column label="租金" width="110" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.rentAmount) }}</template>
              </el-table-column>
              <el-table-column label="押金" width="110" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.depositAmount) }}</template>
              </el-table-column>
            </el-table>

            <div class="sum-line">
              <span>租金合计 <b>¥{{ formatMoney(detail.rentAmount) }}</b></span>
              <span>押金合计 <b>¥{{ formatMoney(detail.depositAmount) }}</b></span>
              <span class="grand">应付合计 <b>¥{{ formatMoney(detail.totalAmount) }}</b></span>
            </div>
          </div>

          <div class="card">
            <h3 class="card-title">
              支付流水
              <span class="title-note">租金计入营收，押金为代收待退</span>
            </h3>
            <el-table :data="detail.payments" size="small" border>
              <el-table-column prop="payNo" label="流水号" min-width="210" />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain" :type="row.payType === 'RENT' ? 'success' : 'warning'">
                    {{ row.payTypeLabel || row.payType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="110" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column label="渠道" width="130">
                <template #default="{ row }">
                  {{ row.payMethod === 'MOCK_ALIPAY' ? '支付宝' : '微信支付' }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === 'SUCCESS' ? 'success' : 'info'" effect="plain">
                    {{ row.status === 'SUCCESS' ? '已支付' : row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="支付时间" width="160">
                <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
              </el-table-column>
            </el-table>
            <el-empty
              v-if="!detail.payments || detail.payments.length === 0"
              description="订单尚未支付，暂无流水"
              :image-size="70"
            />
          </div>

          <div v-if="detail.refunds && detail.refunds.length > 0" class="card">
            <h3 class="card-title">
              退款流水
              <span class="title-note">押金于还车结算后原路退回</span>
            </h3>
            <el-table :data="detail.refunds" size="small" border>
              <el-table-column prop="refundNo" label="退款单号" min-width="200" />
              <el-table-column label="类型" width="110">
                <template #default="{ row }">
                  <el-tag
                    size="small"
                    effect="plain"
                    :type="row.refundType === 'DEPOSIT' ? 'warning' : 'info'"
                  >
                    {{ row.refundTypeLabel }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="110" align="right">
                <template #default="{ row }">
                  <span class="refund-amount">¥{{ formatMoney(row.amount) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
              <el-table-column label="状态" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" type="success" effect="plain">{{ row.statusLabel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="退款时间" width="160">
                <template #default="{ row }">{{ formatDateTime(row.refundTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>

        <el-empty v-else-if="!loading" description="订单不存在或无权查看">
          <el-button type="primary" @click="router.push('/orders')">返回我的订单</el-button>
        </el-empty>
      </div>
    </div>

    <el-dialog
      v-model="evalVisible"
      title="发表评价"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetEvalForm"
    >
      <div class="eval-form">
        <p class="eval-tip">
          车况和服务分开打分：车的问题归车，人的问题归人，商家才知道该去整备车辆还是该培训店员。
        </p>
        <div class="eval-row">
          <span class="eval-label">车辆状况</span>
          <el-rate v-model="evalForm.vehicleRating" :texts="RATE_TEXTS" show-text />
        </div>
        <div class="eval-row">
          <span class="eval-label">门店服务</span>
          <el-rate v-model="evalForm.storeRating" :texts="RATE_TEXTS" show-text />
        </div>
        <div class="eval-field">
          <span class="eval-label">评价内容</span>
          <el-input
            v-model="evalForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="说说车况、取还车流程、店员服务……至少 5 个字"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="evalVisible = false">取消</el-button>
        <el-button type="primary" :loading="evalSubmitting" @click="onSubmitEvaluation">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyReturnOrder,
  cancelOrder,
  confirmReturnOrder,
  getOrderDetail,
  pickupOrder,
  submitEvaluation
} from '@/api/order'
import { useUserStore } from '@/store/user'
import { resolveImage } from '@/utils/image'
import { CREDIT_LEVELS, formatDate, formatDateTime, formatMoney, ORDER_STATUS_TAG } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const detail = ref(null)
const loading = ref(false)
const busy = ref(false)

/* ==================== 评价 ==================== */

const RATE_TEXTS = ['很不满意', '不满意', '一般', '满意', '非常满意']
const evalVisible = ref(false)
const evalSubmitting = ref(false)
const evalForm = ref({ vehicleRating: 5, storeRating: 5, content: '' })

function openEvaluate() {
  resetEvalForm()
  evalVisible.value = true
}

function resetEvalForm() {
  evalForm.value = { vehicleRating: 5, storeRating: 5, content: '' }
}

async function onSubmitEvaluation() {
  const { vehicleRating, storeRating, content } = evalForm.value
  if (!vehicleRating || !storeRating) {
    ElMessage.warning('请为车辆状况和门店服务都打个分')
    return
  }
  if (!content || content.trim().length < 5) {
    ElMessage.warning('评价内容至少 5 个字')
    return
  }
  evalSubmitting.value = true
  try {
    const res = await submitEvaluation(detail.value.id, {
      vehicleRating,
      storeRating,
      content: content.trim()
    })
    ElMessage.success(res.message || '评价已提交')
    evalVisible.value = false
    await load()
  } finally {
    evalSubmitting.value = false
  }
}

const DEPOSIT_REFUND_LABEL = {
  NONE: '未退还',
  PENDING: '处理中',
  REFUNDED: '已退还'
}

const levelColor = computed(() => CREDIT_LEVELS[detail.value?.creditLevelSnapshot]?.color || '#909399')

// 违约金在归还结算时才真正扣除，结算前显示的是按当前规则算出的预估
const lateFeeSettled = computed(() => detail.value?.lateFeeSettled !== false)
const overdueText = computed(() => {
  const d = detail.value || {}
  const days = d.overdueDays
  const fee = formatMoney(d.lateFee)
  return lateFeeSettled.value
    ? `本单逾期 ${days} 天，已按平台规则从押金中扣除违约金 ¥${fee}，剩余押金已原路退还`
    : `本单已逾期 ${days} 天，确认归还时将从押金中扣除违约金（当前预估 ¥${fee}），余额退还`
})

async function load() {
  loading.value = true
  try {
    const res = await getOrderDetail(route.params.id)
    detail.value = res.data
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

/** 把后端返回的信用变动逐条弹出来，用户点完操作立刻知道分数怎么变的 */
function creditToast(changes) {
  const list = (Array.isArray(changes) ? changes : [changes]).filter((c) => c && c.scoreChange)
  list.forEach((c) => {
    const sign = c.scoreChange > 0 ? '+' : ''
    const levelText = c.levelChanged ? `，等级降为「${c.levelAfterLabel}」` : ''
    ElMessage({
      type: c.scoreChange > 0 ? 'success' : 'warning',
      duration: 4500,
      message: `信用分 ${sign}${c.scoreChange} · ${c.actionName}，当前 ${c.scoreAfter} 分${levelText}`
    })
  })
}

async function onCancel() {
  const paid = detail.value.status === 'WAIT_PICKUP'
  const tip = paid
    ? '该订单已支付，取消后将全额退还租金与押金，同时扣除 3 分信用分。确定取消吗？'
    : '确定取消这笔订单吗？取消后不可恢复，未支付订单不影响信用分。'
  try {
    await ElMessageBox.confirm(tip, '取消订单', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
  } catch (e) {
    return
  }

  busy.value = true
  try {
    const res = await cancelOrder(detail.value.id, {
      reason: paid ? '用户取消已支付订单' : '用户在订单详情取消'
    })
    ElMessage.success(res.message || '订单已取消')
    creditToast([res.data?.creditChange, res.data?.behaviorChange])
    await userStore.refresh().catch(() => {})
  } finally {
    busy.value = false
    load()
  }
}

async function onPickup() {
  try {
    await ElMessageBox.confirm(
      '确认已拿到车辆？取车后订单进入租期，将不能再取消，只能走归还流程。',
      '确认取车',
      { type: 'info', confirmButtonText: '已取车' }
    )
  } catch (e) {
    return
  }
  busy.value = true
  try {
    const res = await pickupOrder(detail.value.id)
    ElMessage.success(res.message || '已确认取车')
  } finally {
    busy.value = false
    load()
  }
}

async function onApplyReturn() {
  try {
    await ElMessageBox.confirm(
      '提交归还申请后，请在还车时点击「确认归还并结算」，系统将退还押金并结算信用分。',
      '申请归还',
      { type: 'info', confirmButtonText: '提交申请' }
    )
  } catch (e) {
    return
  }
  busy.value = true
  try {
    const res = await applyReturnOrder(detail.value.id)
    ElMessage.success(res.message || '已提交归还申请')
  } finally {
    busy.value = false
    load()
  }
}

async function onConfirmReturn() {
  try {
    await ElMessageBox.confirm(
      `确认归还车辆？押金 ¥${formatMoney(detail.value.depositAmount)} 将原路退回，并按本次履约情况结算信用分。` +
        '若已超过计划还车日期，将按逾期天数从押金中扣除违约金，余额退还。',
      '确认归还',
      { type: 'warning', confirmButtonText: '确认归还' }
    )
  } catch (e) {
    return
  }
  busy.value = true
  try {
    const res = await confirmReturnOrder(detail.value.id)
    ElMessage.success(res.message || '归还完成')
    creditToast(res.data?.creditChanges)
    await userStore.refresh().catch(() => {})
  } finally {
    busy.value = false
    load()
  }
}

onMounted(load)
</script>

<style scoped>
.order-detail-page {
  padding: 22px 0 60px;
}

.breadcrumb {
  font-size: 13px;
  margin-bottom: 16px;
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

.status-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.status-banner .left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-no {
  font-size: 15px;
  font-weight: 600;
}

.time {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.info-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: 16px;
}

.credit-card {
  background: linear-gradient(135deg, #f0f9eb 0%, #ffffff 72%);
  border-color: #e1f3d8;
}

.credit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.credit-head .card-title {
  margin-bottom: 8px;
}

.credit-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
  padding: 6px 0;
}

.credit-row b {
  color: #303133;
}

.credit-row.saved b {
  color: #67c23a;
}

.credit-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.7;
}

.danger-text {
  color: #f56c6c;
  font-weight: 600;
}

.fee-note {
  margin-left: 6px;
  font-size: 11px;
  color: #a8abb2;
}

.overdue-alert {
  margin-top: 12px;
}

.refund-amount {
  color: #67c23a;
  font-weight: 600;
}

.sum-line {
  display: flex;
  justify-content: flex-end;
  gap: 26px;
  margin-top: 14px;
  font-size: 13px;
  color: #606266;
}

.sum-line b {
  color: #303133;
}

.sum-line .grand b {
  color: #f56c6c;
  font-size: 18px;
}

@media (max-width: 960px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .status-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* ==================== 评价 ==================== */

.eval-mine .card-title {
  display: flex;
  align-items: baseline;
}

.eval-head {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  padding-bottom: 14px;
  border-bottom: 1px dashed #ebeef5;
}

.eval-dim {
  display: flex;
  align-items: center;
  gap: 8px;
}

.eval-dim.total {
  padding-left: 22px;
  border-left: 1px solid #ebeef5;
}

.dim-name {
  font-size: 13px;
  color: #909399;
}

.dim-score {
  font-size: 15px;
  color: #e6a23c;
}

.dim-label {
  font-size: 12px;
  color: #909399;
}

.eval-content {
  margin: 14px 0 0;
  font-size: 14px;
  line-height: 1.75;
  color: #303133;
  white-space: pre-wrap;
}

.eval-images {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.eval-thumb {
  width: 84px;
  height: 84px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.eval-reply {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

.reply-tag {
  display: inline-block;
  margin-right: 8px;
  padding: 0 6px;
  font-size: 12px;
  color: #409eff;
  border: 1px solid #d9ecff;
  background: #ecf5ff;
  border-radius: 4px;
}

.eval-tip {
  margin: 0 0 18px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
  background: #f5f7fa;
  border-radius: 8px;
}

.eval-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.eval-field {
  margin-top: 4px;
}

.eval-label {
  display: inline-block;
  width: 72px;
  font-size: 14px;
  color: #606266;
}

.eval-field .eval-label {
  vertical-align: top;
  margin-bottom: 8px;
}
</style>
