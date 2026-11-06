import { createHashRouter, Navigate, RouteObject } from 'react-router-dom'
import { useAuth } from './auth/useAuth'
import type { Role } from './types'

import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Uploads from './pages/Uploads'
import Mapping from './pages/Mapping'
import Orders from './pages/Orders'
import OrderDetail from './pages/OrderDetail'
import Claims from './pages/Claims'
import RateCards from './pages/RateCards'
import Login from './pages/Login'

function ProtectedRoute({ roles, children }: { roles?: Role[]; children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="p-8 text-center text-ink-500">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <>{children}</>
}

export const router = createHashRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: (
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        ),
      },
      {
        path: 'uploads',
        element: (
          <ProtectedRoute roles={['ROLE_FINANCE_EXECUTIVE']}>
            <Uploads />
          </ProtectedRoute>
        ),
      },
      {
        path: 'mapping',
        element: (
          <ProtectedRoute roles={['ROLE_FINANCE_EXECUTIVE']}>
            <Mapping />
          </ProtectedRoute>
        ),
      },
      {
        path: 'orders',
        element: (
          <ProtectedRoute>
            <Orders />
          </ProtectedRoute>
        ),
      },
      {
        path: 'orders/:id',
        element: (
          <ProtectedRoute>
            <OrderDetail />
          </ProtectedRoute>
        ),
      },
      {
        path: 'claims',
        element: (
          <ProtectedRoute>
            <Claims />
          </ProtectedRoute>
        ),
      },
      {
        path: 'ratecards',
        element: (
          <ProtectedRoute roles={['ROLE_FINANCE_EXECUTIVE']}>
            <RateCards />
          </ProtectedRoute>
        ),
      },
    ],
  },
  { path: '/login', element: <Login /> },
  { path: '*', element: <Navigate to="/" replace /> },
] as RouteObject[])

export default router