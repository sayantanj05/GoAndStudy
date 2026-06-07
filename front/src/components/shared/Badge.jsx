import React from 'react';

const Badge = ({ children, variant = 'gray', className = '' }) => {
  const variants = {
    red: 'bg-red-100 text-red-700 border-red-200',
    green: 'bg-green-100 text-green-700 border-green-200',
    blue: 'bg-blue-100 text-blue-700 border-blue-200',
    amber: 'bg-amber-100 text-amber-700 border-amber-200',
    teal: 'bg-teal-100 text-teal-700 border-teal-200',
    gray: 'bg-gray-100 text-gray-700 border-gray-200',
    dark: 'bg-gray-800 text-gray-100 border-gray-700',
  };

  return (
    <span className={`px-2 py-0.5 text-[10px] font-bold uppercase tracking-tighter border ${variants[variant]} ${className}`}>
      {children}
    </span>
  );
};

export default Badge;
