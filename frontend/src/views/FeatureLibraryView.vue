<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ArrowDown, ArrowUp, Edit3, FolderPlus, Plus, RefreshCcw, Search, Trash2 } from '@lucide/vue';
import BaseModal from '../components/common/BaseModal.vue';
import { featureLibraryApi } from '../api/featureLibraryApi';
import { productLineApi } from '../api/productLineApi';
import { useUiStore } from '../stores/ui';
import type {
  FeatureContentBlock,
  FeatureContentBlockType,
  FeatureHistory,
  FeatureNode,
  FeatureNodeType,
  ProductLine,
} from '../types';

type TreeRow = {
  node: FeatureNode;
  depth: number;
};

const ui = useUiStore();
const productLines = ref<ProductLine[]>([]);
const selectedProductLineId = ref<number | null>(null);
const keyword = ref('');
const loading = ref(false);
const tree = ref<FeatureNode[]>([]);
const selectedNodeId = ref<number | null>(null);
const histories = ref<FeatureHistory[]>([]);
const historyLoading = ref(false);

const nodeModalOpen = ref(false);
const nodeModalMode = ref<'create' | 'edit'>('create');
const nodeFormError = ref('');
const nodeForm = reactive({
  id: null as number | null,
  parentId: '',
  name: '',
  description: '',
  nodeType: 'feature' as FeatureNodeType,
});

const blockModalOpen = ref(false);
const blockModalMode = ref<'create' | 'edit'>('create');
const blockFormError = ref('');
const blockForm = reactive({
  id: null as number | null,
  blockType: 'overview' as FeatureContentBlockType,
  title: '',
  content: '',
  metadataText: '',
  sourceRef: '',
});

const confirmModal = reactive({
  open: false,
  title: '',
  message: '',
  action: null as null | (() => Promise<void>),
});

const nodeTypeLabels: Record<FeatureNodeType, string> = {
  module: '模块',
  feature: '功能',
};

const statusLabels: Record<string, string> = {
  added: '新增',
  modified: '已修改',
  deleted: '已删除',
  unchanged: '未变化',
};

const blockTypeLabels: Record<FeatureContentBlockType, string> = {
  overview: '概述',
  rule: '规则说明',
  field: '字段清单',
  api: '接口',
  screenshot: '截图说明',
};

const operationLabels: Record<string, string> = {
  added: '新增',
  modified: '修改',
  deleted: '删除',
  moved: '移动',
};

const flatRows = computed<TreeRow[]>(() => {
  const rows: TreeRow[] = [];
  function walk(nodes: FeatureNode[], depth: number) {
    nodes.forEach((node) => {
      rows.push({ node, depth });
      walk(node.children || [], depth + 1);
    });
  }
  walk(tree.value, 0);
  return rows;
});

const selectedNode = computed(() => {
  if (!selectedNodeId.value) {
    return null;
  }
  return findNode(tree.value, selectedNodeId.value);
});

const parentOptions = computed(() => {
  const excluded = nodeForm.id ? collectDescendantIds(findNode(tree.value, nodeForm.id)) : new Set<number>();
  if (nodeForm.id) {
    excluded.add(nodeForm.id);
  }
  return flatRows.value.filter((row) => !excluded.has(row.node.id));
});

async function loadProductLines() {
  try {
    productLines.value = await productLineApi.list();
    if (!selectedProductLineId.value && productLines.value.length > 0) {
      selectedProductLineId.value = productLines.value[0].id;
    }
    await loadTree();
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '产品线加载失败', 'error');
  }
}

async function loadTree() {
  if (!selectedProductLineId.value) {
    tree.value = [];
    selectedNodeId.value = null;
    histories.value = [];
    return;
  }
  loading.value = true;
  try {
    const previousSelectedId = selectedNodeId.value;
    tree.value = await featureLibraryApi.getTree(selectedProductLineId.value, keyword.value.trim() || undefined);
    const previousStillExists = previousSelectedId ? Boolean(findNode(tree.value, previousSelectedId)) : false;
    selectedNodeId.value = previousStillExists ? previousSelectedId : flatRows.value[0]?.node.id ?? null;
    if (selectedNodeId.value) {
      await loadHistory(selectedNodeId.value);
    } else {
      histories.value = [];
    }
  } catch (error) {
    ui.toast('加载失败', error instanceof Error ? error.message : '功能树加载失败', 'error');
  } finally {
    loading.value = false;
  }
}

