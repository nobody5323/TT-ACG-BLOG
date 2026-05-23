<template>
  <footer class="site-footer">
    <div class="container site-footer__grid">
      <div>
        <div class="site-footer__logo">
          <img class="site-footer__logo-avatar" src="/avatar.jpg" alt="青空站" />
          <span class="site-footer__logo-name">青空站</span>
        </div>
        <p class="site-footer__text">
          谢谢来看我的博客喵
        </p>
        <div class="site-footer__status">
          <span>个人创作站</span>
          <span>持续更新中</span>
        </div>
      </div>

      <div>
        <p class="site-footer__title">主要分区</p>
        <ul class="site-footer__links">
          <li><RouterLink to="/articles">全部文章</RouterLink></li>
          <li><RouterLink to="/articles?category=daily">日常随笔</RouterLink></li>
          <li><RouterLink to="/articles?category=anime">动漫分享</RouterLink></li>
          <li><RouterLink to="/articles?category=life">生活记录</RouterLink></li>
        </ul>
      </div>

      <div>
        <p class="site-footer__title">关于站点</p>
        <ul class="site-footer__links">
          <li><RouterLink to="/about">关于我</RouterLink></li>
          <li><RouterLink to="/search">站内搜索</RouterLink></li>
          <li><RouterLink to="/login">登录</RouterLink></li>
        </ul>
      </div>
    </div>

    <div class="container site-footer__bottom">
      © {{ new Date().getFullYear() }} 青空站 · 谢谢来看我的博客喵 ✨
    </div>
  </footer>
</template>

<style scoped>
.site-footer__logo-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 3px rgba(196, 181, 253, 0.3);
}
</style>
