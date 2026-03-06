<template>
  <div>
    <a-card class="page-card" :title="pageTitle">
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="流程编码" required>
            <a-input v-model:value="formState.workflowCode" placeholder="例如 SOP_WARN_HANDLE" />
          </a-form-item>
          <a-form-item label="流程名称" required>
            <a-input v-model:value="formState.workflowName" placeholder="请输入流程名称" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="formState.remark" placeholder="补充说明" />
          </a-form-item>
        </div>
      </a-form>
      <div class="page-actions-right">
        <a-button @click="router.back()">返回</a-button>
        <a-button type="primary" :loading="submitLoading" @click="submitForm">保存流程</a-button>
      </div>
    </a-card>

    <a-card class="page-card" title="流程设计器">
      <FlowDesigner v-model="graphModel" />
    </a-card>
  </div>
</template>

<script>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

import FlowDesigner from '@/components/FlowDesign/FlowDesigner.vue';
import { createWorkflow, getWorkflow, updateWorkflow } from '@/api/sop';

function createDefaultGraph() {
  return {
    nodes: [
      { id: 'start_1', nodeCode: 'START_1', nodeName: '开始', nodeType: 'START', xPos: 80, yPos: 80, sortOrder: 1, properties: '{}' },
      { id: 'process_1', nodeCode: 'PROCESS_1', nodeName: '处理', nodeType: 'PROCESS', xPos: 320, yPos: 80, sortOrder: 2, properties: '{}' },
      { id: 'end_1', nodeCode: 'END_1', nodeName: '结束', nodeType: 'END', xPos: 560, yPos: 80, sortOrder: 3, properties: '{}' }
    ],
    edges: [
      { sourceNodeId: 'start_1', targetNodeId: 'process_1', sourceNodeCode: 'START_1', targetNodeCode: 'PROCESS_1', conditionExpr: '', sortOrder: 1 },
      { sourceNodeId: 'process_1', targetNodeId: 'end_1', sourceNodeCode: 'PROCESS_1', targetNodeCode: 'END_1', conditionExpr: '', sortOrder: 2 }
    ]
  };
}

export default {
  name: 'WorkflowDesigner',
  components: {
    FlowDesigner
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const submitLoading = ref(false);
    const formState = reactive({
      id: route.params.id || null,
      workflowCode: '',
      workflowName: '',
      remark: ''
    });
    const graphModel = ref(createDefaultGraph());

    const pageTitle = computed(() => (formState.id ? '编辑流程模板' : '新增流程模板'));

    const loadDetail = async () => {
      if (!formState.id) {
        return;
      }
      const detail = await getWorkflow(formState.id);
      Object.assign(formState, {
        id: detail.id,
        workflowCode: detail.workflowCode,
        workflowName: detail.workflowName,
        remark: detail.remark
      });
      graphModel.value = {
        nodes: (detail.nodes || []).map(item => ({
          id: item.id,
          nodeCode: item.nodeCode,
          nodeName: item.nodeName,
          nodeType: item.nodeType,
          assigneeType: item.assigneeType,
          assigneeId: item.assigneeId,
          sortOrder: item.sortOrder,
          xPos: item.xPos,
          yPos: item.yPos,
          properties: item.properties || '{}'
        })),
        edges: (detail.edges || []).map(item => ({
          id: item.id,
          sourceNodeId: item.sourceNodeId,
          targetNodeId: item.targetNodeId,
          conditionExpr: item.conditionExpr,
          sortOrder: item.sortOrder
        }))
      };
    };

    const submitForm = async () => {
      submitLoading.value = true;
      try {
        const nodeIdToCodeMap = new Map((graphModel.value.nodes || []).map(item => [String(item.id), item.nodeCode]));
        const payload = {
          workflowCode: formState.workflowCode,
          workflowName: formState.workflowName,
          remark: formState.remark,
          nodes: (graphModel.value.nodes || []).map(item => ({
            nodeCode: item.nodeCode,
            nodeName: item.nodeName,
            nodeType: item.nodeType,
            assigneeType: item.assigneeType,
            assigneeId: item.assigneeId,
            sortOrder: item.sortOrder,
            xPos: item.xPos,
            yPos: item.yPos,
            properties: item.properties || '{}'
          })),
          edges: (graphModel.value.edges || []).map(item => ({
            sourceNodeCode: item.sourceNodeCode || nodeIdToCodeMap.get(String(item.sourceNodeId)),
            targetNodeCode: item.targetNodeCode || nodeIdToCodeMap.get(String(item.targetNodeId)),
            conditionExpr: item.conditionExpr,
            sortOrder: item.sortOrder
          }))
        };
        if (formState.id) {
          await updateWorkflow(formState.id, payload);
        } else {
          await createWorkflow(payload);
        }
        message.success('流程保存成功');
        router.push('/sop/workflows');
      } finally {
        submitLoading.value = false;
      }
    };

    loadDetail();

    return {
      router,
      formState,
      graphModel,
      submitLoading,
      pageTitle,
      submitForm
    };
  }
};
</script>