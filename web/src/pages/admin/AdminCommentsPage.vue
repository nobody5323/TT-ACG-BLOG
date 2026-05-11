<script setup>
import { computed, onMounted, ref } from "vue";
import {
  batchAdminCommentStatus,
  deleteAdminComment,
  listAdminComments,
  patchAdminCommentStatus,
  replyAdminComment,
} from "../../api/admin";

const comments = ref([]);
const loading = ref(false);
const keyword = ref("");
const status = ref("");
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedIds = ref([]);

const pageText = computed(() => `第 ${pageNum.value} 页 / 共 ${Math.max(1, Math.ceil(total.value / pageSize.value))} 页`);

function statusText(v) {
  if (v === 1) return "可见";
  if (v === 0) return "待审/隐藏";
  return `状态${v}`;
}

function normalizeItem(item) {
  return {
    id: item.id,
    user: item.nickname || "匿名",
    content: item.content || "",
    article: item.articleTitle || "",
    status: statusText(item.status),
    rawStatus: item.status,
  };
}

async function fetchComments() {
  loading.value = true;
  try {
    const data = await listAdminComments({
      keyword: keyword.value.trim() || undefined,
      status: status.value === "" ? undefined : Number(status.value),
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    total.value = data?.total || 0;
    comments.value = Array.isArray(data?.items) ? data.items.map(normalizeItem) : [];
  } catch (error) {
    const message = error instanceof Error ? error.message : "加载评论失败";
    window.alert(message);
  } finally {
    loading.value = false;
  }
}

async function searchNow() {
  pageNum.value = 1;
  await fetchComments();
}

async function prevPage() {
  if (pageNum.value <= 1) return;
  pageNum.value -= 1;
  await fetchComments();
}

async function nextPage() {
  if (pageNum.value * pageSize.value >= total.value) return;
  pageNum.value += 1;
  await fetchComments();
}

function toggleSelect(id, checked) {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id);
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id);
  }
}

async function handleReply(item) {
  const text = window.prompt("输入回复内容：");
  if (!text || !text.trim()) return;
  try {
    await replyAdminComment(item.id, { content: text.trim() });
    await fetchComments();
  } catch (error) {
    const message = error instanceof Error ? error.message : "回复失败";
    window.alert(message);
  }
}

async function handleStatus(item, toStatus) {
  try {
    await patchAdminCommentStatus(item.id, toStatus);
    await fetchComments();
  } catch (error) {
    const message = error instanceof Error ? error.message : "更新状态失败";
    window.alert(message);
  }
}

async function handleDelete(item) {
  if (!window.confirm("确认删除该评论吗？")) return;
  try {
    await deleteAdminComment(item.id);
    await fetchComments();
  } catch (error) {
    const message = error instanceof Error ? error.message : "删除失败";
    window.alert(message);
  }
}

async function batchApprove() {
  if (!selectedIds.value.length) {
    window.alert("请先勾选评论");
    return;
  }
  try {
    await batchAdminCommentStatus(selectedIds.value, 1);
    selectedIds.value = [];
    await fetchComments();
  } catch (error) {
    const message = error instanceof Error ? error.message : "批量审核失败";
    window.alert(message);
  }
}

onMounted(fetchComments);
</script>

<template>
  <section class="admin-grid">
    <article class="admin-card">
      <h3>评论管理</h3>
      <div class="admin-toolbar">
        <input v-model="keyword" type="search" placeholder="搜索评论内容 / 用户 / 文章" @keyup.enter="searchNow" />
        <select v-model="status" @change="searchNow">
          <option value="">全部状态</option>
          <option value="1">可见</option>
          <option value="0">待审/隐藏</option>
        </select>
        <button type="button" @click="batchApprove">批量审核</button>
      </div>
    </article>

    <article class="admin-card">
      <p v-if="loading">加载中...</p>
      <table v-else class="admin-table">
        <thead>
          <tr>
            <th></th>
            <th>ID</th>
            <th>用户</th>
            <th>评论内容</th>
            <th>所属文章</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in comments" :key="item.id">
            <td>
              <input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleSelect(item.id, $event.target.checked)" />
            </td>
            <td>#{{ item.id }}</td>
            <td>{{ item.user }}</td>
            <td>{{ item.content }}</td>
            <td>{{ item.article }}</td>
            <td><span class="admin-tag">{{ item.status }}</span></td>
            <td class="admin-actions">
              <button type="button" @click="handleReply(item)">回复</button>
              <button type="button" @click="handleStatus(item, item.rawStatus === 1 ? 0 : 1)">
                {{ item.rawStatus === 1 ? "隐藏" : "通过" }}
              </button>
              <button type="button" @click="handleDelete(item)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="admin-pagebar">
        <span>{{ pageText }}</span>
        <div class="admin-actions">
          <button type="button" @click="prevPage">上一页</button>
          <button type="button" @click="nextPage">下一页</button>
        </div>
      </div>
    </article>
  </section>
</template>
