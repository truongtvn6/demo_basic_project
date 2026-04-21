const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const API_TOKEN = import.meta.env.VITE_API_TOKEN ?? 'dev-token';

export interface DeviceToken {
  id: number;
  token: string;
  label: string | null;
  createdAt: string;
}

export interface ProjectEventLog {
  id: number;
  source: string;
  routingKey: string;
  externalId: string;
  title: string | null;
  deliveredAt: string;
  success: boolean;
  errorMessage: string | null;
}

export interface SyncState {
  jiraLastSyncAt: string | null;
  githubLastSha: string | null;
}

export interface SyncResult {
  jira?: number;
  github?: number;
  status?: string;
  externalId?: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Authorization', `Bearer ${API_TOKEN}`);
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  headers.set('Accept', 'application/json');

  const res = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`${res.status} ${res.statusText}${text ? `: ${text}` : ''}`);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  const contentType = res.headers.get('Content-Type') ?? '';
  if (contentType.includes('application/json')) {
    return (await res.json()) as T;
  }
  return (await res.text()) as unknown as T;
}

export const api = {
  registry: {
    list(): Promise<DeviceToken[]> {
      return request('/api/registry/devices');
    },
    register(token: string, label?: string): Promise<DeviceToken> {
      return request('/api/registry/devices/register', {
        method: 'POST',
        body: JSON.stringify({ token, label: label ?? null }),
      });
    },
    remove(id: number): Promise<void> {
      return request(`/api/registry/devices/${id}`, { method: 'DELETE' });
    },
  },
  integration: {
    state(): Promise<SyncState> {
      return request('/api/integration/sync/state');
    },
    syncAll(): Promise<SyncResult> {
      return request('/api/integration/sync', { method: 'POST' });
    },
    syncJira(): Promise<SyncResult> {
      return request('/api/integration/sync/jira', { method: 'POST' });
    },
    syncGithub(): Promise<SyncResult> {
      return request('/api/integration/sync/github', { method: 'POST' });
    },
    syncMock(): Promise<SyncResult> {
      return request('/api/integration/sync/mock', { method: 'POST' });
    },
  },
  notification: {
    recent(limit = 50): Promise<ProjectEventLog[]> {
      return request(`/api/notification/events/recent?limit=${limit}`);
    },
  },
};
