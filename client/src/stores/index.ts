import { createPinia } from 'pinia';

export const pinia = createPinia();

export { useApplicationStore } from '@/stores/modules/application';
