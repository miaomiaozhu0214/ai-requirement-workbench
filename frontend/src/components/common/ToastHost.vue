<script setup lang="ts">
import { AlertCircle, CheckCircle2, Info, X } from '@lucide/vue';
import { useUiStore } from '../../stores/ui';

const ui = useUiStore();
</script>

<template>
  <div class="toast-wrap">
    <div v-for="toast in ui.toasts" :key="toast.id" class="toast" :class="toast.type">
      <CheckCircle2 v-if="toast.type === 'success'" :size="18" />
      <AlertCircle v-else-if="toast.type === 'error'" :size="18" />
      <Info v-else :size="18" />
      <div>
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>
      <button class="toast-close" type="button" @click="ui.removeToast(toast.id)" aria-label="关闭提示">
        <X :size="14" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.toast-wrap { position: fixed; top: 84px; right: 20px; z-index: 80; display: grid; gap: 10px; width: min(420px, calc(100vw - 40px)); }
.toast { display: grid; grid-template-columns: 20px 1fr 24px; gap: 10px; align-items: start; padding: 12px; border: 1px solid var(--line); border-left: 4px solid var(--primary); border-radius: 8px; background: white; box-shadow: 0 12px 28px rgba(15, 23, 42, .12); }
.toast.success { border-left-color: var(--success); }
.toast.warning { border-left-color: var(--warning); }
.toast.error { border-left-color: var(--danger); }
.toast strong, .toast span { display: block; }
.toast strong { font-size: 13px; }
.toast span { margin-top: 3px; color: var(--muted); font-size: 12px; line-height: 1.45; }
.toast-close { border: 0; background: transparent; color: var(--muted); padding: 3px; display: grid; place-items: center; }
</style>
