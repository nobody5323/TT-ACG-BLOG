import { computed, ref } from "vue";
import { defineStore } from "pinia";

export const useAppStore = defineStore("app", () => {
  const mobileNavOpen = ref(false);
  const user = ref(
    JSON.parse(window.localStorage.getItem("acg-user") || "null"),
  );
  const token = ref(window.localStorage.getItem("acg-token") || "");

  const isLoggedIn = computed(() => Boolean(user.value && token.value));

  function toggleMobileNav() {
    mobileNavOpen.value = !mobileNavOpen.value;
  }

  function closeMobileNav() {
    mobileNavOpen.value = false;
  }

  function setUser(nextUser) {
    user.value = nextUser;
    window.localStorage.setItem("acg-user", JSON.stringify(nextUser));
  }

  function setAuth(nextUser, nextToken) {
    user.value = nextUser;
    token.value = nextToken || "";
    window.localStorage.setItem("acg-user", JSON.stringify(nextUser));
    if (token.value) {
      window.localStorage.setItem("acg-token", token.value);
    } else {
      window.localStorage.removeItem("acg-token");
    }
  }

  function logout() {
    user.value = null;
    token.value = "";
    window.localStorage.removeItem("acg-user");
    window.localStorage.removeItem("acg-token");
  }

  return {
    mobileNavOpen,
    user,
    token,
    isLoggedIn,
    toggleMobileNav,
    closeMobileNav,
    setUser,
    setAuth,
    logout,
  };
});
