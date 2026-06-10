import { defineStore } from 'pinia';
import { conversationApi } from '../api/conversationApi';
import { requirementApi } from '../api/requirementApi';
import type { AiAction, Candidate, ConversationMessage, ConversationSummary, Requirement } from '../types';

export const useWorkbenchStore = defineStore('workbench', {
  state: () => ({
    sessions: [] as ConversationSummary[],
    activeSession: null as ConversationSummary | null,
    messages: [] as ConversationMessage[],
    candidates: [] as Candidate[],
    requirements: [] as Requirement[],
    aiActions: [] as AiAction[],
    loading: false,
    sending: false,
  }),
  actions: {
    async loadSessions() {
      this.sessions = await conversationApi.list();
      if (!this.activeSession && this.sessions.length > 0) {
        await this.selectSession(this.sessions[0].id);
      } else if (this.sessions.length === 0) {
        this.activeSession = null;
        this.messages = [];
        this.candidates = [];
        this.aiActions = [];
      }
    },
    async createSession() {
      const detail = await conversationApi.create();
      this.applyDetail(detail);
      await this.loadSessions();
      this.activeSession = detail.session;
    },
    async selectSession(id: number) {
      const detail = await conversationApi.detail(id);
      this.applyDetail(detail);
    },
    async sendMessage(content: string) {
      if (!this.activeSession) {
        await this.createSession();
      }
      if (!this.activeSession) return;
      this.sending = true;
      try {
        const detail = await conversationApi.sendMessage(this.activeSession.id, content);
        this.applyDetail(detail);
        await this.loadSessions();
        this.activeSession = detail.session;
      } finally {
        this.sending = false;
      }
    },
    async refreshRequirements() {
      this.requirements = await requirementApi.list();
    },
    async deleteSession(id: number) {
      await conversationApi.delete(id);
      const wasActive = this.activeSession?.id === id;
      this.sessions = this.sessions.filter((session) => session.id !== id);
      if (wasActive) {
        this.activeSession = null;
        this.messages = [];
        this.candidates = [];
        this.aiActions = [];
        if (this.sessions.length > 0) {
          await this.selectSession(this.sessions[0].id);
        }
      }
      await this.loadSessions();
    },
    applyDetail(detail: { session: ConversationSummary; messages: ConversationMessage[]; candidates: Candidate[]; requirements?: Requirement[]; aiActions?: AiAction[] }) {
      this.activeSession = detail.session;
      this.messages = detail.messages;
      this.candidates = detail.candidates;
      if (detail.requirements) this.requirements = detail.requirements;
      this.aiActions = detail.aiActions ?? [];
    },
  },
});
