import request from '@/utils/request';

export function listWorkflows(params) {
  return request({ url: '/sop/workflows', method: 'get', params });
}

export function getWorkflow(id) {
  return request({ url: `/sop/workflows/${id}`, method: 'get' });
}

export function createWorkflow(data) {
  return request({ url: '/sop/workflows', method: 'post', data });
}

export function updateWorkflow(id, data) {
  return request({ url: `/sop/workflows/${id}`, method: 'put', data });
}

export function publishWorkflow(id) {
  return request({ url: `/sop/workflows/${id}/publish`, method: 'put' });
}

export function disableWorkflow(id) {
  return request({ url: `/sop/workflows/${id}/disable`, method: 'put' });
}

export function listTaskTemplates(params) {
  return request({ url: '/sop/task-templates', method: 'get', params });
}

export function createTaskTemplate(data) {
  return request({ url: '/sop/task-templates', method: 'post', data });
}

export function updateTaskTemplate(id, data) {
  return request({ url: `/sop/task-templates/${id}`, method: 'put', data });
}

export function deleteTaskTemplate(id) {
  return request({ url: `/sop/task-templates/${id}`, method: 'delete' });
}

export function updateTaskTemplateStatus(id, status) {
  return request({
    url: `/sop/task-templates/${id}/status`,
    method: 'put',
    params: { status }
  });
}

export function listTasks(params) {
  return request({ url: '/sop/tasks', method: 'get', params });
}

export function getTask(id) {
  return request({ url: `/sop/tasks/${id}`, method: 'get' });
}

export function createTask(data) {
  return request({ url: '/sop/tasks', method: 'post', data });
}

export function terminateTask(id) {
  return request({ url: `/sop/tasks/${id}/terminate`, method: 'put' });
}

export function listMyTodo(params) {
  return request({ url: '/sop/tasks/my-todo', method: 'get', params });
}

export function listMyDone(params) {
  return request({ url: '/sop/tasks/my-done', method: 'get', params });
}

export function advanceTaskExec(id, data) {
  return request({ url: `/sop/task-execs/${id}/advance`, method: 'post', data });
}

export function rollbackTaskExec(id, data) {
  return request({ url: `/sop/task-execs/${id}/rollback`, method: 'post', data });
}