import React, { useEffect, useState, useRef } from 'react';
import * as Icons from 'lucide-react';
import { motion, useAnimation } from 'framer-motion';

const StatCard = ({ title, value, subtitle, icon, color, trend, delta, portal, variant }) => {
  const [displayValue, setDisplayValue] = useState(0);
  const Icon = Icons[icon] || Icons.Activity;
  const controls = useAnimation();
  const prevValue = useRef(value);

  useEffect(() => {
    let start = displayValue;
    const end = parseInt(value) || 0;
    if (start === end) return;

    let totalDuration = 1000;
    let startTime = null;

    const animate = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / totalDuration, 1);
      const current = Math.floor(progress * (end - start) + start);
      setDisplayValue(current);
      if (progress < 1) {
        requestAnimationFrame(animate);
      }
    };
    requestAnimationFrame(animate);
  }, [value]);

  useEffect(() => {
    if (prevValue.current !== value) {
      controls.start({
        backgroundColor: ['rgba(255,255,255,0)', 'rgba(255,255,0,0.1)', 'rgba(255,255,0,0)'],
        transition: { duration: 0.5 }
      });
      prevValue.current = value;
    }
  }, [value, controls]);

  const colorClasses = {
    red: 'bg-red-500/10 text-red-500',
    teal: 'bg-teal-500/10 text-teal-500',
    amber: 'bg-amber-500/10 text-amber-500',
    blue: 'bg-blue-500/10 text-blue-500',
    green: 'bg-green-500/10 text-green-500',
  };

  return (
    <motion.div
      animate={controls}
      className="relative p-6 rounded-none bg-white border border-gray-200 overflow-hidden"
    >
      <div className="flex justify-between items-start">
        <div>
          <p className="text-xs font-mono uppercase tracking-wider mb-1 text-gray-500">{title}</p>
          <h3 className="text-3xl font-display font-bold text-gray-900">
            {displayValue.toLocaleString()}
          </h3>
          {subtitle && <p className="text-xs opacity-50 mt-1">{subtitle}</p>}
        </div>
        <div className={`p-3 ${colorClasses[color] || colorClasses.blue}`}>
          <Icon size={24} />
        </div>
      </div>

      {trend && (
        <div className="mt-4 flex items-center space-x-2">
          {trend === 'up' ? (
            <Icons.TrendingUp size={14} className="text-green-500" />
          ) : (
            <Icons.TrendingDown size={14} className="text-red-500" />
          )}
          <span className={`text-xs font-medium ${trend === 'up' ? 'text-green-500' : 'text-red-500'}`}>
            {delta}%
          </span>
          <span className="text-[10px] uppercase text-gray-400">vs last week</span>
        </div>
      )}
    </motion.div>
  );
};

export default StatCard;
