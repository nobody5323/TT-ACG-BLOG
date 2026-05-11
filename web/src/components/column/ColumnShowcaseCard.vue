<script setup>
defineProps({
  column: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <article class="column-showcase" :data-theme="column.theme">
    <span class="column-showcase__eyebrow">{{ column.persona }}</span>
    <h3>{{ column.title }}</h3>
    <p>{{ column.description }}</p>
    <RouterLink class="ghost-button" to="/articles">进入专栏</RouterLink>
  </article>
</template>
