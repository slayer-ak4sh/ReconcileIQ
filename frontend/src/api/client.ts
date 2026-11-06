import axios, { AxiosInstance, AxiosError } from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const api: AxiosInstance = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('reconcileiq_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('reconcileiq_token')
      localStorage.removeItem('reconcileiq_user')
      window.location.href = '/login'
    }
    return Promise.resolve(error)
  }
)

export function getApiError(error: unknown): { message: string; code?: string; details?: unknown } {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as any
    return {
      message: data?.message || error.message || 'Request failed',
      code: data?.code,
      details: data?.details,
    }
  }
  return { message: error instanceof Error ? error.message : 'Unknown error' }
}

export default api