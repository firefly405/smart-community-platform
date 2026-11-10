<template>
  <section class="resident-page" v-loading="loading">
    <div class="page-heading">
      <div><p class="eyebrow">智慧社区 / 业主服务</p><h1>{{ title }}</h1></div>
      <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="feedback" />
    <el-alert v-if="summary.bound === false" title="账号尚未关联业主档案，请联系物业核实房屋信息。" type="warning" show-icon :closable="false" class="feedback" />

    <template v-if="section === 'home'">
      <div class="welcome"><div><p>业主服务中心</p><h2>{{ auth.userInfo?.realName || auth.userInfo?.username }}，欢迎回家</h2><span>{{ today }}</span></div><img src="/community-home.jpg" alt="社区住宅与绿化" /></div>
      <div class="metrics">
        <router-link to="/community/houses"><el-icon><OfficeBuilding /></el-icon><span>我的房屋<strong>{{ summary.houses || 0 }}<small>套</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
        <router-link to="/community/bills"><el-icon class="amber"><Tickets /></el-icon><span>待缴账单<strong>{{ summary.unpaid || 0 }}<small>笔</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
        <router-link to="/community/repairs"><el-icon class="blue"><Tools /></el-icon><span>进行中报修<strong>{{ summary.repairs || 0 }}<small>单</small></strong></span><el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <h2 class="section-title">日常服务</h2>
      <div class="quick-links"><router-link v-for="item in shortcuts" :key="item.path" :to="'/community/' + item.path"><el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span><el-icon><ArrowRight /></el-icon></router-link></div>
      <div class="section-heading"><h2>社区公告</h2><router-link to="/community/notices">全部公告 <el-icon><ArrowRight /></el-icon></router-link></div>
      <el-empty v-if="!records.length && !loading" description="暂无已发布公告" />
      <button v-for="notice in records.slice(0, 5)" :key="notice.id" class="notice-row" @click="detail = notice"><el-icon><Bell /></el-icon><span>{{ notice.title }}</span><time>{{ date(notice.publishTime) }}</time><el-icon><ArrowRight /></el-icon></button>
    </template>

    <template v-else-if="section === 'profile'">
      <div class="profile-heading"><el-avatar :size="64" :src="auth.userInfo?.avatar">{{ auth.userInfo?.realName?.slice(0, 1) || '业' }}</el-avatar><div><h2>{{ auth.userInfo?.realName || auth.userInfo?.username }}</h2><p>{{ auth.userInfo?.username }} · 普通业主</p></div></div>
      <el-tabs v-model="profileTab">
        <el-tab-pane label="个人资料" name="info">
          <el-form class="profile-form" label-position="top" @submit.prevent="saveProfile">
            <el-form-item label="姓名"><el-input v-model="profile.realName" maxlength="50" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="profile.phone" maxlength="11" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="profile.email" maxlength="100" /></el-form-item>
            <el-form-item label="性别"><el-radio-group v-model="profile.gender"><el-radio :value="0">未设置</el-radio><el-radio :value="1">男</el-radio><el-radio :value="2">女</el-radio></el-radio-group></el-form-item>
            <el-form-item label="生日"><el-date-picker v-model="profile.birthday" value-format="YYYY-MM-DD" type="date" /></el-form-item>
            <el-button type="primary" native-type="submit" :icon="Check" :loading="saving">保存资料</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="账户安全" name="security"><el-form class="profile-form" label-position="top" @submit.prevent="savePassword">
          <el-form-item label="当前密码"><el-input v-model="password.oldPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password autocomplete="new-password" placeholder="8–20位，包含字母和数字" /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="password.confirm" type="password" show-password autocomplete="new-password" /></el-form-item>
          <el-button type="primary" native-type="submit" :icon="Lock" :loading="saving">修改密码</el-button>
        </el-form></el-tab-pane>
      </el-tabs>
    </template>

    <template v-else>
      <div class="list-toolbar"><span>共 {{ total }} 条记录</span><el-button v-if="isService" type="primary" :icon="Plus" :disabled="summary.bound === false" @click="openService">{{ section === 'repairs' ? '提交报修' : '提交投诉建议' }}</el-button></div>
      <el-table v-if="!error" :data="records" empty-text="暂无记录" stripe @row-click="detail = $event" class="resident-table">
        <el-table-column v-for="column in columns" :key="column.key" :label="column.label" :min-width="column.width || 130" show-overflow-tooltip><template #default="{ row }">{{ format(row, column.key) }}</template></el-table-column>
        <el-table-column label="操作" width="90" fixed="right"><template #default="{ row }"><el-button link type="primary" @click.stop="detail = row">详情</el-button></template></el-table-column>
      </el-table>
      <el-pagination v-if="total > 10" v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" />
    </template>

    <el-dialog :model-value="!!detail" :title="detail?.title || title + '详情'" width="min(640px, calc(100vw - 32px))" @close="detail = null">
      <template v-if="detail">
        <dl class="detail-list"><template v-for="column in detailColumns" :key="column.key"><dt>{{ column.label }}</dt><dd>{{ format(detail, column.key) }}</dd></template></dl>
        <p v-if="detail.content" class="detail-content">{{ plainText(detail.content) }}</p>
        <div v-if="section === 'repairs' && detail.status === 3 && !detail.evaluateTime" class="evaluation"><h3>服务评价</h3><el-rate v-model="rating" /><el-input v-model="evaluation" type="textarea" :rows="3" maxlength="500" placeholder="评价内容（选填）" /><el-button type="primary" :loading="saving" @click="evaluate">提交评价</el-button></div>
      </template>
    </el-dialog>

    <el-dialog v-model="serviceDialog" :title="section === 'repairs' ? '提交报修' : '提交投诉建议'" width="min(620px, calc(100vw - 32px))" :close-on-click-modal="false">
      <el-form ref="serviceRef" :model="service" :rules="serviceRules" label-position="top" @submit.prevent="submitService">
        <el-form-item label="关联房屋" prop="houseId"><el-select v-model="service.houseId" placeholder="选择我的房屋" style="width: 100%"><el-option v-for="house in houseOptions" :key="house.id" :value="house.id" :label="houseLabel(house)" /></el-select></el-form-item>
        <el-form-item label="标题" prop="title"><el-input v-model="service.title" maxlength="100" show-word-limit /></el-form-item>
        <div class="form-grid"><el-form-item label="类型" prop="type"><el-select v-model="service.type"><el-option v-for="(label, index) in serviceTypes" :key="label" :label="label" :value="index + 1" /></el-select></el-form-item>
        <el-form-item v-if="section === 'repairs'" label="紧急程度"><el-select v-model="service.urgency"><el-option label="一般" :value="1" /><el-option label="紧急" :value="2" /><el-option label="特急" :value="3" /></el-select></el-form-item></div>
        <el-form-item label="详细描述" prop="content"><el-input v-model="service.content" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <div class="form-grid"><el-form-item label="联系人" prop="contactName"><el-input v-model="service.contactName" maxlength="50" /></el-form-item><el-form-item label="联系电话" prop="contactPhone"><el-input v-model="service.contactPhone" maxlength="11" /></el-form-item></div>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="serviceDialog = false">取消</el-button><el-button type="primary" :loading="saving" :icon="Check" @click="submitService">提交</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Refresh, Plus, Check, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getUserInfo, updateProfile, changePassword } from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import { residentNav } from './navigation'

