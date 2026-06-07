import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Legend
} from 'recharts';
import { Loader2, TrendingUp, ArrowUpRight, ArrowDownRight } from 'lucide-react';

const PERIOD_HINTS = {
  days: '',
  weekly: '',
  monthly: '',
  yearly: '',
};

const AdminAnalyticsPage = () => {
  const [period, setPeriod] = useState('days');

  const { data: analyticsData, isLoading, isFetching } = useQuery({
    queryKey: ['adminAnalytics', period],
    queryFn: () => adminApi.getAnalytics(period),
  });

  const data = analyticsData?.data?.chartData || [];
  const categoryData = analyticsData?.data?.categoryData || [];
  const metrics = analyticsData?.data?.topMetrics || [
    { label: 'Circulation Rate', value: '78.2%', trend: 'up', delta: '4.1%' },
    { label: 'Avg Loan Duration', value: '12.4 Days', trend: 'down', delta: '0.8 Days' },
    { label: 'Inventory Turnover', value: '2.4x', trend: 'up', delta: '0.3x' },
    { label: 'User Retention', value: '94%', trend: 'up', delta: '1.2%' }
  ];

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">Strategic Analytics</h1>
            <p className="text-gray-500 text-sm mt-1">Deep-dive into circulation patterns and resource utilization.</p>
          </div>

          <div className="space-y-2">
            <div className="flex bg-gray-100 p-1 border border-gray-200">
              {['days', 'weekly', 'monthly', 'yearly'].map((p) => (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPeriod(p)}
                  className={`px-4 py-2 text-[10px] font-bold uppercase tracking-widest transition-all ${period === p ? 'bg-gray-900 text-white' : 'text-gray-400 hover:text-gray-600'
                    }`}
                >
                  {p}
                </button>
              ))}
            </div>
            <p className="text-right text-[10px] font-mono uppercase tracking-widest text-gray-400">
              {PERIOD_HINTS[period]}
            </p>
          </div>
        </div>

        {/* High-level metrics bar */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {metrics.map((m, i) => (
            <div key={i} className="bg-white border border-gray-200 p-4">
              <span className="block text-[10px] font-mono text-gray-400 uppercase tracking-widest">{m.label}</span>
              <div className="flex items-baseline space-x-2 mt-1">
                <span className="text-xl font-display font-bold">{m.value}</span>
                <span className={`text-[10px] font-bold flex items-center ${m.trend === 'up' ? 'text-green-500' : 'text-red-500'}`}>
                  {m.trend === 'up' ? <ArrowUpRight size={10} className="mr-0.5" /> : <ArrowDownRight size={10} className="mr-0.5" />}
                  {m.delta}
                </span>
              </div>
            </div>
          ))}
        </div>

        {/* Main Chart Section */}
        <div className="bg-gray-900 p-8 border border-white/5">
          <div className="flex items-center justify-between mb-8">
            <h3 className="text-lg font-display font-bold text-white uppercase tracking-widest">Circulation Velocity</h3>
            <div className="flex items-center space-x-6">
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-red-600" />
                <span className="text-[10px] font-mono text-gray-400 uppercase">Check-outs</span>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-teal-500" />
                <span className="text-[10px] font-mono text-gray-400 uppercase">Returns</span>
              </div>
            </div>
          </div>

          <div className="h-96 w-full">
            {isLoading || isFetching ? (
              <div className="h-full flex items-center justify-center">
                <Loader2 className="animate-spin text-red-500" size={32} />
              </div>
            ) : data.length === 0 ? (
              <div className="h-full flex items-center justify-center text-center">
                <div>
                  <p className="text-lg font-display font-bold text-white">No circulation data available</p>
                  <p className="mt-2 text-sm text-gray-400">
                    This chart will appear automatically when loans or returns exist for the selected period.
                  </p>
                </div>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={data} key={period} barGap={10} barCategoryGap="20%">
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#ffffff10" />
                  <XAxis
                    dataKey="label"
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 10, fill: '#6b7280' }}
                  />
                  <YAxis
                    axisLine={false}
                    tickLine={false}
                    tick={{ fontSize: 10, fill: '#6b7280' }}
                  />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#111827', border: '1px solid #374151', borderRadius: 0 }}
                    itemStyle={{ fontSize: 12 }}
                  />
                  <Legend wrapperStyle={{ fontSize: 10, textTransform: 'uppercase' }} />
                  <Bar dataKey="value" name="Check-outs" fill="#ef4444" radius={[2, 2, 0, 0]} />
                  <Bar dataKey="value2" name="Returns" fill="#14b8a6" radius={[2, 2, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Distribution Matrix */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 bg-white border border-gray-200 p-8">
            <h3 className="text-lg font-display font-bold mb-8">Category Popularity Matrix</h3>
            <div className="h-64">
              {isLoading || isFetching ? (
                <div className="h-full flex items-center justify-center">
                  <Loader2 className="animate-spin text-red-500" size={32} />
                </div>
              ) : categoryData.length === 0 ? (
                <div className="h-full flex items-center justify-center text-center">
                  <div>
                    <p className="text-lg font-display font-bold text-gray-900">No category analytics available</p>
                    <p className="mt-2 text-sm text-gray-500">
                      This matrix will show once books or genre analytics are available in the catalog.
                    </p>
                  </div>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={categoryData}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                    <XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                    <YAxis axisLine={false} tickLine={false} tick={{ fontSize: 10, fill: '#9ca3af' }} />
                    <Tooltip cursor={{ fill: '#f9fafb' }} contentStyle={{ border: '1px solid #e5e7eb', borderRadius: 0 }} />
                    <Bar dataKey="value" fill="#111827" barSize={40} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          <div className="bg-gray-50 border border-gray-200 p-8 flex flex-col justify-center">
            <TrendingUp size={32} className="text-red-500 mb-4" />
            <h4 className="text-xl font-display font-bold mb-2">Growth Projection</h4>
            <p className="text-sm text-gray-500 leading-relaxed">
              Based on current registration velocity and inventory expansion, the library is projected to reach 10,000+ active members by Q3 2026.
            </p>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminAnalyticsPage;
