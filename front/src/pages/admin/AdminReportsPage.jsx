import React from 'react';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { adminApi } from '../../api/admin.api';
import { 
  FileText, Database, TrendingUp, DollarSign, 
  Users, Download, ChevronRight, Loader2, FileSpreadsheet, FileIcon
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminReportsPage = () => {
  const [downloading, setDownloading] = React.useState(null);
  const [showFormatMenu, setShowFormatMenu] = React.useState(null);
  const menuRef = React.useRef(null);

  // Close menu when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowFormatMenu(null);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const reports = [
    { 
      id: 'inventory', 
      title: 'Global Inventory Audit', 
      desc: 'Complete status of all physical and digital assets including low stock alerts.', 
      icon: Database, 
      color: 'blue' 
    },
    { 
      id: 'circulation', 
      title: 'Circulation Statistics', 
      desc: 'Detailed breakdown of loans, returns, and reservation conversion rates.', 
      icon: TrendingUp, 
      color: 'teal' 
    },
    { 
      id: 'financial', 
      title: 'Revenue & Fines Report', 
      desc: 'Audit of generated fines, waived fees, and overall collection efficiency.', 
      icon: DollarSign, 
      color: 'red' 
    },
    { 
      id: 'users', 
      title: 'User Engagement Metrics', 
      desc: 'Analysis of registrations, active members, and staff productivity.', 
      icon: Users, 
      color: 'amber' 
    },
  ];

  const handleDownload = async (id, format = 'csv') => {
    setDownloading(id);
    try {
      console.log(`Downloading ${id} report as ${format}...`);
      const res = await adminApi.exportReport(id, format);
      console.log('Download response:', res);
      
      const contentType = format === 'pdf' ? 'application/pdf' : 
                         format === 'csv' ? 'text/csv' : 'text/tab-separated-values';
      const blob = new Blob([res.data], { type: contentType });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${id}_report_${new Date().toISOString().slice(0, 10)}.${format}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success(`Report downloaded as ${format.toUpperCase()}`);
    } catch (err) {
      console.error('Download error:', err);
      console.error('Error response:', err.response);
      const errorMsg = err.response?.data?.message || err.message || 'Unknown error';
      toast.error(`Failed to download: ${errorMsg}`);
    } finally {
      setDownloading(null);
      setShowFormatMenu(null);
    }
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">System Intelligence</h1>
            <p className="text-gray-500 text-sm mt-1">Generate high-fidelity reports for strategic decision making.</p>
          </div>
          <FileText className="text-gray-200" size={48} />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6" ref={menuRef}>
          {reports.map((report) => {
            const Icon = report.icon;
            const isDownloading = downloading === report.id;

            return (
              <div key={report.id} className="bg-white border border-gray-200 p-8 flex flex-col justify-between hover:border-gray-900 transition-all group">
                <div>
                  <div className={`w-12 h-12 flex items-center justify-center mb-6 border border-gray-200 group-hover:bg-gray-900 group-hover:text-white transition-colors`}>
                    <Icon size={24} />
                  </div>
                  <h3 className="text-xl font-display font-bold mb-2">{report.title}</h3>
                  <p className="text-sm text-gray-500 leading-relaxed mb-12">
                    {report.desc}
                  </p>
                </div>

                <div className="relative">
                  {showFormatMenu === report.id && (
                    <div className="absolute bottom-full left-0 right-0 mb-2 bg-white border border-gray-200 shadow-lg rounded-sm overflow-hidden z-10">
                      <button
                        onClick={() => handleDownload(report.id, 'csv')}
                        disabled={isDownloading}
                        className="w-full px-4 py-3 text-left text-xs font-bold uppercase tracking-widest hover:bg-gray-50 flex items-center"
                      >
                        <FileSpreadsheet size={14} className="mr-2" />
                        Download as CSV
                      </button>
                      <button
                        onClick={() => handleDownload(report.id, 'tsv')}
                        disabled={isDownloading}
                        className="w-full px-4 py-3 text-left text-xs font-bold uppercase tracking-widest hover:bg-gray-50 flex items-center border-t border-gray-100"
                      >
                        <FileSpreadsheet size={14} className="mr-2" />
                        Download as TSV
                      </button>
                      <button
                        onClick={() => handleDownload(report.id, 'pdf')}
                        disabled={isDownloading}
                        className="w-full px-4 py-3 text-left text-xs font-bold uppercase tracking-widest hover:bg-gray-50 flex items-center border-t border-gray-100"
                      >
                        <FileIcon size={14} className="mr-2" />
                        Download as PDF
                      </button>
                    </div>
                  )}
                  <button
                    onClick={() => setShowFormatMenu(showFormatMenu === report.id ? null : report.id)}
                    disabled={isDownloading}
                    className="w-full py-4 border-2 border-gray-900 font-bold uppercase tracking-widest text-xs flex items-center justify-center hover:bg-gray-900 hover:text-white transition-all disabled:opacity-50"
                  >
                    {isDownloading ? (
                      <Loader2 className="animate-spin mr-2" size={16} />
                    ) : (
                      <Download size={16} className="mr-2" />
                    )}
                    {isDownloading ? 'Downloading...' : 'Download'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Scheduled Reports */}
        <div className="bg-gray-900 p-8 border border-white/5">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="text-lg font-display font-bold text-white uppercase tracking-widest">Scheduled Automation</h3>
              <p className="text-gray-400 text-sm mt-1">Reports are automatically dispatched to regional leads every Sunday at 00:00 UTC.</p>
            </div>
            <Badge variant="teal">Active</Badge>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminReportsPage;
