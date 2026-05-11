import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { adminMe, adminPermissions } from "../api/admin";

const SESSION_KEY = "acg-admin-session";
const PROFILE_KEY = "acg-admin-profile";
const TOKEN_KEY = "acg-admin-token";
const PERMS_KEY = "acg-admin-permissions";

function loadJson(key, fallback) {
  const raw = window.localStorage.getItem(key);
  if (!raw) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export const useAdminStore = defineStore("admin", () => {
  const navOpen = ref(false);
  const session = ref(window.localStorage.getItem(SESSION_KEY) === "1");
  const token = ref(window.localStorage.getItem(TOKEN_KEY) || "");
  const profile = ref(loadJson(PROFILE_KEY, { name: "", role: "后台管理员" }));
  const permissions = ref(loadJson(PERMS_KEY, []));

  const isLoggedIn = computed(() => session.value && Boolean(token.value));

  function setAuth(loginResp) {
    const nextToken = loginResp?.token || "";
    const user = loginResp?.user || {};

    token.value = nextToken;
    session.value = Boolean(nextToken);
    profile.value = {
      name: user.nickname || "",
      role: "后台管理员",
    };

    if (nextToken) {
      window.localStorage.setItem(TOKEN_KEY, nextToken);
      window.localStorage.setItem(SESSION_KEY, "1");
    } else {
      window.localStorage.removeItem(TOKEN_KEY);
      window.localStorage.removeItem(SESSION_KEY);
    }
    window.localStorage.setItem(PROFILE_KEY, JSON.stringify(profile.value));
  }

  function setPermissions(perms) {
    permissions.value = Array.isArray(perms) ? perms : [];
    window.localStorage.setItem(PERMS_KEY, JSON.stringify(permissions.value));
  }

  function hasPermission(perm) {
    if (!perm) return true;
    return permissions.value.includes(perm);
  }

  async function refreshMe() {
    if (!isLoggedIn.value) return null;
    try {
      const data = await adminMe();
      const user = data?.user || {};
      profile.value = {
        name: user.nickname || profile.value.name || "",
        role: "后台管理员",
      };
      window.localStorage.setItem(PROFILE_KEY, JSON.stringify(profile.value));
      return data;
    } catch (error) {
      logout();
      throw error;
    }
  }

  async function refreshPermissions() {
    if (!isLoggedIn.value) return [];
    try {
      const perms = await adminPermissions();
      setPermissions(perms);
      return permissions.value;
    } catch (error) {
      setPermissions([]);
      throw error;
    }
  }

  function logout() {
    session.value = false;
    navOpen.value = false;
    token.value = "";
    profile.value = { name: "", role: "后台管理员" };
    permissions.value = [];
    window.localStorage.removeItem(SESSION_KEY);
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(PROFILE_KEY);
    window.localStorage.removeItem(PERMS_KEY);
  }

  function toggleNav() {
    navOpen.value = !navOpen.value;
  }

  function closeNav() {
    navOpen.value = false;
  }

  return {
    navOpen,
    profile,
    token,
    permissions,
    isLoggedIn,
    setAuth,
    setPermissions,
    hasPermission,
    refreshMe,
    refreshPermissions,
    logout,
    toggleNav,
    closeNav,
  };
});
