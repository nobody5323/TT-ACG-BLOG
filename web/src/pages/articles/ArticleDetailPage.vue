<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";

import {
  createArticleComment,
  deleteArticleComment,
  getArticleBySlug,
  getArticleComments,
  likeArticle,
  replyArticleComment,
  viewArticle,
} from "../../api/content";
import ArticleFeatureCard from "../../components/article/ArticleFeatureCard.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import SectionHeading from "../../components/common/SectionHeading.vue";
import WebLayout from "../../layouts/WebLayout.vue";
import { useAppStore } from "../../stores/app";
import { useRoute } from "vue-router";

const route = useRoute();
const appStore = useAppStore();

const payload = ref({
  article: null,
  related: [],
});

const comments = ref([]);
const nextCursor = ref(null);
const commentsLoading = ref(false);
const commentsLoadingMore = ref(false);
const commentsError = ref("");

const commentSubmitting = ref(false);
const commentForm = reactive({
  content: "",
});

const activeReplyId = ref(null);
const replyDrafts = reactive({});
const replySubmittingId = ref(null);
const deletingId = ref(null);

const liked = ref(false);
const likeLoading = ref(false);

const article = computed(() => payload.value.article);
const canSubmitComment = computed(
  () =>
    appStore.isLoggedIn &&
    commentForm.content.trim().length > 0 &&
    commentForm.content.trim().length <= 500 &&
    !commentSubmitting.value,
);

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

function normalizeCommentsPayload(data) {
  if (Array.isArray(data)) {
    return { items: data, nextCursor: null };
  }
  if (data && Array.isArray(data.items)) {
    return {
      items: data.items,
      nextCursor: data.nextCursor ?? null,
    };
  }
  return { items: [], nextCursor: null };
}

function isOwner(comment) {
  if (!appStore.user || !appStore.user.id) {
    return false;
  }
  return String(appStore.user.id) === String(comment.userId);
}

function toggleReplyBox(commentId) {
  if (activeReplyId.value === commentId) {
    activeReplyId.value = null;
    return;
  }
  activeReplyId.value = commentId;
  if (typeof replyDrafts[commentId] === "undefined") {
    replyDrafts[commentId] = "";
  }
}

function canSubmitReply(commentId) {
  const content = (replyDrafts[commentId] || "").trim();
  return (
    appStore.isLoggedIn &&
    content.length > 0 &&
    content.length <= 500 &&
    replySubmittingId.value !== commentId
  );
}

function likedStorageKey(slug = route.params.slug) {
  return `acg-liked-article:${slug ?? ""}`;
}

function hasLikedArticle(slug = route.params.slug) {
  if (typeof window === "undefined") {
    return false;
  }
  return window.localStorage.getItem(likedStorageKey(slug)) === "1";
}

function markArticleLiked(slug = route.params.slug) {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.setItem(likedStorageKey(slug), "1");
}

async function loadArticle() {
  payload.value = await getArticleBySlug(route.params.slug);
}

async function loadComments({ append = false } = {}) {
  if (append && !nextCursor.value) {
    return;
  }

  if (append) {
    commentsLoadingMore.value = true;
  } else {
    commentsLoading.value = true;
    commentsError.value = "";
  }

  try {
    const data = await getArticleComments(route.params.slug, {
      cursor: append ? nextCursor.value : null,
      size: 10,
    });
    const normalized = normalizeCommentsPayload(data);

    if (append) {
      comments.value = [...comments.value, ...normalized.items];
    } else {
      comments.value = normalized.items;
    }
    nextCursor.value = normalized.nextCursor;
  } catch (error) {
    if (!append) {
      comments.value = [];
      nextCursor.value = null;
    }
    commentsError.value = error instanceof Error ? error.message : "评论加载失败";
  } finally {
    commentsLoading.value = false;
    commentsLoadingMore.value = false;
  }
}

async function submitComment() {
  if (!appStore.isLoggedIn) {
    window.alert("请先登录后再评论");
    return;
  }
  if (!canSubmitComment.value) {
    return;
  }

  commentSubmitting.value = true;
  try {
    await createArticleComment(route.params.slug, {
      content: commentForm.content.trim(),
    });
    commentForm.content = "";
    await loadComments({ append: false });
  } catch (error) {
    const message = error instanceof Error ? error.message : "评论发布失败";
    window.alert(message);
  } finally {
    commentSubmitting.value = false;
  }
}

