<script setup>
import { ref } from "vue";

defineProps({
  character: {
    type: Object,
    required: true,
  },
});

const imageFailed = ref(false);

function handleImageError() {
  imageFailed.value = true;
}
</script>

<template>
  <article class="character-panel" :data-tone="character.tone">
    <div class="character-panel__portrait">
      <img
        v-if="character.image && !imageFailed"
        class="character-panel__image"
        :src="character.image"
        :alt="character.name"
        @error="handleImageError"
      />
      <div v-if="!character.image || imageFailed" class="character-panel__fallback" />
      <span>{{ character.universe }}</span>
      <strong>{{ character.name }}</strong>
    </div>
    <div class="character-panel__body">
      <p class="character-panel__meta">{{ character.role }}</p>
      <h3>{{ character.name }}</h3>
      <p>{{ character.vibe }}</p>
      <small>{{ character.quote }}</small>
    </div>
  </article>
</template>
