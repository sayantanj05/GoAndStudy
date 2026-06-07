import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  ArrowLeft, Heart, Bookmark, 
  MapPin, Clock, Loader2,
  ShieldCheck, Share2, BookOpen, Calendar, Star
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const MemberBookDetailPage = () => {
  const { id } = useParams();
  const queryClient = useQueryClient();

  React.useEffect(() => {
    queryClient.removeQueries(['bookDetail', id]);
  }, [id, queryClient]);

  const { data: bookData, isLoading, error, isError } = useQuery({
    queryKey: ['bookDetail', id],
    queryFn: () => memberApi.getBookDetail(id),
    enabled: Boolean(id),
    staleTime: 0,
    cacheTime: 0,
    retry: 1,
    retryDelay: 500
  });

  const book = bookData?.data;
  const resolvedBookId = book?.bookId || id;

  const reserveMutation = useMutation({
    mutationFn: () => memberApi.reserveBook(resolvedBookId),
    onSuccess: (data) => {
      queryClient.invalidateQueries(['bookDetail', id]);
      queryClient.invalidateQueries(['memberReservations']);
      toast.success(`Reservation confirmed! You are #${data.data?.queuePosition || 'X'} in queue.`);
    },
    onError: (error) => {
      const message = error?.response?.data?.message || 'Failed to reserve book';
      toast.error(message);
    }
  });

  const toggleWishlist = useMutation({
    mutationFn: () => memberApi.toggleWishlist(resolvedBookId),
    onSuccess: () => {
      queryClient.invalidateQueries(['bookDetail', id]);
      toast.success('Wishlist updated');
    }
  });

  if (isLoading) return (
    <div className="min-h-screen flex items-center justify-center">
      <Loader2 className="animate-spin text-indigo-500" size={48} />
    </div>
  );

  if (isError || !book) {
    const errorMessage = error?.response?.data?.message || error?.message || 'Book details could not be loaded';
    const errorStatus = error?.response?.status || 'Unknown';
    
    return (
      <div className="min-h-screen flex flex-col items-center justify-center p-8 bg-gradient-to-b from-amber-50 to-white">
        <div className="bg-white border-2 border-red-200 rounded-lg p-8 max-w-md text-center shadow-lg">
          <p className="text-red-600 font-bold text-lg mb-2">Asset Not Found</p>
          <p className="text-gray-600 text-sm mb-4">{errorMessage}</p>
          {errorStatus !== 'Unknown' && (
            <p className="text-gray-400 text-xs mb-6 font-mono">Error Code: {errorStatus}</p>
          )}
          <p className="text-gray-500 text-sm mb-6">
            The book you're looking for may have been removed or the ID might be invalid.
          </p>
          <Link 
            to="/member/browse" 
            className="inline-block px-6 py-3 bg-indigo-600 text-white font-bold rounded-lg hover:bg-indigo-700 transition-colors"
          >
            Return to Repository
          </Link>
        </div>
      </div>
    );
  }

  const StarRating = ({ rating }) => (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <Star
          key={star}
          size={16}
          className={star <= rating ? "text-amber-400 fill-amber-400" : "text-gray-300"}
        />
      ))}
    </div>
  );

  return (
    <PageTransition>
      <div className="relative min-h-screen">
        <div className="absolute top-0 left-0 w-full h-[60vh] overflow-hidden pointer-events-none opacity-20">
           <div 
             className="w-full h-full bg-cover bg-center blur-3xl scale-110"
             style={{ backgroundImage: `url(https://picsum.photos/seed/${book.isbn || book.bookId}/800/1200.jpg)` }}
           />
        </div>

        <div className="relative z-10 p-8 pt-12 max-w-7xl mx-auto space-y-12">
          <Link to="/member/browse" className="inline-flex items-center text-indigo-600 font-bold text-xs uppercase tracking-widest hover:translate-x-[-4px] transition-transform">
            <ArrowLeft size={16} className="mr-2" />
            Back to Discovery
          </Link>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-16">
            <div className="lg:col-span-4 space-y-8">
              <div className="aspect-[3/4] bg-white rounded-sm shadow-2xl relative overflow-hidden group">
                 <img 
                   src={`https://picsum.photos/seed/${book.isbn || book.bookId}/400/600.jpg`} 
                   alt={book.title}
                   className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                   onError={(e) => e.target.src = 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=2730&auto=format&fit=crop'}
                 />
                 <div className="absolute top-4 right-4 flex flex-col space-y-2">
                    <button 
                      onClick={() => toggleWishlist.mutate()}
                      className="w-10 h-10 bg-white/90 backdrop-blur text-indigo-600 rounded-sm flex items-center justify-center hover:bg-white transition-all shadow-lg"
                    >
                      <Heart size={20} fill={book.isWishlisted ? 'currentColor' : 'none'} />
                    </button>
                    <button className="w-10 h-10 bg-white/90 backdrop-blur text-indigo-600 rounded-sm flex items-center justify-center hover:bg-white transition-all shadow-lg">
                      <Share2 size={20} />
                    </button>
                 </div>
              </div>
              
              {/* Member Reviews Section */}
              {book.reviews && book.reviews.length > 0 && (
                <div className="bg-white border border-gray-100 p-6 rounded-sm space-y-4">
                  <h3 className="text-lg font-display font-bold uppercase tracking-[0.2em] text-gray-600">Member Reviews</h3>
                  <div className="space-y-4">
                    {book.reviews.map((review, idx) => (
<div key={review.id || idx} className="border-b border-gray-100 last:border-0 pb-4 last:pb-0">
                         <div className="flex items-center justify-between mb-2">
                           <StarRating rating={review.rating} />
                           <span className="text-xs text-gray-400">{review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ''}</span>
                         </div>
                         <p className="text-sm text-gray-600 leading-relaxed mb-1">
                           {review.reviewText || "No review text available"}
                         </p>
                         <p className="text-xs text-gray-500 font-medium">
                           by {review.memberName || "Anonymous Member"}
                         </p>
                       </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
 
            <div className="lg:col-span-8 space-y-12">
               <div className="space-y-4">
                  <div className="flex items-center space-x-3 flex-wrap gap-2">
                    {(book.categoryNames?.length > 0 ? book.categoryNames : (book.category ? [book.category] : [])).map((cat, idx) => (
                      <Badge key={idx} variant="indigo">{cat}</Badge>
                    ))}
                    <Badge variant={book.availableCopies > 0 ? 'teal' : 'red'}>
                      {book.availableCopies > 0 ? `${book.availableCopies} Copies Ready` : 'Reservations Only'}
                    </Badge>
                  </div>
                  <h1 className="text-6xl font-display font-bold text-gray-900 leading-tight">
                    {book.title}
                  </h1>
                  <p className="text-2xl text-gray-500 font-medium">by {book.authors && book.authors.length > 0 ? book.authors.join(', ') : book.author}</p>
               </div>

               <div className="grid grid-cols-3 gap-8 py-8 border-y border-gray-100 uppercase font-mono">
                  <div className="space-y-2">
                    <span className="text-[10px] text-gray-400">Locator</span>
                    <p className="flex items-center text-sm font-bold text-gray-900">
                       <MapPin size={14} className="mr-2 text-indigo-500" /> Section {book.location || 'A-12'}
                    </p>
                  </div>
                  <div className="space-y-2">
                    <span className="text-[10px] text-gray-400">Digital Asset</span>
                    <p className="flex items-center text-sm font-bold text-gray-900">
                       <ShieldCheck size={14} className="mr-2 text-teal-500" /> Hardcopy
                    </p>
                  </div>
                  <div className="space-y-2">
                    <span className="text-[10px] text-gray-400">Turnaround</span>
                    <p className="flex items-center text-sm font-bold text-gray-900">
                       <Clock size={14} className="mr-2 text-amber-500" /> 14 Days
                    </p>
                  </div>
               </div>

               <div className="space-y-6">
                  <h3 className="text-lg font-display font-bold uppercase tracking-[0.2em] text-gray-400">Abstract</h3>
                  <p className="text-gray-600 leading-relaxed text-lg max-w-3xl">
                    {book.description || "The Smart Library System has not yet synthesized a summary for this asset. However, our neural network identifies its primary themes as 'Persistence', 'Metaphysical Inquiry', and 'Structuralism'."}
                  </p>
               </div>

               <div className="flex items-center space-x-6 pt-8">
                  {book.availableCopies > 0 ? (
                    <button 
                      className="px-12 py-5 bg-teal-600 text-white rounded-sm font-bold uppercase tracking-[0.2em] text-xs hover:bg-teal-700 transition-all shadow-xl shadow-teal-100 flex items-center"
                      onClick={() => toast.success('Please visit the library counter to borrow this book')}
                    >
                      <BookOpen size={18} className="mr-3" />
                      Borrow Now
                    </button>
                  ) : (
                    <button 
                      onClick={() => reserveMutation.mutate()}
                      disabled={reserveMutation.isPending}
                      className="px-12 py-5 bg-indigo-600 text-white rounded-sm font-bold uppercase tracking-[0.2em] text-xs hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-100 disabled:opacity-50 flex items-center"
                    >
                      {reserveMutation.isPending ? <Loader2 className="animate-spin mr-2" size={18} /> : (
                        <>
                          <Bookmark size={18} className="mr-3" />
                          Reserve Book
                        </>
                      )}
                    </button>
                  )}
                  
                  {book.availableCopies <= 0 && (
                    <div className="text-sm text-gray-500 flex items-center">
                      <Calendar size={16} className="mr-2 text-amber-500" />
                      Join the queue - you'll be notified when available
                    </div>
                  )}
               </div>
            </div>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default MemberBookDetailPage;