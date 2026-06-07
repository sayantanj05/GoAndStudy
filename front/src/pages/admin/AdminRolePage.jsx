import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { roleApi } from '../../api/role.api';
import PageTransition from '../../components/shared/PageTransition';
import ConfirmModal from '../../components/shared/ConfirmModal';
import { 
  Shield, Plus, Search, MoreVertical, 
  Edit2, Trash2, Loader2
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminRolePage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  
  const [formData, setFormData] = useState({
    name: '',
    description: ''
  });

  const { data: roleData, isLoading } = useQuery({
    queryKey: ['adminRoles'],
    queryFn: roleApi.getRoles
  });

  const createMutation = useMutation({
    mutationFn: roleApi.createRole,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminRoles']);
      toast.success('Role created successfully');
      setIsModalOpen(false);
      setFormData({ name: '', description: '' });
      setIsEditMode(false);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to create role');
    }
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => roleApi.updateRole(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminRoles']);
      toast.success('Role updated successfully');
      setIsModalOpen(false);
      setFormData({ name: '', description: '' });
      setIsEditMode(false);
      setSelectedRole(null);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to update role');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: roleApi.deleteRole,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminRoles']);
      toast.success('Role deleted successfully');
      setIsConfirmOpen(false);
      setSelectedRole(null);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete role');
    }
  });

  const responseFromServer = roleData?.data || roleData || {};
  const roles = responseFromServer?.roles || [];

  const filteredRoles = roles.filter((role) => 
    (role.name || '').toLowerCase().includes(searchTerm.toLowerCase()) || 
    (role.description || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (isEditMode && selectedRole) {
      updateMutation.mutate({ id: selectedRole.id, data: formData });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleEdit = (role) => {
    setSelectedRole(role);
    setFormData({
      name: role.name,
      description: role.description || ''
    });
    setIsEditMode(true);
    setIsModalOpen(true);
  };

  const handleDelete = (role) => {
    setSelectedRole(role);
    setIsConfirmOpen(true);
  };

  const openCreateModal = () => {
    setFormData({ name: '', description: '' });
    setIsEditMode(false);
    setSelectedRole(null);
    setIsModalOpen(true);
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Role Management</h1>
            <p className="text-gray-500 text-sm mt-1">Manage system roles and their permissions.</p>
          </div>
          <button 
            onClick={openCreateModal}
            className="flex items-center space-x-2 bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors"
          >
            <Plus size={18} />
            <span>Add Role</span>
          </button>
        </div>

        {/* Search */}
        <div className="relative max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search roles..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-red-500 font-medium"
          />
        </div>

        {/* Table */}
        <div className="bg-white border border-gray-200 overflow-hidden">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Role Name</th>
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Description</th>
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Created</th>
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                <tr>
                  <td colSpan="4" className="py-20 text-center">
                    <Loader2 className="animate-spin text-red-500 mx-auto" size={32} />
                  </td>
                </tr>
              ) : filteredRoles.length === 0 ? (
                <tr>
                  <td colSpan="4" className="py-20 text-center text-gray-400">
                    No roles found matching your search.
                  </td>
                </tr>
              ) : (
                filteredRoles.map((role) => (
                  <tr key={role.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-gray-100 flex items-center justify-center">
                          <Shield size={20} className="text-gray-600" />
                        </div>
                        <div>
                          <p className="font-bold text-gray-900 leading-none">{role.name}</p>
                          <p className="text-[10px] uppercase font-mono text-gray-400 mt-1">ID: {role.id.slice(0, 8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm text-gray-600 line-clamp-2">
                        {role.description || 'No description provided'}
                      </p>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-sm text-gray-600">
                        {new Date(role.createdAt).toLocaleDateString()}
                      </p>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button 
                          onClick={() => handleEdit(role)}
                          className="p-2 border border-gray-200 hover:bg-white transition-colors text-gray-600"
                        >
                          <Edit2 size={16} />
                        </button>
                        <button 
                          onClick={() => handleDelete(role)}
                          className="p-2 border border-gray-200 text-red-600 hover:bg-red-50 transition-colors"
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

      {/* Create/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm">
          <div className="bg-white w-full max-w-md p-8 shadow-2xl relative">
            <h3 className="text-2xl font-display font-bold mb-6">
              {isEditMode ? 'Edit Role' : 'Create Role'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Role Name</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                  placeholder="e.g., Librarian"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Description</label>
                <textarea
                  rows="4"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors resize-none"
                  placeholder="Describe the role and its responsibilities..."
                />
              </div>
              <div className="pt-6 flex space-x-3">
                <button 
                  type="button" 
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 px-4 py-3 border border-gray-200 font-bold uppercase text-xs tracking-widest hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  disabled={createMutation.isLoading || updateMutation.isLoading}
                  className="flex-1 px-4 py-3 bg-gray-900 text-white font-bold uppercase text-xs tracking-widest hover:bg-black transition-colors disabled:opacity-50"
                >
                  {createMutation.isLoading || updateMutation.isLoading 
                    ? (isEditMode ? 'Updating...' : 'Creating...') 
                    : (isEditMode ? 'Update Role' : 'Create Role')
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirm Delete */}
      <ConfirmModal
        isOpen={isConfirmOpen}
        onClose={() => setIsConfirmOpen(false)}
        onConfirm={() => deleteMutation.mutate(selectedRole?.id)}
        title="Delete Role"
        message={`Are you sure you want to permanently delete the role "${selectedRole?.name}"? This action cannot be undone.`}
        variant="red"
      />
    </PageTransition>
  );
};

export default AdminRolePage;
