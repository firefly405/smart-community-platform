<template>
  <section class="tenant-page" v-loading="loading">
    <div class="page-heading">
      <div><p class="eyebrow">智慧社区 / 租户服务中心</p><h1>{{ title }}</h1></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="feedback" />
    <el-alert v-if="summary.bound === false" title="账号当前没有有效租约，请联系物业核实租户档案。" type="warning" show-icon :closable="false" class="feedback" />

    <template v-if="section === 'home'">
      <div class="welcome">
        <div>
          <p>租户服务中心</p>
          <h2>{{ auth.userInfo?.realName || auth.userInfo?.username }}，欢迎回来</h2>
          <span>{{ today }}</span>
        </div>
        <img src="/community-home.jpg" alt="社区住宅与绿化" />
      </div>

      <div v-if="summary.bound" class="lease-strip" :class="{ warning: (summary.daysRemaining ?? 999) <= 30 }">
        <el-icon><Calendar /></el-icon>
        <div><span>当前租约</span><strong>{{ summary.leaseEndDate ? `有效期至 ${summary.leaseEndDate}` : '租约有效' }}</strong></div>
        <el-tag :type="(summary.daysRemaining ?? 999) <= 30 ? 'warning' : 'success'" effect="plain">
          {{ remainingText }}
        </el-tag>
      </div>

      <div class="metrics">
        <router-link to="/tenant/bills"><el-icon class="amber"><Tickets /></el-icon><span>待缴账单<strong>{{ summary.unpaid || 0 }}<small>笔</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
        <router-link to="/tenant/repairs"><el-icon><Tools /></el-icon><span>进行中报修<strong>{{ summary.repairs || 0 }}<small>单</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
        <router-link to="/tenant/complaints"><el-icon class="rose"><ChatLineSquare /></el-icon><span>处理中建议<strong>{{ summary.complaints || 0 }}<small>条</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
      </div>

      <h2 class="section-title">常用服务</h2>
      <div class="quick-links">
        <router-link v-for="item in shortcuts" :key="item.path" :to="`/tenant/${item.path}`">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span><el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>

      <div class="section-heading"><h2>社区公告</h2><router-link to="/tenant/notices">全部公告 <el-icon><ArrowRight /></el-icon></router-link></div>
      <el-empty v-if="!records.length && !loading" description="暂无已发布公告" />
      <button v-for="notice in records.slice(0, 5)" :key="notice.id" class="notice-row" @click="detail = notice">
        <el-icon><Bell /></el-icon><span>{{ notice.title }}</span><time>{{ date(notice.publishTime) }}</time><el-icon><ArrowRight /></el-icon>
      </button>
    </template>

    <template v-else-if="section === 'lease'">
      <el-empty v-if="!summary.bound && !loading" description="暂无有效租约" />
      <div v-else-if="lease" class="lease-grid">
        <article class="info-panel lease-panel">
          <div class="panel-heading"><span><el-icon><Document /></el-icon></span><div><h2>租约信息</h2><p>仅展示当前有效租约</p></div><el-tag type="success">生效中</el-tag></div>
          <dl>
            <div><dt>承租人</dt><dd>{{ lease.tenant.realName || '—' }}</dd></div>
            <div><dt>租期开始</dt><dd>{{ lease.tenant.leaseStartDate || '—' }}</dd></div>
            <div><dt>租期结束</dt><dd>{{ lease.tenant.leaseEndDate || '—' }}</dd></div>
            <div><dt>剩余租期</dt><dd>{{ remainingText }}</dd></div>
            <div><dt>月租金</dt><dd>{{ money(lease.tenant.monthlyRent) }}</dd></div>
            <div><dt>押金</dt><dd>{{ money(lease.tenant.deposit) }}</dd></div>
            <div><dt>身份证号</dt><dd>{{ maskIdCard(lease.tenant.idCard) }}</dd></div>
            <div><dt>登记手机号</dt><dd>{{ maskPhone(lease.tenant.phone) }}</dd></div>
          </dl>
        </article>
        <article class="info-panel house-panel">
          <div class="panel-heading"><span><el-icon><OfficeBuilding /></el-icon></span><div><h2>承租房屋</h2><p>{{ houseAddress }}</p></div></div>
          <dl>
            <div><dt>楼栋</dt><dd>{{ lease.house.buildingName || '—' }}</dd></div>
            <div><dt>单元</dt><dd>{{ lease.house.unitName || '—' }}</dd></div>
            <div><dt>房号</dt><dd>{{ lease.house.houseNo || '—' }}</dd></div>
            <div><dt>楼层</dt><dd>{{ lease.house.floor ?? '—' }}</dd></div>
            <div><dt>户型</dt><dd>{{ lease.house.houseType || '—' }}</dd></div>
            <div><dt>朝向</dt><dd>{{ lease.house.orientation || '—' }}</dd></div>
            <div><dt>建筑面积</dt><dd>{{ area(lease.house.area) }}</dd></div>
            <div><dt>业主</dt><dd>{{ lease.house.ownerName || lease.tenant.ownerName || '—' }}</dd></div>
          </dl>
        </article>
      </div>
    </template>

    <template v-else-if="section === 'profile'">
      <div class="profile-heading">
        <el-avatar :size="64" :src="auth.userInfo?.avatar">{{ (auth.userInfo?.realName || auth.userInfo?.username || '租').slice(0, 1) }}</el-avatar>
        <div><h2>{{ auth.userInfo?.realName || auth.userInfo?.username }}</h2><p>{{ auth.userInfo?.username }} · 社区租户</p></div>
      </div>
      <el-tabs v-model="profileTab">
        <el-tab-pane label="个人资料" name="info">
          <el-form class="profile-form" label-position="top" @submit.prevent="saveProfile">
            <el-form-item label="姓名"><el-input v-model="profile.realName" maxlength="50" /></el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profile.phone" disabled />
              <p class="field-help">手机号用于匹配租户档案与租约，如需修改请联系物业。</p>
            </el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" maxlength="100" /></el-form-item>
            <el-form-item label="性别"><el-radio-group v-model="profile.gender"><el-radio :value="0">未设置</el-radio><el-radio :value="1">男</el-radio><el-radio :value="2">女</el-radio></el-radio-group></el-form-item>
            <el-form-item label="生日"><el-date-picker v-model="profile.birthday" value-format="YYYY-MM-DD" type="date" :disabled-date="disableFutureDate" /></el-form-item>
            <el-button type="primary" native-type="submit" :icon="Check" :loading="saving">保存资料</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="账户安全" name="security">
          <el-form class="profile-form" label-position="top" @submit.prevent="savePassword">
            <el-form-item label="当前密码"><el-input v-model="password.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password autocomplete="new-password" placeholder="8–20位，包含字母和数字" /></el-form-item>
            <el-form-item label="确认新密码"><el-input v-model="password.confirm" type="password" show-password autocomplete="new-password" /></el-form-item>
            <el-button type="primary" native-type="submit" :icon="Lock" :loading="saving">修改密码</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template v-else>
      <div class="list-toolbar">
        <span>共 {{ total }} 条记录</span>
        <el-button v-if="isService" type="primary" :icon="Plus" :disabled="summary.bound === false" @click="openService">
          {{ section === 'repairs' ? '提交报修' : '提交投诉建议' }}
        </el-button>
      </div>
      <el-table v-if="!error" :data="records" empty-text="暂无记录" stripe class="tenant-table" @row-click="detail = $event">
        <el-table-column v-for="column in columns" :key="column.key" :label="column.label" :min-width="column.width || 130" show-overflow-tooltip>
          <template #default="{ row }">{{ format(row, column.key) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="detail = row">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-if="total > 10" v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" />
    </template>

    <el-dialog :model-value="!!detail" :title="String(detail?.title || `${title}详情`)" width="min(640px, calc(100vw - 32px))" @close="detail = null">
      <template v-if="detail">
        <dl class="detail-list"><template v-for="column in detailColumns" :key="column.key"><dt>{{ column.label }}</dt><dd>{{ format(detail, column.key) }}</dd></template></dl>
        <p v-if="detail.content" class="detail-content">{{ plainText(String(detail.content)) }}</p>
        <div v-if="section === 'repairs' && detail.status === 3 && !detail.evaluateTime" class="evaluation">
          <h3>服务评价</h3><el-rate v-model="rating" /><el-input v-model="evaluation" type="textarea" :rows="3" maxlength="500" placeholder="评价内容（选填）" /><el-button type="primary" :loading="saving" @click="evaluate">提交评价</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="serviceDialog" :title="section === 'repairs' ? '提交报修' : '提交投诉建议'" width="min(620px, calc(100vw - 32px))" :close-on-click-modal="false">
      <el-form ref="serviceRef" :model="service" :rules="serviceRules" label-position="top" @submit.prevent="submitService">
        <el-form-item label="关联房屋"><el-input :model-value="houseAddress" disabled /></el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="service.title" maxlength="100" show-word-limit /></el-form-item>
        <div class="form-grid">
          <el-form-item label="类型" prop="type"><el-select v-model="service.type"><el-option v-for="(label, index) in serviceTypes" :key="label" :label="label" :value="index + 1" /></el-select></el-form-item>
          <el-form-item v-if="section === 'repairs'" label="紧急程度"><el-select v-model="service.urgency"><el-option label="一般" :value="1" /><el-option label="紧急" :value="2" /><el-option label="特急" :value="3" /></el-select></el-form-item>
        </div>
        <el-form-item label="详细描述" prop="content"><el-input v-model="service.content" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <div class="form-grid"><el-form-item label="联系人" prop="contactName"><el-input v-model="service.contactName" maxlength="50" /></el-form-item><el-form-item label="联系电话" prop="contactPhone"><el-input v-model="service.contactPhone" maxlength="11" /></el-form-item></div>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="serviceDialog = false">取消</el-button><el-button type="primary" :loading="saving" :icon="Check" @click="submitService">提交</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Calendar, Check, Document, Lock, Plus, Refresh } from '@element-plus/icons-vue'
