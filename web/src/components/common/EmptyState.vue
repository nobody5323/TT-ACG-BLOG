<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <div class="empty-state">
    <p class="empty-state__icon">☆</p>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </div>
</template>
