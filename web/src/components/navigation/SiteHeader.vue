<script setup>
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "../../stores/app";

const appStore = useAppStore();
const route = useRoute();
const router = useRouter();

const navItems = [
  { label: "首页", to: "/" },
  { label: "技术文章", to: "/articles" },
  { label: "检索", to: "/search" },
  { label: "个人中心", to: "/user" },
];
const adminEntry = { label: "后台登录", to: "/admin/login" };

const searchValue = ref(route.query.keyword || "");

watch(
  () => route.query.keyword,
  (value) => {
    searchValue.value = value || "";
  },
);

watch(
  () => route.fullPath,
  () => appStore.closeMobileNav(),
);

function submitSearch() {
  appStore.closeMobileNav();
  if (!searchValue.value) {
    router.push("/search");
    return;
  }
  router.push({
    path: "/search",
    query: { keyword: searchValue.value },
  });
}
</script>

<template>
  <header class="site-header" :class="{ 'site-header--open': appStore.mobileNavOpen }">
    <div class="container site-header__inner">
      <RouterLink class="site-logo" to="/">
        <span class="site-logo__mark">绯</span>
        <span>
          <strong>绯光技术手记</strong>
          <small>Personal ACG Tech Blog</small>
        </span>
      </RouterLink>

      <nav class="site-nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="site-nav__link"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <form class="site-search" @submit.prevent="submitSearch">
        <input
          v-model="searchValue"
          type="search"
          placeholder="搜索技术文章、标签、关键词"
        />
        <button type="submit">检索</button>
      </form>

      <div class="site-header__actions">
        <RouterLink class="ghost-button" :to="adminEntry.to">{{ adminEntry.label }}</RouterLink>
        <RouterLink class="ghost-button" to="/search">快速检索</RouterLink>
        <RouterLink
          v-if="appStore.isLoggedIn"
          class="profile-pill"
          to="/user"
        >
          {{ appStore.user.nickname }}
        </RouterLink>
        <RouterLink v-else class="primary-button" to="/login">登录</RouterLink>
        <button
          class="site-header__toggle"
          type="button"
          :aria-expanded="appStore.mobileNavOpen"
          aria-label="切换导航"
          @click="appStore.toggleMobileNav()"
        >
          <span />
          <span />
        </button>
      </div>
    </div>

    <div v-if="appStore.mobileNavOpen" class="site-mobile-panel">
      <div class="container site-mobile-panel__inner">
        <nav class="site-mobile-panel__links">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="site-mobile-panel__link"
          >
            {{ item.label }}
          </RouterLink>
          <RouterLink :to="adminEntry.to" class="site-mobile-panel__link">
            {{ adminEntry.label }}
          </RouterLink>
        </nav>

        <div class="site-mobile-panel__search">
          <p>快速检索</p>
          <form class="site-search" @submit.prevent="submitSearch">
            <input
              v-model="searchValue"
              type="search"
              placeholder="输入关键词开始检索"
            />
            <button type="submit">搜索</button>
          </form>
        </div>
      </div>
    </div>
  </header>
</template>
