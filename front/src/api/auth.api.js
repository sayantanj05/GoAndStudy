import api from './axios';

export const authApi = {
  login: async (email, password) => {
    const response = await api.post('/api/v1/auth/login', { email, password });
    // Backend returns ApiResponse wrapper, extract the actual data
    return response.data.data || response.data;
  },
  
  register: async (userData) => {
    const response = await api.post('/api/v1/auth/register', userData);
    // Backend returns ApiResponse wrapper, extract the actual data
    return response.data.data || response.data;
  },
  
  logout: async () => {
    return await api.post('/api/v1/auth/logout');
  }
};
