import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import { useAuthStore } from '../../store/authStore';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Calendar, User, BookOpen, Hash, Code,
  AlertCircle, Loader2, Search
} from 'lucide-react';
import { format, isValid } from 'date-fns';

const safeFormat = (dateStr, fmt) => {
  if (!dateStr) return 'N/A';
  const d = new Date(dateStr);
  return isValid(d) ? format(d, fmt) : 'N/A';
};

const normalizeStatus = (status = '') => status.toString().trim().toUpperCase();

const AdminLoansPage = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const { role } = useAuthStore();
  const isStaff = role === 'ROLE_STAFF';
  const pageTitle = isStaff ? 'Circulation Monitor (Staff View)' : 'Circulation Monitor';
  const pageDescription = isStaff 
    ? 'View all active book loans and manage return schedules.'
    : 'Track all active book loans and manage return schedules.';

  const { data: loansData, isLoading, error } = useQuery({
    queryKey: ['adminLoans'],
    queryFn: () => adminApi.getLoans()
  });

  const responseFromServer = loansData?.data?.data || loansData?.data || [];
  const loans = (Array.isArray(responseFromServer) ? responseFromServer : responseFromServer?.loans) || [];

  const filteredLoans = useMemo(() => {
    if (!searchQuery.trim()) return loans;
    const query = searchQuery.toLowerCase();
    return loans.filter(loan => 
      (loan.loanId?.toLowerCase().includes(query)) ||
      (loan.bookTitle?.toLowerCase().includes(query)) ||
      (loan.bookIsbn?.toLowerCase().includes(query)) ||
      (loan.memberId?.toLowerCase().includes(query)) ||
      (loan.memberName?.toLowerCase().includes(query)) ||
      (loan.status?.toLowerCase().includes(query))
    );
  }, [loans, searchQuery]);

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div>
          <h1 className="text-3xl font-display font-bold">{pageTitle}</h1>
          <p className="text-gray-500 text-sm mt-1">{pageDescription}</p>
        </div>

        {/* Search Bar */}
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="relative max-w-md">
            <Search size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search by Loan ID, Book Title, ISBN, Member ID, or Status..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-md focus:outline-none focus:border-red-500 text-sm"
            />
          </div>
          <div className="text-xs text-gray-500 mt-2">
            Showing {filteredLoans.length} of {loans.length} loans
          </div>
        </div>

        <div className="bg-white border border-gray-200 overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-24">Loan ID</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold flex-1">Book Title</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-28">ISBN</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-32">Borrower ID</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-32">Issued On</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-28">Due Date</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-24">Status</th>
                <th className="text-left px-6 py-4 text-[10px] font-mono uppercase tracking-widest text-gray-500 font-semibold w-20">Fine</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {error ? (
                <tr>
                  <td colSpan="8" className="py-20 text-center text-red-500">
                    <AlertCircle className="mx-auto mb-2" size={32} />
                    <p>Error loading loans: {error?.message || 'Unknown error'}</p>
                  </td>
                </tr>
              ) : isLoading ? (
                <tr>
                  <td colSpan="8" className="py-20 text-center">
                    <Loader2 className="animate-spin text-red-500 mx-auto" size={32} />
                  </td>
                </tr>
              ) : filteredLoans.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-20 text-center text-gray-400">
                    {searchQuery ? 'No loans match your search.' : 'No active loans found in circulation.'}
                  </td>
                </tr>
              ) : (
                filteredLoans.map((loan, idx) => (
                  <tr key={loan.loanId || idx} className="hover:bg-gray-50/50 transition-colors">
                    <td className="px-6 py-5 text-xs text-gray-600 font-mono">
                      <div className="flex items-center space-x-2">
                        <Hash size={12} className="opacity-50 flex-shrink-0" />
                        <span className="truncate">{loan.loanId?.substring(0, 8) || 'N/A'}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-sm">
                      <div className="flex items-center space-x-3">
                        <div className="w-8 h-8 bg-gray-100 flex items-center justify-center flex-shrink-0">
                          <BookOpen size={16} className="text-gray-400" />
                        </div>
                        <p className="font-bold text-gray-900 line-clamp-1">{loan.bookTitle || 'Unknown Title'}</p>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-xs text-gray-600 font-mono">
                      <div className="flex items-center space-x-2">
                        <Code size={12} className="opacity-50 flex-shrink-0" />
                        <span className="truncate">{loan.bookIsbn || 'N/A'}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-xs text-gray-600 font-mono">
                      <div className="flex items-center space-x-2">
                        <User size={12} className="opacity-50 flex-shrink-0" />
                        <span className="truncate">{loan.memberId || 'Unknown'}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-xs text-gray-600">
                      <div className="flex items-center space-x-2">
                        <Calendar size={12} className="opacity-50 flex-shrink-0" />
                        <span>{safeFormat(loan.issuedAt, 'MMM dd, yyyy')}</span>
                      </div>
                    </td>
                    <td className="px-6 py-5 text-xs text-gray-600">
                      {safeFormat(loan.dueDate, 'MMM dd, yyyy')}
                    </td>
                    <td className="px-6 py-5 text-xs">
                      <span className={`inline-block px-3 py-1 rounded font-semibold text-center ${
                        ['ACTIVE', 'ISSUED', 'RENEWED'].includes(normalizeStatus(loan.status)) ? 'bg-green-100 text-green-700' :
                        normalizeStatus(loan.status) === 'OVERDUE' ? 'bg-red-100 text-red-700' :
                        normalizeStatus(loan.status) === 'RETURNED' ? 'bg-blue-100 text-blue-700' :
                        'bg-gray-100 text-gray-700'
                      }`}>
                        {loan.status || 'Unknown'}
                      </span>
                    </td>
                    <td className="px-6 py-5 text-xs font-bold">
                      <span className={loan.fineAmount > 0 ? 'text-red-500' : 'text-green-600'}>
                        ₹{loan.fineAmount?.toFixed(2) || '0.00'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminLoansPage;
