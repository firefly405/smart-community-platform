<template>
  <div class="system-page ai-config-page" v-loading="loading">
    <SystemPageHeader>
      <template #meta>
        <el-tag :type="status.runtimeEnabled ? 'success' : 'info'" effect="plain">
          {{ status.runtimeEnabled ? '服务运行中' : '服务未启用' }}
        </el-tag>
      </template>
    </SystemPageHeader>

    <el-alert
      v-if="status.restartRequired"
      title="配置已保存，重启后端服务后生效"
      type="warning"
      :closable="false"
      show-icon
      class="restart-alert"
    />

    <div class="config-layout">
      <aside class="status-panel">
        <div class="panel-heading"><span class="panel-icon"><el-icon><Connection /></el-icon></span><div><h2>运行状态</h2><p>当前后端进程</p></div></div>
        <dl class="status-list">
          <div><dt>接口类型</dt><dd>{{ status.provider || 'OpenAI兼容 API' }}</dd></div>
          <div><dt>AI 服务</dt><dd><span class="state-dot" :class="{ active: status.runtimeEnabled }" />{{ status.runtimeEnabled ? '已启用' : '未启用' }}</dd></div>
          <div><dt>API Key</dt><dd>{{ status.apiKeyConfigured ? status.maskedApiKey : '未配置' }}</dd></div>
          <div><dt>API 地址</dt><dd>{{ status.apiUrl || '未配置' }}</dd></div>
          <div><dt>对话模型</dt><dd>{{ status.chatModel || '-' }}</dd></div>
          <div><dt>向量模型</dt><dd>{{ status.embeddingModel || '-' }}</dd></div>
        </dl>
        <div v-if="testResult" class="test-result" :class="testResult.success ? 'success' : 'error'">
          <el-icon><CircleCheck v-if="testResult.success" /><CircleClose v-else /></el-icon>
          <div><strong>{{ testResult.message }}</strong><span v-if="testResult.latencyMs">{{ testResult.latencyMs }} ms</span></div>
        </div>
      </aside>

      <section class="form-panel">
        <header class="form-heading">
          <div><h2>模型服务配置</h2><p>自动识别 OpenAI 兼容接口的可用模型</p></div>
          <el-switch v-model="form.enabled" inline-prompt active-text="启用" inactive-text="停用" />
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="config-form">
          <el-form-item label="API Base URL" prop="apiUrl">
            <el-input
              v-model="form.apiUrl"
              clearable
              placeholder="https://api.siliconflow.cn/v1"
            >
              <template #prefix><el-icon><Link /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item label="API Key" prop="apiKey">
            <el-input
              v-model="form.apiKey"
              type="password"
              show-password
              clearable
              autocomplete="new-password"
              :placeholder="status.apiKeyConfigured ? `当前 ${status.maskedApiKey}` : '请输入服务商 API Key'"
            >
              <template #prefix><el-icon><Key /></el-icon></template>
            </el-input>
          </el-form-item>

          <div class="model-grid">
            <el-form-item label="对话模型" prop="chatModel">
              <el-select v-model="form.chatModel" filterable allow-create clearable default-first-option placeholder="测试连接后自动选择">
                <el-option v-for="model in chatModels" :key="model" :label="model" :value="model" />
              </el-select>
            </el-form-item>
            <el-form-item label="向量模型" prop="embeddingModel">
              <el-select v-model="form.embeddingModel" filterable allow-create clearable default-first-option placeholder="无向量模型时自动关闭知识库">
                <el-option v-for="model in embeddingModels" :key="model" :label="model" :value="model" />
              </el-select>
            </el-form-item>
          </div>

          <div class="config-note">
            <el-icon><Lock /></el-icon>
            <span>支持确实现 /v1/models 和 /v1/chat/completions 的 HTTPS 接口；密钥仅保存于服务器本地运行时配置。</span>
          </div>

          <div class="form-actions">
            <el-button :loading="testing" :disabled="!canTest" @click="handleTest"><el-icon><Connection /></el-icon>测试连接</el-button>
            <el-button :loading="restarting" :disabled="saving || testing" @click="handleRestart"><el-icon><RefreshRight /></el-icon>重启后端</el-button>
            <el-button type="primary" :loading="saving" @click="handleSave"><el-icon><Check /></el-icon>保存配置</el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CircleCheck, CircleClose, Connection, Key, Link, Lock, RefreshRight } from '@element-plus/icons-vue'
