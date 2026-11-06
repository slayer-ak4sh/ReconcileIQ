import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AuthUser, Role } from '../types'

interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  hasRole: (role: Role) => boolean
  loading: boolean
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [token, setToken] = useState<string | null>(localStorage.getItem('reconcileiq_token'))
  const [loading, setLoading] = useState(true)

  const loginMutation = useMutation({
    mutationFn: async (payload: { email: string; password: string }) => {
      const res = await api.post('/auth/login', payload)
      return res.data
    },
    onSuccess: (data) => {
      const u: AuthUser = {
        email: data.email,
        role: data.role,
        fullName: data.fullName,
      }
      localStorage.setItem('reconcileiq_token', data.token)
      localStorage.setItem('reconcileiq_user', JSON.stringify(u))
      setToken(data.token)
      setUser(u)
    },
  })

  const logout = useCallback(() => {
    localStorage.removeItem('reconcileiq_token')
    localStorage.removeItem('reconcileiq_user')
    setToken(null)
    setUser(null)
  }, [])

  const hasRole = useCallback(
    (role: Role) => user?.role === role,
    [user]
  )

  useEffect(() => {
    const stored = localStorage.getItem('reconcileiq_user')
    if (stored) {
      try {
        setUser(JSON.parse(stored))
      } catch {
        setUser(null)
      }
    }
    setLoading(false)
  }, [])

  const value = useMemo(
    () => ({ user, token, login: (e: string, p: string) => loginMutation.mutateAsync({ email: e, password: p }), logout, hasRole, loading }),
    [user, token, loginMutation, logout, hasRole, loading]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}