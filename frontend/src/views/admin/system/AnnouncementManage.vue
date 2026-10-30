<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h2>公告管理</h2>
        <p class="sub">
          公告走<strong>草稿 → 发布</strong>两态：写完先存草稿，确认无误再点发布，避免一句话没敲完就推给全站用户。
          发布时记录发布时间，前台公告栏按时间倒序排列，<strong>重新发布即刷新时间、相当于置顶</strong>；
          下架不清空原发布时间，方便复盘「这条挂了多久」。
        </p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="reload" />
        <el-button type="primary" :icon="Plus" @click="openCreate">新增公告</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">公告总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">已发布</div>
        <div class="stat-value success">{{ stats.published }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">草稿</div>
        <div class="stat-value warn">{{ stats.draft }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">累计浏览</div>
        <div class="stat-value">{{ stats.viewTotal }}</div>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="标题 / 正文关键字"
          clearable
          style="width: 230px"
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 120px" @change="onSearch">
          <el-option label="公告" value="NOTICE" />
          <el-option label="活动" value="ACTIVITY" />
          <el-option label="规则" value="RULE" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px" @change="onSearch">
          <el-option label="已发布" :value="1" />
          <el-option label="草稿" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <span class="count-hint">共 {{ total }} 条</span>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border>
        <el-table-column label="标题" min-width="280">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag size="small" :type="typeTag(row.type)" effect="plain">{{ row.typeLabel }}</el-tag>
              <span class="title-text" @click="openPreview(row)">{{ row.title }}</span>
              <el-tag v-if="row.status === 0" size="small" type="info" effect="light">草稿</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="80" align="right">
          <template #default="{ row }">{{ row.viewCount }}</template>
        </el-table-column>
        <el-table-column label="发布人" width="100">
          <template #default="{ row }">{{ row.publisherName || '—' }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="150">
          <template #default="{ row }">
            <span :class="{ muted: !row.publishTime }">{{ formatDateTime(row.publishTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有公告，点右上角写一条" :image-size="90" />
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑公告' : '新增公告'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="一句话说清这条公告在讲什么" maxlength="100" show-word-limit />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option label="公告" value="NOTICE" />
              <el-option label="活动" value="ACTIVITY" />
              <el-option label="规则" value="RULE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="0">存草稿</el-radio>
              <el-radio :value="1">立即发布</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="5000"
            show-word-limit
            placeholder="写清楚「什么时候、对谁、有什么影响」。正文会原样展示给用户，不支持 Markdown。"
          />
        </el-form-item>
        <div class="hint">
          已发布公告再次编辑不会改变发布时间；从草稿改为「立即发布」才会写入发布时间。
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="previewVisible" title="前台预览" width="620px">
      <div v-if="preview" class="preview">
        <div class="preview-head">
          <el-tag size="small" :type="typeTag(preview.type)" effect="plain">{{ preview.typeLabel }}</el-tag>
          <h3>{{ preview.title }}</h3>
        </div>
        <div class="preview-meta">
          {{ formatDateTime(preview.publishTime === null ? preview.createTime : preview.publishTime) }}
          · 浏览 {{ preview.viewCount }}
          <el-tag v-if="preview.status === 0" size="small" type="info" effect="light" style="margin-left: 8px">
            草稿（前台不可见）
          </el-tag>
        </div>
        <div class="preview-body">{{ preview.content }}</div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" plain @click="editFromPreview">编辑这条</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createAnnouncement,
  deleteAnnouncement,
  getAnnouncementDetail,
  getAnnouncementStats,
  getAnnouncements,
  updateAnnouncement,
  updateAnnouncementStatus
} from '@/api/systemAdmin'
import { formatDateTime } from '@/utils/format'

const TYPE_TAG = { NOTICE: 'primary', ACTIVITY: 'success', RULE: 'warning' }

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})

const query = reactive({
  keyword: '',
  type: null,
  status: null,
  page: 1,
  size: 10
})

const dialogVisible = ref(false)
const saving = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const previewVisible = ref(false)
const preview = ref(null)

const form = reactive({
  title: '',
  content: '',
  type: 'NOTICE',
  status: 0
})

const rules = {
  title: [{ required: true, message: '请填写公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请填写公告正文', trigger: 'blur' }]
}

function typeTag(type) {
  return TYPE_TAG[type] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getAnnouncements(query)
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await getAnnouncementStats()
  stats.value = res.data || {}
}

function onSearch() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.type = null
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
  Object.assign(form, { title: '', content: '', type: 'NOTICE', status: 0 })
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = true
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    content: row.content || '',
    type: row.type,
    status: row.status
  })
  // 列表接口不返回正文，编辑时单独取一次详情
  if (!row.content) {
    getAnnouncementDetail(row.id).then((res) => {
      form.content = res.data?.content || ''
    })
  }
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    const res = editing.value
      ? await updateAnnouncement(editingId.value, payload)
      : await createAnnouncement(payload)
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
  const publish = row.status === 0
  try {
    await ElMessageBox.confirm(
      publish
        ? `发布后「${row.title}」会立即出现在前台公告栏，并记录发布时间。`
        : `下架后「${row.title}」从前台消失，但不会删除，可随时重新发布。\n重新发布会把发布时间刷新为当时，相当于重新置顶。`,
      publish ? '发布公告' : '下架公告',
      { confirmButtonText: publish ? '确认发布' : '确认下架', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateAnnouncementStatus(row.id, { status: publish ? 1 : 0 })
    ElMessage.success(res.message || '操作成功')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 取消或业务异常，拦截器已提示
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `删除「${row.title}」？\n删除为逻辑删除，历史浏览量仍保留在库中，但后台不再展示。`,
      '删除公告',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'error' }
    )
    const res = await deleteAnnouncement(row.id)
    ElMessage.success(res.message || '已删除')
    await Promise.all([load(), loadStats()])
  } catch (e) {
    // 取消或业务异常
  }
}

async function openPreview(row) {
  const res = await getAnnouncementDetail(row.id)
  preview.value = res.data
  previewVisible.value = true
}

function editFromPreview() {
  const row = preview.value
  previewVisible.value = false
  openEdit(row)
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

.stat-value.success { color: #67c23a; }
.stat-value.warn { color: #e6a23c; }

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

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-text:hover {
  color: #409eff;
}

.muted {
  color: #c0c4cc;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.hint {
  font-size: 11px;
  color: #a8abb2;
  margin-left: 80px;
  margin-top: 8px;
  padding-bottom: 4px;
}

.preview-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-head h3 {
  margin: 0;
  font-size: 17px;
  color: #1f2937;
}

.preview-meta {
  margin: 8px 0 14px;
  font-size: 12px;
  color: #a8abb2;
}

.preview-body {
  font-size: 14px;
  line-height: 1.9;
  color: #4b5563;
  white-space: pre-wrap;
  background: #fafafa;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  padding: 16px;
  max-height: 380px;
  overflow: auto;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
