<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getArticles } from "../../api/content";
import EmptyState from "../../components/common/EmptyState.vue";
import WebLayout from "../../layouts/WebLayout.vue";

const route = useRoute();
const router = useRouter();

const data = ref({ items: [], filters: { categories: [], tags: [] } });
const selectedCategory = ref(route.query.category || "");
const selectedTag = ref(route.query.tag || "");
const sortValue = ref(route.query.sort || "latest");
const sidebarSearch = ref("");
const currentPage = ref(1);
const pageSize = 10;

const tagColors = ["pink", "purple", "teal", "orange", "green", "blue", "amber", "rose"];

function tagColor(i) {
  return tagColors[i % tagColors.length];
}

// Simple calendar data
const now = new Date();
const calYear = ref(now.getFullYear());
const calMonth = ref(now.getMonth()); // 0-indexed

const calWeekdays = ["一", "二", "三", "四", "五", "六", "日"];

const calDays = computed(() => {
  const firstDay = new Date(calYear.value, calMonth.value, 1).getDay();
  const daysInMonth = new Date(calYear.value, calMonth.value + 1, 0).getDate();
  const prevDays = new Date(calYear.value, calMonth.value, 0).getDate();
  const offset = (firstDay + 6) % 7; // Monday-first offset

  const days = [];
  for (let i = offset - 1; i >= 0; i--) {
    days.push({ day: prevDays - i, other: true });
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const isToday =
      d === now.getDate() &&
      calMonth.value === now.getMonth() &&
      calYear.value === now.getFullYear();
    days.push({ day: d, today: isToday });
  }
  let next = 1;
  while (days.length % 7 !== 0) {
    days.push({ day: next++, other: true });
  }
  return days;
});

const calTitle = computed(() => {
  const monthNames = ["1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"];
  return `${calYear.value}年${monthNames[calMonth.value]}`;
});

function prevMonth() {
  if (calMonth.value === 0) { calYear.value--; calMonth.value = 11; }
  else calMonth.value--;
}

function nextMonth() {
  if (calMonth.value === 11) { calYear.value++; calMonth.value = 0; }
  else calMonth.value++;
}

const paginatedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return data.value.items.slice(start, start + pageSize);
});

const totalPages = computed(() => Math.ceil(data.value.items.length / pageSize));

const pageNumbers = computed(() => {
  const total = totalPages.value;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const pages = [1];
  if (currentPage.value > 3) pages.push("...");
  for (let i = Math.max(2, currentPage.value - 1); i <= Math.min(total - 1, currentPage.value + 1); i++) {
    pages.push(i);
  }
  if (currentPage.value < total - 2) pages.push("...");
  pages.push(total);
  return pages;
});

const recentArticles = computed(() => data.value.items.slice(0, 5));

const authorStats = computed(() => [
  { num: data.value.items.length || "—", label: "文章" },
  { num: data.value.filters.tags.length || "—", label: "标签" },
  { num: "—", label: "访问" },
]);

async function loadArticles() {
  data.value = await getArticles({
    category: selectedCategory.value,
    tag: selectedTag.value,
    sort: sortValue.value,
  });
  currentPage.value = 1;
}

async function syncRoute() {
  await router.replace({
    query: {
      ...(selectedCategory.value ? { category: selectedCategory.value } : {}),
      ...(selectedTag.value ? { tag: selectedTag.value } : {}),
      ...(sortValue.value !== "latest" ? { sort: sortValue.value } : {}),
    },
  });
}

function submitSidebarSearch() {
  if (!sidebarSearch.value.trim()) return;
  router.push({ path: "/search", query: { keyword: sidebarSearch.value } });
}

function setTag(tagName) {
  selectedTag.value = tagName;
  syncRoute();
}

watch(() => route.query, async (query) => {
  selectedCategory.value = query.category || "";
  selectedTag.value = query.tag || "";
  sortValue.value = query.sort || "latest";
  await loadArticles();
});

onMounted(loadArticles);
</script>

