<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";

import { getUserProfile } from "../../api/content";
import ArticleFeatureCard from "../../components/article/ArticleFeatureCard.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import SectionHeading from "../../components/common/SectionHeading.vue";
import WebLayout from "../../layouts/WebLayout.vue";
import { useAppStore } from "../../stores/app";

const router = useRouter();
const appStore = useAppStore();
const profile = ref(null);

function logout() {
  appStore.logout();
  router.push("/");
}

onMounted(async () => {
  if (!appStore.isLoggedIn) {
    router.push("/login");
    return;
  }

  profile.value = await getUserProfile();
});
</script>

<template>
  <WebLayout>
    <section v-if="profile" class="container user-page">
      <header class="profile-hero">
        <div>
          <span class="profile-hero__eyebrow">{{ profile.level }}</span>
          <h1>{{ profile.nickname }}</h1>
          <p>{{ profile.signature }}</p>
        </div>
        <button class="ghost-button" type="button" @click="logout">退出登录</button>
      </header>

      <div class="profile-stats">
        <article>
          <strong>{{ profile.stats.favorites }}</strong>
          <span>收藏文章</span>
        </article>
        <article>
          <strong>{{ profile.stats.comments }}</strong>
          <span>评论数</span>
        </article>
        <article>
          <strong>{{ profile.stats.history }}</strong>
          <span>浏览记录</span>
        </article>
      </div>

      <section class="page-section">
        <SectionHeading
          eyebrow="收藏"
          title="我的收藏"
          description="可扩展为文章、角色卡、设定条目三个维度。"
        />
        <div v-if="profile.favorites.length" class="feature-grid">
          <ArticleFeatureCard
            v-for="article in profile.favorites"
            :key="article.slug"
            :article="article"
          />
        </div>
        <EmptyState
          v-else
          title="还没有收藏内容"
          description="可以先去首页或文章广场挑一篇喜欢的内容。"
        />
      </section>

      <section class="page-section">
        <SectionHeading
          eyebrow="历史"
          title="最近浏览"
          description="保留最近阅读足迹，为后续个性化推荐做准备。"
        />
        <div v-if="profile.history.length" class="feature-grid">
          <ArticleFeatureCard
            v-for="article in profile.history"
            :key="article.slug"
            :article="article"
          />
        </div>
        <EmptyState
          v-else
          title="最近还没有浏览记录"
          description="开始探索内容后，这里会自动出现阅读轨迹。"
        />
      </section>
    </section>
  </WebLayout>
</template>