type Row = Record<string, any>
type Column = { key: string; label: string; width?: number }
const auth = useAuthStore()
const route = useRoute()
const section = computed(() => String(route.meta.residentSection || 'home'))
const title = computed(() => String(route.meta.title || '智慧社区'))
const isService = computed(() => ['repairs', 'complaints'].includes(section.value))
const shortcuts = residentNav.filter(item => ['bills', 'repairs', 'complaints', 'phones', 'qa', 'marketplace', 'posts'].includes(item.path))
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const loading = ref(false), saving = ref(false), error = ref(''), page = ref(1), total = ref(0)
const records = ref<Row[]>([]), detail = ref<Row | null>(null), summary = ref<Row>({})
const profileTab = ref('info')
const profile = reactive({ realName: '', phone: '', email: '', gender: 0, birthday: '' })
const password = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const definitions: Record<string, Column[]> = {
  houses: [{ key: 'buildingName', label: '楼栋' }, { key: 'unitName', label: '单元' }, { key: 'houseNo', label: '房号' }, { key: 'area', label: '建筑面积（㎡）' }],
  bills: [{ key: 'billNo', label: '账单编号', width: 190 }, { key: 'itemName', label: '收费项目' }, { key: 'houseNo', label: '房号' }, { key: 'billPeriod', label: '账期' }, { key: 'payableAmount', label: '应缴金额（元）' }, { key: 'paidAmount', label: '已缴金额（元）' }, { key: 'status', label: '状态' }],
  repairs: [{ key: 'title', label: '报修标题', width: 180 }, { key: 'houseNo', label: '房号' }, { key: 'status', label: '状态' }, { key: 'createTime', label: '提交时间', width: 180 }],
  complaints: [{ key: 'title', label: '投诉 / 建议', width: 180 }, { key: 'houseNo', label: '房号' }, { key: 'status', label: '状态' }, { key: 'createTime', label: '提交时间', width: 180 }],
  notices: [{ key: 'title', label: '公告标题', width: 240 }, { key: 'publisher', label: '发布人' }, { key: 'publishTime', label: '发布时间', width: 180 }],
  activities: [{ key: 'title', label: '活动名称', width: 180 }, { key: 'location', label: '活动地点' }, { key: 'startTime', label: '开始时间', width: 180 }, { key: 'endTime', label: '结束时间', width: 180 }],
  phones: [{ key: 'name', label: '服务名称' }, { key: 'phone', label: '联系电话', width: 170 }, { key: 'workTime', label: '服务时间' }, { key: 'address', label: '地址', width: 200 }],
  vehicles: [{ key: 'plateNo', label: '车牌号码' }, { key: 'brand', label: '品牌' }, { key: 'color', label: '颜色' }]
}
const columns = computed(() => definitions[section.value] || [])
const detailColumns = computed(() => {
  const base = section.value === 'home' ? definitions.notices : columns.value
  return [...base, ...(isService.value ? [{ key: 'handleResult', label: '处理结果' }, { key: 'replyContent', label: '物业回复' }, { key: 'contactName', label: '联系人' }, { key: 'contactPhone', label: '联系电话' }] : [])]
})
function date(value: unknown) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function format(row: Row, key: string) {
  if (key === 'status') {
    const labels: Record<string, string[]> = { repairs: ['待处理', '已派单', '处理中', '已完成', '已取消'], complaints: ['待处理', '处理中', '已完成', '已关闭'], bills: ['待缴费', '部分缴费', '已缴清', '已作废'] }
    return labels[section.value]?.[row[key]] || '未知状态'
  }
  if (key.endsWith('Time')) return date(row[key])
  if (key.endsWith('Amount')) return Number(row[key] || 0).toFixed(2)
  return row[key] ?? '—'
}
function plainText(content: string) { return new DOMParser().parseFromString(content, 'text/html').body.textContent || '' }
function houseLabel(house: Row) { return [house.buildingName, house.unitName, house.houseNo].filter(Boolean).join(' / ') }
let loadSequence = 0
async function load() {
  const sequence = ++loadSequence, currentSection = section.value
  loading.value = true; error.value = ''
  try {
    const info: any = await request.get('/api/v1/resident/summary')
    if (sequence !== loadSequence) return
    summary.value = info
    if (currentSection === 'profile') {
      const user = await getUserInfo()
      if (sequence !== loadSequence) return
      auth.setUserInfo({ ...user, roles: [] }); Object.assign(profile, user)
    } else {
      if (info.bound === false && ['houses', 'bills', 'vehicles', 'repairs', 'complaints'].includes(currentSection)) { records.value = []; total.value = 0; return }
      const result: any = await request.get('/api/v1/resident/' + (currentSection === 'home' ? 'notices' : currentSection), { params: { page: page.value, pageSize: 10 } })
      if (sequence !== loadSequence) return
      records.value = result.records; total.value = result.total
    }
  } catch (ex: any) { if (sequence === loadSequence) error.value = ex.response?.data?.message || ex.message || '加载失败，请重试' }
  finally { if (sequence === loadSequence) loading.value = false }
}
watch(section, () => { page.value = 1; detail.value = null; serviceDialog.value = false; records.value = []; total.value = 0; void load() })

