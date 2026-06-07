import React, { useEffect, useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import useRealtime from '../../hooks/useRealtime';
import useAuthStore from '../../store/authStore';
import PageTransition from '../../components/shared/PageTransition';
import Badge from '../../components/shared/Badge';
import { 
  User, Mail, Calendar, Sparkles, 
  Settings, LogOut, Zap, BookOpen,
  PieChart, RefreshCw, GraduationCap, Loader2, X, Edit3, Save,
  Camera, Trash2
} from 'lucide-react';
import { motion } from 'framer-motion';
import { toast } from 'react-hot-toast';
import { format, differenceInYears } from 'date-fns';

const MemberProfilePage = () => {
  const { user: authUser, updateUser, logout } = useAuthStore();
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editData, setEditData] = useState({ name: '', phone: '', gender: '', dateOfBirth: '' });
  const [isLoading, setIsLoading] = useState(false);
  const [previewImage, setPreviewImage] = useState(null);
  const fileInputRef = useRef(null);

  const queryClient = useQueryClient();

  // Fetch full profile data
  const { data: profileData, isLoading: profileLoading } = useQuery({
    queryKey: ['memberProfile'],
    queryFn: memberApi.getProfile,
    enabled: !!authUser,
  });

  // Fetch real analytics data
  const { data: analyticsData, isLoading: analyticsLoading } = useQuery({
    queryKey: ['memberAnalytics'],
    queryFn: memberApi.getAnalytics,
    enabled: !!authUser,
    refetchInterval: 20000,
    refetchOnWindowFocus: true,
  });

  useRealtime({
    BOOK_RETURNED: () => queryClient.invalidateQueries({ queryKey: ['memberAnalytics'] }),
    BOOK_ISSUED: () => queryClient.invalidateQueries({ queryKey: ['memberAnalytics'] }),
    FINE_COLLECTED: () => queryClient.invalidateQueries({ queryKey: ['memberAnalytics'] }),
    MEMBERSHIP_UPGRADED: () => {
      queryClient.invalidateQueries({ queryKey: ['memberAnalytics'] });
      queryClient.invalidateQueries({ queryKey: ['memberProfile'] });
    },
  });
  const updateMutation = useMutation({
    mutationFn: memberApi.updateProfile,
    onMutate: async (newData) => {
      const previousUser = user;
      updateUser(newData);
      return { previousUser };
    },
    onError: (err, newData, context) => {
      toast.error('Update failed');
      if (context?.previousUser) updateUser(context.previousUser);
    },
    onSettled: () => {
      setIsEditOpen(false);
    },
    onSuccess: () => toast.success('Profile updated'),
  });

  // Neural controls mutation
  const neuralControlsMutation = useMutation({
    mutationFn: memberApi.updateNeuralControls,
    onSuccess: () => {
      queryClient.invalidateQueries(['memberAnalytics']);
      toast.success('Neural controls updated');
    },
    onError: () => toast.error('Failed to update neural controls'),
  });

  // Merge auth user with full profile data
  const user = profileData?.data || authUser;

  // Calculate age from dateOfBirth
  const calculateAge = () => {
    if (user?.dateOfBirth) {
      return differenceInYears(new Date(), new Date(user.dateOfBirth));
    }
    if (user?.age) return user.age;
    if (user?.analytics?.age) return user.analytics.age;
    return null;
  };

  const age = calculateAge();

  useEffect(() => {
    if (user) {
      setEditData({
        name: user.name || '',
        phone: user.phone || '',
        gender: user.gender || '',
        dateOfBirth: user.dateOfBirth ? user.dateOfBirth.split('T')[0] : ''
      });
      // Load profile image from localStorage (fallback to user.profileImageUrl from API)
      const storedImage = localStorage.getItem(`profileImage_${user.memberId || user.userId || user.id}`);
      setPreviewImage(storedImage || user.profileImageUrl || null);
    }
  }, [user]);

  // Real analytics data
  const analytics = analyticsData?.data || {};
  const readingMetrics = [
    { label: 'Books Read', value: analytics.booksRead || 0, icon: <BookOpen />, color: 'indigo' },
    { label: 'Reading Velocity', value: analytics.readingVelocity || '1.0x', icon: <Zap />, color: 'amber' },
    { label: 'Knowledge Nodes', value: analytics.knowledgeNodes || 0, icon: <GraduationCap />, color: 'teal' }
  ];

  const handleEditOpen = () => {
    setIsEditOpen(true);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const payload = {
      name: editData.name,
      phone: editData.phone,
      gender: editData.gender,
      dateOfBirth: editData.dateOfBirth
    };
    updateMutation.mutate(payload);
  };

  const handleEditClose = () => {
    setIsEditOpen(false);
  };

  const handleImageUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      toast.error('Please select an image file');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Image size should be less than 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      const imageData = reader.result;
      setPreviewImage(imageData);
      // Save to localStorage with user-specific key
      const userId = user?.memberId || user?.userId || user?.id;
      if (userId) {
        localStorage.setItem(`profileImage_${userId}`, imageData);
      }
      toast.success('Profile image updated!');
    };
    reader.readAsDataURL(file);
  };

  return (
    <PageTransition>
      <div className="p-8 max-w-6xl mx-auto space-y-12 pb-24">
        {/* Profile Header */}
        <div className="relative group overflow-hidden bg-gradient-to-r from-indigo-600 to-indigo-900 rounded-sm p-12 text-white shadow-2xl">
           <div className="relative z-10 flex flex-col md:flex-row md:items-center gap-10">
              <div className="relative group w-40 h-40">
                <div 
                  className="w-40 h-40 bg-white/10 backdrop-blur-xl border-4 border-white/20 rounded-sm flex items-center justify-center text-6xl font-display font-bold shadow-2xl overflow-hidden cursor-pointer"
                  onClick={() => fileInputRef.current?.click()}
                >
                  {previewImage ? (
                    <img src={previewImage} alt="Profile" className="w-full h-full object-cover" />
                  ) : (
                    user?.name?.[0]
                  )}
                </div>
                
                {/* Hover overlay */}
                <div className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded-sm cursor-pointer"
                     onClick={() => fileInputRef.current?.click()}>
                  <Camera className="text-white" size={32} />
                </div>
                
                {/* Remove image button */}
                {previewImage && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setPreviewImage(null);
                      // Remove from localStorage
                      const userId = user?.memberId || user?.userId || user?.id;
                      if (userId) {
                        localStorage.removeItem(`profileImage_${userId}`);
                      }
                      toast.success('Profile image removed');
                    }}
                    className="absolute -top-2 -right-2 w-8 h-8 bg-red-500 text-white rounded-full flex items-center justify-center shadow-lg hover:bg-red-600"
                  >
                    <Trash2 size={16} />
                  </button>
                )}
                
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleImageUpload}
                  className="hidden"
                />
              </div>
              <div className="space-y-4 flex-1">
                 <div className="flex items-center space-x-3">
                    <Badge variant="indigo">Verified Member</Badge>
                    <span className="text-[10px] font-mono text-indigo-300 uppercase tracking-widest bg-white/5 px-2 py-0.5 border border-white/10 rounded-sm">Active Subscription</span>
                 </div>
                 <h1 className="text-5xl font-display font-bold">{user?.name}</h1>
                 <div className="text-[10px] font-mono text-indigo-300/60 uppercase tracking-widest">
                    Member ID: {user?.memberId || user?.userId || user?.id || 'N/A'}
                 </div>
                 <div className="flex flex-wrap items-center gap-6 text-indigo-100 opacity-70 text-sm">
                    <div className="flex items-center"><Mail size={16} className="mr-2" /> {user?.email}</div>
                    <div className="flex items-center"><Calendar size={16} className="mr-2" /> 
                      Joined {user?.timeCreated ? format(new Date(user.timeCreated), 'dd/MM/yyyy') : 
                             user?.registrationDate ? format(new Date(user.registrationDate), 'dd/MM/yyyy') : 'N/A'}
                    </div>
                    {user?.gender && <div className="flex items-center"><span className="w-4 h-4 bg-white/20 rounded-full mr-2" /> {user.gender}</div>}
                    {age && <div className="flex items-center"><Calendar size={14} className="mr-1 w-3.5 h-3.5" /> Age {age}</div>}
                 </div>
              </div>
              <div className="flex flex-col gap-3">
                 <button onClick={handleEditOpen} className="px-8 py-3 bg-white text-indigo-900 rounded-sm font-bold uppercase tracking-widest text-xs hover:bg-indigo-50 transition-all flex items-center">
                    <Settings size={16} className="mr-2" /> Edit Profile
                 </button>
                 <button 
                  onClick={logout}
                  className="px-8 py-3 bg-white/10 backdrop-blur-md border border-white/30 text-white rounded-sm font-bold uppercase tracking-widest text-xs hover:bg-red-500 hover:border-red-500 transition-all flex items-center"
                >
                   <LogOut size={16} className="mr-2" /> Terminate Session
                 </button>
              </div>
           </div>
           
           {/* Abstract Decoration */}
           <Sparkles className="absolute -bottom-10 -right-10 opacity-10 group-hover:scale-125 transition-transform" size={300} />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-12">
            {/* Column 1: Metrics */}
            <div className="lg:col-span-2 space-y-8">
               <h3 className="text-2xl font-display font-bold">Inference Insights</h3>
               <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  {readingMetrics.map((m, idx) => (
                    <motion.div 
                      key={idx}
                      whileHover={{ y: -5 }}
                      className="bg-white border border-gray-100 rounded-sm p-8 shadow-xl shadow-indigo-50 border-t-4 border-t-indigo-600"
                    >
                      <div className={`text-${m.color}-600 mb-6`}>{m.icon}</div>
                      <p className="text-4xl font-display font-bold text-gray-900">{m.value}</p>
                      <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">{m.label}</p>
                    </motion.div>
                  ))}
               </div>

               <div className="bg-white border border-gray-100 rounded-sm p-8 space-y-8">
                  <div className="flex justify-between items-end">
                    <div>
                      <h4 className="font-display font-bold text-xl text-gray-900">Psychographic Interest Tree</h4>
                      <p className="text-xs text-gray-400 uppercase tracking-widest mt-1">Based on your reading history</p>
                    </div>
                    <PieChart size={24} className="text-indigo-400" />
                  </div>

                  <div className="space-y-6">
                    {analyticsLoading ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="animate-spin text-indigo-500" size={24} />
                      </div>
                    ) : analytics.genreTree && analytics.genreTree.length > 0 ? (
                      analytics.genreTree.map((genre, idx) => {
                        const colors = ['bg-indigo-600', 'bg-blue-500', 'bg-teal-500', 'bg-purple-500', 'bg-pink-500'];
                        const color = colors[idx % colors.length];
                        return (
                          <div key={genre.category} className="space-y-2">
                            <div className="flex justify-between text-[10px] font-bold uppercase tracking-widest">
                              <span className="text-gray-600">{genre.category}</span>
                              <span className="text-gray-400 font-mono">{genre.percentage}%</span>
                            </div>
                            <div className="w-full h-1.5 bg-gray-50 rounded-full overflow-hidden">
                               <div className={`h-full ${color}`} style={{ width: `${genre.percentage}%` }} />
                            </div>
                          </div>
                        );
                      })
                    ) : (
                      <div className="text-center py-8 text-gray-400">
                        <p className="text-sm">No reading data yet</p>
                        <p className="text-xs mt-1">Borrow and return books to see your genre preferences</p>
                      </div>
                    )}
                  </div>
               </div>
            </div>

            {/* Column 2: Intelligence Settings */}
            <div className="space-y-8">
               <h3 className="text-2xl font-display font-bold">Neural Controls</h3>
               <div className="bg-gray-900 rounded-sm p-8 space-y-8 text-white">
                  <div className="space-y-4">
                     <p className="text-xs text-indigo-400 font-bold uppercase tracking-widest">Inference Level</p>
                     <div className="flex justify-between items-center p-3 bg-white/5 border border-white/10 rounded-sm">
                        <span className="text-sm">{analytics.neuralControls?.inferenceLevel || 'Exploratory'}</span>
                        <button
                          onClick={() => {
                            const levels = ['Exploratory', 'Balanced', 'Focused'];
                            const current = analytics.neuralControls?.inferenceLevel || 'Exploratory';
                            const nextIndex = (levels.indexOf(current) + 1) % levels.length;
                            neuralControlsMutation.mutate({ inferenceLevel: levels[nextIndex] });
                          }}
                          disabled={neuralControlsMutation.isPending || analyticsLoading}
                          className="w-10 h-6 bg-indigo-600 rounded-full p-1 flex justify-end items-center px-1.5 hover:bg-indigo-500 transition-colors disabled:opacity-50"
                        >
                           <div className="w-3 h-3 bg-white rounded-full shadow-lg" />
                        </button>
                     </div>
                     <p className="text-[10px] text-gray-500">Click to cycle: Exploratory → Balanced → Focused</p>
                  </div>

                  <div className="space-y-4">
                     <p className="text-xs text-indigo-400 font-bold uppercase tracking-widest">Privacy Projection</p>
                     <div className="flex justify-between items-center p-3 bg-white/5 border border-white/10 rounded-sm">
                        <span className="text-sm">{analytics.neuralControls?.obfuscationMode ? 'Obfuscation On' : 'Standard'}</span>
                        <button
                          onClick={() => {
                            const current = analytics.neuralControls?.obfuscationMode || false;
                            neuralControlsMutation.mutate({ obfuscationMode: !current });
                          }}
                          disabled={neuralControlsMutation.isPending || analyticsLoading}
                          className={`w-10 h-6 rounded-full p-1 flex items-center px-1.5 transition-colors disabled:opacity-50 ${
                            analytics.neuralControls?.obfuscationMode ? 'bg-indigo-600 justify-end' : 'bg-gray-600 justify-start'
                          }`}
                        >
                           <div className="w-3 h-3 bg-white rounded-full shadow-lg" />
                        </button>
                     </div>
                  </div>

                  <div className="pt-4 border-t border-white/10">
                     <button
                        onClick={() => {
                           neuralControlsMutation.mutate({ inferenceLevel: 'Exploratory', obfuscationMode: false });
                        }}
                        disabled={neuralControlsMutation.isPending}
                        className="w-full py-4 bg-white/5 border border-white/10 text-white rounded-sm font-bold uppercase tracking-widest text-[10px] hover:bg-red-500 hover:border-red-500 transition-all flex items-center justify-center disabled:opacity-50"
                     >
                        {neuralControlsMutation.isPending ? (
                          <Loader2 size={14} className="mr-2 animate-spin" />
                        ) : (
                          <RefreshCw size={14} className="mr-2" />
                        )}
                        Reset Affinity Vector
                     </button>
                     <p className="text-[10px] text-indigo-300/40 text-center mt-4">Resetting will clear all AI discovery personalization.</p>
                  </div>
               </div>
            </div>
        </div>

        {isEditOpen && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={handleEditClose}
          >
            <motion.div 
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-white rounded-sm max-w-md w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-gray-200"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="sticky top-0 bg-white border-b border-gray-200 p-6 flex items-center justify-between">
                <h3 className="text-xl font-display font-bold">Edit Profile</h3>
                <button onClick={handleEditClose} className="p-1 hover:bg-gray-100 rounded-sm">
                  <X size={20} />
                </button>
              </div>
              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-1">Full Name</label>
                  <input 
                    type="text" 
                    value={editData.name}
                    onChange={(e) => setEditData({...editData, name: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-1">Phone</label>
                  <input 
                    type="tel" 
                    value={editData.phone}
                    onChange={(e) => setEditData({...editData, phone: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  />
                </div>
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-1">Gender</label>
                  <select 
                    value={editData.gender}
                    onChange={(e) => setEditData({...editData, gender: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  >
                    <option value="">Select Gender</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-bold text-gray-700 mb-1">Date of Birth</label>
                  <input 
                    type="date" 
                    value={editData.dateOfBirth}
                    onChange={(e) => setEditData({...editData, dateOfBirth: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-sm focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  />
                </div>
                <div className="flex gap-3 pt-2">
                  <button 
                    type="button"
                    onClick={handleEditClose}
                    className="flex-1 py-2.5 px-4 border border-gray-300 text-gray-700 font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-gray-50 transition-all"
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit" 
                    disabled={updateMutation.isPending}
                    className="flex-1 py-2.5 px-4 bg-indigo-600 text-white font-bold uppercase tracking-widest text-xs rounded-sm hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-waiting transition-all flex items-center justify-center"
                  >
                    {updateMutation.isPending ? (
                      <>
                        <Loader2 className="animate-spin w-4 h-4 mr-2" />
                        Saving...
                      </>
                    ) : (
                      <>
                        <Save className="w-4 h-4 mr-2" />
                        Save Changes
                      </>
                    )}
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </div>
    </PageTransition>
  );
};

export default MemberProfilePage;
