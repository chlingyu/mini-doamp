<template>
  <div>
    <a-card class="page-card" title="任务模板管理">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <a-input v-model:value="query.keyword" placeholder="按模板名称搜索" allow-clear style="width: 240px" @pressEnter="loadData" />
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </div>
        <div class="table-toolbar-right">
          <a-button type="primary" @click="openCreate">新增模板</a-button>
        </div>
      </div>

      <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="模板名称" data-index="templateName" key="templateName" />
        <a-table-column title="流程名称" data-index="workflowName" key="workflowName" />
        <a-table-column title="触发方式" data-index="triggerType" key="triggerType" />
        <a-table-column title="Cron" data-index="cronExpr" key="cronExpr" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="创建时间" key="createTime">
          <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="action" :width="280">
          <template #default="{ record }">
            <a-space wrap>
              <a-button type="link" @click="openEdit(record)">编辑</a-button>
              <a-button type="link" @click="toggleStatus(record)">{{ record.status === 1 ? '停用' : '启用' }}</a-button>
              <a-popconfirm title="确认删除该模板吗？" @confirm="removeItem(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="modalTitle" width="760px" @ok="submitForm" :confirm-loading="submitLoading">
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="模板名称" required>
            <a-input v-model:value="formState.templateName" />
          </a-form-item>
          <a-form-item label="流程模板" required>
            <a-select v-model:value="formState.workflowId" :options="workflowOptions" />
          </a-form-item>
          <a-form-item label="触发方式">
            <a-select v-model:value="formState.triggerType" :options="triggerOptions" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="formState.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </div>
        <a-form-item label="Cron 表达式">
          <a-input v-model:value="formState.cronExpr" placeholder="触发方式为 CRON 时填写" />
        </a-form-item>
        <a-form-item label="内容参数(JSON)">
          <a-textarea v-model:value="formState.contentParams" :rows="4" />
        </a-form-item>
        <a-form-item label="反馈参数(JSON)">
          <a-textarea v-model:value="formState.feedbackParams" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { message } from 'ant-design-vue';
import { computed, reactive, ref } from 'vue';

import { createTaskTemplate, deleteTaskTemplate, listTaskTemplates, listWorkflows, updateTaskTemplate, updateTaskTemplateStatus } from '@/api/sop';
import { formatDateTime } from '@/utils/format';

function createFormState() {
  return {
    id: null,
    templateName: '',
    workflowId: undefined,
    contentParams: '{}',
    feedbackParams: '{}',
    triggerType: 'MANUAL',
    cronExpr: '',
    status: 1
  };
}

export default {
  name: 'TaskTemplateList',
  setup() {
    const loading = ref(false);
    const submitLoading = ref(false);
    const modalVisible = ref(false);
    const dataSource = ref([]);
    const workflowOptions = ref([]);
    const query = reactive({ keyword: '' });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });
    const formState = reactive(createFormState());
    const triggerOptions = [
      { label: '手动触发', value: 'MANUAL' },
      { label: 'Cron 触发', value: 'CRON' }
    ];
    const modalTitle = computed(() => (formState.id ? '编辑任务模板' : '新增任务模板'));

    const loadWorkflowOptions = async () => {
      const response = await listWorkflows({ pageNum: 1, pageSize: 200 });
      workflowOptions.value = (response.records || []).map(item => ({ label: item.workflowName, value: item.id }));
    };

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listTaskTemplates({
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

    const resetForm = () => {
      Object.assign(formState, createFormState());
    };

    const openCreate = () => {
      resetForm();
      modalVisible.value = true;
    };

    const openEdit = record => {
      resetForm();
      Object.assign(formState, { ...record });
      modalVisible.value = true;
    };

    const submitForm = async () => {
      submitLoading.value = true;
      try {
        const payload = {
          templateName: formState.templateName,
          workflowId: formState.workflowId,
          contentParams: formState.contentParams,
          feedbackParams: formState.feedbackParams,
          triggerType: formState.triggerType,
          cronExpr: formState.cronExpr,
          status: formState.status
        };
        if (formState.id) {
          await updateTaskTemplate(formState.id, payload);
        } else {
          await createTaskTemplate(payload);
        }
        message.success('保存成功');
        modalVisible.value = false;
        loadData();
      } finally {
        submitLoading.value = false;
      }
    };

    const removeItem = async id => {
      await deleteTaskTemplate(id);
      message.success('删除成功');
      loadData();
    };

    const toggleStatus = async record => {
      await updateTaskTemplateStatus(record.id, record.status === 1 ? 0 : 1);
      message.success('状态更新成功');
      loadData();
    };

    loadWorkflowOptions();
    loadData();

    return {
      loading,
      submitLoading,
      modalVisible,
      dataSource,
      workflowOptions,
      query,
      pagination,
      formState,
      triggerOptions,
      modalTitle,
      formatDateTime,
      loadData,
      resetQuery,
      handleTableChange,
      openCreate,
      openEdit,
      submitForm,
      removeItem,
      toggleStatus
    };
  }
};
</script>