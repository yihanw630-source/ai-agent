<template>
  <section class="chat-page page-frame" :class="`theme-${aiTheme}`">
    <header class="chat-header">
      <button class="icon-button" type="button" aria-label="返回主页" @click="$emit('back')">
        ‹
      </button>
      <div class="chat-title">
        <div class="chat-avatar ai-avatar">{{ aiAvatarText }}</div>
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
      </div>
      <div v-if="chatId" class="chat-id" :title="chatId">会话 {{ chatId }}</div>
    </header>

    <main ref="messageListRef" class="message-list">
      <div v-if="messages.length === 0" class="empty-state">
        <div class="empty-avatar ai-avatar">{{ aiAvatarText }}</div>
        <h2>{{ emptyTitle }}</h2>
        <p>{{ emptyDescription }}</p>
      </div>

      <article
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="message.role === 'user' ? 'is-user' : 'is-ai'"
      >
        <div class="avatar" :class="message.role === 'user' ? 'user-avatar' : 'ai-avatar'">
          {{ message.role === 'user' ? '我' : aiAvatarText }}
        </div>
        <div class="bubble">
          <p>{{ message.content }}</p>
        </div>
      </article>
    </main>

    <form class="composer" @submit.prevent="sendMessage">
      <textarea
        v-model.trim="draft"
        :placeholder="placeholder"
        rows="1"
        @keydown.enter.exact.prevent="sendMessage"
      />
      <button type="submit" :disabled="!draft || streaming">
        {{ streaming ? '回复中' : '发送' }}
      </button>
    </form>

    <slot name="footer"></slot>
  </section>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref } from 'vue'
import { createSseUrl } from '../api/client'

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  placeholder: {
    type: String,
    default: '输入你的问题...',
  },
  emptyTitle: {
    type: String,
    default: '开始一段新对话',
  },
  emptyDescription: {
    type: String,
    default: '输入消息后，AI 会通过实时流式响应展示内容。',
  },
  aiAvatarText: {
    type: String,
    default: 'AI',
  },
  aiTheme: {
    type: String,
    default: 'manus',
    validator: (value) => ['love', 'manus'].includes(value),
  },
  chatId: {
    type: String,
    default: '',
  },
  endpoint: {
    type: String,
    required: true,
  },
  buildParams: {
    type: Function,
    required: true,
  },
  responseMode: {
    type: String,
    default: 'stream',
    validator: (value) => ['stream', 'finalChinese'].includes(value),
  },
})

defineEmits(['back'])

const draft = ref('')
const messages = ref([])
const streaming = ref(false)
const messageListRef = ref(null)
let eventSource = null
let responseBuffer = ''

function appendMessage(role, content) {
  const message = {
    id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    role,
    content,
  }

  messages.value.push(message)
  scrollToBottom()
  return message
}

function scrollToBottom() {
  nextTick(() => {
    const messageList = messageListRef.value
    if (messageList) {
      messageList.scrollTop = messageList.scrollHeight
    }
  })
}

function closeStream() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function isToolTrace(text) {
  return (
    /^Step\s+\d+\s*:/i.test(text) ||
    /^Tool\s+/i.test(text) ||
    /Tool\s+\w+\s+returned/i.test(text) ||
    /"position"\s*:/i.test(text) ||
    /"snippet"\s*:/i.test(text) ||
    /"displayed_link"\s*:/i.test(text)
  )
}

function hasChinese(text) {
  return /[\u4e00-\u9fff]/.test(text)
}

function formatFinalChineseAnswer(rawText) {
  const normalized = rawText
    .replace(/\r/g, '')
    .replace(/\[DONE\]/g, '')
    .replace(/\\n/g, '\n')
    .trim()

  const lines = normalized
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !isToolTrace(line))

  const chineseLines = lines.filter((line) => hasChinese(line))
  const answer = (chineseLines.length ? chineseLines : lines).join('\n').trim()

  if (!answer || !hasChinese(answer)) {
    return '已收到智能体的工具调用过程，但后端没有返回最终中文答复。请稍后重试，或在后端智能体提示词中要求“只输出最终中文答案，不输出工具调用过程”。'
  }

  return answer
}

function finishStream(aiMessage) {
  if (props.responseMode === 'finalChinese') {
    aiMessage.content = formatFinalChineseAnswer(responseBuffer)
  }

  closeStream()
  streaming.value = false
  responseBuffer = ''
  scrollToBottom()
}

function sendMessage() {
  if (!draft.value || streaming.value) {
    return
  }

  const userText = draft.value
  draft.value = ''
  responseBuffer = ''
  appendMessage('user', userText)
  const aiMessage = appendMessage(
    'ai',
    props.responseMode === 'finalChinese' ? '正在整理中文回复...' : '',
  )

  closeStream()
  streaming.value = true

  eventSource = new EventSource(
    createSseUrl(props.endpoint, props.buildParams({ message: userText, chatId: props.chatId })),
  )

  eventSource.onmessage = (event) => {
    if (!event.data || event.data === '[DONE]') {
      finishStream(aiMessage)
      return
    }

    if (props.responseMode === 'finalChinese') {
      responseBuffer += `${event.data}\n`
      return
    }

    aiMessage.content += event.data
    scrollToBottom()
  }

  eventSource.onerror = () => {
    if (props.responseMode === 'finalChinese' && responseBuffer) {
      finishStream(aiMessage)
      return
    }

    if (!aiMessage.content) {
      aiMessage.content = '连接中断，请稍后重试。'
    }
    closeStream()
    streaming.value = false
  }
}

onBeforeUnmount(() => {
  closeStream()
})
</script>
