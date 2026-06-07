import API from './axios';

const mapBookSummary = (book = {}) => ({
  bookId: book.bookId ?? book.id ?? book._id ?? book.book_id ?? book.isbn ?? '',
  title: book.title ?? '',
  author: book.author ?? (book.authorIds?.[0] ?? ''),
  // Prefer human names from backend; fall back to IDs only if necessary.
  authors: book.authors
    ?? (book.author ? [book.author] : null)
    ?? (book.authorIds ?? []),
  coverImageUrl: book.coverImageUrl ?? '',
  availableCopies: book.availableCopies ?? 0,
  averageRating: book.averageRating ?? 0,
  genre: book.genre ?? book.category ?? '',
  category: book.category ?? book.genre ?? '',
  // Support multiple categories for display
  categoryNames: book.categoryNames ?? (book.category ? [book.category] : []),
  categoryIds: book.categoryIds ?? [],
  isbn: book.isbn ?? '',
  isWishlisted: Boolean(book.isWishlisted),
});

const mapBookDetail = (payload = {}) => {
  const book = payload.book ?? {};
  const authors = payload.authors ?? [];
  const categories = payload.categories ?? [];
  const topReviews = payload.topReviews ?? [];

  // Safely map authors to array of strings
  const authorNames = Array.isArray(authors) 
    ? authors
        .filter(author => author && author.name)
        .map((item) => item.name)
    : [];

  // Extract all category names and IDs with multiple fallbacks
  const categoryNames = categories.length > 0
    ? categories
        .filter(cat => cat && cat.name)
        .map((cat) => cat.name)
    : (book.categoryNames ?? (book.category ? [book.category] : []));
  
  const categoryIds = book.categoryIds 
    ? (Array.isArray(book.categoryIds) ? book.categoryIds : [])
    : (categories.length > 0 ? categories.map((cat) => cat.id).filter(Boolean) : []);

  // Map reviews to include member names
  const reviews = Array.isArray(topReviews)
    ? topReviews.map(review => ({
        id: review.id ?? review._id ?? '',
        rating: review.rating ?? 0,
        reviewText: review.reviewText ?? review.feedbackText ?? '',
        helpfulVotes: review.helpfulVotes ?? 0,
        createdAt: review.createdAt ?? '',
        memberName: review.memberName ?? 'Anonymous Member',
      }))
    : [];

  return {
    bookId: book.id ?? book.bookId ?? '',
    title: book.title ?? 'Unknown Title',
    author: authorNames[0] ?? (book.authorIds?.[0] ?? 'Unknown Author'),
    authors: authorNames.length > 0 ? authorNames : ['Unknown Author'],
    category: categories[0]?.name ?? book.category ?? (categoryNames[0] ?? 'Uncategorized'),
    genre: categories[0]?.name ?? book.genre ?? (categoryNames[0] ?? 'Uncategorized'),
    categoryNames: categoryNames.length > 0 ? categoryNames : ['Uncategorized'],
    categoryIds: categoryIds.length > 0 ? categoryIds : [],
    coverImageUrl: book.coverImageUrl ?? '',
    availableCopies: typeof book.availableCopies === 'number' ? book.availableCopies : 0,
    averageRating: typeof book.averageRating === 'number' ? book.averageRating : 0,
    isbn: book.isbn ?? '',
    description: book.description ?? '',
    isWishlisted: Boolean(payload.isWishlisted),
    location: book.location ?? '',
    reviews: reviews.slice(0, 3),
  };
};

const mapRecommendation = (item = {}) => ({
  id: item.bookId ?? item.id ?? '',
  bookId: item.bookId ?? item.id ?? item._id ?? item.book_id ?? '',
  title: item.title ?? '',
  author: item.author ?? '',
  authors: item.author ? [item.author] : [],
  coverImageUrl: item.coverImageUrl ?? '',
  availableCopies: item.availableCopies ?? 0,
  averageRating: item.matchScore ?? item.score ?? 0,
  matchScore: item.matchScore ?? item.score ?? 0,
  genre: item.genre ?? '',
  category: item.genre ?? '',
  reason: item.reason ?? '',
});