const serviceDialog = ref(false), serviceRef = ref<FormInstance>(), houseOptions = ref<Row[]>([])
const service = reactive({ houseId: undefined as number | undefined, title: '', content: '', type: 1, urgency: 1, contactName: '', contactPhone: '' })
const serviceTypes = computed(() => section.value === 'repairs' ? ['水电维修', '门窗维修', '墙面维修', '公共设施', '其他'] : ['物业服务', '环境卫生', '噪音扰民', '安全管理', '其他'])
const required = { required: true, message: '请填写此项', trigger: 'blur' }
const serviceRules = { houseId: [required], title: [required], content: [required], type: [required], contactName: [required], contactPhone: [required, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }] }
async function openService() {
  try {
    const first: any = await request.get('/api/v1/resident/houses', { params: { page: 1, pageSize: 50 } })
    const options = [...first.records]
    for (let p = 2; options.length < first.total; p++) {
      const batch: any = await request.get('/api/v1/resident/houses', { params: { page: p, pageSize: 50 } }); options.push(...batch.records)
      if (!batch.records.length) break
    }
    houseOptions.value = options
    if (!options.length) { ElMessage.warning('暂无关联房屋，请联系物业核实'); return }
    Object.assign(service, { houseId: options[0].id, title: '', content: '', type: 1, urgency: 1, contactName: auth.userInfo?.realName || '', contactPhone: auth.userInfo?.phone || '' })
    serviceDialog.value = true; serviceRef.value?.clearValidate()
  } catch { /* Request errors are displayed by the interceptor. */ }
}
async function submitService() {
  if (saving.value || !await serviceRef.value?.validate().catch(() => false)) return
  saving.value = true
  try { await request.post('/api/v1/resident/' + section.value, service); serviceDialog.value = false; ElMessage.success('提交成功'); page.value = 1; await load() } catch {} finally { saving.value = false }
}
const rating = ref(5), evaluation = ref('')
watch(detail, () => { rating.value = 5; evaluation.value = '' })
async function evaluate() {
  if (!detail.value || saving.value || !rating.value) return
  saving.value = true
  try { await request.post(`/api/v1/resident/repairs/${detail.value.id}/evaluate`, { score: rating.value, content: evaluation.value }); detail.value = null; ElMessage.success('评价成功'); await load() } catch {} finally { saving.value = false }
}
async function saveProfile() {
  if (saving.value) return
  saving.value = true
  try {
    const user = await updateProfile({ realName: profile.realName, phone: profile.phone, email: profile.email, gender: profile.gender, birthday: profile.birthday || undefined })
    auth.setUserInfo({ ...user, roles: [] }); ElMessage.success('资料已保存')
  } catch {} finally { saving.value = false }
}
async function savePassword() {
  if (saving.value) return
  if (!password.oldPassword || !/^(?=.*[A-Za-z])(?=.*\d).{8,20}$/.test(password.newPassword) || password.newPassword !== password.confirm) { ElMessage.warning('请填写当前密码，新密码需8–20位字母数字组合，且两次输入一致'); return }
  saving.value = true
  try { await changePassword({ oldPassword: password.oldPassword, newPassword: password.newPassword }); Object.assign(password, { oldPassword: '', newPassword: '', confirm: '' }); ElMessage.success('密码已修改') } catch {} finally { saving.value = false }
}
void load()
</script>

