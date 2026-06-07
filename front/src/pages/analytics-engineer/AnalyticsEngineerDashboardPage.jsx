import React from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { analyticsEngineerApi } from '../../api/analyticsEngineer.api';
import PageTransition from '../../components/shared/PageTransition';
import StatCard from '../../components/shared/StatCard';
import { 
  Loader2, RefreshCw, Database, Cpu, Wand2, BarChart3, 
  Activity, AlertTriangle, TrendingUp, Users, BookOpen,
  Clock, CheckCircle, Zap, Shield, FileText, Settings
} from 'lucide-react';

const AnalyticsEngineerDashboardPage = () => {
  const { data, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['analyticsEngineerDashboard'],
    queryFn: analyticsEngineerApi.getDashboard,
    staleTime: 60_000,
  });

  const dashboard = data?.data || data || {};

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Analytics Engineer Portal</h1>
            <p className="text-gray-500 text-sm mt-1">ETL, AI pipeline QA, data quality monitoring, and analytics operations control center.</p>
          </div>

          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-2 hover:bg-gray-100 transition-colors rounded-none border border-gray-200"
          >
            <RefreshCw size={18} className={isFetching ? 'animate-spin' : ''} />
          </button>
        </div>

        {isLoading && !dashboard ? (
          <div className="h-64 flex items-center justify-center">
            <Loader2 className="animate-spin text-orange-500" size={48} />
          </div>
        ) : (
          <>
            {/* Key Performance Indicators */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <StatCard
                title="Pipeline Health"
                value={`${dashboard.pipelineHealth || 0}%`}
                trend="up"
                delta={5}
                icon="Activity"
                color="orange"
                portal="analyticsEngineer"
                variant="light"
              />
              <StatCard
                title="Data Quality"
                value={`${dashboard.qualityScore || 95}%`}
                trend="up"
                delta={2}
                icon="Shield"
                color="green"
                portal="analyticsEngineer"
                variant="light"
              />
              <StatCard
                title="ETL Jobs"
                value={dashboard.etlJobsRunning || 3}
                trend="stable"
                delta={0}
                icon="Clock"
                color="blue"
                portal="analyticsEngineer"
                variant="light"
              />
              <StatCard
                title="Anomalies"
                value={dashboard.totalAnomalies || 7}
                trend="down"
                delta={-3}
                icon="AlertTriangle"
                color="red"
                portal="analyticsEngineer"
                variant="light"
              />
            </div>

            {/* Operations Overview */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Pipeline Status */}
              <div className="bg-white border border-gray-200 p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Activity size={18} className="text-orange-500" />
                  <h3 className="text-lg font-display font-bold">Pipeline Status</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Change Stream</span>
                    <span className={`px-2 py-1 text-xs font-bold uppercase ${
                      dashboard.changeStreamStatus === 'RUNNING' 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {dashboard.changeStreamStatus || 'UNKNOWN'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Collections Synced</span>
                    <span className="text-sm font-bold text-green-600">{dashboard.collectionsSynced || 0}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">DLQ Items</span>
                    <span className={`text-sm font-bold ${(dashboard.dlqPending || 0) > 0 ? 'text-red-600' : 'text-green-600'}`}>
                      {dashboard.dlqPending || 0}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Throughput</span>
                    <span className="text-sm font-bold">{dashboard.throughput || '0 rec/sec'}</span>
                  </div>
                </div>
              </div>

              {/* Data Quality Metrics */}
              <div className="bg-white border border-gray-200 p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Shield size={18} className="text-green-500" />
                  <h3 className="text-lg font-display font-bold">Data Quality</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Completeness</span>
                    <span className="text-sm font-bold">98%</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Accuracy</span>
                    <span className="text-sm font-bold">92%</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Consistency</span>
                    <span className="text-sm font-bold">96%</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Validation Rules</span>
                    <span className="text-sm font-bold">{dashboard.activeRules || 24}</span>
                  </div>
                </div>
              </div>

              {/* System Performance */}
              <div className="bg-white border border-gray-200 p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Cpu size={18} className="text-purple-500" />
                  <h3 className="text-lg font-display font-bold">System Performance</h3>
                </div>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">CPU Usage</span>
                    <span className="text-sm font-bold">{dashboard.cpuUsage || '45%'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Memory</span>
                    <span className="text-sm font-bold">{dashboard.memoryUsage || '2.1GB'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Avg Latency</span>
                    <span className="text-sm font-bold">{dashboard.avgLatency || '245ms'}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Error Rate</span>
                    <span className="text-sm font-bold text-red-600">{dashboard.errorRate || '0.12%'}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Recent Activity & Quick Actions */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Recent Activity */}
              <div className="bg-white border border-gray-200 p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Clock size={18} className="text-blue-500" />
                  <h3 className="text-lg font-display font-bold">Recent Activity</h3>
                </div>
                <div className="space-y-3">
                  {[
                    { time: '2 mins ago', action: 'Member sync completed', status: 'success' },
                    { time: '15 mins ago', action: 'Data quality check passed', status: 'success' },
                    { time: '1 hour ago', action: 'ETL job failed: dim_book', status: 'error' },
                    { time: '2 hours ago', action: 'Pipeline restarted', status: 'warning' }
                  ].map((activity, index) => (
                    <div key={index} className="flex items-start gap-3 p-3 border border-gray-100">
                      <div className={`w-2 h-2 rounded-full mt-1.5 ${
                        activity.status === 'success' ? 'bg-green-500' :
                        activity.status === 'error' ? 'bg-red-500' : 'bg-yellow-500'
                      }`} />
                      <div className="flex-1">
                        <p className="text-sm font-medium">{activity.action}</p>
                        <p className="text-xs text-gray-500">{activity.time}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quick Actions */}
              <div className="bg-white border border-gray-200 p-6">
                <div className="flex items-center gap-2 mb-4">
                  <Zap size={18} className="text-orange-500" />
                  <h3 className="text-lg font-display font-bold">Quick Actions</h3>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <button className="p-4 border border-gray-200 hover:bg-gray-50 transition-colors text-left">
                    <Activity size={18} className="text-orange-500 mb-2" />
                    <p className="text-sm font-medium">Start Pipeline</p>
                    <p className="text-xs text-gray-500">Begin data sync</p>
                  </button>
                  <button className="p-4 border border-gray-200 hover:bg-gray-50 transition-colors text-left">
                    <RefreshCw size={18} className="text-blue-500 mb-2" />
                    <p className="text-sm font-medium">Trigger Sync</p>
                    <p className="text-xs text-gray-500">Force resync</p>
                  </button>
                  <button className="p-4 border border-gray-200 hover:bg-gray-50 transition-colors text-left">
                    <FileText size={18} className="text-green-500 mb-2" />
                    <p className="text-sm font-medium">View Logs</p>
                    <p className="text-xs text-gray-500">System logs</p>
                  </button>
                  <button className="p-4 border border-gray-200 hover:bg-gray-50 transition-colors text-left">
                    <Settings size={18} className="text-purple-500 mb-2" />
                    <p className="text-sm font-medium">Settings</p>
                    <p className="text-xs text-gray-500">Configure</p>
                  </button>
                </div>
              </div>
            </div>

            {/* Analytics Insights */}
            <div className="bg-white border border-gray-200 p-6">
              <div className="flex items-center gap-2 mb-4">
                <BarChart3 size={18} className="text-red-500" />
                <h3 className="text-lg font-display font-bold">Analytics Insights</h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="text-center">
                  <div className="text-2xl font-bold text-orange-600">{dashboard.totalMembers || 0}</div>
                  <p className="text-sm text-gray-600">Total Members</p>
                  <p className="text-xs text-green-600 mt-1">↑ 12% this month</p>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">{dashboard.activeLoans || 0}</div>
                  <p className="text-sm text-gray-600">Active Loans</p>
                  <p className="text-xs text-green-600 mt-1">↑ 5% this week</p>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-teal-600">{dashboard.aiConversionRate || 0}%</div>
                  <p className="text-sm text-gray-600">AI Conversion Rate</p>
                  <p className="text-xs text-green-600 mt-1">↑ 8% improvement</p>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </PageTransition>
  );
};

export default AnalyticsEngineerDashboardPage;

