import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { History, Calendar, Loader2, Clock, BookOpen } from 'lucide-react';
import { format } from 'date-fns';

const MemberHistoryPage = () => {
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedGenre, setSelectedGenre] = useState(null);

  const { data: historyData, isLoading } = useQuery({
    queryKey: ['memberHistory', currentPage, selectedGenre],
    queryFn: () => memberApi.getHistory(currentPage, selectedGenre)
  });

  const history = historyData?.data?.history || [];
  const total = historyData?.data?.total || 0;
  const totalPages = Math.ceil(total / 20);  // Backend uses 20 items per page

  // Extract unique genres for filtering
  const genres = [...new Set(history.map(item => item.genre).filter(Boolean))];

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-4xl font-display font-bold text-gray-900">Reading History</h1>
            <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">Tracking {total} completed reads</p>
          </div>
        </div>

        {/* Genre Filter */}
        {genres.length > 0 && (
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => setSelectedGenre(null)}
              className={`px-4 py-2 rounded-sm font-bold uppercase tracking-widest text-xs transition-all ${
                selectedGenre === null
                  ? 'bg-indigo-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              All Genres
            </button>
            {genres.map(genre => (
              <button
                key={genre}
                onClick={() => setSelectedGenre(genre)}
                className={`px-4 py-2 rounded-sm font-bold uppercase tracking-widest text-xs transition-all ${
                  selectedGenre === genre
                    ? 'bg-indigo-600 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                {genre}
              </button>
            ))}
          </div>
        )}

        {isLoading ? (
          <div className="py-24 text-center">
            <Loader2 className="animate-spin text-indigo-500 mx-auto" size={48} />
          </div>
        ) : history.length === 0 ? (
          <div className="bg-white border border-gray-100 p-20 text-center space-y-6 rounded-sm shadow-xl shadow-indigo-50">
            <div className="w-20 h-20 bg-gray-50 text-gray-300 rounded-full flex items-center justify-center mx-auto">
              <BookOpen size={40} />
            </div>
            <div>
              <h3 className="text-xl font-display font-bold text-gray-900">No reading history found</h3>
              <p className="text-gray-500 text-sm mt-2">Your reading journey will appear here once you return your first book.</p>
            </div>
          </div>
        ) : (
          <>
            <div className="space-y-4">
              {history.map((item) => (
                <div
                  key={item.loanId}
                  className="bg-white border border-gray-100 p-6 rounded-sm shadow-lg hover:shadow-xl hover:border-indigo-100 transition-all"
                >
                  <div className="flex items-start justify-between">
                    <div className="flex space-x-6 flex-1">
                      <div className="w-20 h-28 bg-gray-100 flex-shrink-0 shadow-lg overflow-hidden flex items-center justify-center rounded-sm">
                        <img
                          src={item.bookCoverImageUrl || 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=2730&auto=format&fit=crop'}
                          alt={item.bookTitle}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=2730&auto=format&fit=crop';
                          }}
                        />
                      </div>
                      <div className="flex-1 space-y-3">
                        <div className="flex items-start justify-between">
                          <h3 className="text-lg font-display font-bold text-gray-900 line-clamp-2">
                            {item.bookTitle}
                          </h3>
                          {item.genre && (
                            <Badge variant="gray">{item.genre}</Badge>
                          )}
                        </div>
                        <div className="grid grid-cols-3 gap-4 text-sm">
                          <div>
                            <p className="text-[10px] font-mono text-gray-400 uppercase tracking-tighter">Returned</p>
                            <p className="font-bold text-gray-900 flex items-center mt-1">
                              <Calendar size={14} className="mr-2" />
                              {item.returnedAt ? format(new Date(item.returnedAt), 'MMM dd, yyyy') : 'N/A'}
                            </p>
                          </div>
                          <div>
                            <p className="text-[10px] font-mono text-gray-400 uppercase tracking-tighter">Days Held</p>
                            <p className="font-bold text-gray-900 flex items-center mt-1">
                              <Clock size={14} className="mr-2 text-indigo-500" />
                              {item.daysHeld} days
                            </p>
                          </div>
                          <div>
                            <p className="text-[10px] font-mono text-gray-400 uppercase tracking-tighter">Fine Amount</p>
                            <p className={`font-bold flex items-center mt-1 ${item.fineAmount > 0 ? 'text-red-600' : 'text-green-600'}`}>
                              ${item.fineAmount?.toFixed(2) || '0.00'}
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-center items-center space-x-2 mt-8">
                <button
                  onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                  disabled={currentPage === 1}
                  className="px-4 py-2 border border-gray-200 rounded-sm font-bold uppercase text-xs disabled:opacity-50 disabled:cursor-not-allowed hover:border-indigo-200 transition-all"
                >
                  Previous
                </button>
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                  <button
                    key={page}
                    onClick={() => setCurrentPage(page)}
                    className={`w-10 h-10 rounded-sm font-bold uppercase text-xs transition-all ${
                      currentPage === page
                        ? 'bg-indigo-600 text-white'
                        : 'border border-gray-200 hover:border-indigo-200'
                    }`}
                  >
                    {page}
                  </button>
                ))}
                <button
                  onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                  disabled={currentPage === totalPages}
                  className="px-4 py-2 border border-gray-200 rounded-sm font-bold uppercase text-xs disabled:opacity-50 disabled:cursor-not-allowed hover:border-indigo-200 transition-all"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberHistoryPage;
