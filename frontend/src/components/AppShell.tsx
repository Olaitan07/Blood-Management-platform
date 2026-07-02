import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'

const ADMIN_NAV_LINKS = [
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/hospitals', label: 'Hospitals' },
]

export function AppShell() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-gray-950">
      <header className="border-b border-gray-800 px-6 py-3">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <span className="h-6 w-6 rounded-md bg-gradient-to-br from-brand-500 to-brand-700" />
              <span className="text-sm font-semibold tracking-wide text-gray-200">bloodlink</span>
            </div>
            {user?.role === 'ADMIN' && (
              <nav className="flex items-center gap-4 text-sm">
                {ADMIN_NAV_LINKS.map((link) => (
                  <NavLink
                    key={link.to}
                    to={link.to}
                    className={({ isActive }) =>
                      isActive ? 'font-medium text-gray-100' : 'text-gray-400 hover:text-gray-200'
                    }
                  >
                    {link.label}
                  </NavLink>
                ))}
              </nav>
            )}
          </div>
          {user && (
            <div className="flex items-center gap-3 text-sm">
              <span className="text-gray-400">
                {user.name} <span className="text-gray-600">·</span> {titleCase(user.role)}
              </span>
              <button
                type="button"
                onClick={logout}
                className="rounded-lg border border-gray-700 px-3 py-1.5 text-gray-100 hover:bg-gray-800"
              >
                Log out
              </button>
            </div>
          )}
        </div>
      </header>
      <Outlet />
    </div>
  )
}

function titleCase(value: string): string {
  return value.charAt(0) + value.slice(1).toLowerCase()
}
