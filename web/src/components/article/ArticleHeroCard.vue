<script setup>
defineProps({
  article: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'" class="hero-article-card">
    <div class="hero-article-card__media" :data-tone="article.coverTone">
      <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" />
      <span>{{ article.category }}</span>
      <strong>置顶推荐</strong>
    </div>
    <div class="hero-article-card__body">
      <p class="hero-article-card__meta">
        <span>{{ article.author }}</span>
        <span>{{ article.publishedAt }}</span>
      </p>
      <h3>{{ article.title }}</h3>
      <p>{{ article.summary }}</p>
      <div class="hero-article-card__tags">
        <span v-for="tag in article.tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
  </RouterLink>
</template>