async function submitReply(commentId) {
  if (!appStore.isLoggedIn) {
    window.alert("请先登录后再回复");
    return;
  }
  if (!canSubmitReply(commentId)) {
    return;
  }

  replySubmittingId.value = commentId;
  try {
    await replyArticleComment(commentId, {
      content: (replyDrafts[commentId] || "").trim(),
    });
    replyDrafts[commentId] = "";
    activeReplyId.value = null;
    await loadComments({ append: false });
  } catch (error) {
    const message = error instanceof Error ? error.message : "回复发布失败";
    window.alert(message);
  } finally {
    replySubmittingId.value = null;
  }
}

async function removeComment(comment) {
  if (!isOwner(comment)) {
    return;
  }
  if (!window.confirm("确认删除这条评论吗？")) {
    return;
  }

  deletingId.value = comment.id;
  try {
    await deleteArticleComment(comment.id);
    await loadComments({ append: false });
  } catch (error) {
    const message = error instanceof Error ? error.message : "删除评论失败";
    window.alert(message);
  } finally {
    deletingId.value = null;
  }
}

async function handleLike() {
  if (liked.value) return;
  if (likeLoading.value) return;
  likeLoading.value = true;
  try {
    const counts = await likeArticle(route.params.slug);
    if (payload.value.article && counts) {
      payload.value.article = { ...payload.value.article, likes: counts.likes ?? payload.value.article.likes };
    }
    markArticleLiked();
    liked.value = Boolean(counts.liked ?? true);
  } catch (error) {
    const message = error instanceof Error ? error.message : "点赞失败";
    window.alert(message);
  } finally {
    likeLoading.value = false;
  }
}

async function loadPage() {
  await loadArticle();
  liked.value = hasLikedArticle();
  viewArticle(route.params.slug).then((counts) => {
    if (payload.value.article && counts) {
      payload.value.article = { ...payload.value.article, views: counts.views ?? payload.value.article.views };
    }
  }).catch(() => {});
  await loadComments({ append: false });
}

watch(() => route.params.slug, loadPage);
onMounted(loadPage);
</script>

