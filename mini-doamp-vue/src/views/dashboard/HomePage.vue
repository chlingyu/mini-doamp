<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="8" v-for="card in overviewCards" :key="card.title">
        <a-card class="page-card" :title="card.title">
          <div class="overview-value">{{ card.value }}</div>
          <div class="muted-text">{{ card.desc }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="page-card" title="快速入口">
      <a-space wrap>
        <a-button v-for="entry in quickEntries" :key="entry.path" type="primary" ghost @click="goTo(entry.path)">
          {{ entry.title }}
        </a-button>
      </a-space>
    </a-card>

    <a-card class="page-card" title="当前前端能力覆盖">
      <a-timeline>
        <a-timeline-item>Vue 3 + Vuex + Vue Router 基础框架</a-timeline-item>
        <a-timeline-item>Axios 请求拦截、JWT 注入、401 刷新、403 处理</a-timeline-item>
        <a-timeline-item>预警规则 / 预警记录 / 消息记录页面</a-timeline-item>
        <a-timeline-item>SOP 流程设计器（AntV X6）与任务详情只读查看器</a-timeline-item>
        <a-timeline-item>字典缓存刷新与任务调度监控</a-timeline-item>
      </a-timeline>
    </a-card>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';

export default {
  name: 'HomePage',
  setup() {
    const router = useRouter();

    const overviewCards = [
      { title: 'Phase 进度', value: '6 / 7 完成', desc: '前端为当前唯一进行中模块' },
      { title: '核心特性', value: 'X6 + Vuex', desc: '覆盖流程设计、JWT 认证与状态管理' },
      { title: '联调入口', value: '9 个页面', desc: '预警 / SOP / 系统管理核心页面均已接线' }
    ];

    const quickEntries = [
      { title: '预警规则', path: '/warn/rules' },
      { title: '流程模板', path: '/sop/workflows' },
      { title: '任务执行中心', path: '/sop/tasks' },
      { title: '字典管理', path: '/system/dicts' }
    ];

    const goTo = path => router.push(path);

    return {
      overviewCards,
      quickEntries,
      goTo
    };
  }
};
</script>

<style lang="less" scoped>
.overview-value {
  margin-bottom: 8px;
  font-size: 32px;
  font-weight: 700;
}
</style>