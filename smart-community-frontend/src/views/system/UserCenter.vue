<template>
  <div class="page-container user-center">
    <SystemPageHeader>
      <template #meta><span>资料完整度 <strong>{{ profileCompletion }}%</strong></span></template>
    </SystemPageHeader>

    <div class="profile-layout" v-loading="loading">
      <aside class="identity-panel">
        <el-upload
          class="avatar-upload"
          accept="image/jpeg,image/png,image/webp"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="handleAvatarUpload"
        >
          <div class="avatar-frame" aria-label="更换头像">
            <el-avatar :size="104" :src="userForm.avatar">
              <span class="avatar-fallback">{{ profileInitial }}</span>
            </el-avatar>
            <span class="avatar-action"><el-icon><Camera /></el-icon></span>
          </div>
          <el-button class="avatar-button" type="primary" plain :loading="avatarUploading">
            <el-icon><Camera /></el-icon><span>更换头像</span>
          </el-button>
        </el-upload>

        <h2>{{ userForm.realName || userForm.username || '未设置姓名' }}</h2>
        <p class="username">@{{ userForm.username || '--' }}</p>
        <div class="identity-tags">
          <el-tag type="success" effect="plain">{{ userForm.status === 0 ? '已停用' : '账号正常' }}</el-tag>
          <el-tag effect="plain">{{ userTypeLabel }}</el-tag>
        </div>

        <dl class="identity-details">
          <div><dt><el-icon><Phone /></el-icon>手机号</dt><dd>{{ maskedPhone }}</dd></div>
          <div><dt><el-icon><Message /></el-icon>邮箱</dt><dd>{{ userForm.email || '未设置' }}</dd></div>
          <div><dt><el-icon><Calendar /></el-icon>最近登录</dt><dd>{{ formatDate(userForm.loginTime) }}</dd></div>
        </dl>
      </aside>

      <main class="profile-main">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-heading">
              <span class="heading-icon profile-icon"><el-icon><UserFilled /></el-icon></span>
              <div><h2>基本资料</h2><p>维护用于社区业务联系和身份展示的信息</p></div>
            </div>
          </template>

          <el-form ref="infoFormRef" :model="userForm" :rules="infoRules" label-position="top" size="large" @submit.prevent>
            <div class="form-grid">
              <el-form-item label="登录账号">
                <el-input v-model="userForm.username" disabled>
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="userForm.realName" maxlength="50" placeholder="请输入姓名" clearable />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userForm.phone" maxlength="11" placeholder="请输入手机号" clearable />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userForm.email" maxlength="100" placeholder="请输入邮箱" clearable />
              </el-form-item>
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="userForm.gender" class="gender-options">
                  <el-radio-button :value="0">未设置</el-radio-button>
                  <el-radio-button :value="1">男</el-radio-button>
                  <el-radio-button :value="2">女</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="生日" prop="birthday">
                <el-date-picker v-model="userForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择生日" :disabled-date="disableFutureDate" />
              </el-form-item>
            </div>
            <div class="form-actions">
              <el-button :disabled="!profileChanged" @click="resetProfile">撤销修改</el-button>
              <el-button type="primary" :loading="saving" :disabled="!profileChanged" @click="handleSave">保存资料</el-button>
            </div>
          </el-form>
        </el-card>

        <el-card id="password" class="content-card security-card" shadow="never">
          <template #header>
            <div class="card-heading">
              <span class="heading-icon security-icon"><el-icon><Lock /></el-icon></span>
              <div><h2>账号安全</h2><p>定期更新登录密码，保护账号和社区数据</p></div>
            </div>
          </template>

          <div class="security-content">
            <div class="security-summary">
              <div class="security-status"><el-icon><Check /></el-icon></div>
              <div><strong>登录密码已设置</strong><span>修改成功后需要重新登录</span></div>
            </div>
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top" size="large" class="password-form" @submit.prevent>
              <el-form-item label="当前密码" prop="oldPassword">
                <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <div class="password-row">
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
                </el-form-item>
              </div>
              <div class="form-actions">
                <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">
                  <el-icon><Key /></el-icon><span>更新密码</span>
                </el-button>
              </div>
            </el-form>
          </div>
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadProps, UploadRequestOptions } from 'element-plus'
import { Calendar, Camera, Check, Key, Lock, Message, Phone, User, UserFilled } from '@element-plus/icons-vue'
import { changePassword, getUserInfo, updateProfile, uploadAvatar } from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import SystemPageHeader from '@/components/SystemPageHeader.vue'

