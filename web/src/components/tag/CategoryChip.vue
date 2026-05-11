<script setup>
defineProps({
  category: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink
    :to="`/articles?category=${category.slug}`"
    class="category-chip"
    :data-accent="category.accent"
  >
    <strong>{{ category.name }}</strong>
    <p>{{ category.description }}</p>
  </RouterLink>
</template>
