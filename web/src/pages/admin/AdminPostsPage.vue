<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import {
  createAdminPost,
  deleteAdminPost,
  getAdminPost,
  listAdminPosts,
  offlineAdminPost,
  publishAdminPost,
  updateAdminPost,
  uploadAdminImage,
} from "../../api/admin";

const posts = ref([]);
const loading = ref(false);
const keyword = ref("");
const status = ref("");

const publishOpen = ref(false);
const publishing = ref(false);
const editingPostId = ref(null);
const editOriginal = ref(null);
const editLoading = ref(false);
const newTag = ref("");
const tagPool = ref(["校园", "机甲", "治愈", "悬疑", "百合", "奇幻", "偶像"]);
const selectedTags = ref([]);
const detailOpen = ref(false);
const detailLoading = ref(false);
const detail = ref(null);
const coverUploading = ref(false);
const inlineImageUploading = ref(false);
const inlineImages = ref([]);
const editorRef = ref(null);
const draftStatus = ref("");
const pasteUploading = ref(false);
const DRAFT_PREFIX = "acg-admin-post-draft:";
let draftTimer = null;

const form = reactive({
  slug: "",
  title: "",
  summary: "",
  content: "",
  coverUrl: "",
  coverTone: "",
  status: "published", // published | draft
});

const canSubmit = computed(
  () => Boolean(form.title.trim()) && Boolean(form.content.trim()) && !publishing.value && !editLoading.value,
);
const isEditing = computed(() => Boolean(editingPostId.value));
const editorSections = computed(() => parseContentSections(form.content));
const editorOutline = computed(() => editorSections.value.map((section) => section.heading).filter(Boolean));
const detailSections = computed(() => {
  const parsed = splitContentTags(detail.value?.content || "");
  return parseContentSections(parsed.content);
});
const hasDraftStatus = computed(() => Boolean(draftStatus.value));
const postModalTitle = computed(() => (isEditing.value ? "修改文章" : "发布文章"));

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
  resetPostForm();
  restoreDraft();
  publishOpen.value = true;
}

function closePublishModal() {
  publishOpen.value = false;
  resetPostForm();
}

function resetPostForm() {
  editingPostId.value = null;
  editOriginal.value = null;
  editLoading.value = false;
  form.slug = "";
  form.title = "";
  form.summary = "";
  form.content = "";
  form.coverUrl = "";
  form.coverTone = "";
  form.status = "published";
  newTag.value = "";
  selectedTags.value = [];
  inlineImages.value = [];
  draftStatus.value = "";
}

function draftKey(id = editingPostId.value) {
  return `${DRAFT_PREFIX}${id ? `edit:${id}` : "new"}`;
}

function draftSnapshot() {
  return {
    form: {
      slug: form.slug,
      title: form.title,
      summary: form.summary,
      content: form.content,
      coverUrl: form.coverUrl,
      coverTone: form.coverTone,
      status: form.status,
    },
    selectedTags: [...selectedTags.value],
    inlineImages: [...inlineImages.value],
    savedAt: new Date().toISOString(),
  };
}

function applyDraft(snapshot) {
  if (!snapshot || !snapshot.form) return false;
  form.slug = snapshot.form.slug || "";
  form.title = snapshot.form.title || "";
  form.summary = snapshot.form.summary || "";
  form.content = snapshot.form.content || "";
  form.coverUrl = snapshot.form.coverUrl || "";
  form.coverTone = snapshot.form.coverTone || "";
  form.status = snapshot.form.status || "published";
  selectedTags.value = Array.isArray(snapshot.selectedTags) ? snapshot.selectedTags : [];
  inlineImages.value = Array.isArray(snapshot.inlineImages) ? snapshot.inlineImages : [];
  for (const tag of selectedTags.value) {
    if (!tagPool.value.includes(tag)) tagPool.value.push(tag);
  }
  return true;
}

function restoreDraft(id = editingPostId.value) {
  if (typeof window === "undefined") return false;
  const raw = window.localStorage.getItem(draftKey(id));
  if (!raw) return false;
  try {
    const snapshot = JSON.parse(raw);
    if (applyDraft(snapshot)) {
      const time = snapshot.savedAt ? new Date(snapshot.savedAt).toLocaleString() : "";
      draftStatus.value = time ? `已恢复本地草稿：${time}` : "已恢复本地草稿";
      return true;
    }
  } catch {
    window.localStorage.removeItem(draftKey(id));
  }
  return false;
}

