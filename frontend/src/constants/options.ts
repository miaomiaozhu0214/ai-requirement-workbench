export const requirementTypeOptions = [
  { value: 'new_feature', label: '新功能' },
  { value: 'optimization', label: '功能优化' },
  { value: 'defect', label: '缺陷反馈' },
  { value: 'permission', label: '权限需求' },
  { value: 'process', label: '流程需求' },
  { value: 'data', label: '数据需求' },
  { value: 'interface', label: '接口需求' },
  { value: 'report', label: '报表需求' },
];

export const priorityOptions = [
  { value: 'low', label: '低' },
  { value: 'medium', label: '中' },
  { value: 'high', label: '高' },
  { value: 'urgent', label: '紧急' },
];

export const candidateStatusLabels: Record<string, string> = {
  draft: '草稿',
  refining: '澄清中',
  ready_to_card: '可生成',
  confirmed: '已确认',
  converted: '已转正式',
  closed: '已关闭',
};

export const requirementStatusLabels: Record<string, string> = {
  draft: '草稿',
  confirmed: '已确认',
  prd_generated: '已生成PRD',
  prototype_generated: '已生成原型',
  closed: '已关闭',
  archived: '已归档',
};
