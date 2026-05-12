<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";

import { registerUser } from "../../api/content";
import WebLayout from "../../layouts/WebLayout.vue";
import { useAppStore } from "../../stores/app";

const router = useRouter();
const appStore = useAppStore();

const form = reactive({
  nickname: "",
  password: "",
  confirmPassword: "",
});

async function submitRegister() {
  if (!form.nickname.trim()) {
    window.alert("昵称不能为空");
    return;
  }
  if (!form.password) {
    window.alert("密码不能为空");
    return;
  }
  if (form.password !== form.confirmPassword) {
    window.alert("两次输入的密码不一致");
    return;
  }

  try {
    const result = await registerUser({
      nickname: form.nickname,
      password: form.password,
    });
    if (!result || !result.user || !result.token) {
      throw new Error("注册成功但登录态初始化失败，请稍后重试");
    }
    appStore.setAuth(result.user, result.token);
    router.push("/user");
  } catch (error) {
    const message = error instanceof Error ? error.message : "注册失败，请稍后重试";
    window.alert(message);
  }
}
</script>

<template>
  <WebLayout>
    <section class="login-page container">
      <div class="login-panel">
        <span class="login-panel__eyebrow">新用户注册</span>
        <h1>创建账号并加入终端</h1>
        <p>注册后会自动登录，你可以继续完善资料、收藏内容并参与互动。</p>

        <form class="login-form" @submit.prevent="submitRegister">
          <label>
            昵称
            <input v-model="form.nickname" type="text" maxlength="24" placeholder="2-24 位昵称" />
          </label>
          <label>
            密码
            <input v-model="form.password" type="password" placeholder="至少 6 位密码" />
          </label>
          <label>
            确认密码
            <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
          </label>
          <button class="primary-button" type="submit">注册并登录</button>
        </form>
      </div>

      <div class="login-sidecard">
        <h2>注册后可用</h2>
        <ul>
          <li>收藏和追踪你关心的文章与专题</li>
          <li>参与评论互动并管理历史记录</li>
          <li>后续接入 AI 助手上下文记忆能力</li>
        </ul>
        <p style="margin-top: 16px">
          已有账号？
          <RouterLink to="/login" class="primary-button" style="margin-left: 8px">去登录</RouterLink>
        </p>
      </div>
    </section>
  </WebLayout>
</template>