function saveDraftNow() {
  if (typeof window === "undefined" || !publishOpen.value || editLoading.value || publishing.value) return;
  try {
    window.localStorage.setItem(draftKey(), JSON.stringify(draftSnapshot()));
    draftStatus.value = `草稿已自动保存：${new Date().toLocaleTimeString()}`;
  } catch {
    draftStatus.value = "本地草稿保存失败，可能是浏览器存储空间不足";
  }
}

function scheduleDraftSave() {
  if (!publishOpen.value || editLoading.value || publishing.value) return;
  window.clearTimeout(draftTimer);
  draftTimer = window.setTimeout(saveDraftNow, 500);
}

function clearDraft(id = editingPostId.value) {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(draftKey(id));
  draftStatus.value = "";
}

function clearCurrentDraft() {
  clearDraft();
  draftStatus.value = "本地草稿已清除";
}

function splitContentTags(content = "") {
  const raw = String(content || "");
  const match = raw.match(/(?:^|\n)#tags\s+([^\n]*)\s*$/);
  if (!match) {
    return { content: raw, tags: [] };
  }
  return {
    content: raw.slice(0, match.index).trimEnd(),
    tags: match[1].split(/\s+/).filter(Boolean),
  };
}

function buildContentWithTags() {
  const content = form.content.trim();
  if (!selectedTags.value.length) {
    return content;
  }
  return `${content}\n\n#tags ${selectedTags.value.join(" ")}`;
}

function parseParagraph(paragraph = "") {
  const value = String(paragraph || "").trim();
  const imageMatch = value.match(/^!\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)$/);
  if (imageMatch) {
    return {
      type: "image",
      alt: imageMatch[1] || "文章图片",
      src: imageMatch[2],
    };
  }
  return {
    type: "text",
    text: value,
  };
}

function parseContentSections(raw = "") {
  const content = splitContentTags(raw).content.replace(/\r\n/g, "\n").trim();
  if (!content) {
    return [];
  }

  const sections = [];
  let currentHeading = "正文";
  let paragraphs = [];

  for (const block of content.split(/\n\s*\n/)) {
    const cleaned = block.trim();
    if (!cleaned) continue;

    const lines = cleaned.split("\n");
    let paragraphStart = 0;
    const firstLine = lines[0].trim();
    const headingMatch = firstLine.match(/^#{1,6}\s+(.+)$/);
    if (headingMatch) {
      if (paragraphs.length) {
        sections.push({ heading: currentHeading, paragraphs });
        paragraphs = [];
      }
      currentHeading = headingMatch[1].trim();
      paragraphStart = 1;
    }

    const paragraph = lines
      .slice(paragraphStart)
      .map((line) => line.trim())
      .filter(Boolean)
      .join("\n");
    if (paragraph) {
      paragraphs.push(paragraph);
    }
  }

  if (paragraphs.length || !sections.length) {
    sections.push({ heading: currentHeading, paragraphs });
  }
  return sections;
}

async function insertHeading(level = 2) {
  const textarea = editorRef.value;
  const start = textarea?.selectionStart ?? form.content.length;
  const end = textarea?.selectionEnd ?? start;
  const selected = form.content.slice(start, end).trim() || "新目录标题";
  const before = form.content.slice(0, start);
  const after = form.content.slice(end);
  const prefix = !before || before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n";
  const suffix = !after || after.startsWith("\n\n") ? "" : after.startsWith("\n") ? "\n" : "\n\n";
  const heading = `${"#".repeat(level)} ${selected}`;
  form.content = `${before}${prefix}${heading}${suffix}${after}`;

  await nextTick();
  const cursor = `${before}${prefix}${heading}`.length;
  editorRef.value?.focus();
  editorRef.value?.setSelectionRange(cursor, cursor);
}

function fillFormFromDetail(data) {
  const parsed = splitContentTags(data?.content || "");
  form.slug = data?.slug || "";
  form.title = data?.title || "";
  form.summary = data?.summary || "";
  form.content = parsed.content;
  form.coverUrl = data?.coverUrl || "";
  form.coverTone = data?.coverTone || "";
  form.status = data?.publishStatus === 1 ? "published" : "draft";
  selectedTags.value = parsed.tags;
  for (const tag of parsed.tags) {
    if (!tagPool.value.includes(tag)) tagPool.value.push(tag);
  }
}

async function openEditModal(item) {
  resetPostForm();
  publishOpen.value = true;
  editLoading.value = true;
  editingPostId.value = item.id;
  try {
    const data = await getAdminPost(item.id);
    editOriginal.value = data;
    fillFormFromDetail(data);
    restoreDraft(item.id);
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "加载文章失败");
    closePublishModal();
  } finally {
    editLoading.value = false;
  }
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

function toPublicUploadUrl(item) {
  const path = item?.relativePath || "";
  if (!path) return "";
  return path.startsWith("/") ? path : `/${path}`;
}

function insertTextAtCursor(text) {
  const textarea = editorRef.value;
  const insertText = String(text || "");
  const start = textarea?.selectionStart ?? form.content.length;
  const end = textarea?.selectionEnd ?? start;
  form.content = `${form.content.slice(0, start)}${insertText}${form.content.slice(end)}`;

  return nextTick().then(() => {
    const cursor = start + insertText.length;
    editorRef.value?.focus();
    editorRef.value?.setSelectionRange(cursor, cursor);
  });
}

function markdownForUpload(item) {
  const url = toPublicUploadUrl(item);
  if (!url) return "";
  const name = item.originalName || item.storedName || "image";
  return `![${name}](${url})`;
}

function appendInlineImage(item) {
  const markdown = markdownForUpload(item);
  if (!markdown) return;
  form.content = `${form.content.trimEnd()}\n\n${markdown}\n`;
}

async function insertInlineImageAtCursor(item) {
  const markdown = markdownForUpload(item);
  if (!markdown) return;
  const prefix = form.content && !form.content.endsWith("\n\n") ? "\n\n" : "";
  await insertTextAtCursor(`${prefix}${markdown}\n\n`);
}

async function handleCoverUpload(event) {
  const [file] = event.target.files || [];
  event.target.value = "";
  if (!file || coverUploading.value) return;

  coverUploading.value = true;
  try {
    const item = await uploadAdminImage(file);
    form.coverUrl = toPublicUploadUrl(item);
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "上传封面失败");
  } finally {
    coverUploading.value = false;
  }
}

