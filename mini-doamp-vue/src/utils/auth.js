const ACCESS_TOKEN_KEY = 'mini_doamp_access_token';
const REFRESH_TOKEN_KEY = 'mini_doamp_refresh_token';
const USER_INFO_KEY = 'mini_doamp_user_info';

export function getToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || '';
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || '';
}

export function getUserInfo() {
  const raw = localStorage.getItem(USER_INFO_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    localStorage.removeItem(USER_INFO_KEY);
    return null;
  }
}

export function setTokenPair({ token, refreshToken }) {
  if (token) {
    localStorage.setItem(ACCESS_TOKEN_KEY, token);
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function setUserInfo(userInfo) {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo || {}));
}

export function clearAuthStorage() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_INFO_KEY);
}