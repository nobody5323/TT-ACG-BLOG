<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { searchArticles } from "../../api/content";
import ArticleListItem from "../../components/article/ArticleListItem.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import SectionHeading from "../../components/common/SectionHeading.vue";
import WebLayout from "../../layouts/WebLayout.vue";

const route = useRoute();
const router = useRouter();

const keyword = ref(route.query.keyword || "");
const results = ref([]);

async function loadResults() {
  results.value = await searchArticles(keyword.value);
}

function submitSearch() {
  router.replace({
    query: keyword.value ? { keyword: keyword.value } : {},
  });
}

watch(
  () => route.query.keyword,
  async (value) => {
    keyword.value = value || "";
    await loadResults();
  },
);

onMounted(loadResults);
</script>

<template>
  <WebLayout>
    <section class="container page-banner">
      <SectionHeading
        eyebrow="Terminal Search"
        title="终端检索"
        description="先做站内关键词搜索，后续再接入 AI RAG 与角色化问答。"
      />

      <form class="search-panel" @submit.prevent="submitSearch">
        <input v-model="keyword" type="search" placeholder="试试输入：机甲、校园、角色设定" />
        <button class="primary-button" type="submit">搜索</button>
      </form>
    </section>

    <section class="container page-section">
      <div v-if="results.length" class="article-browser__list">
        <ArticleListItem v-for="article in results" :key="article.slug" :article="article" />
      </div>
      <EmptyState
        v-else
        title="暂时没有匹配内容"
        :description="keyword ? '可以试试更宽泛的关键词，或者前往文章宇宙浏览。' : '输入关键词后开始检索。'"
      />
    </section>
  </WebLayout>
</template>
