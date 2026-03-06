<template>
  <div>
    <a-card class="page-card" :title="pageTitle">
      <a-form layout="vertical">
        <div class="form-grid">
          <a-form-item label="规则名称" required>
            <a-input v-model:value="formState.ruleName" placeholder="请输入规则名称" />
          </a-form-item>
          <a-form-item label="预警指标" required>
            <a-select v-model:value="formState.indexId" :options="indexOptions" placeholder="请选择预警指标" @change="handleIndexChange" />
          </a-form-item>
          <a-form-item label="通知方式">
            <a-checkbox-group v-model:value="notifyTypes" :options="notifyTypeOptions" />
          </a-form-item>
          <a-form-item label="状态">
            <a-radio-group v-model:value="formState.status">
              <a-radio :value="1">启用</a-radio>
              <a-radio :value="0">停用</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="接收人ID列表">
            <a-input v-model:value="formState.receiverIds" placeholder="多个用逗号分隔" />
          </a-form-item>
          <a-form-item label="Cron 表达式">
            <a-input v-model:value="formState.cronExpr" placeholder="例如 0 0/5 * * * ?" />
          </a-form-item>
        </div>
      </a-form>
      <div class="page-actions-right">
        <a-button @click="router.back()">返回</a-button>
        <a-button @click="goToIndexList">管理指标</a-button>
        <a-button type="primary" :loading="submitLoading" @click="submitForm">保存</a-button>
      </div>
    </a-card>

    <a-card class="page-card" title="指标配置联动">
      <template #extra>
        <span class="muted-text">当前页面可直接调整已选指标的类型与阈值</span>
      </template>
      <a-empty v-if="!indexState.id" description="请先选择预警指标" />
      <template v-else>
        <div class="form-grid">
          <a-form-item label="指标编码">
            <a-input v-model:value="indexState.indexCode" disabled />
          </a-form-item>
          <a-form-item label="指标名称">
            <a-input v-model:value="indexState.indexName" disabled />
          </a-form-item>
          <a-form-item label="指标类型">
            <a-select v-model:value="indexState.indexType" :options="indexTypeOptions" />
          </a-form-item>
          <a-form-item label="数据表">
            <a-input v-model:value="indexState.dataTable" />
          </a-form-item>
          <a-form-item label="数据列">
            <a-input v-model:value="indexState.dataColumn" />
          </a-form-item>
          <a-form-item label="分组列">
            <a-input v-model:value="indexState.groupColumn" />
          </a-form-item>
        </div>
        <a-form-item v-if="indexState.indexType === 'CUSTOM_SQL'" label="自定义SQL">
          <a-textarea v-model:value="indexState.customSql" :rows="4" />
        </a-form-item>
        <a-card size="small" title="阈值配置">
          <template #extra>
            <a-button type="link" @click="addThreshold">新增阈值</a-button>
          </template>
          <a-space direction="vertical" style="width: 100%">
            <div v-for="(item, index) in indexState.thresholds" :key="index" class="threshold-row">
              <a-input-number v-model:value="item.level" :min="1" placeholder="级别" />
              <a-select v-model:value="item.compareType" :options="compareTypeOptions" style="width: 140px" />
              <a-input-number v-model:value="item.lowerLimit" :precision="2" placeholder="下限" style="width: 140px" />
              <a-input-number v-model:value="item.upperLimit" :precision="2" placeholder="上限" style="width: 140px" />
              <a-button danger @click="removeThreshold(index)">删除</a-button>
            </div>
          </a-space>
        </a-card>
      </template>
    </a-card>
  </div>
</template>

<script>
import { message } from 'ant-design-vue';
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { createWarnRule, getWarnIndex, getWarnRule, listWarnIndexes, updateWarnIndex, updateWarnRule } from '@/api/warn';

function createIndexState() {
  return {
    id: null,
    indexCode: '',
    indexName: '',
    indexType: 'RUNNING',
    dataTable: '',
    dataColumn: '',
    groupColumn: '',
    customSql: '',
    remark: '',
    status: 1,
    thresholds: []
  };
}

