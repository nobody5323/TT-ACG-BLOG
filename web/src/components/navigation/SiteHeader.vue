<script setup>
import { ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "../../stores/app";

const appStore = useAppStore();
const route = useRoute();
const router = useRouter();

const navItems = [
  { label: "主页", to: "/" },
  { label: "博客", to: "/articles" },
  { label: "关于", to: "/about" },
];

const searchOpen = ref(false);
const searchValue = ref(route.query.keyword || "");
const darkMode = ref(document.documentElement.getAttribute("data-theme") === "dark");

watch(
  () => route.query.keyword,
  (value) => { searchValue.value = value || ""; },
);

watch(
  () => route.fullPath,
  () => appStore.closeMobileNav(),
);

function toggleDark() {
  darkMode.value = !darkMode.value;
  document.documentElement.setAttribute("data-theme", darkMode.value ? "dark" : "light");
}

function toggleSearch() {
  searchOpen.value = !searchOpen.value;
  if (searchOpen.value) {
    setTimeout(() => {
      document.getElementById("header-search-input")?.focus();
    }, 50);
  }
}

function submitSearch() {
  appStore.closeMobileNav();
  searchOpen.value = false;
  if (!searchValue.value.trim()) {
    router.push("/search");
    return;
  }
  router.push({ path: "/search", query: { keyword: searchValue.value } });
}
</script>

<template>
  <header class="site-header" :class="{ 'site-header--open': appStore.mobileNavOpen }">
    <div class="container site-header__inner">
      <!-- Logo -->
      <RouterLink class="site-logo" to="/">
        <img class="site-logo__avatar" src="/avatar.jpg" alt="青空站" />
        <div class="site-logo__text">
          <span class="site-logo__name">青空站</span>
          <span class="site-logo__sub">谢谢来看我的博客喵</span>
        </div>
      </RouterLink>

      <!-- Desktop Nav -->
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

      <!-- Actions -->
      <div class="site-header__actions">
        <!-- Search toggle -->
        <button class="icon-btn" type="button" aria-label="搜索" @click="toggleSearch">
          🔍
        </button>

        <!-- Dark mode toggle -->
        <button class="icon-btn" type="button" aria-label="切换主题" @click="toggleDark">
          {{ darkMode ? '☀️' : '🌙' }}
        </button>

        <!-- Admin Login -->
        <RouterLink class="icon-btn" to="/admin/login" title="后台登录" style="font-size:0.9rem;text-decoration:none;">
          ⚙️
        </RouterLink>

        <!-- User / Login -->
        <RouterLink
          v-if="appStore.isLoggedIn"
          class="profile-pill"
          to="/user"
        >
          <div class="profile-pill__avatar">👤</div>
          {{ appStore.user?.nickname || '我' }}
        </RouterLink>
        <template v-else>
          <RouterLink class="ghost-button" to="/login" style="min-height:36px;padding:0 16px;font-size:0.85rem;">
            登录
          </RouterLink>
        </template>

        <!-- Mobile toggle -->
        <button
          class="site-header__toggle"
          type="button"
          :aria-expanded="appStore.mobileNavOpen"
          aria-label="切换导航"
          @click="appStore.toggleMobileNav()"
        >
          ☰
        </button>
      </div>
    </div>

    <!-- Inline search bar (slides in below header) -->
    <div v-if="searchOpen" class="site-search-bar">
      <div class="container site-search-bar__inner">
        <form @submit.prevent="submitSearch">
          <div class="search-widget">
            <input
              id="header-search-input"
              v-model="searchValue"
              type="search"
              placeholder="搜索文章、标签、关键词…"
            />
            <button type="submit">🔍</button>
          </div>
        </form>
        <button class="icon-btn" type="button" @click="toggleSearch">✕</button>
      </div>
    </div>

    <!-- Mobile Panel -->
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
          <RouterLink
            v-if="!appStore.isLoggedIn"
            to="/login"
            class="site-mobile-panel__link"
          >
            登录
          </RouterLink>
          <RouterLink
            v-if="!appStore.isLoggedIn"
            to="/register"
            class="site-mobile-panel__link"
          >
            注册
          </RouterLink>
          <RouterLink
            to="/admin/login"
            class="site-mobile-panel__link"
          >
            ⚙️ 后台登录
          </RouterLink>
        </nav>

        <div class="site-mobile-panel__search">
          <p>搜索文章</p>
          <form @submit.prevent="submitSearch">
            <div class="search-widget">
              <input
                v-model="searchValue"
                type="search"
                placeholder="输入关键词…"
              />
              <button type="submit">🔍</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-search-bar {
  border-top: 1px solid var(--line);
  background: var(--surface-float);
  backdrop-filter: blur(20px);
  animation: fade-in 200ms ease;
}

.site-search-bar__inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
}

.site-search-bar__inner form {
  flex: 1;
}
</style>
