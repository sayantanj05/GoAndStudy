import { useEffect, useState, useRef } from 'react';
import { motion, useInView } from 'framer-motion';
import { ArrowRight, BookOpen, Sparkles } from 'lucide-react';
import SunlightBackground from '../../components/SunlightBackground';
import Book3D from '../../components/Book3D';

// Animated counter hook
function useCountUp(end, duration = 2000, start = 0) {
  const [count, setCount] = useState(start);
  const ref = useRef(null);
  const isInView = useInView(ref, { once: true });
  
  useEffect(() => {
    if (!isInView) return;
    
    let startTime = null;
    let animationFrame;
    
    const animate = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);
      
      // Easing function (ease-out)
      const easeOut = 1 - Math.pow(1 - progress, 3);
      setCount(Math.floor(easeOut * (end - start) + start));
      
      if (progress < 1) {
        animationFrame = requestAnimationFrame(animate);
      }
    };
    
    animationFrame = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(animationFrame);
  }, [isInView, end, duration, start]);
  
  return [count, ref];
}

// Stat Card Component with count-up animation
function StatCard({ value, suffix = '', label, delay = 0 }) {
  const numericValue = parseFloat(value.replace(/[^0-9.]/g, ''));
  const [count, ref] = useCountUp(numericValue, 2000);
  const isDecimal = value.includes('.');
  
  return (
    <motion.div
      ref={ref}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.8 + delay, duration: 0.6 }}
      className="relative p-5 border border-white/10 bg-gradient-to-br from-white/5 to-transparent backdrop-blur-sm group overflow-hidden"
    >
      {/* Ambient glow effect */}
      <div className="absolute inset-0 bg-gradient-to-br from-amber-500/0 via-amber-500/0 to-amber-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
      
      {/* Animated border on hover */}
      <div className="absolute inset-0 border border-amber-500/0 group-hover:border-amber-500/30 transition-colors duration-300" />
      
      <div className="relative z-10">
        <motion.span 
          className="block text-3xl font-display font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 via-amber-400 to-orange-400"
          animate={{ 
            textShadow: [
              '0 0 20px rgba(251, 191, 36, 0.3)',
              '0 0 30px rgba(251, 191, 36, 0.5)',
              '0 0 20px rgba(251, 191, 36, 0.3)',
            ]
          }}
          transition={{ duration: 2, repeat: Infinity }}
          style={{
            textShadow: '0 0 20px rgba(251, 191, 36, 0.4)',
          }}
        >
          {isDecimal ? (count / 10).toFixed(1) : count}{suffix}
        </motion.span>
        <span className="text-[10px] uppercase tracking-[0.2em] text-amber-200/40 font-medium">
          {label}
        </span>
      </div>
      
      {/* Corner accent */}
      <div className="absolute top-0 right-0 w-8 h-8 border-t border-r border-amber-500/20 opacity-0 group-hover:opacity-100 transition-opacity" />
    </motion.div>
  );
}

// Main Landing Page Component
export default function LandingPage() {
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
          <motion.div 
            className="relative z-10"
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.8, delay: 0.2 }}
          >
            <div className="flex items-center gap-3 mb-2">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center shadow-lg shadow-amber-500/20">
                <BookOpen className="text-white" size={20} />
              </div>
              <h1 className="text-3xl font-display font-bold text-transparent bg-clip-text bg-gradient-to-r from-amber-200 to-orange-400">
                GoAndStudy
              </h1>
            </div>
            <p className="font-mono text-[10px] tracking-[0.3em] uppercase text-amber-200/40 pl-1">
              Intelligent Library Ecosystem
            </p>
          </motion.div>
          
          {/* Main Headline */}
          <div className="relative z-10 space-y-8 max-w-lg">
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.4 }}
              className="space-y-4"
            >
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
            </motion.div>
            
            {/* Animated Stats */}
            <div className="grid grid-cols-2 gap-4 pt-4">
              <StatCard value="40" suffix="k+" label="Digital Assets" delay={0} />
              <StatCard value="0.2" suffix="s" label="Search Latency" delay={0.2} />
            </div>
            
            {/* Feature highlights */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.2 }}
              className="flex items-center gap-4 pt-4"
            >
              <div className="flex items-center gap-2 text-xs text-amber-300/50">
                <Sparkles size={14} className="text-amber-400" />
                <span>AI-Powered</span>
              </div>
              <div className="w-1 h-1 rounded-full bg-amber-500/30" />
              <div className="text-xs text-amber-300/50">
                <span className="text-amber-400 font-semibold">50+</span> Libraries Connected
              </div>
            </motion.div>
          </div>
          
          {/* Footer */}
          <motion.div 
            className="relative z-10 flex items-center gap-4"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1 }}
          >
            <div className="w-12 h-12 bg-gradient-to-br from-amber-600 to-orange-700 flex items-center justify-center rounded-lg shadow-lg shadow-amber-900/30 group cursor-pointer hover:shadow-amber-500/30 transition-shadow">
              <ArrowRight className="text-white group-hover:translate-x-0.5 transition-transform" />
            </div>
            <div className="space-y-0.5">
              <span className="text-[10px] text-amber-200/30 font-mono block">EST. 2024 / BUILD V1.0.4</span>
              <span className="text-[10px] text-amber-200/50 font-mono">Secured with Spring Security</span>
            </div>
          </motion.div>
        </div>
        
        {/* Right Side - 3D Book Auth */}
        <div className="flex flex-col items-center justify-center p-6 lg:p-12 relative">
          {/* Mobile Logo (visible only on mobile) */}
          <motion.div 
            className="lg:hidden absolute top-8 left-8 z-20 flex items-center gap-2"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
          >
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center">
              <BookOpen className="text-white" size={16} />
            </div>
            <span className="text-xl font-display font-bold text-amber-100">GoAndStudy</span>
          </motion.div>
          
          {/* 3D Book Container */}
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 30 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 1, delay: 0.5, type: 'spring', stiffness: 100 }}
            className="w-full max-w-md"
          >
            <Book3D />
          </motion.div>
          
          {/* Floating particles accent */}
          <div className="absolute inset-0 pointer-events-none overflow-hidden">
            {Array.from({ length: 5 }).map((_, i) => (
              <motion.div
                key={i}
                className="absolute w-1 h-1 bg-amber-400/30 rounded-full"
                style={{
                  left: `${20 + i * 15}%`,
                  top: `${30 + (i % 3) * 20}%`,
                }}
                animate={{
                  y: [0, -20, 0],
                  opacity: [0.3, 0.6, 0.3],
                }}
                transition={{
                  duration: 3 + i * 0.5,
                  repeat: Infinity,
                  delay: i * 0.3,
                }}
              />
            ))}
          </div>
        </div>
      </div>
      
      {/* Ambient light overlay */}
      <div 
        className="absolute inset-0 pointer-events-none z-[5]"
        style={{
          background: 'radial-gradient(ellipse at top left, rgba(251, 191, 36, 0.08) 0%, transparent 50%)',
        }}
      />
    </div>
  );
}
