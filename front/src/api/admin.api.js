import API from './axios';

const DAY_LABELS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
const MONTH_LABELS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const toLocalDateKey = (value) => {
  const date = new Date(value);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const groupByPeriod = (period, loans = [], recentActivity = []) => {
  const now = new Date();
  const returnActivities = recentActivity.filter(
    (activity) => activity.eventType === 'BOOK_RETURNED' && activity.createdAt
  );

  if (period === 'yearly') {
    const years = new Set([now.getFullYear()]);

    loans.forEach((loan) => {
      if (!loan.issuedAt) return;
      years.add(new Date(loan.issuedAt).getFullYear());
    });

    returnActivities.forEach((activity) => {
      years.add(new Date(activity.createdAt).getFullYear());
    });

    return Array.from(years)
      .sort((a, b) => a - b)
      .map((year) => ({
        label: `${year}`,
        value: loans.filter((loan) => loan.issuedAt && new Date(loan.issuedAt).getFullYear() === year).length,
        value2: returnActivities.filter((activity) => new Date(activity.createdAt).getFullYear() === year).length,
        sortKey: year,
      }));
  }

  if (period === 'monthly') {
    const buckets = MONTH_LABELS.map((label, index) => ({
      label,
      value: 0,
      value2: 0,
      sortKey: index,
    }));

    loans.forEach((loan) => {
      if (!loan.issuedAt) return;
      const issuedAt = new Date(loan.issuedAt);
      if (issuedAt.getFullYear() !== now.getFullYear()) return;
      buckets[issuedAt.getMonth()].value += 1;
    });

    returnActivities.forEach((activity) => {
      const createdAt = new Date(activity.createdAt);
      if (createdAt.getFullYear() !== now.getFullYear()) return;
      buckets[createdAt.getMonth()].value2 += 1;
    });

    return buckets;
  }

  if (period === 'weekly') {
    const buckets = Array.from({ length: 4 }, (_, index) => ({
      label: `Week ${index + 1}`,
      value: 0,
      value2: 0,
      sortKey: index,
    }));
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);

    loans.forEach((loan) => {
      if (!loan.issuedAt) return;
      const issuedAt = new Date(loan.issuedAt);
      if (issuedAt < monthStart || issuedAt.getMonth() !== now.getMonth() || issuedAt.getFullYear() !== now.getFullYear()) {
        return;
      }
      const weekIndex = Math.min(3, Math.floor((issuedAt.getDate() - 1) / 7));
      buckets[weekIndex].value += 1;
    });

    returnActivities.forEach((activity) => {
      const createdAt = new Date(activity.createdAt);
      if (createdAt < monthStart || createdAt.getMonth() !== now.getMonth() || createdAt.getFullYear() !== now.getFullYear()) {
        return;
      }
      const weekIndex = Math.min(3, Math.floor((createdAt.getDate() - 1) / 7));
      buckets[weekIndex].value2 += 1;
    });

    return buckets;
  }

  const buckets = DAY_LABELS.map((label, index) => ({
    label,
    value: 0,
    value2: 0,
    sortKey: index,
  }));
  const dateToLabel = new Map();
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date(now);
    date.setHours(0, 0, 0, 0);
    date.setDate(now.getDate() - i);
    dateToLabel.set(toLocalDateKey(date), DAY_LABELS[date.getDay()]);
  }

  loans.forEach((loan) => {
    if (!loan.issuedAt) return;
    const key = toLocalDateKey(loan.issuedAt);
    const label = dateToLabel.get(key);
    if (!label) return;
    const bucket = buckets.find((item) => item.label === label);
    if (bucket) bucket.value += 1;
  });

  returnActivities.forEach((activity) => {
    const key = toLocalDateKey(activity.createdAt);
    const label = dateToLabel.get(key);
    if (!label) return;
    const bucket = buckets.find((item) => item.label === label);
    if (bucket) bucket.value2 += 1;
  });

  return buckets;
};

