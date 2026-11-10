<template>
  <div class="setup-container">
    <div class="setup-box">
      <div class="setup-header">
        <h1>智慧社区综合管理平台</h1>
        <p>Smart Community Management Platform</p>
      </div>

      <el-alert
        title="检测到系统中存在未设置密码的账户，请立即初始化系统密码以确保安全。"
        type="warning"
        :closable="false"
        show-icon
        class="setup-alert"
      />

      <el-form ref="formRef" :model="form" :rules="rules" class="setup-form">
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入新密码（至少6位）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="setup-button" @click="handleInit">
            一键初始化
          </el-button>
        </el-form-item>
      </el-form>

      <div class="setup-footer">
        <span>此操作仅为系统中尚未设置密码的账户统一设置密码</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  password: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: any, value: string, callback: Function) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleInit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await request.post('/api/v1/setup/init', { password: form.password })
    ElMessage.success('密码初始化成功，即将跳转到登录页')
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.setup-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.setup-box {
  width: 440px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.setup-header {
  text-align: center;
  margin-bottom: 24px;
}

.setup-header h1 {
  font-size: 24px;
  color: #303133;
  margin: 0 0 8px 0;
}

.setup-header p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.setup-alert {
  margin-bottom: 24px;
}

.setup-form {
  margin-top: 0;
}

.setup-button {
  width: 100%;
}

.setup-footer {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 16px;
}
</style>
