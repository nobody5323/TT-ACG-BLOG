<script setup>
defineProps({
  article: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'" class="feature-card">
    <div class="feature-card__cover" :data-tone="article.coverTone">
      <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" />
      <span>{{ article.category }}</span>
    </div>
    <div class="feature-card__content">
      <p class="feature-card__meta">{{ article.category }} 路 {{ article.author }}</p>
      <h3>{{ article.title }}</h3>
      <p>{{ article.summary }}</p>
      <div class="feature-card__footer">
        <span>{{ article.readingMinutes }} min</span>
        <span>{{ article.views }} 阅读</span>
      </div>
    </div>
  </RouterLink>
</template>

