<template>
  <section class="social-page" v-loading="loading">
    <div class="social-heading"><div><p>智慧社区 / 邻里互动</p><h1>邻里二手</h1></div><el-button type="primary" :icon="Plus" @click="openEditor()">发布闲置</el-button></div>
    <div class="social-filters"><el-radio-group v-model="scope" @change="search"><el-radio-button value="all">在售物品</el-radio-button><el-radio-button value="mine">我的发布</el-radio-button></el-radio-group><el-select v-model="category" placeholder="全部分类" clearable @change="search"><el-option v-for="(label, index) in itemCategories" :key="label" :label="label" :value="index + 1" /></el-select><el-input v-model="keyword" maxlength="100" clearable placeholder="搜索闲置物品" @keyup.enter="search" @clear="search"><template #append><el-button :icon="Search" aria-label="搜索闲置物品" @click="search" /></template></el-input></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" /><p class="list-count">{{ total }} 件物品</p>
    <el-empty v-if="!records.length && !loading && !error" description="暂无物品" />
    <div class="market-grid">
      <article v-for="item in records" :key="item.id" class="market-item">
        <button class="item-image" :aria-label="'查看物品：' + item.title" @click="openDetail(item.id)">
          <img v-if="imageUrls(item.images).length" :src="imageUrls(item.images)[0]" :alt="item.title" loading="lazy" referrerpolicy="no-referrer" />
          <span v-else class="no-photo"><el-icon><component :is="categoryIcons[item.category - 1] || 'Goods'" /></el-icon><small>暂无图片</small></span>
          <span v-if="exampleImageCredit(item.images)" class="example-label">示意图</span>
          <span class="condition">{{ conditions[item.conditionLevel - 1] || '成色未填写' }}</span>
        </button>
        <div class="item-body"><button class="item-title" @click="openDetail(item.id)">{{ item.title }}</button><p class="item-price">¥ {{ money(item.price) }}<del v-if="item.originPrice">¥ {{ money(item.originPrice) }}</del></p><div class="item-meta"><span>{{ item.userName }}</span><el-tag v-if="scope === 'mine'" :type="item.status === 0 ? 'success' : 'info'" size="small">{{ ['在售', '已售', '已下架'][item.status] }}</el-tag><span v-else>{{ itemCategories[item.category - 1] }}</span></div></div>
      </article>
    </div>
    <el-pagination v-if="total > 12" v-model:current-page="page" :page-size="12" :total="total" layout="prev, pager, next" @current-change="load" />
    <el-dialog v-model="detailOpen" title="物品详情" width="min(720px, calc(100vw - 32px))">
      <div v-if="detail" class="market-detail">
        <div v-if="imageUrls(detail.images).length" class="photo-grid"><el-image v-for="url in imageUrls(detail.images)" :key="url" :src="url" :preview-src-list="imageUrls(detail.images)" preview-teleported fit="contain" /></div>
        <p v-if="detailCredit" class="image-credit">商品示意图，非卖家实拍，具体成色及配件以卖家提供的信息为准。<br />图片：<a :href="detailCredit.source" target="_blank" rel="noopener noreferrer">{{ detailCredit.author || 'Wikimedia Commons' }}</a> · <a :href="detailCredit.licenseUrl" target="_blank" rel="noopener noreferrer">{{ detailCredit.license }}</a>（缩放及留白）</p>
        <el-tag :type="detail.status === 0 ? 'success' : 'info'">{{ ['在售', '已售', '已下架'][detail.status] }}</el-tag>
        <h2>{{ detail.title }}</h2>
        <p class="item-price">¥ {{ money(detail.price) }}<del v-if="detail.originPrice">¥ {{ money(detail.originPrice) }}</del></p>
        <p class="muted">{{ itemCategories[detail.category - 1] }} · {{ conditions[detail.conditionLevel - 1] }} · {{ detail.tradeMethod === 1 ? '自提' : '快递' }}</p>
        <p class="body-copy">{{ detail.description }}</p>
        <dl class="contact-info"><dt>发布人</dt><dd>{{ detail.userName }}</dd><dt>发布时间</dt><dd>{{ displayDate(detail.createTime) }}</dd><dt>联系人</dt><dd>{{ detail.contactName || '未填写' }}</dd><dt>联系电话</dt><dd><a v-if="detail.contactPhone" :href="'tel:' + detail.contactPhone">{{ detail.contactPhone }}</a><span v-else>未填写</span></dd></dl>
        <div v-if="detail.userId === auth.userInfo?.id" class="owner-actions"><template v-if="detail.status === 0"><el-button :icon="EditPen" @click="openEditor(detail)">编辑</el-button><el-button type="success" plain :icon="Check" @click="changeStatus(1)">标记已售</el-button><el-button :icon="SoldOut" @click="changeStatus(2)">下架</el-button></template><el-button type="danger" plain :icon="Delete" @click="removeItem">删除</el-button></div>
        <a v-else-if="detail.status === 0 && detail.contactPhone" class="contact-seller" :href="'tel:' + detail.contactPhone"><el-icon><Phone /></el-icon>联系卖家</a>
      </div>
    </el-dialog>
    <el-dialog v-model="editor" :title="editId ? '编辑闲置物品' : '发布闲置'" width="min(680px, calc(100vw - 32px))" :close-on-click-modal="false" :show-close="!saving && !uploading" :close-on-press-escape="!saving && !uploading" :before-close="closeEditor"><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="物品名称" prop="title"><el-input v-model="form.title" maxlength="200" /></el-form-item><div class="editor-grid"><el-form-item label="分类" prop="category"><el-select v-model="form.category"><el-option v-for="(label, index) in itemCategories" :key="label" :label="label" :value="index + 1" /></el-select></el-form-item><el-form-item label="成色"><el-select v-model="form.conditionLevel"><el-option v-for="(label, index) in conditions" :key="label" :label="label" :value="index + 1" /></el-select></el-form-item><el-form-item label="售价（元）" prop="price"><el-input-number v-model="form.price" :min="0" :max="99999999.99" :precision="2" :controls="false" /></el-form-item><el-form-item label="原价（元）"><el-input-number v-model="form.originPrice" :min="0" :max="99999999.99" :precision="2" :controls="false" /></el-form-item></div><el-form-item label="物品描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="5000" show-word-limit /></el-form-item><el-form-item label="物品图片"><CommunityImages :key="editId || 'new'" v-model="form.images" @uploading="uploading = $event" /></el-form-item><div class="editor-grid"><el-form-item label="联系人" prop="contactName"><el-input v-model="form.contactName" maxlength="50" /></el-form-item><el-form-item label="联系电话" prop="contactPhone"><el-input v-model="form.contactPhone" maxlength="11" /></el-form-item></div><el-form-item label="交易方式"><el-radio-group v-model="form.tradeMethod"><el-radio :value="1">自提</el-radio><el-radio :value="2">快递</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button :disabled="saving || uploading" @click="editor = false">取消</el-button><el-button type="primary" :icon="Check" :loading="saving" :disabled="uploading" @click="save">{{ editId ? '保存修改' : '发布' }}</el-button></template></el-dialog>
  </section>
