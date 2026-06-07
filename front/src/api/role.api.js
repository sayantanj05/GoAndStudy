import API from './axios';

export const roleApi = {
  // Get all roles
  getRoles: async (params = {}) => {
    const { data } = await API.get('/api/v1/admin/roles', { params });
    return data.data || data;
  },

  // Get role by ID
  getRole: async (roleId) => {
    const { data } = await API.get(`/api/v1/admin/roles/${roleId}`);
    return data.data || data;
  },

  // Create new role
  createRole: async (roleData) => {
    const { data } = await API.post('/api/v1/admin/roles', roleData);
    return data.data || data;
  },

  // Update role
  updateRole: async (roleId, roleData) => {
    const { data } = await API.put(`/api/v1/admin/roles/${roleId}`, roleData);
    return data.data || data;
  },

  // Delete role
  deleteRole: async (roleId) => {
    const { data } = await API.delete(`/api/v1/admin/roles/${roleId}`);
    return data.data || data;
  }
};
