import request from '@/utils/request';

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  });
}

export function refreshToken(refreshTokenValue) {
  return request({
    url: '/auth/refresh',
    method: 'post',
    data: {
      refreshToken: refreshTokenValue
    }
  });
}

export function fetchCurrentUser() {
  return request({
    url: '/auth/userInfo',
    method: 'get'
  });
}

export function logout(data) {
  return request({
    url: '/auth/logout',
    method: 'post',
    data
  });
}