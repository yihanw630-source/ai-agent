<template>
  <main class="app-shell">
    <section v-if="currentPage === 'home'" class="home page-frame">
      <nav class="top-nav" aria-label="主导航">
        <div class="logo-mark">
          <span>Y</span>
        </div>
        <div class="nav-copy">
          <strong>Yu AI Agent</strong>
          <span>Real-time AI Console</span>
        </div>
      </nav>

      <section class="hero">
        <div class="hero-copy">
          <p class="eyebrow">AI MATRIX ONLINE</p>
          <h1>启动你的 AI 智能体控制台</h1>
          <p class="lead">
            一个入口切换多种 AI 应用，SSE 实时流式响应，像操作未来控制台一样完成聊天、规划与执行。
          </p>

          <div class="hero-metrics" aria-label="平台特性">
            <div>
              <strong>2</strong>
              <span>AI 应用</span>
            </div>
            <div>
              <strong>SSE</strong>
              <span>实时响应</span>
            </div>
            <div>
              <strong>Vue3</strong>
              <span>前端驱动</span>
            </div>
          </div>
        </div>

        <div class="terminal-panel" aria-label="系统状态">
          <div class="terminal-bar">
            <span></span>
            <span></span>
            <span></span>
          </div>
          <pre>agent@yu:~$ boot --mode=hyper
CORE_STATUS: ONLINE
LOVE_APP: READY
MANUS_AGENT: READY
STREAM_CHANNEL: CONNECTED</pre>
        </div>
      </section>

      <section class="app-grid" aria-label="应用列表">
        <button class="app-card love" type="button" @click="openLoveApp">
          <span class="app-icon">恋</span>
          <span class="app-name">AI 恋爱大师</span>
          <strong>情感沟通、聊天建议、关系分析</strong>
          <em>Enter Love Protocol</em>
        </button>
        <button class="app-card manus" type="button" @click="openManusApp">
          <span class="app-icon">智</span>
          <span class="app-name">AI 超级智能体</span>
          <strong>工具调用、任务规划、智能执行</strong>
          <em>Launch Agent Core</em>
        </button>
      </section>

      <AppFooter />
    </section>

    <ChatRoom
      v-else-if="currentPage === 'love'"
      title="AI 恋爱大师"
      subtitle="把你的困惑讲出来，AI 会实时给出温柔又具体的建议。"
      placeholder="比如：她回复很慢，我该怎么继续聊？"
      empty-title="欢迎来到 AI 恋爱大师"
      empty-description="当前页面已自动创建聊天室 ID，用于区分不同会话。"
      ai-avatar-text="恋"
      ai-theme="love"
      :chat-id="loveChatId"
      endpoint="/ai/love_app/chat/sse"
      :build-params="buildLoveParams"
      @back="currentPage = 'home'"
    >
      <template #footer>
        <AppFooter />
      </template>
    </ChatRoom>

    <ChatRoom
      v-else
      title="AI 超级智能体"
      subtitle="描述你的目标，智能体会整理并展示最终中文回复。"
      placeholder="比如：帮我规划一个学习 Vue3 的路线"
      empty-title="欢迎来到 AI 超级智能体"
      empty-description="输入任务后，页面会隐藏工具调用过程，只展示最终中文内容。"
      ai-avatar-text="智"
      ai-theme="manus"
      endpoint="/ai/manus/chat"
      response-mode="finalChinese"
      :build-params="buildManusParams"
      @back="currentPage = 'home'"
    >
      <template #footer>
        <AppFooter />
      </template>
    </ChatRoom>
  </main>
</template>

<script setup>
import { computed, watchEffect } from 'vue'
import ChatRoom from './components/ChatRoom.vue'
import { createChatId } from './utils/chatId'
import { ref } from 'vue'

const currentPage = ref('home')
const loveChatId = ref('')

const seo = computed(() => {
  const configs = {
    home: {
      title: 'Yu AI Agent | 极客风 AI 智能体平台',
      description: 'Yu AI Agent 提供 AI 恋爱大师和 AI 超级智能体，支持 Vue3 与 SSE 实时流式聊天体验。',
    },
    love: {
      title: 'AI 恋爱大师 | Yu AI Agent',
      description: 'AI 恋爱大师提供情感沟通、聊天建议、关系分析和实时中文对话。',
    },
    manus: {
      title: 'AI 超级智能体 | Yu AI Agent',
      description: 'AI 超级智能体支持任务规划、工具调用和最终中文答案整理。',
    },
  }

  return configs[currentPage.value]
})

watchEffect(() => {
  document.title = seo.value.title
  setMetaContent('description', seo.value.description)
})

function setMetaContent(name, content) {
  let meta = document.querySelector(`meta[name="${name}"]`)
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', name)
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', content)
}

function openLoveApp() {
  loveChatId.value = createChatId('love')
  currentPage.value = 'love'
}

function openManusApp() {
  currentPage.value = 'manus'
}

function buildLoveParams({ message, chatId }) {
  return {
    message,
    chatId,
  }
}

function buildManusParams({ message }) {
  return {
    message: `系统指令：只输出最终中文答案，不输出工具调用过程；不要输出英文调试信息、JSON、Step 日志或乱码；答案必须完整、连贯、可直接给用户阅读。\n\n用户问题：${message}`,
  }
}
</script>

<script>
const currentYear = new Date().getFullYear()

export default {
  components: {
    AppFooter: {
      template: `
        <footer class="site-footer">
          <span>Copyright © ${currentYear} Yu AI Agent. All Rights Reserved.</span>
          <span>Powered by Vue3 · Axios · SSE</span>
        </footer>
      `,
    },
  },
}
</script>
