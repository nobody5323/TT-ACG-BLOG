<template>
  <footer class="site-footer">
    <div class="container site-footer__grid">
      <div class="site-footer__intro">
        <p class="site-footer__title">绯光技术手记</p>
        <p class="site-footer__text">
          一个偏二次元风格的个人技术博客，用文章记录前后端实现、架构设计与踩坑复盘。
        </p>
        <div class="site-footer__status">
          <span>个人创作站</span>
          <span>持续迭代中</span>
        </div>
      </div>

      <div>
        <p class="site-footer__title">主要分区</p>
        <ul class="site-footer__links">
          <li><RouterLink to="/articles?category=anime-analysis">工程实践</RouterLink></li>
          <li><RouterLink to="/articles?category=lore-notes">架构笔记</RouterLink></li>
          <li><RouterLink to="/articles?category=character-design">代码复盘</RouterLink></li>
        </ul>
      </div>

      <div>
        <p class="site-footer__title">站点计划</p>
        <ul class="site-footer__links">
          <li>后台权限体系完善</li>
          <li>标签与搜索能力强化</li>
          <li>个人技术专栏持续更新</li>
        </ul>
      </div>
    </div>
  </footer>
</template>
