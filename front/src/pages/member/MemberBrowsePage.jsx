import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import API from '../../api/axios';
import PageTransition from '../../components/shared/PageTransition';
import BookCard from '../../components/shared/BookCard';
import { toast } from 'react-hot-toast';
import { 
  Search, Filter, Sparkles, 
  Loader2, SlidersHorizontal, BookOpen, X, Heart
} from 'lucide-react';

const MemberBrowsePage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [activeCategory, setActiveCategory] = useState('All');
  const [viewMode, setViewMode] = useState('grid');
  const [showFilters, setShowFilters] = useState(false);
  const [minRating, setMinRating] = useState(0);
  const [availableOnly, setAvailableOnly] = useState(false);
  const [selectedFilterCategories, setSelectedFilterCategories] = useState([]);

  const { data: booksData, isLoading } = useQuery({
    queryKey: ['browseBooks', searchTerm],
    queryFn: () => memberApi.searchBooks(searchTerm),
    staleTime: 0, // Always fetch fresh data
    cacheTime: 0 // Don't cache the response
  });

  const { data: categoriesData } = useQuery({
    queryKey: ['bookCategories'],
    queryFn: async () => {
      const { data } = await API.get('/api/v1/books/categories');
      return data.data || [];
    }
  });

  const { data: wishlistData, refetch: refetchWishlist } = useQuery({
    queryKey: ['memberWishlist'],
    queryFn: memberApi.getWishlist
  });

  const wishlistBookIds = new Set((wishlistData?.data || []).map(item => item.bookId));

  const toggleWishlistMutation = useMutation({
    mutationFn: memberApi.toggleWishlist,
    onSuccess: () => {
      refetchWishlist();
    },
    onError: () => {
      toast.error('Failed to update wishlist');
    }
  });

  const categories = ['All', 'Fiction', 'Science', 'History', 'Technology', 'Philosophy'];
  const allCategories = categoriesData || [];
  
  // Filter books by category and rating and availability
  let filteredBooks = activeCategory === 'All' 
    ? booksData?.data || [] 
    : (booksData?.data || []).filter(book => 
        (book.category || book.genre || '').toLowerCase() === activeCategory.toLowerCase()
      );
  
  // Apply additional filters
  filteredBooks = filteredBooks.filter(book => {
    if (book.averageRating < minRating) return false;
    if (availableOnly && book.availableCopies <= 0) return false;
    
    // Filter by selected categories from filter panel
    if (selectedFilterCategories.length > 0) {
      const bookCategory = book.category || book.genre || '';
      return selectedFilterCategories.some(cat => 
        bookCategory.toLowerCase().includes(cat.toLowerCase())
      );
    }
    
    return true;
  });

  const toggleFilterCategory = (categoryId, categoryName) => {
    setSelectedFilterCategories(prev => 
      prev.includes(categoryName)
        ? prev.filter(name => name !== categoryName)
        : [...prev, categoryName]
    );
  };

  // Helper function to get all category names for a book
  const getBookCategoryNames = (book) => {
    const categories = [];
    // Add single category/genre if present
    if (book.category) categories.push(book.category);
    if (book.genre) categories.push(book.genre);
    // Add categoryNames array if present
    if (book.categoryNames && Array.isArray(book.categoryNames)) {
      categories.push(...book.categoryNames);
    }
    // Add categoryIds array if present (for ID-based matching)
    if (book.categoryIds && Array.isArray(book.categoryIds)) {
      // We'll match against category names from allCategories
      const catIdNames = book.categoryIds.map(id => {
        const cat = allCategories.find(c => c.id === id);
        return cat?.name;
      }).filter(Boolean);
      categories.push(...catIdNames);
    }
    return categories.filter(Boolean);
  };

  // Filter books by category and rating and availability
  filteredBooks = activeCategory === 'All'
    ? booksData?.data || []
    : (booksData?.data || []).filter(book => {
        const bookCategories = getBookCategoryNames(book);
        return bookCategories.some(cat =>
          cat.toLowerCase() === activeCategory.toLowerCase()
        );
      });

  // Apply additional filters
  filteredBooks = filteredBooks.filter(book => {
    if (book.averageRating < minRating) return false;
    if (availableOnly && book.availableCopies <= 0) return false;

    // Filter by selected categories from filter panel (only if categories are selected)
    if (selectedFilterCategories.length > 0) {
      const bookCategories = getBookCategoryNames(book);
      // Check if book has ANY of the selected categories (OR logic)
      return selectedFilterCategories.some(selectedCat =>
        bookCategories.some(bookCat =>
          bookCat.toLowerCase() === selectedCat.toLowerCase()
        )
      );
    }

    return true;
  });

  return (
    <PageTransition>
      <div className="p-8 space-y-12">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <h1 className="text-4xl font-display font-bold text-gray-900">Discover Repository</h1>
            <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">Querying 14.5k digital and physical assets</p>
          </div>
          
          <div className="flex bg-white border border-gray-100 p-1 rounded-sm shadow-sm self-start">
             <button 
               onClick={() => setViewMode('grid')}
               className={`px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded-sm transition-all ${
                 viewMode === 'grid' 
                 ? 'bg-indigo-600 text-white' 
                 : 'text-gray-400 hover:text-indigo-600'
               }`}
             >
               Grid
             </button>
             <button 
               onClick={() => setViewMode('list')}
               className={`px-4 py-2 text-[10px] font-bold uppercase tracking-widest rounded-sm transition-all ${
                 viewMode === 'list' 
                 ? 'bg-indigo-600 text-white' 
                 : 'text-gray-400 hover:text-indigo-600'
               }`}
             >
               List
             </button>
          </div>
        </div>

        {/* Search & Filter Bar */}
        <div className="sticky top-2 z-30 bg-white/80 backdrop-blur-xl border border-white/20 p-6 rounded-sm shadow-xl flex flex-col md:flex-row gap-6">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-indigo-400" size={20} />
            <input
              type="text"
              placeholder="Search by Title, Author, or ISBN..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-4 bg-gray-50 border-none focus:ring-2 focus:ring-indigo-200 rounded-sm transition-all font-medium placeholder:text-gray-400"
            />
          </div>
          <div className="flex items-center space-x-3 overflow-x-auto pb-2 md:pb-0 scrollbar-hide">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={`flex-shrink-0 px-6 py-4 text-[10px] font-bold uppercase tracking-widest rounded-sm border transition-all ${
                  activeCategory === cat 
                  ? 'bg-indigo-600 border-indigo-600 text-white shadow-lg shadow-indigo-200' 
                  : 'bg-white border-gray-100 text-gray-500 hover:border-indigo-200 hover:text-indigo-600'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
          <button 
            onClick={() => setShowFilters(!showFilters)}
            className={`px-6 py-4 rounded-sm flex items-center justify-center transition-all ${
              showFilters 
                ? 'bg-indigo-600 text-white' 
                : 'bg-gray-900 text-white hover:bg-black'
            }`}
            title="Toggle Filters"
          >
            <SlidersHorizontal size={18} />
          </button>
        </div>

        {/* Filters Panel */}
        {showFilters && (
          <div className="bg-white border border-gray-200 p-6 rounded-sm shadow-lg space-y-6">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-bold">Advanced Filters</h3>
              <button 
                onClick={() => setShowFilters(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X size={20} />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Minimum Rating Filter */}
              <div>
                <label className="block text-sm font-bold mb-3">Minimum Rating</label>
                <div className="flex items-center space-x-4">
                  <input
                    type="range"
                    min="0"
                    max="5"
                    step="0.5"
                    value={minRating}
                    onChange={(e) => setMinRating(parseFloat(e.target.value))}
                    className="flex-1"
                  />
                  <span className="text-sm font-bold bg-indigo-100 text-indigo-700 px-3 py-1 rounded">
                    ★ {minRating.toFixed(1)}+
                  </span>
                </div>
              </div>

              {/* Availability Filter */}
              <div>
                <label className="block text-sm font-bold mb-3">Availability</label>
                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={availableOnly}
                    onChange={(e) => setAvailableOnly(e.target.checked)}
                    className="w-4 h-4"
                  />
                  <span className="text-sm">Show only available copies</span>
                </label>
              </div>
            </div>

            {/* Categories Filter */}
            <div>
              <label className="block text-sm font-bold mb-3">Book Categories</label>
              <div className="grid grid-cols-2 md:grid-cols-3 gap-3 max-h-60 overflow-y-auto">
                {allCategories.length > 0 ? (
                  allCategories.map((cat) => (
                    <label key={cat.id} className="flex items-center space-x-2 cursor-pointer p-2 hover:bg-gray-50 rounded">
                      <input
                        type="checkbox"
                        checked={selectedFilterCategories.includes(cat.name)}
                        onChange={() => toggleFilterCategory(cat.id, cat.name)}
                        className="w-4 h-4"
                      />
                      <span className="text-sm">{cat.name}</span>
                    </label>
                  ))
                ) : (
                  <p className="text-sm text-gray-500 col-span-3">Loading categories...</p>
                )}
              </div>
            </div>

            {/* Reset Filters Button */}
            <div className="flex gap-3 pt-4 border-t">
              <button
                onClick={() => {
                  setMinRating(0);
                  setAvailableOnly(false);
                  setSelectedFilterCategories([]);
                }}
                className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 font-bold uppercase text-xs rounded-sm hover:bg-gray-50 transition-all"
              >
                Reset Filters
              </button>
              <button
                onClick={() => setShowFilters(false)}
                className="flex-1 px-4 py-2 bg-indigo-600 text-white font-bold uppercase text-xs rounded-sm hover:bg-indigo-700 transition-all"
              >
                Apply & Close
              </button>
            </div>
          </div>
        )}

        {/* Results Info */}
        <div className="flex items-center space-x-2 text-indigo-600 font-mono text-xs uppercase font-bold">
          <Sparkles size={14} className="animate-pulse" />
          <span>Showing AI-Reranked results based on your profile</span>
        </div>

        {/* Book Grid or List */}
        {isLoading ? (
          <div className="py-24 flex flex-col items-center justify-center space-y-4">
             <Loader2 className="animate-spin text-indigo-500" size={48} />
             <p className="text-sm font-mono text-gray-400 uppercase tracking-widest">Querying Vector Index...</p>
          </div>
        ) : (
          <>
            {viewMode === 'grid' ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-8">
                {filteredBooks?.map((book) => (
                  <BookCard 
                    key={book.bookId} 
                    book={book} 
                    isWishlisted={wishlistBookIds.has(book.bookId)}
                    onWishlistToggle={(bookId) => toggleWishlistMutation.mutate(bookId)}
                  />
                ))}
              </div>
            ) : (
              <div className="bg-white border border-gray-200 rounded-sm overflow-hidden">
                <div className="divide-y divide-gray-100">
                  {filteredBooks?.map((book) => (
                    <div key={book.bookId} className="p-6 hover:bg-gray-50 transition-colors flex gap-6">
                      <div className="w-16 h-24 bg-gray-200 rounded flex-shrink-0 flex items-center justify-center">
                        <BookOpen size={32} className="text-gray-400" />
                      </div>
                      <div className="flex-1">
                        <h3 className="font-bold text-gray-900 text-sm line-clamp-2">{book.title}</h3>
                        <p className="text-xs text-gray-500 mt-1">{book.author}</p>
                        <div className="flex items-center justify-between mt-4">
                          <div className="flex gap-4 text-xs text-gray-600">
                            <span>★ {book.averageRating?.toFixed(1) || '0.0'}</span>
                            <span>{book.availableCopies || 0} copies available</span>
                          </div>
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => toggleWishlistMutation.mutate(book.bookId)}
                              className="p-2 hover:bg-gray-100 rounded transition-colors"
                              title={wishlistBookIds.has(book.bookId) ? 'Remove from wishlist' : 'Add to wishlist'}
                            >
                              <Heart 
                                size={16} 
                                className={wishlistBookIds.has(book.bookId) ? 'fill-red-500 text-red-500' : 'text-gray-400'} 
                              />
                            </button>
                            <Link
                              to={`/member/books/${encodeURIComponent(book.bookId || book.isbn)}`}
                              className="px-3 py-1 bg-indigo-600 text-white text-xs rounded hover:bg-indigo-700 transition-colors"
                            >
                              Details
                            </Link>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}

        {/* Empty State */}
        {!isLoading && filteredBooks?.length === 0 && (
          <div className="py-24 text-center space-y-6 max-w-md mx-auto">
             <div className="w-20 h-20 bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center mx-auto">
               <BookOpen size={40} />
             </div>
             <div>
               <h3 className="text-xl font-display font-bold text-gray-900">Vast Empty Spaces</h3>
               <p className="text-gray-500 text-sm mt-2">No assets found for "{searchTerm}". Try a different query or explore our AI suggestions.</p>
             </div>
             <button 
              onClick={() => {setSearchTerm(''); setActiveCategory('All');}}
              className="px-8 py-3 bg-indigo-600 text-white font-bold uppercase tracking-widest text-[10px] rounded-sm hover:bg-indigo-700 transition-all"
            >
              Reset All Filters
            </button>
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberBrowsePage;
