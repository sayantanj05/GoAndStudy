import React from 'react';
import { motion } from 'framer-motion';

const LoadingSpinner = ({ fullPage }) => {
  const content = (
    <div className="flex flex-col items-center justify-center space-y-4">
      <motion.div 
        animate={{ rotate: 360 }}
        transition={{ repeat: Infinity, duration: 1, ease: "linear" }}
        className="w-12 h-12 border-4 border-indigo-100 border-t-indigo-600 rounded-sm"
      />
      <p className="text-[10px] font-mono font-bold uppercase tracking-[0.2em] text-gray-400">Synchronizing Repository...</p>
    </div>
  );

  if (fullPage) {
    return (
      <div className="fixed inset-0 bg-white/80 backdrop-blur-md z-[100] flex items-center justify-center">
        {content}
      </div>
    );
  }

  return content;
};

export default LoadingSpinner;
