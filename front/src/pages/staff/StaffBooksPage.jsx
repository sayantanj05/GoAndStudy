import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  Search, Filter, Book, BookOpen, 
  Loader2, ChevronRight, AlertCircle,
  Library
} from 'lucide-react';

const StaffBooksPage = () => {
  const [searchTerm, setSearchTerm] = useState('');

  const { data: booksData, isLoading } = useQuery({
    queryKey: ['staffBooks'],
    queryFn: staffApi.getBooks
  });

  const books = booksData?.books || [];
  const totalBooks = booksData?.total || books.length;
  const filteredBooks = books.filter((book) =>
    book.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (book.author || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (book.isbn || '').includes(searchTerm)
  );

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div>
          <h1 className="text-3xl font-display font-bold text-gray-900">Inventory Status</h1>
          <p className="text-gray-500 text-sm mt-1">Real-time repository query for ISBN lookup and availability.</p>
        </div>

        {/* Total Books Box */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white border border-gray-200 p-6 flex items-center space-x-4">
            <div className="w-12 h-12 bg-teal-50 flex items-center justify-center">
              <Library className="text-teal-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{isLoading ? '-' : totalBooks}</p>
              <p className="text-xs font-mono uppercase tracking-wider text-gray-500">Total Books</p>
            </div>
          </div>
          <div className="bg-white border border-gray-200 p-6 flex items-center space-x-4">
            <div className="w-12 h-12 bg-green-50 flex items-center justify-center">
              <BookOpen className="text-green-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{isLoading ? '-' : books.filter(b => b.availableCopies > 0).length}</p>
              <p className="text-xs font-mono uppercase tracking-wider text-gray-500">Available</p>
            </div>
          </div>
        </div>

        {/* Search */}
        <div className="flex items-center space-x-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="ISBN, Title, or Author..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-teal-500 font-medium"
            />
          </div>
          <button className="p-3 bg-white border border-gray-200 hover:bg-gray-50 text-gray-400 flex items-center space-x-2">
            <Filter size={18} />
            <span className="text-[10px] font-bold uppercase tracking-widest hidden sm:inline">Filters</span>
          </button>
        </div>

        {/* Books Table */}
        <div className="bg-white border border-gray-200">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-[10px] font-mono uppercase tracking-widest text-gray-500">
                <th className="px-6 py-4">Title & ISBN</th>
                <th className="px-6 py-4">Category</th>
                <th className="px-6 py-4 text-center">In Stock</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 font-medium">
              {isLoading ? (
                <tr>
                  <td colSpan="5" className="py-20 text-center">
                    <Loader2 className="animate-spin text-teal-500 mx-auto" size={32} />
                  </td>
                </tr>
              ) : filteredBooks.length === 0 ? (
                <tr>
                  <td colSpan="5" className="py-20 text-center text-gray-400">
                    No matching assets in repository.
                  </td>
                </tr>
              ) : (
                filteredBooks.map((book) => (
                  <tr key={book.bookId} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-4">
                        <div className="w-10 h-14 bg-gray-100 flex-shrink-0 flex items-center justify-center overflow-hidden">
                          {book.coverImageUrl ? (
                            <img 
                              src={book.coverImageUrl} 
                              alt={book.title}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                e.target.onerror = null;
                                e.target.style.display = 'none';
                                e.target.parentElement.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-gray-400"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"></path></svg>';
                              }}
                            />
                          ) : (
                            <Book size={18} className="text-gray-400" />
                          )}
                        </div>
                        <div>
                          <p className="font-bold text-gray-900 line-clamp-1">{book.title}</p>
                          <p className="text-[10px] font-mono text-gray-400 mt-1 uppercase">ISBN: {book.isbn}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs opacity-60">
                      {book.category}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className={`inline-flex items-center justify-center w-8 h-8 font-bold font-mono text-sm ${book.availableCopies === 0 ? 'bg-red-50 text-red-500' : 'bg-teal-50 text-teal-600'}`}>
                        {book.availableCopies}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {book.availableCopies > 0 ? (
                        <Badge variant="teal">Shelf Ready</Badge>
                      ) : (
                        <Badge variant="red">Reserved/Loaned</Badge>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                       <button className="p-2 text-gray-400 hover:text-teal-600 hover:bg-teal-50 transition-all opacity-0 group-hover:opacity-100">
                          <ChevronRight size={20} />
                       </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffBooksPage;
