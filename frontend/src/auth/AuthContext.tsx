import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getCurrentUser, login as loginRequest } from '@/api/auth'
import { registerUnauthorizedHandler, setAuthToken } from '@/api/client'
import { decodeToken } from '@/lib/jwt'
import type { LoginRequest, Role } from '@/api/types'

interface AuthUser {
  userId: number
  name: string
  role: Role
  email: string
  // Not carried in the JWT payload itself (it's injected server-side from the
  // DB into the auth details on each request) — fetched via GET /api/auth/me.
  hospitalId: number | null
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (payload: LoginRequest) => Promise<AuthUser>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)

  const logout = useCallback(() => {
    setAuthToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    // A 401/403 on a request that carried a token means the session was
    // invalidated server-side — force logout rather than retry.
    registerUnauthorizedHandler(logout)
  }, [logout])

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await loginRequest(payload)
    setAuthToken(response.token)
    const claims = decodeToken(response.token)
    const currentUser = await getCurrentUser()
    const authUser: AuthUser = {
      userId: claims.userId,
      name: claims.name,
      role: claims.role,
      email: claims.sub,
      hospitalId: currentUser.hospitalId,
    }
    setUser(authUser)
    return authUser
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAuthenticated: user !== null, login, logout }),
    [user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
