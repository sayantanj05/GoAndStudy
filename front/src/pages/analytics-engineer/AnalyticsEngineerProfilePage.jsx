import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import PageTransition from '../../components/shared/PageTransition';
import { analyticsEngineerApi } from '../../api/analyticsEngineer.api';
import { 
  User, Settings, Bell, Shield, Database, Activity, 
  TrendingUp, BookOpen, Users, BarChart3, AlertTriangle,
  Mail, Phone, Calendar, MapPin, Award, Target,
  Edit2, Save, X, CheckCircle, Clock, FileText
} from 'lucide-react';
import { toast } from 'react-hot-toast';

const AnalyticsEngineerProfilePage = () => {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [activeTab, setActiveTab] = useState('overview');
  const [formData, setFormData] = useState({
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@goandstudy.com',
    phone: '+1 234 567 8900',
    department: 'Data Engineering',
    role: 'Senior Analytics Engineer',
    location: 'New York, NY',
    bio: 'Experienced analytics engineer specializing in ETL pipelines, data quality assurance, and business intelligence solutions.',
    preferences: {
      emailNotifications: true,
      pushNotifications: false,
      weeklyReports: true,
      alertThreshold: 'high',
      dashboardRefresh: '5min'
    }
  });

  const { data: profileData, isLoading } = useQuery({
    queryKey: ['analyticsEngineerProfile'],
    queryFn: analyticsEngineerApi.getProfile,
    staleTime: 300_000,
  });

  const { data: statsData } = useQuery({
    queryKey: ['analyticsEngineerStats'],
    queryFn: analyticsEngineerApi.getProfileStats,
    staleTime: 60_000,
  });

  const updateProfileMutation = useMutation({
    mutationFn: analyticsEngineerApi.updateProfile,
    onSuccess: () => {
      toast.success('Profile updated successfully');
      setIsEditing(false);
      queryClient.invalidateQueries(['analyticsEngineerProfile']);
    },
    onError: () => toast.error('Failed to update profile')
  });

  const profile = profileData?.data || profileData || {};
  const stats = statsData?.data || statsData || {};

  const handleSaveProfile = () => {
    updateProfileMutation.mutate(formData);
  };

  const handleCancelEdit = () => {
    setFormData({
      firstName: profile.firstName || 'John',
      lastName: profile.lastName || 'Doe',
      email: profile.email || 'john.doe@goandstudy.com',
      phone: profile.phone || '+1 234 567 8900',
      department: profile.department || 'Data Engineering',
      role: profile.role || 'Senior Analytics Engineer',
      location: profile.location || 'New York, NY',
      bio: profile.bio || 'Experienced analytics engineer...',
      preferences: profile.preferences || formData.preferences
    });
    setIsEditing(false);
  };

  const tabs = [
    { id: 'overview', label: 'Overview', icon: User },
    { id: 'analytics', label: 'Analytics Access', icon: BarChart3 },
    { id: 'preferences', label: 'Preferences', icon: Settings },
    { id: 'activity', label: 'Recent Activity', icon: Clock }
  ];

  const analyticsAccess = [
    { 
      name: 'Executive Overview', 
      description: 'High-level business metrics and KPIs',
      access: true,
      lastAccessed: '2 hours ago'
    },
    { 
      name: 'Pipeline Analytics', 
      description: 'ETL performance and data flow monitoring',
      access: true,
      lastAccessed: '1 day ago'
    },
    { 
      name: 'Data Quality', 
      description: 'Quality metrics and anomaly detection',
      access: true,
      lastAccessed: '3 hours ago'
    },
    { 
      name: 'Business Intelligence', 
      description: 'Member engagement and content analytics',
      access: true,
      lastAccessed: '5 hours ago'
    },
    { 
      name: 'Member Analytics', 
      description: 'User behavior and preference analysis',
      access: true,
      lastAccessed: '1 day ago'
    },
    { 
      name: 'Book Analytics', 
      description: 'Content performance and circulation metrics',
      access: true,
      lastAccessed: '2 days ago'
    },
    { 
      name: 'System Performance', 
      description: 'Infrastructure and resource monitoring',
      access: true,
      lastAccessed: '4 hours ago'
    },
    { 
      name: 'Anomaly Detection', 
      description: 'Real-time monitoring and alerts',
      access: true,
      lastAccessed: '30 mins ago'
    }
  ];

  const recentActivity = [
    { action: 'Generated Pipeline Performance Report', time: '2 hours ago', type: 'report' },
    { action: 'Resolved Data Quality Anomaly', time: '3 hours ago', type: 'quality' },
    { action: 'Started ETL Job: Member Sync', time: '5 hours ago', type: 'etl' },
    { action: 'Updated Validation Rules', time: '1 day ago', type: 'configuration' },
    { action: 'Exported Business Analytics', time: '2 days ago', type: 'export' }
  ];

  return (
    <PageTransition>
      <div className="p-8 space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-display font-bold">Profile</h1>
            <p className="text-gray-500 text-sm mt-1">Manage your profile and analytics access settings.</p>
          </div>
          <div className="flex items-center gap-3">
            {isEditing ? (
              <>
                <button
                  onClick={handleCancelEdit}
                  className="flex items-center gap-2 px-4 py-2 border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  <X size={16} /> Cancel
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={updateProfileMutation.isLoading}
                  className="flex items-center gap-2 bg-orange-600 text-white px-4 py-2 text-sm font-bold uppercase tracking-widest hover:bg-orange-700 transition-colors disabled:opacity-50"
                >
                  <Save size={16} /> {updateProfileMutation.isLoading ? 'Saving...' : 'Save'}
                </button>
              </>
            ) : (
              <button
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-2 bg-orange-600 text-white px-4 py-2 text-sm font-bold uppercase tracking-widest hover:bg-orange-700 transition-colors"
              >
                <Edit2 size={16} /> Edit Profile
              </button>
            )}
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex space-x-1 border-b border-gray-200">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center space-x-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-orange-500 text-orange-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <tab.icon size={16} />
              <span>{tab.label}</span>
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="h-64 flex items-center justify-center">
            <div className="animate-spin text-orange-500" size={48} />
          </div>
        ) : (
          <>
            {/* Overview Tab */}
            {activeTab === 'overview' && (
              <div className="space-y-6">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  {/* Profile Information */}
                  <div className="lg:col-span-2 space-y-6">
                    <div className="bg-white border border-gray-200 p-6">
                      <div className="flex items-center gap-4 mb-6">
                        <div className="w-20 h-20 bg-orange-100 rounded-full flex items-center justify-center">
                          <User size={32} className="text-orange-600" />
                        </div>
                        <div>
                          <h2 className="text-2xl font-bold">{formData.firstName} {formData.lastName}</h2>
                          <p className="text-gray-600">{formData.role}</p>
                          <div className="flex items-center gap-2 mt-1">
                            <CheckCircle size={14} className="text-green-500" />
                            <span className="text-sm text-green-600">Active</span>
                          </div>
                        </div>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">First Name</label>
                          <input
                            type="text"
                            value={formData.firstName}
                            onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Last Name</label>
                          <input
                            type="text"
                            value={formData.lastName}
                            onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Email</label>
                          <input
                            type="email"
                            value={formData.email}
                            onChange={(e) => setFormData({...formData, email: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Phone</label>
                          <input
                            type="tel"
                            value={formData.phone}
                            onChange={(e) => setFormData({...formData, phone: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Department</label>
                          <input
                            type="text"
                            value={formData.department}
                            onChange={(e) => setFormData({...formData, department: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                        <div>
                          <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Location</label>
                          <input
                            type="text"
                            value={formData.location}
                            onChange={(e) => setFormData({...formData, location: e.target.value})}
                            disabled={!isEditing}
                            className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50"
                          />
                        </div>
                      </div>

                      <div className="mt-4">
                        <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Bio</label>
                        <textarea
                          rows={3}
                          value={formData.bio}
                          onChange={(e) => setFormData({...formData, bio: e.target.value})}
                          disabled={!isEditing}
                          className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200 disabled:bg-gray-50 resize-none"
                        />
                      </div>
                    </div>
                  </div>

                  {/* Stats & Quick Info */}
                  <div className="space-y-6">
                    <div className="bg-white border border-gray-200 p-6">
                      <h3 className="text-lg font-display font-bold mb-4">Performance Stats</h3>
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Reports Generated</span>
                          <span className="text-sm font-bold">{stats.reportsGenerated || 247}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">ETL Jobs Managed</span>
                          <span className="text-sm font-bold">{stats.etlJobsManaged || 89}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Anomalies Resolved</span>
                          <span className="text-sm font-bold">{stats.anomaliesResolved || 156}</span>
                        </div>
                        <div className="flex items-center justify-between">
                          <span className="text-sm text-gray-600">Quality Score Maintained</span>
                          <span className="text-sm font-bold text-green-600">{stats.qualityScore || 95.2}%</span>
                        </div>
                      </div>
                    </div>

                    <div className="bg-white border border-gray-200 p-6">
                      <h3 className="text-lg font-display font-bold mb-4">Quick Info</h3>
                      <div className="space-y-3">
                        <div className="flex items-center gap-2">
                          <Calendar size={16} className="text-gray-400" />
                          <span className="text-sm text-gray-600">Joined: {profile.joinDate || 'Jan 15, 2023'}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Shield size={16} className="text-gray-400" />
                          <span className="text-sm text-gray-600">Role: {formData.role}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Target size={16} className="text-gray-400" />
                          <span className="text-sm text-gray-600">Department: {formData.department}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Award size={16} className="text-gray-400" />
                          <span className="text-sm text-gray-600">Certifications: 3</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Analytics Access Tab */}
            {activeTab === 'analytics' && (
              <div className="space-y-6">
                <div className="bg-white border border-gray-200 p-6">
                  <h3 className="text-lg font-display font-bold mb-4">Analytics Portal Access</h3>
                  <p className="text-sm text-gray-600 mb-6">Manage your access to different analytics dashboards and reports.</p>
                  
                  <div className="space-y-3">
                    {analyticsAccess.map((item, index) => (
                      <div key={index} className="flex items-center justify-between p-4 border border-gray-100">
                        <div className="flex items-center gap-3">
                          <div className={`w-2 h-2 rounded-full ${
                            item.access ? 'bg-green-500' : 'bg-red-500'
                          }`} />
                          <div>
                            <p className="text-sm font-medium">{item.name}</p>
                            <p className="text-xs text-gray-500">{item.description}</p>
                          </div>
                        </div>
                        <div className="text-right">
                          <span className={`px-2 py-1 text-xs font-bold uppercase ${
                            item.access ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {item.access ? 'Access Granted' : 'No Access'}
                          </span>
                          <p className="text-xs text-gray-500 mt-1">{item.lastAccessed}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {/* Preferences Tab */}
            {activeTab === 'preferences' && (
              <div className="space-y-6">
                <div className="bg-white border border-gray-200 p-6">
                  <h3 className="text-lg font-display font-bold mb-4">Notification Preferences</h3>
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium">Email Notifications</p>
                        <p className="text-xs text-gray-500">Receive email alerts for critical events</p>
                      </div>
                      <button
                        onClick={() => setFormData({
                          ...formData,
                          preferences: {...formData.preferences, emailNotifications: !formData.preferences.emailNotifications}
                        })}
                        className={`w-12 h-6 rounded-full transition-colors ${
                          formData.preferences.emailNotifications ? 'bg-orange-500' : 'bg-gray-300'
                        }`}
                      >
                        <div className={`w-5 h-5 bg-white rounded-full transition-transform ${
                          formData.preferences.emailNotifications ? 'translate-x-6' : 'translate-x-0.5'
                        }`} />
                      </button>
                    </div>
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium">Push Notifications</p>
                        <p className="text-xs text-gray-500">Real-time browser notifications</p>
                      </div>
                      <button
                        onClick={() => setFormData({
                          ...formData,
                          preferences: {...formData.preferences, pushNotifications: !formData.preferences.pushNotifications}
                        })}
                        className={`w-12 h-6 rounded-full transition-colors ${
                          formData.preferences.pushNotifications ? 'bg-orange-500' : 'bg-gray-300'
                        }`}
                      >
                        <div className={`w-5 h-5 bg-white rounded-full transition-transform ${
                          formData.preferences.pushNotifications ? 'translate-x-6' : 'translate-x-0.5'
                        }`} />
                      </button>
                    </div>
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-medium">Weekly Reports</p>
                        <p className="text-xs text-gray-500">Automated weekly summary emails</p>
                      </div>
                      <button
                        onClick={() => setFormData({
                          ...formData,
                          preferences: {...formData.preferences, weeklyReports: !formData.preferences.weeklyReports}
                        })}
                        className={`w-12 h-6 rounded-full transition-colors ${
                          formData.preferences.weeklyReports ? 'bg-orange-500' : 'bg-gray-300'
                        }`}
                      >
                        <div className={`w-5 h-5 bg-white rounded-full transition-transform ${
                          formData.preferences.weeklyReports ? 'translate-x-6' : 'translate-x-0.5'
                        }`} />
                      </button>
                    </div>
                  </div>
                </div>

                <div className="bg-white border border-gray-200 p-6">
                  <h3 className="text-lg font-display font-bold mb-4">Dashboard Settings</h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Alert Threshold</label>
                      <select
                        value={formData.preferences.alertThreshold}
                        onChange={(e) => setFormData({
                          ...formData,
                          preferences: {...formData.preferences, alertThreshold: e.target.value}
                        })}
                        className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
                      >
                        <option value="low">Low - All alerts</option>
                        <option value="medium">Medium - Important alerts</option>
                        <option value="high">High - Critical alerts only</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-[10px] font-mono uppercase text-gray-500 mb-1">Dashboard Refresh</label>
                      <select
                        value={formData.preferences.dashboardRefresh}
                        onChange={(e) => setFormData({
                          ...formData,
                          preferences: {...formData.preferences, dashboardRefresh: e.target.value}
                        })}
                        className="w-full px-3 py-2 border border-gray-200 focus:outline-none focus:ring-2 focus:ring-orange-200"
                      >
                        <option value="1min">1 minute</option>
                        <option value="5min">5 minutes</option>
                        <option value="10min">10 minutes</option>
                        <option value="30min">30 minutes</option>
                      </select>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Activity Tab */}
            {activeTab === 'activity' && (
              <div className="space-y-6">
                <div className="bg-white border border-gray-200 p-6">
                  <h3 className="text-lg font-display font-bold mb-4">Recent Activity</h3>
                  <div className="space-y-3">
                    {recentActivity.map((activity, index) => (
                      <div key={index} className="flex items-start gap-3 p-3 border border-gray-100">
                        <div className={`w-2 h-2 rounded-full mt-1.5 ${
                          activity.type === 'report' ? 'bg-blue-500' :
                          activity.type === 'quality' ? 'bg-green-500' :
                          activity.type === 'etl' ? 'bg-purple-500' :
                          activity.type === 'configuration' ? 'bg-orange-500' : 'bg-gray-500'
                        }`} />
                        <div className="flex-1">
                          <p className="text-sm font-medium">{activity.action}</p>
                          <p className="text-xs text-gray-500">{activity.time}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </PageTransition>
  );
};

export default AnalyticsEngineerProfilePage;
