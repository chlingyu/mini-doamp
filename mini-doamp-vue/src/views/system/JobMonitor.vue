<template>
  <div>
    <a-card class="page-card" title="定时任务定义">
      <a-table row-key="jobName" :data-source="jobDefs" :loading="loadingDefs" :pagination="false">
        <a-table-column title="任务名称" data-index="jobName" key="jobName" />
        <a-table-column title="Handler" data-index="handler" key="handler" />
        <a-table-column title="Cron" data-index="cronExpr" key="cronExpr" />
        <a-table-column title="说明" data-index="description" key="description" />
      </a-table>
    </a-card>

    <a-card class="page-card" title="执行日志">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <a-select v-model:value="query.jobName" allow-clear placeholder="按任务过滤" style="width: 220px" :options="jobOptions" />
          <a-button type="primary" @click="loadLogs">查询</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </div>
      </div>

      <a-table row-key="id" :data-source="logData" :loading="loadingLogs" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="任务名称" data-index="jobName" key="jobName" />
        <a-table-column title="任务参数" data-index="jobParam" key="jobParam" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '成功' : '失败' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="耗时(ms)" data-index="costMs" key="costMs" />
        <a-table-column title="消息" data-index="message" key="message" :ellipsis="true" />
        <a-table-column title="执行时间" key="createTime">
          <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
        </a-table-column>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { reactive, ref } from 'vue';

import { listJobLogs, listJobs } from '@/api/system';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'JobMonitor',
  setup() {
    const loadingDefs = ref(false);
    const loadingLogs = ref(false);
    const jobDefs = ref([]);
    const logData = ref([]);
    const jobOptions = ref([]);
    const query = reactive({ jobName: undefined });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });

    const loadJobDefs = async () => {
      loadingDefs.value = true;
      try {
        const response = await listJobs();
        jobDefs.value = response || [];
        jobOptions.value = (response || []).map(item => ({ label: item.jobName, value: item.jobName }));
      } finally {
        loadingDefs.value = false;
      }
    };

    const loadLogs = async () => {
      loadingLogs.value = true;
      try {
        const response = await listJobLogs({
          jobName: query.jobName,
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        });
        logData.value = response.records || [];
        pagination.total = response.total || 0;
      } finally {
        loadingLogs.value = false;
      }
    };

    const resetQuery = () => {
      query.jobName = undefined;
      pagination.current = 1;
      loadLogs();
    };

    const handleTableChange = pager => {
      pagination.current = pager.current;
      pagination.pageSize = pager.pageSize;
      loadLogs();
    };

    loadJobDefs();
    loadLogs();

    return {
      loadingDefs,
      loadingLogs,
      jobDefs,
      logData,
      jobOptions,
      query,
      pagination,
      formatDateTime,
      loadLogs,
      resetQuery,
      handleTableChange
    };
  }
};
</script>