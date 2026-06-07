import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import { PlusSquare, X, Loader2, BookOpen, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { format } from 'date-fns';

const MemberBookRequestPage = () => {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    author: '',
    isbn: '',
    reason: ''
  });

  const { data: requestsData, isLoading } = useQuery({
    queryKey: ['memberBookRequests'],
    queryFn: memberApi.getBookRequests
  });

  const createMutation = useMutation({
    mutationFn: memberApi.createBookRequest,
    onSuccess: () => {
      queryClient.invalidateQueries(['memberBookRequests']);
      toast.success('Book request submitted successfully');
      setIsModalOpen(false);
      setFormData({ title: '', author: '', isbn: '', reason: '' });
    },
    onError: (error) => {
      toast.error(error?.response?.data?.message || 'Failed to submit request');
    }
  });

  const cancelMutation = useMutation({
    mutationFn: memberApi.cancelBookRequest,
    onSuccess: () => {
      queryClient.invalidateQueries(['memberBookRequests']);
      toast.success('Request cancelled');
    },
    onError: (error) => {
      toast.error(error?.response?.data?.message || 'Failed to cancel request');
    }
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.title.trim()) {
      toast.error('Title is required');
      return;
    }
    createMutation.mutate(formData);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const getStatusIcon = (status) => {
    switch (status?.toLowerCase()) {
      case 'approved':
        return <CheckCircle size={20} className="text-green-500" />;
      case 'rejected':
        return <XCircle size={20} className="text-red-500" />;
      case 'pending':
        return <Clock size={20} className="text-yellow-500" />;
      default:
        return <AlertCircle size={20} className="text-gray-400" />;
    }
  };

  const getStatusClass = (status) => {
    switch (status?.toLowerCase()) {
      case 'approved':
        return 'bg-green-100 text-green-700';
      case 'rejected':
        return 'bg-red-100 text-red-700';
      case 'pending':
        return 'bg-yellow-100 text-yellow-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  };

  const requests = requestsData?.data?.requests || requestsData?.data || [];

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Acquisition Requests</h1>
            <p className="text-gray-500 text-sm mt-1">Request new books for the library collection</p>
          </div>
          <button
            onClick={() => setIsModalOpen(true)}
            className="px-6 py-3 bg-indigo-600 text-white font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-indigo-700 transition-all flex items-center gap-2"
          >
            <PlusSquare size={18} />
            New Request
          </button>
        </div>

        {isLoading ? (
          <div className="py-24 flex flex-col items-center justify-center space-y-4">
            <Loader2 className="animate-spin text-indigo-500" size={48} />
            <p className="text-sm font-mono text-gray-400 uppercase tracking-widest">Loading requests...</p>
          </div>
        ) : requests.length === 0 ? (
          <div className="bg-white border border-gray-100 p-12 text-center text-gray-400 space-y-6 rounded-sm">
            <PlusSquare size={48} className="mx-auto opacity-20" />
            <p>Is the repository missing an essential asset? Submit a formal acquisition request.</p>
            <button
              onClick={() => setIsModalOpen(true)}
              className="px-8 py-3 border-2 border-indigo-600 text-indigo-600 font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-indigo-50 transition-all"
            >
              New Request
            </button>
          </div>
        ) : (
          <div className="bg-white border border-gray-100 rounded-sm overflow-hidden">
            <div className="divide-y divide-gray-100">
              {requests.map((request) => (
                <div key={request.id || request.requestId} className="p-6 hover:bg-gray-50 transition-colors">
                  <div className="flex items-start justify-between">
                    <div className="flex gap-4">
                      <div className="w-12 h-16 bg-gray-200 rounded flex-shrink-0 flex items-center justify-center">
                        <BookOpen size={24} className="text-gray-400" />
                      </div>
                      <div>
                        <h3 className="font-bold text-gray-900">{request.title}</h3>
                        <p className="text-sm text-gray-500">{request.author || 'Unknown Author'}</p>
                        {request.isbn && <p className="text-xs text-gray-400 mt-1">ISBN: {request.isbn}</p>}
                        {request.reason && <p className="text-sm text-gray-600 mt-2 italic">"{request.reason}"</p>}
                        <div className="flex items-center gap-3 mt-3">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase flex items-center gap-1 ${getStatusClass(request.status)}`}>
                            {getStatusIcon(request.status)}
                            {request.status || 'Pending'}
                          </span>
                          <span className="text-xs text-gray-400">
                            Submitted {request.createdAt ? format(new Date(request.createdAt), 'MMM d, yyyy') : 'N/A'}
                          </span>
                        </div>
                      </div>
                    </div>
                    {request.status?.toLowerCase() === 'pending' && (
                      <button
                        onClick={() => cancelMutation.mutate(request.id || request.requestId)}
                        disabled={cancelMutation.isPending}
                        className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition-colors"
                        title="Cancel request"
                      >
                        <X size={20} />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* New Request Modal */}
        {isModalOpen && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-sm max-w-lg w-full p-8 space-y-6">
              <div className="flex justify-between items-center">
                <h2 className="text-2xl font-display font-bold">New Acquisition Request</h2>
                <button
                  onClick={() => setIsModalOpen(false)}
                  className="p-2 hover:bg-gray-100 rounded transition-colors"
                >
                  <X size={24} />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">Book Title *</label>
                  <input
                    type="text"
                    name="title"
                    value={formData.title}
                    onChange={handleChange}
                    placeholder="Enter book title"
                    className="w-full px-4 py-3 border-2 border-gray-100 focus:border-indigo-500 focus:outline-none transition-colors"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">Author</label>
                  <input
                    type="text"
                    name="author"
                    value={formData.author}
                    onChange={handleChange}
                    placeholder="Enter author name"
                    className="w-full px-4 py-3 border-2 border-gray-100 focus:border-indigo-500 focus:outline-none transition-colors"
                  />
                </div>

                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">ISBN</label>
                  <input
                    type="text"
                    name="isbn"
                    value={formData.isbn}
                    onChange={handleChange}
                    placeholder="Enter ISBN (optional)"
                    className="w-full px-4 py-3 border-2 border-gray-100 focus:border-indigo-500 focus:outline-none transition-colors"
                  />
                </div>

                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-2">Reason for Request</label>
                  <textarea
                    name="reason"
                    value={formData.reason}
                    onChange={handleChange}
                    placeholder="Why should we acquire this book?"
                    rows={3}
                    className="w-full px-4 py-3 border-2 border-gray-100 focus:border-indigo-500 focus:outline-none transition-colors resize-none"
                  />
                </div>

                <div className="flex gap-4 pt-4">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    className="flex-1 px-4 py-3 border-2 border-gray-200 text-gray-700 font-bold uppercase tracking-widest text-xs hover:bg-gray-50 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={createMutation.isPending}
                    className="flex-1 px-4 py-3 bg-indigo-600 text-white font-bold uppercase tracking-widest text-xs hover:bg-indigo-700 transition-colors disabled:opacity-50"
                  >
                    {createMutation.isPending ? 'Submitting...' : 'Submit Request'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberBookRequestPage;
