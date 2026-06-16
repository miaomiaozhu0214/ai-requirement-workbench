<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Edit3, Plus, RefreshCcw, Search, Trash2 } from '@lucide/vue';
import BaseModal from '../components/common/BaseModal.vue';
import { productLineApi } from '../api/productLineApi';
import { useUiStore } from '../stores/ui';
import type { ProductLine, ProductLinePlatform, ProductLineType } from '../types';

const ui = useUiStore();
const loading = ref(false);
const keyword = ref('');
const productLines = ref<ProductLine[]>([]);

const modalOpen = ref(false);
const modalMode = ref<'create' | 'edit'>('create');
const formError = ref('');
const form = reactive({
  id: null as number | null,
  lineCode: '',
  lineName: '',
  ownersText: '',
  productType: 'face_to_customer' as ProductLineType,
  platforms: ['yunlian_front'] as ProductLinePlatform[],
  description: '',
});

const confirmModal = reactive({
  open: false,
  productLine: null as ProductLine | null,
});

const typeLabels: Record<ProductLineType, string> = {
  face_to_customer: '面客产品',
  internal: '内部产品',
  public_service: '公共服务',
  design_spec: '设计规范',
};

const platformOptions: Array<{ value: ProductLinePlatform; label: string }> = [
  { value: 'yunlian_front', label: '云链前台' },
  { value: 'yunzu_front', label: '云租前台' },
  { value: 'yunzu_app', label: '云租APP' },
  { value: 'middle_platform', label: '中台' },
  { value: 'yunlian_back', label: '云链后台' },
  { value: 'yunzu_back', label: '云租后台' },
  { value: 'lianxin', label: '链信' },
  { value: 'lianxin_app', label: '链信APP' },
];

const filteredCountText = computed(() => loading.value ? '加载中' : `${productLines.value.length} 条产品线`);

async function load() {
  loading.value = true;
  try {
    productLines.value = await productLineApi.list(keyword.value.trim() || undefined);
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '产品线列表加载失败', 'error');
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  modalMode.value = 'create';
  formError.value = '';
  form.id = null;
  form.lineCode = '';
  form.lineName = '';
  form.ownersText = '';
  form.productType = 'face_to_customer';
  form.platforms = ['yunlian_front'];
  form.description = '';
  modalOpen.value = true;
}

function openEdit(line: ProductLine) {
  modalMode.value = 'edit';
  formError.value = '';
  form.id = line.id;
  form.lineCode = line.lineCode || '';
  form.lineName = line.lineName;
  form.ownersText = (line.owners || []).join(', ');
  form.productType = line.productType;
  form.platforms = [...(line.platforms || [])];
  form.description = line.description || '';
  modalOpen.value = true;
}

function splitOwners() {
  return form.ownersText
    .split(/[,，]/)
    .map((owner) => owner.trim())
    .filter(Boolean);
}

function togglePlatform(platform: ProductLinePlatform, checked: boolean) {
  if (checked && !form.platforms.includes(platform)) {
    form.platforms.push(platform);
  }
  if (!checked) {
    form.platforms = form.platforms.filter((item) => item !== platform);
  }
}

function validateForm() {
  if (!form.lineName.trim()) {
    return '请输入产品线名称';
  }
  const owners = splitOwners();
  if (owners.length === 0) {
    return '请至少填写一个负责人';
  }
  if (form.platforms.length === 0) {
    return '请至少选择一个涉及平台';
  }
  if (form.description.length > 2000) {
    return '业务介绍不能超过2000字符';
  }
  return '';
}

async function submit() {
  const error = validateForm();
  if (error) {
    formError.value = error;
    return;
  }
  const payload = {
    lineCode: form.lineCode.trim(),
    lineName: form.lineName.trim(),
    owners: splitOwners(),
    productType: form.productType,
    platforms: form.platforms,
    description: form.description.trim(),
  };
  try {
    if (modalMode.value === 'create') {
      await productLineApi.create(payload);
      ui.toast('新增成功', '产品线已创建', 'success');
    } else if (form.id) {
      await productLineApi.update(form.id, payload);
      ui.toast('保存成功', '产品线已更新', 'success');
    }
    modalOpen.value = false;
    await load();
  } catch (requestError) {
    formError.value = requestError instanceof Error ? requestError.message : '保存失败';
  }
}

function openDelete(line: ProductLine) {
  confirmModal.productLine = line;
  confirmModal.open = true;
}

