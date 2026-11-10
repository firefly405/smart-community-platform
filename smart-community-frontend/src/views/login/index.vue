<template>
  <div class="login-container" :class="{ 'community-login': communityLogin, 'tenant-login': tenantLogin }">
    <div class="login-box">
      <div class="login-header">
        <h1>{{ portalTitle }}</h1>
        <p>{{ portalSubtitle }}</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <!-- 账号密码登录 -->
        <el-tab-pane label="账号登录" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" class="login-form" @submit.prevent>
            <el-form-item prop="username">
              <el-input v-model="passwordForm.username" placeholder="请输入用户名/手机号" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="passwordForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" size="large" show-password @keyup.enter="handlePasswordLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" class="login-button" @click="handlePasswordLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 短信验证码登录 -->
        <el-tab-pane label="短信登录" name="sms">
          <el-form ref="smsFormRef" :model="smsForm" :rules="smsRules" class="login-form" @submit.prevent>
            <el-form-item prop="phone">
              <el-input v-model="smsForm.phone" placeholder="请输入手机号" prefix-icon="Phone" size="large" />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input v-model="smsForm.code" placeholder="请输入验证码" prefix-icon="Key" size="large" maxlength="6" />
                <el-button :disabled="codeCountdown > 0" size="large" class="code-btn" @click="handleSendCode">
                  {{ codeCountdown > 0 ? `${codeCountdown}s后重发` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" class="login-button" @click="handleSmsLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <router-link v-if="!communityLogin" to="/community/login">业主登录</router-link>
        <router-link v-if="!tenantLogin" to="/tenant/login">租户登录</router-link>
        <router-link v-if="communityLogin || tenantLogin" to="/login">物业管理登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { login as loginApi, loginBySms as loginBySmsApi, sendCode as sendCodeApi } from '@/api/auth'
import type { LoginResponse } from '@/api/auth'
import { getTenantSummary } from '@/api/tenant'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const route = useRoute()
const communityLogin = computed(() => route.meta.communityLogin === true)
const tenantLogin = computed(() => route.meta.tenantLogin === true)
const portalTitle = computed(() => communityLogin.value || tenantLogin.value ? '智慧社区' : '智慧社区综合管理平台')
const portalSubtitle = computed(() => tenantLogin.value ? '租户服务中心' : communityLogin.value ? '业主服务中心' : '物业管理工作台')
const authStore = useAuthStore()
const loading = ref(false)
const activeTab = ref('password')
let unmounted = false

onBeforeUnmount(() => { unmounted = true; if (countdownTimer) clearInterval(countdownTimer) })

async function submitLogin(form: FormInstance | undefined, signIn: () => Promise<LoginResponse>) {
  if (!form || loading.value) return
  loading.value = true
  const loginPath = route.fullPath
  try {
    if (!await form.validate().catch(() => false)) return
    const data = await signIn()
    // Do not let a delayed login response override a newer navigation.
    if (unmounted || route.fullPath !== loginPath) return
    if (communityLogin.value && data.userInfo.userType !== 1) {
      ElMessage.warning('此入口仅供普通业主使用，请使用物业管理登录')
      return
    }
    if (tenantLogin.value && data.userInfo.userType !== 3) {
      ElMessage.warning('此入口仅供社区租户使用，请选择对应登录入口')
      return
    }
    if (!communityLogin.value && !tenantLogin.value && ![4, 5].includes(data.userInfo.userType)) {
      ElMessage.warning('此入口仅供物业人员和管理员使用')
      return
    }
    if (![1, 3, 4, 5].includes(data.userInfo.userType)) {
      ElMessage.warning('当前账号尚未开通此平台权限，请联系物业')
      return
    }
    authStore.setToken(data.token)
    if (data.userInfo.userType === 3) {
      try {
        const summary = await getTenantSummary()
        if (!summary.bound) {
          authStore.clearAuth()
          ElMessage.warning('当前账号没有有效租约，请联系物业核实')
          return
        }
      } catch {
        authStore.clearAuth()
        return
      }
    }
    authStore.setUserInfo(data.userInfo)
    ElMessage.success('登录成功')

    const redirect = route.query.redirect
    const userType = data.userInfo.userType
    let target = userType === 1 ? '/community/home' : userType === 3 ? '/tenant/home' : '/dashboard'
    if (typeof redirect === 'string' && redirect.startsWith('/') &&
        !redirect.startsWith('//') && !redirect.includes('\\')) {
      const resolved = router.resolve(redirect)
      const correctPortal = userType === 1
        ? resolved.path.startsWith('/community/')
        : userType === 3
          ? resolved.path.startsWith('/tenant/')
          : !resolved.path.startsWith('/community/') && !resolved.path.startsWith('/tenant/')
      if (resolved.matched.length && !['Login', 'CommunityLogin', 'TenantLogin', 'Setup', 'NotFound'].includes(String(resolved.name)) && correctPortal) {
        target = resolved.fullPath
      }
    }
    await router.replace(target)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

// ===== 密码登录 =====
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  username: '',
  password: ''
})
const passwordRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
watch([communityLogin, tenantLogin], () => {
  passwordForm.username = ''
  passwordForm.password = ''
})

const handlePasswordLogin = () => submitLogin(passwordFormRef.value, () => loginApi(passwordForm))

// ===== 短信登录 =====
const smsFormRef = ref<FormInstance>()
const smsForm = reactive({
  phone: '',
  code: ''
})
const smsRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const handleSendCode = async () => {
  if (!smsForm.phone || !/^1[3-9]\d{9}$/.test(smsForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  try {
    await sendCodeApi(smsForm.phone)
    ElMessage.success('验证码发送成功，请查看控制台日志')
    codeCountdown.value = 60
    countdownTimer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) {
        if (countdownTimer) clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // 错误已在拦截器处理
  }
}

const handleSmsLogin = () => submitLogin(smsFormRef.value, () => loginBySmsApi(smsForm))
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.community-login {
  --el-color-primary: #176b5b;
  background: #edf3f1;
  .login-box { border-radius: 8px; border-top: 4px solid #176b5b; box-shadow: 0 12px 36px #153b2512; }
  h1 { color: #176b5b; font-size: 30px; }
}
.tenant-login {
  --el-color-primary: #236979;
  background: #eef3f2;
  .login-box { border: 1px solid #dce6e4; border-top: 4px solid #236979; border-radius: 8px; box-shadow: 0 12px 36px rgb(35 74 80 / 10%); }
  h1 { color: #1f5965; font-size: 30px; }
  :deep(.el-tabs__active-bar) { background-color: #236979; }
  :deep(.el-tabs__item.is-active), :deep(.el-tabs__item:hover) { color: #236979; }
  .login-button { --el-button-bg-color: #236979; --el-button-border-color: #236979; --el-button-hover-bg-color: #2f7b89; --el-button-hover-border-color: #2f7b89; }
}
.login-box {
  width: 440px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 20px;

  h1 {
    font-size: 24px;
    color: #333;
    margin-bottom: 8px;
  }

  p {
    font-size: 12px;
    color: #999;
  }
}

.login-tabs {
  :deep(.el-tabs__nav) {
    width: 100%;
    display: flex;
  }
  :deep(.el-tabs__item) {
    flex: 1;
    text-align: center;
    font-size: 15px;
  }
}

.login-form {
  margin-top: 10px;

  .login-button {
    width: 100%;
  }
}

.code-row {
  display: flex;
  gap: 10px;

  .el-input {
    flex: 1;
  }

  .code-btn {
    width: 130px;
    white-space: nowrap;
    padding: 0 8px;
  }
}

.login-footer {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #999;
}
.login-footer a { color: var(--el-color-primary); text-decoration: none; }

@media (max-width: 520px) {
  .login-box { width: calc(100% - 32px); padding: 28px 24px; }
}
</style>
