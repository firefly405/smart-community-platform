<template>
  <section class="social-page" v-loading="loading">
    <div class="social-heading"><div><p>智慧社区 / 邻里互动</p><h1>{{ postId ? '帖子详情' : '社区广场' }}</h1></div><el-button v-if="!postId" type="primary" :icon="EditPen" @click="openEditor()">发布帖子</el-button><el-button v-else :icon="Back" @click="router.push('/community/posts')">返回广场</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <template v-if="!postId">
      <div class="social-filters"><el-radio-group v-model="scope" @change="search"><el-radio-button value="all">公共帖子</el-radio-button><el-radio-button value="mine">我的帖子</el-radio-button></el-radio-group><el-select v-model="category" placeholder="全部分类" clearable @change="search"><el-option v-for="(label, index) in postCategories" :key="label" :label="label" :value="index + 1" /></el-select><el-input v-model="keyword" maxlength="100" clearable placeholder="搜索帖子" @keyup.enter="search" @clear="search"><template #append><el-button :icon="Search" aria-label="搜索帖子" @click="search" /></template></el-input></div>
      <p class="list-count">{{ total }} 篇帖子</p>
      <el-empty v-if="!records.length && !loading && !error" description="暂无帖子" />
      <article v-for="post in records" :key="post.id" class="forum-row"><div class="author"><el-avatar :size="32">{{ post.userName?.slice(0, 1) || '邻' }}</el-avatar><span>{{ post.userName }}<small>{{ displayDate(post.createTime) }}</small></span><el-tag v-if="post.status !== 1" type="info">{{ post.status === 2 ? '违规内容' : '已隐藏' }}</el-tag><el-tag v-if="post.isTop" type="warning" effect="plain">置顶</el-tag></div><router-link class="post-title" :to="'/community/posts/' + post.id">{{ post.title }}</router-link><p class="excerpt">{{ post.content }}</p><div class="post-footer"><span>{{ postCategories[Number(post.category) - 1] || '其他' }}</span><router-link :to="'/community/posts/' + post.id"><el-icon><ChatLineSquare /></el-icon>{{ post.commentCount }} 条评论</router-link></div></article>
      <el-pagination v-if="total > 10" v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" />
    </template>
    <template v-else-if="detail">
      <article class="post-detail"><div class="author"><el-avatar :size="36">{{ detail.userName?.slice(0, 1) || '邻' }}</el-avatar><span>{{ detail.userName }}<small>{{ displayDate(detail.createTime) }}</small></span><el-tag effect="plain">{{ postCategories[Number(detail.category) - 1] || '其他' }}</el-tag></div><h2>{{ detail.title }}</h2><p class="body-copy">{{ detail.content }}</p><div v-if="imageUrls(detail.images).length" class="photo-grid"><el-image v-for="url in imageUrls(detail.images)" :key="url" :src="url" :preview-src-list="imageUrls(detail.images)" preview-teleported fit="contain" /></div><div v-if="detail.userId === auth.userInfo?.id" class="owner-actions"><el-button v-if="detail.status === 1" :icon="EditPen" @click="openEditor(detail)">编辑帖子</el-button><el-button type="danger" plain :icon="Delete" @click="removePost">删除帖子</el-button></div></article>
      <template v-if="detail.status === 1">
        <div class="discussion-heading"><h2>评论 <small>{{ commentTotal }}</small></h2></div>
        <el-empty v-if="!comments.length && !commentLoading" description="还没有评论" />
        <div v-loading="commentLoading"><article v-for="comment in comments" :key="comment.id" class="comment-row"><div class="author"><el-avatar :size="28">{{ comment.userName?.slice(0, 1) || '邻' }}</el-avatar><span>{{ comment.userName }}<small>{{ displayDate(comment.createTime) }}</small></span></div><p v-if="comment.parentId" class="reply-reference">回复 #{{ comment.parentId }}</p><p class="body-copy">{{ comment.content }}</p><div class="comment-actions"><span>#{{ comment.id }}</span><el-button link type="primary" @click="replyTo = comment; focusComment()">回复</el-button><el-button v-if="comment.userId === auth.userInfo?.id" link type="danger" @click="removeComment(comment)">删除</el-button></div></article></div>
        <el-pagination v-if="commentTotal > 20" v-model:current-page="commentPage" :page-size="20" :total="commentTotal" layout="prev, pager, next" @current-change="loadComments" />
        <form class="comment-compose" @submit.prevent="submitComment"><div v-if="replyTo" class="reply-target"><span>回复 {{ replyTo.userName }} · #{{ replyTo.id }}</span><el-button link :icon="Close" aria-label="取消回复" @click="replyTo = null" /></div><el-input ref="commentInput" v-model="commentText" aria-label="评论内容" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="分享您的看法" /><el-button type="primary" :icon="Promotion" native-type="submit" :loading="saving" :disabled="!commentText.trim()">{{ replyTo ? '提交回复' : '发表评论' }}</el-button></form>
      </template><el-alert v-else title="此帖已隐藏或不可评论" type="info" :closable="false" />
    </template>
    <el-dialog v-model="editor" :title="editId ? '编辑帖子' : '发布帖子'" width="min(680px, calc(100vw - 32px))" :close-on-click-modal="false" :show-close="!saving && !uploading" :close-on-press-escape="!saving && !uploading" :before-close="closeEditor">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="200" show-word-limit /></el-form-item><el-form-item label="分类" prop="category"><el-select v-model="form.category"><el-option v-for="(label, index) in postCategories" :key="label" :label="label" :value="index + 1" /></el-select></el-form-item><el-form-item label="正文" prop="content"><el-input v-model="form.content" type="textarea" :rows="7" maxlength="5000" show-word-limit /></el-form-item><el-form-item label="图片"><CommunityImages :key="editId || 'new'" v-model="form.images" @uploading="uploading = $event" /></el-form-item></el-form>
      <template #footer><el-button :disabled="saving || uploading" @click="editor = false">取消</el-button><el-button type="primary" :icon="Check" :loading="saving" :disabled="uploading" @click="savePost">{{ editId ? '保存修改' : '发布' }}</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type InputInstance } from 'element-plus'
