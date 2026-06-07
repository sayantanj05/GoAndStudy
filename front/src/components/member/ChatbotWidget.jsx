import React from 'react';
import { Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

const ChatbotWidget = () => (
  <Link 
    to="/member/chatbot" 
    className="fixed bottom-8 right-8 z-[60] w-14 h-14 bg-indigo-600 text-white rounded-sm shadow-2xl flex items-center justify-center hover:bg-indigo-700 hover:scale-110 transition-all group"
  >
    <Sparkles size={24} className="group-hover:rotate-12 transition-transform" />
    <div className="absolute right-full mr-4 bg-gray-900 px-4 py-2 text-[10px] font-bold uppercase tracking-widest text-white whitespace-nowrap opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity rounded-sm">
      Talk to Neural Assistant
    </div>
  </Link>
);

export default ChatbotWidget;
