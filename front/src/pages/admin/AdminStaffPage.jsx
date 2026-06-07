import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import { roleApi } from '../../api/role.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import ConfirmModal from '../../components/shared/ConfirmModal';
import { 
  Users, Plus, Search, MoreVertical, 
  Mail, Phone, Shield, UserX, UserCheck, 
  Trash2, Loader2, Edit2, Eye, EyeOff
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminStaffPage = () => {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState(null);
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  
  const [formData, setFormData] = useState({
    staffId: '',
    name: '',
    email: '',
    phone: '',
    role: '',
    temporaryPassword: ''
  });

  const { data: staffData, isLoading } = useQuery({
    queryKey: ['adminStaff'],
    queryFn: adminApi.getStaff
  });

  const { data: rolesData } = useQuery({
    queryKey: ['adminRoles'],
    queryFn: roleApi.getRoles
  });

  const createMutation = useMutation({
    mutationFn: adminApi.createStaff,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminStaff']);
      toast.success('Staff member created');
      setIsModalOpen(false);
      setFormData({ staffId: '', name: '', email: '', phone: '', role: '', temporaryPassword: '' });
    }
  });

  const toggleMutation = useMutation({
    mutationFn: adminApi.toggleStaffStatus,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminStaff']);
      toast.success('Staff status updated');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteStaff,
    onSuccess: () => {
      queryClient.invalidateQueries(['adminStaff']);
      toast.success('Staff member deleted');
    }
  });

  const responseFromServer = staffData?.data?.data || staffData?.data || [];
  const staff = (Array.isArray(responseFromServer) ? responseFromServer : responseFromServer?.staff) || [];

  const filteredStaff = staff.filter((s) => 
    (s.name || '').toLowerCase().includes(searchTerm.toLowerCase()) || 
    (s.email || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Staff Management</h1>
            <p className="text-gray-500 text-sm mt-1">Control access levels and manage internal operations personnel.</p>
          </div>
          <button 
            onClick={() => setIsModalOpen(true)}
            className="flex items-center space-x-2 bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors"
          >
            <Plus size={18} />
            <span>Add Member</span>
          </button>
        </div>

        {/* Filters */}
        <div className="relative max-w-md">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
          <input
            type="text"
            placeholder="Search by name or email..."
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
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Personnel</th>
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Contact</th>
                <th className="px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500">Status</th>
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
              ) : filteredStaff.length === 0 ? (
                <tr>
                  <td colSpan="4" className="py-20 text-center text-gray-400">
                    No staff members found matching your search.
                  </td>
                </tr>
              ) : (
                filteredStaff.map((staff) => (
                  <tr key={staff.id} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-gray-100 flex items-center justify-center font-bold text-gray-700">
                          {(staff.name?.[0] || '?').toUpperCase()}
                        </div>
                        <div>
                          <p className="font-bold text-gray-900 leading-none">{staff.name}</p>
                          <p className="text-[10px] uppercase font-mono text-gray-400 mt-1">ID: {staff.id.slice(0, 8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="space-y-1">
                        <div className="flex items-center text-xs text-gray-600">
                          <Mail size={12} className="mr-2 opacity-50" /> {staff.email}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={staff.active ? 'teal' : 'red'}>
                        {staff.active ? 'Active' : 'Disabled'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button 
                          onClick={() => toggleMutation.mutate(staff.id)}
                          title={staff.active ? 'Deactivate' : 'Activate'}
                          className={`p-2 border border-gray-200 hover:bg-white transition-colors ${staff.active ? 'text-amber-600' : 'text-teal-600'}`}
                        >
                          {staff.active ? <UserX size={16} /> : <UserCheck size={16} />}
                        </button>
                        <button 
                          onClick={() => {
                            setSelectedStaff(staff);
                            setIsConfirmOpen(true);
                          }}
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

      {/* Create Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm">
          <div className="bg-white w-full max-w-md p-8 shadow-2xl relative">
            <h3 className="text-2xl font-display font-bold mb-6">Create Personnel</h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Staff ID</label>
                <input
                  type="text"
                  required
                  value={formData.staffId}
                  onChange={(e) => setFormData({...formData, staffId: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                  placeholder="e.g., STAFF001"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Full Name</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Email Address</label>
                <input
                  type="email"
                  required
                  value={formData.email}
                  onChange={(e) => setFormData({...formData, email: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Phone Number</label>
                <input
                  type="tel"
                  required
                  value={formData.phone}
                  onChange={(e) => setFormData({...formData, phone: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                  placeholder="e.g., +1234567890"
                />
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Role</label>
                <select
                  required
                  value={formData.role || ''}
                  onChange={(e) => setFormData({...formData, role: e.target.value})}
                  className="w-full px-4 py-2 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                >
                  <option value="">Select a role</option>
                  {rolesData?.roles?.map((role) => (
                    <option key={role.id} value={role.name}>
                      {role.name}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Initial Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    required
                    minLength="6"
                    value={formData.temporaryPassword}
                    onChange={(e) => setFormData({...formData, temporaryPassword: e.target.value})}
                    className="w-full px-4 py-2 pr-10 bg-gray-50 border-b-2 border-gray-200 focus:border-red-500 focus:outline-none transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
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
                  disabled={createMutation.isLoading}
                  className="flex-1 px-4 py-3 bg-gray-900 text-white font-bold uppercase text-xs tracking-widest hover:bg-black transition-colors disabled:opacity-50"
                >
                  {createMutation.isLoading ? 'Creating...' : 'Create Staff'}
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
        onConfirm={() => deleteMutation.mutate(selectedStaff?.id)}
        title="Delete Personnel"
        message={`Are you sure you want to permanently remove ${selectedStaff?.name}? This action cannot be undone.`}
        variant="red"
      />
    </PageTransition>
  );
};

export default AdminStaffPage;
