<template>
  <div class="profile-page">
    <div class="page-container">
      <div class="page-head">
        <div>
          <h1>个人中心</h1>
          <p class="sub">维护个人资料与登录密码 · 用户名与角色由系统管理，不在此修改</p>
        </div>
        <el-button plain @click="router.push('/credit')">查看信用中心</el-button>
      </div>

      <div class="grid">
        <el-card shadow="never" class="card">
          <template #header>
            <div class="card-head">
              <span class="card-title">基本资料</span>
              <span class="card-tip">手机号同时用于账号找回，请填真实号码</span>
            </div>
          </template>

          <el-form ref="profileRef" :model="profile" :rules="profileRules" label-width="88px">
            <el-form-item label="登录账号">
              <el-input :model-value="userStore.userInfo?.username" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="profile.realName" maxlength="50" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profile.phone" maxlength="11" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profile.email" maxlength="100" placeholder="选填，用于接收通知" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="onSaveProfile">保存资料</el-button>
              <el-button @click="resetProfile">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="card">
          <template #header>
            <div class="card-head">
              <span class="card-title">修改密码</span>
              <span class="card-tip">修改成功后需要重新登录</span>
            </div>
          </template>

          <el-form ref="pwdRef" :model="pwd" :rules="pwdRules" label-width="88px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwd.oldPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="pwd.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changing" @click="onChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateProfile, changePassword } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const profileRef = ref()
const pwdRef = ref()
const saving = ref(false)
const changing = ref(false)

const profile = reactive({ realName: '', phone: '', email: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const profileRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需在 6-32 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwd.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function fillProfile(data) {
  profile.realName = data?.realName || ''
  profile.phone = data?.phone || ''
  profile.email = data?.email || ''
}

function resetProfile() {
  fillProfile(userStore.userInfo)
}

async function onSaveProfile() {
  await profileRef.value.validate()
  saving.value = true
  try {
    const res = await updateProfile({ ...profile })
    userStore.userInfo = { ...userStore.userInfo, ...res.data }
    fillProfile(userStore.userInfo)
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function onChangePassword() {
  await pwdRef.value.validate()
  changing.value = true
  try {
    await changePassword({ ...pwd })
    ElMessage.success('密码已修改，请重新登录')
    userStore.logout()
    router.push('/login')
  } finally {
    changing.value = false
  }
}

onMounted(async () => {
  fillProfile(userStore.userInfo)
  // 手机号/邮箱不在登录返回的历史缓存里，进页面拉一次最新资料
  const res = await getCurrentUser()
  userStore.userInfo = { ...userStore.userInfo, ...res.data }
  fillProfile(userStore.userInfo)
})
</script>

<style scoped>
.profile-page {
  padding: 28px 0 56px;
  background: #f7f4f0;
  min-height: 60vh;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-head h1 {
  font-size: 26px;
  color: #2b221b;
  margin: 0;
}

.page-head .sub {
  margin: 6px 0 0;
  color: #8a7b6b;
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

.card {
  border-radius: 8px;
  border-color: #e8dfd4;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2b221b;
}

.card-tip {
  font-size: 12px;
  color: #8a7b6b;
}
</style>