async function selectNode(id: number) {
  selectedNodeId.value = id;
  await loadHistory(id);
}

async function loadHistory(id: number) {
  historyLoading.value = true;
  try {
    histories.value = await featureLibraryApi.getHistory(id);
  } catch (error) {
    histories.value = [];
    ui.toast('历史加载失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  } finally {
    historyLoading.value = false;
  }
}

function openCreateNode(parent?: FeatureNode) {
  nodeModalMode.value = 'create';
  nodeFormError.value = '';
  nodeForm.id = null;
  nodeForm.parentId = parent?.id ? String(parent.id) : '';
  nodeForm.name = '';
  nodeForm.description = '';
  nodeForm.nodeType = parent ? 'feature' : 'module';
  nodeModalOpen.value = true;
}

function openEditNode(node: FeatureNode) {
  nodeModalMode.value = 'edit';
  nodeFormError.value = '';
  nodeForm.id = node.id;
  nodeForm.parentId = node.parentId ? String(node.parentId) : '';
  nodeForm.name = node.name;
  nodeForm.description = node.description || '';
  nodeForm.nodeType = node.nodeType;
  nodeModalOpen.value = true;
}

async function submitNode() {
  if (!selectedProductLineId.value) {
    nodeFormError.value = '请先选择产品线';
    return;
  }
  if (!nodeForm.name.trim()) {
    nodeFormError.value = '请输入节点名称';
    return;
  }
  const parentId = nodeForm.parentId ? Number(nodeForm.parentId) : null;
  try {
    if (nodeModalMode.value === 'create') {
      await featureLibraryApi.createNode({
        productLineId: selectedProductLineId.value,
        parentId,
        name: nodeForm.name.trim(),
        description: nodeForm.description.trim(),
        nodeType: nodeForm.nodeType,
      });
      ui.toast('新增成功', '功能节点已创建', 'success');
    } else if (nodeForm.id) {
      await featureLibraryApi.updateNode(nodeForm.id, {
        parentId,
        name: nodeForm.name.trim(),
        description: nodeForm.description.trim(),
        nodeType: nodeForm.nodeType,
      });
      ui.toast('保存成功', '功能节点已更新', 'success');
      selectedNodeId.value = nodeForm.id;
    }
    nodeModalOpen.value = false;
    await loadTree();
  } catch (error) {
    nodeFormError.value = error instanceof Error ? error.message : '保存失败';
  }
}

function confirmDeleteNode(node: FeatureNode) {
  confirmModal.open = true;
  confirmModal.title = '删除功能节点';
  confirmModal.message = `确认删除“${node.name}”及其所有子节点吗？删除后功能树中将不再展示。`;
  confirmModal.action = async () => {
    await featureLibraryApi.deleteNode(node.id);
    ui.toast('删除成功', '节点及子节点已软删除', 'success');
    selectedNodeId.value = null;
    await loadTree();
  };
}

async function moveNode(node: FeatureNode, delta: -1 | 1) {
  const siblings = findSiblings(tree.value, node);
  const index = siblings.findIndex((item) => item.id === node.id);
  const nextIndex = index + delta;
  if (index < 0 || nextIndex < 0 || nextIndex >= siblings.length) {
    return;
  }
  try {
    await featureLibraryApi.moveNode(node.id, { parentId: node.parentId, index: nextIndex });
    ui.toast('移动成功', '同级排序已更新', 'success');
    selectedNodeId.value = node.id;
    await loadTree();
  } catch (error) {
    ui.toast('移动失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  }
}

function openCreateBlock() {
  if (!selectedNode.value) {
    return;
  }
  blockModalMode.value = 'create';
  blockFormError.value = '';
  blockForm.id = null;
  blockForm.blockType = 'overview';
  blockForm.title = '';
  blockForm.content = '';
  blockForm.metadataText = '';
  blockForm.sourceRef = '';
  blockModalOpen.value = true;
}

function openEditBlock(block: FeatureContentBlock) {
  blockModalMode.value = 'edit';
  blockFormError.value = '';
  blockForm.id = block.id;
  blockForm.blockType = block.blockType;
  blockForm.title = block.title || '';
  blockForm.content = block.content;
  blockForm.metadataText = block.metadata ? JSON.stringify(block.metadata, null, 2) : '';
  blockForm.sourceRef = block.sourceRef || '';
  blockModalOpen.value = true;
}

async function submitBlock() {
  if (!selectedNode.value) {
    blockFormError.value = '请先选择功能节点';
    return;
  }
  if (!blockForm.content.trim()) {
    blockFormError.value = '请输入内容';
    return;
  }
  let metadata: Record<string, unknown> | undefined;
  if (blockForm.metadataText.trim()) {
    try {
      metadata = JSON.parse(blockForm.metadataText);
    } catch {
      blockFormError.value = 'metadata 必须是合法 JSON';
      return;
    }
  }
  const payload = {
    blockType: blockForm.blockType,
    title: blockForm.title.trim(),
    content: blockForm.content.trim(),
    metadata,
    sourceRef: blockForm.sourceRef.trim(),
  };
  try {
    if (blockModalMode.value === 'create') {
      await featureLibraryApi.createContentBlock(selectedNode.value.id, payload);
      ui.toast('新增成功', '结构化内容块已创建', 'success');
    } else if (blockForm.id) {
      await featureLibraryApi.updateContentBlock(blockForm.id, payload);
      ui.toast('保存成功', '结构化内容块已更新', 'success');
    }
    blockModalOpen.value = false;
    await loadTree();
  } catch (error) {
    blockFormError.value = error instanceof Error ? error.message : '保存失败';
  }
}

function confirmDeleteBlock(block: FeatureContentBlock) {
  confirmModal.open = true;
  confirmModal.title = '删除内容块';
  confirmModal.message = `确认删除“${block.title || blockTypeLabels[block.blockType]}”吗？`;
  confirmModal.action = async () => {
    await featureLibraryApi.deleteContentBlock(block.id);
    ui.toast('删除成功', '内容块已软删除', 'success');
    await loadTree();
  };
}

async function runConfirmAction() {
  if (!confirmModal.action) {
    confirmModal.open = false;
    return;
  }
  try {
    await confirmModal.action();
    confirmModal.open = false;
  } catch (error) {
    ui.toast('操作失败', error instanceof Error ? error.message : '请稍后重试', 'error');
  }
}

function findNode(nodes: FeatureNode[], id: number): FeatureNode | null {
  for (const node of nodes) {
    if (node.id === id) {
      return node;
    }
    const child = findNode(node.children || [], id);
    if (child) {
      return child;
    }
  }
  return null;
}

function findSiblings(nodes: FeatureNode[], target: FeatureNode): FeatureNode[] {
  if (target.parentId === null || target.parentId === undefined) {
    return nodes;
  }
  const parent = findNode(nodes, target.parentId);
  return parent?.children || [];
}

function collectDescendantIds(node: FeatureNode | null): Set<number> {
  const result = new Set<number>();
  function walk(current: FeatureNode | null) {
    current?.children?.forEach((child) => {
      result.add(child.id);
      walk(child);
    });
  }
  walk(node);
  return result;
}

function isFirstSibling(node: FeatureNode) {
  return findSiblings(tree.value, node)[0]?.id === node.id;
}

function isLastSibling(node: FeatureNode) {
  const siblings = findSiblings(tree.value, node);
  return siblings[siblings.length - 1]?.id === node.id;
}

onMounted(loadProductLines);
</script>

<template>
  <header class="page-head">
    <div class="page-title">
      <h1>功能库</h1>
      <p>按产品线维护全量功能树、结构化说明和节点变更历史</p>
    </div>
    <button class="btn primary" type="button" :disabled="!selectedProductLineId" @click="openCreateNode()">
      <FolderPlus :size="16" />
      新增根节点
    </button>
  </header>

  <section class="page-body feature-page">
    <section class="panel feature-toolbar-panel">
      <div class="panel-body feature-toolbar">
        <div class="field">
          <label>产品线</label>
          <select v-model.number="selectedProductLineId" class="select" @change="loadTree">
            <option v-if="productLines.length === 0" :value="null">暂无产品线</option>
            <option v-for="line in productLines" :key="line.id" :value="line.id">{{ line.lineName }}</option>
          </select>
        </div>
        <div class="field keyword-field">
          <label>关键字</label>
          <input v-model="keyword" class="input" maxlength="80" placeholder="搜索节点名称或描述" @keyup.enter="loadTree" />
        </div>
        <button class="btn" type="button" :disabled="loading || !selectedProductLineId" @click="loadTree">
          <Search :size="16" />
          查询
        </button>
        <button class="btn" type="button" :disabled="loading || !selectedProductLineId" @click="loadTree">
          <RefreshCcw :size="16" />
          刷新
        </button>
      </div>
    </section>

    <div class="feature-layout">
      <section class="panel feature-tree-panel">
        <div class="panel-head">
          <div>
            <h2>功能树</h2>
            <p>同级按排序值升序展示，删除节点会级联隐藏子节点</p>
          </div>
          <button class="btn small" type="button" :disabled="!selectedProductLineId" @click="openCreateNode()">新增根节点</button>
        </div>
        <div class="panel-body tree-body">
          <div v-if="productLines.length === 0" class="empty">暂无产品线，请先在“产品线配置”中新增产品线。</div>
          <div v-else-if="loading" class="empty">正在加载功能树...</div>
          <div v-else-if="flatRows.length === 0" class="empty">暂无功能节点。可以先新增一个根模块。</div>
          <div v-else class="tree-list">
            <article
              v-for="row in flatRows"
              :key="row.node.id"
              class="tree-row"
              :class="{ active: row.node.id === selectedNodeId }"
              :style="{ paddingLeft: `${12 + row.depth * 18}px` }"
              @click="selectNode(row.node.id)"
            >
              <div class="tree-main">
                <span class="tree-dot" :class="row.node.nodeType" />
                <div>
                  <strong>{{ row.node.name }}</strong>
                  <span>{{ nodeTypeLabels[row.node.nodeType] }} · {{ statusLabels[row.node.status] || row.node.status }}</span>
                </div>
              </div>
              <div class="tree-actions">
                <button class="btn small" type="button" title="新增子节点" @click.stop="openCreateNode(row.node)">
                  <Plus :size="14" />
                </button>
                <button class="btn small" type="button" title="编辑" @click.stop="openEditNode(row.node)">
                  <Edit3 :size="14" />
                </button>
                <button class="btn small" type="button" title="上移" :disabled="isFirstSibling(row.node)" @click.stop="moveNode(row.node, -1)">
                  <ArrowUp :size="14" />
                </button>
                <button class="btn small" type="button" title="下移" :disabled="isLastSibling(row.node)" @click.stop="moveNode(row.node, 1)">
                  <ArrowDown :size="14" />
                </button>
                <button class="btn small danger" type="button" title="删除" @click.stop="confirmDeleteNode(row.node)">
                  <Trash2 :size="14" />
                </button>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="detail-column">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>节点详情</h2>
              <p>功能节点的基础信息和结构化内容</p>
            </div>
            <div v-if="selectedNode" class="toolbar">
              <button class="btn small" type="button" @click="openEditNode(selectedNode)">编辑</button>
              <button class="btn small primary" type="button" @click="openCreateBlock">新增内容块</button>
            </div>
          </div>
          <div class="panel-body">
            <div v-if="!selectedNode" class="empty">请选择左侧功能节点查看详情。</div>
            <div v-else class="node-detail">
              <div class="detail-grid">
                <div>
                  <label>名称</label>
                  <strong>{{ selectedNode.name }}</strong>
                </div>
                <div>
                  <label>类型</label>
                  <span class="tag blue">{{ nodeTypeLabels[selectedNode.nodeType] }}</span>
                </div>
                <div>
                  <label>状态</label>
                  <span class="tag" :class="{ green: selectedNode.status === 'unchanged', orange: selectedNode.status === 'modified', red: selectedNode.status === 'deleted' }">
                    {{ statusLabels[selectedNode.status] || selectedNode.status }}
                  </span>
                </div>
                <div>
                  <label>排序</label>
                  <strong>{{ selectedNode.sortOrder }}</strong>
                </div>
              </div>
              <div class="description-box">
                <label>描述</label>
                <p>{{ selectedNode.description || '暂无描述' }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>结构化内容块</h2>
              <p>用于沉淀概述、规则、字段、接口和截图说明</p>
            </div>
          </div>
          <div class="panel-body">
            <div v-if="!selectedNode" class="empty">请选择功能节点。</div>
            <div v-else-if="selectedNode.contentBlocks.length === 0" class="empty">暂无内容块。可以新增概述或规则说明。</div>
            <div v-else class="content-block-list">
              <article v-for="block in selectedNode.contentBlocks" :key="block.id" class="content-block">
                <header>
                  <div>
                    <span class="tag blue">{{ blockTypeLabels[block.blockType] }}</span>
                    <strong>{{ block.title || blockTypeLabels[block.blockType] }}</strong>
                  </div>
                  <div class="toolbar">
                    <button class="btn small" type="button" @click="openEditBlock(block)">编辑</button>
                    <button class="btn small danger" type="button" @click="confirmDeleteBlock(block)">删除</button>
                  </div>
                </header>
                <p>{{ block.content }}</p>
                <small v-if="block.sourceRef">来源：{{ block.sourceRef }}</small>
              </article>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>变更历史</h2>
              <p>新增、编辑、移动、删除节点时自动记录</p>
            </div>
          </div>
          <div class="panel-body history-body">
            <div v-if="historyLoading" class="empty">正在加载历史...</div>
            <div v-else-if="histories.length === 0" class="empty">暂无变更历史。</div>
            <div v-else class="history-list">
              <article v-for="history in histories" :key="history.id" class="history-item">
                <span class="tag">{{ operationLabels[history.operationType] || history.operationType }}</span>
                <div>
                  <strong>{{ history.description }}</strong>
                  <small>{{ history.createdAt }}</small>
                </div>
              </article>
            </div>
          </div>
        </section>
      </section>
    </div>
  </section>

  <BaseModal :open="nodeModalOpen" :title="nodeModalMode === 'create' ? '新增功能节点' : '编辑功能节点'" @close="nodeModalOpen = false">
    <div class="grid-form">
      <div class="field">
        <label>节点名称</label>
        <input v-model="nodeForm.name" class="input" maxlength="200" placeholder="请输入模块或功能名称" />
      </div>
      <div class="field">
        <label>节点类型</label>
        <select v-model="nodeForm.nodeType" class="select">
          <option value="module">模块</option>
          <option value="feature">功能</option>
        </select>
      </div>
      <div class="field">
        <label>父节点</label>
        <select v-model="nodeForm.parentId" class="select">
          <option value="">作为根节点</option>
          <option v-for="row in parentOptions" :key="row.node.id" :value="String(row.node.id)">
            {{ '—'.repeat(row.depth) }} {{ row.node.name }}
          </option>
        </select>
      </div>
      <div class="field full">
        <label>描述</label>
        <textarea v-model="nodeForm.description" class="textarea" maxlength="2000" placeholder="补充功能边界、业务说明或维护备注" />
      </div>
    </div>
    <div class="field-error">{{ nodeFormError }}</div>
    <template #footer>
      <button class="btn" type="button" @click="nodeModalOpen = false">取消</button>
      <button class="btn primary" type="button" @click="submitNode">保存</button>
    </template>
  </BaseModal>

  <BaseModal :open="blockModalOpen" :title="blockModalMode === 'create' ? '新增内容块' : '编辑内容块'" @close="blockModalOpen = false">
    <div class="grid-form">
      <div class="field">
        <label>内容类型</label>
        <select v-model="blockForm.blockType" class="select">
          <option value="overview">概述</option>
          <option value="rule">规则说明</option>
          <option value="field">字段清单</option>
          <option value="api">接口</option>
          <option value="screenshot">截图说明</option>
        </select>
      </div>
      <div class="field">
        <label>标题</label>
        <input v-model="blockForm.title" class="input" maxlength="200" placeholder="可选" />
      </div>
      <div class="field full">
        <label>内容</label>
        <textarea v-model="blockForm.content" class="textarea large" placeholder="请输入结构化说明内容" />
      </div>
      <div class="field">
        <label>来源引用</label>
        <input v-model="blockForm.sourceRef" class="input" maxlength="200" placeholder="文档章节、页面或接口编号" />
      </div>
      <div class="field">
        <label>Metadata JSON</label>
        <textarea v-model="blockForm.metadataText" class="textarea metadata" placeholder='可选，例如 {"fields":["合同编号"]}' />
      </div>
    </div>
    <div class="field-error">{{ blockFormError }}</div>
    <template #footer>
      <button class="btn" type="button" @click="blockModalOpen = false">取消</button>
      <button class="btn primary" type="button" @click="submitBlock">保存</button>
    </template>
  </BaseModal>

  <BaseModal :open="confirmModal.open" :title="confirmModal.title" @close="confirmModal.open = false">
    <p class="confirm-message">{{ confirmModal.message }}</p>
    <template #footer>
      <button class="btn" type="button" @click="confirmModal.open = false">取消</button>
      <button class="btn danger" type="button" @click="runConfirmAction">确认删除</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.feature-page { display: grid; gap: 14px; }
.feature-toolbar-panel .panel-body { padding: 14px 16px; }
.feature-toolbar { display: grid; grid-template-columns: minmax(180px, 240px) minmax(220px, 1fr) 96px 96px; gap: 12px; align-items: end; }
.keyword-field { min-width: 0; }
.feature-layout { display: grid; grid-template-columns: minmax(320px, 420px) minmax(0, 1fr); gap: 14px; align-items: start; }
.feature-tree-panel { min-height: 640px; }
.tree-body { padding: 10px; }
.tree-list { display: grid; gap: 6px; }
.tree-row { border: 1px solid transparent; border-radius: 8px; padding: 10px 10px 10px 12px; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; align-items: center; }
.tree-row:hover { background: #f8fafc; border-color: var(--line); }
.tree-row.active { background: var(--soft-blue); border-color: #bfdbfe; }
.tree-main { min-width: 0; display: flex; gap: 9px; align-items: center; }
.tree-main strong, .tree-main span { display: block; }
.tree-main strong { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-main span { color: var(--muted); font-size: 12px; margin-top: 2px; }
.tree-dot { width: 9px; height: 9px; border-radius: 50%; background: var(--primary); flex: 0 0 auto; }
.tree-dot.feature { background: var(--success); }
.tree-actions { display: flex; gap: 4px; opacity: .78; }
.tree-row:hover .tree-actions, .tree-row.active .tree-actions { opacity: 1; }
.detail-column { display: grid; gap: 14px; }
.node-detail { display: grid; gap: 14px; }
.detail-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.detail-grid label, .description-box label { display: block; color: var(--muted); font-size: 12px; margin-bottom: 5px; }
.detail-grid strong { font-size: 14px; }
.description-box { border: 1px solid var(--line); border-radius: 8px; padding: 12px; background: #fcfcfd; }
.description-box p { margin: 0; line-height: 1.6; white-space: pre-wrap; }
.content-block-list { display: grid; gap: 10px; }
.content-block { border: 1px solid var(--line); border-radius: 8px; padding: 12px; background: #fff; }
.content-block header { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 8px; }
.content-block header > div:first-child { display: flex; gap: 8px; align-items: center; min-width: 0; }
.content-block p { margin: 0; line-height: 1.65; white-space: pre-wrap; }
.content-block small { display: block; color: var(--muted); margin-top: 8px; }
.history-list { display: grid; gap: 10px; }
.history-item { display: grid; grid-template-columns: 62px minmax(0, 1fr); gap: 10px; align-items: start; }
.history-item strong, .history-item small { display: block; }
.history-item small { color: var(--muted); margin-top: 4px; font-size: 12px; }
.field.full { grid-column: 1 / -1; }
.textarea.large { min-height: 150px; }
.textarea.metadata { min-height: 100px; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace; font-size: 12px; }
.confirm-message { margin: 0; line-height: 1.7; }
@media (max-width: 1120px) {
  .feature-layout { grid-template-columns: 1fr; }
  .feature-tree-panel { min-height: auto; }
}
@media (max-width: 760px) {
  .feature-toolbar, .detail-grid { grid-template-columns: 1fr; }
  .tree-row { grid-template-columns: 1fr; }
  .tree-actions { justify-content: flex-start; }
}
</style>
