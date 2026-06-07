import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/admin.api';
import PageTransition from '../../components/shared/PageTransition';
import { Settings, Save, RefreshCw, Server, Cpu, Database, Bell, Send } from 'lucide-react';
import { toast } from 'react-hot-toast';

const AdminConfigPage = () => {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState(null);
  const [broadcastMessage, setBroadcastMessage] = useState('');

  const { data: configData, isLoading } = useQuery({
    queryKey: ['adminConfig'],
    queryFn: adminApi.getConfig
  });

  useEffect(() => {
    if (configData?.data) {
      const backendData = configData.data.data || configData.data;
      setFormData({
        maxActiveLoans: backendData.maxActiveLoans ?? 5,
        loanDurationDays: backendData.loanDurationDays ?? 14,
        fineRatePerDay: backendData.fineRatePerDay ?? 1.0,
        gracePeriodDays: backendData.gracePeriodDays ?? 3,
        chatbotEnabled: backendData.chatbotEnabled ?? false,
        aiModelId: backendData.aiModelId ?? 'default',
        aiMaxTokens: backendData.aiMaxTokens ?? 1000,
        chatbotMaxMessagesPerSession: backendData.chatbotMaxMessagesPerSession ?? 10,
        chatbotMaxTokens: backendData.chatbotMaxTokens ?? 500,
        chatbotContextWindow: backendData.chatbotContextWindow ?? 5
      });
    }
  }, [configData]);

  const updateMutation = useMutation({
    mutationFn: (data) => adminApi.updateConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['adminConfig']);
      queryClient.invalidateQueries(['adminMembers']);
      queryClient.invalidateQueries(['staffMembers']);
      queryClient.invalidateQueries(['adminLoans']);
      queryClient.invalidateQueries(['adminAnalytics']);
      toast.success('System configuration updated');
    }
  });

  const broadcastMutation = useMutation({
    mutationFn: (message) => adminApi.broadcastNotification(message),
    onSuccess: () => {
      toast.success('Broadcast message sent successfully to all users!');
      setBroadcastMessage('');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to send broadcast message');
    }
  });

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox'
        ? checked
        : (type === 'number' ? (value.includes('.') ? parseFloat(value) : parseInt(value, 10)) : value)
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    updateMutation.mutate(formData);
  };

  const handleBroadcastSend = () => {
    if (!broadcastMessage.trim()) {
      toast.error('Please enter a broadcast message');
      return;
    }
    broadcastMutation.mutate(broadcastMessage);
  };

  if (isLoading || !formData) {
    return <div className="p-8"><RefreshCw className="animate-spin text-red-500" /></div>;
  }

  return (
    <PageTransition>
      <div className="p-8 space-y-8 max-w-4xl">
        <div className="flex justify-between items-end">
          <div>
            <h1 className="text-3xl font-display font-bold">System Parameters</h1>
            <p className="text-gray-500 text-sm mt-1">Global logic overrides and ecosystem configuration.</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-12">
          {/* Circulation Logic */}
          <section className="space-y-6">
            <h3 className="text-xs font-mono uppercase tracking-[0.3em] text-red-500 flex items-center">
              <Database size={14} className="mr-2" /> Circulation Policies
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="space-y-2">
                <label className="block text-sm font-bold text-gray-700">Maximum Books per Member</label>
                <input
                  type="number"
                  name="maxActiveLoans"
                  value={formData.maxActiveLoans}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border-2 border-gray-100 focus:border-red-500 focus:outline-none transition-colors font-mono"
                />
              </div>
              <div className="space-y-2">
                <label className="block text-sm font-bold text-gray-700">Standard Loan Duration (Days)</label>
                <input
                  type="number"
                  name="loanDurationDays"
                  value={formData.loanDurationDays}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border-2 border-gray-100 focus:border-red-500 focus:outline-none transition-colors font-mono"
                />
              </div>
              <div className="space-y-2">
                <label className="block text-sm font-bold text-gray-700">Daily Fine Rate ($)</label>
                <input
                  type="number"
                  step="0.1"
                  name="fineRatePerDay"
                  value={formData.fineRatePerDay}
                  onChange={handleChange}
                  className="w-full px-4 py-3 border-2 border-gray-100 focus:border-red-500 focus:outline-none transition-colors font-mono"
                />
              </div>
            </div>
          </section>

          {/* AI Convergence */}
          <section className="space-y-6">
            <h3 className="text-xs font-mono uppercase tracking-[0.3em] text-teal-500 flex items-center">
              <Cpu size={14} className="mr-2" /> AI Core Integration
            </h3>
            <div className="space-y-4">
              <label className="relative inline-flex items-center cursor-pointer group">
                <input
                  type="checkbox"
                  name="chatbotEnabled"
                  checked={formData.chatbotEnabled}
                  onChange={handleChange}
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-teal-500"></div>
                <span className="ml-3 text-sm font-bold text-gray-700 uppercase tracking-tighter">Enable Vector Search & AI Recommendations</span>
              </label>
            </div>
          </section>

          {/* Infrastructure */}
          <section className="space-y-6">
            <h3 className="text-xs font-mono uppercase tracking-[0.3em] text-blue-500 flex items-center">
              <Server size={14} className="mr-2" /> Global Notifications
            </h3>
            <div className="bg-gray-50 p-6 border border-gray-200">
              <div className="flex items-center space-x-4 mb-4">
                <Bell size={20} className="text-gray-400" />
                <h4 className="font-bold text-gray-900">Broadcast Maintenance Message</h4>
              </div>
              <div className="space-y-4">
                <textarea
                  value={broadcastMessage}
                  onChange={(e) => setBroadcastMessage(e.target.value)}
                  placeholder="Message will appear in all user portals..."
                  className="w-full h-24 px-4 py-2 border-2 border-white focus:border-blue-500 focus:outline-none transition-colors text-sm resize-none"
                />
                <div className="flex justify-end">
                  <button
                    onClick={handleBroadcastSend}
                    disabled={broadcastMutation.isPending || !broadcastMessage.trim()}
                    className="px-6 py-2 bg-blue-600 text-white font-medium text-sm rounded-lg hover:bg-blue-700 transition-all flex items-center space-x-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {broadcastMutation.isPending ? (
                      <>
                        <RefreshCw size={16} className="animate-spin" />
                        <span>Sending...</span>
                      </>
                    ) : (
                      <>
                        <Send size={16} />
                        <span>Send Broadcast</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </section>

          <div className="pt-8 border-t border-gray-100 flex justify-end">
            <button
              type="submit"
              disabled={updateMutation.isPending}
              className="px-8 py-4 bg-gray-900 text-white font-bold uppercase tracking-widest hover:bg-black transition-all flex items-center space-x-2"
            >
              <Save size={18} />
              <span>{updateMutation.isPending ? 'Committing...' : 'Commit Changes'}</span>
            </button>
          </div>
        </form>
      </div>
    </PageTransition>
  );
};

export default AdminConfigPage;
