import React from 'react';
import { useAuthStore } from '../../store/authStore';
import PageTransition from '../../components/shared/PageTransition';
import { User, Mail, Shield, Calendar, Key, UserCheck } from 'lucide-react';
import { format } from 'date-fns';

const AdminProfilePage = () => {
  const { user } = useAuthStore();

  return (
    <PageTransition>
      <div className="p-8 space-y-8 max-w-2xl">
        <div className="flex flex-col md:flex-row md:items-center space-y-4 md:space-y-0 md:space-x-8 pb-12 border-b border-gray-100">
          <div className="w-32 h-32 bg-gray-900 text-white flex items-center justify-center font-display text-5xl font-bold border-4 border-red-500">
            {user?.name?.[0]?.toUpperCase()}
          </div>
          <div className="space-y-1">
            <h1 className="text-4xl font-display font-bold text-gray-900">{user?.name}</h1>
            <div className="flex items-center space-x-2">
              <Shield size={14} className="text-red-600" />
              <span className="text-xs font-mono uppercase tracking-widest text-red-600 font-bold">System Administrator</span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="p-6 bg-gray-50 border border-gray-200">
            <div className="flex items-center text-gray-400 mb-4">
              <Mail size={16} className="mr-2" />
              <span className="text-[10px] font-mono uppercase tracking-tighter">Account Email</span>
            </div>
            <p className="font-bold text-gray-900">{user?.email}</p>
          </div>

          <div className="p-6 bg-gray-50 border border-gray-200">
            <div className="flex items-center text-gray-400 mb-4">
              <Calendar size={16} className="mr-2" />
              <span className="text-[10px] font-mono uppercase tracking-tighter">Access Since</span>
            </div>
            <p className="font-bold text-gray-900">
              {user?.createdAt ? format(new Date(user.createdAt), 'MMMM dd, yyyy') : 'October 12, 2024'}
            </p>
          </div>
        </div>

        <div className="space-y-4">
          <h3 className="text-xs font-mono uppercase tracking-widest font-bold text-gray-400">Account Security</h3>
          <div className="space-y-3">
            <button className="w-full flex items-center justify-between p-4 border border-gray-200 hover:border-gray-900 transition-all group">
              <div className="flex items-center space-x-4">
                <Key size={18} className="text-gray-400 group-hover:text-red-500" />
                <span className="text-xs font-bold uppercase tracking-widest text-gray-700">Change System Password</span>
              </div>
              <UserCheck size={18} className="text-gray-200 group-hover:text-gray-900" />
            </button>
          </div>
        </div>
      </div>
    </PageTransition>
  );
};

export default AdminProfilePage;
