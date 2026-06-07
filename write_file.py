import os

content = '''import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { toast } from 'react-hot-toast';
import { Mail, Lock, User, ShieldCheck, UserCheck, Users, ArrowRight, Loader2, Phone, ArrowLeft } from 'lucide-react';
import { authApi } from '../../api/auth.api';
import { motion, AnimatePresence } from 'framer-motion';
import SunlightBackground from '../../components/SunlightBackground';

const LoginRegisterPage = () => {
  const [isBookOpen, setIsBookOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register, user, isAuthenticated } = useAuthStore();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    phone: '',
    confirmPassword: ''
  });

  useEffect(() => {
    if (isAuthenticated && user) {
      const role = user.role;
      if (role === 'ROLE_ADMIN') navigate('/admin/dashboard');
      else if (role === 'ROLE_STAFF') navigate('/staff/dashboard');
      else navigate('/member/home');
    }
  }, [isAuthenticated, user, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleAuth = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isBookOpen) {
        if (formData.password !== formData.confirmPassword) {
          toast.error('Passwords do not match');
          setLoading(false);
          return;
        }
        console.log('Attempting registration...', formData);
        await register({
          email: formData.email,
          password: formData.password,
          name: formData.name,
          phone: formData.phone
        });
        toast.success('Account created successfully!');
      } else {
        console.log('Attempting login...', formData.email);
        await login(formData.email, formData.password);
        toast.success('Welcome back to GoAndStudy!');
      }
    } catch (error) {
      console.error('Auth error:', error);
      toast.error(error.response?.data?.message || error.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const quickFill = (email, password) => {
    setFormData({ ...formData, email, password });
  };

  const openBook = () => setIsBookOpen(true);
  const closeBook = () => setIsBookOpen(false);

  return (
    <div className="min-h-screen grid lg:grid-cols-2 overflow-hidden relative">
      <SunlightBackground />

      {/* Left Side: CSS Art & Branding */}
      <div className="hidden lg:flex flex-col justify-between p-12 overflow-hidden relative">
        <div className="absolute inset-0 z-0">
          <div className="absolute top-[-10%] left-[-10%] w-[60%] h-[60%] border border-white/5 rotate-12" />
          <div className="absolute bottom-[-20%] right-[-10%] w-[70%] h-[70%] border border-white/5 -rotate-12" />
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-px h-[120%] bg-gradient-to-b from-transparent via-white/10 to-transparent" />
        </div>

        <div className="relative z-10">
          <h1 className="text-4xl font-display font-bold text-red-500 mb-2">GoAndStudy</h1>
          <p className="font-mono text-[10px] tracking-[0.3em] uppercase text-gray-400">
            Intelligent Library Ecosystem
          </p>
        </div>

        <div className="relative z-10 space-y-8 max-w-md">
          <div className="space-y-4">
            <h2 className="text-5xl font-display font-bold text-white leading-[1.1]">
              Knowledge is <br />
              <span className="text-amber-500">Dynamic.</span>
            </h2>
            <p className="text-gray-400 font-light leading-relaxed">
              Experience the future of library management with real-time analytics, AI-curated recommendations, and seamless collaboration.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="p-4 border border-amber-500/20 bg-amber-500/5">
              <span className="block text-2xl font-display font-bold text-white">40k+</span>
              <span className="text-[10px] uppercase tracking-wider text-gray-500">Digital Assets</span>
            </div>
            <div className="p-4 border border-amber-500/20 bg-amber-500/5">
              <span className="block text-2xl font-display font-bold text-white">0.2s</span>
              <span className="text-[10px] uppercase tracking-wider text-gray-500">Search Latency</span>
            </div>
          </div>
        </div>

        <div className="relative z-10 flex items-center space-x-4">
          <div className="w-12 h-12 bg-amber-500/20 border border-amber-500/30 flex items-center justify-center">
            <ShieldCheck className="text-amber-500" size={24} />
          </div>
          <div>
            <p className="text-white font-semibold">Secure Access</p>
            <p className="text-gray-500 text-sm">End-to-end encrypted</p>
          </div>
        </div>
      </div>

      {/* Right Side: 3D Book */}
      <div className="flex items-center justify-center p-6 lg:p-12 relative z-10">
        {/* Book Container with 3D Perspective */}
        <div 
          className="relative w-full max-w-md"
          style={{ 
            perspective: '1500px',
            transformStyle: 'preserve-3d'
          }}
        >
          {/* Book Spine Effect */}
          <div className="absolute left-0 top-0 bottom-0 w-4 bg-gradient-to-r from-gray-800 to-gray-700 rounded-l-lg z-20" />

          {/* 3D Book Container */}
          <div 
            className="relative"
            style={{
              transformStyle: 'preserve-3d',
              transition: 'transform 1.2s cubic-bezier(0.4, 0, 0.2, 1)'
            }}
          >
            {/* COVER - Sign In Form (Front) */}
            <motion.div
              className="relative bg-gray-900 rounded-r-lg p-8 shadow-2xl"
              style={{
                transformStyle: 'preserve-3d',
                backfaceVisibility: 'hidden',
                transformOrigin: 'left center',
              }}
              initial={false}
              animate={{ rotateY: isBookOpen ? -180 : 0 }}
              transition={{ duration: 1.2, ease: [0.4, 0, 0.2, 1] }}
            >
              {/* Cover Texture/Design */}
              <div 
                className="absolute inset-0 rounded-r-lg opacity-20"
                style={{
                  backgroundImage: 'url("data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.65\' numOctaves=\'3\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\'/%3E%3C/svg%3E")',
                }}
              />
              <div className="absolute top-0 right-0 w-32 h-32 bg-amber-500/10 rounded-full blur-3xl" />

              {/* Cover Content */}
              <div className="relative z-10">
                <div className="text-center mb-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 bg-amber-500/20 rounded-full mb-4">
                    <UserCheck className="text-amber-500" size={32} />
                  </div>
                  <h2 className="text-2xl font-bold text-white mb-2">Welcome Back</h2>
                  <p className="text-gray-400 text-sm">Sign in to continue your journey</p>
                </div>

                {/* Quick Fill Buttons */}
                <div className="flex flex-wrap gap-2 mb-6">
                  <span className="text-xs text-gray-500 w-full mb-1">Quick Demo Login:</span>
                  <button 
                    onClick={() => quickFill('admin@gmail.com', 'admin123')}
                    className="px-3 py-1.5 bg-gray-800 border border-gray-700 text-[10px] font-bold uppercase tracking-wider text-gray-300 hover:border-amber-500 hover:text-amber-500 transition-all flex items-center"
                  >
                    <ShieldCheck size={12} className="mr-1.5" /> Admin
                  </button>
                  <button 
                    onClick={() => quickFill('staff@gmail.com', 'staff123')}
                    className="px-3 py-1.5 bg-gray-800 border border-gray-700 text-[10px] font-bold uppercase tracking-wider text-gray-300 hover:border-amber-500 hover:text-amber-500 transition-all flex items-center"
                  >
                    <UserCheck size={12} className="mr-1.5" /> Staff
                  </button>
                  <button 
                    onClick={() => quickFill('member@gmail.com', 'member123')}
                    className="px-3 py-1.5 bg-gray-800 border border-gray-700 text-[10px] font-bold uppercase tracking-wider text-gray-300 hover:border-amber-500 hover:text-amber-500 transition-all flex items-center"
                  >
                    <Users size={12} className="mr-1.5" /> Member
                  </button>
                </div>

                <form onSubmit={handleAuth} className="space-y-5">
                  <div className="space-y-4">
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
                      <input
                        type="email"
                        name="email"
                        placeholder="Email Address"
                        required
                        value={formData.email}
                        onChange={handleChange}
                        className="w-full pl-10 pr-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg focus:border-amber-500 focus:outline-none transition-colors font-medium text-white placeholder-gray-500"
                      />
                    </div>
                    <div className="relative">
                      <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" size={18} />
                      <input
                        type="password"
                        name="password"
                        placeholder="Password"
                        required
                        value={formData.password}
                        onChange={handleChange}
                        className="w-full pl-10 pr-4 py-3 bg-gray-800/50 border border-gray-700 rounded-lg focus:border-amber-500 focus:outline-none transition-colors font-medium text-white placeholder-gray-500"
                      />
                    </div>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3.5 bg-amber-500 text-gray-900 font-bold uppercase tracking-widest hover:bg-amber-400 transition-colors flex items-center justify-center group shadow-lg shadow-amber-500/20"
                  >
                    {loading ? (
                      <Loader2 className="animate-spin" size={20} />
                    ) : (
                      <>
                        Sign In
                        <ArrowRight className="ml-2 group-hover:translate-x-1 transition-transform" size={18} />
                      </>
                    )}
                  </button>
                </form>

                <div className="text-center mt-6">
                  <button
                    onClick={openBook}
                    className="text-xs uppercase tracking-widest font-bold text-gray-400 hover:text-amber-500 transition-colors"
                  >
                    Don't have an account? Create one
                  </button>
                </div>
              </div>
            </motion.div>

            {/* INTERNAL PAGE - Registration Form (Back, revealed when cover flips) */}
            <motion.div
              className="absolute inset-0 bg-amber-50 rounded-r-lg p-8 shadow-2xl"
              style={{
                transformStyle: 'preserve-3d',
                backfaceVisibility: 'hidden',
                transformOrigin: 'left center',
              }}
              initial={{ rotateY: 180 }}
              animate={{ rotateY: isBookOpen ? 0 : 180 }}
              transition={{ duration: 1.2, ease: [0.4, 0, 0.2, 1] }}
            >
              {/* Paper Texture Overlay */}
              <div 
                className="absolute inset-0 rounded-r-lg opacity-30"
                style={{
                  backgroundImage: 'url("data:image/svg+xml,%3Csvg viewBox=\'0 0 200 200\' xmlns=\'http://www.w3.org/2000/svg\'%3E%3Cfilter id=\'noiseFilter\'%3E%3CfeTurbulence type=\'fractalNoise\' baseFrequency=\'0.9\' numOctaves=\'4\' stitchTiles=\'stitch\'/%3E%3C/filter%3E%3Crect width=\'100%25\' height=\'100%25\' filter=\'url(%23noiseFilter)\'/%3E%3C/svg%3E")',
                }}
              />
              <div className="absolute inset-8 right-8 bg-gradient-to-r from-transparent via-amber-900/5 to-transparent" />
              <div className="absolute top-0 right-0 w-24 h-24 bg-amber-200/30 rounded-full blur-3xl" />

              {/* Page Content */}
              <div className="relative z-10">
                <div className="text-center mb-6">
                  <div className="inline-flex items-center justify-center w-14 h-14 bg-amber-500 rounded-full mb-3">
                    <User className="text-white" size={28} />
                  </div>
                  <h2 className="text-2xl font-bold text-gray-800 mb-1">Join Us</h2>
                  <p className="text-gray-600 text-sm">Create your account</p>
                </div>

                <form onSubmit={handleAuth} className="space-y-4">
                  {/* Two-column grid for Name and Phone */}
                  <div className="grid grid-cols-2 gap-3">
                    <div className="relative">
                      <User className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-600" size={16} />
                      <input
                        type="text"
                        name="name"
                        placeholder="Full Name"
                        required
                        value={formData.name}
                        onChange={handleChange}
                        className="w-full pl-9 pr-3 py-2.5 bg-white border border-amber-200 rounded-lg focus:border-amber-500 focus:outline-none transition-colors text-sm font-medium text-gray-800 placeholder-gray-400"
                      />
                    </div>
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-600" size={16} />
                      <input
                        type="tel"
                        name="phone"
                        placeholder="Phone No"
                        required
                        value={formData.phone}
                        onChange={handleChange}
                        className="w-full pl-9 pr-3 py-2.5 bg-white border border-amber-200 rounded-lg focus:border-amber-500 focus:outline-none transition-colors text-sm font-medium text-gray-800 placeholder-gray-400"
                      />
                    </div>
                  </div>

                  {/* Email */}
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-600" size={16} />
                    <input
                      type="email"
                      name="email"
                      placeholder="Email ID"
                      required
                      value={formData.email}
                      onChange={handleChange}
                      className="w-full pl-9 pr-3 py-2.5 bg-white border border-amber-200 rounded-lg focus:border-amber-500 focus:outline-none transition-colors text-sm font-medium text-gray-800 placeholder-gray-400"
                    />
                  </div>

                  {/* Password */}
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-600" size={16} />
                    <input
                      type="password"
                      name="password"
                      placeholder="Password"
                      required
                      value={formData.password}
                      onChange={handleChange}
                      className="w-full pl-9 pr-3 py-2.5 bg-white border border-amber-200 rounded-lg focus:border-amber-500 focus:outline-none transition-colors text-sm font-medium text-gray-800 placeholder-gray-400"
                    />
                  </div>

                  {/* Confirm Password */}
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-600" size={16} />
                    <input
                      type="password"
                      name="confirmPassword"
                      placeholder="Confirm Password"
                      required
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      className="w-full pl-9 pr-3 py-2.5 bg-white border border-amber-200 rounded-lg focus:border-amber-500 focus:outline-none transition-colors text-sm font-medium text-gray-800 placeholder-gray-400"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-white font-bold uppercase tracking-widest hover:from-amber-400 hover:to-amber-500 transition-all flex items-center justify-center shadow-lg shadow-amber-500/30"
                  >
                    {loading ? (
                      <Loader2 className="animate-spin" size={20} />
                    ) : (
                      <>
                        Join Now
                        <ArrowRight className="ml-2" size={18} />
                      </>
                    )}
                  </button>
                </form>

                <div className="text-center mt-4">
                  <button
                    onClick={closeBook}
                    className="text-xs uppercase tracking-widest font-bold text-gray-500 hover:text-amber-600 transition-colors inline-flex items-center"
                  >
                    <ArrowLeft size={14} className="mr-1" />
                    Back to Login
                  </button>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginRegisterPage;
'''

file_path = r'c:\Users\sayan\OneDrive\Desktop\GoAndStudy\goandstudyfrontend\src\pages\auth\LoginRegisterPage.jsx'
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('File written successfully')
