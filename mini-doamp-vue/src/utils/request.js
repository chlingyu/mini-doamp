import axios from 'axios';
import { message } from 'ant-design-vue';

import { clearAuthStorage, getRefreshToken, getToken, getUserInfo, setTokenPair, setUserInfo } from '@/utils/auth';

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 15000
});

const rawRequest = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 15000
});

let isRefreshing = false;
let refreshQueue = [];

function getCurrentHashPath() {
  return window.location.hash.replace(/^#/, '') || '/dashboard';
}

function redirectToLogin() {
  const currentPath = getCurrentHashPath();
  clearAuthStorage();
  if (currentPath !== '/login') {
    window.location.hash = `#/login?redirect=${encodeURIComponent(currentPath)}`;
  }
}

function redirectToDashboard() {
  const currentPath = getCurrentHashPath();
  if (currentPath !== '/dashboard') {
    window.location.hash = '#/dashboard';
  }
}

function resolveRefreshQueue(error, newToken = '') {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
      return;
    }
    resolve(newToken);
  });
  refreshQueue = [];
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('缺少 refresh token');
  }

  const response = await rawRequest.post('/auth/refresh', {
    refreshToken
  });
  const payload = response.data;
  if (!payload || payload.code !== 200 || !payload.data?.token) {
    throw new Error(payload?.msg || '刷新 token 失败');
  }

  setTokenPair({
    token: payload.data.token,
    refreshToken: payload.data.refreshToken
  });
  setUserInfo({
    ...(getUserInfo() || {}),
    userId: payload.data.userId,
    username: payload.data.username,
    realName: payload.data.realName,
    roleCode: payload.data.roleCode,
    permissions: payload.data.permissions || []
  });
  return payload.data.token;
}

async function handleUnauthorized(originalRequest = {}) {
  if (originalRequest.url?.includes('/auth/login') || originalRequest.url?.includes('/auth/refresh')) {
    redirectToLogin();
    return Promise.reject(new Error('登录已失效'));
  }

  if (originalRequest._retry) {
    redirectToLogin();
    return Promise.reject(new Error('登录已失效'));
  }
  originalRequest._retry = true;
  originalRequest.headers = originalRequest.headers || {};

  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshQueue.push({ resolve, reject });
    }).then(token => {
      originalRequest.headers.Authorization = `Bearer ${token}`;
      return service(originalRequest);
    });
  }

  isRefreshing = true;
  try {
    const newToken = await refreshAccessToken();
    resolveRefreshQueue(null, newToken);
    originalRequest.headers.Authorization = `Bearer ${newToken}`;
    return service(originalRequest);
  } catch (error) {
    resolveRefreshQueue(error);
    redirectToLogin();
    return Promise.reject(error);
  } finally {
    isRefreshing = false;
  }
}

service.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  async response => {
    const payload = response.data;
    if (payload && typeof payload.code !== 'undefined') {
      if (payload.code === 200) {
        return payload.data;
      }
      if (payload.code === 401) {
        return handleUnauthorized(response.config);
      }
      if (payload.code === 403) {
        message.warning(payload.msg || '无权限访问');
        redirectToDashboard();
        return Promise.reject(new Error(payload.msg || '无权限访问'));
      }
      message.error(payload.msg || '请求失败');
      return Promise.reject(new Error(payload.msg || '请求失败'));
    }
    return payload;
  },
  async error => {
    const status = error.response?.status;
    if (status === 401) {
      return handleUnauthorized(error.config || {});
    }
    if (status === 403) {
      message.warning('无权限访问');
      redirectToDashboard();
      return Promise.reject(error);
    }
    message.error(error.response?.data?.msg || error.message || '网络异常');
    return Promise.reject(error);
  }
);

export default service;