interface ProfileForm {
  id: number
  username: string
  realName: string
  phone: string
  email: string
  avatar: string
  gender: number
  birthday: string
  status: number
  userType: number
  loginTime: string
  createTime: string
}

const emptyProfile = (): ProfileForm => ({
  id: 0, username: '', realName: '', phone: '', email: '', avatar: '', gender: 0,
  birthday: '', status: 1, userType: 5, loginTime: '', createTime: ''
})

const router = useRouter()
const authStore = useAuthStore()
const infoFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const changingPwd = ref(false)
const avatarUploading = ref(false)
const userForm = reactive<ProfileForm>(emptyProfile())
const savedProfile = ref<ProfileForm>(emptyProfile())

const infoRules: FormRules<ProfileForm> = {
  realName: [{ max: 50, message: '姓名不能超过50个字符', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
}

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,20}$/
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: passwordPattern, message: '密码需为8-20位，且同时包含字母和数字', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        callback(value === pwdForm.newPassword ? undefined : new Error('两次输入的新密码不一致'))
      },
      trigger: 'blur'
    }
  ]
}

const profileInitial = computed(() => (userForm.realName || userForm.username || '用').slice(0, 1))
const maskedPhone = computed(() => userForm.phone ? userForm.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定')
const userTypeLabel = computed(() => ({ 1: '业主', 2: '家属', 3: '租户', 4: '物业人员', 5: '管理员' } as Record<number, string>)[userForm.userType] || '社区用户')
const profileCompletion = computed(() => {
  const values = [userForm.avatar, userForm.realName, userForm.phone, userForm.email, userForm.gender, userForm.birthday]
  return Math.round(values.filter(Boolean).length / values.length * 100)
})
const profileChanged = computed(() => ['realName', 'phone', 'email', 'gender', 'birthday']
  .some(key => userForm[key as keyof ProfileForm] !== savedProfile.value[key as keyof ProfileForm]))

function syncAuthStore(profile: ProfileForm) {
  authStore.setUserInfo({ ...authStore.userInfo, ...profile, roles: authStore.userInfo?.roles || [] })
}

function assignProfile(data: Partial<ProfileForm>) {
  Object.assign(userForm, emptyProfile(), data)
  savedProfile.value = { ...userForm }
  syncAuthStore(userForm)
}

async function loadUserInfo() {
  loading.value = true
  try { assignProfile(await getUserInfo()) }
  finally { loading.value = false }
}

function resetProfile() {
  Object.assign(userForm, savedProfile.value)
  infoFormRef.value?.clearValidate()
}

async function handleSave() {
  if (!infoFormRef.value || !await infoFormRef.value.validate().catch(() => false)) return
  saving.value = true
  try {
    const profile = await updateProfile({
      realName: userForm.realName,
      phone: userForm.phone,
      email: userForm.email,
      gender: userForm.gender,
      birthday: userForm.birthday || undefined
    }) as ProfileForm
    assignProfile(profile)
    ElMessage.success('个人资料已更新')
  } finally { saving.value = false }
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG 或 WebP 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像文件不能超过2MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  avatarUploading.value = true
  try {
    const result = await uploadAvatar(options.file)
    userForm.avatar = result.url
    savedProfile.value.avatar = result.url
    syncAuthStore(userForm)
    ElMessage.success('头像已更新')
    return result
  } finally { avatarUploading.value = false }
}

async function handleChangePassword() {
  if (!pwdFormRef.value || !await pwdFormRef.value.validate().catch(() => false)) return
  changingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码已更新，请重新登录')
    authStore.clearAuth()
    await router.replace({ path: '/login', query: { redirect: '/system/user-center' } })
  } finally { changingPwd.value = false }
}

function disableFutureDate(date: Date) { return date.getTime() > Date.now() }
function formatDate(value: string) { return value ? value.replace('T', ' ').slice(0, 16) : '暂无记录' }
onMounted(loadUserInfo)
</script>

