import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  BookOpen, Clock, AlertCircle, 
  ArrowRight, Loader2, Calendar, 
  RotateCcw, History, Info
} from 'lucide-react';
import { format } from 'date-fns';
import { Link } from 'react-router-dom';

const MemberLoansPage = () => {
  const { data: loansData, isLoading } = useQuery({
    queryKey: ['memberLoans'],
    queryFn: memberApi.getLoans
  });

  const loans = loansData?.data || [];

  return (
    <PageTransition>
      <div className="p-8 space-y-12">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-4xl font-display font-bold text-gray-900">Current Holdings</h1>
            <p className="text-gray-500 text-sm mt-1 uppercase tracking-widest">Managing {loans.length} active literary assets</p>
          </div>
          <button className="flex items-center text-xs font-bold uppercase tracking-widest text-indigo-600 hover:text-indigo-800 transition-all">
            <History size={16} className="mr-2" /> View Reading History
          </button>
        </div>

        {isLoading ? (
          <div className="py-24 text-center">
            <Loader2 className="animate-spin text-indigo-500 mx-auto" size={48} />
          </div>
        ) : loans.length === 0 ? (
          <div className="bg-white border border-gray-100 p-20 text-center space-y-6 rounded-sm shadow-xl shadow-indigo-50">
             <div className="w-20 h-20 bg-gray-50 text-gray-300 rounded-full flex items-center justify-center mx-auto">
                <BookOpen size={40} />
             </div>
             <div>
               <h3 className="text-xl font-display font-bold text-gray-900">No Active Loans</h3>
               <p className="text-gray-500 text-sm mt-2">Your shelf is currently empty. Visit the repository to discover your next journey.</p>
             </div>
             <Link to="/member/browse" className="inline-block px-10 py-4 bg-indigo-600 text-white font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-indigo-700 transition-all">
                Browse Repository
             </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {loans.map((loan) => {
              const overdue = new Date(loan.dueDate) < new Date();
              return (
                <div key={loan.loanId} className={`bg-white border p-8 rounded-sm shadow-xl transition-all hover:translate-y-[-4px] ${overdue ? 'border-red-200 ring-2 ring-red-50' : 'border-gray-100 hover:border-indigo-100'}`}>
                  <div className="flex space-x-6">
                    <div className="w-24 h-36 bg-gray-100 flex-shrink-0 shadow-lg overflow-hidden flex items-center justify-center rounded-sm">
                       <img 
                          src={loan.bookCoverImageUrl || `https://picsum.photos/seed/${loan.bookIsbn || loan.loanId}/200/300.jpg`} 
                          alt={loan.bookTitle}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            e.target.onerror = null;
                            e.target.src = 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?q=80&w=2730&auto=format&fit=crop';
                          }}
                        />
                    </div>
                    <div className="flex-1 space-y-4">
                      <div className="flex justify-between items-start">
                        <Badge variant={overdue ? 'red' : 'indigo'}>{overdue ? 'Overdue' : 'Active Loan'}</Badge>
                        <span className="text-[10px] font-bold text-gray-400 font-mono">ID: {loan.loanId.slice(0, 8)}</span>
                      </div>
                      <h3 className="text-xl font-display font-bold text-gray-900 line-clamp-1">{loan.bookTitle}</h3>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1">
                          <p className="text-[10px] font-mono text-gray-400 uppercase tracking-tighter">Due Date</p>
                          <p className={`text-sm font-bold flex items-center ${overdue ? 'text-red-600' : 'text-gray-900'}`}>
                            <Calendar size={14} className="mr-2" />
                            {format(new Date(loan.dueDate), 'MMM dd, yyyy')}
                          </p>
                        </div>
                        <div className="space-y-1">
                          <p className="text-[10px] font-mono text-gray-400 uppercase tracking-tighter">Fine Accretion</p>
                          <p className="text-sm font-bold text-gray-900 flex items-center">
                            <Clock size={14} className="mr-2 text-indigo-500" />
                            ${loan.currentFine?.toFixed(2) || '0.00'}
                          </p>
                        </div>
                      </div>
                      <div className="pt-4 flex items-center space-x-4">
                        <button className="flex-1 py-3 bg-gray-900 text-white rounded-sm font-bold uppercase tracking-widest text-[10px] hover:bg-black transition-all flex items-center justify-center">
                          <RotateCcw size={14} className="mr-2" />
                          Request Extension
                        </button>
                        <button className="p-3 bg-indigo-50 text-indigo-600 rounded-sm hover:bg-indigo-100 transition-all">
                          <Info size={18} />
                        </button>
                      </div>
                    </div>
                  </div>
                  {overdue && (
                    <div className="mt-6 p-3 bg-red-50 border border-red-100 rounded-sm flex items-center space-x-3">
                       <AlertCircle size={16} className="text-red-500" />
                       <p className="text-[10px] text-red-800 font-bold uppercase tracking-widest">Fines will double after 7 days overdue.</p>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberLoansPage;
