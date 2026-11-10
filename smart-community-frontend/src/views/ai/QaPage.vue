<template>
  <div class="qa-page">
    <div class="chat-container">
      <div class="chat-header">
        <h2>
          <el-icon><ChatLineSquare /></el-icon>
          AI助手 - 智慧社区智能问答
        </h2>
        <el-button text @click="clearChat">清空对话</el-button>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-chat">
          <el-icon :size="60" color="#dcdfe6"><ChatDotRound /></el-icon>
          <p>您好，我是阳光花园智慧社区AI助手</p>
          <p>可以问我车位、账单、报修、活动、快递等问题</p>
          <div class="quick-questions">
            <el-tag v-for="q in quickQuestions" :key="q" @click="sendQuestion(q)" class="quick-tag">
              {{ q }}
            </el-tag>
          </div>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div class="message-avatar">
            <el-icon v-if="msg.role === 'user'" :size="28"><User /></el-icon>
            <el-icon v-else :size="28"><Cpu /></el-icon>
          </div>
          <div class="message-content">
            <div class="message-text" v-html="formatMessage(msg.content)"></div>
            <div class="message-time" v-if="msg.time">{{ msg.time }}</div>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="message-avatar">
            <el-icon :size="28"><Cpu /></el-icon>
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="输入您的问题，按 Enter 发送..."
          :disabled="loading"
          clearable
          @keyup.enter="sendQuestion(inputText)"
        >
          <template #append>
            <el-button :icon="Promotion" :disabled="!inputText.trim() || loading" @click="sendQuestion(inputText)" type="primary">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { isAxiosError } from 'axios'
import { ChatLineSquare, ChatDotRound, User, Cpu, Promotion } from '@element-plus/icons-vue'
import { askQuestionSync } from '@/api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
  time?: string
}

const inputText = ref('')
const loading = ref(false)
const messages = ref<Message[]>([])
const messagesContainer = ref<HTMLElement>()

const quickQuestions = [
  '小区还有几个空车位？在哪里？',
  '我欠物业费了吗？',
  '最近有什么社区活动？',
  '物业电话是多少？',
  '报修流程是什么？',
]

function formatMessage(text: string): string {
  if (!text) return ''
  return text.replace(/\n/g, '<br>')
}

function scrollToBottom() {
  nextTick(() => {
    const el = messagesContainer.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function addMessage(role: 'user' | 'assistant', content: string) {
  const time = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  messages.value.push({ role, content, time })
  scrollToBottom()
}

async function sendQuestion(text: string) {
  const question = text.trim()
  if (!question || loading.value) return

  inputText.value = ''
  addMessage('user', question)
  loading.value = true
  scrollToBottom()

  try {
    const res = await askQuestionSync(question)
    addMessage('assistant', res.answer || '抱歉，我暂时无法回答。')
  } catch (error) {
    const timedOut = isAxiosError(error) && (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT')
    addMessage('assistant', timedOut ? error.message : '抱歉，AI服务暂时不可用，请稍后再试。')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
.qa-page {
  height: calc(100vh - 120px);
  display: flex;
  justify-content: center;
}

.chat-container {
  width: 100%;
  max-width: 860px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;

  h2 {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: #303133;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
  gap: 8px;

  p {
    margin: 0;
    font-size: 14px;
  }

  p:first-of-type {
    margin-top: 16px;
    font-size: 16px;
    font-weight: 500;
    color: #606266;
  }
}

.quick-questions {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #409eff;
    color: #fff;
    border-color: #409eff;
  }
}

.message {
  display: flex;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-text {
      background: #409eff;
      color: #fff;
      border-radius: 12px 12px 4px 12px;
    }
  }

  &.assistant {
    flex-direction: row;

    .message-text {
      background: #f4f4f5;
      color: #303133;
      border-radius: 12px 12px 12px 4px;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e6e8eb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-text {
  padding: 10px 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: #f4f4f5;
  border-radius: 12px 12px 12px 4px;
  width: fit-content;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #c0c4cc;
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

.chat-input {
  padding: 16px 24px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
}
</style>
