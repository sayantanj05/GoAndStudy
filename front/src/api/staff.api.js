import API from './axios';

export const staffApi = {
  // Dashboard
  getStats: async () => {
    const { data } = await API.get('/api/v1/staff/dashboard');
    const dashboard = data.data || {};
    return {
      ...data,
      data: {
        issuesToday: dashboard.issuedToday ?? 0,
        returnsToday: dashboard.returnedToday ?? 0,
        pendingReservations: dashboard.dueToday ?? 0,
        booksOnLoan: dashboard.activeLoans ?? 0,
        ...dashboard,
      },
    };
  },
  
  // Circulation
  issueBook: async (data) => {
    console.log('issueBook request:', data);
    try {
      const response = await API.post('/api/v1/staff/loans/issue', data);
      console.log('issueBook response:', response);
      console.log('issueBook response.data:', response.data);
      return response.data?.data;
    } catch (err) {
      console.error('issueBook error:', err);
      console.error('issueBook error response:', err.response);
      throw err;
    }
  },
  returnBook: async (loanId, notes = '') => {
    const { data } = await API.post(`/api/v1/staff/loans/${loanId}/return`, { notes });
    return data.data;
  },
  renewLoan: async (loanId) => {
    const { data } = await API.patch(`/api/v1/staff/loans/${loanId}/renew`);
    return data.data;
  },
  getActiveLoans: async (params = {}) => {
    const { data } = await API.get('/api/v1/staff/loans/active', { params });
    return data.data;
  },
  getActiveLoanByIsbn: async (isbn) => {
    const { data } = await API.get('/api/v1/staff/loans/active/by-isbn', {
      params: { isbn: isbn.trim() }
    });
    const loans = data.data || [];
    if (loans.length === 0) {
      throw new Error('No active loan found for this ISBN');
    }
    return loans[0];
  },
  getActiveLoansByIsbnAndMember: async (isbn, memberEmail) => {
    const { data } = await API.get('/api/v1/staff/loans/active/by-isbn-member', {
      params: { isbn: isbn.trim(), memberEmail: memberEmail.trim() }
    });
    return data.data || [];
  },

  // Inventory
  getBooks: async (params = {}) => {
    const { data } = await API.get('/api/v1/staff/books', { params: { ...params, size: 0 } });
    return data.data;
  },
  // Lookup a single book by ISBN (staff view)
  getBookByIsbn: async (isbn) => {
    const { data } = await API.get(`/api/v1/staff/books/isbn/${isbn}`);
    return data.data;
  },

  // Members
  getMembers: async (params = {}) => {
    const { data } = await API.get('/api/v1/staff/members', { params });
    return data.data;
  },
  // Staff can lookup member using email or memberId
  getMemberByEmail: async (email) => {
    const { data } = await API.get(`/api/v1/staff/members/lookup`, { params: { q: email } });
    return data.data;
  },

  // Fines
  getRecentFines: async () => {
    const { data } = await API.get('/api/v1/staff/fines/pending');
    return data.data;
  },
  collectFine: async (fineId, amountCollected) => {
    const { data } = await API.patch(`/api/v1/staff/fines/${fineId}/collect`, { amountCollected });
    return data.data;
  },

  // Notifications
  getNotifications: async () => {
    const { data } = await API.get('/api/v1/staff/notifications');
    return data.data;
  },

  markNotificationAsRead: async (notificationId) => {
    const { data } = await API.patch(`/api/v1/staff/notifications/${notificationId}/read`);
    return data;
  },

  // Create book return notification for member
  createReturnNotification: async (memberId, bookTitle, bookId, loanId) => {
    const { data } = await API.post('/api/v1/staff/notifications/return', null, {
      params: { memberId, bookTitle, bookId, loanId },
    });
    return data;
  },
};