export default {
  name: 'WarnRuleForm',
  setup() {
    const route = useRoute();
    const router = useRouter();
    const submitLoading = ref(false);
    const formState = reactive({
      id: route.params.id || null,
      ruleName: '',
      indexId: undefined,
      receiverIds: '',
      cronExpr: '',
      status: 1
    });
    const indexState = reactive(createIndexState());
    const indexOptions = ref([]);
    const notifyTypes = ref([]);

    const pageTitle = computed(() => (formState.id ? '编辑预警规则' : '新增预警规则'));
    const notifyTypeOptions = [
      { label: '短信', value: 'sms' },
      { label: '邮件', value: 'email' },
      { label: '企业微信', value: 'wxwork' }
    ];
    const indexTypeOptions = [
      { label: '运行类', value: 'RUNNING' },
      { label: '运营类', value: 'OPERATION' },
      { label: '银行类', value: 'BANK' },
      { label: '渠道效能类', value: 'CHANNEL' },
      { label: '员工类', value: 'EMPLOYEE' },
      { label: '营业部类', value: 'BRANCH' },
      { label: '自定义SQL类', value: 'CUSTOM_SQL' }
    ];
    const compareTypeOptions = [
      { label: '大于', value: 'GT' },
      { label: '小于', value: 'LT' },
      { label: '大于等于', value: 'GTE' },
      { label: '小于等于', value: 'LTE' },
      { label: '等于', value: 'EQ' },
      { label: '区间', value: 'BETWEEN' }
    ];

    const loadIndexOptions = async () => {
      const response = await listWarnIndexes({ pageNum: 1, pageSize: 200 });
      indexOptions.value = (response.records || []).map(item => ({
        label: `${item.indexName} (${item.indexCode})`,
        value: item.id
      }));
    };

    const loadIndexDetail = async id => {
      const detail = await getWarnIndex(id);
      Object.assign(indexState, createIndexState(), {
        ...detail,
        thresholds: (detail.thresholds || []).map(item => ({ ...item }))
      });
    };

    const handleIndexChange = async value => {
      if (!value) {
        Object.assign(indexState, createIndexState());
        return;
      }
      await loadIndexDetail(value);
    };

    const loadRuleDetail = async () => {
      if (!formState.id) {
        return;
      }
      const detail = await getWarnRule(formState.id);
      Object.assign(formState, {
        id: detail.id,
        ruleName: detail.ruleName,
        indexId: detail.indexId,
        receiverIds: detail.receiverIds,
        cronExpr: detail.cronExpr,
        status: detail.status
      });
      notifyTypes.value = detail.notifyType ? String(detail.notifyType).split(',').filter(Boolean) : [];
      if (detail.indexId) {
        await loadIndexDetail(detail.indexId);
      }
    };

    const submitForm = async () => {
      submitLoading.value = true;
      try {
        if (indexState.id) {
          await updateWarnIndex(indexState.id, {
            indexCode: indexState.indexCode,
            indexName: indexState.indexName,
            indexType: indexState.indexType,
            dataTable: indexState.dataTable,
            dataColumn: indexState.dataColumn,
            groupColumn: indexState.groupColumn,
            customSql: indexState.customSql,
            status: indexState.status,
            remark: indexState.remark,
            thresholds: (indexState.thresholds || []).map(item => ({
              level: Number(item.level),
              compareType: item.compareType,
              upperLimit: item.upperLimit,
              lowerLimit: item.lowerLimit
            }))
          });
        }
        const payload = {
          ruleName: formState.ruleName,
          indexId: formState.indexId,
          notifyType: notifyTypes.value.join(','),
          receiverIds: formState.receiverIds,
          cronExpr: formState.cronExpr,
          status: formState.status
        };
        if (formState.id) {
          await updateWarnRule(formState.id, payload);
        } else {
          await createWarnRule(payload);
        }
        message.success('保存成功');
        router.push('/warn/rules');
      } finally {
        submitLoading.value = false;
      }
    };

    const addThreshold = () => {
      indexState.thresholds.push({
        level: (indexState.thresholds?.length || 0) + 1,
        compareType: 'GT',
        upperLimit: null,
        lowerLimit: null
      });
    };

    const removeThreshold = index => {
      indexState.thresholds.splice(index, 1);
    };

    const goToIndexList = () => router.push('/warn/indexes');

    loadIndexOptions();
    loadRuleDetail();

    return {
      router,
      formState,
      indexState,
      indexOptions,
      notifyTypes,
      notifyTypeOptions,
      indexTypeOptions,
      compareTypeOptions,
      pageTitle,
      submitLoading,
      handleIndexChange,
      submitForm,
      addThreshold,
      removeThreshold,
      goToIndexList
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