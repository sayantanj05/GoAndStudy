import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import {
  RefreshCw, Search, User,
  CheckCircle, Loader2,
  ArrowRight, ArrowLeft, ShieldCheck, CalendarDays
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const StaffReturnPage = () => {
const queryClient = useQueryClient();
  const [step, setStep] = useState(1);
  const [isbn, setIsbn] = useState('');
  const [memberEmail, setMemberEmail] = useState('');
  const [candidateLoan, setCandidateLoan] = useState(null);
  const [candidateLoans, setCandidateLoans] = useState([]);
  const [selectedLoanId, setSelectedLoanId] = useState('');
  const [processedReturn, setProcessedReturn] = useState(null);

  const resetFlow = () => {
    setStep(1);
    setIsbn('');
    setMemberEmail('');
    setCandidateLoan(null);
    setCandidateLoans([]);
    setSelectedLoanId('');
    setProcessedReturn(null);
  };

  const verifyIsbnMutation = useMutation({
    mutationFn: () => staffApi.getActiveLoanByIsbn(isbn.trim()),
    onSuccess: (loan) => {
      console.log('Loan found:', loan);
      setCandidateLoan(loan);
      setStep(2);
      toast.success('Active loan found. Verify the member email.');
    },
    onError: (err) => {
      console.error('ISBN verification error:', err);
      toast.error(err.response?.data?.message || err.message || 'No active loan found for this ISBN');
    },
  });

  const verifyMemberMutation = useMutation({
    mutationFn: () => staffApi.getActiveLoansByIsbnAndMember(isbn.trim(), memberEmail.trim()),
    onSuccess: (loans) => {
      if (loans.length === 0) {
        toast.error('No matching active loan for this member and ISBN');
        return;
      }
      setCandidateLoans(loans);
      setSelectedLoanId(loans[0].loanId);
      setStep(3);
      toast.success('Member verified. Confirm the return.');
    },
    onError: (err) => {
      console.error('Verify member error:', err);
      toast.error(err.response?.data?.message || err.message || 'Verification failed');
    },
  });

  const returnMutation = useMutation({
    mutationFn: (loanId) => staffApi.returnBook(loanId),
    onSuccess: (returnedLoan) => {
      console.log('Return successful:', returnedLoan);
      queryClient.invalidateQueries(['staffStats']);
      queryClient.invalidateQueries(['staffMembers']);
      queryClient.invalidateQueries(['adminLoans']);
      setProcessedReturn(returnedLoan);
      setStep(3);
      toast.success('Book returned successfully');
    },
    onError: (err) => {
      console.error('Return error:', err);
      toast.error(err.response?.data?.message || err.message || 'Return failed');
    },
  });

  const renewMutation = useMutation({
    mutationFn: () => staffApi.renewLoan(processedReturn.loanId),
    onSuccess: (data) => {
      queryClient.invalidateQueries(['staffStats']);
      toast.success(`Loan renewed! New due date: ${new Date(data.newDueDate).toLocaleDateString()}`);
    },
    onError: (err) => {
      toast.error(err.response?.data?.message || err.message || 'Renewal failed');
    },
  });

  const handleRenew = () => {
    renewMutation.mutate();
  };

  const handleIsbnSubmit = (e) => {
    e.preventDefault();
    if (!isbn.trim()) return;
    verifyIsbnMutation.mutate();
  };

  const handleMemberSubmit = (e) => {
    e.preventDefault();
    if (!memberEmail.trim()) return;
    verifyMemberMutation.mutate();
  };

  const handleReturnSelected = () => {
    if (!selectedLoanId) return;
    returnMutation.mutate(selectedLoanId);
  };

  const handleSelectionBack = () => {
    setStep(2);
    setSelectedLoanId('');
  };

  return (
    <PageTransition>
      <div className="p-8 max-w-4xl mx-auto space-y-12">
        <div className="flex items-center justify-between relative">
          <div className="absolute top-1/2 left-0 w-full h-px bg-gray-200 z-0" />
          {[1, 2, 3].map((currentStep) => (
            <div
              key={currentStep}
              className={`relative z-10 w-10 h-10 flex items-center justify-center font-bold font-mono text-sm border-2 transition-all ${step >= currentStep ? 'bg-teal-600 border-teal-600 text-white' : 'bg-white border-gray-200 text-gray-400'
                }`}
            >
              {step > currentStep ? <CheckCircle size={18} /> : currentStep}
            </div>
          ))}
        </div>

        {step === 1 && (
          <div className="bg-white border border-gray-200 p-12 animate-in fade-in">
            <div className="text-center space-y-4 mb-8">
              <div className="w-16 h-16 bg-gray-100 text-gray-500 rounded-full flex items-center justify-center mx-auto">
                <RefreshCw size={32} />
              </div>
              <h2 className="text-3xl font-display font-bold">Return Processing</h2>
              <p className="text-gray-500 text-sm max-w-md mx-auto">
              </p>
            </div>

            <form onSubmit={handleIsbnSubmit} className="max-w-md mx-auto space-y-6">
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-gray-400 mb-2">Book ISBN</label>
                <div className="relative">
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                  <input
                    type="text"
                    required
                    autoFocus
                    placeholder="978785001"
                    value={isbn}
                    onChange={(e) => setIsbn(e.target.value)}
                    className="w-full pl-12 pr-4 py-4 bg-gray-50 border-b-2 border-gray-200 focus:border-teal-500 focus:outline-none transition-colors font-mono tracking-widest text-lg"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={verifyIsbnMutation.isPending}
                className="w-full py-4 bg-gray-900 text-white font-bold uppercase tracking-widest hover:bg-black transition-all flex items-center justify-center group"
              >
                {verifyIsbnMutation.isPending ? <Loader2 className="animate-spin" size={20} /> : (
                  <>
                    <span>Verify ISBN</span>
                    <ArrowRight className="ml-2 group-hover:translate-x-1 transition-transform" size={18} />
                  </>
                )}
              </button>
            </form>
          </div>
        )}

        {step === 2 && candidateLoan && (
          <div className="bg-white border border-gray-200 p-12 space-y-8 animate-in fade-in">
            <div className="flex items-center justify-between pb-6 border-b border-gray-100">
              <div>
                <p className="text-[10px] font-mono uppercase tracking-widest text-gray-400">Matched Book</p>
                <p className="font-bold text-gray-900 mt-2">{candidateLoan.bookTitle}</p>
                <p className="text-sm text-gray-500">ISBN: {candidateLoan.bookIsbn}</p>
              </div>
              <button
                onClick={resetFlow}
                className="text-[10px] font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 flex items-center"
              >
                <ArrowLeft size={14} className="mr-1" /> Change ISBN
              </button>
            </div>

            <div className="text-center space-y-3">
              <div className="w-16 h-16 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mx-auto">
                <User size={32} />
              </div>
              <h3 className="text-2xl font-display font-bold">Verify Member</h3>
              <p className="text-gray-500 text-sm max-w-md mx-auto">
                Enter the email address of the member who borrowed this book. The return will proceed only if the loan matches both ISBN and member email.
              </p>
            </div>

            <form onSubmit={handleMemberSubmit} className="max-w-md mx-auto space-y-6">
              <div>
                <label className="block text-[10px] font-mono uppercase tracking-widest text-gray-400 mb-2">Member Email</label>
                <div className="relative">
                  <User className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                  <input
                    type="email"
                    required
                    placeholder="member@example.com"
                    value={memberEmail}
                    onChange={(e) => setMemberEmail(e.target.value)}
                    className="w-full pl-12 pr-4 py-4 bg-gray-50 border-b-2 border-gray-200 focus:border-teal-500 focus:outline-none transition-colors font-medium"
                  />
                </div>
              </div>
              <div className="bg-amber-50 border border-amber-100 p-4 text-sm text-amber-800">
                This ISBN has an active loan record. Verify the member email before returning because the same ISBN can be on loan to multiple members.
              </div>
              <button
                type="submit"
                disabled={verifyMemberMutation.isPending}
                className="w-full py-4 bg-teal-600 text-white font-bold uppercase tracking-widest hover:bg-teal-700 transition-all flex items-center justify-center group"
              >
                {verifyMemberMutation.isPending ? <Loader2 className="animate-spin" size={20} /> : (
                  <>
                    <span>Verify Member</span>
                    <ArrowRight className="ml-2 group-hover:translate-x-1 transition-transform" size={18} />
                  </>
                )}
              </button>
            </form>
          </div>
        )}

        {step === 3 && candidateLoans.length > 0 && (
          <div className="bg-white border border-gray-200 p-12 space-y-8 animate-in fade-in">
            {processedReturn ? (
              <div className="text-center space-y-5">
                <div className="w-16 h-16 bg-green-50 text-green-600 rounded-full flex items-center justify-center mx-auto">
                  <CheckCircle size={34} />
                </div>
                <div>
                  <h3 className="text-2xl font-display font-bold">Return Completed</h3>
                  <p className="text-gray-500 text-sm mt-2">{processedReturn.bookTitle} has been returned for {processedReturn.memberName || memberEmail}.</p>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left">
                  <div className="bg-gray-50 p-4 border border-gray-100">
                    <p className="text-[10px] font-mono uppercase tracking-widest text-gray-400">Returned On</p>
                    <p className="font-bold mt-1">{processedReturn.returnedAt ? new Date(processedReturn.returnedAt).toLocaleDateString() : 'Today'}</p>
                  </div>
                  <div className="bg-gray-50 p-4 border border-gray-100">
                    <p className="text-[10px] font-mono uppercase tracking-widest text-gray-400">Overdue Days</p>
                    <p className="font-bold mt-1">{processedReturn.overdueDays || 0}</p>
                  </div>
                  <div className="bg-gray-50 p-4 border border-gray-100">
                    <p className="text-[10px] font-mono uppercase tracking-widest text-gray-400">Fine</p>
                    <p className={`font-bold mt-1 ${(processedReturn.fineAmount || 0) > 0 ? 'text-red-600' : 'text-green-600'}`}>
                      Rs {(processedReturn.fineAmount || 0).toFixed(2)}
                    </p>
                  </div>
                </div>
                <button
                  onClick={resetFlow}
                  className="px-6 py-3 bg-gray-900 text-white font-bold uppercase tracking-widest hover:bg-black transition-all"
                >
                  Process Another Return
                </button>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between pb-6 border-b border-gray-100">
                  <div>
                    <p className="text-[10px] font-mono uppercase tracking-widest text-gray-400">Verified Return</p>
                    <p className="font-bold text-gray-900 mt-2">{candidateLoans[0].bookTitle}</p>
                    <p className="text-sm text-gray-500">ISBN: {candidateLoans[0].bookIsbn} • Member: {memberEmail}</p>
                  </div>
                  <button
                    onClick={handleSelectionBack}
                    className="text-[10px] font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 flex items-center"
                  >
                    <ArrowLeft size={14} className="mr-1" /> Change Email
                  </button>
                </div>

                <div className="space-y-3">
                  {candidateLoans.map((loan) => (
                    <label
                      key={loan.loanId}
                      className={`block border p-4 cursor-pointer transition-colors ${selectedLoanId === loan.loanId ? 'border-teal-500 bg-teal-50' : 'border-gray-200 hover:border-gray-300'}`}
                    >
                      <div className="flex items-start gap-3">
                        <input
                          type="radio"
                          name="loan"
                          value={loan.loanId}
                          checked={selectedLoanId === loan.loanId}
                          onChange={(e) => setSelectedLoanId(e.target.value)}
                          className="mt-1"
                        />
                        <div className="flex-1">
                          <div className="flex flex-wrap items-center justify-between gap-3">
                            <p className="font-bold text-gray-900">{loan.bookTitle}</p>
                            <span className="text-[10px] font-mono uppercase tracking-widest text-gray-500">{loan.status}</span>
                          </div>
                          <div className="mt-2 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs text-gray-500">
                            <span>Loan: {loan.loanId}</span>
                            <span>Issued: {loan.issuedAt ? new Date(loan.issuedAt).toLocaleDateString() : 'N/A'}</span>
                            <span className="flex items-center"><CalendarDays size={12} className="mr-1" /> Due: {loan.dueDate ? new Date(loan.dueDate).toLocaleDateString() : 'N/A'}</span>
                          </div>
                        </div>
                      </div>
                    </label>
                  ))}
                </div>

                <button
                  type="button"
                  onClick={handleReturnSelected}
                  disabled={!selectedLoanId || returnMutation.isPending}
                  className="w-full py-4 bg-gray-900 text-white font-bold uppercase tracking-widest hover:bg-black transition-all flex items-center justify-center group disabled:opacity-50"
                >
                  {returnMutation.isPending ? <Loader2 className="animate-spin" size={20} /> : (
                    <>
                      <ShieldCheck className="mr-2" size={18} />
                      <span>Confirm Book Return</span>
                    </>
                  )}
                </button>
              </>
            )}
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default StaffReturnPage;
