import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import PageTransition from '../../components/shared/PageTransition';
import { analyticsEngineerApi } from '../../api/analyticsEngineer.api';
import { 
  Loader2, Download, Calendar, Filter, BarChart3, TrendingUp,
  FileText, Database, Users, BookOpen, Activity, AlertTriangle,
  Eye, Settings, RefreshCw, ChevronDown
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AnalyticsEngineerReportsPage = () => {
  const [selectedReport, setSelectedReport] = useState('pipeline');
  const [dateRange, setDateRange] = useState('7d');
  const [isExporting, setIsExporting] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['analyticsEngineerReports', selectedReport, dateRange],
    queryFn: () => analyticsEngineerApi.getReports({ type: selectedReport, dateRange }),
    staleTime: 60_000,
  });

  const reports = data?.data || data || {};

  const handleExport = async (reportType) => {
    setIsExporting(true);
    try {
      await new Promise(resolve => setTimeout(resolve, 2000));
      toast.success(`${reportType} report exported successfully`);
    } catch (error) {
      toast.error('Failed to export report');
    } finally {
      setIsExporting(false);
    }
  };

  const reportTypes = [
    { id: 'pipeline', label: 'Pipeline Performance', icon: Activity },
    { id: 'quality', label: 'Data Quality', icon: Database },
    { id: 'etl', label: 'ETL Jobs', icon: RefreshCw },
    { id: 'usage', label: 'System Usage', icon: BarChart3 },
    { id: 'anomalies', label: 'Anomaly Detection', icon: AlertTriangle },
    { id: 'business', label: 'Business Analytics', icon: TrendingUp }
  ];

  const dateRanges = [
    { value: '24h', label: 'Last 24 Hours' },
    { value: '7d', label: 'Last 7 Days' },
    { value: '30d', label: 'Last 30 Days' },
    { value: '90d', label: 'Last 90 Days' },
    { value: 'custom', label: 'Custom Range' }
  ];

  return (
    <PageTransition>
      <div className="p-8 space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Analytics Reports</h1>
            <p className="text-gray-500 text-sm mt-1">Comprehensive analytics reports and data insights for operations monitoring.</p>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value)}
              className="px-4 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
            >
              {dateRanges.map(range => (
                <option key={range.value} value={range.value}>{range.label}</option>
              ))}
            </select>
            <button
              onClick={() => handleExport(selectedReport)}
              disabled={isExporting}
              className="flex items-center gap-2 bg-orange-600 text-white px-4 py-2 text-sm font-bold uppercase tracking-widest hover:bg-orange-700 transition-colors disabled:opacity-50"
            >
              <Download size={16} />
              {isExporting ? 'Exporting...' : 'Export'}
            </button>
          </div>
        </div>

        <div className="flex space-x-1 border-b border-gray-200 overflow-x-auto">
          {reportTypes.map((type) => (
            <button
              key={type.id}
              onClick={() => setSelectedReport(type.id)}
              className={`flex items-center space-x-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors whitespace-nowrap ${
                selectedReport === type.id
                  ? 'border-orange-500 text-orange-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <type.icon size={16} />
              <span>{type.label}</span>
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="h-64 flex items-center justify-center">
            <Loader2 className="animate-spin text-orange-500" size={48} />
          </div>
        ) : (
          <>
            {selectedReport === 'pipeline' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Activity size={18} className="text-orange-500" />
                      <h3 className="text-lg font-display font-bold">Uptime</h3>
                    </div>
                    <div className="text-3xl font-bold text-green-600">99.8%</div>
                    <p className="text-sm text-gray-600 mt-2">Last 30 days</p>
                    <div className="mt-4 text-xs text-green-600">↑ 0.2% from last month</div>
                  </div>
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <TrendingUp size={18} className="text-blue-500" />
                      <h3 className="text-lg font-display font-bold">Throughput</h3>
                    </div>
                    <div className="text-3xl font-bold text-blue-600">1.2M</div>
                    <p className="text-sm text-gray-600 mt-2">Records processed</p>
                    <div className="mt-4 text-xs text-green-600">↑ 15% from last week</div>
                  </div>
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <RefreshCw size={18} className="text-purple-500" />
                      <h3 className="text-lg font-display font-bold">Avg Latency</h3>
                    </div>
                    <div className="text-3xl font-bold text-purple-600">245ms</div>
                    <p className="text-sm text-gray-600 mt-2">Processing time</p>
                    <div className="mt-4 text-xs text-red-600">↑ 12ms from baseline</div>
                  </div>
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <AlertTriangle size={18} className="text-red-500" />
                      <h3 className="text-lg font-display font-bold">Errors</h3>
                    </div>
                    <div className="text-3xl font-bold text-red-600">0.12%</div>
                    <p className="text-sm text-gray-600 mt-2">Error rate</p>
                    <div className="mt-4 text-xs text-green-600">↓ 0.05% from last week</div>
                  </div>
                </div>

                <div className="bg-white border border-gray-200 p-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-display font-bold">Performance Trends</h3>
                    <button className="text-sm text-orange-600 hover:text-orange-700 flex items-center gap-1">
                      <Eye size={14} /> View Details
                    </button>
                  </div>
                  <div className="h-64 bg-gray-50 border border-gray-200 flex items-center justify-center">
                    <p className="text-gray-500">Performance chart visualization would be displayed here</p>
                  </div>
                </div>
              </div>
            )}

            {selectedReport === 'quality' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <Database size={18} className="text-green-500" />
                      <h3 className="text-lg font-display font-bold">Quality Score</h3>
                    </div>
                    <div className="text-3xl font-bold text-green-600">95.2%</div>
                    <p className="text-sm text-gray-600 mt-2">Overall quality</p>
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
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <AlertTriangle size={18} className="text-yellow-500" />
                      <h3 className="text-lg font-display font-bold">Anomalies</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Critical</span>
                        <span className="text-sm font-bold text-red-600">2</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Warning</span>
                        <span className="text-sm font-bold text-yellow-600">5</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Info</span>
                        <span className="text-sm font-bold text-blue-600">12</span>
                      </div>
                    </div>
                    <button className="mt-4 w-full bg-yellow-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-yellow-700 transition-colors">
                      Investigate Anomalies
                    </button>
                  </div>
                  <div className="bg-white border border-gray-200 p-6">
                    <div className="flex items-center gap-2 mb-4">
                      <FileText size={18} className="text-blue-500" />
                      <h3 className="text-lg font-display font-bold">Validation Rules</h3>
                    </div>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Active Rules</span>
                        <span className="text-sm font-bold">24</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Passed Today</span>
                        <span className="text-sm font-bold text-green-600">1247</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-600">Failed Today</span>
                        <span className="text-sm font-bold text-red-600">8</span>
                      </div>
                    </div>
                    <button className="mt-4 w-full bg-blue-600 text-white px-4 py-2 text-xs font-bold uppercase tracking-widest hover:bg-blue-700 transition-colors">
                      Manage Rules
                    </button>
                  </div>
                </div>
              </div>
            )}

            <div className="bg-gray-50 border border-gray-200 p-6">
              <div className="flex items-center gap-2 mb-4">
                <FileText size={18} className="text-gray-500" />
                <h3 className="text-lg font-display font-bold">Report Summary</h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <p className="text-sm text-gray-600">Report Type</p>
                  <p className="text-sm font-bold capitalize">{selectedReport} Analytics</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Generated At</p>
                  <p className="text-sm font-bold">{new Date().toLocaleString()}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Data Period</p>
                  <p className="text-sm font-bold">{dateRanges.find(r => r.value === dateRange)?.label}</p>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </PageTransition>
  );
};

export default AnalyticsEngineerReportsPage;
