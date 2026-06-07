import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

const getPersistedToken = () => {
  try {
    const stored = localStorage.getItem('goandstudyauth');
    return stored ? JSON.parse(stored)?.state?.token || null : null;
  } catch (error) {
    return null;
  }
};

// Attach JWT token to every request if available
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token || getPersistedToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Global response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle 401 Unauthorized — clear session and redirect
    // Skip if this is the logout request itself to prevent infinite loops
    const isLogoutRequest = error.config?.url?.includes('/auth/logout');
    if (error.response?.status === 401 && !isLogoutRequest) {
      useAuthStore.getState().logout();
      window.location.href = '/';
    }
    
    // Provide a consistent error object to the rest of the app
    const message = error.response?.data?.message || error.message || 'Something went wrong';
    return Promise.reject({ ...error, message });
  }
);

export default api;
