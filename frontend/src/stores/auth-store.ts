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
  showLoginModal: boolean
  loginModalMessage: string | null
  redirectAfterLogin: string | null
  openLoginModal: (redirect?: string, message?: string) => void
  closeLoginModal: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  showLoginModal: false,
  loginModalMessage: null,
  redirectAfterLogin: null,
  openLoginModal: (redirect, message) => set({ showLoginModal: true, redirectAfterLogin: redirect ?? null, loginModalMessage: message ?? null }),
  closeLoginModal: () => set({ showLoginModal: false, redirectAfterLogin: null, loginModalMessage: null }),
}))
