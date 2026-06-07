import React, { useState, useEffect } from 'react';
import { useAuthStore } from '../../store/authStore';
import { format } from 'date-fns';

const Topbar = ({ portal }) => {
  const { user } = useAuthStore();
  const today = new Date();
  const [profileImage, setProfileImage] = useState(null);

  // Load profile image from localStorage
  useEffect(() => {
    if (user) {
      const userId = user?.memberId || user?.userId || user?.id;
      if (userId) {
        const storedImage = localStorage.getItem(`profileImage_${userId}`);
        setProfileImage(storedImage);
      }
    }
  }, [user]);

  const themes = {
    admin: 'bg-gray-900 border-b border-gray-800 text-gray-100',
    staff: 'bg-gray-900 border-b border-gray-800 text-gray-100',
    analyticsEngineer: 'bg-gray-900 border-b border-gray-800 text-gray-100',
    member: 'bg-white border-b border-amber-100 text-gray-900',
  };

  return (
    <header className={`${themes[portal]} h-16 flex items-center justify-between px-8 z-10 sticky top-0`}>
      <div className="flex items-center space-x-4">
        <h2 className="text-lg font-medium capitalize">{portal} Dashboard</h2>
        <span className="text-gray-400 text-sm hidden md:inline">•</span>
        <span className="text-gray-400 text-sm hidden md:inline">{format(today, 'EEEE, dd MMMM yyyy')}</span>
      </div>

      <div className="flex items-center space-x-4">
        <div className="text-right hidden sm:block">
          <p className="text-sm font-medium">{user?.name || 'User'}</p>
          <p className="text-[10px] font-mono uppercase opacity-60">{user?.role?.replace('ROLE_', '')}</p>
        </div>
        <div className={`w-10 h-10 rounded-full flex items-center justify-center font-display text-lg font-bold border overflow-hidden ${
          portal === 'member' ? 'bg-amber-100 text-amber-700 border-amber-200' : 'bg-gray-800 text-white border-gray-700'
        }`}>
          {profileImage ? (
            <img src={profileImage} alt="Profile" className="w-full h-full object-cover" />
          ) : (
            user?.name?.[0]?.toUpperCase() || 'U'
          )}
        </div>
      </div>
    </header>
  );
};

export default Topbar;
