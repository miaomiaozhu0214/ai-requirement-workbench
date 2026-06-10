import { http, unwrap } from './http';
import type { Requirement } from '../types';

export const requirementApi = {
  list: () => unwrap<Requirement[]>(http.get('/requirements')),
  detail: (id: number) => unwrap<Requirement>(http.get(`/requirements/${id}`)),
};
