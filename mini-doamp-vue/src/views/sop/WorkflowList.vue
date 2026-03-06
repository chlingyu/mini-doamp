<template>
  <a-card class="page-card" title="流程模板管理">
    <div class="table-toolbar">
      <div class="table-toolbar-left">
        <a-input v-model:value="query.keyword" placeholder="按流程编码/名称搜索" allow-clear style="width: 240px" @pressEnter="loadData" />
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </div>
      <div class="table-toolbar-right">
        <a-button type="primary" @click="goCreate">新增流程</a-button>
      </div>
    </div>

    <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
      <a-table-column title="流程编码" data-index="workflowCode" key="workflowCode" />
      <a-table-column title="流程名称" data-index="workflowName" key="workflowName" />
      <a-table-column title="版本" data-index="version" key="version" />
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="statusColor(record.status)">{{ workflowStatusMap[record.status] || record.status }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="创建时间" key="createTime">
        <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" key="action" :width="260">
        <template #default="{ record }">
          <a-space wrap>
            <a-button type="link" @click="goEdit(record.id)">编辑</a-button>
            <a-button type="link" @click="publish(record.id)">发布</a-button>
            <a-button type="link" @click="disable(record.id)">停用</a-button>
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

import { disableWorkflow, listWorkflows, publishWorkflow } from '@/api/sop';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'WorkflowList',
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

    const workflowStatusMap = {
      0: '草稿',
      1: '已发布',
      2: '已停用'
    };

    const statusColor = status => ({ 0: 'default', 1: 'green', 2: 'orange' }[status] || 'default');

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listWorkflows({
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

    const goCreate = () => router.push('/sop/workflows/create');
    const goEdit = id => router.push(`/sop/workflows/${id}/edit`);

    const publish = async id => {
      await publishWorkflow(id);
      message.success('流程已发布');
      loadData();
    };

    const disable = async id => {
      await disableWorkflow(id);
      message.success('流程已停用');
      loadData();
    };

    loadData();

    return {
      loading,
      dataSource,
      query,
      pagination,
      workflowStatusMap,
      formatDateTime,
      statusColor,
      loadData,
      resetQuery,
      handleTableChange,
      goCreate,
      goEdit,
      publish,
      disable
    };
  }
};
</script>