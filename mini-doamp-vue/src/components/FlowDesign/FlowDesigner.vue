<template>
  <div class="flow-designer">
    <div class="flow-toolbar">
      <a-space wrap>
        <a-button v-for="item in nodeTypeOptions" :key="item.value" @click="addNode(item.value)">
          添加{{ item.label }}
        </a-button>
        <a-button @click="deleteSelected">删除选中</a-button>
        <a-button @click="fitView">适应画布</a-button>
      </a-space>
    </div>
    <div class="flow-content">
      <div ref="containerRef" class="flow-canvas"></div>
      <a-card class="flow-inspector" title="属性面板" size="small">
        <template v-if="selectedType === 'node'">
          <a-form layout="vertical">
            <a-form-item label="节点编码">
              <a-input v-model:value="selectedNode.nodeCode" @change="applyNodeChange" />
            </a-form-item>
            <a-form-item label="节点名称">
              <a-input v-model:value="selectedNode.nodeName" @change="applyNodeChange" />
            </a-form-item>
            <a-form-item label="节点类型">
              <a-select v-model:value="selectedNode.nodeType" :options="nodeTypeOptions" @change="applyNodeChange" />
            </a-form-item>
            <a-form-item label="处理人类型">
              <a-input v-model:value="selectedNode.assigneeType" @change="applyNodeChange" />
            </a-form-item>
            <a-form-item label="处理人标识">
              <a-input v-model:value="selectedNode.assigneeId" @change="applyNodeChange" />
            </a-form-item>
            <a-form-item label="扩展属性(JSON)">
              <a-textarea v-model:value="selectedNode.properties" :rows="4" @change="applyNodeChange" />
            </a-form-item>
          </a-form>
        </template>
        <template v-else-if="selectedType === 'edge'">
          <a-form layout="vertical">
            <a-form-item label="分支条件">
              <a-input v-model:value="selectedEdge.conditionExpr" @change="applyEdgeChange" />
            </a-form-item>
            <a-form-item label="排序号">
              <a-input-number v-model:value="selectedEdge.sortOrder" :min="1" style="width: 100%" @change="applyEdgeChange" />
            </a-form-item>
          </a-form>
        </template>
        <a-empty v-else description="请选择节点或连线后编辑属性" />
      </a-card>
    </div>
  </div>
</template>

<script>
import { Graph } from '@antv/x6';
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

const nodeColors = {
  START: '#d9f7be',
  PROCESS: '#e6f4ff',
  APPROVE: '#fff1b8',
  COPY: '#f9f0ff',
  BRANCH: '#ffd6e7',
  END: '#ffd8bf'
};

function createNodeId() {
  return `node_${Date.now()}_${Math.random().toString(16).slice(2, 6)}`;
}

