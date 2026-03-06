<template>
  <a-card class="page-card" title="预警规则管理">
    <div class="table-toolbar">
      <div class="table-toolbar-left">
        <a-input v-model:value="query.keyword" placeholder="按规则名称搜索" allow-clear style="width: 240px" @pressEnter="loadData" />
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </div>
      <div class="table-toolbar-right">
        <a-button type="primary" @click="goCreate">新增规则</a-button>
      </div>
    </div>

    <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
      <a-table-column title="规则名称" data-index="ruleName" key="ruleName" />
      <a-table-column title="指标名称" data-index="indexName" key="indexName" />
      <a-table-column title="指标类型" key="indexType">
        <template #default="{ record }">
          <a-tag color="blue">{{ indexTypeMap[record.indexType] || record.indexType }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="通知方式" key="notifyType">
        <template #default="{ record }">
          <a-space wrap>
            <a-tag v-for="item in splitNotifyType(record.notifyType)" :key="`${record.id}_${item}`">{{ item }}</a-tag>
          </a-space>
        </template>
      </a-table-column>
      <a-table-column title="Cron" data-index="cronExpr" key="cronExpr" />
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'default'" class="status-tag">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="创建时间" key="createTime">
        <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" key="action" :width="320">
        <template #default="{ record }">
          <a-space wrap>
            <a-button type="link" @click="goEdit(record.id)">编辑</a-button>
            <a-button type="link" @click="triggerItem(record.id)">手动触发</a-button>
            <a-button type="link" @click="toggleStatus(record)">{{ record.status === 1 ? '停用' : '启用' }}</a-button>
            <a-popconfirm title="确认删除该规则吗？" @confirm="removeItem(record.id)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>
</template>

<script>
import { message } from 'ant-design-vue';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { deleteWarnRule, listWarnRules, triggerWarnRule, updateWarnRuleStatus } from '@/api/warn';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'WarnRuleList',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const dataSource = ref([]);
    const query = reactive({ keyword: '' });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });

    const indexTypeMap = {
      RUNNING: '运行类',
      OPERATION: '运营类',
      BANK: '银行类',
      CHANNEL: '渠道效能类',
      EMPLOYEE: '员工类',
      BRANCH: '营业部类',
      CUSTOM_SQL: '自定义SQL类'
    };

    const splitNotifyType = value => (value ? String(value).split(',').filter(Boolean) : []);

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listWarnRules({
          keyword: query.keyword,
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
      query.keyword = '';
      pagination.current = 1;
      loadData();
    };

    const handleTableChange = pager => {
      pagination.current = pager.current;
      pagination.pageSize = pager.pageSize;
      loadData();
    };

    const goCreate = () => router.push('/warn/rules/create');
    const goEdit = id => router.push(`/warn/rules/${id}/edit`);

    const removeItem = async id => {
      await deleteWarnRule(id);
      message.success('删除成功');
      loadData();
    };

    const toggleStatus = async record => {
      const targetStatus = record.status === 1 ? 0 : 1;
      await updateWarnRuleStatus(record.id, targetStatus);
      message.success('状态更新成功');
      loadData();
    };

    const triggerItem = async id => {
      const count = await triggerWarnRule(id);
      message.success(`手动触发完成，本次产生 ${count} 条预警记录`);
    };

    loadData();

    return {
      loading,
      dataSource,
      query,
      pagination,
      indexTypeMap,
      splitNotifyType,
      formatDateTime,
      loadData,
      resetQuery,
      handleTableChange,
      goCreate,
      goEdit,
      removeItem,
      toggleStatus,
      triggerItem
    };
  }
};
</script>