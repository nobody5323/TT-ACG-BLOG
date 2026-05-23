<script setup>
import { computed, nextTick, ref } from "vue";
import { sendAiChat, uploadAiFile } from "../../api/ai";

const isOpen = ref(false);
const isMinimized = ref(false);
const isSending = ref(false);
const isUploading = ref(false);
const input = ref("");
const uploadNotice = ref("");
const errorMessage = ref("");
const messages = ref([
  {
    id: "welcome",
    role: "assistant",
    text: "你好呀！我是青空小助手～\n可以帮你写文章灵感、优化标题、整理大纲呦 ✨",
    citations: [],
  },
]);

const suggestions = [
  "帮我想几个文章标题",
  "根据主题生成大纲",
  "优化这段文字",
  "推荐热门标签",
];

const sessionId = `web-session-${Date.now()}`;
const userId = "web-guest";
const personaId = "default-anime-assistant";
const messageListRef = ref(null);

const canSend = computed(() => input.value.trim() && !isSending.value);

function toggleWindow() {
  isOpen.value = !isOpen.value;
  isMinimized.value = false;
}

function minimizeWindow() {
  isMinimized.value = !isMinimized.value;
}

function clearMessages() {
  messages.value = [
    {
      id: "welcome",
      role: "assistant",
      text: "你好呀！我是青空小助手～\n可以帮你写文章灵感、优化标题、整理大纲呦 ✨",
      citations: [],
    },
  ];
  errorMessage.value = "";
  uploadNotice.value = "";
}

