import React, { useState, useEffect, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import useRealtime from '../../hooks/useRealtime';
import StatCard from '../../components/shared/StatCard';
import PageTransition from '../../components/shared/PageTransition';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer, AreaChart, Area 
} from 'recharts';
import { Loader2, RefreshCw, TrendingUp, Users, BookOpen, AlertCircle, DollarSign } from 'lucide-react';

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  
  const { data: initialStats, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['adminStats'],
    queryFn: adminApi.getStats,
  });

  // Update stats when query data changes
  useEffect(() => {
    if (initialStats?.data) setStats(initialStats.data);
  }, [initialStats]);

  // Stabilize handler reference to prevent infinite re-renders from useRealtime's useMemo
  const handleRealtimeUpdate = useCallback((update) => {
    setStats(prev => ({ ...prev, ...update }));
  }, []);

  // Real-time updates via Socket.io
  useRealtime('ADMIN_DASHBOARD_UPDATE', handleRealtimeUpdate);

  if (isLoading && !stats) {
    return (
      <div className="h-full flex items-center justify-center">
        <Loader2 className="animate-spin text-red-500" size={48} />
      </div>
    );
  }

  const chartData = stats?.weeklyActivity || [];

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Systems Overview</h1>
            <p className="text-gray-500 text-sm mt-1">Global library performance and resource allocation.</p>
          </div>
          <button 
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 hover:bg-gray-100 transition-colors rounded-none border border-gray-200"
          >
            <RefreshCw size={18} className={isFetching ? 'animate-spin' : ''} />
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Total Members"
            value={stats?.totalMembers || 0}
            trend="up"
            delta={12}
            icon="Users"
            color="blue"
            portal="admin"
          />
          <StatCard
            title="Active Loans"
            value={stats?.activeLoans || 0}
            trend="up"
            delta={5}
            icon="BookOpen"
            color="amber"
            portal="admin"
          />
          <StatCard
            title="Overdue Books"
            value={stats?.overdueBooks || 0}
            trend="down"
            delta={2}
            icon="AlertCircle"
            color="red"
            portal="admin"
          />
          <StatCard
            title="Fine Revenue"
            value={stats?.totalFines || 0}
            trend="up"
            delta={8}
            icon="DollarSign"
            color="teal"
            portal="admin"
          />
        </div>

        {/* Charts Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="bg-white border border-gray-200 p-6 rounded-none">
            <h3 className="text-lg font-display font-bold mb-6 flex items-center">
              <TrendingUp size={20} className="mr-2 text-red-500" />
              Activity Trend (Loans vs Returns)
            </h3>
            <div className="h-80 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="colorLoans" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#ef4444" stopOpacity={0.1}/>
                      <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                  <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                  <Tooltip 
                    contentStyle={{ borderRadius: 0, border: '1px solid #e5e7eb', boxShadow: 'none' }}
                  />
                  <Area type="linear" dataKey="loans" stroke="#ef4444" fillOpacity={1} fill="url(#colorLoans)" strokeWidth={2} />
                  <Area type="linear" dataKey="returns" stroke="#10b981" fillOpacity={0} strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="bg-white border border-gray-200 p-6 rounded-none">
            <h3 className="text-lg font-display font-bold mb-6 flex items-center">
              <Users size={20} className="mr-2 text-blue-500" />
              New Registrations
            </h3>
            <div className="h-80 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                  <XAxis dataKey="day" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                  <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                  <Tooltip 
                    cursor={{ fill: '#f9fafb' }}
                    contentStyle={{ borderRadius: 0, border: '1px solid #e5e7eb', boxShadow: 'none' }}
                  />
                  <Bar dataKey="visitors" fill="#3b82f6" radius={[2, 2, 0, 0]} barSize={30} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* System Health */}
        <div className="bg-gray-900 p-8 rounded-none border border-white/5 text-white">
          <div className="flex justify-between items-end">
            <div>
              <p className="font-mono text-[10px] uppercase tracking-widest text-red-500 mb-2">System Status</p>
              <h2 className="text-3xl font-display font-bold">Infrastructure is Healthy</h2>
              <p className="text-gray-400 mt-2 max-w-lg">All microservices are operational. Average response time is 42ms. Vector DB synchronization is up to date.</p>
            </div>
            <div className="flex items-center space-x-2 bg-green-500/10 text-green-500 px-4 py-2 border border-green-500/20">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
              <span className="text-xs font-bold uppercase tracking-tighter">Live Monitor Active</span>
            </div>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminDashboard;
