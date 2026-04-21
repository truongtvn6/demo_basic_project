import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import Nav from './components/Nav';
import TokensPage from './pages/TokensPage';
import SyncPage from './pages/SyncPage';
import EventsPage from './pages/EventsPage';
import { onForegroundMessage } from './lib/firebase';

interface Toast {
  id: number;
  title: string;
  body: string;
}

let toastSeq = 0;

export default function App() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    return onForegroundMessage(({ title, body }) => {
      const toast: Toast = {
        id: ++toastSeq,
        title: title ?? 'New event',
        body: body ?? '',
      };
      setToasts((prev) => [...prev, toast]);
      window.setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== toast.id));
      }, 5000);
    });
  }, []);

  return (
    <div className="min-h-full flex flex-col">
      <Nav />
      <main className="flex-1 max-w-5xl w-full mx-auto px-4 py-6">
        <Routes>
          <Route path="/" element={<Navigate to="/tokens" replace />} />
          <Route path="/tokens" element={<TokensPage />} />
          <Route path="/sync" element={<SyncPage />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="*" element={<Navigate to="/tokens" replace />} />
        </Routes>
      </main>
      <footer className="py-4 text-center text-xs text-slate-500">
        demobasic · Gateway · {import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'}
      </footer>

      <div className="fixed bottom-4 right-4 space-y-2 z-50">
        {toasts.map((t) => (
          <div
            key={t.id}
            className="w-80 rounded-lg border border-slate-200 bg-white shadow-lg p-3 animate-in fade-in"
          >
            <div className="text-sm font-semibold text-slate-900">{t.title}</div>
            {t.body && <div className="text-xs text-slate-600 mt-1">{t.body}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
