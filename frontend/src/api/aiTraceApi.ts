import { http, unwrap } from './http';
import type { AiTrace } from '../types';

type TraceListResponse = AiTrace[] | {
  records?: AiTrace[];
  content?: AiTrace[];
  list?: AiTrace[];
};

function normalizeTraceList(data: TraceListResponse): AiTrace[] {
  if (Array.isArray(data)) {
    return data;
  }
  return data.records ?? data.content ?? data.list ?? [];
}

export const aiTraceApi = {
  list: async (sessionId?: number | string) => {
    const data = await unwrap<TraceListResponse>(http.get('/ai/traces', { params: sessionId ? { sessionId } : undefined }));
    return normalizeTraceList(data);
  },
  get: (id: number) => unwrap<AiTrace>(http.get(`/ai/traces/${id}`)),
};
