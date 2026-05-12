<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";

import { loginUser } from "../../api/content";
import WebLayout from "../../layouts/WebLayout.vue";
import { useAppStore } from "../../stores/app";

const router = useRouter();
const appStore = useAppStore();

const form = reactive({
  nickname: "",
  password: "",
});

async function submitLogin() {
  try {
    const result = await loginUser(form);
    if (!result || !result.user || !result.token) {
      throw new Error("登录失败，请检查账号和密码");
    }
    appStore.setAuth(result.user, result.token);
    router.push("/user");
  } catch (error) {
    const message = error instanceof Error ? error.message : "登录失败，请稍后重试";
    window.alert(message);
  }
}
</script>

<template>
  <WebLayout>
    <section class="login-page container">
      <div class="login-panel">
        <span class="login-panel__eyebrow">登录入口</span>
        <h1>进入用户终端</h1>
        <p>先保留基础登录入口，后续再接入更完整的账号体系和社区权限。</p>

        <form class="login-form" @submit.prevent="submitLogin">
          <label>
            昵称
            <input v-model="form.nickname" type="text" placeholder="输入你的账号昵称" />
          </label>
          <label>
            密码
            <input v-model="form.password" type="password" placeholder="输入密码" />
          </label>
          <button class="primary-button" type="submit">登录</button>
        </form>
      </div>

      <div class="login-sidecard">
        <h2>登录后可用</h2>
        <ul>
          <li>收藏文章和角色设定卡</li>
          <li>参与评论与站内活动</li>
          <li>后续接入 AI 助手历史上下文</li>
        </ul>
        <p style="margin-top: 16px">
          还没有账号？
          <RouterLink to="/register" class="primary-button" style="margin-left: 8px">去注册</RouterLink>
        </p>
      </div>
    </section>
  </WebLayout>
</template>

