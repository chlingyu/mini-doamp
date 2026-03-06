<template>
  <div class="login-page">
    <a-card class="login-card" :bordered="false">
      <div class="login-title">Mini DOAMP 登录</div>
      <div class="login-subtitle">Spring Security + JWT / Vuex / Axios 拦截器</div>
      <a-form layout="vertical" :model="formState" @finish="handleSubmit">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="formState.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block html-type="submit" :loading="submitting">登录</a-button>
        </a-form-item>
      </a-form>
      <div class="login-tip">如无初始化账号，请先通过后端建表脚本插入用户数据。</div>
    </a-card>
  </div>
</template>

<script>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { message } from 'ant-design-vue';

export default {
  name: 'LoginPage',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const store = useStore();
    const submitting = ref(false);
    const formState = reactive({
      username: '',
      password: ''
    });

    const handleSubmit = async () => {
      submitting.value = true;
      try {
        await store.dispatch('user/login', formState);
        message.success('登录成功');
        router.replace(route.query.redirect || '/dashboard');
      } catch (error) {
        // 请求层已统一提示错误，这里兜底避免未捕获 Promise 触发开发环境报错覆盖层
      } finally {
        submitting.value = false;
      }
    };

    return {
      formState,
      submitting,
      handleSubmit
    };
  }
};
</script>

<style lang="less" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #102a54 0%, #2454d7 100%);
}

.login-card {
  width: 420px;
  border-radius: 16px;
}

.login-title {
  margin-bottom: 8px;
  font-size: 28px;
  font-weight: 700;
  text-align: center;
}

.login-subtitle,
.login-tip {
  margin-bottom: 24px;
  text-align: center;
  color: #8c8c8c;
}

.login-tip {
  margin-bottom: 0;
  font-size: 12px;
}
</style>
