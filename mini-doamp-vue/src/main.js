import { createApp } from 'vue';
import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/antd.css';

import App from './App.vue';
import router from './router';
import store from './store';
import './styles/index.less';

async function bootstrap() {
  store.commit('user/RESTORE_STATE');
  if (store.getters['user/token']) {
    try {
      await store.dispatch('user/fetchProfile');
    } catch (error) {
      store.commit('user/CLEAR_STATE');
    }
  }

  createApp(App)
    .use(store)
    .use(router)
    .use(Antd)
    .mount('#app');
}

bootstrap();