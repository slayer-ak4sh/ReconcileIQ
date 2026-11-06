import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getApiError } from '../api/client'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      const apiErr = getApiError(err)
      setError(apiErr.message || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-ink-50 px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-lg bg-accent-500 flex items-center justify-center text-white font-bold text-xl mx-auto">R</div>
          <h1 className="mt-4 text-2xl font-semibold text-ink-900">ReconcileIQ</h1>
          <p className="text-ink-500 text-sm">Sign in to your reconciliation workspace</p>
        </div>
        <form className="card p-6 space-y-4" onSubmit={submit}>
          <div>
            <label className="block text-sm text-ink-700 mb-1">Email</label>
            <input
              type="email"
              className="w-full px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-sm text-ink-700 mb-1">Password</label>
            <input
              type="password"
              className="w-full px-3 py-2 rounded-md border border-ink-300 focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <div className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-md">{error}</div>}
          <button
            type="submit"
            disabled={busy}
            className="w-full bg-accent-600 hover:bg-accent-700 disabled:opacity-50 text-white py-2 rounded-md text-sm font-medium transition-colors"
          >
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="text-center text-xs text-ink-400 mt-4">
          Demo credentials: exec@demo.com / founder@demo.com
        </p>
      </div>
    </div>
  )
}