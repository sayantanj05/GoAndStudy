import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageTransition from '../../components/shared/PageTransition';
import { analyticsEngineerApi } from '../../api/analyticsEngineer.api';
import { 
  Loader2, ToggleLeft, Database, RefreshCw, Play, Pause, 
  AlertTriangle, CheckCircle, Clock, TrendingUp, FileText,
  Settings, Activity, BarChart3, Zap, Shield, Download
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AnalyticsEngineerOperationsPage = () => {
  const queryClient = useQueryClient();
  const [selectedCollection, setSelectedCollection] = useState('dim_book');
  const [activeTab, setActiveTab] = useState('pipelines');

  const { data, isLoading } = useQuery({
    queryKey: ['analyticsEngineerDashboard'],
    queryFn: analyticsEngineerApi.getDashboard,
    staleTime: 60_000,
  });

  const dashboard = data?.data || data || {};

  // Mock mutations for operations
  const startPipelineMutation = useMutation({
    mutationFn: analyticsEngineerApi.startPipeline,
    onSuccess: () => {
      toast.success('Pipeline started successfully');
      queryClient.invalidateQueries(['analyticsEngineerDashboard']);
    },
    onError: () => toast.error('Failed to start pipeline')
  });

  const stopPipelineMutation = useMutation({
    mutationFn: analyticsEngineerApi.stopPipeline,
    onSuccess: () => {
      toast.success('Pipeline stopped successfully');
      queryClient.invalidateQueries(['analyticsEngineerDashboard']);
    },
    onError: () => toast.error('Failed to stop pipeline')
  });

  const triggerSyncMutation = useMutation({
    mutationFn: analyticsEngineerApi.triggerSync,
    onSuccess: () => {
      toast.success('Sync triggered successfully');
      queryClient.invalidateQueries(['analyticsEngineerDashboard']);
    },
    onError: () => toast.error('Failed to trigger sync')
  });

  return (
    <PageTransition>
      <div className="p-8 space-y-6">
        <div>
          <h1 className="text-3xl font-display font-bold">Analytics Operations</h1>
          <p className="text-gray-500 text-sm mt-1">ETL/CDC orchestration, data pipeline management, and analytics operations control center.</p>
        </div>

        {/* Tab Navigation */}
        <div className="flex space-x-1 border-b border-gray-200">
          {[
            { id: 'pipelines', label: 'Data Pipelines', icon: Activity },
            { id: 'etl', label: 'ETL Jobs', icon: Database },
            { id: 'quality', label: 'Data Quality', icon: Shield },
            { id: 'monitoring', label: 'Monitoring', icon: BarChart3 }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center space-x-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-orange-500 text-orange-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <tab.icon size={16} />
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="h-64 flex items-center justify-center">
            <Loader2 className="animate-spin text-orange-500" size={48} />
          </div>
        ) : (
          <>
            {/* Data Pipelines Tab */}
            {activeTab === 'pipelines' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Change Stream Control */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <ToggleLeft size={18} className="text-teal-500" />
                      <h3 className="text-lg font-display font-bold">Change Stream</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Status</span>
                        <span className={`px-2 py-1 text-xs font-bold uppercase ${
                          dashboard.changeStreamStatus === 'RUNNING' 
                            ? 'bg-green-100 text-green-800' 
                            : 'bg-red-100 text-red-800'
                        }`}>
                          {dashboard.changeStreamStatus || 'UNKNOWN'}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Events Processed</span>
                        <span className="text-sm font-bold">{dashboard.eventsProcessed || 0}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Last Event</span>
                        <span className="text-sm font-mono">{dashboard.lastEventTime || '—'}</span>
                      </div>
                    </div>
                    <div className="mt-4 flex items-center gap-3">
                      <button 
                        onClick={() => startPipelineMutation.mutate()}
                        disabled={startPipelineMutation.isLoading}
                        className="bg-teal-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-teal-700 transition-colors disabled:opacity-50 flex items-center gap-2"
                      >
                        <Play size={14} /> Start
                      </button>
                      <button 
                        onClick={() => stopPipelineMutation.mutate()}
                        disabled={stopPipelineMutation.isLoading}
                        className="bg-gray-900 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-black transition-colors disabled:opacity-50 flex items-center gap-2"
                      >
                        <Pause size={14} /> Stop
                      </button>
                    </div>
                  </div>

                  {/* Pipeline Health */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Activity size={18} className="text-orange-500" />
                      <h3 className="text-lg font-display font-bold">Pipeline Health</h3>
                    </div>
                    <div className="space-y-3">
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
                        <span className="text-sm text-gray-600">Sync Rate</span>
                        <span className="text-sm font-bold">{dashboard.syncRate || '0 rec/sec'}</span>
                      </div>
                    </div>
                    <div className="mt-4">
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div 
                          className="bg-orange-500 h-2 rounded-full transition-all duration-300"
                          style={{ width: `${dashboard.pipelineHealth || 0}%` }}
                        />
                      </div>
                      <p className="text-xs text-gray-500 mt-1">Pipeline Health: {dashboard.pipelineHealth || 0}%</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* ETL Jobs Tab */}
            {activeTab === 'etl' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Batch Sync Control */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Database size={18} className="text-red-500" />
                      <h3 className="text-lg font-display font-bold">Trigger Batch Sync</h3>
                    </div>
                    <p className="text-sm text-gray-600 mb-4">Force resync of a collection</p>
                    <div className="space-y-3">
                      <select
                        value={selectedCollection}
                        onChange={(e) => setSelectedCollection(e.target.value)}
                        className="w-full p-3 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
                      >
                        <option value="dim_book">dim_book</option>
                        <option value="dim_member">dim_member</option>
                        <option value="dim_loan">dim_loan</option>
                        <option value="fact_reading">fact_reading</option>
                      </select>
                      <button 
                        onClick={() => triggerSyncMutation.mutate({ collection: selectedCollection })}
                        disabled={triggerSyncMutation.isLoading}
                        className="w-full bg-orange-600 text-white px-4 py-3 text-xs font-bold uppercase tracking-widest hover:bg-orange-700 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
                      >
                        <RefreshCw size={16} /> Trigger Sync
                      </button>
                    </div>
                  </div>

                  {/* ETL Job Status */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Clock size={18} className="text-blue-500" />
                      <h3 className="text-lg font-display font-bold">ETL Job Status</h3>
                    </div>
                    <div className="space-y-3">
                      {[
                        { name: 'Member Sync', status: 'RUNNING', lastRun: '2 mins ago' },
                        { name: 'Book Sync', status: 'COMPLETED', lastRun: '15 mins ago' },
                        { name: 'Loan Sync', status: 'FAILED', lastRun: '1 hour ago' },
                        { name: 'Analytics Update', status: 'PENDING', lastRun: 'Scheduled' }
                      ].map((job, index) => (
                        <div key={index} className="flex items-center justify-between p-2 border border-gray-100">
                          <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${
                              job.status === 'RUNNING' ? 'bg-green-500 animate-pulse' :
                              job.status === 'COMPLETED' ? 'bg-green-500' :
                              job.status === 'FAILED' ? 'bg-red-500' : 'bg-yellow-500'
                            }`} />
                            <span className="text-sm font-medium">{job.name}</span>
                          </div>
                          <span className="text-xs text-gray-500">{job.lastRun}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Data Quality Tab */}
            {activeTab === 'quality' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  {/* Data Quality Metrics */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Shield size={18} className="text-green-500" />
                      <h3 className="text-lg font-display font-bold">Quality Score</h3>
                    </div>
                    <div className="text-center">
                      <div className="text-4xl font-bold text-green-600">{dashboard.qualityScore || 95}%</div>
                      <p className="text-sm text-gray-600 mt-2">Overall Data Quality</p>
                    </div>
                    <div className="mt-4 space-y-2">
                      <div className="flex justify-between text-sm">
                        <span>Completeness</span>
                        <span className="font-bold">98%</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span>Accuracy</span>
                        <span className="font-bold">92%</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span>Consistency</span>
                        <span className="font-bold">96%</span>
                      </div>
                    </div>
                  </div>

                  {/* Anomaly Detection */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <AlertTriangle size={18} className="text-yellow-500" />
                      <h3 className="text-lg font-display font-bold">Anomalies</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Critical</span>
                        <span className="text-sm font-bold text-red-600">{dashboard.criticalAnomalies || 2}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Warning</span>
                        <span className="text-sm font-bold text-yellow-600">{dashboard.warningAnomalies || 5}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Info</span>
                        <span className="text-sm font-bold text-blue-600">{dashboard.infoAnomalies || 12}</span>
                      </div>
                    </div>
                    <button className="mt-4 w-full bg-yellow-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-yellow-700 transition-colors">
                      View Details
                    </button>
                  </div>

                  {/* Data Validation */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <CheckCircle size={18} className="text-blue-500" />
                      <h3 className="text-lg font-display font-bold">Validation Rules</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Active Rules</span>
                        <span className="text-sm font-bold">{dashboard.activeRules || 24}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Passed Today</span>
                        <span className="text-sm font-bold text-green-600">{dashboard.rulesPassed || 1247}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Failed Today</span>
                        <span className="text-sm font-bold text-red-600">{dashboard.rulesFailed || 8}</span>
                      </div>
                    </div>
                    <button className="mt-4 w-full bg-blue-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-blue-700 transition-colors">
                      Manage Rules
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Monitoring Tab */}
            {activeTab === 'monitoring' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Performance Metrics */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <TrendingUp size={18} className="text-purple-500" />
                      <h3 className="text-lg font-display font-bold">Performance Metrics</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Avg Processing Time</span>
                        <span className="text-sm font-bold">{dashboard.avgProcessingTime || '245ms'}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Throughput</span>
                        <span className="text-sm font-bold">{dashboard.throughput || '1,234 rec/min'}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Error Rate</span>
                        <span className="text-sm font-bold text-red-600">{dashboard.errorRate || '0.12%'}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">CPU Usage</span>
                        <span className="text-sm font-bold">{dashboard.cpuUsage || '45%'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Alert Configuration */}
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Zap size={18} className="text-orange-500" />
                      <h3 className="text-lg font-display font-bold">Alert Configuration</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between p-2 border border-gray-100">
                        <span className="text-sm">Pipeline Failure</span>
                        <div className="w-10 h-6 bg-orange-500 rounded-full relative">
                          <div className="absolute right-1 top-1 w-4 h-4 bg-white rounded-full" />
                        </div>
                      </div>
                      <div className="flex items-center justify-between p-2 border border-gray-100">
                        <span className="text-sm">High DLQ Count</span>
                        <div className="w-10 h-6 bg-orange-500 rounded-full relative">
                          <div className="absolute right-1 top-1 w-4 h-4 bg-white rounded-full" />
                        </div>
                      </div>
                      <div className="flex items-center justify-between p-2 border border-gray-100">
                        <span className="text-sm">Data Quality Drop</span>
                        <div className="w-10 h-6 bg-gray-300 rounded-full relative">
                          <div className="absolute left-1 top-1 w-4 h-4 bg-white rounded-full" />
                        </div>
                      </div>
                    </div>
                    <button className="mt-4 w-full bg-orange-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-orange-700 transition-colors">
                      Configure Alerts
                    </button>
                  </div>
                </div>

                {/* Recent Logs */}
                <div className="bg-white border border-gray-200 p-6">
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <FileText size={18} className="text-gray-500" />
                      <h3 className="text-lg font-display font-bold">Recent Activity Logs</h3>
                    </div>
                    <button className="text-sm text-orange-600 hover:text-orange-700 flex items-center gap-1">
                      <Download size={14} /> Export Logs
                    </button>
                  </div>
                  <div className="space-y-2 font-mono text-xs">
                    <div className="flex items-start gap-2">
                      <span className="text-gray-500">14:32:15</span>
                      <span className="text-green-600">[INFO]</span>
                      <span>Change stream started successfully</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <span className="text-gray-500">14:28:42</span>
                      <span className="text-yellow-600">[WARN]</span>
                      <span>High latency detected in member sync pipeline</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <span className="text-gray-500">14:15:30</span>
                      <span className="text-red-600">[ERROR]</span>
                      <span>Failed to process 3 records in dim_book collection</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <span className="text-gray-500">14:10:22</span>
                      <span className="text-blue-600">[DEBUG]</span>
                      <span>ETL job completed: 1,234 records processed</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Info Footer */}
            <div className="bg-gray-50 border border-gray-200 p-6 text-sm text-gray-600">
              <p className="font-mono">Analytics Engineer Operations Portal</p>
              <p className="mt-2">Backend endpoints for these operations are scaffolded in <span className="font-mono">analyticsEngineer.api.js</span>.</p>
              <p>Real-time data integration and monitoring capabilities require backend implementation.</p>
            </div>
          </>
        )}
      </div>
    </PageTransition>
  );
};

export default AnalyticsEngineerOperationsPage;

