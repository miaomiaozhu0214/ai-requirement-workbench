import { http, unwrap } from './http';
import type { FeatureContentBlock, FeatureHistory, FeatureNode, FeatureContentBlockType, FeatureNodeType } from '../types';

export interface FeatureNodePayload {
  productLineId: number;
  parentId?: number | null;
  name: string;
  description?: string;
  nodeType: FeatureNodeType;
}

export interface UpdateFeatureNodePayload {
  parentId?: number | null;
  name?: string;
  description?: string;
  nodeType?: FeatureNodeType;
}

export interface MoveFeatureNodePayload {
  parentId?: number | null;
  index: number;
}

export interface FeatureContentBlockPayload {
  blockType: FeatureContentBlockType;
  title?: string;
  content: string;
  metadata?: Record<string, unknown>;
  sourceRef?: string;
  sortOrder?: number;
}

export const featureLibraryApi = {
  getTree: (productLineId: number, keyword?: string) => unwrap<FeatureNode[]>(http.get('/feature-library/tree', { params: { productLineId, keyword } })),
  createNode: (payload: FeatureNodePayload) => unwrap<FeatureNode>(http.post('/feature-library/nodes', payload)),
  updateNode: (id: number, payload: UpdateFeatureNodePayload) => unwrap<FeatureNode>(http.put(`/feature-library/nodes/${id}`, payload)),
  moveNode: (id: number, payload: MoveFeatureNodePayload) => unwrap<FeatureNode>(http.put(`/feature-library/nodes/${id}/move`, payload)),
  deleteNode: (id: number) => unwrap<void>(http.delete(`/feature-library/nodes/${id}`)),
  getHistory: (id: number) => unwrap<FeatureHistory[]>(http.get(`/feature-library/nodes/${id}/history`)),
  createContentBlock: (featureId: number, payload: FeatureContentBlockPayload) => unwrap<FeatureContentBlock>(http.post(`/feature-library/nodes/${featureId}/content-blocks`, payload)),
  updateContentBlock: (blockId: number, payload: FeatureContentBlockPayload) => unwrap<FeatureContentBlock>(http.put(`/feature-library/content-blocks/${blockId}`, payload)),
  deleteContentBlock: (blockId: number) => unwrap<void>(http.delete(`/feature-library/content-blocks/${blockId}`)),
};
