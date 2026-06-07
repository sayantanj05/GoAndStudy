import React from 'react';
import { useQuery } from '@tanstack/react-query';
import useAuthStore from '../../store/authStore';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import {
  Shield, Mail, CreditCard, LogOut,
  Loader2, PlusCircle, RefreshCw, BookOpen, Clock
} from 'lucide-react';

const StaffProfilePage = () => {
  const { user, logout } = useAuthStore();
  const { data: statsData, isLoading } = useQuery({
    queryKey: ['staffStats'],
    queryFn: staffApi.getStats
  });

  const stats = statsData?.data || {};

  return (
    <PageTransition>
      <div className="p-8 max-w-4xl space-y-8">
        <div className="flex items-center space-x-8">
          <div className="w-32 h-32 bg-teal-50 text-teal-600 flex items-center justify-center font-bold text-4xl border-2 border-teal-600">
            {user?.name?.[0] || 'S'}
          </div>
          <div>
            <h1 className="text-4xl font-display font-bold text-gray-900">{user?.name || 'Staff User'}</h1>
            <div className="flex items-center space-x-2 text-teal-600 mt-2">
              <Shield size={16} />
              <span className="text-xs font-bold uppercase tracking-widest">Library Staff Member</span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="bg-white border border-gray-200 p-8 space-y-6">
            <h3 className="text-lg font-display font-bold uppercase tracking-widest border-b border-gray-100 pb-4">Live Performance Snapshot</h3>
            {isLoading ? (
              <div className="py-10 flex items-center justify-center">
                <Loader2 className="animate-spin text-teal-500" size={28} />
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 bg-gray-50 border-l-4 border-teal-500">
                  <p className="text-[10px] font-mono text-gray-400 uppercase flex items-center gap-2">
                    <PlusCircle size={12} /> Issues Today
                  </p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{stats.issuesToday ?? 0}</p>
                </div>
                <div className="p-4 bg-gray-50 border-l-4 border-blue-500">
                  <p className="text-[10px] font-mono text-gray-400 uppercase flex items-center gap-2">
                    <RefreshCw size={12} /> Returns Today
                  </p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{stats.returnsToday ?? 0}</p>
                </div>
                <div className="p-4 bg-gray-50 border-l-4 border-amber-500">
                  <p className="text-[10px] font-mono text-gray-400 uppercase flex items-center gap-2">
                    <BookOpen size={12} /> Active Loans
                  </p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{stats.booksOnLoan ?? 0}</p>
                </div>
                <div className="p-4 bg-gray-50 border-l-4 border-red-500">
                  <p className="text-[10px] font-mono text-gray-400 uppercase flex items-center gap-2">
                    <Clock size={12} /> Due Today
                  </p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">{stats.pendingReservations ?? 0}</p>
                </div>
              </div>
            )}
          </div>

          <div className="bg-white border border-gray-200 p-8 space-y-6">
            <h3 className="text-lg font-display font-bold uppercase tracking-widest border-b border-gray-100 pb-4">Account Details</h3>
            <div className="space-y-4">
              <div className="flex items-center space-x-4 text-sm">
                <Mail size={16} className="text-gray-400" />
                <span className="text-gray-600">{user?.email || 'No email available'}</span>
              </div>
              <div className="flex items-center space-x-4 text-sm">
                <CreditCard size={16} className="text-gray-400" />
                <span className="text-gray-600">{user?.userId || 'No staff ID available'}</span>
              </div>
              <div className="text-xs text-gray-500">
                This page only shows details available from your active authenticated session and live dashboard data.
              </div>
            </div>
            <button
              onClick={logout}
              className="w-full mt-4 flex items-center justify-center space-x-2 py-3 border-2 border-red-500 text-red-500 font-bold uppercase text-[10px] tracking-widest hover:bg-red-500 hover:text-white transition-all"
            >
              <LogOut size={16} />
              <span>Terminate Session</span>
            </button>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffProfilePage;
