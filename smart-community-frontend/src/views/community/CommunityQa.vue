<template>
  <section class="qa-community">
    <div class="heading"><div><p>智慧社区 / 业主服务</p><h1>智能问答</h1></div><el-tooltip content="最近问答" placement="bottom"><el-button :icon="Clock" circle aria-label="最近问答" @click="openHistory" /></el-tooltip></div>
    <div class="chat" ref="chat" aria-live="polite" aria-label="社区智能问答">
      <div v-if="!messages.length" class="welcome"><el-icon><ChatDotRound /></el-icon><h2>您好，{{ auth.userInfo?.realName || '邻居' }}</h2><div class="suggestions"><el-button v-for="question in questions" :key="question" @click="send(question)">{{ question }}</el-button></div></div>
      <article v-for="(message, index) in messages" :key="index" :class="['message', message.role]"><el-avatar :size="32"><el-icon><component :is="message.role === 'user' ? 'User' : 'Cpu'" /></el-icon></el-avatar><div><span>{{ message.role === 'user' ? '我' : '社区助手' }}</span><p class="message-text">{{ message.content }}</p></div></article>
      <article v-if="loading" class="message assistant"><el-avatar :size="32"><el-icon><Cpu /></el-icon></el-avatar><div><span>社区助手</span><p class="pending"><el-icon class="is-loading"><Loading /></el-icon> 正在查询</p></div></article>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <form class="compose" @submit.prevent="send(input)"><el-input v-model="input" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" maxlength="2000" aria-label="咨询问题" placeholder="请输入您的问题" :disabled="loading" /><el-button type="primary" native-type="submit" :icon="Promotion" :loading="loading" :disabled="!input.trim()">发送</el-button></form>
    <el-drawer v-model="historyOpen" title="最近问答" size="min(440px, 100vw)"><div v-loading="historyLoading"><el-empty v-if="!history.length && !historyLoading" description="暂无问答记录" /><article v-for="entry in history" :key="entry.id" class="history-entry"><time>{{ displayDate(entry.createTime) }}</time><h3>{{ entry.question }}</h3><p>{{ entry.answer }}</p></article><el-pagination v-if="historyTotal > 20" v-model:current-page="historyPage" :total="historyTotal" :page-size="20" layout="prev, pager, next" @current-change="loadHistory" /></div></el-drawer>
  </section>
</template>
<script setup lang="ts">
import { ref, nextTick, onBeforeUnmount } from 'vue'
import { Clock, Promotion } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { beginQuestion, questionResult, qaHistory, displayDate, type QaEntry } from '@/api/community'
const auth = useAuthStore(), chat = ref<HTMLElement>()
const input = ref(''), loading = ref(false), error = ref('')
const messages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const questions = ['小区还有哪些空车位？', '我有未缴清的账单吗？', '我的报修处理到哪一步了？', '物业联系电话是多少？']
let active = true, timer: ReturnType<typeof setTimeout> | undefined, releaseWait: (() => void) | undefined
const scroll = () => nextTick(() => chat.value?.scrollTo({ top: chat.value.scrollHeight, behavior: 'smooth' }))
onBeforeUnmount(() => { active = false; clearTimeout(timer); releaseWait?.() })
async function send(value: string) {
  const question = value.trim()
  if (!question || loading.value) return
  messages.value.push({ role: 'user', content: question }); input.value = ''; error.value = ''; loading.value = true; void scroll()
  try {
    const job = await beginQuestion(question)
    const deadline = Date.now() + 180000
    while (active && Date.now() < deadline) {
      const result = await questionResult(job.id)
      if (!active) return
      if (result.state === 'failed') throw new Error(result.message || '智能问答暂时不可用')
      if (result.state === 'completed') { messages.value.push({ role: 'assistant', content: result.response?.answer || '暂未获取到回答，请稍后重试' }); return }
      await new Promise<void>(resolve => { releaseWait = resolve; timer = setTimeout(resolve, 1200) })
    }
    if (active) throw new Error('回答仍在处理中，请稍后查看最近问答记录')
  } catch (ex: any) { if (active) error.value = ex.message || '智能问答暂时不可用，请稍后重试' }
  finally { if (active) { loading.value = false; void scroll() } }
}
const historyOpen = ref(false), historyLoading = ref(false), history = ref<QaEntry[]>([]), historyTotal = ref(0), historyPage = ref(1)
async function loadHistory() { historyLoading.value = true; try { const result = await qaHistory(historyPage.value); history.value = result.records; historyTotal.value = result.total } catch {} finally { historyLoading.value = false } }
function openHistory() { historyOpen.value = true; historyPage.value = 1; void loadHistory() }
</script>
<style scoped lang="scss">
.qa-community { min-width: 0; }.heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; gap: 16px; }.heading p { color: #7b8b83; font-size: 12px; margin-bottom: 8px; }h1 { font-size: 24px; font-weight: 600; }.chat { height: max(350px, calc(100dvh - 355px)); max-height: 750px; overflow-y: auto; padding: 20px 0; border-top: 1px solid #dce5df; }.welcome { min-height: 320px; display: flex; flex-direction: column; justify-content: center; align-items: center; gap: 22px; }.welcome > .el-icon { font-size: 44px; color: #176b5b; }h2 { font-size: 21px; }.suggestions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; max-width: 100%; }.suggestions .el-button { margin: 0; height: auto; white-space: normal; line-height: 1.6; min-height: 40px; }.message { display: flex; gap: 12px; margin-bottom: 24px; }.message > div { min-width: 0; max-width: 88%; }.message span { font-size: 12px; color: #7b8b83; }.message-text { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.8; font-size: 14px; padding: 12px 0; }.message.user { flex-direction: row-reverse; }.user .message-text { padding: 10px 14px; background: #e4efeb; border-radius: 6px; margin-top: 6px; }.assistant .el-avatar { background: #176b5b; flex-shrink: 0; }.pending { padding: 12px 0; color: #708379; font-size: 14px; }.compose { border-top: 1px solid #dce5df; padding-top: 18px; display: flex; align-items: flex-end; gap: 12px; }.history-entry { padding: 18px 0; border-bottom: 1px solid #e2e8e3; }time { color: #7d8b83; font-size: 12px; }.history-entry h3 { font-size: 15px; margin: 10px 0; overflow-wrap: anywhere; }.history-entry p { white-space: pre-wrap; overflow-wrap: anywhere; font-size: 14px; line-height: 1.8; }
@media (max-width: 580px) { .suggestions { grid-template-columns: 1fr; }.chat { height: max(400px, calc(100dvh - 380px)); }.compose { gap: 8px; }.compose > .el-button { padding: 10px; } }
</style>