function pushMessage(role, text, citations = []) {
  messages.value.push({
    id: `${role}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role,
    text,
    citations,
  });
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
}

async function submitChat(text) {
  const message = (text || input.value).trim();
  if (!message || isSending.value) return;

  errorMessage.value = "";
  input.value = "";
  pushMessage("user", message);
  isSending.value = true;

  try {
    const result = await sendAiChat({
      user_id: userId,
      session_id: sessionId,
      persona_id: personaId,
      message,
    });
    pushMessage("assistant", result.answer, result.citations || []);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "AI 对话失败";
    pushMessage("assistant", "抱歉，请求失败了。请稍后再试哦～ 💦");
  } finally {
    isSending.value = false;
  }
}

function useSuggestion(text) {
  submitChat(text);
}

async function handleFileChange(event) {
  const [file] = event.target.files || [];
  event.target.value = "";
  if (!file) return;

  uploadNotice.value = "";
  errorMessage.value = "";
  isUploading.value = true;

  try {
    const result = await uploadAiFile(file);
    uploadNotice.value = `已上传 ${file.name}，写入 ${result.chunks} 个片段。`;
    pushMessage("assistant", `文件 ${file.name} 已入库，现在可以继续提问相关内容啦～ ✨`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "文件上传失败";
  } finally {
    isUploading.value = false;
  }
}

function handleKeydown(e) {
  if (e.key === "Enter" && !e.shiftKey) {
    e.preventDefault();
    submitChat();
  }
}
</script>

<template>
  <div class="ai-window">
    <!-- Floating Toggle Button -->
    <button
      class="ai-window__toggle"
      type="button"
      :aria-expanded="isOpen"
      aria-label="切换 AI 助手"
      @click="toggleWindow"
    >
      {{ isOpen ? '✕' : '✨' }}
    </button>

    <!-- Panel -->
    <transition name="ai-slide">
      <div v-if="isOpen" class="ai-panel" :class="{ 'ai-panel--minimized': isMinimized }">
        <!-- Header -->
        <div class="ai-panel__head">
          <div class="ai-panel__title">
            <span style="font-size:1.1rem">🌟</span>
            青空小助手
            <span class="ai-badge">AI</span>
          </div>
          <div class="ai-panel__head-actions">
            <button class="ai-panel__head-btn" type="button" title="清空对话" @click="clearMessages">🔄</button>
            <button class="ai-panel__head-btn" type="button" title="最小化" @click="minimizeWindow">{{ isMinimized ? '▢' : '—' }}</button>
          </div>
        </div>

        <!-- Body (hideable on minimize) -->
        <template v-if="!isMinimized">
          <!-- Messages -->
          <div ref="messageListRef" class="ai-panel__body">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['ai-message', msg.role === 'user' ? 'ai-message--user' : 'ai-message--bot']"
            >
              <div v-if="msg.role === 'assistant'" class="ai-message__avatar">🌟</div>
              <div class="ai-message__bubble">
                <span style="white-space:pre-wrap">{{ msg.text }}</span>
                <small v-if="msg.citations?.length" style="display:block;margin-top:4px;opacity:0.7;font-size:0.72rem">
                  来源：{{ msg.citations.join("、") }}
                </small>
              </div>
            </div>

            <!-- Typing indicator -->
            <div v-if="isSending" class="ai-message ai-message--bot">
              <div class="ai-message__avatar">🌟</div>
              <div class="ai-message__bubble ai-typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>

          <!-- Suggestions -->
          <div v-if="messages.length <= 2" class="ai-panel__suggestions">
            <button
              v-for="s in suggestions"
              :key="s"
              class="ai-suggestion"
              type="button"
              @click="useSuggestion(s)"
            >
              {{ s }}
            </button>
          </div>

          <!-- Upload notice -->
          <div v-if="uploadNotice" class="ai-panel__notice">{{ uploadNotice }}</div>
          <div v-if="errorMessage" class="ai-panel__notice ai-panel__notice--error">{{ errorMessage }}</div>

          <!-- Input -->
          <div class="ai-panel__input">
            <label class="ai-panel__upload-btn" title="上传文件">
              <input
                type="file"
                accept=".txt,.md,.markdown,.html,.htm"
                style="display:none"
                :disabled="isUploading"
                @change="handleFileChange"
              />
              {{ isUploading ? '⏳' : '📎' }}
            </label>
            <input
              v-model="input"
              type="text"
              placeholder="输入你的问题…"
              :disabled="isSending"
              @keydown="handleKeydown"
            />
            <button
              class="ai-panel__send"
              type="button"
              :disabled="!canSend"
              @click="submitChat()"
            >
              ➤
            </button>
          </div>
        </template>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.ai-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #8b5cf6, #06b6d4);
  letter-spacing: 0.05em;
}

.ai-panel__head-actions {
  display: flex;
  gap: 4px;
}

.ai-panel__head-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--muted);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 0.8rem;
  transition: all 150ms ease;
}

.ai-panel__head-btn:hover {
  background: rgba(139, 92, 246, 0.1);
  color: var(--primary);
}

.ai-panel--minimized {
  width: auto;
  min-width: 240px;
}

.ai-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.ai-message--user {
  justify-content: flex-end;
}

.ai-message__avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c4b5fd, #f9a8d4);
  display: grid;
  place-items: center;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.ai-message__bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 0.85rem;
  line-height: 1.6;
}

.ai-message--bot .ai-message__bubble {
  background: rgba(139, 92, 246, 0.08);
  color: var(--text);
  border-top-left-radius: 4px;
}

.ai-message--user .ai-message__bubble {
  background: linear-gradient(135deg, #8b5cf6, #ec4899);
  color: white;
  border-top-right-radius: 4px;
}

/* Typing indicator */
.ai-typing {
  display: flex;
  gap: 4px;
  padding: 12px 18px;
}

.ai-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  animation: ai-bounce 1.2s ease-in-out infinite;
}

.ai-typing span:nth-child(2) { animation-delay: 0.15s; }
.ai-typing span:nth-child(3) { animation-delay: 0.3s; }

@keyframes ai-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.ai-panel__notice {
  padding: 6px 18px;
  font-size: 0.78rem;
  color: var(--green);
}

.ai-panel__notice--error {
  color: #ef4444;
}

.ai-panel__upload-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 150ms ease;
  font-size: 1rem;
  flex-shrink: 0;
}

.ai-panel__upload-btn:hover {
  background: rgba(139, 92, 246, 0.1);
}

/* Slide transition */
.ai-slide-enter-active,
.ai-slide-leave-active {
  transition: all 250ms ease;
}

.ai-slide-enter-from,
.ai-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}
</style>
