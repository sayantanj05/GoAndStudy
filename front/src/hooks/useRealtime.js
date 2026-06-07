import { useEffect, useMemo, useRef } from 'react';
import { io } from 'socket.io-client';
import { useAuthStore } from '../store/authStore';

/**
 * Custom hook to handle real-time socket connections.
 * @param {Object} eventHandlers - Map of event names to handler functions.
 */
export function useRealtime(eventHandlersOrEvent = {}, singleHandler) {
  const { token, user } = useAuthStore();
  const socketRef = useRef(null);
  const eventHandlers = useMemo(() => {
    if (typeof eventHandlersOrEvent === 'string') {
      return singleHandler ? { [eventHandlersOrEvent]: singleHandler } : {};
    }
    return eventHandlersOrEvent ?? {};
  }, [eventHandlersOrEvent, singleHandler]);

  useEffect(() => {
    // Only connect if user is authenticated
    if (!token || !user || !import.meta.env.VITE_REALTIME_URL) return;

    // Initialize socket connection
    socketRef.current = io(import.meta.env.VITE_REALTIME_URL, {
      query: { 
        role: user.role, 
        userId: user.userId 
      },
      transports: ['websocket'],
      auth: { token }
    });

    // Attach all event handlers
    Object.entries(eventHandlers).forEach(([event, handler]) => {
      socketRef.current.on(event, handler);
    });

    // Cleanup on unmount or token change
    return () => {
      if (socketRef.current) {
        Object.keys(eventHandlers).forEach((event) => {
          socketRef.current.off(event);
        });
        socketRef.current.disconnect();
      }
    };
  }, [token, user, eventHandlers]);

  return socketRef.current;
}

export default useRealtime;
