import { createRouter, createWebHashHistory } from 'vue-router';

import store from '@/store';
import routes from './routes';

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

function hasRoutePermission(permissionMeta) {
  if (!permissionMeta) {
    return true;
  }
  if (Array.isArray(permissionMeta)) {
    return permissionMeta.some(permission => store.getters['user/hasPermission'](permission));
  }
  return store.getters['user/hasPermission'](permissionMeta);
}

router.beforeEach((to, from, next) => {
  if (to.meta?.public) {
    if (to.path === '/login' && store.getters['user/token']) {
      next('/dashboard');
      return;
    }
    next();
    return;
  }

  const token = store.getters['user/token'];
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  const permission = to.meta?.permissions || to.meta?.permission;
  if (!hasRoutePermission(permission)) {
    next('/dashboard');
    return;
  }

  next();
});

export default router;