export const memberApi = {
  getFeaturedBooks: async () => {
    const { data } = await API.get('/api/v1/books/featured');
    return {
      ...data,
      data: (data.data?.topRated ?? []).map(mapBookSummary),
    };
  },

  searchBooks: async (query = '') => {
    const { data } = await API.get('/api/v1/books', {
      params: query ? { q: query, page: 1, size: 0 } : { page: 1, size: 0 },
    });
    return {
      ...data,
      data: (data.data?.books ?? []).map(mapBookSummary),
      total: data.data?.total || 0,
    };
  },

  getBookDetail: async (id) => {
    const { data } = await API.get(`/api/v1/books/${id}`);
    return {
      ...data,
      data: mapBookDetail(data.data),
    };
  },

  getAIRecommendations: async () => {
    const [aiResult, mlResult] = await Promise.allSettled([
      API.get('/api/v1/member/ai/recommendations', { params: { limit: 15 } }),
      memberApi.getMLRecommendations(5, 10),
    ]);

    const aiData = aiResult.status === 'fulfilled' ? aiResult.value.data : {};
    const response = aiData.data ?? {};
    const topRecommendations = (response.recommendations ?? []).map(mapRecommendation);
    const similarBooks = (response.similarBooks ?? []).map(mapRecommendation);

    if (topRecommendations.length > 0 || similarBooks.length > 0) {
      return {
        ...aiData,
        data: {
          topRecommendations,
          similarBooks,
          basedOn: response.basedOn ?? '',
        },
      };
    }

    const mlData = mlResult.status === 'fulfilled' ? mlResult.value.data : {};
    return {
      success: true,
      message: 'Recommendations fetched',
      data: {
        topRecommendations: mlData.top ?? [],
        similarBooks: mlData.similar ?? [],
        basedOn: mlData.fallbackToPopular ? 'Trending library activity' : mlData.modelVersion ?? '',
      },
    };
  },

  getMLRecommendations: async (topN = 5, similarN = 10) => {
    const { data } = await API.get('/api/v1/member/recommendations/sections', {
      params: { topN, similarN },
    });
    const response = data.data ?? {};
    return {
      ...data,
      data: {
        top: (response.top ?? []).map(mapRecommendation),
        similar: (response.similar ?? []).map(mapRecommendation),
        modelVersion: response.modelVersion ?? 'unknown',
        branch: response.branch ?? 'control',
        servedFromCache: response.servedFromCache ?? false,
        fallbackToPopular: response.fallbackToPopular ?? false,
        latencyMs: response.latencyMs ?? 0,
      },
    };
  },

  submitMLFeedback: async (bookId, action = 'click') => {
    const { data } = await API.post('/api/v1/member/recommendations/feedback', null, {
      params: { bookId, action },
    });
    return data;
  },

  getReadingPath: async () => {
    const { data } = await API.get('/api/v1/member/ai/recommendations', { params: { limit: 4 } });
    return {
      ...data,
      data: (data.data?.recommendations ?? []).map((item, index) => ({
        id: item.bookId ?? `step-${index + 1}`,
        title: item.title ?? `Recommended Step ${index + 1}`,
        status: index === 0 ? 'Active' : index === 1 ? 'Suggested' : 'Future',
        detail: item.reason ?? 'Recommended based on your reading history and wishlist.',
      })),
    };
  },

  getLoans: async () => {
    const { data } = await API.get('/api/v1/member/loans');
    return {
      ...data,
      data: (data.data?.loans ?? []).map((loan) => ({
        ...loan,
        currentFine: loan.fineAmount ?? 0,
      })),
    };
  },

  getHistory: async (page = 1, genre) => {
    const { data } = await API.get("/api/v1/member/history", { params: { page, genre } });
    return { ...data, data: { history: (data.data?.history ?? []).map(item => ({ loanId: item.loanId ?? "", bookTitle: item.bookTitle ?? "", bookCoverImageUrl: item.bookCoverImageUrl ?? "", genre: item.genre ?? "", returnedAt: item.returnedAt ?? null, daysHeld: item.daysHeld ?? 0, fineAmount: item.fineAmount ?? 0 })), total: data.data?.total ?? 0 } };
  },


  getReservations: async () => {
    const { data } = await API.get('/api/v1/member/reservations');
    return {
      ...data,
      data: (data.data?.reservations ?? []).map((res) => ({
        reservationId: res.reservationId ?? res.id ?? '',
        bookId: res.bookId ?? '',
        bookTitle: res.bookTitle ?? '',
        bookCoverImageUrl: res.bookCoverImageUrl ?? '',
        queuePosition: res.queuePosition ?? 0,
        status: res.status ?? 'Pending',
        reservedAt: res.reservedAt ?? null,
        expiresAt: res.expiresAt ?? null,
      })),
    };
  },

  reserveBook: async (bookId) => {
    const { data } = await API.post('/api/v1/member/reservations', { bookId });
    return data;
  },

  cancelReservation: async (reservationId) => {
    const { data } = await API.delete(`/api/v1/member/reservations/${reservationId}`);
    return data;
  },

  getWishlist: async () => {
    const { data } = await API.get('/api/v1/member/wishlist');
    return {
      ...data,
      data: (data.data?.wishlist ?? []).map((item) => ({
        bookId: item.bookId ?? '',
        title: item.bookTitle ?? '',
        author: item.author ?? '',
        authors: item.author ? [item.author] : [],
        coverImageUrl: item.bookCoverImageUrl ?? '',
        availableCopies: item.availableCopies ?? 0,
        averageRating: item.averageRating ?? 0,
        genre: item.genre ?? '',
        category: item.genre ?? '',
        isbn: item.isbn ?? '',
        isWishlisted: true,
      })),
    };
  },

  toggleWishlist: async (bookId) => {
    try {
      const { data } = await API.post('/api/v1/member/wishlist', { bookId, addedFrom: 'browse', notifyOnAvailable: false });
      return data;
    } catch (error) {
      if (error.response?.status === 409) {
        const { data } = await API.delete(`/api/v1/member/wishlist/${bookId}`);
        return data;
      }
      throw error;
    }
  },

  getStats: async () => {
    const [profileResponse, loansResponse, reservationsResponse] = await Promise.all([
      API.get('/api/v1/member/profile'),
      API.get('/api/v1/member/loans'),
      API.get('/api/v1/member/reservations'),
    ]);

    const profile = profileResponse.data?.data ?? {};
    const loans = loansResponse.data?.data?.loans ?? [];
    const reservations = reservationsResponse.data?.data?.reservations ?? [];

    return {
      success: true,
      message: 'Member stats fetched',
      data: {
        booksRead: profile.analytics?.totalReturned ?? 0,
        currentLoans: loans.length,
        activeReservations: reservations.length,
        finesDue: loans.reduce((sum, loan) => sum + (loan.fineAmount ?? 0), 0),
      },
    };
  },

  updateProfile: async (payload) => {
    const { data } = await API.put('/api/v1/member/profile', payload);
    return data;
  },

  sendChatMessage: async (messageOrPayload) => {
    const payload = typeof messageOrPayload === 'string'
      ? { message: messageOrPayload }
      : messageOrPayload;
    const { data } = await API.post('/api/v1/member/chatbot/message', payload);
    return {
      ...data,
      data: {
        ...data.data,
        response: data.data?.assistantReply ?? '',
      },
    };
  },

  // Profile
  getProfile: async () => {
    const { data } = await API.get('/api/v1/member/profile');
    return data;
  },

  // Notifications
  getNotifications: async () => {
    const { data } = await API.get('/api/v1/member/notifications');
    return data;
  },

  markNotificationAsRead: async (notificationId) => {
    const { data } = await API.patch(`/api/v1/member/notifications/${notificationId}/read`);
    return data;
  },

  markAllNotificationsAsRead: async () => {
    const { data } = await API.patch('/api/v1/member/notifications/read-all');
    return data;
  },

  // Reviews - Submit rating and feedback for returned books
  submitReview: async (reviewData) => {
    const { data } = await API.post('/api/v1/member/reviews', reviewData);
    return data;
  },

  getReviews: async () => {
    const { data } = await API.get('/api/v1/member/reviews');
    return data;
  },

  // Reading Goal
  getReadingGoal: async () => {
    const { data } = await API.get('/api/v1/member/reading-goal');
    return data;
  },

  setReadingGoal: async (targetBooks, year) => {
    const { data } = await API.post('/api/v1/member/reading-goal', { targetBooks, year });
    return data;
  },

  deleteReadingGoal: async () => {
    const { data } = await API.delete('/api/v1/member/reading-goal');
    return data;
  },

  // Book Queues
  getQueues: async () => {
    const { data } = await API.get('/api/v1/member/queues');
    return data;
  },

  createQueue: async (queueName, bookIds) => {
    const { data } = await API.post('/api/v1/member/queues', bookIds, { params: { queueName } });
    return data;
  },

  updateQueue: async (queueId, bookIds) => {
    const { data } = await API.put(`/api/v1/member/queues/${queueId}`, bookIds);
    return data;
  },

  deleteQueue: async (queueId) => {
    const { data } = await API.delete(`/api/v1/member/queues/${queueId}`);
    return data;
  },

  markQueueBookCompleted: async (queueId, bookId) => {
    const { data } = await API.post(`/api/v1/member/queues/${queueId}/books/${bookId}/complete`);
    return data;
  },

  startQueue: async (queueId) => {
    const { data } = await API.post(`/api/v1/member/queues/${queueId}/start`);
    return data;
  },

  // Book Milestones
  getMilestones: async () => {
    const { data } = await API.get('/api/v1/member/milestones');
    return data;
  },

  createMilestone: async (bookId, targetDate, notes) => {
    const { data } = await API.post('/api/v1/member/milestones', null, {
      params: { bookId, targetDate, notes }
    });
    return data;
  },

  updateMilestone: async (milestoneId, targetDate, notes) => {
    const { data } = await API.put(`/api/v1/member/milestones/${milestoneId}`, null, {
      params: { targetDate, notes }
    });
    return data;
  },

  deleteMilestone: async (milestoneId) => {
    const { data } = await API.delete(`/api/v1/member/milestones/${milestoneId}`);
    return data;
  },

  completeMilestone: async (milestoneId) => {
    const { data } = await API.post(`/api/v1/member/milestones/${milestoneId}/complete`);
    return data;
  },

  updateMilestoneProgress: async (milestoneId, progressPercent) => {
    const { data } = await API.patch(`/api/v1/member/milestones/${milestoneId}/progress`, null, {
      params: { progressPercent }
    });
    return data;
  },

  // Book Requests (Acquisition Requests)
  getBookRequests: async () => {
    const { data } = await API.get('/api/v1/member/book-requests');
    return data;
  },

  createBookRequest: async (requestData) => {
    const { data } = await API.post('/api/v1/member/book-requests', requestData);
    return data;
  },

  cancelBookRequest: async (requestId) => {
    const { data } = await API.delete(`/api/v1/member/book-requests/${requestId}`);
    return data;
  },

  // Analytics & Neural Controls
  getAnalytics: async () => {
    const { data } = await API.get('/api/v1/member/analytics');
    return data;
  },

  updateNeuralControls: async (controls) => {
    const { data } = await API.put('/api/v1/member/neural-controls', null, {
      params: controls
    });
    return data;
  },
};
