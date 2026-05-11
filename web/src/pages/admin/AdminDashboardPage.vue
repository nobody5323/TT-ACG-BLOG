<script setup>
import { onMounted, ref } from "vue";
import { getAdminDashboardOverview, getAdminDashboardTimeline } from "../../api/admin";

const overview = ref({
  publishedPosts: 0,
  pendingComments: 0,
  activeUsers: 0,
  weeklyDrafts: 0,
});
const timeline = ref([]);
const loading = ref(false);

async function loadDashboard() {
  loading.value = true;
  try {
    const [o, t] = await Promise.all([getAdminDashboardOverview(), getAdminDashboardTimeline()]);
    overview.value = {
      publishedPosts: o?.publishedPosts || 0,
      pendingComments: o?.pendingComments || 0,
      activeUsers: o?.activeUsers || 0,
      weeklyDrafts: o?.weeklyDrafts || 0,
    };
    timeline.value = Array.isArray(t?.items) ? t.items : [];
  } catch (error) {
    const message = error instanceof Error ? error.message : "加载仪表盘失败";
    window.alert(message);
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<template>
  <section class="admin-grid">
    <article class="admin-card admin-card--hero">
      <p class="admin-card__eyebrow">Backstage Overview</p>
      <h2>后台总览</h2>
      <p>集中查看内容、评论和用户状态，及时处理运营任务。</p>
    </article>

    <div class="admin-metrics">
      <article class="admin-card admin-card--metric">
        <p>已发布文章</p>
        <strong>{{ overview.publishedPosts }}</strong>
      </article>
      <article class="admin-card admin-card--metric">
        <p>待审核评论</p>
        <strong>{{ overview.pendingComments }}</strong>
      </article>
      <article class="admin-card admin-card--metric">
        <p>活跃用户</p>
        <strong>{{ overview.activeUsers }}</strong>
      </article>
      <article class="admin-card admin-card--metric">
        <p>草稿文章</p>
        <strong>{{ overview.weeklyDrafts }}</strong>
      </article>
    </div>

    <article class="admin-card">
      <h3>最新动态</h3>
      <p v-if="loading">加载中...</p>
      <ul v-else class="admin-timeline">
        <li v-for="item in timeline" :key="item.time + item.text">
          <time>{{ item.time }}</time>
          <p>{{ item.text }}</p>
        </li>
      </ul>
    </article>
  </section>
</template>
