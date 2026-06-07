import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Mail, Lock, User, Phone, ArrowRight, ArrowLeft, Loader2, ShieldCheck, UserCheck, Users, Sparkles, Calendar, Eye, EyeOff } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useAuthStore } from '../store/authStore';

const demoAccounts = [
  { email: 'admin@gmail.com', password: 'admin123', role: 'admin', label: 'Admin', color: 'amber' },
  { email: 'staff0001@gmail.com', password: 'staff001', role: 'staff', label: 'Staff', color: 'teal' },
  { email: 'member@gmail.com', password: 'member123', role: 'member', label: 'Member', color: 'orange' },
];

const leatherTexture = `url("data:image/svg+xml,%3Csvg viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='leather'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.65' numOctaves='4' stitchTiles='stitch'/%3E%3CfeColorMatrix type='matrix' values='0.3 0 0 0 0.08 0 0.25 0 0 0.04 0 0 0.2 0 0.02 0 0 0 1 0'/%3E%3CfeGaussianBlur stdDeviation='0.5'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23leather)'/%3E%3C/svg%3E")`;
const paperTexture = `url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='paperNoise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='5' stitchTiles='stitch'/%3E%3CfeColorMatrix type='matrix' values='0.95 0 0 0 0.02 0 0.92 0 0 0.01 0 0 0.88 0 0 -0.01 0 0 0 1 0'/%3E%3CfeGaussianBlur stdDeviation='0.3'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23paperNoise)'/%3E%3C/svg%3E")`;

export default function Book3D() {
  const [isBookOpen, setIsBookOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    phone: '',
    gender: '',
    dateOfBirth: '',
    confirmPassword: '',
  });

  const { login, register, user, isAuthenticated } = useAuthStore();
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const quickFill = (email, password) => {
    setFormData({ ...formData, email, password });
  };

  useEffect(() => {
    if (isAuthenticated && user) {
      const role = user.role;
      if (role === 'ROLE_ADMIN') navigate('/admin/dashboard');
      else if (role === 'ROLE_STAFF') navigate('/staff/dashboard');
      else navigate('/member/home');
    }
  }, [isAuthenticated, user, navigate]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(formData.email, formData.password);
      toast.success('Welcome back to GoAndStudy!');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Login failed');
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
        dateOfBirth: formData.dateOfBirth,
      });
      toast.success('Account created successfully!');
      setIsBookOpen(false);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="relative w-full max-w-md mx-auto">
      <motion.div
        className="relative"
style={{
          height: '550px',
          width: '450px',
          transformStyle: 'preserve-3d',
          perspective: '2500px',
        }}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
      >
        {/* Book Cover/Back - Single flipping container */}
        <motion.div
          className="w-full h-full relative shadow-2xl"
          style={{
            transformStyle: 'preserve-3d',
            transformOrigin: 'left center',
            border: '1px solid rgba(180, 130, 60, 0.4)',
            boxShadow: '0 12px 40px rgba(0,0,0,0.5), inset -2px 0 10px rgba(0,0,0,0.3)',
          }}
          animate={{ rotateY: isBookOpen ? 180 : 0 }}
          transition={{ type: "tween", duration: 0.3, ease: "easeInOut" }}
          onAnimationStart={() => { /* Optional */ }}
          onAnimationComplete={() => { /* Optional */ }}
        >
          {/* Front (SignIn) - backface hidden */}
          <div
            className="rounded-r-2xl overflow-hidden"
            style={{
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              position: 'absolute',
              inset: 0,
              background: `linear-gradient(135deg, rgba(75, 25, 25, 0.98) 0%, rgba(50, 18, 18, 0.99) 50%, rgba(35, 12, 12, 1) 100%), ${leatherTexture}`,
            }}
          >
            {!isBookOpen && (
              <SignInForm 
                formData={formData} 
                handleChange={handleChange} 
                loading={loading} 
                handleLogin={handleLogin} 
                setIsBookOpen={setIsBookOpen} 
                quickFill={quickFill} 
              />
            )}
          </div>
          {/* Back (Register) - backface hidden, rotated 180 initially */}
          <div
            className="rounded-r-2xl overflow-hidden"
            style={{
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden',
              position: 'absolute',
              inset: 0,
              transform: 'rotateY(180deg)',
              background: `linear-gradient(135deg, #f5e6d3 0%, #e8d4b8 50%, #e0d0b0 100%), ${paperTexture}`,
              border: '1px solid rgba(160, 110, 50, 0.15)',
            }}
          >
            {isBookOpen && (
              <RegistrationForm 
                formData={formData} 
                handleChange={handleChange} 
                loading={loading} 
                handleRegister={handleRegister} 
                setIsBookOpen={setIsBookOpen} 
                quickFill={quickFill} 
              />
            )}
          </div>
        </motion.div>
        {/* Spine */}
        <div
          className="absolute left-0 top-0 bottom-0 w-5 rounded-l-xl"
          style={{
            background: `linear-gradient(90deg, rgba(30, 10, 10, 1) 0%, rgba(55, 20, 20, 0.98) 25%, rgba(75, 28, 28, 0.95) 50%, rgba(55, 20, 20, 0.98) 75%, rgba(30, 10, 10, 1) 100%), ${leatherTexture}`,
            boxShadow: 'inset -3px 0 6px rgba(0,0,0,0.4), 3px 0 10px rgba(0,0,0,0.25)',
            zIndex: 100,
          }}
        >
          <div className="absolute inset-y-6 left-0.5 right-0.5 border border-amber-700/25 rounded" />
          <div className="absolute inset-y-10 left-0.5 right-0.5 border border-amber-700/25 rounded" />
          <div className="absolute inset-y-14 left-0.5 right-0.5 border border-amber-700/25 rounded" />
        </div>
      </motion.div>
    </div>
  );
}

