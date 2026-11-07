import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getApiError } from '../api/client'

interface LoginFormData {
  email: string
  password: string
}

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [formData, setFormData] = useState<LoginFormData>({ email: '', password: '' })
  const [emailError, setEmailError] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const from = (location.state as any)?.from?.pathname || '/'

  const showSessionExpired = new URLSearchParams(location.search).get('session') === 'expired'

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setEmailError(null)
    setPasswordError(null)

    // Client-side validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.email)) {
      setEmailError('Enter a valid email address')
      return
    }
    if (!formData.password.trim()) {
      setPasswordError('Password is required')
      return
    }

    setIsSubmitting(true)
    try {
      await login(formData.email, formData.password)
      navigate(from, { replace: true })
    } catch (err: unknown) {
      const apiErr = getApiError(err)
      if (apiErr.message === 'Unauthorized' || apiErr.code === 'UNAUTHORIZED') {
        setPasswordError('Session expired. Please log in again.')
      } else {
        setEmailError('Incorrect email or password.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChange = (field: keyof LoginFormData) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [field]: e.target.value })
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-ink-50 px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-6">
          <div className="w-12 h-12 rounded-lg bg-accent-500 flex items-center justify-center text-white font-bold text-xl mx-auto">R</div>
          <h1 className="mt-4 text-2xl font-semibold text-ink-900">ReconcileIQ</h1>
          <p className="text-ink-500 text-sm">Sign in to your reconciliation workspace</p>
        </div>
        {showSessionExpired && (
          <div className="text-sm text-amber-600 bg-amber-50 px-3 py-2 rounded-md mb-4" aria-live="polite">
            Your session has expired. Please log in again.
          </div>
        )}
        <form className="card p-6 space-y-4" onSubmit={onSubmit}>
          <div>
            <label className="block text-sm text-ink-700 mb-1" htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              autoFocus
              className={`w-full px-3 py-2 rounded-md border ${emailError ? 'border-red-300' : 'border-ink-300'} focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none`}
              value={formData.email}
              onChange={handleChange('email')}
              aria-invalid={!!emailError}
              aria-describedby="email-error"
              required
            />
            {emailError && (
              <p id="email-error" className="text-xs text-red-600 mt-1" aria-live="polite">{emailError}</p>
            )}
          </div>
          <div>
            <label className="block text-sm text-ink-700 mb-1" htmlFor="password">Password</label>
            <PasswordInput
              id="password"
              autoComplete="current-password"
              className={emailError ? '' : ''}
              aria-invalid={!!passwordError}
              aria-describedby="password-error"
              value={formData.password}
              onChange={handleChange('password')}
            />
            {passwordError && (
              <p id="password-error" className="text-xs text-red-600 mt-1" aria-live="polite">{passwordError}</p>
            )}
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-accent-600 hover:bg-accent-700 disabled:opacity-50 text-white py-2 rounded-md text-sm font-medium transition-colors"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
        <p className="text-center text-xs text-ink-400 mt-4">
          Demo credentials: exec@demo.com / founder@demo.com
        </p>
      </div>
    </div>
  )
}

interface PasswordInputProps {
  id: string
  autoComplete?: string
  className?: string
  value: string
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  'aria-invalid'?: boolean
  'aria-describedby'?: string
}

function PasswordInput({ id, autoComplete, className = '', value, onChange, 'aria-invalid': ariaInvalid, 'aria-describedby': ariaDescribedBy }: PasswordInputProps) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative">
      <input
        id={id}
        type={show ? 'text' : 'password'}
        autoComplete={autoComplete}
        className={`w-full px-3 py-2 pr-10 rounded-md border ${ariaInvalid ? 'border-red-300' : (className || 'border-ink-300')} focus:ring-2 focus:ring-accent-500 focus:border-accent-500 outline-none`}
        value={value}
        onChange={onChange}
        aria-invalid={ariaInvalid}
        aria-describedby={ariaDescribedBy}
        required
      />
      <button
        type="button"
        tabIndex={-1}
        className="absolute right-2 top-1/2 -translate-y-1/2 text-ink-500 hover:text-ink-700"
        onClick={() => setShow(!show)}
        aria-label={show ? 'Hide password' : 'Show password'}
      >
        {show ? '🔒' : '👁️'}
      </button>
    </div>
  )
}