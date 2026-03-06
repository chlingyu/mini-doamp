<template>
  <div>
    <a-card class="page-card" title="任务执行中心">
      <template #extra>
        <a-button v-if="canCreateTask" type="primary" @click="openCreate">新建任务</a-button>
      </template>
      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <a-tab-pane key="all" tab="全部任务">
          <a-table row-key="id" :data-source="allTasks" :loading="loading" :pagination="allPagination" @change="handleAllChange">
            <a-table-column title="任务编号" data-index="taskCode" key="taskCode" />
            <a-table-column title="任务名称" data-index="taskName" key="taskName" />
            <a-table-column title="流程名称" data-index="workflowName" key="workflowName" />
            <a-table-column title="状态" data-index="statusDesc" key="statusDesc" />
            <a-table-column title="创建时间" key="createTime">
              <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
            </a-table-column>
            <a-table-column title="操作" key="action" :width="180">
              <template #default="{ record }">
                <a-space>
                  <a-button type="link" @click="goDetail(record.id)">详情</a-button>
                  <a-button type="link" danger @click="terminate(record.id)">终止</a-button>
                </a-space>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="todo" tab="我的待办">
          <a-table row-key="id" :data-source="todoTasks" :loading="loading" :pagination="todoPagination" @change="handleTodoChange">
            <a-table-column title="任务ID" data-index="taskId" key="taskId" />
            <a-table-column title="节点名称" data-index="nodeName" key="nodeName" />
            <a-table-column title="节点类型" data-index="nodeType" key="nodeType" />
            <a-table-column title="状态" data-index="status" key="status" />
            <a-table-column title="创建时间" key="createTime">
              <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
            </a-table-column>
            <a-table-column title="操作" key="action">
              <template #default="{ record }">
                <a-button type="link" @click="goDetail(record.taskId)">详情</a-button>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="done" tab="我的已办">
          <a-table row-key="id" :data-source="doneTasks" :loading="loading" :pagination="donePagination" @change="handleDoneChange">
            <a-table-column title="任务ID" data-index="taskId" key="taskId" />
            <a-table-column title="节点名称" data-index="nodeName" key="nodeName" />
            <a-table-column title="节点类型" data-index="nodeType" key="nodeType" />
            <a-table-column title="结果" data-index="result" key="result" />
            <a-table-column title="完成时间" key="endTime">
              <template #default="{ record }">{{ formatDateTime(record.endTime || record.createTime) }}</template>
            </a-table-column>
            <a-table-column title="操作" key="action">
              <template #default="{ record }">
                <a-button type="link" @click="goDetail(record.taskId)">详情</a-button>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:visible="modalVisible" title="新建任务" @ok="submitCreate" :confirm-loading="submitLoading">
      <a-form layout="vertical">
        <a-form-item label="任务名称" required>
          <a-input v-model:value="createForm.taskName" />
        </a-form-item>
        <a-form-item label="任务模板" required>
          <a-select v-model:value="createForm.templateId" :options="templateOptions" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { message } from 'ant-design-vue';
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';

import { createTask, listMyDone, listMyTodo, listTaskTemplates, listTasks, terminateTask } from '@/api/sop';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'TaskCenter',
  setup() {
    const router = useRouter();
    const store = useStore();
    const loading = ref(false);
    const submitLoading = ref(false);
    const modalVisible = ref(false);
    const activeTab = ref('all');
    const allTasks = ref([]);
    const todoTasks = ref([]);
    const doneTasks = ref([]);
    const templateOptions = ref([]);
    const createForm = reactive({ taskName: '', templateId: undefined });
    const canCreateTask = computed(() => store.getters['user/hasPermission']('sop.task')
      && store.getters['user/hasPermission']('sop.template'));
    const allPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: total => `共 ${total} 条` });
    const todoPagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: total => `共 ${total} 条` });
    const donePagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: total => `共 ${total} 条` });

    const loadTemplateOptions = async () => {
      const response = await listTaskTemplates({ pageNum: 1, pageSize: 200 });
      templateOptions.value = (response.records || []).map(item => ({ label: item.templateName, value: item.id }));
    };

    const loadAllTasks = async () => {
      const response = await listTasks({ pageNum: allPagination.current, pageSize: allPagination.pageSize });
      allTasks.value = response.records || [];
      allPagination.total = response.total || 0;
    };

    const loadTodoTasks = async () => {
      const response = await listMyTodo({ pageNum: todoPagination.current, pageSize: todoPagination.pageSize });
      todoTasks.value = response.records || [];
      todoPagination.total = response.total || 0;
    };

    const loadDoneTasks = async () => {
      const response = await listMyDone({ pageNum: donePagination.current, pageSize: donePagination.pageSize });
      doneTasks.value = response.records || [];
      donePagination.total = response.total || 0;
    };

    const loadByTab = async () => {
      loading.value = true;
      try {
        if (activeTab.value === 'all') {
          await loadAllTasks();
        } else if (activeTab.value === 'todo') {
          await loadTodoTasks();
        } else {
          await loadDoneTasks();
        }
      } finally {
        loading.value = false;
      }
    };

    const openCreate = () => {
      if (!canCreateTask.value) {
        message.warning('当前账号无新建任务权限');
        return;
      }
      createForm.taskName = '';
      createForm.templateId = undefined;
      modalVisible.value = true;
    };

    const submitCreate = async () => {
      if (!canCreateTask.value) {
        message.warning('当前账号无新建任务权限');
        return;
      }
      submitLoading.value = true;
      try {
        await createTask({ ...createForm });
        message.success('任务创建成功');
        modalVisible.value = false;
        activeTab.value = 'all';
        loadByTab();
      } finally {
        submitLoading.value = false;
      }
    };

    const terminate = async id => {
      await terminateTask(id);
      message.success('任务已终止');
      loadByTab();
    };

    const goDetail = id => router.push(`/sop/tasks/${id}`);
    const handleTabChange = () => loadByTab();
    const handleAllChange = pager => { allPagination.current = pager.current; allPagination.pageSize = pager.pageSize; loadByTab(); };
    const handleTodoChange = pager => { todoPagination.current = pager.current; todoPagination.pageSize = pager.pageSize; loadByTab(); };
    const handleDoneChange = pager => { donePagination.current = pager.current; donePagination.pageSize = pager.pageSize; loadByTab(); };

    if (canCreateTask.value) {
      loadTemplateOptions();
    }
    loadByTab();

    return {
      loading,
      submitLoading,
      modalVisible,
      activeTab,
      allTasks,
      todoTasks,
      doneTasks,
      templateOptions,
      canCreateTask,
      createForm,
      allPagination,
      todoPagination,
      donePagination,
      formatDateTime,
      openCreate,
      submitCreate,
      terminate,
      goDetail,
      handleTabChange,
      handleAllChange,
      handleTodoChange,
      handleDoneChange
    };
  }
};
</script>
