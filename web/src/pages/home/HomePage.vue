<script setup>
import { onMounted, ref, computed } from "vue";
import { getHomeData } from "../../api/content";
import WebLayout from "../../layouts/WebLayout.vue";

const home = ref(null);

const categoryIcons = ["✏️", "🎬", "🎮", "🌿", "🎵", "⭐"];
const tagColors = ["pink", "purple", "teal", "orange", "green", "blue", "amber", "rose"];

function tagColor(index) {
  return tagColors[index % tagColors.length];
}

const displayCategories = computed(() => {
  if (!home.value) return [];
  return home.value.hotCategories.slice(0, 6).map((cat, i) => ({
    ...cat,
    icon: categoryIcons[i % categoryIcons.length],
  }));
});

const authorStats = computed(() => {
  if (!home.value) return [];
  const metrics = home.value.hero?.metrics || [];
  return metrics.slice(0, 3);
});

onMounted(async () => {
  home.value = await getHomeData();
});
</script>

<template>
  <WebLayout>
    <!-- Hero Banner -->
    <section class="page-hero">
      <div class="page-hero__bg" />
      <div class="page-hero__bg--overlay" />
      <div class="page-hero__inner">
        <div class="page-hero__copy">
          <h1 class="page-hero__title">青空站</h1>
          <p class="page-hero__sub">
            谢谢来看我的博客喵
          </p>
          <div class="page-hero__actions">
            <RouterLink class="primary-button" to="/articles">
              ✨ 开始阅读
            </RouterLink>
            <RouterLink class="ghost-button" to="/about">
              👤 关于我
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <!-- Main Content -->
    <div v-if="home" class="container home-content">
      <div class="home-content__layout">
        <!-- Left: Recent Articles -->
        <div>
          <div class="section-title">
            <div class="section-title__left">
              <span class="section-title__icon">✨</span>
              <h2>最近文章</h2>
            </div>
            <RouterLink class="section-title__more" to="/articles">
              查看全部 &rsaquo;
            </RouterLink>
          </div>

          <div class="article-grid">
            <RouterLink
              v-for="article in home.latestArticles.slice(0, 6)"
              :key="article.slug"
              :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'"
              class="article-grid-card"
            >
              <div class="article-grid-card__cover" :data-tone="article.coverTone">
                <img v-if="article.coverUrl" class="article-grid-card__img" :src="article.coverUrl" :alt="article.title" />
                <div v-else class="cover-tone" />
                <span class="article-grid-card__category">{{ article.category }}</span>
              </div>
              <div class="article-grid-card__body">
                <div class="article-grid-card__title">{{ article.title }}</div>
                <div class="article-grid-card__summary">{{ article.summary }}</div>
                <div class="article-grid-card__meta">
                  <span>📅 {{ article.publishedAt }}</span>
                  <span>👁 {{ article.views }}</span>
                  <span>❤️ {{ article.likes }}</span>
                </div>
              </div>
            </RouterLink>
          </div>

          <!-- Hot Categories (mobile) -->
          <div v-if="displayCategories.length" class="home-categories-mobile">
            <div class="section-title" style="margin-top: 40px;">
              <div class="section-title__left">
                <span class="section-title__icon">🗂</span>
                <h2>文章分类</h2>
              </div>
            </div>
            <div class="category-grid">
              <RouterLink
                v-for="cat in displayCategories"
                :key="cat.id"
                :to="`/articles?category=${cat.slug}`"
                class="category-item"
              >
                <span class="category-item__icon">{{ cat.icon }}</span>
                <span class="category-item__name">{{ cat.name }}</span>
                <span v-if="cat.heat" class="category-item__count">{{ cat.heat }}</span>
              </RouterLink>
            </div>
          </div>
        </div>

        <!-- Right: Sidebar -->
        <aside class="sidebar">
          <!-- Author Card -->
          <div class="widget author-widget">
            <img class="author-widget__avatar-img" src="/avatar.jpg" alt="头像" />
            <div class="author-widget__name">
              青空站主 <span style="color:var(--amber)">✦</span>
            </div>
            <div class="author-widget__bio">
              谢谢来看我的博客喵
            </div>
            <div v-if="authorStats.length" class="author-widget__stats">
              <div v-for="stat in authorStats" :key="stat.label" class="author-stat">
                <span class="author-stat__num">{{ stat.value }}</span>
                <span class="author-stat__label">{{ stat.label }}</span>
              </div>
            </div>
            <RouterLink to="/about" class="author-widget__btn">
              💬 关于我
            </RouterLink>
          </div>

          <!-- Categories Widget -->
          <div v-if="displayCategories.length" class="widget home-categories-desktop">
            <div class="widget__title">
              <span class="widget__title-icon">🗂</span> 文章分类
            </div>
            <div class="category-grid">
              <RouterLink
                v-for="cat in displayCategories"
                :key="cat.id"
                :to="`/articles?category=${cat.slug}`"
                class="category-item"
              >
                <span class="category-item__icon">{{ cat.icon }}</span>
                <span class="category-item__name">{{ cat.name }}</span>
                <span v-if="cat.heat" class="category-item__count">{{ cat.heat }}</span>
              </RouterLink>
            </div>
          </div>

          <!-- Hot Tags Widget -->
          <div v-if="home.hotTags && home.hotTags.length" class="widget">
            <div class="widget__title">
              <span class="widget__title-icon">🏷</span> 热门标签
            </div>
            <div class="tags-cloud">
              <RouterLink
                v-for="(tag, i) in home.hotTags.slice(0, 10)"
                :key="tag.id"
                :to="`/articles?tag=${tag.name}`"
                :class="`tag-pill tag-pill--${tagColor(i)}`"
              >
                {{ tag.name }}
                <span v-if="tag.heat" style="opacity:0.65;font-size:0.72rem">{{ tag.heat }}</span>
              </RouterLink>
            </div>
          </div>

          <!-- Rankings Widget -->
          <div v-if="home.rankings && home.rankings.length" class="widget">
            <div class="widget__title">
              <span class="widget__title-icon">🔥</span> 阅读排行
            </div>
            <div class="recent-list">
              <RouterLink
                v-for="(article, i) in home.rankings.slice(0, 5)"
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
    </div>

    <!-- Loading -->
    <div v-else class="container home-content">
      <div style="text-align:center;padding:80px 0;color:var(--muted);">
        <div style="font-size:2rem;margin-bottom:12px">🌟</div>
        加载中…
      </div>
    </div>
  </WebLayout>
</template>

<style scoped>
.home-categories-mobile {
  display: none;
}
.home-categories-desktop {
  display: block;
}
@media (max-width: 1024px) {
  .home-categories-mobile { display: block; }
  .home-categories-desktop { display: none; }
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
