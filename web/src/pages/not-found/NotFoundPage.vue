<template>
  <WebLayout>
    <section class="container page-section">
      <div class="empty-state not-found">
        <p class="empty-state__icon">404</p>
        <h1>这条航线不存在</h1>
        <p>你访问的页面可能已经被移动，或者仍在编辑部整理中。</p>
        <RouterLink class="primary-button" to="/">返回首页</RouterLink>
      </div>
    </section>
  </WebLayout>
</template>

<script setup>
import WebLayout from "../../layouts/WebLayout.vue";
</script>
