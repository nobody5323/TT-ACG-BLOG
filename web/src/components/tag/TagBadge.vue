<script setup>
defineProps({
  tag: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink
    :to="`/articles?tag=${encodeURIComponent(tag.name)}`"
    class="tag-badge"
  >
    <span># {{ tag.name }}</span>
    <small>热度 {{ tag.heat }}</small>
  </RouterLink>
</template>