async function handleInlineImageUpload(event) {
  const files = Array.from(event.target.files || []);
  event.target.value = "";
  if (!files.length || inlineImageUploading.value) return;

  inlineImageUploading.value = true;
  try {
    const uploaded = [];
    for (const file of files) {
      const item = await uploadAdminImage(file);
      uploaded.push(item);
      appendInlineImage(item);
    }
    inlineImages.value = [...inlineImages.value, ...uploaded];
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "上传正文图片失败");
  } finally {
    inlineImageUploading.value = false;
  }
}

async function fileFromDataUrl(dataUrl, index = 0) {
  const response = await fetch(dataUrl);
  const blob = await response.blob();
  const extension = blob.type.split("/")[1] || "png";
  return new File([blob], `pasted-image-${Date.now()}-${index}.${extension}`, {
    type: blob.type || "image/png",
  });
}

async function clipboardImagesFromHtml(html = "") {
  const images = [];
  if (!html || typeof DOMParser === "undefined") return images;
  const doc = new DOMParser().parseFromString(html, "text/html");
  const nodes = Array.from(doc.querySelectorAll("img"));
  for (const [index, img] of nodes.entries()) {
    const src = img.getAttribute("src") || "";
    if (src.startsWith("data:image/")) {
      images.push(await fileFromDataUrl(src, index));
    }
  }
  return images;
}

async function collectClipboardImages(clipboardData) {
  const files = [];
  for (const item of Array.from(clipboardData?.items || [])) {
    if (item.kind === "file" && item.type.startsWith("image/")) {
      const file = item.getAsFile();
      if (file) files.push(file);
    }
  }

  const htmlImages = await clipboardImagesFromHtml(clipboardData?.getData("text/html") || "");
  return [...files, ...htmlImages];
}

function clipboardHasImages(clipboardData) {
  const hasFileImage = Array.from(clipboardData?.items || []).some(
    (item) => item.kind === "file" && item.type.startsWith("image/"),
  );
  const html = clipboardData?.getData("text/html") || "";
  return hasFileImage || /<img[\s>]/i.test(html);
}

function normalizePastedText(text = "") {
  return String(text || "").replace(/\r\n/g, "\n").trimEnd();
}

