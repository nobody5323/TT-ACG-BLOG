<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  createAdminPost,
  deleteAdminPost,
  getAdminPost,
  listAdminPosts,
  offlineAdminPost,
  publishAdminPost,
} from "../../api/admin";

const posts = ref([]);
const loading = ref(false);
const keyword = ref("");
const status = ref("");

const publishOpen = ref(false);
const publishing = ref(false);
const newTag = ref("");
const tagPool = ref(["校园", "机甲", "治愈", "悬疑", "百合", "奇幻", "偶像"]);
const selectedTags = ref([]);
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref(null);

const form = reactive({
  title: "",
  summary: "",
  content: "",
  status: "published", // published | draft
});

const canSubmit = computed(
  () => Boolean(form.title.trim()) && Boolean(form.content.trim()) && !publishing.value,
);

function statusText(v) {
  if (v === 1) return "已发布";
  if (v === 2) return "下线";
  return "草稿";
}

function normalizeItem(item) {
  return {
    id: item.id,
    title: item.title || "",
    publishStatus: item.publishStatus,
    status: statusText(item.publishStatus),
    updated: item.updateTime || "",
  };
}

async function fetchPosts() {
  loading.value = true;
  try {
    const data = await listAdminPosts({
      keyword: keyword.value.trim() || undefined,
      status: status.value === "" ? undefined : Number(status.value),
      pageNum: 1,
      pageSize: 50,
    });
    posts.value = Array.isArray(data?.items) ? data.items.map(normalizeItem) : [];
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "加载文章失败");
  } finally {
    loading.value = false;
  }
}

function openPublishModal() {
  publishOpen.value = true;
}

function closePublishModal() {
  publishOpen.value = false;
  form.title = "";
  form.summary = "";
  form.content = "";
  form.status = "published";
  newTag.value = "";
  selectedTags.value = [];
}

function toggleTag(tag) {
  if (selectedTags.value.includes(tag)) {
    selectedTags.value = selectedTags.value.filter((t) => t !== tag);
  } else {
    selectedTags.value.push(tag);
  }
}

function addCustomTag() {
  const tag = newTag.value.trim();
  if (!tag) return;
  if (!tagPool.value.includes(tag)) tagPool.value.push(tag);
  if (!selectedTags.value.includes(tag)) selectedTags.value.push(tag);
  newTag.value = "";
}

function removeTag(tag) {
  selectedTags.value = selectedTags.value.filter((t) => t !== tag);
}

async function submitPublish() {
  if (!canSubmit.value) return;

  publishing.value = true;
  try {
    const payload = {
      slug: `post-${Date.now()}`,
      postType: 1,
      title: form.title.trim(),
      summary: form.summary.trim(),
      content: `${form.content.trim()}\n\n#tags ${selectedTags.value.join(" ")}`,
      readingMinutes: 3,
      visibility: 1,
      isTop: 0,
      isFeatured: 0,
    };

    const created = await createAdminPost(payload); // 默认草稿
    if (form.status === "published" && created?.id) {
      await publishAdminPost(created.id); // 立即发布
    }

    closePublishModal();
    await fetchPosts();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "发布失败");
  } finally {
    publishing.value = false;
  }
}

async function openDetail(item) {
  detailOpen.value = true;
  detailLoading.value = true;
  detail.value = null;
  try {
    const data = await getAdminPost(item.id);
    detail.value = data;
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "加载详情失败");
    detailOpen.value = false;
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailOpen.value = false;
  detail.value = null;
}

async function handlePublish(item) {
  if (item.publishStatus === 1) return;
  try {
    await publishAdminPost(item.id);
    await fetchPosts();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "发布失败");
  }
}

async function handleOffline(item) {
  if (item.publishStatus !== 1) return;
  try {
    await offlineAdminPost(item.id);
    await fetchPosts();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "下线失败");
  }
}

async function handleDelete(item) {
  if (!window.confirm(`确认删除《${item.title}》吗？`)) return;
  try {
    await deleteAdminPost(item.id);
    await fetchPosts();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "删除失败");
  }
}

onMounted(fetchPosts);
</script>

