<script setup>
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAdminStore } from "../../stores/admin";
import { adminLogin } from "../../api/admin";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const form = reactive({
  nickname: "",
  password: "",
});
const submitting = ref(false);

async function submitLogin() {
  if (!form.nickname.trim() || !form.password.trim()) {
    window.alert("请输入后台账号和密码");
    return;
  }
  if (submitting.value) return;

  submitting.value = true;
  try {
    const data = await adminLogin({
      nickname: form.nickname.trim(),
      password: form.password,
    });
    adminStore.setAuth(data);
    await adminStore.refreshMe();
    await adminStore.refreshPermissions();
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/admin";
    router.push(redirect);
  } catch (error) {
    const message = error instanceof Error ? error.message : "登录失败";
    window.alert(message);
  } finally {
    submitting.value = false;
  }
}

function clearAdminSession() {
  adminStore.logout();
  window.alert("已清理后台会话，请重新登录");
}
</script>

<template>
  <div class="admin-login">
    <div class="admin-login__layout">
      <section class="admin-login__intro">
        <p class="admin-login__eyebrow">K-On Backstage</p>
        <h1>欢迎来到绯光后台</h1>
        <p class="admin-login__quote">“没有未来的未来不是我想要的未来”</p>
      </section>

      <section class="admin-login__card">
        <p class="admin-login__eyebrow">Sign In</p>
        <h2>后台登录</h2>

        <form class="admin-login__form" @submit.prevent="submitLogin">
          <label>
            昵称
            <input v-model="form.nickname" type="text" placeholder="输入管理员昵称" />
          </label>
          <label>
            密码
            <input v-model="form.password" type="password" placeholder="输入密码" />
          </label>
          <button class="admin-login__button" type="submit" :disabled="submitting">
            {{ submitting ? "登录中..." : "进入后台" }}
          </button>
        </form>

        <div class="admin-login__status">
          <span class="dot" />
          <p>系统状态正常。</p>
        </div>

        <button class="admin-login__button admin-login__button--ghost" type="button" @click="clearAdminSession">
          清理本地后台会话
        </button>
      </section>
    </div>
  </div>
</template>
