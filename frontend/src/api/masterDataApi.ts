import { http, unwrap } from './http';
import type { ProductLine, ProductModule } from '../types';

export const masterDataApi = {
  productLines: () => unwrap<ProductLine[]>(http.get('/master-data/product-lines')),
  modules: (lineId: number) => unwrap<ProductModule[]>(http.get(`/master-data/product-lines/${lineId}/modules`)),
};
