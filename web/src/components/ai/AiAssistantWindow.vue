<script setup>
import { computed, nextTick, ref } from "vue";

import { sendAiChat, uploadAiFile } from "../../api/ai";

const isOpen = ref(false);
const isSending = ref(false);
const isUploading = ref(false);
const input = ref("");
const uploadNotice = ref("");
const errorMessage = ref("");
const messages = ref([
  {
    id: "welcome",
    role: "assistant",
    text: "这里可以直接和本站 AI 对话，也可以上传 txt、md、html 文件补充知识库。",
    citations: [],
  },
]);

const sessionId = `web-session-${Date.now()}`;
const userId = "web-guest";
const personaId = "default-anime-assistant";
const messageListRef = ref(null);

const canSend = computed(() => input.value.trim() && !isSending.value);

function toggleWindow() {
  isOpen.value = !isOpen.value;
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

async function submitChat() {
  const message = input.value.trim();
  if (!message || isSending.value) {
    return;
  }

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
    pushMessage("assistant", "请求失败了。请检查 AI 后端是否启动，以及 API Key 是否正确。");
  } finally {
    isSending.value = false;
  }
}

async function handleFileChange(event) {
  const [file] = event.target.files || [];
  event.target.value = "";
  if (!file) {
    return;
  }

  uploadNotice.value = "";
  errorMessage.value = "";
  isUploading.value = true;

  try {
    const result = await uploadAiFile(file);
    uploadNotice.value = `已上传 ${file.name}，写入 ${result.chunks} 个片段。`;
    pushMessage("assistant", `文件 ${file.name} 已入库，现在可以继续提问相关内容。`);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "文件上传失败";
  } finally {
    isUploading.value = false;
  }
}
</script>

<template>
  <div class="ai-assistant">
    <button
      class="ai-assistant__fab"
      type="button"
      :aria-expanded="isOpen"
      aria-label="切换 AI 助手窗口"
      @click="toggleWindow"
    >
      <span>AI</span>
      <small>对话</small>
    </button>

    <transition name="ai-assistant-panel">
      <section v-if="isOpen" class="ai-assistant__panel">
        <header class="ai-assistant__header">
          <div>
            <p>ACG Blog AI</p>
            <strong>站内问答与文件入库</strong>
          </div>
          <button type="button" class="ai-assistant__close" @click="toggleWindow">×</button>
        </header>

        <div ref="messageListRef" class="ai-assistant__messages">
          <article
            v-for="message in messages"
            :key="message.id"
            class="ai-assistant__message"
            :class="`ai-assistant__message--${message.role}`"
          >
            <span class="ai-assistant__message-role">
              {{ message.role === "assistant" ? "AI" : "你" }}
            </span>
            <p>{{ message.text }}</p>
            <small v-if="message.citations?.length">
              来源：{{ message.citations.join("、") }}
            </small>
          </article>
        </div>

        <div class="ai-assistant__upload">
          <label class="ai-assistant__upload-button">
            <input
              type="file"
              accept=".txt,.md,.markdown,.html,.htm,text/plain,text/markdown,text/html"
              :disabled="isUploading"
              @change="handleFileChange"
            />
            {{ isUploading ? "上传中..." : "上传文件" }}
          </label>
          <span class="ai-assistant__upload-note">支持 txt / md / html</span>
        </div>

        <p v-if="uploadNotice" class="ai-assistant__notice">{{ uploadNotice }}</p>
        <p v-if="errorMessage" class="ai-assistant__error">{{ errorMessage }}</p>

        <form class="ai-assistant__composer" @submit.prevent="submitChat">
          <textarea
            v-model="input"
            rows="3"
            placeholder="问文章、角色、项目实现细节，或者先上传一份文本文件…"
          />
          <button type="submit" class="primary-button" :disabled="!canSend">
            {{ isSending ? "发送中..." : "发送" }}
          </button>
        </form>
      </section>
    </transition>
  </div>
</template>
