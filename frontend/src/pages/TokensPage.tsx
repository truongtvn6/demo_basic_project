import { FormEvent, useCallback, useEffect, useState } from 'react';
import { api, type DeviceToken } from '../lib/api';
import { enablePushOnThisBrowser, isFcmEnabled } from '../lib/firebase';

export default function TokensPage() {
  const [tokens, setTokens] = useState<DeviceToken[]>([]);
  const [token, setToken] = useState('');
  const [label, setLabel] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setError(null);
      setTokens(await api.registry.list());
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!token.trim()) return;
    setLoading(true);
    try {
      await api.registry.register(token.trim(), label.trim() || undefined);
      setToken('');
      setLabel('');
      setInfo('Token registered.');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
      setTimeout(() => setInfo(null), 2500);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm(`Delete device token ${id}?`)) return;
    try {
      await api.registry.remove(id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  };

  const handleEnablePush = async () => {
    try {
      const result = await enablePushOnThisBrowser();
      setInfo(`Registered browser token: ${result.token.slice(0, 16)}...`);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  };

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">Device tokens</h1>
        <div className="flex items-center gap-2">
          {isFcmEnabled() && (
            <button
              onClick={handleEnablePush}
              className="rounded-md bg-emerald-600 px-3 py-2 text-white text-sm font-medium hover:bg-emerald-700"
            >
              Enable push on this browser
            </button>
          )}
          <button
            onClick={load}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
          >
            Refresh
          </button>
        </div>
      </header>

      {error && (
        <div className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </div>
      )}
      {info && (
        <div className="rounded-md border border-emerald-300 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
          {info}
        </div>
      )}

      <section className="rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="font-medium text-slate-900">Register a token manually</h2>
        <p className="text-xs text-slate-500 mb-3">
          Use any string for demo; real FCM tokens come from the Enable push button above.
        </p>
        <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-[1fr_180px_auto] gap-2">
          <input
            value={token}
            onChange={(e) => setToken(e.target.value)}
            placeholder="FCM token or fake string"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
            required
          />
          <input
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            placeholder="Label (optional)"
            className="rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-slate-500 focus:outline-none"
          />
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-slate-900 px-4 py-2 text-white text-sm font-medium disabled:opacity-60"
          >
            {loading ? 'Saving...' : 'Register'}
          </button>
        </form>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600 text-left">
            <tr>
              <th className="px-3 py-2 font-medium">#</th>
              <th className="px-3 py-2 font-medium">Token</th>
              <th className="px-3 py-2 font-medium">Label</th>
              <th className="px-3 py-2 font-medium">Created</th>
              <th className="px-3 py-2 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {tokens.length === 0 && (
              <tr>
                <td colSpan={5} className="px-3 py-6 text-center text-slate-500">
                  No tokens registered yet.
                </td>
              </tr>
            )}
            {tokens.map((t) => (
              <tr key={t.id} className="border-t border-slate-200">
                <td className="px-3 py-2 text-slate-500">{t.id}</td>
                <td className="px-3 py-2 font-mono text-xs break-all">{t.token}</td>
                <td className="px-3 py-2">{t.label ?? <span className="text-slate-400">-</span>}</td>
                <td className="px-3 py-2 text-slate-500">
                  {new Date(t.createdAt).toLocaleString()}
                </td>
                <td className="px-3 py-2 text-right">
                  <button
                    onClick={() => handleDelete(t.id)}
                    className="rounded-md border border-red-300 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
