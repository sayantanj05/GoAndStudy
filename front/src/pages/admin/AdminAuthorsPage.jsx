import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  User, Search, Filter, Plus, 
  AlertTriangle, ArrowRight, Loader2, BookOpen, X, Pencil, Trash2
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminAuthorsPage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingAuthor, setEditingAuthor] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    bio: '',
    nationality: '',
    genres: []
  });
  const [editFormData, setEditFormData] = useState({});

  const { data: authorsData, isLoading } = useQuery({
    queryKey: ['adminAuthors'],
    queryFn: () => adminApi.getAuthors()
  });

  const authors = authorsData?.data?.authors || authorsData?.authors || [];

  const createMutation = useMutation({
    mutationFn: adminApi.addAuthor,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminAuthors']);
      toast.success('Author added successfully');
      setIsModalOpen(false);
      setFormData({ name: '', bio: '', nationality: '', genres: [] });
    },
    onError: (error) => toast.error(error?.message || 'Failed to add author')
  });

  const updateMutation = useMutation({
    mutationFn: ({ authorId, payload }) => adminApi.updateAuthor(authorId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminAuthors']);
      toast.success('Author updated');
      setIsEditModalOpen(false);
    },
    onError: (error) => toast.error(error?.message || 'Failed to update author')
  });

  const deleteMutation = useMutation({
    mutationFn: (authorId) => adminApi.deleteAuthor(authorId),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminAuthors']);
      toast.success('Author deleted');
    },
    onError: (error) => toast.error(error?.message || 'Failed to delete author')
  });

  const filteredAuthors = authors.filter(a => 
    a.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    a.nationality?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleCreate = (e) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const handleUpdate = (e) => {
    e.preventDefault();
    updateMutation.mutate({ authorId: editingAuthor.id, payload: editFormData });
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Author Management</h1>
            <p className="text-gray-500 text-sm mt-1">Manage author catalog, bios, and stats.</p>
          </div>
          <div className="flex items-center space-x-4">
            <div className="bg-white border border-gray-200 px-6 py-3 rounded-lg">
              <div className="text-center">
                <div className="text-2xl font-bold text-gray-900">{authors.length}</div>
                <div className="text-xs font-medium text-gray-500 uppercase tracking-wider">Total Author</div>
              </div>
            </div>
            <button 
              onClick={() => setIsModalOpen(true)}
              className="flex items-center space-x-2 bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors text-xs">
              <Plus size={18} />
              <span>Add Author</span>
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search authors by name or nationality..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-red-500 font-medium"
          />
        </div>

        {/* Authors Table */}
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Author ID</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                <th className="px-6 py-4 text-left text-xs font-medium text-gray-500 uppercase">Nationality</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-500 uppercase">Books</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-500 uppercase">Avg Rating</th>
                <th className="px-6 py-4 text-right text-xs font-medium text-gray-500 uppercase">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan="6" className="py-20 text-center">
                    <Loader2 className="animate-spin text-red-500 mx-auto" size={32} />
                  </td>
                </tr>
              ) : filteredAuthors.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-20 text-center text-gray-400">
                    No authors found.
                  </td>
                </tr>
              ) : (
                filteredAuthors.map((author) => (
                  <tr key={author.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4">
                      <span className="font-mono text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">{author.id}</span>
                    </td>
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-bold text-gray-900">{author.name}</p>
                        <p className="text-sm text-gray-500">{author.genres?.join(', ') || 'No genres'}</p>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant="gray">{author.nationality || 'Unknown'}</Badge>
                    </td>
                    <td className="px-6 py-4 text-right font-mono text-sm">{author.totalBooks}</td>
                    <td className="px-6 py-4 text-right">
                      <Badge variant="teal">{author.averageRating.toFixed(1)}</Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="inline-flex items-center gap-1">
                        <button
                          onClick={() => {
                            setEditingAuthor(author);
                            setEditFormData({
                              bio: author.bio,
                              nationality: author.nationality,
                              genres: author.genres
                            });
                            setIsEditModalOpen(true);
                          }}
                          className="p-2 hover:bg-gray-100 rounded"
                          title="Edit">
                          <Pencil size={16} />
                        </button>
                        <button
                          onClick={() => deleteMutation.mutate(author.id)}
                          className="p-2 hover:bg-red-100 rounded text-red-500"
                          title="Delete">
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

      {/* Add Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-8 max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold">Add New Author</h2>
              <button onClick={() => setIsModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold mb-2">Name *</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                  placeholder="Author name"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2">Bio</label>
                <textarea
                  value={formData.bio}
                  onChange={(e) => setFormData({...formData, bio: e.target.value})}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                  placeholder="Author biography"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2">Nationality</label>
                <input
                  type="text"
                  value={formData.nationality}
                  onChange={(e) => setFormData({...formData, nationality: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                  placeholder="e.g. British"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2">Genres (comma separated)</label>
                <input
                  type="text"
                  value={formData.genres.join(', ')}
                  onChange={(e) => setFormData({...formData, genres: e.target.value.split(',').map(g => g.trim())})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                  placeholder="Fiction, Fantasy"
                />
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 py-2 px-4 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 py-2 px-4 bg-gray-900 text-white rounded-md hover:bg-black font-bold disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {createMutation.isPending ? <Loader2 className="animate-spin w-4 h-4" /> : <Plus className="w-4 h-4" />}
                  Add Author
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {isEditModalOpen && editingAuthor && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl p-8 max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-2xl font-bold">Edit {editingAuthor.name}</h2>
              <button onClick={() => setIsEditModalOpen(false)} className="text-gray-400 hover:text-gray-600">
                <X size={24} />
              </button>
            </div>
            <form onSubmit={handleUpdate} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold mb-2">Name</label>
                <input
                  type="text"
                  value={editingAuthor.name}
                  disabled
                  className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2">Bio</label>
                <textarea
                  value={editFormData.bio || ''}
                  onChange={(e) => setEditFormData({...editFormData, bio: e.target.value})}
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                />
              </div>
              <div>
                <label className="block text-sm font-semibold mb-2">Nationality</label>
                <input
                  type="text"
                  value={editFormData.nationality || ''}
                  onChange={(e) => setEditFormData({...editFormData, nationality: e.target.value})}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                />
              </div>
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="flex-1 py-2 px-4 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="flex-1 py-2 px-4 bg-gray-900 text-white rounded-md hover:bg-black font-bold disabled:opacity-50 flex items-center justify-center gap-2"
                >
                  {updateMutation.isPending ? <Loader2 className="animate-spin w-4 h-4" /> : <Pencil className="w-4 h-4" />}
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </PageTransition>
  );
};

export default AdminAuthorsPage;