<template>
  <WebLayout>
    <div class="container blog-layout">
      <!-- Left: Article List -->
      <main>
        <h1 class="blog-main__title">
          <span class="blog-main__title-icon">✨</span>
          文章归档
        </h1>

        <!-- Filter bar -->
        <div class="blog-filter" style="margin-bottom:16px;display:flex;gap:10px;flex-wrap:wrap;align-items:center;">
          <select
            v-model="selectedCategory"
            class="blog-filter__select"
            @change="syncRoute"
          >
            <option value="">全部分类</option>
            <option v-for="c in data.filters.categories" :key="c.slug" :value="c.slug">
              {{ c.name }}
            </option>
          </select>

          <select
            v-model="sortValue"
            class="blog-filter__select"
            @change="syncRoute"
          >
            <option value="latest">最新发布</option>
            <option value="popular">热度优先</option>
          </select>
        </div>

        <!-- Article list -->
        <div v-if="paginatedItems.length" class="article-list">
          <RouterLink
            v-for="article in paginatedItems"
            :key="article.slug"
            :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'"
            class="article-list-card"
          >
            <!-- Cover -->
            <div class="article-list-card__cover" :data-tone="article.coverTone">
              <img v-if="article.coverUrl" class="article-list-card__img" :src="article.coverUrl" :alt="article.title" />
              <div v-else class="cover-tone" />
              <span v-if="article.featured" class="article-list-card__pin">置顶</span>
              <span class="article-list-card__bookmark">☆</span>
            </div>

            <!-- Body -->
            <div class="article-list-card__body">
              <div class="article-list-card__title">{{ article.title }}</div>
              <div class="article-list-card__summary">{{ article.summary }}</div>
              <div class="article-list-card__meta">
                <span>📅 {{ article.publishedAt }}</span>
                <span>🗂 {{ article.category }}</span>
                <span>👁 {{ article.views }}</span>
              </div>
            </div>
          </RouterLink>
        </div>

        <EmptyState
          v-else
          title="暂无文章"
          description="当前筛选条件下没有找到文章，试试调整分类或标签。"
        />

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="pagination__btn pagination__btn--nav"
            :disabled="currentPage === 1"
            @click="currentPage > 1 && currentPage--"
          >
            ‹
          </button>

          <template v-for="p in pageNumbers" :key="p">
            <span v-if="p === '...'" style="padding:0 4px;color:var(--muted)">…</span>
            <button
              v-else
              :class="['pagination__btn', p === currentPage ? 'pagination__btn--active' : '']"
              @click="currentPage = p"
            >
              {{ p }}
            </button>
          </template>

          <button
            class="pagination__btn pagination__btn--nav"
            :disabled="currentPage === totalPages"
            @click="currentPage < totalPages && currentPage++"
          >
            ›
          </button>
        </div>
      </main>

      <!-- Right: Sidebar -->
      <aside class="sidebar">
        <!-- Search widget -->
        <div class="widget">
          <div class="widget__title">
            <span class="widget__title-icon">🔍</span> 搜索文章
          </div>
          <form @submit.prevent="submitSidebarSearch">
            <div class="search-widget">
              <input
                v-model="sidebarSearch"
                type="text"
                placeholder="输入关键词…"
              />
              <button type="submit">🔍</button>
            </div>
          </form>
        </div>

        <!-- Author widget -->
        <div class="widget author-widget">
          <img class="author-widget__avatar-img" src="/avatar.jpg" alt="头像" />
          <div class="author-widget__name">青空站主 <span style="color:var(--amber)">✦</span></div>
          <div class="author-widget__bio">一个人的小站，写点博客笔记什么的喵</div>
          <div class="author-widget__stats">
            <div v-for="stat in authorStats" :key="stat.label" class="author-stat">
              <span class="author-stat__num">{{ stat.num }}</span>
              <span class="author-stat__label">{{ stat.label }}</span>
            </div>
          </div>
        </div>

        <!-- Tags widget -->
        <div v-if="data.filters.tags.length" class="widget">
          <div class="widget__title">
            <span class="widget__title-icon">🏷</span> 标签
            <RouterLink to="/articles" style="margin-left:auto;font-size:0.78rem;color:var(--primary)">更多 &rsaquo;</RouterLink>
          </div>
          <div class="tags-cloud">
            <button
              v-for="(tag, i) in data.filters.tags.slice(0, 12)"
              :key="tag.id"
              :class="['tag-pill', `tag-pill--${tagColor(i)}`, selectedTag === tag.name ? 'tag-pill--active' : '']"
              type="button"
              @click="setTag(selectedTag === tag.name ? '' : tag.name)"
            >
              {{ tag.name }}
              <span v-if="tag.heat" style="opacity:0.6;font-size:0.7rem">{{ tag.heat }}</span>
            </button>
          </div>
        </div>

        <!-- Archive Calendar -->
        <div class="widget">
          <div class="widget__title">
            <span class="widget__title-icon">📅</span> 归档日历
          </div>
          <div class="calendar-widget">
            <div class="calendar-header">
              <button class="calendar-nav-btn" type="button" @click="prevMonth">‹</button>
              <span>{{ calTitle }}</span>
              <button class="calendar-nav-btn" type="button" @click="nextMonth">›</button>
            </div>
            <div class="calendar-grid">
              <div v-for="wd in calWeekdays" :key="wd" class="calendar-grid__header">{{ wd }}</div>
              <div
                v-for="(d, i) in calDays"
                :key="i"
                :class="[
                  'calendar-grid__day',
                  d.today ? 'calendar-grid__day--today' : '',
                  d.other ? 'calendar-grid__day--other' : '',
                ]"
              >
                {{ d.day }}
              </div>
            </div>
          </div>
        </div>

        <!-- Recent updates widget -->
        <div v-if="recentArticles.length" class="widget">
          <div class="widget__title">
            <span class="widget__title-icon">🔔</span> 最近更新
          </div>
          <div class="recent-list">
            <RouterLink
              v-for="(article, i) in recentArticles"
              :key="article.slug"
              :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'"
              class="recent-item"
            >
              <span :class="['recent-item__num', i < 3 ? 'recent-item__num--top' : '']">
                {{ i + 1 }}
              </span>
              <div class="recent-item__thumb" :data-tone="article.coverTone">
                <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" />
                <div v-else class="cover-tone" />
              </div>
              <div class="recent-item__body">
                <div class="recent-item__title">{{ article.title }}</div>
                <div class="recent-item__date">{{ article.publishedAt }}</div>
              </div>
            </RouterLink>
          </div>
        </div>
      </aside>
    </div>
  </WebLayout>
</template>

<style scoped>
.blog-filter__select {
  padding: 8px 12px;
  border-radius: 10px;
  border: 1.5px solid var(--line-strong);
  background: var(--surface);
  color: var(--text);
  outline: none;
  font-size: 0.85rem;
  cursor: pointer;
  transition: border-color 180ms ease;
}

.blog-filter__select:focus {
  border-color: var(--primary);
}

.tag-pill--active {
  outline: 2px solid var(--primary);
  outline-offset: 1px;
}

.author-widget__avatar-img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 auto 12px;
  display: block;
  box-shadow: 0 0 0 4px rgba(196, 181, 253, 0.3);
}
</style>
