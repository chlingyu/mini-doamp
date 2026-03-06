<template>
  <a-card class="page-card" title="预警记录查询">
    <div class="table-toolbar">
      <div class="table-toolbar-left">
        <a-select v-model:value="query.ruleId" placeholder="按规则筛选" allow-clear style="width: 220px" :options="ruleOptions" />
        <a-select v-model:value="query.indexId" placeholder="按指标筛选" allow-clear style="width: 220px" :options="indexOptions" />
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </div>
    </div>

    <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
      <a-table-column title="记录ID" data-index="id" key="id" />
      <a-table-column title="规则ID" data-index="ruleId" key="ruleId" />
      <a-table-column title="指标类型" data-index="indexType" key="indexType" />
      <a-table-column title="预警级别" key="warnLevel">
        <template #default="{ record }">
          <a-tag :color="warnLevelColor(record.warnLevel)">L{{ record.warnLevel }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="当前值" data-index="currentValue" key="currentValue" />
      <a-table-column title="阈值" data-index="thresholdValue" key="thresholdValue" />
      <a-table-column title="分组键" data-index="groupKey" key="groupKey" />
      <a-table-column title="预警时间" key="warnTime">
        <template #default="{ record }">{{ formatDateTime(record.warnTime || record.createTime) }}</template>
      </a-table-column>
    </a-table>
  </a-card>
</template>

<script>
import { reactive, ref } from 'vue';

import { listWarnIndexes, listWarnRecords, listWarnRules } from '@/api/warn';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'WarnRecordList',
  setup() {
    const loading = ref(false);
    const dataSource = ref([]);
    const ruleOptions = ref([]);
    const indexOptions = ref([]);
    const query = reactive({ ruleId: undefined, indexId: undefined });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });

    const warnLevelColor = level => ({ 1: 'gold', 2: 'orange', 3: 'red' }[level] || 'blue');

    const loadFilters = async () => {
      const [ruleResp, indexResp] = await Promise.all([
        listWarnRules({ pageNum: 1, pageSize: 200 }),
        listWarnIndexes({ pageNum: 1, pageSize: 200 })
      ]);
      ruleOptions.value = (ruleResp.records || []).map(item => ({ label: item.ruleName, value: item.id }));
      indexOptions.value = (indexResp.records || []).map(item => ({ label: item.indexName, value: item.id }));
    };

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listWarnRecords({
          ...query,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        });
        dataSource.value = response.records || [];
        pagination.total = response.total || 0;
      } finally {
        loading.value = false;
      }
    };

    const resetQuery = () => {
      query.ruleId = undefined;
      query.indexId = undefined;
      pagination.current = 1;
      loadData();
    };

    const handleTableChange = pager => {
      pagination.current = pager.current;
      pagination.pageSize = pager.pageSize;
      loadData();
    };

    loadFilters();
    loadData();

    return {
      loading,
      dataSource,
      ruleOptions,
      indexOptions,
      query,
      pagination,
      formatDateTime,
      warnLevelColor,
      loadData,
      resetQuery,
      handleTableChange
    };
  }
};
</script>