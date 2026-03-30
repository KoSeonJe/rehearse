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
  setUser: (user: AuthUser | null) => void
  setLoading: (loading: boolean) => void
  openLoginModal: () => void
  closeLoginModal: () => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isLoading: true,
  showLoginModal: false,
  setUser: (user) => set({ user }),
  setLoading: (isLoading) => set({ isLoading }),
  openLoginModal: () => set({ showLoginModal: true }),
  closeLoginModal: () => set({ showLoginModal: false }),
  logout: () => set({ user: null }),
}))
