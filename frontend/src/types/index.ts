export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface ConversationSummary {
  id: number;
  title: string;
  status: string;
  currentStage: string;
  candidateCount: number;
  requirementCount: number;
  lastMessageAt?: string;
  updatedAt?: string;
}

export interface ConversationMessage {
  id: number;
  sessionId: number;
  role: 'user' | 'assistant' | 'system';
  messageType: string;
  content: string;
  createdAt: string;
}

export interface Candidate {
  id: number;
  sessionId: number;
  title: string;
  status: string;
  contentJson: Record<string, unknown>;
  completenessScore: number;
  missingItemsJson?: string[];
  riskyItemsJson?: string[];
  suggestedQuestionsJson?: string[];
  confidence?: number;
  convertedRequirementId?: number;
  updatedAt?: string;
}

export interface ConversationDetail {
  session: ConversationSummary;
  messages: ConversationMessage[];
  candidates: Candidate[];
  requirements: Requirement[];
  aiActions: AiAction[];
}

export interface ProductLine {
  id: number;
  lineCode: string;
  lineName: string;
}

export interface ProductModule {
  id: number;
  productLineId: number;
  moduleCode: string;
  moduleName: string;
}

export interface GenerateCardRequest {
  title: string;
  productLineId: number | null;
  moduleId: number | null;
  requirementType: string;
  priority: string;
}

export interface Requirement {
  id: number;
  requirementNo: string;
  sourceSessionId: number;
  sourceCandidateId: number;
  title: string;
  productLineId: number;
  moduleId: number;
  requirementType: string;
  priority: string;
  status: string;
  contentJson: Record<string, unknown>;
  completenessScore: number;
  currentVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface AiAction {
  traceId: number;
  traceNo: string;
  abilityType: string;
  status: string;
  intent?: string;
  nextActions?: string[];
  businessObjectType?: string;
  businessObjectId?: number;
  modelName?: string;
  promptTemplateId?: number;
  promptTemplateCode?: string;
  promptTemplateName?: string;
  promptVersion?: string;
  inputSummary?: string;
  outputJson?: Record<string, unknown>;
  durationMs?: number;
  tokenInput?: number;
  tokenOutput?: number;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
}

export interface AiTrace {
  id: number;
  traceId?: number;
  traceNo: string;
  sessionId?: number;
  businessObjectType?: string;
  businessObjectId?: number;
  abilityType: string;
  modelConfigId?: number;
  modelName: string;
  promptTemplateId?: number;
  promptTemplateCode?: string;
  promptTemplateName?: string;
  promptVersion?: string;
  inputSummary?: string;
  inputJson?: Record<string, unknown>;
  outputJson?: Record<string, unknown>;
  outputText?: string;
  intent?: string;
  nextActions?: string[];
  tokenInput?: number;
  tokenOutput?: number;
  durationMs?: number;
  status: string;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
}

export interface AiModelConfig {
  id?: number;
  provider: string;
  modelName: string;
  displayName: string;
  apiBaseUrl: string;
  apiKeyEnv: string;
  apiKeySecret?: string;
  temperature: number;
  maxOutputTokens: number;
  timeoutSeconds: number;
  status: string;
  isDefault: boolean;
  updatedAt?: string;
}

export interface AiConfigStatus {
  provider: string;
  modeLabel: string;
  llmConfigured: boolean;
  defaultModelConfigured: boolean;
  apiKeyConfigured: boolean;
  enabledModelCount: number;
  modelName?: string;
  missingItems: string[];
}

export interface PromptTemplate {
  id?: number;
  abilityType: string;
  templateCode: string;
  templateName: string;
  version: string;
  systemPrompt: string;
  userPrompt: string;
  jsonSchema: Record<string, unknown>;
  status: string;
  isDefault: boolean;
  updatedAt?: string;
}

export interface AiAbilityConfig {
  id?: number;
  abilityType: string;
  abilityName: string;
  enabled: boolean;
  modelConfigId: number | null;
  promptTemplateId: number | null;
  fallbackToMock: boolean;
  executionOrder: number;
  status: string;
  updatedAt?: string;
}

export type FeatureNodeType = 'module' | 'feature';
export type FeatureNodeStatus = 'added' | 'modified' | 'deleted' | 'unchanged';
export type FeatureContentBlockType = 'overview' | 'rule' | 'field' | 'api' | 'screenshot';

export interface FeatureContentBlock {
  id: number;
  featureId: number;
  blockType: FeatureContentBlockType;
  title?: string;
  content: string;
  metadata?: Record<string, unknown>;
  sourceRef?: string;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface FeatureNode {
  id: number;
  productLineId: number;
  parentId: number | null;
  name: string;
  description?: string;
  nodeType: FeatureNodeType;
  status: FeatureNodeStatus;
  sortOrder: number;
  children: FeatureNode[];
  contentBlocks: FeatureContentBlock[];
}

export interface FeatureHistory {
  id: number;
  featureId: number;
  operationType: 'added' | 'modified' | 'deleted' | 'moved';
  description: string;
  operatorId?: number;
  createdAt: string;
}
