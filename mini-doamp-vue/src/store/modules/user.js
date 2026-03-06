import { fetchCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth';
import { clearAuthStorage, getRefreshToken, getToken, getUserInfo, setTokenPair, setUserInfo } from '@/utils/auth';

function resolvePermissions(profile) {
  return Array.isArray(profile?.permissions) ? profile.permissions : [];
}

function getDefaultState() {
  const profile = getUserInfo() || {};
  return {
    token: getToken(),
    refreshToken: getRefreshToken(),
    profile,
    permissions: resolvePermissions(profile)
  };
}

export default {
  namespaced: true,
  state: getDefaultState(),
  getters: {
    token: state => state.token,
    refreshToken: state => state.refreshToken,
    profile: state => state.profile,
    permissions: state => state.permissions,
    hasPermission: state => permission => {
      if (!permission) {
        return true;
      }
      return state.permissions.includes(permission);
    }
  },
  mutations: {
    SET_SESSION(state, payload) {
      state.token = payload.token || '';
      state.refreshToken = payload.refreshToken || '';
      state.profile = payload.profile || {};
      state.permissions = resolvePermissions(state.profile);
      setTokenPair({ token: state.token, refreshToken: state.refreshToken });
      setUserInfo(state.profile);
    },
    UPDATE_PROFILE(state, profile) {
      state.profile = profile || {};
      state.permissions = resolvePermissions(state.profile);
      setUserInfo(state.profile);
    },
    CLEAR_STATE(state) {
      Object.assign(state, {
        token: '',
        refreshToken: '',
        profile: {},
        permissions: []
      });
      clearAuthStorage();
    },
    RESTORE_STATE(state) {
      Object.assign(state, getDefaultState());
    }
  },
  actions: {
    async login({ commit }, payload) {
      const response = await loginApi(payload);
      commit('SET_SESSION', {
        token: response.token,
        refreshToken: response.refreshToken,
        profile: {
          userId: response.userId,
          username: response.username,
          realName: response.realName,
          roleCode: response.roleCode,
          permissions: response.permissions || []
        }
      });
      return response;
    },
    async fetchProfile({ commit, state }) {
      if (!state.token) {
        return null;
      }
      const profile = await fetchCurrentUser();
      commit('UPDATE_PROFILE', {
        ...state.profile,
        ...profile,
        permissions: profile.permissions || state.profile.permissions || []
      });
      return profile;
    },
    async logout({ commit, state }) {
      try {
        await logoutApi({
          refreshToken: state.refreshToken
        });
      } catch (error) {
        // no-op
      }
      commit('CLEAR_STATE');
    }
  }
};