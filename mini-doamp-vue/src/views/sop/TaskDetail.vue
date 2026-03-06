<template>
  <div>
    <a-card class="page-card" title="任务基础信息">
      <a-descriptions :column="3" bordered size="small">
        <a-descriptions-item label="任务编号">{{ taskDetail.taskCode }}</a-descriptions-item>
        <a-descriptions-item label="任务名称">{{ taskDetail.taskName }}</a-descriptions-item>
        <a-descriptions-item label="流程名称">{{ taskDetail.workflowName }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ taskDetail.statusDesc }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatDateTime(taskDetail.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="完成时间">{{ formatDateTime(taskDetail.completeTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card class="page-card" title="流程查看器">
      <FlowViewer :nodes="workflowGraph.nodes" :edges="workflowGraph.edges" :current-node-id="currentNodeId" />
    </a-card>

    <a-card class="page-card" title="当前处理动作">
      <a-empty v-if="!currentExec" description="当前没有待处理执行记录" />
      <template v-else>
        <a-alert v-if="!canHandleCurrentExec" type="warning" show-icon message="当前账号仅可查看，不能处理该节点" style="margin-bottom: 16px" />
        <div class="form-grid">
          <a-form-item label="当前节点">
            <a-input :value="`${currentExec.nodeName} (${currentExec.nodeType})`" disabled />
          </a-form-item>
          <a-form-item label="分支结果/处理结果">
            <a-input v-model:value="advanceForm.result" placeholder="分支节点可填写路由结果，例如 PASS / REJECT" />
          </a-form-item>
        </div>
        <a-form-item label="反馈数据(JSON)">
          <a-textarea v-model:value="advanceForm.feedbackData" :rows="3" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="advanceForm.remark" :rows="2" />
        </a-form-item>
        <a-space wrap>
          <a-button v-if="currentExec.nodeType !== 'APPROVE'" type="primary" :disabled="!canHandleCurrentExec" @click="advance('SUBMIT')">提交</a-button>
          <a-button v-if="currentExec.nodeType === 'APPROVE'" type="primary" :disabled="!canHandleCurrentExec" @click="advance('APPROVE')">审批通过</a-button>
          <a-button v-if="currentExec.nodeType === 'APPROVE'" danger :disabled="!canHandleCurrentExec" @click="advance('REJECT')">驳回</a-button>
          <a-button danger :disabled="!canHandleCurrentExec" @click="terminateCurrentTask">终止任务</a-button>
        </a-space>
      </template>
    </a-card>

    <a-card class="page-card" title="回退到任意已完成节点">
      <div class="form-grid">
        <a-form-item label="目标节点">
          <a-select v-model:value="rollbackForm.targetNodeId" :options="rollbackOptions" placeholder="请选择已完成节点" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="rollbackForm.remark" />
        </a-form-item>
      </div>
      <a-button type="primary" :disabled="!currentExec || !rollbackForm.targetNodeId || !canHandleCurrentExec" @click="rollbackCurrent">执行回退</a-button>
    </a-card>

    <a-card class="page-card" title="执行记录">
      <a-table row-key="id" :data-source="taskDetail.execRecords || []" :pagination="false">
        <a-table-column title="节点" data-index="nodeName" key="nodeName" />
        <a-table-column title="类型" data-index="nodeType" key="nodeType" />
        <a-table-column title="处理人" data-index="assigneeName" key="assigneeName" />
        <a-table-column title="状态" data-index="status" key="status" />
        <a-table-column title="结果" data-index="result" key="result" />
        <a-table-column title="开始时间" key="startTime">
          <template #default="{ record }">{{ formatDateTime(record.startTime || record.createTime) }}</template>
        </a-table-column>
        <a-table-column title="结束时间" key="endTime">
          <template #default="{ record }">{{ formatDateTime(record.endTime) }}</template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-card class="page-card" title="操作流水">
      <a-table row-key="id" :data-source="taskDetail.operationLogs || []" :pagination="false">
        <a-table-column title="节点" data-index="nodeName" key="nodeName" />
        <a-table-column title="操作人" data-index="operatorName" key="operatorName" />
        <a-table-column title="动作" data-index="action" key="action" />
        <a-table-column title="状态流转" key="statusFlow">
          <template #default="{ record }">{{ record.fromStatus }} → {{ record.toStatus }}</template>
        </a-table-column>
        <a-table-column title="备注" data-index="remark" key="remark" />
        <a-table-column title="时间" key="createTime">
          <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
        </a-table-column>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { message } from 'ant-design-vue';
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';

import FlowViewer from '@/components/FlowDesign/FlowViewer.vue';
import { advanceTaskExec, getTask, getWorkflow, rollbackTaskExec, terminateTask } from '@/api/sop';
import { formatDateTime } from '@/utils/format';

export default {
  name: 'TaskDetail',
  components: {
    FlowViewer
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const store = useStore();
    const taskDetail = ref({});
    const workflowGraph = ref({ nodes: [], edges: [] });
    const advanceForm = reactive({ result: 'PASS', feedbackData: '{}', remark: '' });
    const rollbackForm = reactive({ targetNodeId: undefined, remark: '' });

    const currentExec = computed(() => (taskDetail.value.execRecords || []).find(item => item.status === 'PENDING' || item.status === 'PROCESSING'));
    const currentNodeId = computed(() => currentExec.value?.nodeId || '');
    const currentActionPermission = computed(() => (currentExec.value?.nodeType === 'APPROVE' ? 'sop.approve' : 'sop.task'));
    const canHandleCurrentExec = computed(() => !currentExec.value
      || store.getters['user/hasPermission'](currentActionPermission.value));
    const rollbackOptions = computed(() => {
      const map = new Map();
      (taskDetail.value.execRecords || [])
        .filter(item => item.status === 'DONE')
        .forEach(item => {
          if (!map.has(item.nodeId)) {
            map.set(item.nodeId, { label: item.nodeName, value: item.nodeId });
          }
        });
      return Array.from(map.values());
    });

    const loadDetail = async () => {
      const detail = await getTask(route.params.id);
      taskDetail.value = detail;
      if (detail.workflowId) {
        const workflow = await getWorkflow(detail.workflowId);
        workflowGraph.value = {
          nodes: workflow.nodes || [],
          edges: workflow.edges || []
        };
      }
    };

    const advance = async action => {
      if (!currentExec.value) {
        return;
      }
      if (!canHandleCurrentExec.value) {
        message.warning('当前账号无该节点处理权限');
        return;
      }
      await advanceTaskExec(currentExec.value.id, {
        action,
        result: advanceForm.result,
        feedbackData: advanceForm.feedbackData,
        remark: advanceForm.remark
      });
      message.success('流程推进成功');
      loadDetail();
    };

    const rollbackCurrent = async () => {
      if (!currentExec.value || !rollbackForm.targetNodeId) {
        return;
      }
      if (!canHandleCurrentExec.value) {
        message.warning('当前账号无该节点处理权限');
        return;
      }
      await rollbackTaskExec(currentExec.value.id, {
        targetNodeId: rollbackForm.targetNodeId,
        remark: rollbackForm.remark
      });
      message.success('回退成功');
      loadDetail();
    };

    const terminateCurrentTask = async () => {
      if (!canHandleCurrentExec.value) {
        message.warning('当前账号无该节点处理权限');
        return;
      }
      await terminateTask(route.params.id);
      message.success('任务已终止');
      router.push('/sop/tasks');
    };

    loadDetail();

    return {
      taskDetail,
      workflowGraph,
      advanceForm,
      rollbackForm,
      currentExec,
      currentNodeId,
      canHandleCurrentExec,
      rollbackOptions,
      formatDateTime,
      advance,
      rollbackCurrent,
      terminateCurrentTask
    };
  }
};
</script>
