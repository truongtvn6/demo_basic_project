import { useCallback, useEffect, useRef, useState } from 'react';
import { api, type ProjectEventLog } from '../lib/api';
import { onForegroundMessage } from '../lib/firebase';

const POLL_INTERVAL_MS = 3000;

export default function EventsPage() {
  const [events, setEvents] = useState<ProjectEventLog[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [paused, setPaused] = useState(false);
  const pausedRef = useRef(paused);
  pausedRef.current = paused;

  const load = useCallback(async () => {
    if (pausedRef.current) return;
    try {
      setError(null);
      setEvents(await api.notification.recent(50));
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }, []);

  useEffect(() => {
    load();
    const id = window.setInterval(load, POLL_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [load]);

  useEffect(() => {
    return onForegroundMessage(() => {
      load();
    });
  }, [load]);

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Events</h1>
          <p className="text-xs text-slate-500">
            Polling /api/notification/events/recent every {POLL_INTERVAL_MS / 1000}s.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setPaused((p) => !p)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
          >
            {paused ? 'Resume' : 'Pause'} polling
          </button>
          <button
            onClick={load}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
          >
            Refresh now
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}

      <section className="rounded-lg border border-slate-200 bg-white overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600 text-left">
            <tr>
              <th className="px-3 py-2 font-medium">Time</th>
              <th className="px-3 py-2 font-medium">Source</th>
              <th className="px-3 py-2 font-medium">External ID</th>
              <th className="px-3 py-2 font-medium">Title</th>
              <th className="px-3 py-2 font-medium">Routing key</th>
              <th className="px-3 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            {events.length === 0 && (
              <tr>
                <td colSpan={6} className="px-3 py-6 text-center text-slate-500">
                  No events yet. Trigger a sync from the Sync tab.
                </td>
              </tr>
            )}
            {events.map((e) => (
              <tr key={e.id} className="border-t border-slate-200 align-top">
                <td className="px-3 py-2 text-slate-500 whitespace-nowrap">
                  {new Date(e.deliveredAt).toLocaleTimeString()}
                </td>
                <td className="px-3 py-2">
                  <SourceBadge source={e.source} />
                </td>
                <td className="px-3 py-2 font-mono text-xs break-all">{e.externalId}</td>
                <td className="px-3 py-2">{e.title ?? <span className="text-slate-400">-</span>}</td>
                <td className="px-3 py-2 font-mono text-xs text-slate-500">{e.routingKey}</td>
                <td className="px-3 py-2">
                  <StatusBadge success={e.success} message={e.errorMessage} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

function SourceBadge({ source }: { source: string }) {
  const color =
    source === 'JIRA'
      ? 'bg-indigo-100 text-indigo-700 border-indigo-200'
      : source === 'GITHUB'
        ? 'bg-slate-900 text-white border-slate-900'
        : source === 'MOCK'
          ? 'bg-emerald-100 text-emerald-700 border-emerald-200'
          : 'bg-slate-100 text-slate-700 border-slate-200';
  return (
    <span className={`inline-flex rounded-md border px-2 py-0.5 text-xs font-medium ${color}`}>
      {source}
    </span>
  );
}

function StatusBadge({ success, message }: { success: boolean; message: string | null }) {
  if (success) {
    return (
      <span className="inline-flex rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-xs text-emerald-700">
        ok
      </span>
    );
  }
  return (
    <span
      title={message ?? 'error'}
      className="inline-flex rounded-md border border-red-200 bg-red-50 px-2 py-0.5 text-xs text-red-700"
    >
      error
    </span>
  );
}
