<template>
  <div class="login-page">
    <!-- 全屏实拍背景：公路 + 车 + 夕阳，纯摄影作品（Pexels），做轻微推拉动效 -->
    <div class="bg" aria-hidden="true"></div>
    <div class="veil" aria-hidden="true"></div>

    <header class="topbar">
      <div class="brand">
        <BrandMark :size="36" />
        <span class="brand-name">微租车</span>
      </div>
      <div class="topbar-meta">泉州南安店 · 泉州晋江店</div>
    </header>

    <main class="stage">
      <!-- 背景上的品牌文案：如需彻底隐藏，给 .hero 加 v-if="false" 即可 -->
      <section class="hero">
        <p class="hero-eyebrow">信用分驱动 · 动态押金</p>
        <h1>信用越好<br />押金越低</h1>
        <p class="hero-desc">
          平台依据按时归还率、取消率、逾期次数与完成订单数计算信用分，等级越高押金越低，优秀档低至 8 折。
        </p>
      </section>

      <!-- 半透明毛玻璃卡片：只保留表单，背景透出来但不影响阅读 -->
      <section class="card">
        <h2>账号登录</h2>

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

        <el-divider class="demo-divider">体验账号 · 密码 123456</el-divider>
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
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import BrandMark from '@/components/BrandMark.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

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
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #fff;
}

/* ---------- 背景 ---------- */
.bg {
  position: fixed;
  inset: 0;
  background: url('/images/login/bg.jpg') center 58% / cover no-repeat;
  transform: scale(1.06);
  animation: drift 28s ease-in-out infinite alternate;
  will-change: transform;
}

@keyframes drift {
  from {
    transform: scale(1.06) translate3d(0, 0, 0);
  }
  to {
    transform: scale(1.13) translate3d(-1.5%, -1%, 0);
  }
}

/* 左侧压暗保证文案可读，右侧留亮让卡片浮起来 */
.veil {
  position: fixed;
  inset: 0;
  background:
    linear-gradient(
      100deg,
      rgba(6, 15, 28, 0.82) 0%,
      rgba(6, 15, 28, 0.6) 30%,
      rgba(6, 15, 28, 0.24) 56%,
      rgba(6, 15, 28, 0.48) 100%
    ),
    radial-gradient(120% 85% at 76% 12%, rgba(255, 168, 64, 0.18), transparent 62%);
}

/* ---------- 顶栏 ---------- */
.topbar {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 46px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

.topbar-meta {
  font-size: 13px;
  letter-spacing: 0.6px;
  color: rgba(255, 255, 255, 0.7);
}

/* ---------- 主区 ---------- */
.stage {
  position: relative;
  z-index: 2;
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 8px 46px 56px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: center;
  gap: 56px;
}

.hero-eyebrow {
  margin: 0 0 14px;
  font-size: 12px;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.62);
  text-transform: uppercase;
}

.hero h1 {
  margin: 0 0 20px;
  font-size: clamp(34px, 3.9vw, 52px);
  line-height: 1.24;
  font-weight: 700;
  letter-spacing: 2px;
  text-shadow: 0 8px 34px rgba(0, 0, 0, 0.5);
}

.hero-desc {
  margin: 0;
  max-width: 460px;
  font-size: 14px;
  line-height: 2.05;
  color: rgba(255, 255, 255, 0.78);
  text-shadow: 0 2px 14px rgba(0, 0, 0, 0.45);
  text-wrap: pretty;
}

/* ---------- 毛玻璃卡片 ---------- */
.card {
  width: 100%;
  padding: 36px 32px 30px;
  border-radius: 20px;
  background: linear-gradient(158deg, rgba(255, 255, 255, 0.87) 0%, rgba(255, 255, 255, 0.71) 100%);
  backdrop-filter: blur(16px) saturate(170%);
  -webkit-backdrop-filter: blur(16px) saturate(170%);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow:
    0 30px 80px rgba(3, 10, 22, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  color: #1f2d3d;
  animation: rise 0.65s cubic-bezier(0.2, 0.8, 0.25, 1) both;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.card h2 {
  margin: 0 0 26px;
  font-size: 22px;
  font-weight: 650;
  letter-spacing: 0.5px;
  color: #16233a;
}

.card :deep(.el-form-item) {
  margin-bottom: 20px;
}

.card :deep(.el-form-item__label) {
  font-size: 13px;
  padding-bottom: 6px;
  color: #45536a;
}

.card :deep(.el-input__wrapper) {
  padding: 4px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 1px rgba(31, 58, 95, 0.1) inset;
  transition: box-shadow 0.18s ease;
}

.card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(47, 124, 246, 0.4) inset;
}

.card :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #2f7cf6 inset,
    0 0 0 3px rgba(47, 124, 246, 0.14);
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 2px;
  font-size: 15px;
  letter-spacing: 6px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #1f6fe0 0%, #3d9bff 100%);
  box-shadow: 0 14px 26px rgba(31, 111, 224, 0.32);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    filter 0.18s ease;
}

.submit-btn:hover {
  filter: brightness(1.07);
  transform: translateY(-1px);
  box-shadow: 0 18px 32px rgba(31, 111, 224, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

.demo-divider {
  margin: 26px 0 18px;
}

.demo-divider :deep(.el-divider__text) {
  padding: 0 10px;
  font-size: 12px;
  color: #7b8798;
  background: transparent;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* Element Plus 的 plain 标签是三重类选择器，这里拉高优先级才能压住默认色 */
.card :deep(.el-tag.demo-tag) {
  cursor: pointer;
  color: #2f6fd0;
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(47, 124, 246, 0.3);
  transition: all 0.18s ease;
}

.card :deep(.el-tag.demo-tag:hover) {
  color: #1f6fe0;
  background: #fff;
  border-color: #2f7cf6;
  transform: translateY(-1px);
}

.register-link {
  margin-top: 20px;
  font-size: 13px;
  color: #5b6b80;
}

.register-link a {
  color: #1f6fe0;
  font-weight: 600;
  margin: 0 4px;
}

.register-link .text-muted {
  font-size: 12px;
  margin-left: 4px;
  color: #8a97a8;
}

/* ---------- 适配 ---------- */
@media (max-width: 980px) {
  .stage {
    grid-template-columns: 1fr;
    justify-items: center;
    gap: 24px;
    padding: 8px 24px 40px;
  }

  .hero {
    display: none;
  }

  .card {
    max-width: 420px;
  }

  .topbar {
    padding: 20px 24px;
  }

  .topbar-meta {
    display: none;
  }
}

@media (max-width: 640px) {
  .bg {
    background-image: url('/images/login/bg-sm.jpg');
  }
}

@media (max-width: 480px) {
  .card {
    padding: 26px 20px 22px;
    border-radius: 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .bg,
  .card {
    animation: none;
  }
}
</style>
