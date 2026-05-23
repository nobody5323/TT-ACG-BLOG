<script setup>
import WebLayout from "../../layouts/WebLayout.vue";
</script>

<template>
  <WebLayout>
    <div class="about-page">
      <!-- Hero -->
      <div class="about-hero">
        <img class="about-hero__avatar-img" src="/avatar.jpg" alt="头像" />
        <h1 class="about-hero__name">青空站主</h1>
        <p class="about-hero__bio">
          我是一个人，懂我意思？
        </p>
      </div>

      <!-- About sections -->
      <div class="about-section">
        <h2>🌸 关于这个站</h2>
        <p>
          一个人的小站，写点博客笔记什么的喵
        </p>
      </div>

      <div class="about-section">
        <h2>✨ 关于我</h2>
        <p>
          我是一个人，懂我意思？
        </p>
      </div>

      <div class="about-section">
        <h2>💌 联系方式</h2>
        <p>
          欢迎通过文章评论与我交流喵 🌙
        </p>
      </div>
    </div>
  </WebLayout>
</template>

<style scoped>
.about-hero__avatar-img {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  margin: 0 auto 16px;
  display: block;
  box-shadow: 0 0 0 6px rgba(196, 181, 253, 0.25);
}
</style>
