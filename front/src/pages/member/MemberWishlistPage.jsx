import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import BookCard from '../../components/shared/BookCard';
import { 
  Heart, Sparkles, Loader2, 
  Trash2, BookOpen, ArrowRight
} from 'lucide-react';
import { Link } from 'react-router-dom';

const MemberWishlistPage = () => {
  const queryClient = useQueryClient();
  
  const { data: wishlistData, isLoading } = useQuery({
    queryKey: ['memberWishlist'],
    queryFn: memberApi.getWishlist
  });

  const wishlist = wishlistData?.data || [];

  return (
    <PageTransition>
      <div className="p-8 space-y-12">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <h1 className="text-4xl font-display font-bold text-gray-900">Your Reading Queue</h1>
            <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">Future literary acquisitions ({wishlist.length})</p>
          </div>
          <div className="flex items-center space-x-3 text-indigo-600 text-[10px] font-bold uppercase tracking-widest bg-indigo-50 px-4 py-2 rounded-sm border border-indigo-100">
             <Sparkles size={14} className="animate-pulse" />
             <span>AI Analysis: 3 items in your queue are currently shelf-ready</span>
          </div>
        </div>

        {isLoading ? (
          <div className="py-24 text-center">
            <Loader2 className="animate-spin text-indigo-500 mx-auto" size={48} />
          </div>
        ) : wishlist.length === 0 ? (
          <div className="bg-white border border-gray-100 p-20 text-center space-y-6 rounded-sm shadow-xl shadow-indigo-50">
             <div className="w-20 h-20 bg-indigo-50 text-indigo-400 rounded-full flex items-center justify-center mx-auto">
                <Heart size={40} />
             </div>
             <div>
               <h3 className="text-xl font-display font-bold text-gray-900">Wishlist is Silent</h3>
               <p className="text-gray-500 text-sm mt-2">No future reads have been synthesized yet. Explore the repository and click the heart icon to save.</p>
             </div>
             <Link to="/member/browse" className="inline-block px-10 py-4 bg-indigo-600 text-white font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-indigo-700 transition-all">
                Start Exploring
             </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-8">
            {wishlist.map((book) => (
               <div key={book.bookId} className="relative group">
                  <BookCard book={book} />
                  <button 
                    onClick={() => memberApi.toggleWishlist(book.bookId).then(() => queryClient.invalidateQueries(['memberWishlist']))}
                    className="absolute top-4 right-4 p-2 bg-white/90 backdrop-blur text-red-500 rounded-sm shadow-lg opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 size={16} />
                  </button>
               </div>
            ))}
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberWishlistPage;
