import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Search, User, Book as BookIcon, 
  CheckCircle, ArrowRight, ArrowLeft, 
  Loader2, AlertCircle, ShieldCheck
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const StaffIssuePage = () => {
  const queryClient = useQueryClient();
  const [step, setStep] = useState(1);
  const [member, setMember] = useState(null);
  const [book, setBook] = useState(null);
  const [searchLoading, setSearchLoading] = useState(false);
  
  const [searchVal, setSearchVal] = useState({
    memberEmail: '',
    bookIsbn: ''
  });

  const issueMutation = useMutation({
    mutationFn: staffApi.issueBook,
    onSuccess: () => {
      queryClient.invalidateQueries(['staffStats']);
      toast.success('Book issued successfully!');
      setStep(4); // Success step
    },
    onError: (err) => {
      console.error('Issue book error:', err);
      console.error('Error response:', err.response);
      console.error('Error message:', err.message);
      toast.error(err.response?.data?.message || err.message || 'Failed to issue book');
    }
  });

  const handleMemberSearch = async (e) => {
    e.preventDefault();
    setSearchLoading(true);
    try {
      const memberData = await staffApi.getMemberByEmail(searchVal.memberEmail);
      setMember(memberData);
      setStep(2);
    } catch (err) {
      toast.error('Member not found');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleBookSearch = async (e) => {
    e.preventDefault();
    setSearchLoading(true);
    try {
      const bookData = await staffApi.getBookByIsbn(searchVal.bookIsbn);
      if (bookData.availableCopies <= 0) {
        toast.error('No copies available for this book');
        return;
      }
      setBook(bookData);
      setStep(3);
    } catch (err) {
      toast.error('Book not found');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleFinalIssue = () => {
    issueMutation.mutate({
      memberId: member.memberId,
      bookId: book.bookId
    });
  };

  return (
    <PageTransition>
      <div className="p-8 max-w-4xl mx-auto space-y-12">
        {/* Progress Bar */}
        <div className="flex items-center justify-between relative">
          <div className="absolute top-1/2 left-0 w-full h-px bg-gray-200 z-0" />
          {[1, 2, 3].map((s) => (
            <div 
              key={s} 
              className={`relative z-10 w-10 h-10 flex items-center justify-center font-bold font-mono text-sm border-2 transition-all ${
                step >= s ? 'bg-teal-600 border-teal-600 text-white' : 'bg-white border-gray-200 text-gray-400'
              }`}
            >
              {step > s ? <CheckCircle size={18} /> : s}
            </div>
          ))}
        </div>

        {/* Step 1: Member Identification */}
        {step === 1 && (
          <div className="bg-white border border-gray-200 p-12 text-center space-y-8 animate-in fade-in slide-in-from-bottom-4">
            <div className="w-16 h-16 bg-teal-50 text-teal-600 rounded-full flex items-center justify-center mx-auto">
              <User size={32} />
            </div>
            <div>
              <h2 className="text-2xl font-display font-bold">Identify Member</h2>
              <p className="text-gray-500 text-sm mt-2">Enter the registered email of the member to proceed with issuance.</p>
            </div>
            <form onSubmit={handleMemberSearch} className="max-w-md mx-auto">
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="email"
                  required
                  placeholder="member@example.com"
                  value={searchVal.memberEmail}
                  onChange={(e) => setSearchVal({...searchVal, memberEmail: e.target.value})}
                  className="w-full pl-12 pr-4 py-4 bg-gray-50 border-b-2 border-gray-200 focus:border-teal-500 focus:outline-none transition-colors font-medium"
                />
              </div>
              <button 
                type="submit"
                disabled={searchLoading}
                className="w-full mt-6 py-4 bg-gray-900 text-white font-bold uppercase tracking-widest hover:bg-black transition-all flex items-center justify-center"
              >
                {searchLoading ? <Loader2 className="animate-spin" size={20} /> : 'Lookup Member'}
              </button>
            </form>
          </div>
        )}

        {/* Step 2: Book Selection */}
        {step === 2 && (
          <div className="bg-white border border-gray-200 p-12 space-y-8 animate-in fade-in slide-in-from-right-4">
            <div className="flex items-center justify-between pb-6 border-b border-gray-100">
              <div className="flex items-center space-x-4">
                <div className="w-12 h-12 bg-teal-50 text-teal-600 flex items-center justify-center font-bold">
                  {member.name[0]}
                </div>
                <div>
                  <p className="font-bold text-gray-900">{member.name}</p>
                  <p className="text-xs text-gray-500 uppercase font-mono tracking-tighter">Verified Member</p>
                </div>
              </div>
              <button 
                onClick={() => setStep(1)}
                className="text-[10px] font-bold uppercase tracking-widest text-gray-400 hover:text-red-500 flex items-center"
              >
                <ArrowLeft size={14} className="mr-1" /> Switch Member
              </button>
            </div>

            <div className="text-center py-8">
              <div className="w-16 h-16 bg-blue-50 text-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <BookIcon size={32} />
              </div>
              <h2 className="text-2xl font-display font-bold">Identify Book</h2>
              <p className="text-gray-500 text-sm mt-2">Scan ISBN or enter manually to select the book for checkout.</p>
            </div>

            <form onSubmit={handleBookSearch} className="max-w-md mx-auto">
              <div className="relative">
                <input
                  type="text"
                  required
                  placeholder="ISBN (e.g. 9780123456789)"
                  value={searchVal.bookIsbn}
                  onChange={(e) => setSearchVal({...searchVal, bookIsbn: e.target.value})}
                  className="w-full px-4 py-4 bg-gray-50 border-b-2 border-gray-200 focus:border-blue-500 focus:outline-none transition-colors font-mono tracking-[0.2em]"
                />
              </div>
              <button 
                type="submit"
                disabled={searchLoading}
                className="w-full mt-6 py-4 bg-teal-600 text-white font-bold uppercase tracking-widest hover:bg-teal-700 transition-all flex items-center justify-center"
              >
                {searchLoading ? <Loader2 className="animate-spin" size={20} /> : 'Process ISBN'}
              </button>
            </form>
          </div>
        )}

        {/* Step 3: Confirmation */}
        {step === 3 && (
          <div className="bg-white border border-gray-200 p-12 space-y-8 animate-in fade-in slide-in-from-right-4">
             <div className="text-center">
                <h2 className="text-3xl font-display font-bold">Final Review</h2>
                <p className="text-gray-500 text-sm mt-2 uppercase tracking-widest font-mono">Authorization Required</p>
             </div>

             <div className="grid grid-cols-2 gap-8 py-8 border-y border-gray-100">
                <div className="space-y-4">
                  <span className="text-[10px] font-mono uppercase text-gray-400">Recipient</span>
                  <div className="p-4 bg-gray-50 border-l-4 border-teal-500">
                    <p className="font-bold text-gray-900">{member.name}</p>
                    <p className="text-xs text-gray-500">{member.email}</p>
                  </div>
                </div>
                <div className="space-y-4">
                  <span className="text-[10px] font-mono uppercase text-gray-400">Asset</span>
                  <div className="p-4 bg-gray-50 border-l-4 border-blue-500">
                    <p className="font-bold text-gray-900 line-clamp-1">{book.title}</p>
                    <p className="text-xs text-gray-500">ISBN: {book.isbn}</p>
                  </div>
                </div>
             </div>

             <div className="bg-amber-50 p-4 flex items-start space-x-3 border border-amber-200">
                <AlertCircle size={18} className="text-amber-600 mt-0.5" />
                <p className="text-xs text-amber-800 leading-relaxed font-medium">
                  By confirming, you authorize the loan for 14 days. Failure to return by the due date will generate an automatic daily fine of $0.50.
                </p>
             </div>

             <div className="flex space-x-4">
                <button 
                  onClick={() => setStep(2)}
                  className="flex-1 py-4 border border-gray-200 text-gray-600 font-bold uppercase tracking-widest text-xs hover:bg-gray-50 transition-all"
                >
                  Go Back
                </button>
                <button 
                  onClick={handleFinalIssue}
                  disabled={issueMutation.isLoading}
                  className="flex-[2] py-4 bg-gray-900 text-white font-bold uppercase tracking-widest text-xs hover:bg-black transition-all flex items-center justify-center"
                >
                  {issueMutation.isLoading ? <Loader2 className="animate-spin" size={18} /> : (
                    <>
                      <ShieldCheck size={18} className="mr-2" />
                      Authorize & Checkout
                    </>
                  )}
                </button>
             </div>
          </div>
        )}

        {/* Step 4: Success */}
        {step === 4 && (
          <div className="bg-white border border-gray-200 p-20 text-center space-y-8 animate-in zoom-in-95">
            <div className="w-24 h-24 bg-teal-50 text-teal-600 rounded-full flex items-center justify-center mx-auto mb-6">
              <CheckCircle size={48} />
            </div>
            <div className="space-y-2">
              <h2 className="text-4xl font-display font-bold">Transaction Complete</h2>
              <p className="text-gray-500 max-w-sm mx-auto">
                The asset has been successfully registered to {member.name}'s account. A confirmation email has been dispatched.
              </p>
            </div>
            <div className="pt-8">
              <button 
                onClick={() => {
                  setStep(1);
                  setMember(null);
                  setBook(null);
                  setSearchVal({ memberEmail: '', bookIsbn: '' });
                }}
                className="px-12 py-4 bg-gray-900 text-white font-bold uppercase tracking-widest text-xs hover:bg-black transition-all"
              >
                New Transaction
              </button>
            </div>
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default StaffIssuePage;
