import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  Users, Search, Filter, Mail, Calendar, 
  ChevronRight, Loader2, Download, Ban, CheckCircle2, Trash2
} from 'lucide-react';
import { format } from 'date-fns';
import { toast } from 'react-hot-toast';

const AdminMembersPage = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const queryClient = useQueryClient();

  const { data: membersData, isLoading } = useQuery({
    queryKey: ['adminMembers'],
    queryFn: adminApi.getMembers
  });

  const deactivateMutation = useMutation({
    mutationFn: (memberId) => adminApi.deactivateMember(memberId),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminMembers']);
      toast.success('Member deactivated');
    },
    onError: (error) => {
      console.error('Deactivate member failed', error);
      toast.error(error?.message || 'Failed to deactivate member');
    }
  });

  const reactivateMutation = useMutation({
    mutationFn: (memberId) => adminApi.reactivateMember(memberId),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminMembers']);
      toast.success('Member reactivated');
    },
    onError: (error) => {
      console.error('Reactivate member failed', error);
      toast.error(error?.message || 'Failed to reactivate member');
    }
  });

  const deleteMutation = useMutation({
    mutationFn: (memberId) => adminApi.deleteMember(memberId),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminMembers']);
      toast.success('Member deleted');
    },
    onError: (error) => {
      console.error('Delete member failed', error);
      toast.error(error?.message || 'Failed to delete member');
    }
  });

  const responseFromServer = membersData?.data?.data || membersData?.data || [];
  const members = (Array.isArray(responseFromServer) ? responseFromServer : responseFromServer?.members) || [];

  const filteredMembers = members.filter((m) =>
    (m.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (m.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (m.id || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Member Directory</h1>
            <p className="text-gray-500 text-sm mt-1">Monitor the community and oversee user engagement metrics.</p>
          </div>
          <button className="flex items-center space-x-2 bg-white border border-gray-200 px-6 py-3 font-bold uppercase tracking-widest hover:bg-gray-50 transition-colors text-xs">
            <Download size={18} />
            <span>Export List</span>
          </button>
        </div>

        {/* Filters */}
        <div className="flex items-center space-x-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="Filter by name, email or ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3 bg-white border border-gray-200 focus:outline-none focus:border-red-500 font-medium"
            />
          </div>
          <button className="p-3 bg-white border border-gray-200 hover:bg-gray-50 text-gray-500">
            <Filter size={18} />
          </button>
        </div>

        {/* Table/List */}
        <div className="bg-white border border-gray-200">
<div className="grid grid-cols-12 gap-4 px-6 py-4 bg-gray-50 border-b border-gray-200 text-[10px] font-mono uppercase tracking-widest text-gray-500">
             <div className="col-span-3">Member Info</div>
             <div className="col-span-2">Gender / DOB</div>
             <div className="col-span-2">Contact & Pass</div>
             <div className="col-span-2 text-center">Active Loans</div>
             <div className="col-span-2">Joined</div>
             <div className="col-span-1"></div>
           </div>

          <div className="divide-y divide-gray-100">
            {isLoading ? (
              <div className="py-20 text-center">
                <Loader2 className="animate-spin text-red-500 mx-auto" size={32} />
              </div>
            ) : filteredMembers.length === 0 ? (
              <div className="py-20 text-center text-gray-400">
                No members found in the directory.
              </div>
            ) : (
              filteredMembers.map((member) => (
                (() => {
                  const isActive = member?.active ?? member?.isActive ?? true;
                  return (
                <div key={member.id} className="grid grid-cols-12 gap-4 px-6 py-5 items-center hover:bg-gray-50/50 transition-colors group">
                  <div className="col-span-3 flex items-center space-x-4">
                    <div className="w-12 h-12 bg-amber-50 text-amber-700 flex items-center justify-center font-display text-xl font-bold">
                      {(member.name?.[0] || '?').toUpperCase()}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-bold text-gray-900 truncate">{member.name}</p>
                      <p className="text-[10px] font-mono text-gray-400 mt-1 uppercase tracking-tighter">
                        {member.id.slice(0, 12)}
                      </p>
                      {!isActive && (
                        <div className="mt-1">
                          <Badge variant="red">Deactivated</Badge>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="col-span-2 text-xs">
                    <div className="font-semibold text-gray-900 capitalize">{member.gender || 'N/A'}</div>
                    <div className="text-gray-500">{member.dateOfBirth ? format(new Date(member.dateOfBirth), 'MMM dd, yy') : 'N/A'}</div>
                  </div>
                  
<div className="col-span-2 space-y-1">
                     <div className="flex items-center text-xs text-gray-600 truncate">
                       <Mail size={12} className="mr-2 opacity-50" /> {member.email}
                     </div>
<div className="text-xs font-mono text-gray-900 bg-gray-100 px-2 py-1 rounded truncate mt-1" title={member.plainPassword || 'No password stored'}>
                        {member.plainPassword || '—'}
                      </div>
                   </div>

                   <div className="col-span-2 text-center">
                    <div className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-gray-100 text-xs font-bold font-mono">
                      {member.activeLoanCount || 0}
                    </div>
                  </div>

                  <div className="col-span-2">
                    <div className="flex items-center text-xs text-gray-600">
                      <Calendar size={12} className="mr-2 opacity-50" />
                      {member.createdAt ? format(new Date(member.createdAt), 'MMM dd, yyyy') : 'N/A'}
                    </div>
                  </div>

                  <div className="col-span-1 text-right">
                    <div className="inline-flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      {isActive ? (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            const ok = window.confirm(`Deactivate ${member.name}? They will not be able to sign in.`);
                            if (!ok) return;
                            deactivateMutation.mutate(member.id);
                          }}
                          disabled={deactivateMutation.isPending}
                          className="p-2 text-gray-500 hover:text-amber-700 hover:bg-amber-50 transition-all disabled:opacity-50"
                          title="Deactivate"
                        >
                          <Ban size={18} />
                        </button>
                      ) : (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            reactivateMutation.mutate(member.id);
                          }}
                          disabled={reactivateMutation.isPending}
                          className="p-2 text-gray-500 hover:text-teal-700 hover:bg-teal-50 transition-all disabled:opacity-50"
                          title="Reactivate"
                        >
                          <CheckCircle2 size={18} />
                        </button>
                      )}

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          const ok = window.confirm(`Delete ${member.name}? This cannot be undone.`);
                          if (!ok) return;
                          deleteMutation.mutate(member.id);
                        }}
                        disabled={deleteMutation.isPending}
                        className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 transition-all disabled:opacity-50"
                        title="Delete"
                      >
                        <Trash2 size={18} />
                      </button>

                      <button 
                        onClick={(e) => e.stopPropagation()}
                        className="p-2 text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-all" title="Details (coming soon)">
                        <ChevronRight size={20} />
                      </button>
                    </div>
                  </div>
                </div>
                  );
                })()
              ))
            )}
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminMembersPage;
