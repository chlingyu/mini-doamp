<template>
  <a-card class="page-card" title="消息发送记录">
    <div class="table-toolbar">
      <div class="table-toolbar-left">
        <a-select v-model:value="query.status" allow-clear placeholder="消息状态" style="width: 180px" :options="statusOptions" />
        <a-select v-model:value="query.notifyType" allow-clear placeholder="通知方式" style="width: 180px" :options="notifyTypeOptions" />
        <a-button type="primary" @click="loadData">查询</a-button>
        <a-button @click="resetQuery">重置</a-button>
      </div>
    </div>

    <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
      <a-table-column title="消息ID" data-index="msgId" key="msgId" :width="220" />
      <a-table-column title="通知方式" data-index="notifyType" key="notifyType" />
      <a-table-column title="接收人" data-index="receiverName" key="receiverName" />
      <a-table-column title="标题" data-index="title" key="title" :ellipsis="true" />
      <a-table-column title="状态" key="status">
        <template #default="{ record }">
          <a-tag :color="statusColor(record.status)" class="status-tag">{{ record.status }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="重试次数" data-index="retryCount" key="retryCount" />
      <a-table-column title="失败原因" data-index="failReason" key="failReason" :ellipsis="true" />
      <a-table-column title="发送时间" key="sendTime">
        <template #default="{ record }">{{ formatDateTime(record.sendTime || record.createTime) }}</template>
      </a-table-column>
      <a-table-column title="操作" key="action" :width="120">
        <template #default="{ record }">
          <a-button type="link" :disabled="record.status === 'SENT'" @click="retry(record.id)">手动重试</a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-card>
</template>

<script>
import { message } from 'ant-design-vue';
import { reactive, ref } from 'vue';

import { listMsgRecords, retryMsgRecord } from '@/api/warn';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'MsgRecordList',
  setup() {
    const loading = ref(false);
    const dataSource = ref([]);
    const query = reactive({ status: undefined, notifyType: undefined });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });

    const statusOptions = ['PENDING', 'SENT', 'FAILED', 'RETRYING', 'ALARM'].map(item => ({ label: item, value: item }));
    const notifyTypeOptions = ['sms', 'email', 'wxwork'].map(item => ({ label: item, value: item }));
    const statusColor = status => ({ PENDING: 'blue', SENT: 'green', FAILED: 'red', RETRYING: 'orange', ALARM: 'magenta' }[status] || 'default');

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listMsgRecords({
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
      query.status = undefined;
      query.notifyType = undefined;
      pagination.current = 1;
      loadData();
    };

    const retry = async id => {
      await retryMsgRecord(id);
      message.success('已提交重试');
      loadData();
    };

    const handleTableChange = pager => {
      pagination.current = pager.current;
      pagination.pageSize = pager.pageSize;
      loadData();
    };

    loadData();

    return {
      loading,
      dataSource,
      query,
      pagination,
      statusOptions,
      notifyTypeOptions,
      formatDateTime,
      statusColor,
      loadData,
      resetQuery,
      retry,
      handleTableChange
    };
  }
};
</script>