import { changePassword, getUserInfo, updateProfile } from '@/api/auth'
import { createTenantService, evaluateTenantRepair, getTenantLease, getTenantList, getTenantSummary } from '@/api/tenant'
import type { TenantLeaseDetail, TenantListSection, TenantRecord, TenantServicePayload, TenantSummary } from '@/api/tenant'
import { useAuthStore } from '@/store/auth'
import { tenantNav } from './navigation'

type Column = { key: string; label: string; width?: number }
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const section = computed(() => String(route.meta.tenantSection || 'home'))
const title = computed(() => String(route.meta.title || '租户服务中心'))
const isService = computed(() => ['repairs', 'complaints'].includes(section.value))
const shortcuts = tenantNav.filter(item => ['lease', 'bills', 'repairs', 'complaints', 'phones'].includes(item.path))
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const page = ref(1)
const total = ref(0)
const records = ref<TenantRecord[]>([])
const detail = ref<TenantRecord | null>(null)
const lease = ref<TenantLeaseDetail | null>(null)
const summary = ref<TenantSummary>({ bound: true, unpaid: 0, repairs: 0, complaints: 0 })
const profileTab = ref('info')
const profile = reactive({ realName: '', phone: '', email: '', gender: 0, birthday: '' })
const password = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const definitions: Record<string, Column[]> = {
  bills: [{ key: 'billNo', label: '账单编号', width: 190 }, { key: 'itemName', label: '收费项目' }, { key: 'billPeriod', label: '账期' }, { key: 'payableAmount', label: '应缴金额（元）' }, { key: 'paidAmount', label: '已缴金额（元）' }, { key: 'status', label: '状态' }],
  repairs: [{ key: 'repairNo', label: '报修单号', width: 190 }, { key: 'title', label: '报修标题', width: 180 }, { key: 'status', label: '状态' }, { key: 'createTime', label: '提交时间', width: 180 }],
  complaints: [{ key: 'complaintNo', label: '投诉单号', width: 190 }, { key: 'title', label: '投诉 / 建议', width: 180 }, { key: 'status', label: '状态' }, { key: 'createTime', label: '提交时间', width: 180 }],
  notices: [{ key: 'title', label: '公告标题', width: 240 }, { key: 'publisher', label: '发布人' }, { key: 'publishTime', label: '发布时间', width: 180 }],
  activities: [{ key: 'title', label: '活动名称', width: 180 }, { key: 'location', label: '活动地点' }, { key: 'startTime', label: '开始时间', width: 180 }, { key: 'endTime', label: '结束时间', width: 180 }],
  phones: [{ key: 'name', label: '服务名称' }, { key: 'phone', label: '联系电话', width: 170 }, { key: 'workTime', label: '服务时间' }, { key: 'address', label: '地址', width: 200 }]
}
const columns = computed(() => definitions[section.value] || [])
const detailColumns = computed(() => {
  const base = section.value === 'home' ? definitions.notices : columns.value
  return [...base, ...(isService.value ? [{ key: 'handleResult', label: '处理结果' }, { key: 'replyContent', label: '物业回复' }, { key: 'contactName', label: '联系人' }, { key: 'contactPhone', label: '联系电话' }] : [])]
})
const remainingText = computed(() => {
  if (!summary.value.bound) return '无有效租约'
  const days = summary.value.daysRemaining
  if (days == null) return '租约有效'
  if (days === 0) return '今天到期'
  return days > 0 ? `剩余 ${days} 天` : `已到期 ${Math.abs(days)} 天`
})
const houseAddress = computed(() => {
  const house = lease.value?.house
  return house ? [house.buildingName, house.unitName, house.houseNo].filter(Boolean).join(' / ') : '当前承租房屋'
})

