<script setup>
import { onMounted, ref } from "vue";

import { getHomeData } from "../../api/content";
import ArticleFeatureCard from "../../components/article/ArticleFeatureCard.vue";
import ArticleHeroCard from "../../components/article/ArticleHeroCard.vue";
import BoardCard from "../../components/common/BoardCard.vue";
import CharacterPanel from "../../components/common/CharacterPanel.vue";
import RankingList from "../../components/common/RankingList.vue";
import SectionHeading from "../../components/common/SectionHeading.vue";
import ColumnShowcaseCard from "../../components/column/ColumnShowcaseCard.vue";
import CategoryChip from "../../components/tag/CategoryChip.vue";
import TagBadge from "../../components/tag/TagBadge.vue";
import WebLayout from "../../layouts/WebLayout.vue";

const home = ref(null);

onMounted(async () => {
  home.value = await getHomeData();
});
</script>

<template>
  <WebLayout>
    <template v-if="home">
      <section class="hero-world hero-world--scifi container">
        <div class="hero-world__copy hero-world__copy--wide">
          <span class="hero-world__eyebrow">{{ home.hero.eyebrow }}</span>
          <h1>{{ home.hero.title }}</h1>
          <p>{{ home.hero.subtitle }}</p>
          <div class="hero-world__actions">
            <RouterLink class="primary-button" to="/articles">浏览技术文章</RouterLink>
            <RouterLink class="ghost-button" to="/search">搜索技术关键词</RouterLink>
          </div>
          <div class="hero-world__metrics">
            <article v-for="metric in home.hero.metrics" :key="metric.label">
              <strong>{{ metric.value }}</strong>
              <span>{{ metric.label }}</span>
            </article>
          </div>
        </div>

        <div class="hero-scene">
          <div class="hero-scene__signal">
            <span>视觉角色面板</span>
            <strong>个人技术工作台</strong>
            <p>以二次元风格组织个人开发记录，把前后端实现、架构思考与踩坑复盘沉淀为长期可读的技术资产。</p>
          </div>
          <div class="hero-scene__cards">
            <CharacterPanel
              v-for="character in home.fandomCharacters.slice(0, 2)"
              :key="character.id"
              :character="character"
            />
          </div>
        </div>
      </section>

      <section class="container hero-dashboard">
        <div class="editorial-panel">
          <SectionHeading
            eyebrow="开发看板"
            title="本周开发信号"
            description="快速查看当前更新重点、模块状态与接下来要推进的技术事项。"
          />
          <div class="signal-grid">
            <article
              v-for="signal in home.editorialSignals"
              :key="signal.id"
              class="signal-card"
            >
              <p class="signal-card__label">{{ signal.label }}</p>
              <h3>{{ signal.title }}</h3>
              <p>{{ signal.description }}</p>
              <small>{{ signal.status }}</small>
            </article>
          </div>
        </div>

        <aside class="schedule-panel">
          <div class="schedule-panel__head">
            <span class="section-heading__eyebrow">每周节奏</span>
            <h3>更新排期</h3>
            <p>保持站点持续输出：技术文章、后台能力、体验优化按节奏推进。</p>
          </div>
          <div class="schedule-list">
            <article
              v-for="item in home.weeklySchedule"
              :key="item.id"
              class="schedule-card"
            >
              <strong>{{ item.day }}</strong>
              <div>
                <h4>{{ item.title }}</h4>
                <p>{{ item.description }}</p>
              </div>
            </article>
          </div>
        </aside>
      </section>

      <section class="container page-section">
        <SectionHeading
          eyebrow="精选入口"
          title="推荐阅读"
          description="从最有代表性的技术文章开始，快速进入站点内容体系。"
        />
        <div class="feature-grid">
          <ArticleFeatureCard
            v-for="article in home.featuredArticles"
            :key="article.slug"
            :article="article"
          />
        </div>
      </section>

      <section class="container page-section">
        <SectionHeading
          eyebrow="角色面板"
          title="风格角色"
          description="角色卡片承载站点氛围，用于强化个人博客的视觉记忆点。"
        />
        <div class="character-grid">
          <CharacterPanel
            v-for="character in home.fandomCharacters"
            :key="character.id"
            :character="character"
          />
        </div>
      </section>

      <section class="container page-section">
        <div class="forum-section">
          <div>
            <SectionHeading
              eyebrow="技术分区"
              title="专题板块"
              description="按主题组织文章与讨论，方便定位某一类技术问题。"
            />
            <div class="board-grid">
              <BoardCard v-for="board in home.boards" :key="board.id" :board="board" />
            </div>
          </div>

          <div class="topic-universe topic-universe--compact">
            <SectionHeading
              eyebrow="标签索引"
              title="热门主题"
              description="分类和标签作为索引入口，帮助快速跳转到相关技术内容。"
            />
            <div class="topic-universe__categories">
              <CategoryChip
                v-for="category in home.hotCategories"
                :key="category.id"
                :category="category"
              />
            </div>
            <div class="topic-universe__tags">
              <TagBadge v-for="tag in home.hotTags" :key="tag.id" :tag="tag" />
            </div>
          </div>
        </div>
      </section>

      <section class="container page-section content-grid">
        <div>
          <SectionHeading
            eyebrow="最新文章"
            title="最近更新"
            description="按时间顺序展示最近发布的技术记录与开发笔记。"
          />
          <div class="feed-list">
            <RouterLink
              v-for="article in home.latestArticles"
              :key="article.slug"
              class="feed-list__item"
              :to="article.slug ? { name: 'article-detail', params: { slug: article.slug } } : '/articles'"
            >
              <div class="feed-list__cover" :data-tone="article.coverTone" />
              <div class="feed-list__body">
                <p>{{ article.category }} / {{ article.publishedAt }}</p>
                <h3>{{ article.title }}</h3>
                <span>{{ article.summary }}</span>
              </div>
            </RouterLink>
          </div>
        </div>

        <aside class="sidebar-stack">
          <div class="notice-panel">
            <h3>公告</h3>
            <ul>
              <li v-for="note in home.announcements" :key="note">{{ note }}</li>
            </ul>
          </div>
          <div class="notice-panel">
            <h3>阅读排行</h3>
            <RankingList :items="home.rankings" />
          </div>
          <ArticleHeroCard :article="home.hero.featured" />
        </aside>
      </section>

      <section class="container page-section">
        <SectionHeading
          eyebrow="专栏入口"
          title="个人专栏"
          description="围绕固定主题持续连载，把碎片经验积累成结构化知识。"
        />
        <div class="column-grid">
          <ColumnShowcaseCard
            v-for="column in home.columns"
            :key="column.id"
            :column="column"
          />
        </div>
      </section>

      <section class="container page-section station-protocol">
        <SectionHeading
          eyebrow="站点流程"
          title="内容生产流"
          description="从选题、实现、发布到复盘，形成个人技术创作闭环。"
        />

        <div class="protocol-grid">
          <article
            v-for="item in home.stationProtocols"
            :key="item.id"
            class="protocol-card"
          >
            <span>{{ item.step }}</span>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </article>
        </div>

        <div class="station-protocol__cta">
          <div>
            <p class="station-protocol__eyebrow">Next Layer</p>
            <h3>继续扩展到专题页、工具页与完整后台能力</h3>
            <p>当前首页结构已能承载个人技术博客长期迭代，无需重做视觉骨架。</p>
          </div>
          <div class="station-protocol__actions">
            <RouterLink class="primary-button" to="/articles">进入技术文章</RouterLink>
            <RouterLink class="ghost-button" to="/login">进入用户中心</RouterLink>
          </div>
        </div>
      </section>
    </template>
  </WebLayout>
</template>
