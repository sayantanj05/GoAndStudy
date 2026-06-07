import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authApi } from '../api/auth.api';

export const useAuthStore = create(
  persist(
    (set) => ({
      token: null,
      user: null,   // {userId, name, role, email}
      role: null,   // ROLE_ADMIN | ROLE_STAFF | ROLE_MEMBER
      isAuthenticated: false,

      login: async (email, password) => {
        const response = await authApi.login(email, password);
        console.log('AuthStore - Login response:', response);
        const { token, role, userId, name } = response;
        console.log('AuthStore - Extracted data:', { token, role, userId, name });
        set({ token, user: { userId, name, role, email }, role, isAuthenticated: true });
        console.log('AuthStore - State after login:', { token, user: { userId, name, role, email }, role, isAuthenticated: true });
        return response;
      },
      
      register: async (userData) => {
        const response = await authApi.register(userData);
        const { token, role, memberId, name, email: regEmail } = response;
        set({ token, user: { userId: memberId, name, role, email: regEmail }, role, isAuthenticated: true });
        return response;
      },
      
      logout: () => {
        authApi.logout().catch(() => {}); // Fire and forget
        set({ token: null, user: null, role: null, isAuthenticated: false });
        // Clear all access_granted flags from sessionStorage
        Object.keys(sessionStorage).forEach(key => {
          if (key.startsWith('access_granted_')) {
            sessionStorage.removeItem(key);
          }
        });
      },
      
      updateUser: (updates) => set((state) => ({ 
        user: state.user ? { ...state.user, ...updates } : null 
      })),
    }),
    { name: 'goandstudyauth' }
  )
);

export default useAuthStore;
