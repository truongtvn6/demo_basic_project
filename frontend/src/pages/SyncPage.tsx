import { useCallback, useEffect, useState } from 'react';
import { api, type SyncResult, type SyncState } from '../lib/api';

type SyncKey = 'mock' | 'jira' | 'github' | 'all';

export default function SyncPage() {
  const [state, setState] = useState<SyncState | null>(null);
  const [loading, setLoading] = useState<SyncKey | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastResult, setLastResult] = useState<{ kind: SyncKey; result: SyncResult } | null>(null);

  const loadState = useCallback(async () => {
    try {
      setError(null);
      setState(await api.integration.state());
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }, []);

  useEffect(() => {
    loadState();
    const id = window.setInterval(loadState, 5000);
    return () => window.clearInterval(id);
  }, [loadState]);

  const trigger = async (kind: SyncKey) => {
    setLoading(kind);
    setError(null);
    try {
      const result =
        kind === 'mock'
          ? await api.integration.syncMock()
          : kind === 'jira'
            ? await api.integration.syncJira()
            : kind === 'github'
              ? await api.integration.syncGithub()
              : await api.integration.syncAll();
      setLastResult({ kind, result });
      await loadState();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Sync</h1>
        <button
          onClick={loadState}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
        >
          Refresh state
        </button>
      </header>

      {error && (
        <div className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="rounded-lg border border-slate-200 bg-white p-4 space-y-3">
        <h2 className="font-medium">Trigger sync</h2>
        <p className="text-xs text-slate-500">
          Buttons call the integration-service behind the gateway. Mock does not need credentials;
          Jira / GitHub require env variables on the integration-service.
        </p>
        <div className="flex flex-wrap gap-2">
          <SyncButton onClick={() => trigger('mock')} loading={loading === 'mock'} label="Publish mock event" color="slate" />
          <SyncButton onClick={() => trigger('jira')} loading={loading === 'jira'} label="Sync Jira" color="indigo" />
          <SyncButton onClick={() => trigger('github')} loading={loading === 'github'} label="Sync GitHub" color="slate" />
          <SyncButton onClick={() => trigger('all')} loading={loading === 'all'} label="Sync all" color="emerald" />
        </div>
        {lastResult && (
          <pre className="rounded-md border border-slate-200 bg-slate-50 p-3 text-xs text-slate-700 overflow-auto">
{`Last action: ${lastResult.kind}
${JSON.stringify(lastResult.result, null, 2)}`}
          </pre>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4 space-y-3">
        <h2 className="font-medium">Sync state (persisted in Postgres)</h2>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-sm">
          <Stat label="Jira last synced at" value={state?.jiraLastSyncAt} mono />
          <Stat label="GitHub last commit SHA" value={state?.githubLastSha} mono />
        </dl>
      </section>
    </div>
  );
}

function SyncButton({
  onClick,
  loading,
  label,
  color,
}: {
  onClick: () => void;
  loading: boolean;
  label: string;
  color: 'slate' | 'indigo' | 'emerald';
}) {
  const colorClass = {
    slate: 'bg-slate-900 hover:bg-slate-800',
    indigo: 'bg-indigo-600 hover:bg-indigo-700',
    emerald: 'bg-emerald-600 hover:bg-emerald-700',
  }[color];
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={`rounded-md px-4 py-2 text-white text-sm font-medium disabled:opacity-60 ${colorClass}`}
    >
      {loading ? 'Working...' : label}
    </button>
  );
}

function Stat({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="rounded-md bg-slate-50 border border-slate-200 p-3">
      <dt className="text-xs uppercase tracking-wide text-slate-500">{label}</dt>
      <dd className={`mt-1 ${mono ? 'font-mono text-xs break-all' : 'text-sm'}`}>
        {value ?? <span className="text-slate-400">not set</span>}
      </dd>
    </div>
  );
}
