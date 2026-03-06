<template>
  <div ref="containerRef" class="flow-viewer"></div>
</template>

<script>
import { Graph } from '@antv/x6';
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const baseColors = {
  START: '#d9f7be',
  PROCESS: '#e6f4ff',
  APPROVE: '#fff1b8',
  COPY: '#f9f0ff',
  BRANCH: '#ffd6e7',
  END: '#ffd8bf'
};

export default {
  name: 'FlowViewer',
  props: {
    nodes: {
      type: Array,
      default: () => []
    },
    edges: {
      type: Array,
      default: () => []
    },
    currentNodeId: {
      type: [String, Number],
      default: ''
    }
  },
  setup(props) {
    const containerRef = ref(null);
    const graphRef = ref(null);

    const renderGraph = () => {
      const graph = graphRef.value;
      if (!graph) {
        return;
      }
      graph.clearCells();
      const codeToIdMap = new Map((props.nodes || []).map(item => [item.nodeCode, String(item.id)]));
      (props.nodes || []).forEach(node => {
        const active = String(node.id) === String(props.currentNodeId);
        graph.addNode({
          id: String(node.id),
          shape: 'rect',
          x: Number(node.xPos || 60),
          y: Number(node.yPos || 60),
          width: 150,
          height: 46,
          label: node.nodeName,
          attrs: {
            body: {
              fill: baseColors[node.nodeType] || '#e6f4ff',
              stroke: active ? '#ff4d4f' : '#3d6bff',
              strokeWidth: active ? 3 : 1,
              rx: 8,
              ry: 8
            }
          }
        });
      });
      (props.edges || []).forEach((edge, index) => {
        const source = edge.sourceNodeId || codeToIdMap.get(edge.sourceNodeCode);
        const target = edge.targetNodeId || codeToIdMap.get(edge.targetNodeCode);
        if (!source || !target) {
          return;
        }
        graph.addEdge({
          id: String(edge.id || `viewer_edge_${index}`),
          source,
          target,
          label: edge.conditionExpr || '',
          attrs: {
            line: {
              stroke: '#8c8c8c',
              strokeWidth: 2,
              targetMarker: { name: 'block', width: 10, height: 8 }
            }
          }
        });
      });
      nextTick(() => {
        graph.centerContent();
        graph.zoomToFit({ padding: 16, maxScale: 1 });
      });
    };

    onMounted(() => {
      graphRef.value = new Graph({
        container: containerRef.value,
        grid: true,
        background: { color: '#fafafa' },
        panning: true,
        mousewheel: true,
        interacting: false
      });
      renderGraph();
    });

    watch(() => [props.nodes, props.edges, props.currentNodeId], renderGraph, { deep: true });

    onBeforeUnmount(() => {
      graphRef.value?.dispose();
    });

    return {
      containerRef
    };
  }
};
</script>

<style lang="less" scoped>
.flow-viewer {
  min-height: 420px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}
</style>