async function confirmDelete() {
  if (!confirmModal.productLine) {
    return;
  }
  try {
    await productLineApi.remove(confirmModal.productLine.id);
    ui.toast('删除成功', '产品线已软删除', 'success');
    confirmModal.open = false;
    await load();
  } catch (error) {
    ui.toast('删除失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  }
}

function platformLabel(platform: ProductLinePlatform) {
  return platformOptions.find((option) => option.value === platform)?.label || platform;
}

onMounted(load);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>产品线配置</h1>
      <p>维护全量功能库和需求分析使用的产品线基础信息</p>
    </div>
    <button class="btn primary" type="button" @click="openCreate">
      <Plus :size="16" />
      新增产品线
    </button>
  </header>

  <section class="page-body product-line-page">
    <section class="panel">
      <div class="panel-head">
        <div>
          <h2>产品线列表</h2>
          <p>{{ filteredCountText }}，按更新时间倒序展示</p>
        </div>
        <div class="toolbar">
          <button class="btn" type="button" :disabled="loading" @click="load">
            <RefreshCcw :size="16" />
            刷新
          </button>
        </div>
      </div>
      <div class="panel-body">
        <div class="product-line-toolbar">
          <div class="field">
            <label>搜索</label>
            <input v-model="keyword" class="input" maxlength="80" placeholder="搜索名称、编码或负责人" @keyup.enter="load" />
          </div>
          <button class="btn" type="button" :disabled="loading" @click="load">
            <Search :size="16" />
            查询
          </button>
        </div>

        <div v-if="loading" class="empty">正在加载产品线...</div>
        <div v-else-if="productLines.length === 0" class="empty">暂无产品线，请新增。</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>产品线</th>
                <th>类型</th>
                <th>涉及平台</th>
                <th>负责人</th>
                <th>功能节点数</th>
                <th>描述</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="line in productLines" :key="line.id">
                <td>
                  <strong>{{ line.lineName }}</strong>
                  <small v-if="line.lineCode">{{ line.lineCode }}</small>
                </td>
                <td><span class="tag blue">{{ typeLabels[line.productType] || line.productType }}</span></td>
                <td>
                  <div class="tag-list">
                    <span v-for="platform in line.platforms" :key="platform" class="tag">{{ platformLabel(platform) }}</span>
                  </div>
                </td>
                <td>{{ line.owners.join('、') }}</td>
                <td>{{ line.featureCount }}</td>
                <td class="description-cell">{{ line.description || '暂无描述' }}</td>
                <td>{{ line.updatedAt || '-' }}</td>
                <td>
                  <div class="toolbar">
                    <button class="btn small" type="button" :disabled="!line.canEdit" @click="openEdit(line)">
                      <Edit3 :size="14" />
                      编辑
                    </button>
                    <button class="btn small danger" type="button" :disabled="!line.canDelete" @click="openDelete(line)">
                      <Trash2 :size="14" />
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </section>

  <BaseModal :open="modalOpen" :title="modalMode === 'create' ? '新增产品线' : '编辑产品线'" @close="modalOpen = false">
    <div class="grid-form">
      <div class="field">
        <label>产品线编码</label>
        <input v-model="form.lineCode" class="input" maxlength="64" placeholder="可选，例如 cloud_lease" />
      </div>
      <div class="field">
        <label>产品线名称</label>
        <input v-model="form.lineName" class="input" maxlength="100" placeholder="请输入产品线名称" />
      </div>
      <div class="field">
        <label>产品线类型</label>
        <select v-model="form.productType" class="select">
          <option value="face_to_customer">面客产品</option>
          <option value="internal">内部产品</option>
          <option value="public_service">公共服务</option>
          <option value="design_spec">设计规范</option>
        </select>
      </div>
      <div class="field">
        <label>负责人</label>
        <input v-model="form.ownersText" class="input" placeholder="多个负责人用逗号分隔" />
      </div>
      <div class="field full">
        <label>涉及平台</label>
        <div class="platform-grid">
          <label v-for="option in platformOptions" :key="option.value" class="checkbox-option">
            <input
              type="checkbox"
              :checked="form.platforms.includes(option.value)"
              @change="togglePlatform(option.value, ($event.target as HTMLInputElement).checked)"
            />
            {{ option.label }}
          </label>
        </div>
      </div>
      <div class="field full">
        <label>业务介绍</label>
        <textarea v-model="form.description" class="textarea" maxlength="2000" placeholder="说明产品线定位、使用范围或维护备注" />
      </div>
    </div>
    <div class="field-error">{{ formError }}</div>
    <template #footer>
      <button class="btn" type="button" @click="modalOpen = false">取消</button>
      <button class="btn primary" type="button" @click="submit">保存</button>
    </template>
  </BaseModal>

  <BaseModal :open="confirmModal.open" title="删除产品线" @close="confirmModal.open = false">
    <p class="confirm-message">
      确认删除“{{ confirmModal.productLine?.lineName }}”吗？删除后列表和全量功能库产品线下拉将不再展示。
    </p>
    <template #footer>
      <button class="btn" type="button" @click="confirmModal.open = false">取消</button>
      <button class="btn danger" type="button" @click="confirmDelete">确认删除</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.product-line-page { display: grid; gap: 14px; }
.product-line-toolbar { display: grid; grid-template-columns: minmax(240px, 420px) 96px; gap: 12px; align-items: end; margin-bottom: 14px; }
.table-wrap { overflow: auto; border: 1px solid var(--line); border-radius: 8px; }
td strong, td small { display: block; }
td small { color: var(--muted); margin-top: 4px; }
.tag-list { display: flex; gap: 6px; flex-wrap: wrap; min-width: 180px; }
.description-cell { min-width: 220px; max-width: 360px; white-space: pre-wrap; color: #475467; }
.field.full { grid-column: 1 / -1; }
.platform-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.checkbox-option { border: 1px solid var(--line); border-radius: 8px; padding: 9px 10px; display: flex; gap: 8px; align-items: center; font-size: 13px; background: #fff; }
.confirm-message { margin: 0; line-height: 1.7; }
@media (max-width: 920px) {
  .platform-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 760px) {
  .product-line-toolbar, .platform-grid { grid-template-columns: 1fr; }
}
</style>
