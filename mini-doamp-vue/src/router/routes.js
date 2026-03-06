import AppLayout from '@/layout/AppLayout.vue';

export const menuGroups = [
  {
    key: 'dashboard',
    title: '工作台',
    items: [
      { path: '/dashboard', title: '首页', permission: 'dashboard.view' }
    ]
  },
  {
    key: 'warn',
    title: '预警中心',
    items: [
      { path: '/warn/indexes', title: '预警指标', permission: 'warn.index' },
      { path: '/warn/rules', title: '预警规则', permission: 'warn.rule' },
      { path: '/warn/records', title: '预警记录', permission: 'warn.record' },
      { path: '/warn/messages', title: '消息记录', permission: 'warn.message' }
    ]
  },
  {
    key: 'sop',
    title: 'SOP 工作流',
    items: [
      { path: '/sop/workflows', title: '流程模板', permission: 'sop.workflow' },
      { path: '/sop/templates', title: '任务模板', permission: 'sop.template' },
      { path: '/sop/tasks', title: '任务执行中心', permissions: ['sop.task', 'sop.approve'] }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    items: [
      { path: '/system/dicts', title: '字典管理', permission: 'system.dict' },
      { path: '/system/jobs', title: '任务监控', permission: 'system.job' }
    ]
  }
];

export default [
  {
    path: '/login',
    name: 'LoginPage',
    component: () => import('@/views/login/LoginPage.vue'),
    meta: {
      public: true,
      title: '登录'
    }
  },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'HomePage',
        component: () => import('@/views/dashboard/HomePage.vue'),
        meta: { title: '首页', permission: 'dashboard.view' }
      },
      {
        path: 'warn/indexes',
        name: 'WarnIndexList',
        component: () => import('@/views/warn/WarnIndexList.vue'),
        meta: { title: '预警指标', permission: 'warn.index' }
      },
      {
        path: 'warn/rules',
        name: 'WarnRuleList',
        component: () => import('@/views/warn/WarnRuleList.vue'),
        meta: { title: '预警规则', permission: 'warn.rule' }
      },
      {
        path: 'warn/rules/create',
        name: 'WarnRuleCreate',
        component: () => import('@/views/warn/WarnRuleForm.vue'),
        meta: { title: '新增预警规则', permission: 'warn.rule', hidden: true }
      },
      {
        path: 'warn/rules/:id/edit',
        name: 'WarnRuleEdit',
        component: () => import('@/views/warn/WarnRuleForm.vue'),
        meta: { title: '编辑预警规则', permission: 'warn.rule', hidden: true }
      },
      {
        path: 'warn/records',
        name: 'WarnRecordList',
        component: () => import('@/views/warn/WarnRecordList.vue'),
        meta: { title: '预警记录', permission: 'warn.record' }
      },
      {
        path: 'warn/messages',
        name: 'MsgRecordList',
        component: () => import('@/views/warn/MsgRecordList.vue'),
        meta: { title: '消息记录', permission: 'warn.message' }
      },
      {
        path: 'sop/workflows',
        name: 'WorkflowList',
        component: () => import('@/views/sop/WorkflowList.vue'),
        meta: { title: '流程模板', permission: 'sop.workflow' }
      },
      {
        path: 'sop/workflows/create',
        name: 'WorkflowCreate',
        component: () => import('@/views/sop/WorkflowDesigner.vue'),
        meta: { title: '新增流程', permission: 'sop.workflow', hidden: true }
      },
      {
        path: 'sop/workflows/:id/edit',
        name: 'WorkflowEdit',
        component: () => import('@/views/sop/WorkflowDesigner.vue'),
        meta: { title: '编辑流程', permission: 'sop.workflow', hidden: true }
      },
      {
        path: 'sop/templates',
        name: 'TaskTemplateList',
        component: () => import('@/views/sop/TaskTemplateList.vue'),
        meta: { title: '任务模板', permission: 'sop.template' }
      },
      {
        path: 'sop/tasks',
        name: 'TaskCenter',
        component: () => import('@/views/sop/TaskCenter.vue'),
        meta: { title: '任务执行中心', permissions: ['sop.task', 'sop.approve'] }
      },
      {
        path: 'sop/tasks/:id',
        name: 'TaskDetail',
        component: () => import('@/views/sop/TaskDetail.vue'),
        meta: { title: '任务详情', permissions: ['sop.task', 'sop.approve'], hidden: true }
      },
      {
        path: 'system/dicts',
        name: 'DictList',
        component: () => import('@/views/system/DictList.vue'),
        meta: { title: '字典管理', permission: 'system.dict' }
      },
      {
        path: 'system/jobs',
        name: 'JobMonitor',
        component: () => import('@/views/system/JobMonitor.vue'),
        meta: { title: '任务监控', permission: 'system.job' }
      }
    ]
  }
];
