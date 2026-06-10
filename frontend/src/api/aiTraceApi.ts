import { http, unwrap } from './http';
import type { AiTrace } from '../types';

type TraceListResponse = AiTrace[] | {
  records?: AiTrace[];
  content?: AiTrace[];
  list?: AiTrace[];
};

function normalizeTraceList(data: TraceListResponse): AiTrace[] {
  // 后端当前返回数组；这里兼容分页对象是为了防止接口形态调整后 Trace 页面再次空白。
  if (Array.isArray(data)) {
    return data;
  }
  return data.records ?? data.content ?? data.list ?? [];
}

export const aiTraceApi = {
  list: async (sessionId?: number | string) => {
    // 带 sessionId 时只查看某个会话的调用链；不带时查看全局最近 Trace。
    const data = await unwrap<TraceListResponse>(http.get('/ai/traces', { params: sessionId ? { sessionId } : undefined }));
    return normalizeTraceList(data);
  },
  get: (id: number) => unwrap<AiTrace>(http.get(`/ai/traces/${id}`)),
};
