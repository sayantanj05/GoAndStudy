import React, { useEffect, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { staffApi } from '../../api/staff.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Bell, AlertTriangle, Info, Clock, 
  CheckCircle, Loader2, RefreshCw
} from 'lucide-react';
import { format } from 'date-fns';

const StaffNotificationsPage = () => {
  console.log('[StaffNotificationsPage] Component rendering');
  const queryClient = useQueryClient();
  const [hasMarkedRead, setHasMarkedRead] = useState(false);
  
  const { data: notificationsData, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['staffNotifications'],
    queryFn: staffApi.getNotifications
  });
  
  console.log('[StaffNotificationsPage] notificationsData:', notificationsData, 'isLoading:', isLoading);

  const notifications = notificationsData?.notifications || [];

  // Mark all unread notifications as read when page loads (only once)
  useEffect(() => {
    if (hasMarkedRead || notifications.length === 0 || isLoading) {
      return;
    }

    const markAllAsRead = async () => {
      try {
        const unreadNotifications = notifications.filter(n => n.status === 'Sent' || n.status === 'Unread');
        
        if (unreadNotifications.length > 0) {
          console.log(`[StaffNotificationsPage] Marking ${unreadNotifications.length} notifications as read`);
          
          // Mark each unread notification as read
          await Promise.all(
            unreadNotifications.map(notification => 
              staffApi.markNotificationAsRead(notification.id).catch(err => {
                console.error('Error marking notification as read:', err);
              })
            )
          );
          
          // Invalidate notifications queries to refresh data
          queryClient.invalidateQueries({ queryKey: ['staffNotifications'] });
          queryClient.invalidateQueries({ queryKey: ['notifications', 'staff'] });
        }
        
        setHasMarkedRead(true);
      } catch (error) {
        console.error('[StaffNotificationsPage] Error in markAllAsRead:', error);
      }
    };

    markAllAsRead();
  }, [notifications, isLoading, queryClient, hasMarkedRead]);

  const getIcon = (type) => {
    switch(type) {
      case 'OVERDUE_ALERT': return <AlertTriangle className="text-red-500" size={20} />;
      case 'RESERVATION_AVAILABLE': return <Bell className="text-amber-500" size={20} />;
      case 'WELCOME':
      case 'GENERAL': return <Info className="text-blue-500" size={20} />;
      case 'FINE_REMINDER': return <CheckCircle className="text-teal-500" size={20} />;
      default: return <Info className="text-blue-500" size={20} />;
    }
  };

  return (
    <PageTransition>
      <div className="p-8 space-y-8 max-w-4xl">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Operation Alerts</h1>
            <p className="text-gray-500 text-sm mt-1">Real-time system notifications and task requirements.</p>
          </div>
          <button 
            onClick={() => refetch()}
            disabled={isFetching}
            className="p-3 border border-gray-200 hover:bg-gray-50 transition-colors"
          >
            <RefreshCw size={18} className={isFetching ? 'animate-spin' : ''} />
          </button>
        </div>

        <div className="space-y-4">
          {isLoading ? (
            <div className="py-20 text-center"><Loader2 className="animate-spin text-teal-500 mx-auto" size={32} /></div>
          ) : notifications.length === 0 ? (
            <div className="py-20 text-center text-gray-400">No active notifications.</div>
          ) : (
            notifications.map((n) => (
              <div key={n.id} className="bg-white border-l-4 border-l-teal-600 border border-teal-100 p-6 flex items-start space-x-6 hover:shadow-md transition-shadow">
                <div className="bg-gray-50 p-3 flex-shrink-0">
                  {getIcon(n.type)}
                </div>
                <div className="flex-1">
                  <div className="flex justify-between items-start">
                    <h3 className="font-bold text-gray-900">{n.title}</h3>
                    <span className="text-[10px] font-mono uppercase text-gray-400 flex items-center">
                      <Clock size={10} className="mr-1" /> {n.createdAt ? format(new Date(n.createdAt), 'dd MMM yyyy, HH:mm') : 'Unknown time'}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 mt-1 leading-relaxed">{n.message}</p>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </PageTransition>
  );
};

export default StaffNotificationsPage;
