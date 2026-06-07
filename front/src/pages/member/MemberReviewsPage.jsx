import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import { Quote, Star, Calendar, BookOpen, MessageSquare, Loader2 } from 'lucide-react';

const MemberReviewsPage = () => {
  const { data: reviewsResponse, isLoading, error } = useQuery({
    queryKey: ['memberReviews'],
    queryFn: memberApi.getReviews,
  });

  const reviews = reviewsResponse?.data?.reviews || [];

  const renderStars = (rating) => {
    return (
      <div className="flex space-x-1">
        {[1, 2, 3, 4, 5].map((star) => (
          <Star
            key={star}
            size={14}
            className={`${
              star <= rating ? 'text-amber-400 fill-amber-400' : 'text-gray-200'
            }`}
          />
        ))}
      </div>
    );
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8 max-w-6xl mx-auto">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-display font-bold text-indigo-950">Literary Contributions</h1>
            <p className="text-gray-500 mt-1">Your personal archive of book reflections and feedback.</p>
          </div>
          <div className="px-4 py-2 bg-indigo-50 text-indigo-700 rounded-full text-sm font-bold border border-indigo-100">
            {reviews.length} Reflections
          </div>
        </div>

        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-24 space-y-4">
            <Loader2 className="animate-spin text-indigo-600" size={32} />
            <p className="text-indigo-400 font-medium font-mono text-xs uppercase tracking-widest">Accessing repository...</p>
          </div>
        ) : error ? (
          <div className="bg-red-50 border border-red-100 p-8 rounded-2xl text-center">
            <p className="text-red-800 font-medium">Failed to retrieve reviews.</p>
            <p className="text-red-600/70 text-sm mt-1">Our archives are temporarily unreachable. Please try again later.</p>
          </div>
        ) : reviews.length === 0 ? (
          <div className="bg-white/50 backdrop-blur-sm border border-indigo-50 p-24 text-center rounded-3xl shadow-sm">
            <div className="relative inline-block mb-6">
              <div className="absolute inset-0 bg-indigo-500 blur-3xl opacity-10 animate-pulse" />
              <Quote size={64} className="relative text-indigo-200" />
            </div>
            <h3 className="text-xl font-bold text-indigo-900 mb-2">Pen your first reflection</h3>
            <p className="text-gray-500 max-w-md mx-auto">
              Return a book to receive a review prompt. Your insights help the community discover their next favorite read.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {reviews.map((review) => (
              <div 
                key={review.id} 
                className="group bg-white border border-gray-100 rounded-3xl p-6 hover:shadow-xl hover:shadow-indigo-500/5 transition-all duration-500 flex flex-col space-y-4"
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-4">
                    <div className="w-12 h-16 bg-indigo-50 rounded-lg overflow-hidden flex-shrink-0 border border-indigo-100">
                      {/* Fallback for cover image if missing in review object */}
                      <div className="w-full h-full flex items-center justify-center">
                        <BookOpen size={20} className="text-indigo-200" />
                      </div>
                    </div>
                    <div>
                      <h4 className="font-bold text-indigo-950 group-hover:text-indigo-600 transition-colors line-clamp-1">
                        {review.bookTitle || 'Unknown Title'}
                      </h4>
                      <p className="text-xs font-medium text-indigo-400/80 mt-0.5">Contributor: You</p>
                    </div>
                  </div>
                  <div className="flex flex-col items-end space-y-2">
                    {renderStars(review.rating)}
                    <span className="text-[10px] uppercase tracking-tighter font-mono text-gray-400 flex items-center">
                      <Calendar size={10} className="mr-1" />
                      {new Date(review.createdAt).toLocaleDateString()}
                    </span>
                  </div>
                </div>

                <div className="relative p-5 bg-indigo-50/30 rounded-2xl border border-indigo-50 flex-grow">
                  <MessageSquare size={16} className="absolute -top-2 -right-2 text-indigo-200 bg-white rounded-full p-0.5" />
                  <p className="text-sm text-gray-600 italic leading-relaxed">
                    "{review.reviewText || review.feedbackText || 'No commentary provided.'}"
                  </p>
                </div>

                {review.sentimentScore !== undefined && (
                  <div className="flex items-center justify-between text-[10px] font-mono uppercase tracking-widest text-indigo-300">
                    <span>Pulse Analysis</span>
                    <div className="flex items-center space-x-2">
                      <div className="w-24 h-1 bg-gray-100 rounded-full overflow-hidden">
                        <div 
                          className={`h-full transition-all duration-1000 ${
                            review.sentimentScore > 0 ? 'bg-emerald-400' : review.sentimentScore < 0 ? 'bg-rose-400' : 'bg-gray-400'
                          }`}
                          style={{ width: `${Math.abs(review.sentimentScore) * 100}%`, marginLeft: review.sentimentScore < 0 ? 'auto' : '0' }}
                        />
                      </div>
                      <span>{review.sentimentScore > 0 ? 'Positive' : review.sentimentScore < 0 ? 'Critical' : 'Neutral'}</span>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberReviewsPage;
