import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { LogOut, LayoutDashboard, UploadCloud, TableProperties, FileText, FolderKanban, BarChart3 } from 'lucide-react'

const NAV = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/orders', label: 'Orders', icon: TableProperties },
  { to: '/claims', label: 'Claims', icon: FolderKanban },
  { to: '/uploads', label: 'Uploads', icon: UploadCloud, exec: true },
  { to: '/mapping', label: 'Column Mapping', icon: FileText, exec: true },
  { to: '/ratecards', label: 'Rate Cards', icon: BarChart3, exec: true },
]

export default function Layout() {
  const { user, logout, hasRole } = useAuth()
  const loc = useLocation()

  return (
    <div className="min-h-screen flex">
      <aside className="w-60 bg-ink-900 text-ink-300 flex flex-col">
        <div className="p-5 border-b border-ink-700">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-md bg-accent-500 flex items-center justify-center text-white font-semibold">R</div>
            <div>
              <div className="text-white font-semibold text-sm">ReconcileIQ</div>
              <div className="text-ink-500 text-xs">Finance reconciliation</div>
            </div>
          </div>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {NAV.map((item) => {
            if (item.exec && !hasRole('ROLE_FINANCE_EXECUTIVE')) return null
            const active = loc.pathname === item.to
            const Icon = item.icon
            return (
              <Link
                key={item.to}
                to={item.to}
                className={`flex items-center gap-2.5 px-3 py-2 rounded-md text-sm transition-colors ${
                  active ? 'bg-ink-700 text-white' : 'hover:bg-ink-800 text-ink-300'
                }`}
              >
                <Icon size={18} />
                {item.label}
              </Link>
            )
          })}
        </nav>
        <div className="p-3 border-t border-ink-700">
          <div className="px-3 py-2">
            <div className="text-white text-sm truncate">{user?.fullName}</div>
            <div className="text-ink-500 text-xs truncate">{user?.email}</div>
          </div>
          <button
            onClick={logout}
            className="w-full flex items-center gap-2 px-3 py-2 rounded-md text-sm text-ink-300 hover:bg-ink-800 hover:text-white transition-colors"
          >
            <LogOut size={18} /> Sign out
          </button>
        </div>
      </aside>

      <main className="flex-1 flex flex-col min-h-screen">
        <header className="bg-white border-b border-ink-200 px-6 py-3 flex items-center justify-between">
          <h1 className="text-ink-900 font-semibold">
            {NAV.find((n) => n.to === loc.pathname)?.label || 'ReconcileIQ'}
          </h1>
          <span className="text-xs text-ink-500">Demo environment</span>
        </header>
        <div className="flex-1 p-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}