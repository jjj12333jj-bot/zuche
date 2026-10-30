<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>轮播图管理</h2>
        <p class="sub">
          轮播图只在<strong>该位置内按排序号升序</strong>展示，序号小的在前；不填序号时自动排到该位置末尾，不会插队。
          活动结束建议先<strong>隐藏</strong>而不是删除——下次换张图还能复用同一位置，省得重新调顺序。
          图片地址支持站内路径（如 <code>/images/banners/banner-1.svg</code>）与 http(s) 外链。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增轮播图</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">轮播图总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">显示中</div>
        <div class="stat-value success">{{ stats.showing }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已隐藏</div>
        <div class="stat-value muted-value">{{ stats.hidden }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="标题关键字"
          clearable
          style="width: 200px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.position" placeholder="全部位置" clearable style="width: 130px" @change="onSearch">
          <el-option label="首页" value="HOME" />
          <el-option label="列表页" value="PORTAL" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="onSearch">
          <el-option label="显示中" :value="1" />
          <el-option label="已隐藏" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 张</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="预览" width="112" align="center">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" fit="cover" class="thumb">
              <template #error>
                <div class="thumb-error">加载失败</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <div class="cell-main">{{ row.title }}</div>
            <div class="cell-sub mono">{{ row.imageUrl }}</div>
          </template>
        </el-table-column>
        <el-table-column label="位置" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.positionLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="70" align="center">
          <template #default="{ row }">{{ row.sortNo }}</template>
        </el-table-column>
        <el-table-column label="跳转地址" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.linkUrl" class="mono">{{ row.linkUrl }}</span>
            <span v-else class="muted">不可点击</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="176" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有轮播图" :image-size="90" />
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑轮播图' : '新增轮播图'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="如：信用越好 押金越低" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="/images/banners/banner-1.svg 或 https://…" maxlength="255" />
        </el-form-item>
        <el-form-item label="预览">
          <div class="preview-box">
            <el-image v-if="form.imageUrl" :src="form.imageUrl" fit="contain" class="preview-img">
              <template #error>
                <div class="thumb-error">图片无法加载，请检查地址</div>
              </template>
            </el-image>
            <span v-else class="muted">填入图片地址后这里会实时预览</span>
          </div>
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.linkUrl" placeholder="选填，如 /credit；留空表示点击无跳转" maxlength="255" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="展示位置">
            <el-select v-model="form.position" style="width: 100%">
              <el-option label="首页" value="HOME" />
              <el-option label="列表页" value="PORTAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序号">
            <el-input-number v-model="form.sortNo" :min="0" :max="999" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="1">显示</el-radio>
              <el-radio :value="0">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <div class="hint">排序号留空时自动排到该位置末尾；同一位置的轮播按序号从小到大展示。</div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createBanner,
  deleteBanner,
  getBannerStats,
  getBanners,
  updateBanner,
  updateBannerStatus
} from '@/api/systemAdmin'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})

const query = reactive({
  keyword: '',
  position: null,
  status: null,
  page: 1,
  size: 10
})

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  position: 'HOME',
  sortNo: null,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请填写标题', trigger: 'blur' }],
  imageUrl: [
    { required: true, message: '请填写图片地址', trigger: 'blur' },
    { pattern: /^(\/|https?:\/\/).+/, message: '以 / 或 http(s):// 开头', trigger: 'blur' }
  ]
}

async function load() {
  loading.value = true
  try {
    const res = await getBanners(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getBannerStats()
  stats.value = res.data || {}
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.position = null
  query.status = null
  query.page = 1
  load()
}

function reload() {
  load()
  loadStats()
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    title: '',
    imageUrl: '',
    linkUrl: '',
    position: 'HOME',
    sortNo: null,
    status: 1
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    imageUrl: row.imageUrl,
    linkUrl: row.linkUrl || '',
    position: row.position,
    sortNo: row.sortNo,
    status: row.status
  })
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const res = editing.value
      ? await updateBanner(editingId.value, { ...form })
      : await createBanner({ ...form })
    ElMessage.success(res.message || '保存成功')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 业务错误已由请求拦截器统一提示
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const show = row.status === 0
  try {
    await ElMessageBox.confirm(
      show
        ? `显示后「${row.title}」会出现在${row.positionLabel}的轮播位。`
        : `隐藏后「${row.title}」不再出现在${row.positionLabel}，数据保留，可随时恢复。`,
      show ? '显示轮播图' : '隐藏轮播图',
      { confirmButtonText: show ? '确认显示' : '确认隐藏', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateBannerStatus(row.id, { status: show ? 1 : 0 })
    ElMessage.success(res.message || '操作成功')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 取消或业务异常
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.title}」？\n如果只是这期活动结束了，建议改为「隐藏」，下次还能直接复用。`,
      '删除轮播图',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
    const res = await deleteBanner(row.id)
    ElMessage.success(res.message || '已删除')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 取消或业务异常
  }
}

onMounted(() => {
  load()
  loadStats()
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
  max-width: 920px;
}

.sub strong {
  color: #606266;
}

.sub code {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 12px;
  color: #606266;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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

.stat-value.success { color: #67c23a; }
.stat-value.muted-value { color: #909399; }

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

.thumb {
  width: 88px;
  height: 46px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.thumb-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #c0c4cc;
  background: #fafafa;
}

.cell-main {
  font-size: 13px;
  color: #303133;
}

.cell-sub {
  font-size: 11px;
  color: #a8abb2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.muted {
  color: #c0c4cc;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.preview-box {
  width: 100%;
  min-height: 84px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px;
}

.preview-img {
  max-width: 100%;
  max-height: 140px;
}

.hint {
  font-size: 11px;
  color: #a8abb2;
  margin-left: 88px;
  margin-top: -6px;
}
</style>