import { EditPen, Back, Search, Delete, Close, Promotion, Check } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import request from '@/utils/request'
import { postList, postDetail, commentList, socialBase, postCategories, imageUrls, displayDate, type CommunityPost, type CommunityComment } from '@/api/community'
import CommunityImages from './CommunityImages.vue'
import './social.scss'
const route = useRoute(), router = useRouter(), auth = useAuthStore()
const postId = computed(() => route.params.id ? Number(route.params.id) : null)
const records = ref<CommunityPost[]>([]), detail = ref<CommunityPost | null>(null), comments = ref<CommunityComment[]>([])
const loading = ref(false), commentLoading = ref(false), saving = ref(false), error = ref(''), scope = ref('all'), category = ref<number>(), keyword = ref(''), page = ref(1), total = ref(0)
const commentPage = ref(1), commentTotal = ref(0), replyTo = ref<CommunityComment | null>(null), commentText = ref(''), commentInput = ref<InputInstance>()
let sequence = 0
async function load() {
  const current = ++sequence; loading.value = true; error.value = ''
  try {
    if (postId.value) { const post = await postDetail(postId.value); if (current !== sequence) return; detail.value = post; if (post.status === 1) await loadComments() }
    else { const result = await postList({ page: page.value, mine: scope.value === 'mine', category: category.value || undefined, keyword: keyword.value }); if (current !== sequence) return; records.value = result.records; total.value = result.total }
  } catch (ex: any) { if (current === sequence) error.value = ex.message || '加载失败，请重试' } finally { if (current === sequence) loading.value = false }
}
function search() { page.value = 1; void load() }
async function loadComments() {
  const id = postId.value; if (!id) return
  commentLoading.value = true
  try { const result = await commentList(id, commentPage.value); if (postId.value !== id) return; comments.value = result.records; commentTotal.value = result.total } catch {} finally { commentLoading.value = false }
}
watch(() => route.fullPath, () => { detail.value = null; comments.value = []; replyTo.value = null; commentText.value = ''; commentPage.value = 1; void load() }, { immediate: true })
function focusComment() { void nextTick(() => { commentInput.value?.focus(); commentInput.value?.$el.scrollIntoView({ block: 'center', behavior: 'smooth' }) }) }
async function submitComment() {
  if (saving.value || !postId.value || !commentText.value.trim()) return
  saving.value = true
  try { await request.post(`${socialBase}/posts/${postId.value}/comments`, { content: commentText.value.trim(), parentId: replyTo.value?.id }); commentText.value = ''; replyTo.value = null; ElMessage.success('评论已发布'); await loadComments() } catch {} finally { saving.value = false }
}
async function removeComment(comment: CommunityComment) {
  try { await ElMessageBox.confirm('删除这条评论？', '删除评论', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); await request.delete(`${socialBase}/comments/${comment.id}`); if (comments.value.length === 1 && commentPage.value > 1) commentPage.value--; await loadComments() } catch {}
}
async function removePost() {
  if (!postId.value) return
  try { await ElMessageBox.confirm('删除此帖后将不再公开展示。', '删除帖子', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }); await request.delete(`${socialBase}/posts/${postId.value}`); ElMessage.success('帖子已删除'); await router.replace('/community/posts') } catch {}
}
const editor = ref(false), editId = ref<number>(), uploading = ref(false), formRef = ref<FormInstance>()
const form = reactive({ title: '', content: '', category: 2, images: [] as string[] })
const required = { required: true, message: '请填写此项', trigger: 'blur' }, rules = { title: [required], content: [required], category: [required] }
function openEditor(post?: CommunityPost) { editId.value = post?.id; Object.assign(form, { title: post?.title || '', content: post?.content || '', category: Number(post?.category || 2), images: imageUrls(post?.images) }); editor.value = true; void nextTick(() => formRef.value?.clearValidate()) }
function closeEditor(done: () => void) { if (!saving.value && !uploading.value) done() }
async function savePost() {
  if (saving.value || uploading.value || !await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try { const id = editId.value ? (await request.put(`${socialBase}/posts/${editId.value}`, form), editId.value) : await request.post<any, number>(socialBase + '/posts', form); editor.value = false; ElMessage.success('帖子已保存'); if (postId.value === id) await load(); else await router.push('/community/posts/' + id) } catch {} finally { saving.value = false }
}
</script>
