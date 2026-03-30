/* eslint-env jest */

jest.mock('axios', () => {
  const instances = [];

  function buildInstance() {
    const requestHandlers = [];
    const responseHandlers = [];

    const instance = jest.fn(config => {
      let nextConfig = config || {};
      requestHandlers.forEach(handler => {
        const transformed = handler(nextConfig);
        if (transformed) {
          nextConfig = transformed;
        }
      });

      if (instance.__rejectWith) {
        const error = instance.__rejectWith;
        instance.__rejectWith = null;
        return Promise.reject(error);
      }

      const response = typeof instance.__resolveWith === 'undefined'
        ? { data: { code: 200, data: {} }, config: nextConfig }
        : instance.__resolveWith;
      instance.__resolveWith = undefined;
      return Promise.resolve(response);
    });

    instance.post = jest.fn();
    instance.interceptors = {
      request: {
        use: jest.fn(handler => {
          requestHandlers.push(handler);
        })
      },
      response: {
        use: jest.fn((onFulfilled, onRejected) => {
          responseHandlers.push({ onFulfilled, onRejected });
        })
      }
    };
    instance.__responseHandlers = responseHandlers;

    return instance;
  }

  const axios = {
    create: jest.fn(() => {
      const instance = buildInstance();
      instances.push(instance);
      return instance;
    }),
    __instances: instances
  };

  return {
    __esModule: true,
    default: axios
  };
});

jest.mock('ant-design-vue', () => ({
  message: {
    warning: jest.fn(),
    error: jest.fn()
  }
}));

const axiosMock = require('axios').default;
const messageMock = require('ant-design-vue').message;
require('@/utils/request');

const serviceInstance = axiosMock.__instances[0];
const rawRequestInstance = axiosMock.__instances[1];
const responseInterceptor = serviceInstance.__responseHandlers[0].onFulfilled;

describe('request.js auth flow', () => {
  beforeEach(() => {
    localStorage.clear();
    window.location.hash = '#/dashboard';
    messageMock.warning.mockClear();
    messageMock.error.mockClear();
    serviceInstance.mockClear();
    rawRequestInstance.post.mockReset();
    serviceInstance.__resolveWith = undefined;
    serviceInstance.__rejectWith = null;
  });

  test('401 should refresh token and retry original request', async () => {
    localStorage.setItem('mini_doamp_refresh_token', 'refresh-old');
    localStorage.setItem('mini_doamp_user_info', JSON.stringify({ username: 'admin' }));

    rawRequestInstance.post.mockResolvedValue({
      data: {
        code: 200,
        data: {
          token: 'token-new',
          refreshToken: 'refresh-new',
          userId: 1,
          username: 'admin',
          realName: 'Admin',
          roleCode: 'ADMIN',
          permissions: ['perm:a']
        }
      }
    });

    serviceInstance.__resolveWith = { retried: true };

    const result = await responseInterceptor({
      data: { code: 401, msg: 'token expired' },
      config: { url: '/warn/records', headers: {} }
    });

    expect(rawRequestInstance.post).toHaveBeenCalledWith('/auth/refresh', {
      refreshToken: 'refresh-old'
    });
    expect(result).toEqual({ retried: true });
    expect(localStorage.getItem('mini_doamp_access_token')).toBe('token-new');
    expect(localStorage.getItem('mini_doamp_refresh_token')).toBe('refresh-new');
  });

  test('403 should redirect to dashboard and reject request', async () => {
    window.location.hash = '#/system/user';

    await expect(responseInterceptor({
      data: { code: 403, msg: '无权限访问' },
      config: { url: '/system/user/list', headers: {} }
    })).rejects.toThrow('无权限访问');

    expect(messageMock.warning).toHaveBeenCalledTimes(1);
    expect(window.location.hash).toBe('#/dashboard');
  });
});
