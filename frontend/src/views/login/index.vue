<template>
  <div class="login-page">
    <div class="login-card">
      <aside class="brand">
        <div class="brand-logo">
          <el-icon :size="26"><Van /></el-icon>
          <span>微租车</span>
        </div>
        <h1>信用越好，押金越低</h1>
        <p class="brand-desc">
          平台依据按时归还率、取消率、逾期次数与完成订单数计算信用分，
          不同信用等级对应不同押金系数，优秀档押金低至 8 折。
        </p>
        <ul class="brand-points">
          <li><b>押金 8 折</b> 优秀档专享</li>
          <li><b>多网点</b> 支持异地还车与跨店调车</li>
          <li><b>全流程</b> 下单 · 支付 · 取还车 · 押金退还</li>
        </ul>
        <div class="brand-foot">毕业设计演示项目 · 支付与地图均为模拟实现</div>
      </aside>

      <section class="form-side">
        <h2>账号登录</h2>
        <p class="text-muted">请使用平台分配的账号登录，不同角色进入不同工作台</p>

        <el-alert
          v-if="switching"
          class="switch-tip"
          type="warning"
          :closable="false"
          show-icon
          title="正在为当前标签页登录新账号"
          description="原标签页的登录状态保持不变，两个标签页可以分别使用不同账号。"
        />
        <p v-else class="session-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>登录状态只作用于当前标签页：需要同时用多个账号时，再开一个标签页登录即可，互不覆盖。</span>
        </p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @keyup.enter="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable>
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            登 录
          </el-button>
        </el-form>

        <el-divider class="demo-divider">演示账号 · 密码 123456</el-divider>
        <div class="demo-accounts">
          <el-tag
            v-for="item in demoAccounts"
            :key="item.username"
            class="demo-tag"
            effect="plain"
            @click="fill(item.username)"
          >
            {{ item.username }} · {{ item.role }}
          </el-tag>
        </div>

        <div class="register-link">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
          <span class="text-muted">（注册默认 100 信用分 · 良好档）</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 从「新标签页登录其他账号」进来时，路由守卫已清掉继承来的身份，这里只做文案区分 */
const switching = computed(() => route.query.switch === '1')

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ]
}

const demoAccounts = [
  { username: 'admin', role: '系统管理员' },
  { username: 'store1', role: '网点管理员' },
  { username: 'user1', role: '普通用户' }
]

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function onSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await userStore.login({ ...form })
      ElMessage.success(`欢迎回来，${data.realName || data.username}`)
      // 按角色分流：S0 阶段只有前台与首页，后续补 /store /admin
      const target =
        data.roleCode === 'SYS_ADMIN'
          ? '/admin/dashboard'
          : data.roleCode === 'STORE_ADMIN'
            ? '/store/overview'
            : '/'
      router.push(route.query.redirect || target)
    } catch (e) {
      // 错误提示已在 request 拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef3fb 0%, #f7f9fc 55%, #eef7f2 100%);
  padding: 24px;
}

.login-card {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  width: 100%;
  max-width: 940px;
  min-height: 520px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(31, 45, 61, 0.1);
  overflow: hidden;
}

.brand {
  padding: 40px 36px;
  background: #1f3a5f;
  color: #fff;
  display: flex;
  flex-direction: column;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
}

.brand h1 {
  font-size: 28px;
  line-height: 1.35;
  margin: 32px 0 14px;
  font-weight: 600;
}

.brand-desc {
  font-size: 14px;
  line-height: 1.85;
  color: rgba(255, 255, 255, 0.78);
  margin: 0;
}

.brand-points {
  list-style: none;
  padding: 0;
  margin: 28px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}

.brand-points li {
  margin-bottom: 12px;
}

.brand-points b {
  color: #7fd4a8;
  margin-right: 8px;
}

.brand-foot {
  margin-top: auto;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.form-side {
  padding: 40px 40px 32px;
}

.form-side h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
}

.form-side p {
  margin: 0 0 22px;
  font-size: 13px;
}

/* 标签页会话提示：比正文弱一档，不抢「登录」按钮的注意力 */
.form-side .session-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: -8px 0 18px;
  padding: 9px 11px;
  border-radius: 6px;
  background: #f4f7fb;
  color: #6b7a90;
  font-size: 12px;
  line-height: 1.65;
}

.form-side .session-hint .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
  color: #7f93ad;
}

.switch-tip {
  margin: -6px 0 16px;
}

.switch-tip :deep(.el-alert__title) {
  font-size: 13px;
}

.switch-tip :deep(.el-alert__description) {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-divider :deep(.el-divider__text) {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.demo-tag {
  cursor: pointer;
}

.register-link {
  margin-top: 18px;
  font-size: 13px;
  color: #606266;
}

.register-link a {
  color: #409eff;
  font-weight: 600;
  margin: 0 4px;
}

.register-link .text-muted {
  font-size: 12px;
  margin-left: 4px;
}

@media (max-width: 860px) {
  .login-card {
    grid-template-columns: 1fr;
  }
  .brand {
    display: none;
  }
}
</style>