import SystemPageHeader from '@/components/SystemPageHeader.vue'
import { getAiConfig, restartBackend, saveAiConfig, testAiConnection, type AiConfig, type AiConnectionResult } from '@/api/ai'

const emptyStatus: AiConfig = {
  provider: 'OpenAI兼容 API', enabled: false, runtimeEnabled: false,
  apiKeyConfigured: false, maskedApiKey: '', apiUrl: '', chatModel: '',
  embeddingModel: '', restartRequired: false
}
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const restarting = ref(false)
const formRef = ref<FormInstance>()
const status = ref<AiConfig>({ ...emptyStatus })
const testResult = ref<AiConnectionResult | null>(null)
const availableModels = ref<string[]>([])
const form = reactive({ enabled: false, apiUrl: '', apiKey: '', chatModel: '', embeddingModel: '' })
const modelPattern = /^[A-Za-z0-9._:/-]{1,160}$/
const isEmbeddingModel = (model: string) => /embed|bge|gte-|(^|[\/_-])e5([\/_-]|$)/i.test(model)
const chatModels = computed(() => availableModels.value.filter(model => !isEmbeddingModel(model)))
const embeddingModels = computed(() => availableModels.value.filter(isEmbeddingModel))
const rules: FormRules = {
  apiUrl: [{ validator: (_rule, value, callback) => {
    if (form.enabled && !String(value || '').trim()) callback(new Error('启用AI服务前请填写API地址'))
    else if (value && !/^https:\/\//i.test(String(value).trim())) callback(new Error('请填写HTTPS API地址'))
    else callback()
  }, trigger: 'blur' }],
  apiKey: [{ validator: (_rule, value, callback) => {
    if (form.enabled && !status.value.apiKeyConfigured && !String(value || '').trim()) callback(new Error('启用AI服务前请填写API Key'))
    else callback()
  }, trigger: 'blur' }],
  chatModel: [{ pattern: modelPattern, message: '模型名称格式不正确', trigger: 'blur' }],
  embeddingModel: [{ pattern: modelPattern, message: '模型名称格式不正确', trigger: 'blur' }]
}
const canTest = computed(() => Boolean(form.apiUrl.trim() && (form.apiKey.trim() || status.value.apiKeyConfigured)))

function applyStatus(data: AiConfig) {
  status.value = data
  form.enabled = data.enabled
  form.apiUrl = data.apiUrl || ''
  form.apiKey = ''
  form.chatModel = data.chatModel || ''
  form.embeddingModel = data.embeddingModel || ''
}

async function loadConfig() {
  loading.value = true
  try { applyStatus(await getAiConfig()) } finally { loading.value = false }
}
async function handleSave() {
  if (!await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    applyStatus(await saveAiConfig({ ...form, apiUrl: form.apiUrl.trim(), apiKey: form.apiKey.trim() || undefined }))
    testResult.value = null
    ElMessage.success('配置已保存')
  } finally { saving.value = false }
}
async function handleTest() {
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await testAiConnection({ apiUrl: form.apiUrl.trim(), apiKey: form.apiKey.trim() || undefined, chatModel: form.chatModel, embeddingModel: form.embeddingModel })
    if (testResult.value.success) {
      availableModels.value = testResult.value.models || []
      form.chatModel = testResult.value.chatModel || ''
      form.embeddingModel = testResult.value.embeddingModel || ''
    }
    ElMessage[testResult.value.success ? 'success' : 'warning'](testResult.value.message)
  } finally { testing.value = false }
}
async function handleRestart() {
  try {
    await ElMessageBox.confirm('重启期间系统将短暂不可用，是否继续？', '重启后端服务', {
      confirmButtonText: '确认重启', cancelButtonText: '取消', type: 'warning'
    })
  } catch { return }

  restarting.value = true
  try {
    await restartBackend()
    ElMessage.info('后端正在重启，请稍候')
    const token = localStorage.getItem('token')
    const startedAt = Date.now()
    const deadline = Date.now() + 45000
    let sawOffline = false
    while (Date.now() < deadline) {
      await new Promise(resolve => setTimeout(resolve, 1000))
      try {
        const response = await fetch('/api/v1/system/ai-config', {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        })
        if (!response.ok) sawOffline = true
        if (response.ok && (sawOffline || Date.now() - startedAt >= 3000)) {
          await loadConfig()
          ElMessage.success('后端服务已重启')
          return
        }
      } catch {
        sawOffline = true
      }
    }
    ElMessage.warning('服务尚未恢复，请检查后端日志')
  } finally { restarting.value = false }
}
onMounted(loadConfig)
</script>

