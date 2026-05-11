<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";

import { getArticles } from "../../api/content";
import ArticleListItem from "../../components/article/ArticleListItem.vue";
import EmptyState from "../../components/common/EmptyState.vue";
import SectionHeading from "../../components/common/SectionHeading.vue";
import WebLayout from "../../layouts/WebLayout.vue";

const route = useRoute();
const router = useRouter();

const data = ref({
  items: [],
  filters: {
    categories: [],
    tags: [],
  },
});

const selectedCategory = ref(route.query.category || "");
const selectedTag = ref(route.query.tag || "");
const sortValue = ref(route.query.sort || "latest");

const headingDescription = computed(() => {
  if (selectedCategory.value) {
    const match = data.value.filters.categories.find((item) => item.slug === selectedCategory.value);
    return match?.description || "围绕某个技术主题集中阅读相关文章。";
  }
  return "这里汇总了个人在前后端开发中的实践记录、架构思考与问题复盘。";
});

async function loadArticles() {
  data.value = await getArticles({
    category: selectedCategory.value,
    tag: selectedTag.value,
    sort: sortValue.value,
  });
}

async function syncRoute() {
  await router.replace({
    query: {
      ...(selectedCategory.value ? { category: selectedCategory.value } : {}),
      ...(selectedTag.value ? { tag: selectedTag.value } : {}),
      ...(sortValue.value !== "latest" ? { sort: sortValue.value } : {}),
    },
  });
}

watch(
  () => route.query,
  async (query) => {
    selectedCategory.value = query.category || "";
    selectedTag.value = query.tag || "";
    sortValue.value = query.sort || "latest";
    await loadArticles();
  },
);

onMounted(loadArticles);
</script>

<template>
  <WebLayout>
    <section class="page-banner container">
      <SectionHeading
        eyebrow="Tech Notes"
        title="技术文章"
        :description="headingDescription"
      />
    </section>

    <section class="container article-browser">
      <div class="filter-panel">
        <div>
          <label>主题分类</label>
          <select v-model="selectedCategory" @change="syncRoute">
            <option value="">全部</option>
            <option
              v-for="item in data.filters.categories"
              :key="item.slug"
              :value="item.slug"
            >
              {{ item.name }}
            </option>
          </select>
        </div>

        <div>
          <label>标签</label>
          <select v-model="selectedTag" @change="syncRoute">
            <option value="">全部</option>
            <option v-for="item in data.filters.tags" :key="item.id" :value="item.name">
              {{ item.name }}
            </option>
          </select>
        </div>

        <div>
          <label>排序</label>
          <select v-model="sortValue" @change="syncRoute">
            <option value="latest">最新发布</option>
            <option value="popular">热度优先</option>
          </select>
        </div>
      </div>

      <div v-if="data.items.length" class="article-browser__list">
        <ArticleListItem
          v-for="article in data.items"
          :key="article.slug"
          :article="article"
        />
      </div>
      <EmptyState
        v-else
        title="当前筛选下还没有文章"
        description="可以调整分类和标签，或通过搜索页继续检索内容。"
      />
    </section>
  </WebLayout>
</template>
