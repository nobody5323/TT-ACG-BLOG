<script setup>
import AiAssistantWindow from "../components/ai/AiAssistantWindow.vue";
import SiteFooter from "../components/navigation/SiteFooter.vue";
import SiteHeader from "../components/navigation/SiteHeader.vue";
</script>

<template>
  <div class="site-shell">
    <div class="site-shell__glow site-shell__glow--left" />
    <div class="site-shell__glow site-shell__glow--right" />
    <SiteHeader />
    <main class="site-main">
      <slot />
    </main>
    <SiteFooter />
    <AiAssistantWindow />
  </div>
</template>