const buildCategoryMatrix = (genres = [], books = [], loans = []) => {
  const loanCounts = new Map();
  loans.forEach((loan) => {
    const label = loan.genre || loan.category || 'Uncategorized';
    loanCounts.set(label, (loanCounts.get(label) ?? 0) + 1);
  });
  if (loanCounts.size > 0) {
    return Array.from(loanCounts.entries())
      .map(([label, value]) => ({ label, value }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 6);
  }

  if (genres.length > 0) {
    return genres
      .map((genre) => ({
        label: genre.genreName || genre.name || 'Unknown',
        value: genre.totalLoans ?? genre.loansLast30Days ?? 0,
      }))
      .filter((item) => item.label)
      .sort((a, b) => b.value - a.value)
      .slice(0, 6);
  }

  const counts = new Map();
  books.forEach((book) => {
    const label = book.genre || book.category;
    if (!label) return;
    counts.set(label, (counts.get(label) ?? 0) + 1);
  });

  return Array.from(counts.entries())
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 6);
};

export const adminApi = {
  // Authors API
  getAuthors: () => API.get('/api/v1/admin/authors'),
  addAuthor: (data) => API.post('/api/v1/admin/authors', data),
  getAuthor: (id) => API.get(`/api/v1/admin/authors/${id}`),
  updateAuthor: (id, data) => API.put(`/api/v1/admin/authors/${id}`, data),
  deleteAuthor: (id) => API.delete(`/api/v1/admin/authors/${id}`),

  // Dashboard
  getStats: async () => {
    const { data } = await API.get('/api/v1/admin/analytics/dashboard');
    const dashboard = data.data || {};
    const registrationCounts = new Map(
      (dashboard.registrationsThisWeek || []).map((item) => [item.day || '', item.count ?? 0])
    );
    const weeklyActivity = (dashboard.loansThisWeek || []).map((item) => ({
      day: item.day || '',
      loans: item.loans ?? item.count ?? 0,
      returns: item.returns ?? 0,
      visitors: registrationCounts.get(item.day || '') ?? 0,
    }));
    const totalFines = (dashboard.fineCollectionTrend || []).reduce(
      (sum, item) => sum + (item.totalCollected ?? 0),
      0
    );

    return {
      ...data,
      data: {
        ...dashboard,
        overdueBooks: dashboard.overdueLoans ?? 0,
        totalFines,
        weeklyActivity,
      },
    };
  },
  getAnalytics: async (period) => {
    const [dashboardRes, genresRes, booksRes, loansRes, membersRes] = await Promise.all([
      API.get('/api/v1/admin/analytics/dashboard'),
      API.get('/api/v1/admin/analytics/genres'),
      API.get('/api/v1/admin/books'),
      API.get('/api/v1/admin/loans'),
      API.get('/api/v1/admin/members'),
    ]);

    const dashboard = dashboardRes.data.data || {};
    const genres = genresRes.data.data?.genres || [];
    const books = booksRes.data.data?.books || [];
    const loans = loansRes.data.data?.loans || [];
    const members = membersRes.data.data?.members || [];

    const chartData = period === 'days' && Array.isArray(dashboard.loansThisWeek)
      ? dashboard.loansThisWeek.map((item) => ({
          label: item.day,
          value: item.loans ?? item.count ?? 0,
          value2: item.returns ?? 0,
        }))
      : groupByPeriod(period, loans, dashboard.recentActivity || []);
    const categoryData = buildCategoryMatrix(genres, books, loans);

    const totalLoans = loans.length;
    const totalBooks = books.length;
    const circulationRate = totalBooks === 0 ? 0 : (totalLoans / totalBooks) * 100;
    const avgLoanDuration = loans.length === 0
      ? 0
      : loans.reduce((sum, loan) => {
          if (!loan.issuedAt || !loan.dueDate) return sum;
          const issuedAt = new Date(loan.issuedAt);
          const dueDate = new Date(loan.dueDate);
          return sum + Math.max(0, Math.round((dueDate - issuedAt) / (1000 * 60 * 60 * 24)));
        }, 0) / loans.length;
    const inventoryTurnover = totalBooks === 0 ? 0 : totalLoans / totalBooks;
    const retainedMembers = members.filter((member) => (member.loginCount ?? 0) > 1).length;
    const userRetention = members.length === 0 ? 0 : (retainedMembers / members.length) * 100;

    return {
      data: {
        chartData,
        categoryData,
        topMetrics: [
          { label: 'Circulation Rate', value: `${circulationRate.toFixed(1)}%`, trend: circulationRate > 0 ? 'up' : 'down', delta: `${totalLoans} loans` },
          { label: 'Avg Loan Duration', value: `${avgLoanDuration.toFixed(1)} Days`, trend: avgLoanDuration > 0 ? 'up' : 'down', delta: `${loans.length} records` },
          { label: 'Inventory Turnover', value: `${inventoryTurnover.toFixed(1)}x`, trend: inventoryTurnover > 0 ? 'up' : 'down', delta: `${totalBooks} books` },
          { label: 'User Retention', value: `${userRetention.toFixed(0)}%`, trend: userRetention > 0 ? 'up' : 'down', delta: `${retainedMembers} repeat users` },
        ],
      },
    };
  },
  
  // Staff Management
  getStaff: () => API.get('/api/v1/admin/staff'),
  createStaff: (data) => API.post('/api/v1/admin/staff', data),
  updateStaff: (id, data) => API.put(`/api/v1/admin/staff/${id}`, data),
  deleteStaff: (id) => API.delete(`/api/v1/admin/staff/${id}`),
  toggleStaffStatus: (id) => API.patch(`/api/v1/admin/staff/${id}/toggle`),

  // Member Management
  getMembers: () => API.get('/api/v1/admin/members'),
  getMemberDetails: (id) => API.get(`/api/v1/admin/members/${id}`),
  deactivateMember: (memberId) => API.patch(`/api/v1/admin/members/${memberId}/deactivate`),
  reactivateMember: (memberId) => API.patch(`/api/v1/admin/members/${memberId}/reactivate`),
  deleteMember: (memberId) => API.delete(`/api/v1/admin/members/${memberId}`),

  // Inventory/Books
  getBooks: () => API.get('/api/v1/admin/books', { params: { isDeleted: false, page: 1, size: 0 } }),
  getLowStock: () => API.get('/api/v1/admin/books/low-stock'),
  getCategories: () => API.get('/api/v1/admin/categories'),
  addCategory: (data) => API.post('/api/v1/admin/categories', data),
  updateCategory: (id, data) => API.put(`/api/v1/admin/categories/${id}`, data),
  deleteCategory: (id) => API.delete(`/api/v1/admin/categories/${id}`),
  addBook: (data) => API.post('/api/v1/staff/books', data),
  updateBook: (bookId, payload) => API.put(`/api/v1/staff/books/${bookId}`, payload),
  deleteBook: (bookId, reason) => API.delete(`/api/v1/admin/books/${bookId}`, { data: reason ? { reason } : undefined }),

  // Financials
  getFines: (status) => API.get('/api/v1/admin/fines', { params: status ? { status } : {} }),
  waiveFine: (id) => API.post(`/api/v1/admin/fines/${id}/waive`, { reason: 'Waived by admin' }),
  payFine: (id) => API.post(`/api/v1/admin/fines/${id}/pay`),

  // Loan Management
  getLoans: () => API.get('/api/v1/admin/loans'),
  detectOverdues: () => API.post('/api/v1/admin/loans/detect-overdues'),

  // Configuration
  getConfig: () => API.get('/api/v1/admin/config'),
  updateConfig: (data) => API.put('/api/v1/admin/config', data),

  // Reports
  generateReport: (type) => {
    const now = new Date();
    const periodStart = new Date(now);
    periodStart.setMonth(now.getMonth() - 1);
    return API.post('/api/v1/admin/reports', {
      reportType: type,
      periodStart: periodStart.toISOString(),
      periodEnd: now.toISOString(),
    });
  },

  // Export report as CSV/TSV
  exportReport: (type, format = 'csv') => {
    return API.get('/api/v1/admin/reports/export', {
      params: {
        reportType: type,
        format: format,
      },
      responseType: 'blob', // Important for file downloads
    });
  },

  // Broadcast Notifications
  broadcastNotification: (message) => API.post('/api/v1/admin/notifications/broadcast', { message }),
};
