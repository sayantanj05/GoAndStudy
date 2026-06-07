import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import {
  BookOpen, Search, Filter, Plus,
  AlertTriangle, ArrowRight, Loader2, Book, X, Pencil, Trash2
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminBooksPage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    isbn: '',
    authors: '',
    categoryIds: [],
    description: '',
    coverImageUrl: '',
    totalCopies: 1,
    pageCount: 1,
    publishedYear: new Date().getFullYear()
  });
  const [editFormData, setEditFormData] = useState({
    title: '',
    description: '',
    authors: '',
    coverImageUrl: '',
    totalCopies: 1,
  });
  const [authorCount, setAuthorCount] = useState(1);
  const [editAuthorCount, setEditAuthorCount] = useState(1);
  const [editCategoryCount, setEditCategoryCount] = useState(1);
  const [categoryCount, setCategoryCount] = useState(1);

  const { data: booksData, isLoading } = useQuery({
    queryKey: ['adminBooks'],
    queryFn: adminApi.getBooks
  });

  const { data: categoriesData } = useQuery({
    queryKey: ['adminCategories'],
    queryFn: adminApi.getCategories
  });

  const { data: authorsData } = useQuery({
    queryKey: ['adminAuthors'],
    queryFn: adminApi.getAuthors
  });

  const authors = authorsData?.data?.authors || authorsData?.authors || [];

  const { data: lowStockData } = useQuery({
    queryKey: ['adminLowStock'],
    queryFn: adminApi.getLowStock
  });

  const createMutation = useMutation({
    mutationFn: adminApi.addBook || (() => Promise.reject('API not implemented')),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminBooks']);
      toast.success('Book added successfully');
      setIsModalOpen(false);
      setFormData({ title: '', isbn: '', authorName: '', categoryIds: [], description: '', coverImageUrl: '', totalCopies: 1, pageCount: 1, publishedYear: new Date().getFullYear() });
      setAuthorCount(1);
      setCategoryCount(1);
    },
    onError: (error) => {
      console.error('Add book failed', error);
      toast.error(`Failed to add book: ${error?.message || 'server error'}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ bookId, payload }) => adminApi.updateBook(bookId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminBooks']);
      toast.success('Book updated');
      setIsEditModalOpen(false);
      setEditingBook(null);
    },
    onError: (error) => {
      console.error('Update book failed', error);
      toast.error(error?.message || 'Failed to update book');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: ({ bookId, reason }) => adminApi.deleteBook(bookId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminBooks']);
      toast.success('Book removed from inventory');
    },
    onError: (error) => toast.error(error?.message || 'Failed to delete book')
  });

  const adjustQuantityMutation = useMutation({
    mutationFn: ({ bookId, totalCopies }) => adminApi.updateBook(bookId, { totalCopies }),
    onMutate: async ({ bookId, totalCopies }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries(['adminBooks']);
      
      // Snapshot previous value
      const previousData = queryClient.getQueryData(['adminBooks']);
      
      // Optimistically update cache
      queryClient.setQueryData(['adminBooks'], (old) => {
        if (!old) return old;
        const data = old.data?.data || old.data || old;
        const books = Array.isArray(data) ? data : data.books;
        if (!books) return old;
        
        const updatedBooks = books.map(book => 
          book.bookId === bookId 
            ? { ...book, totalCopies, availableCopies: totalCopies }
            : book
        );
        
        if (Array.isArray(data)) {
          return { ...old, data: updatedBooks };
        }
        return { ...old, data: { ...old.data, books: updatedBooks } };
      });
      
      return { previousData };
    },
    onError: (error, variables, context) => {
      // Rollback on error
      if (context?.previousData) {
        queryClient.setQueryData(['adminBooks'], context.previousData);
      }
      toast.error(error?.message || 'Failed to update quantity');
    },
    onSettled: () => {
      // Silent background refresh to sync with server
      queryClient.invalidateQueries(['adminBooks']);
    }
  });

  const responseFromServer = booksData?.data?.data || booksData?.data || [];
  const books = (Array.isArray(responseFromServer) ? responseFromServer : responseFromServer?.books) || [];
  const categories = categoriesData?.data?.data || categoriesData?.data || [];
  
  // Comprehensive category mapping with all seeded categories
  const categoryMap = {
    // API-fetched categories (will be overridden if API returns data)
    ...categories.reduce((map, cat) => {
      map[cat.id] = cat.name;
      return map;
    }, {}),
    
    // All seeded categories from DataSeeder.java
    'CAT001': 'Fiction',
    'CAT002': 'Self-help',
    'CAT003': 'History',
    'CAT004': 'Sci-Fi',
    'CAT005': 'Memoir',
    'CAT006': 'Philosophy',
    'CAT007': 'Psychology',
    'CAT008': 'Biography',
    'CAT009': 'Mystery',
    'CAT010': 'Technology',
    'CAT011': 'Fantasy',
    'CAT012': 'Romance',
    'CAT013': 'Horror',
    'CAT014': 'Poetry',
    'CAT015': 'Religious'
  };
  
  // Log available categories for debugging
  console.log('Available categories:', categories);
  console.log('Category map:', categoryMap);
  
  // Helper function to get category name with comprehensive fallbacks
  const getCategoryName = (category) => {
    if (!category) return 'Unknown';
    
    // If it's already a name (not an ID), return as-is
    if (!/^[A-Z]{3}\d+$/.test(category)) {
      return category;
    }
    
    // If it's an ID, look up the name
    const categoryName = categoryMap[category];
    if (categoryName) {
      return categoryName;
    }
    
    // Enhanced fallback for unknown categories
    console.warn(`Unknown category ID: ${category}`, {
      availableCategories: Object.keys(categoryMap),
      requestedCategory: category
    });
    
    // Try to extract meaningful info from ID
    const categoryNumber = category.match(/\d+/)?.[0];
    if (categoryNumber) {
      return `Category ${categoryNumber}`;
    }
    
    return 'Unknown Category';
  };

  const filteredBooks = books.filter((b) => 
    (b.title || '').toLowerCase().includes(searchTerm.toLowerCase()) || 
    (b.author || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (b.isbn || '').includes(searchTerm)
  );

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Inventory Control</h1>
            <p className="text-gray-500 text-sm mt-1">Manage global book stock, categories, and availability.</p>
          </div>
          <div className="flex items-center space-x-4">
            {/* Total Books Stats Box */}
            <div className="bg-white border border-gray-200 px-6 py-3 flex items-center space-x-4">
              <div className="w-10 h-10 bg-gray-100 flex items-center justify-center">
                <BookOpen size={20} className="text-gray-600" />
              </div>
              <div>
                <p className="text-xs text-gray-500 uppercase tracking-wider">Total Books</p>
                <p className="text-xl font-bold text-gray-900">{filteredBooks.length}</p>
              </div>
            </div>
            <button
              onClick={() => setIsModalOpen(true)}
              className="flex items-center space-x-2 bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors text-xs">
              <Plus size={18} />
              <span>Add Title</span>
            </button>
          </div>
        </div>

        {/* Low Stock Alerts */}
        {lowStockData?.data?.length > 0 && (
          <div className="bg-amber-50 border border-amber-200 p-6 flex flex-col md:flex-row md:items-center justify-between">
            <div className="flex items-center space-x-4 mb-4 md:mb-0">
              <div className="w-12 h-12 bg-amber-100 text-amber-600 flex items-center justify-center">
                <AlertTriangle size={24} />
              </div>
              <div>
                <h4 className="font-bold text-amber-900">Inventory Shortage Detected</h4>
                <p className="text-sm text-amber-700">{lowStockData.data.length} titles have fewer than 2 copies available.</p>
              </div>
            </div>
            <button className="flex items-center space-x-2 text-xs font-bold uppercase tracking-widest text-amber-900 hover:underline">
              <span>View Requirements</span>
              <ArrowRight size={14} />
            </button>
          </div>
        )}

        {/* Search */}
        <div className="flex items-center space-x-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="Search by title, author, or ISBN..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-red-500 font-medium"
            />
          </div>
        </div>

        {/* Books Table */}
        <div className="bg-white border border-gray-200">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">
                <th className="px-6 py-4">Title & Author</th>
                <th className="px-6 py-4">Category</th>
                <th className="px-6 py-4">ISBN</th>
                <th className="px-6 py-4 text-center">Quantity</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan="6" className="py-20 text-center">
                    <Loader2 className="animate-spin text-red-500 mx-auto" size={32} />
                  </td>
                </tr>
              ) : filteredBooks.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-20 text-center text-gray-400">
                    No matching titles in inventory.
                  </td>
                </tr>
              ) : (
                filteredBooks.map((book) => (
                  <tr key={book.bookId} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-4">
                        <div className="w-10 h-14 bg-gray-100 border border-gray-200 flex-shrink-0 flex items-center justify-center overflow-hidden">
                          {book.coverImageUrl ? (
                            <img 
                              src={book.coverImageUrl} 
                              className="w-full h-full object-cover" 
                              alt={book.title}
                              onLoad={(e) => {
                                e.target.style.display = 'block';
                                e.target.nextSibling.style.display = 'none';
                              }}
                              onError={(e) => {
                                e.target.style.display = 'none';
                                e.target.nextSibling.style.display = 'block';
                                console.warn(`Failed to load image: ${book.coverImageUrl} for book: ${book.title}`);
                              }}
                              style={{ display: 'none' }}
                            />
                          ) : null}
                          <Book size={20} className="text-gray-400" />
                        </div>
                        <div>
                          <p className="font-bold text-gray-900 line-clamp-1">{book.title}</p>
                          <p className="text-xs text-gray-400 line-clamp-1">by {book.author || 'Unknown Author'}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1">
                        {(book.categoryIds || (book.category ? [book.category] : [])).slice(0, 3).map((catId, idx) => (
                          <Badge key={idx} variant="gray">{getCategoryName(catId)}</Badge>
                        ))}
                        {(book.categoryIds || []).length > 3 && (
                          <Badge variant="gray">+{(book.categoryIds || []).length - 3}</Badge>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-xs opacity-60">
                      {book.isbn || '---'}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="inline-flex items-center gap-1">
                        <button
                          onClick={() => {
                            const newQty = Math.max(0, book.totalCopies - 1);
                            adjustQuantityMutation.mutate({ bookId: book.bookId, totalCopies: newQty });
                          }}
                          disabled={book.totalCopies <= 0}
                          className="w-6 h-6 flex items-center justify-center border border-gray-300 bg-white hover:bg-gray-100 text-gray-600 disabled:opacity-30"
                          title="Decrease quantity"
                        >
                          -
                        </button>
                        <div className={`w-10 h-8 flex items-center justify-center font-bold font-mono text-sm ${book.availableCopies < 2 ? 'text-red-500 bg-red-50' : 'text-gray-600 bg-gray-100'}`}>
                          {book.totalCopies}
                        </div>
                        <button
                          onClick={() => {
                            const newQty = book.totalCopies + 1;
                            adjustQuantityMutation.mutate({ bookId: book.bookId, totalCopies: newQty });
                          }}
                          className="w-6 h-6 flex items-center justify-center border border-gray-300 bg-white hover:bg-gray-100 text-gray-600"
                          title="Increase quantity"
                        >
                          +
                        </button>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={book.availableCopies > 0 ? 'teal' : 'red'}>
                        {book.availableCopies > 0 ? 'Available' : 'Zero Stock'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="inline-flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={() => {
                            setEditingBook(book);
                            const initialEditAuthors = book.author ? book.author.split(', ').slice(0, 3) : [];
                            const initialEditCount = Math.max(1, initialEditAuthors.length);
                            setEditAuthorCount(initialEditCount);
                            const bookCategories = book.categoryIds || (book.category ? [book.category] : []);
                          const initialEditCategories = Array.isArray(bookCategories) ? bookCategories.slice(0, 5) : [];
                          const initialEditCatCount = Math.max(1, initialEditCategories.length);
                          setEditCategoryCount(initialEditCatCount);
                          setEditFormData({
                              title: book.title || '',
                              description: book.description || '',
                              coverImageUrl: book.coverImageUrl || '',
                              totalCopies: Number(book.totalCopies || book.availableCopies || 1),
                              ...initialEditAuthors.reduce((acc, name, index) => {
                                acc[`editAuthor${index}`] = name;
                                return acc;
                              }, {}),
                              ...initialEditCategories.reduce((acc, catId, index) => {
                                acc[`editCategory${index}`] = catId;
                                return acc;
                              }, {})
                            });
                            setIsEditModalOpen(true);
                          }}
                          className="p-2 border border-gray-200 bg-white hover:bg-gray-50 text-gray-700"
                          title="Edit"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          onClick={() => {
                            const ok = window.confirm(`Delete "${book.title}"? This will remove it from member browse/search.`);
                            if (!ok) return;
                            deleteMutation.mutate({ bookId: book.bookId, reason: 'Deleted from admin inventory' });
                          }}
                          disabled={deleteMutation.isPending}
                          className="p-2 border border-red-200 bg-white hover:bg-red-50 text-red-600 disabled:opacity-50"
                          title="Delete"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Book Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full max-h-[85vh] overflow-y-auto space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-2xl font-display font-bold">Add New Book</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
</div>

<form onSubmit={(e) => {
               e.preventDefault();
               const authorsList = Array.from({length: authorCount}).map((_, index) => formData[`author${index}`]).filter(Boolean);
               // Build categoryIds array from all category selections
               const categoriesList = Array.from({length: categoryCount}).map((_, index) => formData[`category${index}`]).filter(Boolean);
               // Use defaults to ensure validation passes
               const payload = {
                 ...formData,
                 subtitle: formData.subtitle || '',
                 pageCount: formData.pageCount || 1,
                 totalCopies: formData.totalCopies || 1,
                 tags: formData.tags || [],
                 authorName: authorsList.length > 0 ? authorsList.join(', ') : 'Unknown Author',
                 categoryIds: categoriesList.length > 0 ? categoriesList : [],
               };
               console.log('Add book payload:', payload);
               createMutation.mutate(payload);
             }} className="space-y-4">
              <div>
                <label className="block text-sm font-bold mb-2">Title *</label>
                <input
                  type="text"
                  required
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  placeholder="Book title"
                />
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">ISBN *</label>
                <input
                  type="text"
                  required
                  value={formData.isbn}
                  onChange={(e) => setFormData({ ...formData, isbn: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  placeholder="ISBN-13"
                />
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-bold">Authors ({authorCount})</label>
                  <div className="flex items-center space-x-2">
                    <button 
                      type="button"
                      onClick={() => setAuthorCount(Math.max(1, authorCount - 1))}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      -
                    </button>
                    <span className="text-sm font-mono px-2 py-1 bg-gray-100 rounded">{authorCount}</span>
                    <button 
                      type="button"
                      onClick={() => setAuthorCount(authorCount + 1)}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      +
                    </button>
                  </div>
                </div>
                {Array.from({length: authorCount}).map((_, index) => (
                  <select
                    key={index}
                    value={formData[`author${index}`] || ''}
                    onChange={(e) => setFormData({ ...formData, [`author${index}`]: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm bg-white"
                  >
                    <option value="">Select Author {index + 1} (optional)</option>
                    {authors.map((author) => (
                      <option key={author.id} value={author.name}>
                        {author.name}
                      </option>
                    ))}
                  </select>
                ))}
                <p className="text-xs text-gray-500">Add multiple authors individually. Backend creates IDs.</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-bold mb-2">Page Count *</label>
                  <input
                    type="number"
                    required
                    min="1"
                    value={formData.pageCount}
                    onChange={(e) => setFormData({ ...formData, pageCount: Number(e.target.value) })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                    placeholder="Page count"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold mb-2">Published Year</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.publishedYear}
                    onChange={(e) => setFormData({ ...formData, publishedYear: Number(e.target.value) })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                    placeholder="Published year"
                  />
                </div>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-bold">Categories ({categoryCount})</label>
                  <div className="flex items-center space-x-2">
                    <button
                      type="button"
                      onClick={() => setCategoryCount(Math.max(1, categoryCount - 1))}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      -
                    </button>
                    <span className="text-sm font-mono px-2 py-1 bg-gray-100 rounded">{categoryCount}</span>
                    <button
                      type="button"
                      onClick={() => setCategoryCount(categoryCount + 1)}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      +
                    </button>
                  </div>
                </div>
                {Array.from({length: categoryCount}).map((_, index) => (
                  <select
                    key={index}
                    value={formData[`category${index}`] || ''}
                    onChange={(e) => {
                      const newFormData = { ...formData, [`category${index}`]: e.target.value };
                      // Build categoryIds array from all category selections
                      const selectedCategories = Array.from({length: categoryCount})
                        .map((_, i) => newFormData[`category${i}`])
                        .filter(Boolean);
                      newFormData.categoryIds = selectedCategories;
                      setFormData(newFormData);
                    }}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm bg-white"
                  >
                    <option value="">Select Category {index + 1} (optional)</option>
                    {(categoriesData?.data?.data || categoriesData?.data || []).map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                ))}
                <p className="text-xs text-gray-500">Add multiple categories individually.</p>
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Cover Image URL</label>
                <input
                  type="url"
                  value={formData.coverImageUrl}
                  onChange={(e) => setFormData({ ...formData, coverImageUrl: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  placeholder="https://covers.openlibrary.org/b/isbn/..."
                />
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Total Copies</label>
                <input
                  type="number"
                  min="1"
                  value={formData.totalCopies}
                  onChange={(e) => setFormData({ ...formData, totalCopies: parseInt(e.target.value) })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  placeholder="1"
                />
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  placeholder="Book description"
                  rows="3"
                />
              </div>

              <div className="flex space-x-4 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-gray-200 font-bold uppercase text-xs hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 px-4 py-2 bg-gray-900 text-white font-bold uppercase text-xs hover:bg-black disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Adding...' : 'Add Book'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Book Modal */}
      {isEditModalOpen && editingBook && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-lg p-6 max-w-md w-full max-h-[85vh] overflow-y-auto space-y-4">
            <div className="flex justify-between items-center">
              <h2 className="text-2xl font-display font-bold">Edit Book</h2>
              <button onClick={() => { setIsEditModalOpen(false); setEditingBook(null); }} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                const editAuthorsList = Array.from({length: editAuthorCount}).map((_, index) => editFormData[`editAuthor${index}`]).filter(Boolean);
                const editCategoriesList = Array.from({length: editCategoryCount}).map((_, index) => editFormData[`editCategory${index}`]).filter(Boolean);
                updateMutation.mutate({
                  bookId: editingBook.bookId,
                  payload: {
                    title: editFormData.title,
                    description: editFormData.description,
                    authorName: editAuthorsList.length > 0 ? editAuthorsList.join(', ') : undefined,
                    categoryIds: editCategoriesList.length > 0 ? editCategoriesList : undefined,
                    coverImageUrl: editFormData.coverImageUrl,
                    totalCopies: Number(editFormData.totalCopies),
                  },
                });
              }}
              className="space-y-4"
            >
              <div>
                <label className="block text-sm font-bold mb-2">Title *</label>
                <input
                  type="text"
                  required
                  value={editFormData.title}
                  onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                />
              </div>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-bold">Authors ({editAuthorCount})</label>
                  <div className="flex items-center space-x-2">
                    <button 
                      type="button"
                      onClick={() => setEditAuthorCount(Math.max(1, editAuthorCount - 1))}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      -
                    </button>
                    <span className="text-sm font-mono px-2 py-1 bg-gray-100 rounded">{editAuthorCount}</span>
                    <button 
                      type="button"
                      onClick={() => setEditAuthorCount(editAuthorCount + 1)}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      +
                    </button>
                  </div>
                </div>
                {Array.from({length: editAuthorCount}).map((_, index) => (
                  <select
                    key={index}
                    value={editFormData[`editAuthor${index}`] || ''}
                    onChange={(e) => setEditFormData({ ...editFormData, [`editAuthor${index}`]: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm bg-white"
                  >
                    <option value="">Select Author {index + 1}</option>
                    {authors.map((author) => (
                      <option key={author.id} value={author.name}>
                        {author.name}
                      </option>
                    ))}
                  </select>
                ))}
                <p className="text-xs text-gray-500">Update authors for this book.</p>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="block text-sm font-bold">Categories ({editCategoryCount})</label>
                  <div className="flex items-center space-x-2">
                    <button
                      type="button"
                      onClick={() => setEditCategoryCount(Math.max(1, editCategoryCount - 1))}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      -
                    </button>
                    <span className="text-sm font-mono px-2 py-1 bg-gray-100 rounded">{editCategoryCount}</span>
                    <button
                      type="button"
                      onClick={() => setEditCategoryCount(editCategoryCount + 1)}
                      className="p-1 text-gray-500 hover:text-gray-700"
                    >
                      +
                    </button>
                  </div>
                </div>
                {Array.from({length: editCategoryCount}).map((_, index) => (
                  <select
                    key={index}
                    value={editFormData[`editCategory${index}`] || ''}
                    onChange={(e) => setEditFormData({ ...editFormData, [`editCategory${index}`]: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm bg-white"
                  >
                    <option value="">Select Category {index + 1}</option>
                    {(categoriesData?.data?.data || categoriesData?.data || []).map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                ))}
                <p className="text-xs text-gray-500">Update categories for this book.</p>
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Total Copies *</label>
                <input
                  type="number"
                  min="1"
                  required
                  value={editFormData.totalCopies}
                  onChange={(e) => setEditFormData({ ...editFormData, totalCopies: Number(e.target.value) })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                />
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Cover Image URL</label>
                <input
                  type="url"
                  value={editFormData.coverImageUrl}
                  onChange={(e) => setEditFormData({ ...editFormData, coverImageUrl: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                />
                <p className="mt-1 text-xs text-gray-400">
                  Tip: paste a direct image URL (ending with .jpg/.png/.webp). Search-result links may not render.
                </p>
              </div>

              <div>
                <label className="block text-sm font-bold mb-2">Description</label>
                <textarea
                  value={editFormData.description}
                  onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500"
                  rows="4"
                />
              </div>

              <div className="flex space-x-4 pt-4">
                <button
                  type="button"
                  onClick={() => { setIsEditModalOpen(false); setEditingBook(null); }}
                  className="flex-1 px-4 py-2 border border-gray-200 font-bold uppercase text-xs hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="flex-1 px-4 py-2 bg-gray-900 text-white font-bold uppercase text-xs hover:bg-black disabled:opacity-50"
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </PageTransition>
  );
};

export default AdminBooksPage;
