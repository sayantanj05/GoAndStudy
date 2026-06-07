import React from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import useRealtime from '../../hooks/useRealtime';
import StatCard from '../../components/shared/StatCard';
import PageTransition from '../../components/shared/PageTransition';
import {
  PlusCircle, RefreshCw, BookOpen, Clock,
  ArrowRight, CheckCircle, AlertCircle, Loader2
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';

const StaffDashboard = () => {
  const queryClient = useQueryClient();
  const { data: statsData, isLoading } = useQuery({
    queryKey: ['staffStats'],
    queryFn: staffApi.getStats,
    refetchInterval: 15000,
    refetchOnWindowFocus: true,
  });

  useRealtime({
    BOOK_ISSUED: () => queryClient.invalidateQueries({ queryKey: ['staffStats'] }),
    BOOK_RETURNED: () => queryClient.invalidateQueries({ queryKey: ['staffStats'] }),
    OVERDUE_DETECTED: () => queryClient.invalidateQueries({ queryKey: ['staffStats'] }),
    FINE_COLLECTED: () => queryClient.invalidateQueries({ queryKey: ['staffStats'] }),
    NOTIFICATION: () => queryClient.invalidateQueries({ queryKey: ['staffStats'] }),
  });

  const stats = statsData?.data || {};
  const transactions = stats.recentTransactions || [];
  const reminders = stats.reminders || [];

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Operational Command</h1>
            <p className="text-gray-500 text-sm mt-1">Real-time circulation metrics and workforce queue.</p>
          </div>
          <div className="flex space-x-3">
            <Link to="/staff/issue" className="flex items-center bg-teal-600 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-teal-700 transition-colors text-xs">
              <PlusCircle size={18} className="mr-2" /> Issue Book
            </Link>
            <Link to="/staff/return" className="flex items-center bg-gray-900 text-white px-6 py-3 font-bold uppercase tracking-widest hover:bg-black transition-colors text-xs">
              <RefreshCw size={18} className="mr-2" /> Return Book
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Issues Today"
            value={stats.issuesToday ?? 0}
            icon="PlusCircle"
            color="teal"
            portal="staff"
            variant="light"
          />
          <StatCard
            title="Returns Processed"
            value={stats.returnsToday ?? 0}
            icon="RefreshCw"
            color="blue"
            portal="staff"
            variant="light"
          />
          <StatCard
            title="Active Loans"
            value={stats.booksOnLoan ?? 0}
            icon="BookOpen"
            color="amber"
            portal="staff"
            variant="light"
          />
          <StatCard
            title="Due Today"
            value={stats.pendingReservations ?? 0}
            icon="Clock"
            color="red"
            portal="staff"
            variant="light"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            <div className="bg-white border border-gray-200 p-8">
              <h3 className="text-lg font-display font-bold mb-6 uppercase tracking-widest border-b border-gray-100 pb-4">Recent Transactions</h3>
              <div className="space-y-4">
                {isLoading ? (
                  <div className="py-16 flex items-center justify-center">
                    <Loader2 className="animate-spin text-teal-500" size={28} />
                  </div>
                ) : transactions.length === 0 ? (
                  <div className="py-16 text-center text-gray-400">
                    No staff transactions recorded yet.
                  </div>
                ) : (
                  transactions.map((transaction) => (
                    <div
                      key={`${transaction.targetId || transaction.eventType}-${transaction.createdAt || 'now'}`}
                      className="flex items-center justify-between p-4 bg-gray-50 hover:bg-teal-50/50 transition-colors border-l-4 border-teal-500"
                    >
                      <div className="flex items-center space-x-4">
                        <div className="p-2 bg-white text-teal-600">
                          <CheckCircle size={18} />
                        </div>
                        <div>
                          <p className="text-sm font-bold text-gray-900">{transaction.title || transaction.eventType}</p>
                          <p className="text-[10px] font-mono uppercase text-gray-400">
                            {transaction.subtitle || transaction.targetId || 'Recorded in activity log'}
                            {transaction.createdAt
                              ? ` • ${formatDistanceToNow(new Date(transaction.createdAt), { addSuffix: true })}`
                              : ''}
                          </p>
                        </div>
                      </div>
                      <ArrowRight size={16} className="text-gray-300" />
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-gray-900 text-white p-8">
              <h3 className="text-lg font-display font-bold mb-4 uppercase tracking-widest text-teal-500">Live Operations</h3>
              <p className="text-sm text-gray-400 leading-relaxed mb-6">
                This panel is reading live circulation data from database for loans, returns, reminders, and staff activity.
              </p>
              <div className="flex items-center space-x-2 text-green-500 text-xs font-bold uppercase tracking-tighter">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                <span>Database Connected</span>
              </div>
            </div>

            <div className="bg-white border border-gray-200 p-8">
              <h3 className="text-lg font-display font-bold mb-4 uppercase tracking-widest border-b border-gray-100 pb-4">Reminders</h3>
              <div className="space-y-4">
                {isLoading ? (
                  <div className="py-8 flex items-center justify-center">
                    <Loader2 className="animate-spin text-teal-500" size={24} />
                  </div>
                ) : reminders.length === 0 ? (
                  <p className="text-xs text-gray-600">No live reminders in the database right now.</p>
                ) : (
                  reminders.map((reminder) => (
                    <div key={reminder.id} className="flex items-start space-x-3">
                      <AlertCircle size={16} className="text-amber-500 mt-0.5 flex-shrink-0" />
                      <div>
                        <p className="text-xs font-bold text-gray-800">{reminder.title}</p>
                        <p className="text-xs text-gray-600">{reminder.message}</p>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffDashboard;
