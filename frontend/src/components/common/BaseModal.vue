<script setup lang="ts">
import { X } from '@lucide/vue';

defineProps<{
  open: boolean;
  title: string;
}>();

defineEmits<{
  close: [];
}>();
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="$emit('close')">
      <section class="modal">
        <header class="modal-head">
          <h3>{{ title }}</h3>
          <button class="btn small" type="button" @click="$emit('close')" aria-label="关闭">
            <X :size="16" />
          </button>
        </header>
        <div class="modal-body">
          <slot />
        </div>
        <footer v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; background: rgba(15, 23, 42, .45); display: grid; place-items: center; padding: 20px; z-index: 60; }
.modal { width: min(760px, 100%); max-height: 88vh; overflow: auto; background: white; border-radius: 8px; box-shadow: 0 24px 80px rgba(15, 23, 42, .24); }
.modal-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; border-bottom: 1px solid var(--line); }
.modal-head h3 { margin: 0; font-size: 16px; }
.modal-body { padding: 18px; }
.modal-footer { padding: 14px 18px; border-top: 1px solid var(--line); background: #f9fafb; display: flex; justify-content: flex-end; gap: 10px; }
</style>
