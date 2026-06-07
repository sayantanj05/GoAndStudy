import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import ConfirmModal from '../../components/shared/ConfirmModal';
import {
  DollarSign, Book, Loader2, RefreshCw, Search
} from 'lucide-react';
import { format } from 'date-fns';
import { toast } from 'react-hot-toast';

const AdminFinesPage = () => {
  const queryClient = useQueryClient();
  const [isWaiveOpen, setIsWaiveOpen] = useState(false);
  const [selectedFine, setSelectedFine] = useState(null);
  const [statusFilter, setStatusFilter] = useState(''); // '' = all, 'Pending', 'Paid', 'Waived'
  const [searchQuery, setSearchQuery] = useState('');

  const { data: finesData, isLoading } = useQuery({
    queryKey: ['adminFines', statusFilter],
    queryFn: () => adminApi.getFines(statusFilter)
  });

  const waiveMutation = useMutation({
    mutationFn: (id) => adminApi.waiveFine(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminFines']);
      toast.success('Fine successfully waived');
      setIsWaiveOpen(false);
    }
  });

  const payMutation = useMutation({
    mutationFn: (id) => adminApi.payFine(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries(['adminFines']);
      const amount = data?.data?.data?.collectedAmount || 0;
      toast.success(`Fine of ₹${amount.toFixed(2)} marked as paid`);
    },
    onError: (error) => {
      toast.error('Failed to mark fine as paid: ' + error.message);
    }
  });

  const detectOverduesMutation = useMutation({
    mutationFn: () => adminApi.detectOverdues(),
    onSuccess: (data) => {
      queryClient.invalidateQueries(['adminFines']);
      const count = data?.data?.data?.overdueLoansFound || 0;
      if (count > 0) {
        toast.success(`Detected ${count} overdue loans and created fine records`);
      } else {
        toast.success('No new overdue loans found');
      }
    },
    onError: (error) => {
      toast.error('Failed to detect overdues: ' + error.message);
    }
  });

  const responseFromServer = finesData?.data?.data || finesData?.data || [];
  const fines = (Array.isArray(responseFromServer) ? responseFromServer : responseFromServer?.fines) || [];

  // Get totals from API response
  const totalOutstanding = responseFromServer?.totalOutstanding ??
    fines.filter(f => f.status === 'Pending').reduce((sum, f) => sum + (f.totalAmount || 0), 0);
  const totalPaid = responseFromServer?.totalPaid ??
    fines.filter(f => f.status === 'Paid').reduce((sum, f) => sum + (f.totalAmount || 0), 0);
  const totalWaived = responseFromServer?.totalWaived ??
    fines.filter(f => f.status === 'Waived').reduce((sum, f) => sum + (f.totalAmount || 0), 0);

  // Filter fines by search query
  const filteredFines = fines.filter(fine => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      fine.memberId?.toLowerCase().includes(query) ||
      fine.bookTitle?.toLowerCase().includes(query) ||
      fine.fineId?.toLowerCase().includes(query)
    );
  });

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Financial Oversight</h1>
            <p className="text-gray-500 text-sm mt-1">Audit outstanding balances and settle fine accounts.</p>
            <button
              onClick={() => detectOverduesMutation.mutate()}
              disabled={detectOverduesMutation.isPending}
              className="mt-3 flex items-center space-x-2 px-4 py-2 bg-teal-600 text-white text-xs font-bold uppercase tracking-widest hover:bg-teal-700 transition-colors disabled:opacity-50"
            >
              <RefreshCw size={14} className={detectOverduesMutation.isPending ? 'animate-spin' : ''} />
              <span>{detectOverduesMutation.isPending ? 'Detecting...' : 'Detect Overdues'}</span>
            </button>
          </div>
          <div className="flex space-x-4">
            <div className="bg-gray-100 px-6 py-4 flex flex-col justify-center border-l-4 border-teal-500">
              <span className="text-[10px] font-mono uppercase tracking-widest text-gray-500">Total Outstanding</span>
              <span className="text-2xl font-display font-bold text-gray-900">₹{totalOutstanding.toFixed(2)}</span>
            </div>
            <div className="bg-gray-100 px-6 py-4 flex flex-col justify-center border-l-4 border-green-500">
              <span className="text-[10px] font-mono uppercase tracking-widest text-gray-500">Amount Paid</span>
              <span className="text-2xl font-display font-bold text-green-600">₹{totalPaid.toFixed(2)}</span>
            </div>
            <div className="bg-gray-100 px-6 py-4 flex flex-col justify-center border-l-4 border-gray-500">
              <span className="text-[10px] font-mono uppercase tracking-widest text-gray-500">Amount Waived</span>
              <span className="text-2xl font-display font-bold text-gray-600">₹{totalWaived.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Search and Filter */}
        <div className="flex items-center justify-between space-x-4">
          <div className="flex items-center space-x-4">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2 border border-gray-200 text-sm focus:outline-none focus:border-teal-500"
            >
              <option value="">All Fines (Past & Present)</option>
              <option value="Pending">Pending (Outstanding)</option>
              <option value="Paid">Paid (Settled)</option>
              <option value="Waived">Waived (Forgiven)</option>
            </select>
            <span className="text-xs text-gray-500">
              {statusFilter === '' ? 'Showing all fine records' : `Filtered by: ${statusFilter}`}
            </span>
          </div>
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search by Member ID, Book Title, or Fine ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 pr-4 py-2 border border-gray-200 text-sm focus:outline-none focus:border-teal-500 w-80"
            />
          </div>
        </div>

        {/* Fines Table */}
        <div className="bg-white border border-gray-200">
          <table className="w-full text-left">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200 text-[10px] font-mono uppercase tracking-widest text-gray-500 px-6 py-4">
                <th className="px-6 py-4">Borrower ID (Member ID)</th>
                <th className="px-6 py-4">Book Context</th>
                <th className="px-6 py-4">Generated On</th>
                <th className="px-6 py-4">Amount</th>
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
              ) : filteredFines.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-20 text-center text-gray-400">
                    <p className="mb-2">No {statusFilter || 'fine'} records found.</p>
                    {statusFilter === '' && (
                      <p className="text-xs">Click "Detect Overdues" above to check for overdue loans and create fine records.</p>
                    )}
                  </td>
                </tr>
              ) : (
                filteredFines.map((fine) => (
                  <tr key={fine.fineId} className="hover:bg-gray-50/50 transition-colors group">
                    <td className="px-6 py-4">
                      <div className="flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">
                          {fine.memberId?.substring(0, 2) || 'U'}
                        </div>
                        <div>
                          <p className="font-mono text-xs text-gray-600">{fine.memberId}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-xs text-gray-500">
                        <Book size={12} className="mr-2" />
                        <span className="line-clamp-1">{fine.bookTitle}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs opacity-60">
                      {fine.createdAt ? format(new Date(fine.createdAt), 'MMM dd, yyyy') : 'N/A'}
                    </td>
                    <td className="px-6 py-4">
                      <span className="font-mono font-bold text-red-500">₹{fine.totalAmount?.toFixed(2)}</span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-block px-2 py-1 text-xs font-semibold rounded ${
                        fine.status === 'Pending' ? 'bg-amber-100 text-amber-700' :
                        fine.status === 'Paid' ? 'bg-green-100 text-green-700' :
                        fine.status === 'Waived' ? 'bg-gray-100 text-gray-600' :
                        'bg-gray-100 text-gray-600'
                      }`}>
                        {fine.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      {fine.status === 'Pending' ? (
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => {
                              if (confirm(`Mark fine of ₹${fine.totalAmount?.toFixed(2)} as paid?`)) {
                                payMutation.mutate(fine.fineId);
                              }
                            }}
                            disabled={payMutation.isPending}
                            className="px-4 py-2 text-[10px] font-bold uppercase tracking-widest border border-green-500 text-green-600 hover:bg-green-500 hover:text-white transition-all disabled:opacity-50"
                          >
                            {payMutation.isPending ? 'Processing...' : 'Paid'}
                          </button>
                          <button
                            onClick={() => {
                              setSelectedFine(fine);
                              setIsWaiveOpen(true);
                            }}
                            className="px-4 py-2 text-[10px] font-bold uppercase tracking-widest border border-gray-200 hover:bg-teal-500 hover:text-white hover:border-teal-500 transition-all"
                          >
                            Waive
                          </button>
                        </div>
                      ) : (
                        <span className={`text-xs font-semibold ${
                          fine.status === 'Paid' ? 'text-green-600' :
                          fine.status === 'Waived' ? 'text-gray-500' : 'text-gray-400'
                        }`}>{fine.status}</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmModal
        isOpen={isWaiveOpen}
        onClose={() => setIsWaiveOpen(false)}
        onConfirm={() => waiveMutation.mutate(selectedFine.fineId)}
        title="Waive Fine"
        message={`Are you sure you want to waive the fine of ₹${selectedFine?.totalAmount?.toFixed(2)} for ${selectedFine?.memberId}?`}
        variant="teal"
        confirmLabel="Confirm Waive"
      />
    </PageTransition>
  );
};

export default AdminFinesPage;