async function uploadImagesAtCursor(files) {
  if (!files.length) return;
  pasteUploading.value = true;
  inlineImageUploading.value = true;
  try {
    const uploaded = [];
    for (const file of files) {
      const item = await uploadAdminImage(file);
      uploaded.push(item);
      await insertInlineImageAtCursor(item);
    }
    inlineImages.value = [...inlineImages.value, ...uploaded];
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "粘贴图片上传失败");
  } finally {
    pasteUploading.value = false;
    inlineImageUploading.value = false;
  }
}

async function handleEditorPaste(event) {
  const clipboardData = event.clipboardData;
  if (!clipboardData) return;

  if (!clipboardHasImages(clipboardData)) {
    return;
  }
  event.preventDefault();

  const images = await collectClipboardImages(clipboardData);
  if (!images.length) {
    const text = normalizePastedText(clipboardData.getData("text/plain"));
    if (text) {
      await insertTextAtCursor(text);
    }
    return;
  }

  const text = normalizePastedText(clipboardData.getData("text/plain"));
  if (text) {
    await insertTextAtCursor(text);
  }
  await uploadImagesAtCursor(images);
}

function insertInlineImage(item) {
  appendInlineImage(item);
}

function removeInlineImage(item) {
  inlineImages.value = inlineImages.value.filter((entry) => entry.relativePath !== item.relativePath);
}

