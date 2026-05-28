<script setup>
defineProps({
  article: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'" class="article-list-item">
    <div class="article-list-item__cover" :data-tone="article.coverTone">
      <img v-if="article.coverUrl" :src="article.coverUrl" :alt="article.title" />
      <span>{{ article.category }}</span>
    </div>
    <div class="article-list-item__body">
      <p class="article-list-item__meta">
        <span>{{ article.author }}</span>
        <span>{{ article.publishedAt }}</span>
        <span>{{ article.readingMinutes }} min</span>
      </p>
      <h3>{{ article.title }}</h3>
      <p>{{ article.summary }}</p>
      <div class="article-list-item__tags">
        <span v-for="tag in article.tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
  </RouterLink>
</template>