export default {
  name: 'FlowDesigner',
  props: {
    modelValue: {
      type: Object,
      default: () => ({ nodes: [], edges: [] })
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const containerRef = ref(null);
    const graphRef = ref(null);
    const selectedType = ref('');
    const selectedCellId = ref('');
    const selectedNode = reactive({});
    const selectedEdge = reactive({});
    const rendering = ref(false);

    const nodeTypeOptions = [
      { label: '开始节点', value: 'START' },
      { label: '处理节点', value: 'PROCESS' },
      { label: '审批节点', value: 'APPROVE' },
      { label: '抄送节点', value: 'COPY' },
      { label: '分支节点', value: 'BRANCH' },
      { label: '结束节点', value: 'END' }
    ];

    const buildNodeConfig = node => ({
      id: String(node.id || node.nodeCode || createNodeId()),
      shape: 'rect',
      x: Number(node.xPos || 60),
      y: Number(node.yPos || 60),
      width: 150,
      height: 46,
      label: node.nodeName,
      attrs: {
        body: {
          fill: nodeColors[node.nodeType] || '#e6f4ff',
          stroke: '#3d6bff',
          rx: 8,
          ry: 8
        },
        label: {
          fill: '#1f1f1f',
          fontSize: 12
        }
      },
      data: {
        nodeCode: node.nodeCode,
        nodeName: node.nodeName,
        nodeType: node.nodeType,
        assigneeType: node.assigneeType || '',
        assigneeId: node.assigneeId || '',
        sortOrder: Number(node.sortOrder || 1),
        properties: node.properties || '{}'
      }
    });

    const serializeGraph = () => {
      const graph = graphRef.value;
      if (!graph) {
        return { nodes: [], edges: [] };
      }
      const nodes = graph.getNodes().map((node, index) => {
        const data = node.getData() || {};
        const position = node.position();
        return {
          id: node.id,
          nodeCode: data.nodeCode || node.id,
          nodeName: data.nodeName || node.getLabel(),
          nodeType: data.nodeType || 'PROCESS',
          assigneeType: data.assigneeType || '',
          assigneeId: data.assigneeId || '',
          sortOrder: index + 1,
          xPos: Math.round(position.x),
          yPos: Math.round(position.y),
          properties: data.properties || '{}'
        };
      });
      const nodeMap = new Map(nodes.map(item => [item.id, item]));
      const edges = graph.getEdges().map((edge, index) => ({
        id: edge.id,
        sourceNodeCode: nodeMap.get(edge.getSourceCellId())?.nodeCode || edge.getSourceCellId(),
        targetNodeCode: nodeMap.get(edge.getTargetCellId())?.nodeCode || edge.getTargetCellId(),
        sourceNodeId: edge.getSourceCellId(),
        targetNodeId: edge.getTargetCellId(),
        conditionExpr: edge.getData()?.conditionExpr || '',
        sortOrder: Number(edge.getData()?.sortOrder || index + 1)
      }));
      return { nodes, edges };
    };

    const emitGraph = () => {
      if (!rendering.value) {
        emit('update:modelValue', serializeGraph());
      }
    };

    const clearSelection = () => {
      selectedType.value = '';
      selectedCellId.value = '';
      Object.keys(selectedNode).forEach(key => delete selectedNode[key]);
      Object.keys(selectedEdge).forEach(key => delete selectedEdge[key]);
    };

    const selectNode = node => {
      clearSelection();
      selectedType.value = 'node';
      selectedCellId.value = node.id;
      const data = node.getData() || {};
      Object.assign(selectedNode, {
        nodeCode: data.nodeCode || node.id,
        nodeName: data.nodeName || node.getLabel(),
        nodeType: data.nodeType || 'PROCESS',
        assigneeType: data.assigneeType || '',
        assigneeId: data.assigneeId || '',
        properties: data.properties || '{}'
      });
    };

    const selectEdge = edge => {
      clearSelection();
      selectedType.value = 'edge';
      selectedCellId.value = edge.id;
      Object.assign(selectedEdge, {
        conditionExpr: edge.getData()?.conditionExpr || '',
        sortOrder: Number(edge.getData()?.sortOrder || 1)
      });
    };

    const renderGraph = model => {
      const graph = graphRef.value;
      if (!graph) {
        return;
      }
      const safeModel = model || { nodes: [], edges: [] };
      const nodes = safeModel.nodes || [];
      const edges = safeModel.edges || [];
      const codeToIdMap = new Map(nodes.map(item => [item.nodeCode, String(item.id || item.nodeCode)]));
      rendering.value = true;
      clearSelection();
      graph.clearCells();
      nodes.forEach(node => graph.addNode(buildNodeConfig(node)));
      edges.forEach((edge, index) => {
        const source = edge.sourceNodeId || codeToIdMap.get(edge.sourceNodeCode);
        const target = edge.targetNodeId || codeToIdMap.get(edge.targetNodeCode);
        if (!source || !target) {
          return;
        }
        graph.addEdge({
          id: String(edge.id || `edge_${index}`),
          source,
          target,
          attrs: {
            line: {
              stroke: '#8c8c8c',
              strokeWidth: 2,
              targetMarker: {
                name: 'block',
                width: 10,
                height: 8
              }
            }
          },
          label: edge.conditionExpr || '',
          data: {
            conditionExpr: edge.conditionExpr || '',
            sortOrder: Number(edge.sortOrder || index + 1)
          }
        });
      });
      nextTick(() => {
        graph.centerContent();
        rendering.value = false;
      });
    };

    const addNode = type => {
      const graph = graphRef.value;
      const count = graph.getNodes().length + 1;
      const defaultName = nodeTypeOptions.find(item => item.value === type)?.label || '节点';
      graph.addNode(buildNodeConfig({
        id: createNodeId(),
        nodeCode: createNodeId(),
        nodeName: `${defaultName}${count}`,
        nodeType: type,
        xPos: 60 + count * 40,
        yPos: 60 + count * 24,
        sortOrder: count,
        properties: '{}'
      }));
      emitGraph();
    };

    const applyNodeChange = () => {
      const node = graphRef.value?.getCellById(selectedCellId.value);
      if (!node) {
        return;
      }
      node.setData({ ...selectedNode });
      node.setLabel(selectedNode.nodeName);
      node.attr('body/fill', nodeColors[selectedNode.nodeType] || '#e6f4ff');
      emitGraph();
    };

    const applyEdgeChange = () => {
      const edge = graphRef.value?.getCellById(selectedCellId.value);
      if (!edge) {
        return;
      }
      edge.setData({ ...selectedEdge });
      edge.setLabelAt(0, { attrs: { label: { text: selectedEdge.conditionExpr || '' } } });
      emitGraph();
    };

    const deleteSelected = () => {
      const cell = graphRef.value?.getCellById(selectedCellId.value);
      if (cell) {
        cell.remove();
        clearSelection();
        emitGraph();
      }
    };

    const fitView = () => {
      graphRef.value?.centerContent();
      graphRef.value?.zoomToFit({ padding: 16, maxScale: 1 });
    };

    onMounted(() => {
      const graph = new Graph({
        container: containerRef.value,
        grid: true,
        background: { color: '#f8fafc' },
        selecting: true,
        connecting: {
          router: 'manhattan',
          connector: 'rounded',
          allowBlank: false,
          allowLoop: false,
          snap: true,
          highlight: true,
          createEdge() {
            return graph.createEdge({
              attrs: {
                line: {
                  stroke: '#8c8c8c',
                  strokeWidth: 2,
                  targetMarker: { name: 'block', width: 10, height: 8 }
                }
              },
              data: {
                conditionExpr: '',
                sortOrder: 1
              }
            });
          }
        }
      });
      graphRef.value = graph;
      graph.on('node:click', ({ node }) => selectNode(node));
      graph.on('edge:click', ({ edge }) => selectEdge(edge));
      graph.on('blank:click', clearSelection);
      graph.on('node:change:position', emitGraph);
      graph.on('edge:connected', emitGraph);
      graph.on('cell:removed', emitGraph);
      renderGraph(props.modelValue);
    });

    watch(() => props.modelValue, value => {
      const current = JSON.stringify(serializeGraph());
      const incoming = JSON.stringify(value || { nodes: [], edges: [] });
      if (current !== incoming) {
        renderGraph(value);
      }
    }, { deep: true });

    onBeforeUnmount(() => {
      graphRef.value?.dispose();
    });

    return {
      containerRef,
      selectedType,
      selectedNode,
      selectedEdge,
      nodeTypeOptions,
      addNode,
      applyNodeChange,
      applyEdgeChange,
      deleteSelected,
      fitView
    };
  }
};
</script>

<style lang="less" scoped>
.flow-toolbar {
  margin-bottom: 12px;
}

.flow-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
}

.flow-canvas {
  min-height: 560px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}

.flow-inspector {
  height: 560px;
  overflow: auto;
}
</style>