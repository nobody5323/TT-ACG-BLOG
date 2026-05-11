<script setup>
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAdminStore } from "../stores/admin";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const allNavItems = [
  { label: "仪表盘", to: "/admin", permission: "dashboard:read" },
  { label: "文章管理", to: "/admin/posts", permission: "post:create" },
  { label: "评论管理", to: "/admin/comments", permission: "comment:read" },
  { label: "用户管理", to: "/admin/users", permission: "user:read" },
];

const navItems = computed(() =>
  allNavItems.filter((item) => adminStore.hasPermission(item.permission)),
);

const pageTitle = computed(() => route.meta.adminTitle || "后台管理");

watch(
  () => route.fullPath,
  () => {
    adminStore.closeNav();
  },
);

function logout() {
  adminStore.logout();
  router.push("/admin/login");
}
</script>

<template>
  <div class="admin-shell">
    <div class="admin-shell__glow admin-shell__glow--left" />
    <div class="admin-shell__glow admin-shell__glow--right" />

    <aside class="admin-sidebar" :class="{ 'admin-sidebar--open': adminStore.navOpen }">
      <div class="admin-brand">
        <span class="admin-brand__badge">♪</span>
        <div>
          <strong>轻音后台</strong>
          <small>K-On Control Room</small>
        </div>
      </div>

      <nav class="admin-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="admin-nav__link"
          exact-active-class="admin-nav__link--active"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <section class="admin-mascot">
        <p class="admin-mascot__eyebrow">今日值日</p>
        <h3>{{ adminStore.profile.name }}</h3>
        <p>{{ adminStore.profile.role }}</p>
      </section>
    </aside>

    <div class="admin-main-wrap">
      <header class="admin-topbar">
        <button class="admin-topbar__toggle" type="button" @click="adminStore.toggleNav()">
          菜单
        </button>
        <div>
          <p class="admin-topbar__eyebrow">Backstage</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <button class="admin-logout" type="button" @click="logout">退出后台</button>
      </header>

      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>
