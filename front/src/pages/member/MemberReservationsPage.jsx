import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  Clock, Book, Calendar, Users, Loader2, 
  XCircle, BookOpen, AlertCircle 
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { formatDistanceToNow, format } from 'date-fns';

const MemberReservationsPage = () => {
  const queryClient = useQueryClient();

  const { data: reservationsData, isLoading } = useQuery({
    queryKey: ['memberReservations'],
    queryFn: memberApi.getReservations,
  });

  const cancelMutation = useMutation({
    mutationFn: (reservationId) => memberApi.cancelReservation(reservationId),
    onSuccess: () => {
      queryClient.invalidateQueries(['memberReservations']);
      toast.success('Reservation cancelled successfully');
    },
    onError: (error) => {
      toast.error(error?.response?.data?.message || 'Failed to cancel reservation');
    },
  });

  const reservations = reservationsData?.data ?? [];

  const handleCancel = (reservationId, bookTitle) => {
    if (window.confirm(`Cancel your reservation for "${bookTitle}"?`)) {
      cancelMutation.mutate(reservationId);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'Notified':
        return <Badge variant="teal">Ready for Pickup</Badge>;
      case 'Pending':
        return <Badge variant="amber">In Queue</Badge>;
      default:
        return <Badge variant="gray">{status}</Badge>;
    }
  };

  const getQueueMessage = (position, status) => {
    if (status === 'Notified') {
      return 'Book is available! Pick up within 48 hours.';
    }
    if (position === 1) {
      return "You're next in line!";
    }
    return `${position} people ahead of you`;
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-display font-bold">Active Reservations</h1>
            <p className="text-gray-500 mt-1">
              Books you've reserved that are currently unavailable
            </p>
          </div>
          <div className="bg-amber-50 border border-amber-200 px-4 py-2 rounded-lg">
            <span className="text-amber-800 font-bold">{reservations.length}</span>
            <span className="text-amber-600 text-sm ml-1">Active</span>
          </div>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="bg-white border border-gray-100 p-12 text-center">
            <Loader2 size={48} className="mx-auto mb-4 animate-spin text-indigo-500" />
            <p className="text-gray-500">Loading your reservations...</p>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && reservations.length === 0 && (
          <div className="bg-white border border-gray-100 p-12 text-center rounded-lg">
            <Clock size={48} className="mx-auto mb-4 text-gray-300" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              No Active Reservations
            </h3>
            <p className="text-gray-500 max-w-md mx-auto mb-6">
              You haven't reserved any books yet. When a book you're interested in 
              is unavailable, you can reserve it to join the queue.
            </p>
            <a 
              href="/member/browse" 
              className="inline-flex items-center space-x-2 bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
            >
              <BookOpen size={18} />
              <span>Browse Books</span>
            </a>
          </div>
        )}

        {/* Reservations Grid */}
        {!isLoading && reservations.length > 0 && (
          <div className="grid gap-6">
            {reservations.map((reservation) => (
              <div 
                key={reservation.reservationId}
                className="bg-white border border-gray-200 rounded-lg p-6 flex flex-col sm:flex-row gap-6 hover:shadow-md transition-shadow"
              >
                {/* Book Cover */}
                <div className="w-24 h-36 bg-gray-100 rounded-md flex-shrink-0 overflow-hidden">
                  {reservation.bookCoverImageUrl ? (
                    <img 
                      src={reservation.bookCoverImageUrl}
                      alt={reservation.bookTitle}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <Book size={32} className="text-gray-300" />
                    </div>
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 space-y-3">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-xl font-semibold text-gray-900">
                        {reservation.bookTitle}
                      </h3>
                      <div className="flex items-center space-x-3 mt-2">
                        {getStatusBadge(reservation.status)}
                        <span className="text-sm text-gray-500 flex items-center">
                          <Users size={14} className="mr-1" />
                          {getQueueMessage(reservation.queuePosition, reservation.status)}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Reservation Details */}
                  <div className="flex flex-wrap gap-4 text-sm text-gray-500 pt-2">
                    <span className="flex items-center">
                      <Calendar size={14} className="mr-1" />
                      Reserved {formatDistanceToNow(new Date(reservation.reservedAt), { addSuffix: true })}
                    </span>
                    {reservation.expiresAt && reservation.status === 'Notified' && (
                      <span className="flex items-center text-amber-600 font-medium">
                        <AlertCircle size={14} className="mr-1" />
                        Expires {format(new Date(reservation.expiresAt), 'MMM d, h:mm a')}
                      </span>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center space-x-3 pt-2">
                    {reservation.status === 'Notified' && (
                      <button 
                        className="bg-teal-600 text-white px-4 py-2 rounded-md font-medium hover:bg-teal-700 transition-colors flex items-center space-x-2"
                        onClick={() => toast.success('Please visit the counter to pick up your book')}
                      >
                        <BookOpen size={16} />
                        <span>Pick Up Book</span>
                      </button>
                    )}
                    <button
                      onClick={() => handleCancel(reservation.reservationId, reservation.bookTitle)}
                      disabled={cancelMutation.isPending}
                      className="border border-gray-300 text-gray-700 px-4 py-2 rounded-md font-medium hover:bg-gray-50 transition-colors flex items-center space-x-2 disabled:opacity-50"
                    >
                      <XCircle size={16} />
                      <span>Cancel</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Info Box */}
        <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-6">
          <h4 className="font-semibold text-indigo-900 mb-2 flex items-center">
            <AlertCircle size={18} className="mr-2" />
            How Reservations Work
          </h4>
          <ul className="text-sm text-indigo-800 space-y-1 list-disc list-inside">
            <li>Reserve books that are currently checked out by other members</li>
            <li>You'll be placed in a queue and notified when the book becomes available</li>
            <li>Once notified, you have 48 hours to pick up the book</li>
            <li>You can cancel your reservation at any time</li>
          </ul>
        </div>
      </div>
    </PageTransition>
  );
};

export default MemberReservationsPage;
