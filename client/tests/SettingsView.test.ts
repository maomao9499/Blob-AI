import { flushPromises, mount } from '@vue/test-utils';
import { ElButton, ElCard, ElTag } from 'element-plus';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { systemApi } from '@/api/system';
import SettingsView from '@/pages/settings/index.vue';

describe('SettingsView', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('shows MySQL and Redis dependency status independently', async () => {
    vi.spyOn(systemApi, 'getDependencies').mockResolvedValue({
      mysql: { status: 'UP', message: '连接正常' },
      redis: { status: 'DOWN', message: 'Redis 未启动' },
    });

    const wrapper = mount(SettingsView, {
      global: { components: { ElButton, ElCard, ElTag } },
    });
    await flushPromises();

    expect(wrapper.text()).toContain('MySQL');
    expect(wrapper.text()).toContain('连接正常');
    expect(wrapper.text()).toContain('Redis');
    expect(wrapper.text()).toContain('Redis 未启动');
  });
});
