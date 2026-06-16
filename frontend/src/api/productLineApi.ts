import { http, unwrap } from './http';
import type { ProductLine, ProductLinePlatform, ProductLineType } from '../types';

export interface SaveProductLinePayload {
  lineCode?: string;
  lineName: string;
  owners: string[];
  productType: ProductLineType;
  platforms: ProductLinePlatform[];
  description?: string;
}

export const productLineApi = {
  list: (keyword?: string) => unwrap<ProductLine[]>(http.get('/product-lines', { params: { keyword } })),
  detail: (id: number) => unwrap<ProductLine>(http.get(`/product-lines/${id}`)),
  create: (payload: SaveProductLinePayload) => unwrap<ProductLine>(http.post('/product-lines', payload)),
  update: (id: number, payload: SaveProductLinePayload) => unwrap<ProductLine>(http.put(`/product-lines/${id}`, payload)),
  remove: (id: number) => unwrap<void>(http.delete(`/product-lines/${id}`)),
};
