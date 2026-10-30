<template>
  <div class="register-page">
    <div class="register-card">
      <aside class="brand">
        <div class="brand-logo">
          <el-icon :size="26"><Van /></el-icon>
          <span>微租车</span>
        </div>
        <h1>注册即得 100 信用分</h1>
        <p class="brand-desc">
          新用户默认为「良好」信用档。之后每一次按时归还、订单完成都会加分，
          逾期与支付后取消会扣分，信用分越高，租车押金越低。
        </p>
        <ul class="brand-points">
          <li><b>100 分</b> 注册初始信用分</li>
          <li><b>4 档</b> 优秀 / 良好 / 一般 / 待观察</li>
          <li><b>可追溯</b> 每笔信用变动都有日志与申诉入口</li>
        </ul>
        <div class="brand-foot">毕业设计演示项目 · 支付与地图均为模拟实现</div>
      </aside>

      <section class="form-side">
        <h2>创建账号</h2>
        <p class="text-muted">注册后自动登录，可立即浏览车辆</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="default"
          @keyup.enter="onSubmit"
        >
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" placeholder="字母开头，4-20 位" clearable>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="form.realName" placeholder="用于取车核验" clearable>
                  <template #prefix><el-icon><Postcard /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" clearable>
                  <template #prefix><el-icon><Iphone /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="邮箱（选填）" prop="email">
                <el-input v-model="form.email" placeholder="example@mail.com" clearable>
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="密码" prop="password">
                <el-input v-model="form.password" type="password" placeholder="6-32 位" show-password>
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  placeholder="再次输入密码"
                  show-password
                >
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item prop="agree">
            <el-checkbox v-model="form.agree">
              我已阅读并同意《租赁服务协议》与《信用风控规则说明》（演示）
            </el-checkbox>
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">
            注 册
          </el-button>
        </el-form>

        <div class="login-link">
          已有账号？
          <router-link to="/login">返回登录</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const validateConfirm = (rule, value, callback) => {
  if (!value) return callback(new Error('请再次输入密码'))
  if (value !== form.password) return callback(new Error('两次输入的密码不一致'))
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/,
      message: '字母开头，4-20 位字母/数字/下划线',
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '姓名过长', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
  agree: [
    {
      validator: (rule, value, callback) =>
        value ? callback() : callback(new Error('请先同意服务协议')),
      trigger: 'change'
    }
  ]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data = await userStore.register({
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      email: form.email || null,
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success(`注册成功，${data.realName}，初始信用分 ${data.creditScore}`)
    router.push('/')
  } catch (e) {
    // 统一错误提示由 request 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef3fb 0%, #f7f9fc 55%, #eef7f2 100%);
  padding: 28px;
}

.register-card {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  width: 100%;
  max-width: 1000px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(31, 45, 61, 0.1);
  overflow: hidden;
}

.brand {
  padding: 42px 34px;
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
  font-size: 26px;
  line-height: 1.4;
  margin: 32px 0 14px;
  font-weight: 600;
}

.brand-desc {
  font-size: 14px;
  line-height: 1.9;
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
  padding: 38px 40px 30px;
}

.form-side h2 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
}

.form-side p {
  margin: 0 0 20px;
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  margin-top: 4px;
}

.login-link {
  margin-top: 16px;
  font-size: 13px;
  color: #606266;
}

.login-link a {
  color: #409eff;
  font-weight: 600;
  margin-left: 4px;
}

@media (max-width: 900px) {
  .register-card {
    grid-template-columns: 1fr;
  }
  .brand {
    display: none;
  }
  .form-side {
    padding: 28px 22px;
  }
}
</style>
