import { createRouter, createWebHistory } from "vue-router";

import HomePage from "../pages/home/HomePage.vue";
import ArticleListPage from "../pages/articles/ArticleListPage.vue";
import ArticleDetailPage from "../pages/articles/ArticleDetailPage.vue";
import SearchPage from "../pages/search/SearchPage.vue";
import LoginPage from "../pages/auth/LoginPage.vue";
import UserCenterPage from "../pages/user/UserCenterPage.vue";
import NotFoundPage from "../pages/not-found/NotFoundPage.vue";

import AdminLayout from "../layouts/AdminLayout.vue";
import AdminLoginPage from "../pages/admin/AdminLoginPage.vue";
import AdminDashboardPage from "../pages/admin/AdminDashboardPage.vue";
import AdminPostsPage from "../pages/admin/AdminPostsPage.vue";
import AdminCommentsPage from "../pages/admin/AdminCommentsPage.vue";
import AdminUsersPage from "../pages/admin/AdminUsersPage.vue";
import { useAdminStore } from "../stores/admin";

function hasAdminSession() {
  if (typeof window === "undefined") return false;
  return window.localStorage.getItem("acg-admin-session") === "1" && Boolean(window.localStorage.getItem("acg-admin-token"));
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior() {
    return { top: 0 };
  },
  routes: [
    { path: "/", name: "home", component: HomePage, meta: { title: "绯光档案馆" } },
    { path: "/articles", name: "articles", component: ArticleListPage, meta: { title: "文章宇宙" } },
    { path: "/article", redirect: "/articles" },
    { path: "/articles/:slug", name: "article-detail", alias: "/article/:slug", component: ArticleDetailPage, meta: { title: "文章详情" } },
    { path: "/search", name: "search", component: SearchPage, meta: { title: "站内搜索" } },
    { path: "/login", name: "login", component: LoginPage, meta: { title: "登录" } },
    { path: "/user", name: "user-center", component: UserCenterPage, meta: { title: "用户中心" } },
    { path: "/admin/login", name: "admin-login", component: AdminLoginPage, meta: { title: "后台登录" } },
    {
      path: "/admin",
      component: AdminLayout,
      meta: { adminOnly: true, title: "轻音后台" },
      children: [
        {
          path: "",
          name: "admin-dashboard",
          component: AdminDashboardPage,
          meta: { adminOnly: true, title: "后台仪表盘", adminTitle: "后台仪表盘", permission: "dashboard:read" },
        },
        {
          path: "posts",
          name: "admin-posts",
          component: AdminPostsPage,
          meta: { adminOnly: true, title: "文章管理", adminTitle: "文章管理", permission: "post:create" },
        },
        {
          path: "comments",
          name: "admin-comments",
          component: AdminCommentsPage,
          meta: { adminOnly: true, title: "评论管理", adminTitle: "评论管理", permission: "comment:read" },
        },
        {
          path: "users",
          name: "admin-users",
          component: AdminUsersPage,
          meta: { adminOnly: true, title: "用户管理", adminTitle: "用户管理", permission: "user:read" },
        },
      ],
    },
    { path: "/:pathMatch(.*)*", name: "not-found", component: NotFoundPage, meta: { title: "页面不存在" } },
  ],
});

router.beforeEach((to) => {
  const adminStore = useAdminStore();
  const needAdminAuth = to.matched.some((item) => item.meta?.adminOnly);
  const adminSession = hasAdminSession();

  // 登录页永远允许进入，避免本地残留 session/token 导致无法进入登录界面
  if (to.name === "admin-login") {
    return true;
  }

  if (needAdminAuth && !adminSession) {
    return { path: "/admin/login", query: { redirect: to.fullPath } };
  }
  if (needAdminAuth) {
    const required = to.meta?.permission;
    if (required && !adminStore.hasPermission(required)) {
      return { path: "/admin/login", query: { redirect: to.fullPath } };
    }
  }
  return true;
});

router.afterEach((to) => {
  document.title = `${to.meta.title || "绯光档案馆"} | 绯光档案馆`;
});

export default router;
