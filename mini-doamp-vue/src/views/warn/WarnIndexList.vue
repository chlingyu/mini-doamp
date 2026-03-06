<template>
  <div>
    <a-card class="page-card" title="预警指标管理">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <a-input v-model:value="query.keyword" placeholder="按编码/名称搜索" allow-clear style="width: 240px" @pressEnter="loadData" />
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </div>
        <div class="table-toolbar-right">
          <a-button type="primary" @click="openCreate">新增指标</a-button>
        </div>
      </div>

      <a-table row-key="id" :loading="loading" :data-source="dataSource" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="指标编码" data-index="indexCode" key="indexCode" />
        <a-table-column title="指标名称" data-index="indexName" key="indexName" />
        <a-table-column title="指标类型" key="indexType">
          <template #default="{ record }">
            <a-tag color="blue">{{ indexTypeMap[record.indexType] || record.indexType }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="阈值配置" key="thresholds" :width="260">
          <template #default="{ record }">
            <a-space wrap>
              <a-tag v-for="item in record.thresholds || []" :key="`${record.id}_${item.id || item.level}`">
                L{{ item.level }} {{ item.compareType }}
              </a-tag>
            </a-space>
          </template>
        </a-table-column>
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'default'" class="status-tag">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="创建时间" key="createTime">
          <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="action" :width="180">
          <template #default="{ record }">
            <a-space>
              <a-button type="link" @click="openEdit(record.id)">编辑</a-button>
              <a-popconfirm title="确认删除该指标吗？" @confirm="removeItem(record.id)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-modal v-model:visible="modalVisible" :title="modalTitle" width="920px" @ok="submitForm" :confirm-loading="submitLoading">
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="指标编码" required>
            <a-input v-model:value="formState.indexCode" placeholder="例如 RUNNING_CPU" />
          </a-form-item>
          <a-form-item label="指标名称" required>
            <a-input v-model:value="formState.indexName" placeholder="请输入指标名称" />
          </a-form-item>
          <a-form-item label="指标类型" required>
            <a-select v-model:value="formState.indexType" :options="indexTypeOptions" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="formState.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="数据表">
            <a-input v-model:value="formState.dataTable" placeholder="非自定义SQL时填写" />
          </a-form-item>
          <a-form-item label="数据列">
            <a-input v-model:value="formState.dataColumn" placeholder="例如 value_col" />
          </a-form-item>
          <a-form-item label="分组列">
            <a-input v-model:value="formState.groupColumn" placeholder="例如 dept_code" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="formState.remark" placeholder="补充说明" />
          </a-form-item>
        </div>
        <a-form-item v-if="formState.indexType === 'CUSTOM_SQL'" label="自定义SQL">
          <a-textarea v-model:value="formState.customSql" :rows="4" placeholder="仅允许安全 SELECT 语句" />
        </a-form-item>

        <a-card size="small" title="阈值配置">
          <template #extra>
            <a-button type="link" @click="addThreshold">新增阈值</a-button>
          </template>
          <a-space direction="vertical" style="width: 100%">
            <div v-for="(item, index) in formState.thresholds" :key="index" class="threshold-row">
              <a-input-number v-model:value="item.level" :min="1" placeholder="级别" />
              <a-select v-model:value="item.compareType" :options="compareTypeOptions" style="width: 140px" />
              <a-input-number v-model:value="item.lowerLimit" :precision="2" placeholder="下限" style="width: 140px" />
              <a-input-number v-model:value="item.upperLimit" :precision="2" placeholder="上限" style="width: 140px" />
              <a-button danger @click="removeThreshold(index)">删除</a-button>
            </div>
          </a-space>
        </a-card>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { message } from 'ant-design-vue';
import { computed, reactive, ref } from 'vue';

import { createWarnIndex, deleteWarnIndex, getWarnIndex, listWarnIndexes, updateWarnIndex } from '@/api/warn';
import { formatDateTime } from '@/utils/format';

function createThreshold(level = 1) {
  return {
    level,
    compareType: 'GT',
    upperLimit: null,
    lowerLimit: null
  };
}

function createFormState() {
  return {
    id: null,
    indexCode: '',
    indexName: '',
    indexType: 'RUNNING',
    dataTable: '',
    dataColumn: '',
    groupColumn: '',
    customSql: '',
    status: 1,
    remark: '',
    thresholds: [createThreshold(1)]
  };
}

export default {
  name: 'WarnIndexList',
  setup() {
    const loading = ref(false);
    const submitLoading = ref(false);
    const modalVisible = ref(false);
    const query = reactive({ keyword: '' });
    const dataSource = ref([]);
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });
    const formState = reactive(createFormState());

    const indexTypeMap = {
      RUNNING: '运行类',
      OPERATION: '运营类',
      BANK: '银行类',
      CHANNEL: '渠道效能类',
      EMPLOYEE: '员工类',
      BRANCH: '营业部类',
      CUSTOM_SQL: '自定义SQL类'
    };

    const indexTypeOptions = Object.entries(indexTypeMap).map(([value, label]) => ({ value, label }));
    const compareTypeOptions = [
      { label: '大于', value: 'GT' },
      { label: '小于', value: 'LT' },
      { label: '大于等于', value: 'GTE' },
      { label: '小于等于', value: 'LTE' },
      { label: '等于', value: 'EQ' },
      { label: '区间', value: 'BETWEEN' }
    ];

    const modalTitle = computed(() => (formState.id ? '编辑预警指标' : '新增预警指标'));

    const resetForm = () => {
      Object.assign(formState, createFormState());
    };

    const normalizePayload = () => ({
      indexCode: formState.indexCode,
      indexName: formState.indexName,
      indexType: formState.indexType,
      dataTable: formState.dataTable,
      dataColumn: formState.dataColumn,
      groupColumn: formState.groupColumn,
      customSql: formState.customSql,
      status: formState.status,
      remark: formState.remark,
      thresholds: formState.thresholds.map((item, index) => ({
        level: Number(item.level || index + 1),
        compareType: item.compareType,
        upperLimit: item.upperLimit,
        lowerLimit: item.lowerLimit
      }))
    });

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listWarnIndexes({
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

    const openCreate = () => {
      resetForm();
      modalVisible.value = true;
    };

    const openEdit = async id => {
      const detail = await getWarnIndex(id);
      resetForm();
      Object.assign(formState, {
        ...detail,
        thresholds: (detail.thresholds || []).length ? detail.thresholds.map(item => ({ ...item })) : [createThreshold(1)]
      });
      modalVisible.value = true;
    };

    const submitForm = async () => {
      submitLoading.value = true;
      try {
        if (formState.id) {
          await updateWarnIndex(formState.id, normalizePayload());
        } else {
          await createWarnIndex(normalizePayload());
        }
        message.success('保存成功');
        modalVisible.value = false;
        loadData();
      } finally {
        submitLoading.value = false;
      }
    };

    const removeItem = async id => {
      await deleteWarnIndex(id);
      message.success('删除成功');
      loadData();
    };

    const addThreshold = () => {
      formState.thresholds.push(createThreshold(formState.thresholds.length + 1));
    };

    const removeThreshold = index => {
      formState.thresholds.splice(index, 1);
      if (!formState.thresholds.length) {
        formState.thresholds.push(createThreshold(1));
      }
    };

    const handleTableChange = pager => {
      pagination.current = pager.current;
      pagination.pageSize = pager.pageSize;
      loadData();
    };

    loadData();

    return {
      loading,
      query,
      dataSource,
      pagination,
      modalVisible,
      modalTitle,
      submitLoading,
      formState,
      indexTypeMap,
      indexTypeOptions,
      compareTypeOptions,
      formatDateTime,
      loadData,
      resetQuery,
      openCreate,
      openEdit,
      submitForm,
      removeItem,
      addThreshold,
      removeThreshold,
      handleTableChange
    };
  }
};
</script>

<style lang="less" scoped>
.threshold-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>