<template>
  <WebLayout>
    <article v-if="article" class="article-detail container">
      <header class="article-hero">
        <div class="article-hero__copy">
          <p class="article-hero__eyebrow">{{ article.category }} / {{ article.column }}</p>
          <h1>{{ article.title }}</h1>
          <p class="article-hero__summary">{{ article.summary }}</p>
          <div class="article-hero__meta">
            <span>{{ article.author }}</span>
            <span>{{ article.publishedAt }}</span>
            <span>{{ article.readingMinutes }} 分钟</span>
          </div>
          <div class="article-hero__stats">
            <span>{{ article.views }} 阅读</span>
            <button
              class="stat-btn"
              :class="{ 'stat-btn--active': liked }"
              :disabled="likeLoading || liked"
              @click="handleLike"
            >{{ article.likes }} {{ liked ? "已点赞" : "点赞" }}</button>
          </div>
        </div>
        <div class="article-hero__cover" :data-tone="article.coverTone">
          <span>文章封面</span>
        </div>
      </header>

      <div class="article-detail__body">
        <aside class="article-outline">
          <p>目录</p>
          <ol>
            <li v-for="section in article.content" :key="section.heading">
              {{ section.heading }}
            </li>
          </ol>
        </aside>

        <div class="article-content">
          <section v-for="section in article.content" :key="section.heading">
            <h2>{{ section.heading }}</h2>
            <template v-for="paragraph in section.paragraphs" :key="paragraph">
              <figure v-if="parseParagraph(paragraph).type === 'image'" class="article-content__figure">
                <img
                  :src="parseParagraph(paragraph).src"
                  :alt="parseParagraph(paragraph).alt"
                  loading="lazy"
                />
                <figcaption v-if="parseParagraph(paragraph).alt">
                  {{ parseParagraph(paragraph).alt }}
                </figcaption>
              </figure>
              <p v-else>{{ parseParagraph(paragraph).text }}</p>
            </template>
          </section>
        </div>
      </div>

      <section class="comments-section">
        <SectionHeading
          eyebrow="Comments"
          title="评论区"
          description="欢迎交流文章观点，理性讨论。"
        />

        <div class="comment-composer">
          <p v-if="!appStore.isLoggedIn" class="comment-login-tip">
            你还没有登录，登录后可以发表评论。
          </p>
          <textarea
            v-model="commentForm.content"
            class="comment-textarea"
            placeholder="写下你的评论（最多 500 字）"
            maxlength="500"
          />
          <div class="comment-composer__footer">
            <span>{{ commentForm.content.length }}/500</span>
            <button
              class="primary-button"
              type="button"
              :disabled="!canSubmitComment"
              @click="submitComment"
            >
              {{ commentSubmitting ? "发布中..." : "发布评论" }}
            </button>
          </div>
        </div>

        <div v-if="commentsLoading" class="comments-meta">评论加载中...</div>
        <div v-else-if="commentsError" class="comments-meta comments-meta--error">
          评论接口暂不可用：{{ commentsError }}
        </div>
        <div v-else-if="!comments.length" class="comments-meta">暂无评论，来发表第一条吧。</div>
        <div v-else class="comments-list">
          <article
            v-for="item in comments"
            :key="item.id ?? `${item.userId}-${item.createTime}`"
            class="comment-item"
          >
            <header class="comment-item__header">
              <strong>{{ item.nickname || item.author || "匿名用户" }}</strong>
              <time>{{ item.createTime || item.createdAt || "" }}</time>
            </header>
            <p>{{ item.content }}</p>

            <div class="comment-item__actions">
              <button class="ghost-button comment-action" type="button" @click="toggleReplyBox(item.id)">
                回复
              </button>
              <button
                v-if="isOwner(item)"
                class="ghost-button comment-action"
                type="button"
                :disabled="deletingId === item.id"
                @click="removeComment(item)"
              >
                {{ deletingId === item.id ? "删除中..." : "删除" }}
              </button>
            </div>

            <div v-if="activeReplyId === item.id" class="comment-reply-box">
              <textarea
                v-model="replyDrafts[item.id]"
                class="comment-textarea"
                placeholder="写下你的回复（最多 500 字）"
                maxlength="500"
              />
              <div class="comment-composer__footer">
                <span>{{ (replyDrafts[item.id] || "").length }}/500</span>
                <button
                  class="primary-button"
                  type="button"
                  :disabled="!canSubmitReply(item.id)"
                  @click="submitReply(item.id)"
                >
                  {{ replySubmittingId === item.id ? "回复中..." : "发布回复" }}
                </button>
              </div>
            </div>

            <div v-if="item.children && item.children.length" class="comment-children">
              <article
                v-for="child in item.children"
                :key="child.id ?? `${child.userId}-${child.createTime}`"
                class="comment-item comment-item--child"
              >
                <header class="comment-item__header">
                  <strong>{{ child.nickname || "匿名用户" }}</strong>
                  <time>{{ child.createTime || "" }}</time>
                </header>
                <p>{{ child.content }}</p>

                <div class="comment-item__actions">
                  <button class="ghost-button comment-action" type="button" @click="toggleReplyBox(child.id)">
                    回复
                  </button>
                  <button
                    v-if="isOwner(child)"
                    class="ghost-button comment-action"
                    type="button"
                    :disabled="deletingId === child.id"
                    @click="removeComment(child)"
                  >
                    {{ deletingId === child.id ? "删除中..." : "删除" }}
                  </button>
                </div>

                <div v-if="activeReplyId === child.id" class="comment-reply-box">
                  <textarea
                    v-model="replyDrafts[child.id]"
                    class="comment-textarea"
                    placeholder="写下你的回复（最多 500 字）"
                    maxlength="500"
                  />
                  <div class="comment-composer__footer">
                    <span>{{ (replyDrafts[child.id] || "").length }}/500</span>
                    <button
                      class="primary-button"
                      type="button"
                      :disabled="!canSubmitReply(child.id)"
                      @click="submitReply(child.id)"
                    >
                      {{ replySubmittingId === child.id ? "回复中..." : "发布回复" }}
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </article>
        </div>

        <div v-if="nextCursor" class="comments-pagination">
          <button
            class="ghost-button"
            type="button"
            :disabled="commentsLoadingMore"
            @click="loadComments({ append: true })"
          >
            {{ commentsLoadingMore ? "加载中..." : "加载更多评论" }}
          </button>
        </div>
      </section>

      <section class="related-section">
        <SectionHeading
          eyebrow="Related"
          title="继续阅读"
          description="从相近题材或标签进入下一篇内容。"
        />
        <div class="feature-grid">
          <ArticleFeatureCard
            v-for="item in payload.related"
            :key="item.slug"
            :article="item"
          />
        </div>
      </section>
    </article>

    <section v-else class="container page-section">
      <EmptyState
        title="未找到这篇文章"
        description="链接可能失效，或文章仍在整理中。"
      />
    </section>
  </WebLayout>
</template>