const SignInForm = ({ formData, handleChange, loading, handleLogin, setIsBookOpen, quickFill }) => {
  const [showPassword, setShowPassword] = useState(false);
  return (
  <div className="w-full h-full flex flex-col justify-center" style={{ pointerEvents: 'auto', userSelect: 'text' }}>
    <div>
      <div className="text-center mb-8">
        <h3 className="text-3xl font-light tracking-wide text-amber-100 mb-2">Welcome Back</h3>
        <p className="text-sm text-amber-200/50 font-light tracking-wider">Continue your journey</p>
      </div>
      <div className="flex flex-wrap gap-2 justify-center mb-8">
        {demoAccounts.map((account) => (
          <button
            key={account.role}
            onClick={() => quickFill(account.email, account.password)}
            className={`px-3 py-1.5 text-[10px] font-medium tracking-widest border rounded-full transition-all flex items-center gap-1.5 cursor-pointer backdrop-blur-sm
              ${account.role === 'admin' ? 'border-amber-500/40 text-amber-300/80 hover:bg-amber-500/10' : ''}
              ${account.role === 'staff' ? 'border-teal-500/40 text-teal-300/80 hover:bg-teal-500/10' : ''}
              ${account.role === 'member' ? 'border-orange-500/40 text-orange-300/80 hover:bg-orange-500/10' : ''}
            `}
          >
            {account.role === 'admin' && <ShieldCheck size={12} />}
            {account.role === 'staff' && <UserCheck size={12} />}
            {account.role === 'member' && <Users size={12} />}
            {account.label}
          </button>
        ))}
      </div>
      <form onSubmit={handleLogin} autoComplete="off" className="space-y-5">
        <div className="relative group w-4/5 mx-auto">
          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/50 group-focus-within:text-amber-500 transition-colors z-20" size={16} />
          <input
            autoComplete="off"
            type="email"
            name="email"
            placeholder="Email Address"
            required
            value={formData.email}
            onChange={handleChange}
            className="w-full pl-10 pr-3 py-2.5 bg-black/30 border border-amber-900/50 rounded-lg text-amber-100 placeholder-amber-700/50 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/20 transition-all text-sm cursor-text block"
            style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
          />
        </div>
        <div className="relative group w-4/5 mx-auto">
          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/50 group-focus-within:text-amber-500 transition-colors z-20" size={16} />
          <input
            autoComplete="off"
            type={showPassword ? "text" : "password"}
            name="password"
            placeholder="Password"
            required
            value={formData.password}
            onChange={handleChange}
            className="w-full pl-10 pr-10 py-2.5 bg-black/30 border border-amber-900/50 rounded-lg text-amber-100 placeholder-amber-700/50 focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500/20 transition-all text-sm cursor-text block"
            style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
          />
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-amber-700/50 hover:text-amber-500 transition-colors z-20 cursor-pointer"
            style={{ pointerEvents: 'auto' }}
          >
            {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
        <motion.button
          type="submit"
          disabled={loading}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          className="w-4/5 mx-auto py-2.5 bg-gradient-to-r from-amber-600 to-amber-500 text-amber-950 font-bold uppercase tracking-widest rounded-lg hover:from-amber-500 hover:to-amber-400 transition-all flex items-center justify-center gap-2 shadow-lg shadow-amber-500/20 cursor-pointer block"
        >
          {loading ? <Loader2 className="animate-spin" size={20} /> : <>Sign In <ArrowRight size={18} /></>}
        </motion.button>
      </form>
      <div className="mt-8 text-center">
        <button
          onClick={() => setIsBookOpen(true)}
          type="button"
          className="text-xs tracking-widest font-medium text-amber-400/50 hover:text-amber-400/80 transition-colors cursor-pointer"
        >
          Create an account
        </button>
      </div>
    </div>
  </div>
  );
};

const RegistrationForm = ({ formData, handleChange, loading, handleRegister, setIsBookOpen, quickFill }) => {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  return (
  <div className="w-full h-full flex flex-col justify-center p-8" style={{ pointerEvents: 'auto', userSelect: 'text' }}>
    <div>
      <div className="text-center mb-6">
        <div className="inline-flex items-center justify-center w-14 h-14 bg-gradient-to-br from-amber-600 to-amber-500 rounded-full mb-3 shadow-lg shadow-amber-500/20">
          <User className="text-white" size={26} />
        </div>
        <h3 className="text-2xl font-light text-amber-900 mb-1">Join Us</h3>
        <p className="text-xs text-amber-800/60 tracking-wider">Create your account</p>
      </div>
      <form onSubmit={handleRegister} autoComplete="off" className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div className="relative group">
            <User className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <input
              autoComplete="off"
              type="text"
              name="name"
              placeholder="Name"
              required
              value={formData.name}
              onChange={handleChange}
              className="w-full pl-9 pr-3 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            />
          </div>
          <div className="relative group">
            <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <input
              autoComplete="off"
              type="tel"
              name="phone"
              placeholder="Phone"
              required
              value={formData.phone}
              onChange={handleChange}
              className="w-full pl-9 pr-3 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            />
          </div>
        </div>
        <div className="relative group">
          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
          <input
            autoComplete="off"
            type="email"
            name="email"
            placeholder="Email ID"
            required
            value={formData.email}
            onChange={handleChange}
            className="w-full pl-9 pr-3 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
            style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="relative group">
            <Users className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <select
              name="gender"
              value={formData.gender}
              onChange={handleChange}
              className="w-full pl-9 pr-3 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm appearance-none"
              required
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            >
              <option value="">Gender</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div className="relative group">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <input
              autoComplete="off"
              type="date"
              name="dateOfBirth"
              required
              value={formData.dateOfBirth}
              onChange={handleChange}
              max={new Date().toISOString().split('T')[0]}
              className="w-full pl-9 pr-3 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="relative group">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <input
              autoComplete="off"
              type={showPassword ? "text" : "password"}
              name="password"
              placeholder="Password"
              required
              value={formData.password}
              onChange={handleChange}
              className="w-full pl-9 pr-10 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-amber-700/40 hover:text-amber-600/60 transition-colors z-20 cursor-pointer"
              style={{ pointerEvents: 'auto' }}
            >
              {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
          <div className="relative group">
            <Sparkles className="absolute left-3 top-1/2 -translate-y-1/2 text-amber-700/40 group-focus-within:text-amber-600/60 transition-colors z-20" size={16} />
            <input
              autoComplete="off"
              type={showConfirmPassword ? "text" : "password"}
              name="confirmPassword"
              placeholder="Confirm"
              required
              value={formData.confirmPassword}
              onChange={handleChange}
              className="w-full pl-9 pr-10 py-3 bg-amber-50/50 border border-amber-700/15 rounded-xl text-amber-900 placeholder-amber-800/40 focus:border-amber-500/50 focus:outline-none focus:ring-1 focus:ring-amber-500/10 transition-all text-sm"
              style={{ pointerEvents: 'auto', position: 'relative', zIndex: 10 }}
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-amber-700/40 hover:text-amber-600/60 transition-colors z-20 cursor-pointer"
              style={{ pointerEvents: 'auto' }}
            >
              {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            {formData.confirmPassword && formData.password !== formData.confirmPassword && (
              <span className="absolute right-10 top-1/2 -translate-y-1/2 text-[10px] text-red-500 z-20">!</span>
            )}
          </div>
        </div>
        <motion.button
          type="submit"
          disabled={loading || (formData.confirmPassword && formData.password !== formData.confirmPassword)}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          className="w-full py-3.5 bg-gradient-to-r from-amber-600 to-amber-500 text-white font-semibold tracking-widest rounded-xl hover:from-amber-500 hover:to-amber-400 transition-all flex items-center justify-center gap-2 shadow-lg shadow-amber-500/25 disabled:opacity-60 mt-4 cursor-pointer text-sm"
        >
          {loading ? <Loader2 className="animate-spin" size={18} /> : <>Join Now <ArrowRight size={16} /></>}
        </motion.button>
      </form>
      <div className="mt-5 text-center">
        <button
          onClick={() => setIsBookOpen(false)}
          type="button"
          className="text-xs tracking-widest font-medium text-amber-800/50 hover:text-amber-700/70 transition-colors inline-flex items-center gap-1.5 cursor-pointer"
        >
          <ArrowLeft size={14} /> Back to Login
        </button>
      </div>
    </div>
  </div>
  );
};
