import React from 'react';
import * as Icons from 'lucide-react';

const EmptyState = ({ icon, title, description, actionLabel, onAction }) => {
  const Icon = Icons[icon] || Icons.HelpCircle;

  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-white/5 rounded-lg border border-dashed border-gray-700">
      <div className="w-16 h-16 bg-gray-800 rounded-full flex items-center justify-center mb-4">
        <Icon size={32} className="text-gray-400" />
      </div>
      <h3 className="text-xl font-medium mb-2">{title}</h3>
      <p className="text-gray-400 max-w-xs mb-6">{description}</p>
      {actionLabel && (
        <button
          onClick={onAction}
          className="px-6 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded font-medium transition-colors"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};

export default EmptyState;