<template>
  <section class="admin-grid">
    <article class="admin-card">
      <h3>文章管理</h3>
      <div class="admin-toolbar">
        <input v-model="keyword" type="search" placeholder="搜索标题 / slug" @keyup.enter="fetchPosts" />
        <select v-model="status" @change="fetchPosts">
          <option value="">全部状态</option>
          <option value="1">已发布</option>
          <option value="0">草稿</option>
          <option value="2">下线</option>
        </select>
        <button type="button" @click="openPublishModal">发布文章</button>
      </div>
    </article>

    <article class="admin-card">
      <p v-if="loading">加载中...</p>
      <table v-else class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>状态</th>
            <th>更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in posts" :key="item.id">
            <td>#{{ item.id }}</td>
            <td>{{ item.title }}</td>
            <td><span class="admin-tag">{{ item.status }}</span></td>
            <td>{{ item.updated }}</td>
            <td class="admin-actions">
              <button type="button" @click="openDetail(item)">查看</button>
              <button type="button" @click="handlePublish(item)">发布</button>
              <button type="button" @click="handleOffline(item)">下线</button>
              <button type="button" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </article>

    <div v-if="publishOpen" class="admin-modal-mask" @click.self="closePublishModal">
      <section class="admin-modal">
        <header class="admin-modal__header">
          <h3>发布文章</h3>
          <button type="button" class="admin-modal__close" @click="closePublishModal">关闭</button>
        </header>
        <div class="admin-modal__body">
          <div class="admin-form-grid">
            <label>
              文章标题
              <input v-model="form.title" type="text" placeholder="输入文章标题" />
            </label>
            <label>
              发布模式
              <select v-model="form.status">
                <option value="published">立即发布</option>
                <option value="draft">仅保存草稿</option>
              </select>
            </label>
          </div>
          <label>
            摘要
            <textarea v-model="form.summary" placeholder="输入摘要（可选）" />
          </label>
          <label>
            正文
            <textarea v-model="form.content" class="admin-editor" placeholder="输入正文内容" />
          </label>

          <div class="admin-tag-panel">
            <p class="admin-tag-panel__title">标签管理</p>
            <div class="admin-tag-panel__pool">
              <button
                v-for="tag in tagPool"
                :key="tag"
                type="button"
                class="admin-tag-chip"
                :class="{ 'admin-tag-chip--active': selectedTags.includes(tag) }"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
            <div class="admin-tag-panel__creator">
              <input v-model="newTag" type="text" placeholder="新增自定义标签" @keyup.enter="addCustomTag" />
              <button type="button" @click="addCustomTag">添加标签</button>
            </div>
            <div v-if="selectedTags.length" class="admin-tag-panel__selected">
              <span
                v-for="tag in selectedTags"
                :key="`selected-${tag}`"
                class="admin-tag-selected"
              >
                {{ tag }}
                <button type="button" @click="removeTag(tag)">×</button>
              </span>
            </div>
          </div>
        </div>
        <footer class="admin-modal__footer">
          <button type="button" class="admin-modal__cancel" @click="closePublishModal">取消</button>
          <button type="button" class="admin-modal__submit" :disabled="!canSubmit" @click="submitPublish">
            {{ publishing ? "处理中..." : "确认提交" }}
          </button>
        </footer>
      </section>
    </div>

    <div v-if="detailOpen" class="admin-modal-mask" @click.self="closeDetail">
      <section class="admin-modal">
        <header class="admin-modal__header">
          <h3>文章详情</h3>
          <button type="button" class="admin-modal__close" @click="closeDetail">关闭</button>
        </header>
        <div class="admin-modal__body">
          <p v-if="detailLoading">加载中...</p>
          <template v-else-if="detail">
            <p><strong>ID：</strong>{{ detail.id }}</p>
            <p><strong>slug：</strong>{{ detail.slug }}</p>
            <p><strong>标题：</strong>{{ detail.title }}</p>
            <p><strong>状态：</strong>{{ statusText(detail.publishStatus) }}</p>
            <p><strong>摘要：</strong>{{ detail.summary }}</p>
            <p><strong>正文：</strong></p>
            <pre style="white-space: pre-wrap; margin: 0">{{ detail.content }}</pre>
          </template>
        </div>
      </section>
    </div>
  </section>
</template>
