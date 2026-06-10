import { http, unwrap } from './http';
import type { Candidate, GenerateCardRequest, Requirement } from '../types';

export const candidateApi = {
  list: (sessionId: number) => unwrap<Candidate[]>(http.get(`/conversations/${sessionId}/candidates`)),
  close: (id: number) => unwrap<Candidate>(http.post(`/candidates/${id}/close`)),
  generateCard: (id: number, payload: GenerateCardRequest) => unwrap<Requirement>(http.post(`/candidates/${id}/generate-card`, payload)),
};
