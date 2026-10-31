<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>车型管理</h2>
        <p class="sub">
          车型承载参数、指导日租金与标准押金，是车辆建档时的价格来源。
          「在库最低价」与「指导价」不一致时会在表格里标红——说明有人单独给车调过价。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增车型</el-button>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select
          v-model="query.brandId"
          placeholder="全部品牌"
          clearable
          filterable
          style="width: 150px"
          @change="onSearch"
        >
          <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
        </el-select>
        <el-select
          v-model="query.categoryCode"
          placeholder="全部分类"
          clearable
          style="width: 130px"
          @change="onSearch"
        >
          <el-option v-for="c in CATEGORIES" :key="c.code" :label="c.label" :value="c.code" />
        </el-select>
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 120px"
          @change="onSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="车型名 / 品牌名"
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 个车型</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="车型" min-width="200">
          <template #default="{ row }">
            <div class="model-cell">
              <img v-if="row.coverImage" :src="row.coverImage" class="cover" alt="" />
              <div v-else class="cover cover-empty">车</div>
              <div>
                <div class="name">{{ row.modelName }}</div>
                <div class="account">{{ row.brandName || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryType(row.categoryCode)" effect="plain">
              {{ row.categoryLabel }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="参数" width="150">
          <template #default="{ row }">
            <div class="name">{{ row.seats }} 座 · {{ row.gearboxLabel }}</div>
            <div class="account">{{ row.fuelLabel }}{{ row.displacement ? ' · ' + row.displacement : '' }}</div>
          </template>
        </el-table-column>

        <el-table-column label="指导日租金" width="120" align="right">
          <template #default="{ row }">
            <span class="money">¥{{ formatMoney(row.guidePrice) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="标准押金" width="110" align="right">
          <template #default="{ row }">
            <span class="money-sub">¥{{ formatMoney(row.deposit) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="在库" width="130">
          <template #default="{ row }">
            <div class="name">{{ row.vehicleCount || 0 }} 台</div>
            <div v-if="priceGap(row)" class="account warn">
              最低 ¥{{ formatMoney(row.minDailyPrice) }}
            </div>
            <div v-else class="account">价格一致</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="185" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.vehicleCount > 0"
              :title="row.vehicleCount > 0 ? '车型下还有车辆，不能删除' : ''"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="load"
          @size-change="onSearch"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑车型' : '新增车型'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="所属品牌" prop="brandId">
          <el-select v-model="form.brandId" filterable placeholder="请选择品牌" style="width: 100%">
            <el-option v-for="b in brands" :key="b.id" :label="b.brandName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车型名" prop="modelName">
          <el-input v-model="form.modelName" maxlength="50" placeholder="例如 朗逸 2023款" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryCode">
          <el-radio-group v-model="form.categoryCode">
            <el-radio-button v-for="c in CATEGORIES" :key="c.code" :value="c.code">
              {{ c.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="座位 / 变速箱">
          <el-input-number v-model="form.seats" :min="2" :max="9" controls-position="right" style="width: 110px" />
          <el-radio-group v-model="form.gearbox" class="inline-group">
            <el-radio-button value="AUTO">自动挡</el-radio-button>
            <el-radio-button value="MANUAL">手动挡</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="燃料类型">
          <el-radio-group v-model="form.fuelType">
            <el-radio-button value="GASOLINE">汽油</el-radio-button>
            <el-radio-button value="HYBRID">混动</el-radio-button>
            <el-radio-button value="ELECTRIC">纯电</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排量">
          <el-input v-model="form.displacement" maxlength="20" placeholder="1.5T / 纯电" />
        </el-form-item>
        <el-form-item label="日租金" prop="guidePrice">
          <el-input-number v-model="form.guidePrice" :min="1" :max="99999" :precision="2" controls-position="right" />
          <span class="form-hint">车辆建档时默认取这个价</span>
        </el-form-item>
        <el-form-item label="标准押金" prop="deposit">
          <el-input-number v-model="form.deposit" :min="0" :max="999999" :precision="2" controls-position="right" />
          <span class="form-hint">下单时会再乘信用系数</span>
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="/images/cars/car-1.jpg" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" />
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
            class="inline-group"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { formatMoney } from '@/utils/format'
import {
  createModel,
  deleteModel,
  getBrandOptions,
  getModels,
  updateModel,
  updateModelStatus
} from '@/api/adminVehicle'

const CATEGORIES = [
  { code: 'ECONOMY', label: '经济型' },
  { code: 'COMFORT', label: '舒适型' },
  { code: 'SUV', label: 'SUV' },
  { code: 'LUXURY', label: '豪华型' },
  { code: 'EV', label: '新能源' }
]

const CATEGORY_TYPE = {
  ECONOMY: 'info',
  COMFORT: 'primary',
  SUV: 'success',
  LUXURY: 'warning',
  EV: 'success'
}

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const brands = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({
  page: 1,
  size: 10,
  brandId: null,
  categoryCode: '',
  keyword: '',
  status: null
})

const emptyForm = () => ({
  id: null,
  brandId: null,
  modelName: '',
  categoryCode: 'ECONOMY',
  seats: 5,
  gearbox: 'AUTO',
  fuelType: 'GASOLINE',
  displacement: '',
  guidePrice: 199,
  deposit: 1000,
  coverImage: '',
  description: '',
  sortNo: 99,
  status: 1
})

const form = reactive(emptyForm())

const rules = {
  brandId: [{ required: true, message: '请选择品牌', trigger: 'change' }],
  modelName: [{ required: true, message: '请输入车型名', trigger: 'blur' }],
  guidePrice: [{ required: true, message: '请输入日租金', trigger: 'blur' }],
  deposit: [{ required: true, message: '请输入押金', trigger: 'blur' }]
}

function categoryType(code) {
  return CATEGORY_TYPE[code] || 'info'
}

/** 在库最低价与指导价不一致时提示，避免「车型写着 129、实际在租 199」 */
function priceGap(row) {
  if (!row.minDailyPrice || !row.guidePrice) return false
  return Number(row.minDailyPrice) !== Number(row.guidePrice)
}

async function loadBrands() {
  const res = await getBrandOptions()
  brands.value = res.data || []
}

async function load() {
  loading.value = true
  try {
    const res = await getModels({
      page: query.page,
      size: query.size,
      brandId: query.brandId,
      categoryCode: query.categoryCode,
      keyword: query.keyword.trim(),
      status: query.status
    })
    const data = res.data || {}
    rows.value = data.records || []
    total.value = Number(data.total || 0)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.brandId = null
  query.categoryCode = ''
  query.keyword = ''
  query.status = null
  onSearch()
}

function openCreate() {
  Object.assign(form, emptyForm())
  if (query.brandId) form.brandId = query.brandId
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    brandId: row.brandId,
    modelName: row.modelName,
    categoryCode: row.categoryCode,
    seats: row.seats,
    gearbox: row.gearbox,
    fuelType: row.fuelType,
    displacement: row.displacement || '',
    guidePrice: Number(row.guidePrice),
    deposit: Number(row.deposit),
    coverImage: row.coverImage || '',
    description: row.description || '',
    sortNo: row.sortNo,
    status: row.status
  })
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      brandId: form.brandId,
      modelName: form.modelName,
      categoryCode: form.categoryCode,
      seats: form.seats,
      gearbox: form.gearbox,
      fuelType: form.fuelType,
      displacement: form.displacement,
      guidePrice: form.guidePrice,
      deposit: form.deposit,
      coverImage: form.coverImage,
      description: form.description,
      sortNo: form.sortNo,
      status: form.status
    }
    const res = form.id ? await updateModel(form.id, payload) : await createModel(payload)
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      target === 0
        ? `停用后「${row.modelName}」旗下 ${row.vehicleCount || 0} 台车不再出现在前台，在租订单不受影响。`
        : `启用后「${row.modelName}」重新对外展示。`,
      target === 0 ? '停用车型' : '启用车型',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await updateModelStatus(row.id, target)
  ElMessage.success(res.message || '已更新')
  await load()
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除车型「${row.modelName}」？删除后不可恢复。`,
      '删除车型',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await deleteModel(row.id)
  ElMessage.success(res.message || '已删除')
  await load()
}

onMounted(async () => {
  await loadBrands()
  await load()
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
  max-width: 760px;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 18px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.count-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a8abb2;
}

.model-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cover {
  width: 52px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
  background: #f7f8fa;
}

.cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
}

.name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.account {
  font-size: 11px;
  color: #a8abb2;
  line-height: 1.6;
}

.account.warn {
  color: #e6a23c;
}

.money {
  font-size: 13px;
  font-weight: 600;
  color: #f56c6c;
}

.money-sub {
  font-size: 13px;
  color: #606266;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.inline-group {
  margin-left: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