</template>
<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Search, EditPen, Check, SoldOut, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import request from '@/utils/request'
import { itemList, itemDetail, imageUrls, displayDate, exampleImageCredit, itemCategories, conditions, socialBase, type CommunityItem } from '@/api/community'
import CommunityImages from './CommunityImages.vue'
import './social.scss'
const auth = useAuthStore(), categoryIcons = ['OfficeBuilding', 'Refrigerator', 'Monitor', 'Present', 'Reading', 'Goods']
const loading = ref(false), saving = ref(false), error = ref(''), scope = ref('all'), category = ref<number>(), keyword = ref(''), page = ref(1), total = ref(0), records = ref<CommunityItem[]>([])
const detail = ref<CommunityItem | null>(null), detailOpen = ref(false)
const detailCredit = computed(() => exampleImageCredit(detail.value?.images))
const money = (value: number) => Number(value || 0).toFixed(2)
let sequence = 0
async function load() { const current = ++sequence; loading.value = true; error.value = ''; try { const result = await itemList({ page: page.value, mine: scope.value === 'mine', category: category.value || undefined, keyword: keyword.value }); if (current !== sequence) return; records.value = result.records; total.value = result.total } catch (ex: any) { if (current === sequence) error.value = ex.message || '加载失败，请重试' } finally { if (current === sequence) loading.value = false } }
function search() { page.value = 1; void load() }
async function openDetail(id: number) { try { detail.value = await itemDetail(id); detailOpen.value = true } catch {} }
const editor = ref(false), editId = ref<number>(), uploading = ref(false), formRef = ref<FormInstance>()
const form = reactive({ title: '', description: '', category: 1, price: 0, originPrice: undefined as number | undefined, conditionLevel: 2, images: [] as string[], contactName: '', contactPhone: '', tradeMethod: 1 })
const required = { required: true, message: '请填写此项', trigger: 'blur' }, rules = { title: [required], description: [required], category: [required], price: [required], contactName: [required], contactPhone: [required, { pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }] }
function openEditor(item?: CommunityItem) { editId.value = item?.id; Object.assign(form, { title: item?.title || '', description: item?.description || '', category: item?.category || 1, price: Number(item?.price || 0), originPrice: item?.originPrice == null ? undefined : Number(item.originPrice), conditionLevel: item?.conditionLevel || 2, images: imageUrls(item?.images), contactName: item?.contactName || auth.userInfo?.realName || '', contactPhone: item?.contactPhone || auth.userInfo?.phone || '', tradeMethod: item?.tradeMethod || 1 }); detailOpen.value = false; editor.value = true; void nextTick(() => formRef.value?.clearValidate()) }
function closeEditor(done: () => void) { if (!saving.value && !uploading.value) done() }
async function save() { if (saving.value || uploading.value || !await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { if (editId.value) await request.put(`${socialBase}/items/${editId.value}`, form); else await request.post(socialBase + '/items', form); editor.value = false; ElMessage.success('物品已保存'); page.value = 1; await load() } catch {} finally { saving.value = false } }
async function changeStatus(status: number) { if (!detail.value || saving.value) return; try { await ElMessageBox.confirm(status === 1 ? '确认此物品已经售出？' : '确认下架此物品？', '更新物品状态', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); saving.value = true; await request.put(`${socialBase}/items/${detail.value.id}/status`, { status }); detailOpen.value = false; await load() } catch {} finally { saving.value = false } }
async function removeItem() { if (!detail.value || saving.value) return; try { await ElMessageBox.confirm('确认删除此物品？', '删除物品', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); saving.value = true; await request.delete(`${socialBase}/items/${detail.value.id}`); detailOpen.value = false; if (records.value.length === 1 && page.value > 1) page.value--; await load() } catch {} finally { saving.value = false } }
void load()
</script>