<style scoped>
.restart-alert { margin-bottom: 16px; }
.config-layout { display: grid; grid-template-columns: minmax(250px, .72fr) minmax(420px, 1.28fr); gap: 16px; }
.status-panel,.form-panel { min-width: 0; padding: 20px; border: 1px solid #e1e6eb; border-radius: 7px; background: #fff; }
.panel-heading,.form-heading { display: flex; align-items: center; gap: 12px; padding-bottom: 17px; border-bottom: 1px solid #edf0f2; }
.form-heading { justify-content: space-between; }
.panel-icon { display: grid; flex: 0 0 38px; width: 38px; height: 38px; place-items: center; color: #285f91; border-radius: 6px; background: #eaf2fb; font-size: 19px; }
h2 { margin: 0; color: #273444; font-size: 16px; letter-spacing: 0; } p { margin: 4px 0 0; color: #7b8794; font-size: 12px; }
.status-list { margin: 4px 0 0; }
.status-list > div { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 13px 0; border-bottom: 1px solid #f0f2f4; }
.status-list dt { color: #7b8794; font-size: 13px; }.status-list dd { display: flex; align-items: center; gap: 7px; margin: 0; color: #344054; font-size: 13px; font-weight: 600; text-align: right; word-break: break-word; }
.state-dot { width: 7px; height: 7px; border-radius: 50%; background: #aab2bd; }.state-dot.active { background: #2d8a6d; box-shadow: 0 0 0 3px #e3f3ed; }
.test-result { display: flex; align-items: center; gap: 10px; margin-top: 16px; padding: 12px; border-radius: 6px; background: #f6f8fa; color: #667085; }.test-result.success { color: #236b55; background: #edf8f3; }.test-result.error { color: #a13d3d; background: #fff1f1; }
.test-result .el-icon { font-size: 20px; }.test-result strong,.test-result span { display: block; }.test-result strong { font-size: 13px; }.test-result span { margin-top: 2px; font-size: 12px; font-weight: 400; }
.config-form { padding-top: 18px; }.model-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }.model-grid :deep(.el-select) { width: 100%; }
.config-note { display: flex; align-items: flex-start; gap: 9px; padding: 12px 14px; color: #5e6875; border: 1px solid #e2e7ec; border-radius: 6px; background: #f8fafb; font-size: 12px; line-height: 1.55; }.config-note .el-icon { flex: 0 0 auto; margin-top: 2px; color: #285f91; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 22px; padding-top: 18px; border-top: 1px solid #edf0f2; }
@media (max-width: 860px) { .config-layout { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .status-panel,.form-panel { padding: 16px; }.model-grid { grid-template-columns: 1fr; gap: 0; }.form-actions { align-items: stretch; flex-direction: column-reverse; }.form-actions .el-button { width: 100%; margin-left: 0; } }
</style>
