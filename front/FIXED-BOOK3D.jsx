import { useState, useRef, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Mail, Lock, User, Phone, ArrowRight, ArrowLeft, Loader2, ShieldCheck, UserCheck, Users, Sparkles, Calendar, Users2 } from 'lucide-react';
import { toast } from 'react-hot-toast';
import { useAuthStore } from '../store/authStore';

const demoAccounts = [
  { email: 'admin@gmail.com', password: 'admin123', role: 'admin', label: 'Admin', icon: ShieldCheck },
  { email: 'staff0001@gmail.com', password: 'staff001', role: 'staff', label: 'Staff', icon: UserCheck },
  { email: 'member@gmail.com', password: 'member123', role: 'member', label: 'Member', icon: Users },
];

const TOTAL_PAGES = 15;
const VISIBLE_PAGES = 5;

export default function Book3D() {
  const [isBookOpen, setIsBookOpen] = useState(false);
  const [isIntroComplete, setIsIntroComplete] = useState(false);
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
  const bookRef = useRef(null);
  const rafRef = useRef(null);
  const progressRef = useRef(0);
  const [renderProgress, setRenderProgress] = useState(0);

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

  const openBook = () => setIsBookOpen(true);
  const closeBook = () => setIsBookOpen(false);

  useEffect(() => {
    let velocity = 0;
    let frameCount = 0;
    const target = isBookOpen ? 1 : 0;
    const springStiffness = isBookOpen ? 0.09 : 0.22;
    const springDamping = isBookOpen ? 0.8 : 0.7;

    const animateLoop = () => {
      const delta = target - progressRef.current;
      velocity += delta * springStiffness;
      velocity *= springDamping;
      progressRef.current += velocity;
      
      if (Math.abs(delta) < 0.001 && Math.abs(velocity) < 0.001) {
        progressRef.current = target;
      }

      frameCount++;
      if (frameCount % 2 === 0) {
        setRenderProgress(progressRef.current);
      }

      rafRef.current = requestAnimationFrame(animateLoop);
    };

    rafRef.current = requestAnimationFrame(animateLoop);
    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [isBookOpen]);

  useEffect(() => {
    const timer = setTimeout(() => setIsIntroComplete(true), 1500);
    return () => clearTimeout(timer);
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await login(formData.email, formData.password);
      toast.success('Welcome back!');
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
      toast.success('Account created!');
      setIsBookOpen(false);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const getPageStyle = (index, progress) => {
    const isCover = index === 0;
    const isFinal = index === VISIBLE_PAGES - 1;
    const rotation = isCover ? progress * -180 : (index === 4 ? 0 : progress * -(160 + index * 2));
    const translateZ = 4 - index;
    const contentOpacity = isCover ? 1 : (isFinal ? (progress > 0.8 ? 1 : 0) : Math.max(0, progress - 0.6));

    return {
      position: 'absolute',
      top: 0,
      left: 0,
      transformOrigin: 'left center',
      transformStyle: 'preserve-3d',
      backfaceVisibility: 'hidden',
      WebkitBackfaceVisibility: 'hidden',
      zIndex: VISIBLE_PAGES - index,
      opacity: contentOpacity,
      transform: `translateZ(${translateZ}px) rotateY(${rotation}deg)`,
      pointerEvents: isCover || (isFinal && progress > 0.8) ? 'auto' : 'none',
    };
  };

  const inputStyle = {
    pointerEvents: 'auto !important',
    zIndex: 100,
    position: 'relative',
  };

  const pages = [
    { type: 'cover', content: <SignInForm />, bg: 'linear-gradient(135deg, #2d1b1b 0%, #1a0f0f 100%)' },
    { type: 'paper', content: <div className="p-8 text-center text-sm opacity-20">Page 2</div>, bg: '#f5f0e6' },
    { type: 'paper', content: <div className="p-8 text-center text-sm opacity-20">Page 3</div>, bg: '#f5f0e6' },
    { type: 'paper', content: <div className="p-8 text-center text-sm opacity-20">Page 4</div>, bg: '#f5f0e6' },
    { type: 'final', content: <RegistrationForm />, bg: 'linear-gradient(135deg, #fff8e1 0%, #f5e6d3 100%)' },
  ];

  return (
    <div className="relative w-full max-w-md mx-auto" style={{ perspective: '2500px', transformStyle: 'preserve-3d' }}>
      <div ref={bookRef} className="relative" style={{ transformStyle: 'preserve-3d', height: '520px' }}>
        {pages.map((page, index) => (
          <div
            key={index}
            className="w-full h-full rounded-r-2xl overflow-hidden absolute"
            style={{
              ...getPageStyle(index, renderProgress),
              background: page.bg,
              border: '1px solid rgba(0,0,0,0.1)',
              boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
            }}
          >
            <div className="w-full h-full relative z-50" style={{ pointerEvents: 'auto' }}>
              {page.content}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const SignInForm = ({ formData, handleChange, loading, quickFill, openBook, handleLogin }) => (
  <div className="w-full h-full flex flex-col justify-center p-8 pointer-events-auto z-50">
    {/* ... rest unchanged ... */}
  </div>
);

const RegistrationForm = ({ formData, handleChange, loading, closeBook, handleRegister }) => (
  <div className="w-full h-full flex flex-col justify-center p-8 pointer-events-auto z-50">
    {/* ... rest unchanged ... */}
  </div>
);

