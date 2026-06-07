import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import useAuthStore from '../../store/authStore';
import PageTransition from '../../components/shared/PageTransition';
import StatCard from '../../components/shared/StatCard';
import { 
  Sparkles, BookOpen, Clock, 
  ArrowRight, Search, Zap, 
  Heart, Bookmark, Star
} from 'lucide-react';
import { Link } from 'react-router-dom';

const LargeBookCard = ({ book }) => (
  <div className="group bg-white border border-gray-100 rounded-sm overflow-hidden hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
    <div className="aspect-[3/4] bg-gray-100 relative overflow-hidden">
      {book.coverImageUrl ? (
        <img 
          src={book.coverImageUrl} 
          alt={book.title} 
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
      ) : (
        <div className="w-full h-full flex items-center justify-center bg-indigo-50">
          <BookOpen size={48} className="text-indigo-200" />
        </div>
      )}
      {/* Predicted rating badge */}
      {book.matchScore > 0 && (
        <div className="absolute top-2 right-2 bg-indigo-600 text-white text-[10px] font-bold px-2 py-1 rounded-sm flex items-center gap-1">
          <Star size={10} fill="currentColor" />
          {(book.matchScore * 5).toFixed(1)}
        </div>
      )}
      {/* Hover overlay */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-end p-4">
        <button className="w-full py-2 bg-white text-indigo-900 rounded-sm font-bold uppercase tracking-widest text-[10px] hover:bg-indigo-50 transition-colors">
          Quick Reserve
        </button>
      </div>
    </div>
    <div className="p-4 space-y-1">
      <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-tighter bg-indigo-50 px-2 py-0.5 rounded-sm">
        {book.genre || book.category}
      </span>
      <h4 className="font-bold text-gray-900 line-clamp-1 text-sm">{book.title}</h4>
      <p className="text-xs text-gray-500">{book.author}</p>
    </div>
  </div>
);

const CompactBookCard = ({ book }) => (
  <div className="group bg-white border border-gray-100 rounded-sm overflow-hidden hover:shadow-lg transition-all duration-300">
    <div className="aspect-[3/4] bg-gray-100 relative overflow-hidden">
      {book.coverImageUrl ? (
        <img 
          src={book.coverImageUrl} 
          alt={book.title} 
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
        />
      ) : (
        <div className="w-full h-full flex items-center justify-center bg-slate-50">
          <BookOpen size={32} className="text-slate-200" />
        </div>
      )}
    </div>
    <div className="p-3 space-y-1">
      <h4 className="font-bold text-gray-900 line-clamp-1 text-xs">{book.title}</h4>
      <p className="text-[10px] text-gray-500">{book.author}</p>
      {book.reason && (
        <p className="text-[9px] text-indigo-500 italic line-clamp-1">{book.reason}</p>
      )}
    </div>
  </div>
);

const SectionSkeleton = ({ count, compact }) => (
  <div className={`grid grid-cols-2 md:grid-cols-3 ${compact ? 'lg:grid-cols-5' : 'lg:grid-cols-5'} gap-4`}>
    {Array.from({ length: count }).map((_, i) => (
      <div key={i} className="bg-white border border-gray-100 rounded-sm overflow-hidden animate-pulse">
        <div className="aspect-[3/4] bg-gray-200" />
        <div className="p-3 space-y-2">
          <div className="h-3 bg-gray-200 rounded w-3/4" />
          <div className="h-2 bg-gray-200 rounded w-1/2" />
        </div>
      </div>
    ))}
  </div>
);

const MemberHomePage = () => {
  const { user } = useAuthStore();
  
  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ['memberStats'],
    queryFn: memberApi.getStats
  });

  const { data: recsData, isLoading: recsLoading } = useQuery({
    queryKey: ['memberRecs'],
    queryFn: memberApi.getAIRecommendations
  });

  const stats = statsData?.data || {
    booksRead: 24,
    currentLoans: 3,
    activeReservations: 1,
    finesDue: 0
  };

  const topRecommendations = recsData?.data?.topRecommendations ?? [];
  const similarBooks = recsData?.data?.similarBooks ?? [];

  return (
    <PageTransition>
      <div className="p-8 space-y-12">
        {/* Welcome Hero */}
        <div className="relative overflow-hidden bg-gradient-to-br from-indigo-900 to-slate-900 rounded-sm p-12 text-white shadow-2xl">
          <div className="relative z-10 max-w-2xl space-y-6">
            <div className="inline-flex items-center bg-white/10 backdrop-blur-md px-4 py-1.5 rounded-full border border-white/20 text-indigo-200 text-xs font-bold uppercase tracking-widest">
              <Sparkles size={14} className="mr-2" /> Powered by GoAndStudy AI
            </div>
            <h1 className="text-5xl font-display font-bold leading-tight">
              Welcome back, <span className="text-indigo-400">{user?.name}</span>.
            </h1>
            <p className="text-indigo-100/70 text-lg leading-relaxed">
              Based on your interest in <span className="text-white font-bold italic underline underline-offset-4 decoration-indigo-400">Post-Modern Fiction</span>, our engine has curated new titles for you.
            </p>
            <div className="flex space-x-4 pt-4">
              <Link to="/member/browse" className="bg-white text-indigo-900 px-8 py-4 rounded-sm font-bold uppercase tracking-widest text-xs hover:bg-indigo-50 transition-all flex items-center">
                Explore Repository
                <ArrowRight size={16} className="ml-2" />
              </Link>
              <Link to="/member/ai-recommend" className="bg-white/10 backdrop-blur-md border border-white/30 text-white px-8 py-4 rounded-sm font-bold uppercase tracking-widest text-xs hover:bg-white/20 transition-all">
                My AI Path
              </Link>
            </div>
          </div>
          
          {/* Decorative Elements */}
          <div className="absolute top-0 right-0 w-1/3 h-full opacity-10 pointer-events-none transform translate-x-12 translate-y-12">
            <Zap size={400} />
          </div>
        </div>

        {/* Dash Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Literary Journey"
            value={stats.booksRead}
            icon="Zap"
            color="indigo"
            portal="member"
          />
          <StatCard
            title="Now Reading"
            value={stats.currentLoans}
            icon="BookOpen"
            color="amber"
            portal="member"
          />
          <StatCard
            title="Queue"
            value={stats.activeReservations}
            icon="Clock"
            color="blue"
            portal="member"
          />
          <StatCard
            title="Contributions"
            value={`₹${stats.finesDue}`}
            icon="DollarSign"
            color="red"
            portal="member"
          />
        </div>

        {/* Section 1: Top Recommendations for You */}
        <div className="space-y-6">
          <div className="flex justify-between items-end">
            <div>
              <h3 className="text-2xl font-display font-bold text-indigo-900">
                Top Recommendations for You
              </h3>
              <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">
                Hand-picked by our ML engine
              </p>
            </div>
            <Link to="/member/ai-recommend" className="text-xs font-bold uppercase tracking-widest text-indigo-600 hover:text-indigo-800 transition-colors">
              View All Analysis
            </Link>
          </div>

          {recsLoading ? (
            <SectionSkeleton count={5} compact={false} />
          ) : topRecommendations.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
              {topRecommendations.slice(0, 5).map((book) => (
                <LargeBookCard key={book.id} book={book} />
              ))}
            </div>
          ) : (
            <div className="bg-gray-50 border border-gray-200 rounded-sm p-8 text-center">
              <BookOpen size={48} className="mx-auto text-gray-300 mb-4" />
              <p className="text-gray-500 text-sm">No recommendations available yet.</p>
              <p className="text-gray-400 text-xs mt-1">Start borrowing books to get personalized picks!</p>
            </div>
          )}
        </div>

        {/* Section 2: You May Also Like */}
        <div className="space-y-6">
          <div>
            <h3 className="text-2xl font-display font-bold text-slate-800">
              You May Also Like
            </h3>
            <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">
              Readers with similar tastes enjoyed these
            </p>
          </div>

          {recsLoading ? (
            <SectionSkeleton count={10} compact={true} />
          ) : similarBooks.length > 0 ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              {similarBooks.slice(0, 10).map((book) => (
                <CompactBookCard key={book.id} book={book} />
              ))}
            </div>
          ) : (
            <div className="bg-gray-50 border border-gray-200 rounded-sm p-8 text-center">
              <Bookmark size={48} className="mx-auto text-gray-300 mb-4" />
              <p className="text-gray-500 text-sm">More recommendations coming soon.</p>
            </div>
          )}
        </div>

        {/* Floating AI Chat Trigger (Aesthetic Placeholder) */}
        <div className="fixed bottom-10 right-10 z-50">
          <button className="w-16 h-16 bg-indigo-600 text-white rounded-sm shadow-2xl flex items-center justify-center hover:bg-indigo-700 hover:scale-110 transition-all group scale-100">
            <Sparkles size={24} className="group-hover:rotate-12 transition-transform" />
          </button>
        </div>
      </div>
    </PageTransition>
  );
};

export default MemberHomePage;

