import request from '@/utils/request';

export function listDicts(params) {
  return request({ url: '/dict', method: 'get', params });
}

export function getDict(id) {
  return request({ url: `/dict/${id}`, method: 'get' });
}

export function listDictItems(dictCode) {
  return request({ url: `/dict/items/${dictCode}`, method: 'get' });
}

export function createDict(data) {
  return request({ url: '/dict', method: 'post', data });
}

export function updateDict(id, data) {
  return request({ url: `/dict/${id}`, method: 'put', data });
}

export function deleteDict(id) {
  return request({ url: `/dict/${id}`, method: 'delete' });
}

export function refreshDictCache(dictCode) {
  return request({
    url: dictCode ? `/cache/refresh/dict/${dictCode}` : '/cache/refresh/dict',
    method: 'post'
  });
}

export function refreshAllCache() {
  return request({ url: '/cache/refresh/all', method: 'post' });
}

export function listJobs() {
  return request({ url: '/system/job/list', method: 'get' });
}

export function listJobLogs(params) {
  return request({ url: '/system/job/log', method: 'get', params });
}