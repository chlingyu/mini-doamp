<template>
  <div>
    <a-card class="page-card" title="字典管理">
      <div class="table-toolbar">
        <div class="table-toolbar-left">
          <a-input v-model:value="query.keyword" placeholder="按字典编码/名称搜索" allow-clear style="width: 240px" @pressEnter="loadData" />
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button @click="resetQuery">重置</a-button>
        </div>
        <div class="table-toolbar-right">
          <a-button @click="refreshAll">刷新全部缓存</a-button>
          <a-button type="primary" @click="openCreate">新增字典</a-button>
        </div>
      </div>

      <a-table row-key="id" :data-source="dataSource" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="字典编码" data-index="dictCode" key="dictCode" />
        <a-table-column title="字典名称" data-index="dictName" key="dictName" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'default'">{{ record.status === 1 ? '启用' : '停用' }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="字典项" key="items">
          <template #default="{ record }">{{ (record.items || []).length }} 项</template>
        </a-table-column>
        <a-table-column title="创建时间" key="createTime">
          <template #default="{ record }">{{ formatDateTime(record.createTime) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="action" :width="280">
          <template #default="{ record }">
            <a-space wrap>
              <a-button type="link" @click="openEdit(record.id)">编辑</a-button>
              <a-button type="link" @click="refreshOne(record.dictCode)">刷新缓存</a-button>
              <a-popconfirm title="确认删除该字典吗？" @confirm="removeItem(record.id)">
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
          <a-form-item label="字典编码" required>
            <a-input v-model:value="formState.dictCode" :disabled="!!formState.id" />
          </a-form-item>
          <a-form-item label="字典名称" required>
            <a-input v-model:value="formState.dictName" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="formState.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="formState.remark" />
          </a-form-item>
        </div>
        <a-card size="small" title="字典项配置">
          <template #extra>
            <a-button type="link" @click="addItem">新增字典项</a-button>
          </template>
          <a-space direction="vertical" style="width: 100%">
            <div v-for="(item, index) in formState.items" :key="index" class="dict-item-row">
              <a-input v-model:value="item.itemValue" placeholder="字典值" />
              <a-input v-model:value="item.itemLabel" placeholder="字典标签" />
              <a-input-number v-model:value="item.sortOrder" :min="0" placeholder="排序" />
              <a-select v-model:value="item.status" :options="statusOptions" style="width: 120px" />
              <a-button danger @click="removeItemRow(index)">删除</a-button>
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

import { createDict, deleteDict, getDict, listDicts, refreshAllCache, refreshDictCache, updateDict } from '@/api/system';
import { formatDateTime } from '@/utils/format';

function createItem(index = 0) {
  return {
    itemValue: '',
    itemLabel: '',
    sortOrder: index,
    status: 1
  };
}

function createFormState() {
  return {
    id: null,
    dictCode: '',
    dictName: '',
    status: 1,
    remark: '',
    items: [createItem(0)]
  };
}

export default {
  name: 'DictList',
  setup() {
    const loading = ref(false);
    const submitLoading = ref(false);
    const modalVisible = ref(false);
    const dataSource = ref([]);
    const query = reactive({ keyword: '' });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条`
    });
    const formState = reactive(createFormState());
    const statusOptions = [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ];
    const modalTitle = computed(() => (formState.id ? '编辑字典' : '新增字典'));

    const loadData = async () => {
      loading.value = true;
      try {
        const response = await listDicts({
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

    const openEdit = async id => {
      const detail = await getDict(id);
      resetForm();
      Object.assign(formState, {
        ...detail,
        items: (detail.items || []).length ? detail.items.map(item => ({ ...item })) : [createItem(0)]
      });
      modalVisible.value = true;
    };

    const submitForm = async () => {
      submitLoading.value = true;
      try {
        const payload = {
          dictCode: formState.dictCode,
          dictName: formState.dictName,
          status: formState.status,
          remark: formState.remark,
          items: formState.items.map((item, index) => ({
            itemValue: item.itemValue,
            itemLabel: item.itemLabel,
            sortOrder: item.sortOrder ?? index,
            status: item.status
          }))
        };
        if (formState.id) {
          await updateDict(formState.id, payload);
        } else {
          await createDict(payload);
        }
        message.success('保存成功');
        modalVisible.value = false;
        loadData();
      } finally {
        submitLoading.value = false;
      }
    };

    const addItem = () => {
      formState.items.push(createItem(formState.items.length));
    };

    const removeItemRow = index => {
      formState.items.splice(index, 1);
      if (!formState.items.length) {
        formState.items.push(createItem(0));
      }
    };

    const removeItem = async id => {
      await deleteDict(id);
      message.success('删除成功');
      loadData();
    };

    const refreshOne = async dictCode => {
      const msg = await refreshDictCache(dictCode);
      message.success(msg || '缓存刷新成功');
    };

    const refreshAll = async () => {
      const msg = await refreshAllCache();
      message.success(msg || '全部缓存刷新成功');
    };

    loadData();

    return {
      loading,
      submitLoading,
      modalVisible,
      dataSource,
      query,
      pagination,
      formState,
      statusOptions,
      modalTitle,
      formatDateTime,
      loadData,
      resetQuery,
      handleTableChange,
      openCreate,
      openEdit,
      submitForm,
      addItem,
      removeItemRow,
      removeItem,
      refreshOne,
      refreshAll
    };
  }
};
</script>

<style lang="less" scoped>
.dict-item-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>