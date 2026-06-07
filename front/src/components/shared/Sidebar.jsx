import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, Users, BookOpen, Calendar,
  AlertCircle, FileText, BarChart3, Settings,
  User, LogOut, Search, Sparkles, MessageSquare,
  History, Heart, Bookmark, Star, Target,
  PlusCircle, RefreshCw, Bell, Tag, Shield
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../../store/authStore';
import { memberApi } from '../../api/member.api';
import { staffApi } from '../../api/staff.api';

const readUserFromStorage = () => {
  try {
    const stored = localStorage.getItem('goandstudyauth');
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        token: parsed?.state?.token || null,
        userId: parsed?.state?.user?.userId || null
      };
    }
  } catch (e) {
    console.error('Error reading from localStorage:', e);
  }
  return { token: null, userId: null };
};

const Sidebar = ({ portal, isOpen = true }) => {
  const logout = useAuthStore((state) => state.logout);
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const localAuth = readUserFromStorage();

  const effectiveUserId = user?.userId || localAuth.userId;
  const effectiveToken = localAuth.token;

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const { data: notificationsData } = useQuery({
    queryKey: ['notifications', portal, effectiveUserId],
    queryFn: () => {
      if (portal === 'member') {
        return memberApi.getNotifications();
      } else if (portal === 'staff') {
        return staffApi.getNotifications();
      }
      return { data: { notifications: [] } };
    },
    enabled: !!effectiveToken && !!effectiveUserId && (portal === 'member' || portal === 'staff'),
    refetchInterval: 30000,
  });

  const notifications = notificationsData?.data?.notifications || notificationsData?.notifications || [];
  const unreadCount = notificationsData?.data?.unreadCount ?? notificationsData?.unreadCount ?? notifications.filter(n => n.status === 'Sent' || n.status === 'Unread').length;

  const NotificationBadge = ({ count }) => {
    if (count === 0) return null;
    return (
      <span className="absolute -top-1 -right-1 min-w-[14px] h-[14px] bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center px-0.5">
        {count > 99 ? '99+' : count}
      </span>
    );
  };

  const adminLinks = [
    { to: '/admin/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/admin/staff', icon: Users, label: 'Staff' },
    { to: '/admin/roles', icon: Shield, label: 'Roles' },
    { to: '/admin/members', icon: Users, label: 'Members' },
    { to: '/admin/authors', icon: User, label: 'Authors' },
    { to: '/admin/books', icon: BookOpen, label: 'Books' },
    { to: '/admin/categories', icon: Tag, label: 'Categories' },
    { to: '/admin/loans', icon: Calendar, label: 'Loans' },
    { to: '/admin/fines', icon: AlertCircle, label: 'Fines' },
    { to: '/admin/reports', icon: FileText, label: 'Reports' },
    { to: '/admin/analytics', icon: BarChart3, label: 'Analytics' },
    { to: '/admin/config', icon: Settings, label: 'Config' },
    { to: '/admin/profile', icon: User, label: 'Profile' },
    { action: handleLogout, icon: LogOut, label: 'Sign Out', isButton: true },
  ];

  const staffLinks = [
    { to: '/staff/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/staff/issue', icon: PlusCircle, label: 'Issue Book' },
    { to: '/staff/return', icon: RefreshCw, label: 'Return Book' },
    { to: '/staff/books', icon: BookOpen, label: 'Books' },
    { to: '/staff/members', icon: Users, label: 'Members' },
    { to: '/staff/notifications', icon: Bell, label: 'Notifications' },
    { to: '/staff/fines', icon: AlertCircle, label: 'Fines' },
    { to: '/staff/profile', icon: User, label: 'Profile' },
    { action: handleLogout, icon: LogOut, label: 'Sign Out', isButton: true },
  ];

  const analyticsEngineerLinks = [
    { to: '/analytics-engineer/dashboard', icon: BarChart3, label: 'Dashboard' },
    { to: '/analytics-engineer/operations', icon: Settings, label: 'Operations' },
    { to: '/analytics-engineer/reports', icon: FileText, label: 'Reports' },
    { to: '/analytics-engineer/members', icon: Users, label: 'Member Analytics' },
    { to: '/analytics-engineer/profile', icon: User, label: 'Profile' },
    { action: handleLogout, icon: LogOut, label: 'Sign Out', isButton: true },
  ];

  const memberLinks = [
    { to: '/member/home', icon: LayoutDashboard, label: 'Home' },
    { to: '/member/browse', icon: Search, label: 'Browse' },
    { to: '/member/ai-recommend', icon: Sparkles, label: 'AI Recommend' },
    { to: '/member/chatbot', icon: MessageSquare, label: 'Chatbot' },
    { to: '/member/loans', icon: Calendar, label: 'My Loans' },
    { to: '/member/history', icon: History, label: 'History' },
    { to: '/member/wishlist', icon: Heart, label: 'Wishlist' },
    { to: '/member/reservations', icon: Bookmark, label: 'Reservations' },
    { to: '/member/reviews', icon: Star, label: 'Reviews' },
    { to: '/member/reading-goal', icon: Target, label: 'Goal' },
    { to: '/member/book-requests', icon: PlusCircle, label: 'Requests' },
    { to: '/member/notifications', icon: Bell, label: 'Notifications' },
    { to: '/member/profile', icon: User, label: 'Profile' },
    { action: handleLogout, icon: LogOut, label: 'Sign Out', isButton: true },
  ];

  const portalConfigs = {
    admin: {
      links: adminLinks,
      bg: 'bg-gray-900',
      active: 'bg-red-700 text-white',
      inactive: 'text-gray-300 hover:bg-gray-800 hover:text-white',
      iconColor: 'bg-red-600'
    },
    staff: {
      links: staffLinks,
      bg: 'bg-gray-900',
      active: 'bg-teal-700 text-white',
      inactive: 'text-gray-300 hover:bg-gray-800 hover:text-white',
      iconColor: 'bg-teal-600'
    },
    analyticsEngineer: {
      links: analyticsEngineerLinks,
      bg: 'bg-gray-900',
      active: 'bg-orange-700 text-white',
      inactive: 'text-gray-300 hover:bg-gray-800 hover:text-white',
      iconColor: 'bg-orange-600'
    },
    member: {
      links: memberLinks,
      bg: 'bg-amber-50',
      active: 'bg-amber-200 text-amber-900',
      inactive: 'text-gray-700 hover:bg-amber-100 hover:text-amber-900',
      iconColor: 'bg-amber-500'
    }
  };

  const config = portalConfigs[portal];

  return (
    <div className={`h-full flex flex-col ${config.bg}`}>
      <div className="p-4 flex items-center">
        <div className={`flex-shrink-0 w-10 h-10 rounded-lg ${config.iconColor} flex items-center justify-center shadow-lg mr-3`}>
          <BookOpen className="text-white" size={20} />
        </div>
        {isOpen && (
          <div>
            <h1 className="text-xl font-bold text-white">GoAndStudy</h1>
            <p className="text-[10px] text-gray-400 uppercase">{portal} PORTAL</p>
          </div>
        )}
      </div>

      <nav className="flex-1 px-3 space-y-1">
        {config.links.map((link, index) => {
          const Icon = link.icon;
          if (link.isButton) {
            return (
              <button
                key={index}
                onClick={link.action}
                className={`flex items-center w-full px-3 py-3 text-sm font-medium rounded-none transition-colors ${
                  isOpen ? 'space-x-3' : 'justify-center'
                } ${config.inactive}`}
              >
                <Icon size={18} />
                {isOpen && <span className="whitespace-nowrap">{link.label}</span>}
              </button>
            );
          }
          return (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `flex items-center px-3 py-3 text-sm font-medium rounded-none transition-colors ${
                  isActive ? config.active : config.inactive
                } ${isOpen ? 'space-x-3' : 'justify-center'}`
              }
            >
              <div className="relative flex-shrink-0">
                <Icon size={18} />
                {link.label === 'Notifications' && isOpen && <NotificationBadge count={unreadCount} />}
              </div>
              {isOpen && <span className="whitespace-nowrap">{link.label}</span>}
            </NavLink>
          );
        })}
      </nav>
    </div>
  );
};

export default Sidebar;