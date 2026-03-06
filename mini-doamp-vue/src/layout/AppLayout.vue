<template>
  <a-layout class="layout-shell">
    <a-layout-sider v-model:collapsed="collapsed" collapsible width="240">
      <div class="layout-logo">Mini DOAMP</div>
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :open-keys="openKeys"
        @openChange="handleOpenChange"
      >
        <a-sub-menu v-for="group in visibleMenuGroups" :key="group.key">
          <template #title>
            <span>{{ group.title }}</span>
          </template>
          <a-menu-item v-for="item in group.items" :key="item.path" @click="goTo(item.path)">
            {{ item.title }}
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="layout-header">
        <div class="layout-header-title">{{ currentTitle }}</div>
        <div class="layout-header-right">
          <a-tag color="blue">{{ profile.roleCode || 'ADMIN' }}</a-tag>
          <a-dropdown>
            <a class="layout-user-link" @click.prevent>
              {{ profile.realName || profile.username || '未登录' }}
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goTo('/dashboard')">返回首页</a-menu-item>
                <a-menu-item @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="layout-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';

import { menuGroups } from '@/router/routes';

export default {
  name: 'AppLayout',
  setup() {
    const collapsed = ref(false);
    const route = useRoute();
    const router = useRouter();
    const store = useStore();
    const openKeys = ref(['dashboard']);

    const hasMenuPermission = item => {
      const requiredPermissions = item.permissions || item.permission;
      if (!requiredPermissions) {
        return true;
      }
      if (Array.isArray(requiredPermissions)) {
        return requiredPermissions.some(permission => store.getters['user/hasPermission'](permission));
      }
      return store.getters['user/hasPermission'](requiredPermissions);
    };

    const profile = computed(() => store.getters['user/profile']);
    const selectedKeys = computed(() => [route.path]);
    const currentTitle = computed(() => route.meta?.title || 'Mini DOAMP');
    const visibleMenuGroups = computed(() => menuGroups
      .map(group => ({
        ...group,
        items: group.items.filter(item => hasMenuPermission(item))
      }))
      .filter(group => group.items.length));

    watch(() => route.path, value => {
      const firstSegment = value.split('/')[1] || 'dashboard';
      openKeys.value = [firstSegment];
    }, { immediate: true });

    const goTo = path => {
      if (route.path !== path) {
        router.push(path);
      }
    };

    const handleOpenChange = keys => {
      openKeys.value = keys;
    };

    const handleLogout = async () => {
      await store.dispatch('user/logout');
      router.replace('/login');
    };

    return {
      collapsed,
      profile,
      selectedKeys,
      currentTitle,
      visibleMenuGroups,
      openKeys,
      goTo,
      handleOpenChange,
      handleLogout
    };
  }
};
</script>

<style lang="less" scoped>
.layout-shell {
  min-height: 100vh;
}

.layout-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.layout-header-title {
  font-size: 18px;
  font-weight: 600;
}

.layout-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout-user-link {
  color: #262626;
}

.layout-content {
  padding: 24px;
}
</style>
