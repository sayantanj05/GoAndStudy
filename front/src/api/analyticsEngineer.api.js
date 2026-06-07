import API from './axios';

export const analyticsEngineerApi = {
  // Dashboard
  getDashboard: async () => {
    const res = await API.get('/api/v1/analytics-engineer/dashboard');
    return res.data;
  },

  // Member analytics
  getMemberAnalyticsSummary: async () => {
    const res = await API.get('/api/v1/analytics-engineer/member-metrics');
    return res.data;
  },

  // Genre analytics
  getGenreAnalytics: async () => {
    const res = await API.get('/api/v1/analytics-engineer/genre-metrics');
    return res.data;
  },

  // AI usage
  getAiUsage: async (params = {}) => {
    const res = await API.get('/api/v1/analytics-engineer/ai-usage', { params });
    return res.data;
  },

  // Operational actions (placeholders until backend is implemented)
  triggerBatchSync: async (payload) => {
    const res = await API.post('/api/v1/analytics-engineer/etl/sync', payload);
    return res.data;
  },
  toggleChangeStream: async (enabled) => {
    const res = await API.post('/api/v1/analytics-engineer/etl/change-stream', { enabled });
    return res.data;
  },

  // Recommendations QA (placeholders until backend is implemented)
  impersonateUser: async (userId) => {
    const res = await API.get('/api/v1/analytics-engineer/recommendations/impersonate', {
      params: { userId },
    });
    return res.data;
  },

  
  // Operations Control
  startPipeline: async () => {
    const res = await API.post('/api/v1/analytics-engineer/pipeline/start');
    return res.data;
  },

  stopPipeline: async () => {
    const res = await API.post('/api/v1/analytics-engineer/pipeline/stop');
    return res.data;
  },

  triggerSync: async (params = {}) => {
    const res = await API.post('/api/v1/analytics-engineer/etl/trigger-sync', params);
    return res.data;
  },

  getReports: async (params = {}) => {
    const res = await API.get('/api/v1/analytics-engineer/reports', { params });
    return res.data;
  },

  // Profile Management
  getProfile: async () => {
    const res = await API.get('/api/v1/analytics-engineer/profile');
    return res.data;
  },

  updateProfile: async (profileData) => {
    const res = await API.put('/api/v1/analytics-engineer/profile', profileData);
    return res.data;
  },

  getProfileStats: async () => {
    const res = await API.get('/api/v1/analytics-engineer/profile/stats');
    return res.data;
  },
};

