<script setup>
defineProps({
  board: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <article class="board-card" :data-tone="board.tone">
    <span class="board-card__label">板块</span>
    <h3>{{ board.title }}</h3>
    <p>{{ board.description }}</p>
    <div class="board-card__footer">
      <strong>{{ board.threads }}</strong>
      <span>篇主题</span>
    </div>
  </article>
</template>
