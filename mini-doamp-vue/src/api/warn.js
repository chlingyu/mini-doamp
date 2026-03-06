import request from '@/utils/request';

export function listWarnIndexes(params) {
  return request({ url: '/warn/indexes', method: 'get', params });
}

export function getWarnIndex(id) {
  return request({ url: `/warn/indexes/${id}`, method: 'get' });
}

export function createWarnIndex(data) {
  return request({ url: '/warn/indexes', method: 'post', data });
}

export function updateWarnIndex(id, data) {
  return request({ url: `/warn/indexes/${id}`, method: 'put', data });
}

export function deleteWarnIndex(id) {
  return request({ url: `/warn/indexes/${id}`, method: 'delete' });
}

export function listWarnRules(params) {
  return request({ url: '/warn/rules', method: 'get', params });
}

export function getWarnRule(id) {
  return request({ url: `/warn/rules/${id}`, method: 'get' });
}

export function createWarnRule(data) {
  return request({ url: '/warn/rules', method: 'post', data });
}

export function updateWarnRule(id, data) {
  return request({ url: `/warn/rules/${id}`, method: 'put', data });
}

export function deleteWarnRule(id) {
  return request({ url: `/warn/rules/${id}`, method: 'delete' });
}

export function updateWarnRuleStatus(id, status) {
  return request({
    url: `/warn/rules/${id}/status`,
    method: 'put',
    params: { status }
  });
}

export function triggerWarnRule(id) {
  return request({ url: `/warn/rules/${id}/trigger`, method: 'post' });
}

export function listWarnRecords(params) {
  return request({ url: '/warn/records', method: 'get', params });
}

export function listMsgRecords(params) {
  return request({ url: '/msg/records', method: 'get', params });
}

export function retryMsgRecord(id) {
  return request({ url: `/msg/records/${id}/retry`, method: 'post' });
}