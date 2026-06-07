import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import {
  Book, Loader2, Search
} from 'lucide-react';
import { format } from 'date-fns';
import { toast } from 'react-hot-toast';

const StaffFinesPage = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');

  const { data: finesData, isLoading } = useQuery({
    queryKey: ['staffFines'],
    queryFn: staffApi.getRecentFines
  });

  const collectMutation = useMutation({
    mutationFn: (fine) => staffApi.collectFine(fine.fineId, fine.totalAmount),
    onSuccess: () => {
      queryClient.invalidateQueries(['staffFines']);
      toast.success('Collection recorded successfully');
    }
  });

  const fines = finesData?.fines || [];

  // Filter fines by search query
  const filteredFines = fines.filter(fine => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      fine.memberId?.toLowerCase().includes(query) ||
      fine.memberName?.toLowerCase().includes(query) ||
      fine.bookTitle?.toLowerCase().includes(query) ||
      fine.fineId?.toLowerCase().includes(query)
    );
  });

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div>
          <h1 className="text-3xl font-display font-bold">Revenue Operations</h1>
          <p className="text-gray-500 text-sm mt-1">Manage counter-level fine collections and outstanding balances.</p>
          <div className="mt-4 relative max-w-md">
            <Search size={16} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search by Member ID, Name, Book Title, or Fine ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 text-sm focus:outline-none focus:border-teal-500"
            />
          </div>
        </div>

        <div className="bg-white border border-gray-200">
           <div className="grid grid-cols-12 gap-4 px-6 py-4 bg-gray-50 border-b border-gray-200 text-[10px] font-mono uppercase tracking-widest text-gray-500">
            <div className="col-span-4">Member Info</div>
            <div className="col-span-3">Asset Conflict</div>
            <div className="col-span-2 font-center">Amount</div>
            <div className="col-span-2">Generated On</div>
            <div className="col-span-1"></div>
          </div>

          <div className="divide-y divide-gray-100">
            {isLoading ? (
              <div className="py-20 text-center"><Loader2 className="animate-spin text-teal-500 mx-auto" size={32} /></div>
            ) : filteredFines.length === 0 ? (
              <div className="py-20 text-center text-gray-400">
                {searchQuery ? 'No matching fines found.' : 'No recent fines waiting for collection.'}
              </div>
            ) : (
              filteredFines.map((fine) => (
                <div key={fine.fineId} className="grid grid-cols-12 gap-4 px-6 py-6 items-center hover:bg-gray-50/50 transition-colors group">
                  <div className="col-span-4 flex items-center space-x-4">
                    <div className="w-10 h-10 bg-teal-50 text-teal-600 flex items-center justify-center font-bold">
                      {fine.memberName?.[0] || '?'}
                    </div>
                    <div>
                      <p className="font-bold text-gray-900">{fine.memberName || 'Unknown'}</p>
                      <p className="text-xs text-gray-500 font-mono">{fine.memberId}</p>
                    </div>
                  </div>

                  <div className="col-span-3 text-xs text-gray-500 flex items-center">
                    <Book size={12} className="mr-2 opacity-50" />
                    <span className="line-clamp-1">{fine.bookTitle}</span>
                  </div>

                  <div className="col-span-2 font-mono font-bold text-red-500 text-lg">
                    ₹{(fine.totalAmount ?? 0).toFixed(2)}
                  </div>

                  <div className="col-span-2 text-xs text-gray-600">
                    {fine.createdAt ? (
                      <div>
                        <div className="font-medium">{format(new Date(fine.createdAt), 'MMM dd, yyyy')}</div>
                        <div className="text-[10px] text-gray-400">{format(new Date(fine.createdAt), 'HH:mm')}</div>
                      </div>
                    ) : (
                      <span className="text-gray-400">--</span>
                    )}
                  </div>

                  <div className="col-span-1 text-right">
                    <button 
                      onClick={() => collectMutation.mutate(fine)}
                      className="px-4 py-2 bg-gray-900 text-white font-bold uppercase text-[10px] tracking-widest hover:bg-teal-600 transition-all opacity-0 group-hover:opacity-100"
                    >
                      Collect
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffFinesPage;