<style scoped lang="scss">
.resident-page { min-height: 360px; }
.page-heading, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.eyebrow { font-size: 12px; color: #7b8b83; margin-bottom: 8px; }
h1 { font-size: 24px; font-weight: 600; } h2 { font-size: 18px; font-weight: 600; } h3 { font-size: 15px; }
.feedback { margin-bottom: 20px; }
.welcome { display: grid; grid-template-columns: minmax(0, 1fr) 40%; align-items: center; gap: 24px; padding-bottom: 24px; border-bottom: 1px solid #dce5df; }
.welcome p { color: #176b5b; font-size: 13px; margin-bottom: 10px; }
.welcome h2 { font-size: 26px; margin-bottom: 18px; overflow-wrap: anywhere; }
.welcome span { font-size: 13px; color: #7d8b83; }
.welcome img { width: 100%; height: 180px; object-fit: cover; border-radius: 6px; }
.metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; padding: 24px 0; }
.metrics a { display: flex; align-items: center; gap: 14px; text-decoration: none; color: #65746c; padding: 8px 16px 8px 0; border-right: 1px solid #dce5df; font-size: 13px; }
.metrics a:last-child { border: 0; }
.metrics a > .el-icon:first-child { font-size: 28px; color: #176b5b; }
.metrics a > .el-icon.amber { color: #bc8738; }.metrics a > .el-icon.blue { color: #397aaf; }
.metrics a > .el-icon:last-child { margin-left: auto; }
.metrics strong { display: block; font-size: 28px; color: #273d32; margin-top: 8px; }.metrics small { font-size: 12px; margin-left: 8px; font-weight: 400; }
.section-title { margin: 6px 0 16px; }
.quick-links { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 32px; }
.quick-links a { display: flex; gap: 10px; align-items: center; text-decoration: none; color: #345648; background: white; border: 1px solid #e1e8e3; border-radius: 6px; padding: 20px 12px; font-size: 14px; }
.quick-links a:hover { border-color: #176b5b; }.quick-links a > .el-icon:last-child { margin-left: auto; font-size: 12px; }
.section-heading { margin-bottom: 8px; }.section-heading a { color: #176b5b; font-size: 13px; text-decoration: none; display: flex; align-items: center; gap: 6px; }
.notice-row { width: 100%; display: flex; align-items: center; gap: 14px; padding: 20px 0; border: 0; border-bottom: 1px solid #e1e8e3; background: transparent; text-align: left; cursor: pointer; color: #40574a; font-size: 14px; }
.notice-row span { flex: 1; overflow-wrap: anywhere; }.notice-row time { color: #86928a; font-size: 12px; flex-shrink: 0; }
.list-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; color: #75847d; font-size: 13px; gap: 12px; }
.resident-table { border: 1px solid #e2e8e5; border-radius: 6px; cursor: pointer; }.el-pagination { margin-top: 20px; justify-content: flex-end; }
.profile-heading { display: flex; align-items: center; gap: 18px; padding: 0 0 24px; }.profile-heading p { color: #7c8a82; font-size: 13px; margin-top: 8px; }
.profile-form { max-width: 460px; padding-top: 16px; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.form-grid .el-select { width: 100%; }.detail-list { display: grid; grid-template-columns: 110px minmax(0, 1fr); gap: 16px 12px; font-size: 14px; }.detail-list dt { color: #819086; }.detail-list dd { overflow-wrap: anywhere; }
.detail-content { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; margin-top: 24px; }.evaluation { margin-top: 24px; border-top: 1px solid #e1e8e3; padding-top: 16px; }.evaluation > * { margin-bottom: 12px; }
@media (max-width: 1000px) { .quick-links { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 580px) {
  .page-heading { margin-bottom: 20px; } h1 { font-size: 22px; }.welcome { gap: 14px; grid-template-columns: minmax(0, 1fr) 30%; }.welcome h2 { font-size: 20px; }.welcome img { height: 130px; }
  .metrics { gap: 8px; }.metrics a { gap: 6px; padding: 4px; flex-direction: column; align-items: flex-start; }.metrics a > .el-icon:last-child { display: none; }.metrics strong { font-size: 24px; }
  .notice-row { gap: 8px; }.notice-row time { max-width: 82px; }.quick-links { gap: 8px; }.quick-links a { padding: 16px 10px; }.form-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
