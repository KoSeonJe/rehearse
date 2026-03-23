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
  setUser: (user: AuthUser | null) => void
  setLoading: (loading: boolean) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  isLoading: true,
  setUser: (user) => set({ user }),
  setLoading: (isLoading) => set({ isLoading }),
  logout: () => set({ user: null }),
}))
