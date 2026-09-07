import { mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';

import AppShell from '@/layouts/AppShell.vue';

describe('AppShell', () => {
  it('renders the primary knowledge journal navigation', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>首页</div>' } }],
    });
    await router.push('/');
    await router.isReady();

    const wrapper = mount(AppShell, {
      global: { plugins: [createPinia(), router] },
    });

    expect(wrapper.text()).toContain('Blob');
    expect(wrapper.text()).toContain('日志');
    expect(wrapper.text()).toContain('标签');
    expect(wrapper.text()).toContain('成长');
    expect(wrapper.text()).toContain('设置');
  });
});
