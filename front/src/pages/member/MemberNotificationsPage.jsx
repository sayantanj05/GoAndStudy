import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageTransition from '../../components/shared/PageTransition';
import { Bell, Star, Send, Loader2, Cake, Sparkles } from 'lucide-react';
import { memberApi } from '../../api/member.api';
import { useAuthStore } from '../../store/authStore';
import LoadingSpinner from '../../components/shared/LoadingSpinner';
import API from '../../api/axios';
import { toast } from 'react-hot-toast';
import useRealtime from '../../hooks/useRealtime';

const MemberNotificationsPage = () => {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const [ratings, setRatings] = useState({}); // Track ratings by notification ID
  const [feedbacks, setFeedbacks] = useState({}); // Track feedbacks by notification ID

  const { data: notificationsData, isLoading, refetch } = useQuery({
    queryKey: ['memberNotifications', user?.userId],
    queryFn: memberApi.getNotifications,
    enabled: !!user?.userId,
  });

  // Fetch member profile to get dateOfBirth for birthday check
  const { data: profileData } = useQuery({
    queryKey: ['memberProfile', user?.userId],
    queryFn: memberApi.getProfile,
    enabled: !!user?.userId,
  });

  const profile = profileData?.data;

  // Check if today is the member's birthday
  const isBirthdayToday = () => {
    if (!profile?.dateOfBirth) return false;
    const today = new Date();
    const birthDate = new Date(profile.dateOfBirth);
    return today.getDate() === birthDate.getDate() && today.getMonth() === birthDate.getMonth();
  };

  const showBirthdayCard = isBirthdayToday();

  useRealtime('NOTIFICATION', () => {
    refetch();
    toast('New notification received', { icon: '🔔', position: 'top-right' });
  });

  const submitReviewMutation = useMutation({
    mutationFn: memberApi.submitReview,
    onSuccess: (response) => {
      toast.success('Review submitted successfully!');
      // Clear the stored rating and feedback
      setRatings(prev => {
        const updated = { ...prev };
        delete updated[response.data?.notificationId];
        return updated;
      });
      setFeedbacks(prev => {
        const updated = { ...prev };
        delete updated[response.data?.notificationId];
        return updated;
      });
      queryClient.invalidateQueries({ queryKey: ['memberNotifications'] });
    },
    onError: (error) => toast.error(error.response?.data?.message || 'Failed to submit review'),
  });

  const notifications = notificationsData?.data?.notifications || [];

  // Mark all unread notifications as read when page loads
  useEffect(() => {
    const markAllAsRead = async () => {
      const unreadNotifications = notifications.filter(n => n.status === 'Sent');
      
      if (unreadNotifications.length > 0) {
        // Mark each unread notification as read
        await Promise.all(
          unreadNotifications.map(notification => 
            memberApi.markNotificationAsRead(notification.id)
          )
        );
        
        // Invalidate notifications query to refresh data
        queryClient.invalidateQueries({ queryKey: ['memberNotifications', user?.userId] });
        queryClient.invalidateQueries({ queryKey: ['notifications', 'member', user?.userId] });
      }
    };

    if (notifications.length > 0 && !isLoading) {
      markAllAsRead();
    }
  }, [notifications, isLoading, user?.userId, queryClient]);

  const handleRatingChange = (notificationId, rating) => {
    setRatings(prev => ({ ...prev, [notificationId]: rating }));
  };

  const handleFeedbackChange = (notificationId, feedback) => {
    setFeedbacks(prev => ({ ...prev, [notificationId]: feedback }));
  };

  const handleSubmitReview = (notification) => {
    const rating = ratings[notification.id] || 5;
    const feedback = feedbacks[notification.id] || '';
    
    submitReviewMutation.mutate({
      notificationId: notification.id,
      loanId: notification.relatedLoanId,
      bookId: notification.relatedBookId,
      rating: rating,
      reviewText: feedback, // Send as reviewText for sentiment analysis
      feedbackText: feedback, // Send as feedbackText for secondary storage
    });
  };

  if (isLoading) return (
    <PageTransition>
      <div className="p-8">
        <LoadingSpinner />
      </div>
    </PageTransition>
  );

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex items-center space-x-4">
          <h1 className="text-3xl font-display font-bold">Notifications</h1>
          <div className="flex items-center space-x-1 text-sm bg-blue-100 px-3 py-1 rounded-full text-blue-800">
            <Bell size={14} />
            <span>{notifications.filter(n => n.status === 'Sent').length}</span>
          </div>
        </div>

        {/* Birthday Wish Card */}
        {showBirthdayCard && (
          <div className="bg-gradient-to-r from-pink-500 via-purple-500 to-indigo-500 rounded-xl p-6 text-white shadow-lg animate-pulse">
            <div className="flex items-start space-x-4">
              <div className="flex-shrink-0 w-14 h-14 bg-white/20 rounded-xl flex items-center justify-center backdrop-blur-sm">
                <Cake size={28} className="text-white" />
              </div>
              <div className="flex-1">
                <div className="flex items-center space-x-2 mb-2">
                  <Sparkles size={16} className="text-yellow-300" />
                  <h3 className="font-bold text-xl">Happy Birthday, {user?.name?.split(' ')[0] || 'Reader'}!</h3>
                  <Sparkles size={16} className="text-yellow-300" />
                </div>
                <p className="text-white/90">
                  Wishing you a wonderful day filled with joy, laughter, and great books! 
                  May this year bring you countless amazing stories and adventures.
                </p>
                <div className="mt-3 text-sm text-white/70 font-medium">
                  🎉 Celebrate your special day with us! 🎂
                </div>
              </div>
            </div>
          </div>
        )}

        {notifications.length === 0 ? (
          <div className="bg-white border border-gray-100 p-12 text-center text-gray-400 rounded-xl">
            <Bell size={48} className="mx-auto mb-4 opacity-20" />
            <p>No new notifications.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {notifications.map((notification) => (
              <div key={notification.id} className="bg-white border border-gray-200 rounded-xl p-6 hover:shadow-md transition-all">
                <div className="flex items-start space-x-4">
                  <div className="flex-shrink-0 w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center">
                    <Bell size={20} className="text-white" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-2 mb-2">
                      <h3 className="font-semibold text-lg">{notification.title}</h3>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        notification.status === 'Read' ? 'bg-gray-100 text-gray-600' : 'bg-blue-100 text-blue-800'
                      }`}>
                        {notification.status}
                      </span>
                    </div>
                    <p className="text-gray-700 mb-4">{notification.message}</p>

                    {(notification.type === 'RATE_BOOK_RETURNED' || notification.type === 'REVIEW') && !notification.alreadyReviewed && (
                      <div className="mt-4 space-y-6 p-6 bg-gradient-to-br from-indigo-50/50 via-white to-purple-50/50 rounded-2xl border border-indigo-100 shadow-sm">
                        <div className="flex items-center space-x-3 text-indigo-900">
                          <Star className="text-amber-500 fill-amber-500" size={20} />
                          <span className="font-bold tracking-tight">Personal Reading Reflection</span>
                        </div>
                        
                        <div className="space-y-4">
                          <div>
                            <span className="text-xs font-mono uppercase tracking-widest text-indigo-400">Your Experience</span>
                            <div className="flex items-center space-x-2 mt-2">
                              {[1,2,3,4,5].map((star) => (
                                <button
                                  key={star}
                                  type="button"
                                  className={`text-4xl transition-all duration-300 transform hover:scale-110 ${
                                    (ratings[notification.id] || 0) >= star 
                                      ? 'text-amber-400 drop-shadow-sm' 
                                      : 'text-gray-200 hover:text-amber-200'
                                  }`}
                                  onClick={() => handleRatingChange(notification.id, star)}
                                >
                                  ★
                                </button>
                              ))}
                              {ratings[notification.id] && (
                                <span className="ml-4 px-3 py-1 bg-amber-100 text-amber-700 text-xs font-bold rounded-full">
                                  {ratings[notification.id]} / 5
                                </span>
                              )}
                            </div>
                          </div>
  
                          <div className="relative">
                            <textarea
                              placeholder="Any thoughts on this title? (Optional)"
                              rows={4}
                              className="w-full p-4 bg-white/50 border border-indigo-100 rounded-xl focus:ring-4 focus:ring-indigo-500/10 focus:border-indigo-400 focus:outline-none transition-all resize-none text-sm placeholder:text-gray-400"
                              value={feedbacks[notification.id] || ''}
                              onChange={(e) => handleFeedbackChange(notification.id, e.target.value)}
                            />
                          </div>
                        </div>

                        <div className="flex items-center justify-between pt-2">
                          <button
                            className="text-xs font-bold uppercase tracking-widest text-indigo-400 hover:text-indigo-600 transition-colors"
                            onClick={() => refetch()}
                          >
                            Skip for now
                          </button>
                          <button
                            className="group relative px-8 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition-all flex items-center space-x-3 overflow-hidden"
                            onClick={() => handleSubmitReview(notification)}
                            disabled={submitReviewMutation.isPending || !ratings[notification.id]}
                          >
                            <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/10 to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
                            {submitReviewMutation.isPending ? (
                              <Loader2 className="animate-spin" size={18} />
                            ) : (
                              <Send className="group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" size={18} />
                            )}
                            <span className="uppercase tracking-widest text-xs">Submit Feedback</span>
                          </button>
                        </div>
                      </div>
                    )}

                    <div className="text-xs text-gray-500 mt-4">
                      {new Date(notification.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
        <div className="pt-8 border-t">
          <button 
            className="text-sm text-blue-600 hover:text-blue-700 font-medium"
            onClick={() => refetch()}
          >
            Refresh Notifications
          </button>
        </div>
      </div>
    </PageTransition>
  );
};

export default MemberNotificationsPage;
