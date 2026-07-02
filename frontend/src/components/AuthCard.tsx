import type { ReactNode } from 'react'

export function AuthCard({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950 px-4 py-12">
      <div className="w-full max-w-md rounded-2xl border border-gray-800 bg-gray-900 p-8 shadow-2xl">
        <div className="mb-8 flex items-center gap-2">
          <span className="h-6 w-6 rounded-md bg-gradient-to-br from-brand-500 to-brand-700" />
          <span className="text-sm font-semibold tracking-wide text-gray-200">bloodlink</span>
        </div>
        {children}
      </div>
    </div>
  )
}