async function submitPublish() {
  if (!canSubmit.value) return;

  publishing.value = true;
  try {
    const original = editOriginal.value || {};
    const payload = {
      slug: form.slug || (isEditing.value ? `post-${editingPostId.value}` : `post-${Date.now()}`),
      postType: original.postType ?? 1,
      title: form.title.trim(),
      summary: form.summary.trim(),
      coverUrl: form.coverUrl,
      coverTone: form.coverTone.trim(),
      content: buildContentWithTags(),
      readingMinutes: original.readingMinutes ?? 3,
      boardId: original.boardId ?? null,
      categoryId: original.categoryId ?? null,
      columnId: original.columnId ?? null,
      visibility: original.visibility ?? 1,
      isTop: original.isTop ?? 0,
      isFeatured: original.isFeatured ?? 0,
      ranking: original.ranking ?? null,
    };

    if (isEditing.value) {
      const submittedId = editingPostId.value;
      await updateAdminPost(editingPostId.value, payload);
      clearDraft(submittedId);
      closePublishModal();
      await fetchPosts();
      return;
    }

    const created = await createAdminPost(payload); // 默认草稿
    if (form.status === "published" && created?.id) {
      await publishAdminPost(created.id); // 立即发布
    }

    clearDraft();
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

watch(
  () => ({
    slug: form.slug,
    title: form.title,
    summary: form.summary,
    content: form.content,
    coverUrl: form.coverUrl,
    coverTone: form.coverTone,
    status: form.status,
    tags: [...selectedTags.value],
    inlineImages: [...inlineImages.value],
  }),
  scheduleDraftSave,
  { deep: true },
);

onBeforeUnmount(() => {
  window.clearTimeout(draftTimer);
  saveDraftNow();
});

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
              <button type="button" @click="openEditModal(item)">修改</button>
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
      <section class="admin-modal admin-modal--post-editor">
        <header class="admin-modal__header">
          <h3>{{ postModalTitle }}</h3>
          <h3>发布文章</h3>
          <button type="button" class="admin-modal__close" @click="closePublishModal">关闭</button>
        </header>
        <div class="admin-modal__body">
          <p v-if="editLoading">加载文章中...</p>
          <div class="admin-form-grid">
            <label>
              文章标题
              <input v-model="form.title" type="text" placeholder="输入文章标题" />
            </label>
            <label>
              发布模式
              <select v-model="form.status" :disabled="isEditing">
                <option value="published">立即发布</option>
                <option value="draft">仅保存草稿</option>
              </select>
            </label>
          </div>
          <label>
            摘要
            <textarea v-model="form.summary" placeholder="输入摘要（可选）" />
          </label>
          <div class="admin-upload-grid">
            <label class="admin-upload-box">
              文章封面
              <input type="file" accept="image/*" :disabled="coverUploading" @change="handleCoverUpload" />
              <small>{{ coverUploading ? "上传中..." : "支持 jpg / png / webp / gif，上传后会写入封面地址" }}</small>
            </label>
            <label>
              封面色调
              <input v-model="form.coverTone" type="text" placeholder="例如 sunset / midnight，可选" />
            </label>
          </div>
          <div v-if="form.coverUrl" class="admin-cover-preview">
            <p>封面预览</p>
            <img :src="form.coverUrl" alt="文章封面预览" />
            <small>{{ form.coverUrl }}</small>
          </div>
          <label>
            正文
            <div class="admin-editor-toolbar">
              <button type="button" @click="insertHeading(1)">一级目录</button>
              <button type="button" @click="insertHeading(2)">二级目录</button>
              <button type="button" :disabled="!hasDraftStatus" @click="clearCurrentDraft">清除本地草稿</button>
            </div>
            <textarea
              ref="editorRef"
              v-model="form.content"
              class="admin-editor"
              placeholder="输入正文内容，也可以从 Word 复制文字和图片后直接粘贴"
              @paste="handleEditorPaste"
            />
            <small class="admin-draft-status">
              {{ pasteUploading ? "正在上传粘贴图片..." : draftStatus || "草稿会自动保存在本机浏览器" }}
            </small>
          </label>
          <div class="admin-editor-layout">
            <aside class="admin-outline-panel">
              <p>目录设置</p>
              <ol v-if="editorOutline.length">
                <li v-for="heading in editorOutline" :key="heading">{{ heading }}</li>
              </ol>
              <span v-else>暂无目录</span>
            </aside>
            <div v-if="editorSections.length" class="admin-post-preview">
              <p class="admin-post-preview__title">前台预览</p>
              <section v-for="section in editorSections" :key="section.heading">
                <h2>{{ section.heading }}</h2>
                <template v-for="paragraph in section.paragraphs" :key="paragraph">
                  <figure v-if="parseParagraph(paragraph).type === 'image'">
                    <img :src="parseParagraph(paragraph).src" :alt="parseParagraph(paragraph).alt" />
                    <figcaption v-if="parseParagraph(paragraph).alt">{{ parseParagraph(paragraph).alt }}</figcaption>
                  </figure>
                  <p v-else>{{ parseParagraph(paragraph).text }}</p>
                </template>
              </section>
            </div>
          </div>
          <label class="admin-upload-box admin-upload-box--inline-images">
            正文图片
            <input type="file" accept="image/*" multiple :disabled="inlineImageUploading" @change="handleInlineImageUpload" />
            <small>{{ inlineImageUploading ? "上传中..." : "上传后会自动把 Markdown 图片插入正文" }}</small>
          </label>
          <div v-if="inlineImages.length" class="admin-inline-images">
            <p>已上传正文图片</p>
            <ul>
              <li v-for="item in inlineImages" :key="item.relativePath" class="admin-inline-image">
                <img :src="toPublicUploadUrl(item)" :alt="item.originalName" />
                <div>
                  <strong>{{ item.originalName }}</strong>
                  <small>{{ toPublicUploadUrl(item) }}</small>
                </div>
                <div class="admin-inline-image__actions">
                  <button type="button" @click="insertInlineImage(item)">插入</button>
                  <button type="button" @click="removeInlineImage(item)">移除</button>
                </div>
              </li>
            </ul>
          </div>

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
            <p><strong>封面：</strong>{{ detail.coverUrl }}</p>
            <p><strong>正文：</strong></p>
            <div class="admin-editor-layout">
              <aside class="admin-outline-panel">
                <p>目录设置</p>
                <ol v-if="detailSections.length">
                  <li v-for="section in detailSections" :key="section.heading">{{ section.heading }}</li>
                </ol>
                <span v-else>暂无目录</span>
              </aside>
              <div class="admin-post-preview">
                <section v-for="section in detailSections" :key="section.heading">
                  <h2>{{ section.heading }}</h2>
                  <template v-for="paragraph in section.paragraphs" :key="paragraph">
                    <figure v-if="parseParagraph(paragraph).type === 'image'">
                      <img :src="parseParagraph(paragraph).src" :alt="parseParagraph(paragraph).alt" />
                      <figcaption v-if="parseParagraph(paragraph).alt">{{ parseParagraph(paragraph).alt }}</figcaption>
                    </figure>
                    <p v-else>{{ parseParagraph(paragraph).text }}</p>
                  </template>
                </section>
              </div>
            </div>
          </template>
        </div>
      </section>
    </div>
  </section>
</template>
