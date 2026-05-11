<script setup>
defineProps({
  items: {
    type: Array,
    required: true,
  },
});
</script>

<template>
  <div class="ranking-list">
    <RouterLink
      v-for="(item, index) in items"
      :key="item.slug"
      :to="item.slug ? { name: 'article-detail', params: { slug: item.slug } } : '/articles'"
      class="ranking-list__item"
    >
      <span class="ranking-list__index">{{ index + 1 }}</span>
      <div>
        <strong>{{ item.title }}</strong>
        <small>{{ item.views }} 阅读</small>
      </div>
    </RouterLink>
  </div>
</template>