<style lang="scss" scoped>
.profile-layout { display: grid; grid-template-columns: minmax(220px, 280px) minmax(0, 1fr); align-items: start; gap: 18px; }
.identity-panel, .content-card { background: #fff; border: 1px solid #dfe5ec; border-radius: 7px; box-shadow: 0 2px 8px rgba(31, 41, 55, .04); }
.identity-panel { padding: 28px 22px 22px; text-align: center; }
.avatar-upload { display: inline-block; }
.avatar-upload :deep(.el-upload) { display: flex; flex-direction: column; align-items: center; }
.avatar-frame { position: relative; display: inline-flex; cursor: pointer; }
.avatar-frame :deep(.el-avatar) { color: #fff; background: #3f6f97; font-size: 34px; font-weight: 600; }
.avatar-fallback { line-height: 1; }
.avatar-action { position: absolute; right: 2px; bottom: 2px; display: grid; width: 30px; height: 30px; place-items: center; color: #fff; background: #263f56; border: 3px solid #fff; border-radius: 50%; }
.avatar-button { margin-top: 14px; }
.identity-panel h2 { margin: 18px 0 3px; font-size: 20px; line-height: 1.35; }
.username { color: #7a8694; font-size: 13px; }
.identity-tags { display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
.identity-details { margin-top: 24px; border-top: 1px solid #edf0f3; text-align: left; }
.identity-details div { padding: 13px 0; border-bottom: 1px solid #edf0f3; }
.identity-details dt { display: flex; align-items: center; gap: 7px; color: #7a8694; font-size: 12px; }
.identity-details dd { overflow: hidden; margin: 5px 0 0 23px; color: #2f3945; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.profile-main { display: grid; min-width: 0; gap: 18px; }
.content-card :deep(.el-card__header) { padding: 18px 20px; border-bottom-color: #e7ebef; }
.content-card :deep(.el-card__body) { padding: 22px 20px; }
.card-heading { display: flex; align-items: center; gap: 12px; }
.heading-icon { display: grid; flex: 0 0 38px; width: 38px; height: 38px; place-items: center; border-radius: 7px; font-size: 18px; }
.profile-icon { color: #1f5f99; background: #e9f2fa; }
.security-icon { color: #9a5a16; background: #fff3df; }
.card-heading h2 { margin: 0; color: #25313d; font-size: 16px; line-height: 1.4; }
.card-heading p { margin: 2px 0 0; color: #7a8694; font-size: 12px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 20px; }
.form-grid :deep(.el-date-editor) { width: 100%; }
.gender-options { display: flex; width: 100%; }
.gender-options :deep(.el-radio-button) { flex: 1; }
.gender-options :deep(.el-radio-button__inner) { width: 100%; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px; }
.security-content { display: grid; grid-template-columns: minmax(190px, .7fr) minmax(300px, 1.3fr); gap: 28px; }
.security-summary { display: flex; align-items: flex-start; gap: 11px; padding: 14px; background: #f5f8f6; border: 1px solid #dfe9e2; border-radius: 6px; }
.security-status { display: grid; flex: 0 0 30px; width: 30px; height: 30px; place-items: center; color: #287a45; background: #e2f2e7; border-radius: 50%; }
.security-summary strong, .security-summary span { display: block; }
.security-summary strong { color: #2d3a32; font-size: 14px; }
.security-summary span { margin-top: 4px; color: #718078; font-size: 12px; }
.password-form { min-width: 0; }
.password-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 980px) { .profile-layout { grid-template-columns: 220px minmax(0, 1fr); } .form-grid, .password-row, .security-content { grid-template-columns: 1fr; } }
@media (max-width: 700px) { .profile-layout { grid-template-columns: 1fr; } .identity-panel { padding: 22px 18px; } .identity-details { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); } .identity-details div { min-width: 0; padding: 12px 8px; } .identity-details dd { margin-left: 0; } .content-card :deep(.el-card__header), .content-card :deep(.el-card__body) { padding-right: 16px; padding-left: 16px; } }
@media (max-width: 480px) { .identity-details { grid-template-columns: 1fr; } .form-actions { flex-direction: column-reverse; } .form-actions .el-button { width: 100%; margin: 0; } }
</style>
