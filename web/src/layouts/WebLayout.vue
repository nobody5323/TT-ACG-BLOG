<script setup>
import AiAssistantWindow from "../components/ai/AiAssistantWindow.vue";
import SiteFooter from "../components/navigation/SiteFooter.vue";
import SiteHeader from "../components/navigation/SiteHeader.vue";
</script>

<template>
  <div class="site-shell">
    <SiteHeader />
    <main class="site-main">
      <slot />
    </main>
    <SiteFooter />
    <AiAssistantWindow />
  </div>
</template>
