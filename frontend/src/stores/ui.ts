import { defineStore } from 'pinia';

export type ToastType = 'success' | 'warning' | 'error';

export interface ToastItem {
  id: number;
  title: string;
  message: string;
  type: ToastType;
}

export const useUiStore = defineStore('ui', {
  state: () => ({
    toasts: [] as ToastItem[],
  }),
  actions: {
    toast(title: string, message: string, type: ToastType = 'success') {
      const id = Date.now() + Math.random();
      this.toasts.push({ id, title, message, type });
      window.setTimeout(() => this.removeToast(id), 3600);
    },
    removeToast(id: number) {
      this.toasts = this.toasts.filter((toast) => toast.id !== id);
    },
  },
});
