import { NavLink } from 'react-router-dom';

const linkBase = 'px-3 py-2 rounded-md text-sm font-medium transition-colors';
const active = 'bg-slate-900 text-white';
const idle = 'text-slate-700 hover:bg-slate-200';

export default function Nav() {
  return (
    <header className="bg-white border-b border-slate-200">
      <div className="max-w-5xl mx-auto px-4 py-3 flex items-center gap-4">
        <div className="font-semibold text-slate-900">demobasic admin</div>
        <nav className="flex items-center gap-1">
          <NavLink
            to="/tokens"
            className={({ isActive }) => `${linkBase} ${isActive ? active : idle}`}
          >
            Tokens
          </NavLink>
          <NavLink
            to="/sync"
            className={({ isActive }) => `${linkBase} ${isActive ? active : idle}`}
          >
            Sync
          </NavLink>
          <NavLink
            to="/events"
            className={({ isActive }) => `${linkBase} ${isActive ? active : idle}`}
          >
            Events
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
