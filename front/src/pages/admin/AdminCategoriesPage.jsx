import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Tag, Search, Filter, Plus, 
  AlertTriangle, ArrowRight, Loader2, BookOpen, X, Pencil, Trash2
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminCategoriesPage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });
  const [editFormData, setEditFormData] = useState({});

  const { data: categoriesData, isLoading } = useQuery({
    queryKey: ['adminCategories'],
    queryFn: () => adminApi.getCategories()
  });

  const categories = categoriesData?.data?.data || categoriesData?.data || [];

  const createMutation = useMutation({
    mutationFn: adminApi.addCategory,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminCategories']);
      toast.success('Category added successfully');
      setIsModalOpen(false);
      setFormData({ name: '', description: '' });
    },
    onError: (error) => toast.error(error?.message || 'Failed to add category')
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => adminApi.updateCategory(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminCategories']);
      toast.success('Category updated');
      setIsEditModalOpen(false);
    },
    onError: (error) => toast.error(error?.message || 'Failed to update category')
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => adminApi.deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminCategories']);
      toast.success('Category deleted');
    },
    onError: (error) => toast.error(error?.message || 'Failed to delete category')
  });

  const filteredCategories = categories.filter(c => 
    c.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    c.description?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleCreate = (e) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const handleUpdate = (e) => {
    e.preventDefault();
    updateMutation.mutate({ id: editingCategory.id, payload: editFormData });
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Category Management</h1>
            <p className="text-gray-500 text-sm mt-1">Manage book categories and view book counts.</p>
          </div>
          <button 
            onClick={() => setIsModalOpen(true)}
            className="flex items-center space-x-2 bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors text-xs">
            <Plus size={18} />
            <span>Add Category</span>
          </button>
        </div>

        {/* Total Categories Box */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white border border-gray-200 p-6 flex items-center space-x-4">
            <div className="w-12 h-12 bg-amber-50 flex items-center justify-center">
              <Tag className="text-amber-600" size={24} />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900">{isLoading ? '-' : categories.length}</p>
              <p className="text-xs font-mono uppercase tracking-wider text-gray-500">Total Categories</p>
            </div>
          </div>
        </div>

        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search categories..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm"
          />
        </div>

        {/* Categories Table */}
        <div className="bg-white border border-gray-200">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th className="text-left px-6 py-4 text-xs font-bold uppercase tracking-widest text-gray-500">ID</th>
                <th className="text-left px-6 py-4 text-xs font-bold uppercase tracking-widest text-gray-500">Category Name</th>
                <th className="text-left px-6 py-4 text-xs font-bold uppercase tracking-widest text-gray-500">Description</th>
                <th className="text-center px-6 py-4 text-xs font-bold uppercase tracking-widest text-gray-500">Books</th>
                <th className="text-right px-6 py-4 text-xs font-bold uppercase tracking-widest text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center">
                    <Loader2 className="animate-spin mx-auto text-gray-400" size={24} />
                  </td>
                </tr>
              ) : filteredCategories.length === 0 ? (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center text-gray-500">
                    No categories found
                  </td>
                </tr>
              ) : (
                filteredCategories.map((category) => (
                  <tr key={category.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 text-sm font-mono text-gray-500">
                      {category.id}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-amber-100 flex items-center justify-center rounded-sm">
                          <Tag size={18} className="text-amber-600" />
                        </div>
                        <span className="font-medium text-gray-900">{category.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600 max-w-xs truncate">
                      {category.description || 'No description'}
                    </td>
                    <td className="px-6 py-4 text-center text-sm font-bold text-gray-700">
                      {category.bookCount || 0}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-end space-x-2">
                        <button
                          onClick={() => {
                            setEditingCategory(category);
                            setEditFormData({ name: category.name, description: category.description });
                            setIsEditModalOpen(true);
                          }}
                          className="p-2 text-gray-400 hover:text-gray-600"
                        >
                          <Pencil size={18} />
                        </button>
                        <button
                          onClick={() => {
                            if (confirm(`Delete category "${category.name}"?`)) {
                              deleteMutation.mutate(category.id);
                            }
                          }}
                          className="p-2 text-red-400 hover:text-red-600"
                        >
                          <Trash2 size={18} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Add Category Modal */}
        {isModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white max-w-md w-full p-6 space-y-6">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold">Add Category</h2>
                <button onClick={() => setIsModalOpen(false)}>
                  <X size={24} className="text-gray-400 hover:text-gray-600" />
                </button>
              </div>
              <form onSubmit={handleCreate} className="space-y-4">
                <div>
                  <label className="block text-sm font-bold mb-2">Category Name *</label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm"
                    placeholder="e.g., Science Fiction"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold mb-2">Description</label>
                  <textarea
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm h-24 resize-none"
                    placeholder="Brief description..."
                  />
                </div>
                <div className="flex space-x-4 pt-4">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="flex-1 px-4 py-2 border border-gray-200 text-gray-700 font-bold uppercase tracking-widest text-xs hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={createMutation.isPending}
                    className="flex-1 px-4 py-2 bg-gray-900 text-white font-bold uppercase tracking-widest text-xs hover:bg-black disabled:opacity-50"
                  >
                    {createMutation.isPending ? 'Adding...' : 'Add Category'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Edit Category Modal */}
        {isEditModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white max-w-md w-full p-6 space-y-6">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-bold">Edit Category</h2>
                <button onClick={() => setIsEditModalOpen(false)}>
                  <X size={24} className="text-gray-400 hover:text-gray-600" />
                </button>
              </div>
              <form onSubmit={handleUpdate} className="space-y-4">
                <div>
                  <label className="block text-sm font-bold mb-2">Category Name *</label>
                  <input
                    type="text"
                    required
                    value={editFormData.name}
                    onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold mb-2">Description</label>
                  <textarea
                    value={editFormData.description}
                    onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                    className="w-full px-4 py-2 border border-gray-200 focus:outline-none focus:border-red-500 rounded-sm h-24 resize-none"
                  />
                </div>
                <div className="flex space-x-4 pt-4">
                  <button
                    type="button"
                    onClick={() => setIsEditModalOpen(false)}
                    className="flex-1 px-4 py-2 border border-gray-200 text-gray-700 font-bold uppercase tracking-widest text-xs hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={updateMutation.isPending}
                    className="flex-1 px-4 py-2 bg-gray-900 text-white font-bold uppercase tracking-widest text-xs hover:bg-black disabled:opacity-50"
                  >
                    {updateMutation.isPending ? 'Updating...' : 'Update Category'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default AdminCategoriesPage;