function date(value: unknown) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function money(value: unknown) { return value == null ? '—' : `¥ ${Number(value).toFixed(2)}` }
function area(value: unknown) { return value == null ? '—' : `${Number(value)} ㎡` }
function maskPhone(value?: string) { return value ? value.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '—' }
function maskIdCard(value?: string) { return value && value.length > 8 ? `${value.slice(0, 4)}**********${value.slice(-4)}` : '—' }
function plainText(content: string) { return new DOMParser().parseFromString(content, 'text/html').body.textContent || '' }
function format(row: TenantRecord, key: string) {
  if (key === 'status') {
    const labels: Record<string, string[]> = { repairs: ['待处理', '已派单', '处理中', '已完成', '已取消'], complaints: ['待处理', '处理中', '已完成', '已关闭'], bills: ['待缴费', '部分缴费', '已缴清', '已作废'] }
    return labels[section.value]?.[Number(row[key])] || '未知状态'
  }
  if (key.endsWith('Time')) return date(row[key])
  if (key.endsWith('Amount')) return Number(row[key] || 0).toFixed(2)
  return row[key] ?? '—'
}

let loadSequence = 0
async function ensureLease() {
  if (!lease.value && summary.value.bound) lease.value = await getTenantLease()
}
async function load() {
  const sequence = ++loadSequence
  const currentSection = section.value
  loading.value = true
  error.value = ''
  try {
    const info = await getTenantSummary()
    if (sequence !== loadSequence) return
    summary.value = info
    if (currentSection === 'profile') {
      const user = await getUserInfo()
      if (sequence !== loadSequence) return
      auth.setUserInfo({ ...user, roles: [] })
      Object.assign(profile, user)
      return
    }
    if (currentSection === 'lease') {
      lease.value = info.bound ? await getTenantLease() : null
      return
    }
    if (info.bound === false && ['bills', 'repairs', 'complaints'].includes(currentSection)) {
      records.value = []
      total.value = 0
      return
    }
    const listSection = (currentSection === 'home' ? 'notices' : currentSection) as TenantListSection
    const result = await getTenantList(listSection, page.value, 10)
    if (sequence !== loadSequence) return
    records.value = result.records || []
    total.value = result.total || 0
  } catch (ex: any) {
    if (sequence === loadSequence) error.value = ex.response?.data?.message || ex.message || '加载失败，请重试'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

watch(section, () => {
  page.value = 1
  detail.value = null
  serviceDialog.value = false
  records.value = []
  total.value = 0
  void load()
})

const serviceDialog = ref(false)
const serviceRef = ref<FormInstance>()
const service = reactive<TenantServicePayload>({ houseId: 0, title: '', content: '', type: 1, urgency: 1, contactName: '', contactPhone: '' })
const serviceTypes = computed(() => section.value === 'repairs'
  ? ['水电维修', '门窗维修', '墙面维修', '地面维修', '公共设施', '其他']
  : ['服务态度', '环境卫生', '设施设备', '其他'])
const required = { required: true, message: '请填写此项', trigger: 'blur' }
const serviceRules = { title: [required], content: [required], type: [required], contactName: [required], contactPhone: [required, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }] }

async function openService() {
  if (!summary.value.bound || !summary.value.houseId) { ElMessage.warning('当前没有可用的承租房屋'); return }
  try { await ensureLease() } catch { return }
  Object.assign(service, { houseId: summary.value.houseId, title: '', content: '', type: 1, urgency: 1, contactName: auth.userInfo?.realName || '', contactPhone: auth.userInfo?.phone || '' })
  serviceDialog.value = true
  serviceRef.value?.clearValidate()
}
async function submitService() {
  if (saving.value || !await serviceRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    await createTenantService(section.value as 'repairs' | 'complaints', service)
    serviceDialog.value = false
    ElMessage.success('提交成功')
    page.value = 1
    await load()
  } catch { /* Request errors are displayed by the interceptor. */ }
  finally { saving.value = false }
}

const rating = ref(5)
const evaluation = ref('')
watch(detail, () => { rating.value = 5; evaluation.value = '' })
async function evaluate() {
  if (!detail.value || saving.value || !rating.value) return
  saving.value = true
  try {
    await evaluateTenantRepair(detail.value.id, { score: rating.value, content: evaluation.value })
    detail.value = null
    ElMessage.success('评价成功')
    await load()
  } catch { /* Request errors are displayed by the interceptor. */ }
  finally { saving.value = false }
}
async function saveProfile() {
  if (saving.value) return
  saving.value = true
  try {
    const user = await updateProfile({ realName: profile.realName, phone: profile.phone, email: profile.email, gender: profile.gender, birthday: profile.birthday || undefined })
    auth.setUserInfo({ ...user, roles: [] })
    Object.assign(profile, user)
    ElMessage.success('资料已保存')
  } catch { /* Request errors are displayed by the interceptor. */ }
  finally { saving.value = false }
}
async function savePassword() {
  if (saving.value) return
  if (!password.oldPassword || !/^(?=.*[A-Za-z])(?=.*\d).{8,20}$/.test(password.newPassword) || password.newPassword !== password.confirm) {
    ElMessage.warning('请填写当前密码，新密码需8–20位字母数字组合，且两次输入一致')
    return
  }
  saving.value = true
  try {
    await changePassword({ oldPassword: password.oldPassword, newPassword: password.newPassword })
    Object.assign(password, { oldPassword: '', newPassword: '', confirm: '' })
    auth.clearAuth()
    ElMessage.success('密码已修改，请重新登录')
    await router.replace({ path: '/tenant/login', query: { redirect: '/tenant/profile' } })
  } catch { /* Request errors are displayed by the interceptor. */ }
  finally { saving.value = false }
}
function disableFutureDate(value: Date) { return value.getTime() > Date.now() }

void load()
</script>

<style scoped lang="scss">
.tenant-page { min-height: 360px; }
.page-heading, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.eyebrow { margin: 0 0 7px; color: #758789; font-size: 12px; }
h1, h2, h3, p { margin-top: 0; }
h1 { margin-bottom: 0; font-size: 24px; font-weight: 600; }
h2 { font-size: 18px; font-weight: 600; }
h3 { font-size: 15px; }
.feedback { margin-bottom: 20px; }
.welcome { display: grid; grid-template-columns: minmax(0, 1fr) 40%; align-items: center; gap: 24px; padding-bottom: 22px; border-bottom: 1px solid #dce5e3; }
.welcome p { margin-bottom: 9px; color: #236979; font-size: 13px; }
.welcome h2 { margin-bottom: 17px; font-size: 26px; overflow-wrap: anywhere; }
.welcome span { color: #798886; font-size: 13px; }
.welcome img { width: 100%; height: 180px; border-radius: 6px; object-fit: cover; }
.lease-strip { display: flex; align-items: center; gap: 13px; margin-top: 20px; padding: 13px 16px; background: #eaf3f1; border-left: 3px solid #2e7a69; }
.lease-strip.warning { background: #fff5e6; border-left-color: #c4822d; }
.lease-strip > .el-icon { color: #236979; font-size: 22px; }
.lease-strip div { min-width: 0; flex: 1; }
.lease-strip span, .lease-strip strong { display: block; }
.lease-strip span { margin-bottom: 3px; color: #72817f; font-size: 11px; }
.lease-strip strong { font-size: 14px; font-weight: 600; }
.metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; padding: 23px 0; }
.metrics a { display: flex; align-items: center; gap: 13px; padding: 8px 16px 8px 0; color: #667574; border-right: 1px solid #dce5e3; font-size: 13px; text-decoration: none; }
.metrics a:last-child { border: 0; }
.metrics a > .el-icon:first-child { color: #236979; font-size: 27px; }
.metrics a > .el-icon.amber { color: #b77a2e; }
.metrics a > .el-icon.rose { color: #aa5f67; }
.metrics a > .el-icon:last-child { margin-left: auto; }
.metrics strong { display: block; margin-top: 7px; color: #273c3f; font-size: 27px; }
.metrics small { margin-left: 7px; font-size: 12px; font-weight: 400; }
.section-title { margin: 5px 0 15px; }
.quick-links { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 11px; margin-bottom: 30px; }
.quick-links a { display: flex; align-items: center; gap: 9px; min-width: 0; padding: 18px 11px; color: #33585d; background: #fff; border: 1px solid #dfe8e6; border-radius: 6px; font-size: 13px; text-decoration: none; }
.quick-links a:hover { border-color: #236979; }
.quick-links a > span { min-width: 0; overflow-wrap: anywhere; }
.quick-links a > .el-icon:last-child { margin-left: auto; font-size: 11px; }
.section-heading { margin-bottom: 7px; }
.section-heading h2 { margin-bottom: 0; }
.section-heading a { display: flex; align-items: center; gap: 5px; color: #236979; font-size: 13px; text-decoration: none; }
.notice-row { display: flex; width: 100%; align-items: center; gap: 13px; padding: 14px 4px; color: #34494c; background: transparent; border: 0; border-bottom: 1px solid #e1e8e6; text-align: left; cursor: pointer; }
.notice-row:hover { color: #236979; }
.notice-row span { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-row time { color: #85908f; font-size: 12px; white-space: nowrap; }
.lease-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.info-panel { padding: 22px; background: #fff; border: 1px solid #dce5e3; border-radius: 7px; }
.panel-heading { display: flex; align-items: center; gap: 11px; margin-bottom: 20px; }
.panel-heading > span { display: grid; flex: 0 0 38px; width: 38px; height: 38px; place-items: center; color: #236979; background: #e6f0ef; border-radius: 6px; font-size: 19px; }
.panel-heading > div { min-width: 0; flex: 1; }
.panel-heading h2, .panel-heading p { margin-bottom: 0; }
.panel-heading p { margin-top: 3px; color: #798886; font-size: 12px; overflow-wrap: anywhere; }
.info-panel dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0; }
.info-panel dl div { min-width: 0; padding: 13px 8px 13px 0; border-top: 1px solid #edf1f0; }
.info-panel dt { color: #7b8887; font-size: 12px; }
.info-panel dd { margin: 5px 0 0; color: #2d4144; font-size: 14px; overflow-wrap: anywhere; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; color: #7b8988; font-size: 13px; }
.tenant-table { width: 100%; }
.el-pagination { justify-content: flex-end; margin-top: 18px; }
.detail-list { display: grid; grid-template-columns: 110px minmax(0, 1fr); margin: 0; }
.detail-list dt, .detail-list dd { margin: 0; padding: 10px 8px; border-bottom: 1px solid #edf0ef; overflow-wrap: anywhere; }
.detail-list dt { color: #788584; font-size: 12px; }
.detail-list dd { color: #344548; font-size: 13px; }
.detail-content { margin: 17px 0 0; padding: 15px; color: #465659; background: #f5f7f7; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.evaluation { display: grid; gap: 12px; margin-top: 18px; padding-top: 16px; border-top: 1px solid #e3e9e8; }
.evaluation h3 { margin-bottom: 0; }
.evaluation .el-button { justify-self: start; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.form-grid :deep(.el-select) { width: 100%; }
.profile-heading { display: flex; align-items: center; gap: 15px; margin-bottom: 20px; padding: 20px; background: #fff; border: 1px solid #dce5e3; border-radius: 7px; }
.profile-heading h2 { margin-bottom: 4px; overflow-wrap: anywhere; }
.profile-heading p { margin-bottom: 0; color: #7a8887; font-size: 13px; }
.profile-form { max-width: 620px; padding-top: 8px; }
.profile-form :deep(.el-date-editor) { width: 100%; }
.field-help { width: 100%; margin: 7px 0 0; color: #8a7770; font-size: 12px; line-height: 1.5; }

@media (max-width: 980px) {
  .quick-links { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .lease-grid { grid-template-columns: 1fr; }
}

@media (max-width: 700px) {
  .page-heading { margin-bottom: 18px; }
  h1 { font-size: 21px; }
  .welcome { grid-template-columns: 1fr; }
  .welcome img { height: 150px; }
  .metrics { grid-template-columns: 1fr; gap: 0; }
  .metrics a { border-right: 0; border-bottom: 1px solid #dce5e3; }
  .quick-links { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .notice-row time { display: none; }
  .form-grid { grid-template-columns: 1fr; gap: 0; }
}

@media (max-width: 460px) {
  .page-heading .el-button { padding: 8px; }
  .lease-strip { align-items: flex-start; flex-wrap: wrap; }
  .lease-strip .el-tag { margin-left: 35px; }
  .quick-links { grid-template-columns: 1fr; }
  .info-panel { padding: 17px; }
  .info-panel dl { grid-template-columns: 1fr; }
  .detail-list { grid-template-columns: 88px minmax(0, 1fr); }
}
</style>
