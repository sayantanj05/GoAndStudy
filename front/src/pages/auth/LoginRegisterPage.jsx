import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { toast } from 'react-hot-toast';
import { Mail, Lock, User, ShieldCheck, UserCheck, Users, ArrowRight, Loader2, Phone, ArrowLeft, Eye, EyeOff } from 'lucide-react';
import { motion } from 'framer-motion';
import SunlightBackground from '../../components/SunlightBackground';
import Book3D from '../../components/Book3D';

const demoAccounts = [
  { email: 'admin@gmail.com', password: 'admin123', role: 'admin', label: 'Admin', color: 'amber', icon: ShieldCheck },
  { email: 'staff0001@gmail.com', password: 'staff001', role: 'staff', label: 'Staff', color: 'teal', icon: UserCheck },
  { email: 'member@gmail.com', password: 'member123', role: 'member', label: 'Member', color: 'orange', icon: Users },
];

const LoginRegisterPage = () => {
  const [isBookOpen, setIsBookOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const navigate = useNavigate();
  const { login, register, user, isAuthenticated } = useAuthStore();

  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    phone: '',
    gender: '',
    dateOfBirth: '',
    confirmPassword: ''
  });

  useEffect(() => {
    if (isAuthenticated && user) {
      const role = user.role;
      if (role === 'ROLE_ADMIN') navigate('/admin/dashboard');
      else if (role === 'ROLE_STAFF') navigate('/staff/dashboard');
      else if (role === 'ROLE_ANALYTICS_ENGINEER') navigate('/analytics-engineer/dashboard');
      else navigate('/member/home');
    }
  }, [isAuthenticated, user, navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const quickFill = (email, password) => {
    setFormData({ ...formData, email, password });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await login(formData.email, formData.password);
      toast.success('Welcome back to GoAndStudy!');

      if (response.role === 'ROLE_ANALYTICS_ENGINEER') {
        navigate('/analytics-engineer/dashboard');
      } else if (response.role === 'ROLE_ADMIN') {
        navigate('/admin/dashboard');
      } else if (response.role === 'ROLE_STAFF') {
        navigate('/staff/dashboard');
      } else {
        navigate('/member/home');
      }
    } catch (error) {
      toast.error(error.response?.data?.message || error.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await register({
        email: formData.email,
        password: formData.password,
        name: formData.name,
        phone: formData.phone,
        gender: formData.gender,
        dateOfBirth: formData.dateOfBirth
      });
      toast.success('Account created successfully!');
      setIsBookOpen(false);
    } catch (error) {
      toast.error(error.response?.data?.message || error.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen relative overflow-hidden bg-[#1a1814]">
      {/* Three.js Background */}
      <SunlightBackground />

      {/* Main Content Grid */}
      <div className="relative z-10 min-h-screen grid lg:grid-cols-2" style={{ position: 'relative', zIndex: 10 }}>
        
        {/* Left Side - Brand & Stats */}
        <div className="hidden lg:flex flex-col justify-between p-12 xl:p-16 relative">
          {/* Decorative grid lines */}
          <div className="absolute inset-0 z-0 pointer-events-none">
            <div className="absolute top-[20%] left-[10%] w-[1px] h-[30%] bg-gradient-to-b from-transparent via-amber-500/10 to-transparent" />
            <div className="absolute top-[15%] left-[30%] w-[1px] h-[40%] bg-gradient-to-b from-transparent via-amber-500/10 to-transparent" />
            <div className="absolute top-[-10%] left-[-10%] w-[60%] h-[60%] border border-white/5 rotate-12" />
            <div className="absolute bottom-[-20%] right-[-10%] w-[70%] h-[70%] border border-white/5 -rotate-12" />
          </div>
          
          {/* Logo Header */}
          <div className="relative z-10">
            <div className="flex items-center gap-3 mb-2">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center shadow-lg shadow-amber-500/20">
                <User className="text-white" size={20} />
              </div>
              <h1 className="text-3xl font-display font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 to-orange-400">
                GoAndStudy
              </h1>
            </div>
            <p className="font-mono text-[10px] tracking-[0.3em] uppercase text-amber-200/40 pl-1">
              Intelligent Library Ecosystem
            </p>
          </div>
          
          {/* Main Headline */}
          <div className="relative z-10 space-y-8 max-w-lg">
            <div className="space-y-4">
              <h2 className="text-5xl xl:text-6xl font-display font-bold text-white leading-[1.1]">
                Knowledge is{' '}
                <span 
                  className="text-transparent bg-clip-text bg-gradient-to-r from-amber-400 via-orange-400 to-amber-500"
                  style={{
                    textShadow: '0 0 40px rgba(251, 191, 36, 0.5), 0 0 80px rgba(251, 191, 36, 0.3)',
                  }}
                >
                  Dynamic.
                </span>
              </h2>
              <p className="text-amber-100/50 font-light leading-relaxed text-sm max-w-sm">
                Experience the future of library management with real-time 
                analytics, AI-curated recommendations, and seamless 
                collaboration.
              </p>
            </div>
            
            {/* Stats */}
            <div className="grid grid-cols-2 gap-4 pt-4">
              <div className="p-5 border border-white/10 bg-gradient-to-br from-white/5 to-transparent backdrop-blur-sm">
                <span className="block text-3xl font-display font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 via-amber-400 to-orange-400">
                  40k+
                </span>
                <span className="text-[10px] uppercase tracking-[0.2em] text-amber-200/40 font-medium">Digital Assets</span>
              </div>
              <div className="p-5 border border-white/10 bg-gradient-to-br from-white/5 to-transparent backdrop-blur-sm">
                <span className="block text-3xl font-display font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 via-amber-400 to-orange-400">
                  0.2s
                </span>
                <span className="text-[10px] uppercase tracking-[0.2em] text-amber-200/40 font-medium">Search Latency</span>
              </div>
            </div>
          </div>
          
          {/* Footer */}
          <div className="relative z-10 flex items-center gap-4">
            <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-amber-600 to-orange-700 flex items-center justify-center shadow-lg shadow-amber-900/30">
              <ShieldCheck className="text-white" size={20} />
            </div>
            <div>
              <span className="text-[10px] text-amber-200/30 font-mono block">EST. 2024 / BUILD V1.0.4</span>
              <span className="text-[10px] text-amber-200/50 font-mono">Secured with Spring Security</span>
            </div>
          </div>
        </div>
        
        {/* Right Side - 3D Book Auth */}
        <div className="flex flex-col items-center justify-center p-6 lg:p-12 relative">
          {/* Mobile Logo */}
          <div className="lg:hidden absolute top-8 left-8 z-20 flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center">
              <User className="text-white" size={16} />
            </div>
            <span className="text-xl font-display font-bold text-amber-100">GoAndStudy</span>
          </div>

          {/* Book Container */}
          <div className="w-full max-w-md">
            <Book3D />
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginRegisterPage;