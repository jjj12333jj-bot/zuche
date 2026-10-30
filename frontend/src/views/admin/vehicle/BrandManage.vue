<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>品牌管理</h2>
        <p class="sub">
          品牌是车型的上级，车型是车辆的上级。停用品牌只是让前台不再展示，
          在租订单不受影响；删除会被拦下来——只要品牌下还挂着车型。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="load" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增品牌</el-button>
      </div>
    </div>

    <!-- 概览：三个数字就够，不做图表 -->
    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">品牌总数</div>
        <div class="stat-value">{{ summary.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">启用中</div>
        <div class="stat-value ok">{{ summary.enabled }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已停用</div>
        <div class="stat-value muted">{{ summary.disabled }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">在册车辆</div>
        <div class="stat-value">{{ summary.vehicles }} 台</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 130px"
          @change="onSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="品牌名 / 首字母"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 个品牌</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="品牌" min-width="180">
          <template #default="{ row }">
            <div class="brand-cell">
              <img v-if="row.brandLogo" :src="row.brandLogo" class="logo" alt="" />
              <div v-else class="logo logo-empty">{{ row.initial || '?' }}</div>
              <div>
                <div class="name">{{ row.brandName }}</div>
                <div class="account">首字母 {{ row.initial || '—' }} · 排序 {{ row.sortNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="车型数" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.modelCount ? 'num' : 'num zero'">{{ row.modelCount || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="车辆资产" width="150">
          <template #default="{ row }">
            <div class="name">共 {{ row.vehicleCount || 0 }} 台</div>
            <div class="account">其中可租 {{ row.rentableCount || 0 }} 台</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="plain">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              :disabled="row.modelCount > 0"
              :title="row.modelCount > 0 ? '品牌下还有车型，不能删除' : ''"
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑品牌' : '新增品牌'" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="品牌名" prop="brandName">
          <el-input v-model="form.brandName" maxlength="30" placeholder="例如 大众" />
        </el-form-item>
        <el-form-item label="首字母" prop="initial">
          <el-input v-model="form.initial" maxlength="4" placeholder="前台品牌索引用，例如 D" />
        </el-form-item>
        <el-form-item label="Logo 地址" prop="brandLogo">
          <el-input v-model="form.brandLogo" placeholder="/images/brands/brand-1.svg" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortNo">
          <el-input-number v-model="form.sortNo" :min="0" :max="9999" controls-position="right" />
          <span class="form-hint">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import {
  createBrand,
  deleteBrand,
  getBrands,
  getVehicleAssetStats,
  updateBrand,
  updateBrandStatus
} from '@/api/adminVehicle'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

const form = reactive({
  id: null,
  brandName: '',
  initial: '',
  brandLogo: '',
  sortNo: 99,
  status: 1
})

const rules = {
  brandName: [{ required: true, message: '请输入品牌名', trigger: 'blur' }]
}

/** 概览走全量统计接口，不能用当前页 10 条去推——翻页数字会变 */
const summary = ref({ total: 0, enabled: 0, disabled: 0, vehicles: 0 })

async function loadStats() {
  const res = await getVehicleAssetStats()
  const b = res.data?.brand || {}
  summary.value = {
    total: b.total || 0,
    enabled: b.enabled || 0,
    disabled: b.disabled || 0,
    vehicles: b.vehicles || 0
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getBrands({
      page: query.page,
      size: query.size,
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
  query.keyword = ''
  query.status = null
  onSearch()
}

function openCreate() {
  Object.assign(form, { id: null, brandName: '', initial: '', brandLogo: '', sortNo: 99, status: 1 })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    brandName: row.brandName,
    initial: row.initial,
    brandLogo: row.brandLogo,
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
      brandName: form.brandName,
      initial: form.initial,
      brandLogo: form.brandLogo,
      sortNo: form.sortNo,
      status: form.status
    }
    const res = form.id ? await updateBrand(form.id, payload) : await createBrand(payload)
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const target = row.status === 1 ? 0 : 1
  const tip =
    target === 0
      ? `停用后「${row.brandName}」的 ${row.vehicleCount || 0} 台车不再出现在前台车辆列表，在租订单不受影响。`
      : `启用后「${row.brandName}」重新对外展示。`
  try {
    await ElMessageBox.confirm(tip, target === 0 ? '停用品牌' : '启用品牌', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }

  const res = await updateBrandStatus(row.id, target)
  ElMessage.success(res.message || '已更新')
  await Promise.all([load(), loadStats()])
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除品牌「${row.brandName}」？删除后不可恢复。`,
      '删除品牌',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const res = await deleteBrand(row.id)
  ElMessage.success(res.message || '已删除')
  await Promise.all([load(), loadStats()])
}

onMounted(async () => {
  await loadStats()
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
  max-width: 720px;
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
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.stat-value.ok {
  color: #67c23a;
}

.stat-value.muted {
  color: #c0c4cc;
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
}

.count-hint {
  margin-left: auto;
  font-size: 12px;
  color: #a8abb2;
}

.brand-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  object-fit: contain;
  background: #f7f8fa;
}

.logo-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #a8abb2;
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

.num {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.num.zero {
  color: #c0c4cc;
  font-weight: 400;
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #a8abb2;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
