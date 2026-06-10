import { http, unwrap } from './http';
import type { ConversationDetail, ConversationMessage, ConversationSummary } from '../types';

export const conversationApi = {
  list: () => unwrap<ConversationSummary[]>(http.get('/conversations')),
  create: (title?: string) => unwrap<ConversationDetail>(http.post('/conversations', { title })),
  detail: (id: number) => unwrap<ConversationDetail>(http.get(`/conversations/${id}`)),
  messages: (id: number) => unwrap<ConversationMessage[]>(http.get(`/conversations/${id}/messages`)),
  sendMessage: (id: number, content: string) => unwrap<ConversationDetail>(http.post(`/conversations/${id}/messages`, { content })),
  delete: (id: number) => unwrap<void>(http.delete(`/conversations/${id}`)),
};
