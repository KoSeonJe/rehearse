import { create } from 'zustand'

export interface AuthUser {
  id: number
  email: string
  name: string
  profileImage: string | null
  provider: 'GITHUB' | 'GOOGLE'
  role: string
}

interface AuthStore {
  user: AuthUser | null
  isLoading: boolean
  showLoginModal: boolean
  loginModalMessage: string | null
  redirectAfterLogin: string | null
  setUser: (user: AuthUser | null) => void
  setLoading: (loading: boolean) => void
  openLoginModal: (redirect?: string, message?: string) => void
  closeLoginModal: () => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isLoading: true,
  showLoginModal: false,
  loginModalMessage: null,
  redirectAfterLogin: null,
  setUser: (user) => set({ user, ...(user ? { showLoginModal: false, loginModalMessage: null, redirectAfterLogin: null } : {}) }),
  setLoading: (isLoading) => set({ isLoading }),
  openLoginModal: (redirect, message) => set({ showLoginModal: true, redirectAfterLogin: redirect ?? null, loginModalMessage: message ?? null }),
  closeLoginModal: () => set({ showLoginModal: false, redirectAfterLogin: null, loginModalMessage: null }),
  logout: () => set({ user: null }),
}))
