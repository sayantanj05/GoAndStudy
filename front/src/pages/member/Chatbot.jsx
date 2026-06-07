import React, { useState, useRef, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { memberApi } from '../../api/member.api';
import PageTransition from '../../components/shared/PageTransition';
import { 
  Send, Sparkles, User, Info, 
  Loader2, Trash2, ArrowLeft, RefreshCw
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const Chatbot = () => {
  const [messages, setMessages] = useState([
    { id: 1, role: 'ai', text: 'Greetings. I am the GoAndStudy Neural Assistant. How can I assist your literary exploration today?' }
  ]);
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState(null);
  const chatEndRef = useRef(null);

  const mutation = useMutation({
    mutationFn: (message) => memberApi.sendChatMessage({ message, sessionId }),
    onSuccess: (res) => {
      if (res.data?.sessionId) {
        setSessionId(res.data.sessionId);
      }
      setMessages(prev => [...prev, { 
        id: Date.now(), 
        role: 'ai', 
        text: res.data.response || 'I have analyzed your query and synthesized a response.' 
      }]);
    },
    onError: (error) => {
      setMessages(prev => [...prev, {
        id: Date.now(),
        role: 'ai',
        text: error?.message || 'I could not reach the library assistant service. Please try again in a moment.'
      }]);
    }
  });

  const sendMessage = (message) => {
    const trimmed = message.trim();
    if (!trimmed || mutation.isPending) return;

    const userMsg = { id: Date.now(), role: 'user', text: trimmed };
    setMessages(prev => [...prev, userMsg]);
    setInput('');
    mutation.mutate(trimmed);
  };

  const handleSend = (e) => {
    e.preventDefault();
    sendMessage(input);
  };

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <PageTransition>
      <div className="h-[calc(100vh-140px)] flex flex-col p-8 max-w-5xl mx-auto space-y-6">
        {/* Chat Header */}
        <div className="flex justify-between items-center border-b border-gray-100 pb-6">
          <div className="flex items-center space-x-4">
            <div className="w-12 h-12 bg-indigo-600 rounded-sm flex items-center justify-center text-white shadow-lg shadow-indigo-100">
               <Sparkles size={24} />
            </div>
            <div>
              <h1 className="text-2xl font-display font-bold">Neural Assistant</h1>
              <div className="flex items-center space-x-2 text-[10px] font-bold text-indigo-600 uppercase tracking-widest">
                <div className="w-1.5 h-1.5 bg-indigo-600 rounded-full animate-pulse" />
                <span>Inference Model Online</span>
              </div>
            </div>
          </div>
          <button 
            onClick={() => {
              setSessionId(null);
              setMessages([{ id: 1, role: 'ai', text: 'Greetings. How can I assist today?' }]);
            }}
            className="p-3 text-gray-400 hover:text-red-500 transition-colors"
          >
            <Trash2 size={20} />
          </button>
        </div>

        {/* Scrollable Chat Area */}
        <div className="flex-1 overflow-y-auto space-y-8 pr-4 custom-scrollbar">
          <AnimatePresence initial={false}>
            {messages.map((msg) => (
              <motion.div 
                key={msg.id}
                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`max-w-[80%] flex items-start space-x-4 ${msg.role === 'user' ? 'flex-row-reverse space-x-reverse' : ''}`}>
                  <div className={`w-10 h-10 flex-shrink-0 rounded-sm flex items-center justify-center shadow-md ${
                    msg.role === 'user' ? 'bg-gray-900 text-white' : 'bg-white border border-indigo-100 text-indigo-600'
                  }`}>
                    {msg.role === 'user' ? <User size={18} /> : <Sparkles size={18} />}
                  </div>
                  <div className={`p-6 rounded-sm text-sm leading-relaxed ${
                    msg.role === 'user' 
                    ? 'bg-gray-900 text-white shadow-xl translate-x-1' 
                    : 'bg-white border border-gray-100 text-gray-700 shadow-sm border-l-4 border-l-indigo-600'
                  }`}>
                    {msg.text}
                  </div>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
          
          {mutation.isPending && (
            <motion.div 
              initial={{ opacity: 0 }} 
              animate={{ opacity: 1 }}
              className="flex justify-start space-x-4 items-center"
            >
              <div className="w-10 h-10 bg-indigo-50 text-indigo-400 rounded-sm flex items-center justify-center">
                <RefreshCw size={18} className="animate-spin" />
              </div>
              <p className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest animate-pulse">Consulting Vector DB...</p>
            </motion.div>
          )}
          <div ref={chatEndRef} />
        </div>

        {/* Interaction Input */}
        <form onSubmit={handleSend} className="relative mt-6">
          <input
            type="text"
            placeholder="Query the repository... (e.g. 'What are our late return policies?')"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            className="w-full pl-6 pr-32 py-5 bg-white border-2 border-indigo-100 focus:border-indigo-600 focus:outline-none transition-all rounded-sm shadow-xl shadow-indigo-50 font-medium text-gray-700"
          />
          <button 
            type="submit"
            disabled={mutation.isPending}
            className="absolute right-3 top-3 px-6 py-2.5 bg-indigo-600 text-white font-bold uppercase tracking-widest text-[10px] rounded-sm hover:bg-indigo-700 transition-all flex items-center disabled:opacity-50 shadow-lg shadow-indigo-100"
          >
            {mutation.isPending ? <Loader2 className="animate-spin mr-2" size={12} /> : (
              <>
                Execute Query
                <Send size={12} className="ml-2" />
              </>
            )}
          </button>
        </form>
        
        <div className="flex items-center justify-center space-x-6">
           <button type="button" onClick={() => sendMessage('How do I waive a fine?')} className="text-[10px] text-gray-400 uppercase font-bold tracking-tighter cursor-pointer hover:text-indigo-600 transition-colors">How do I waive a fine?</button>
           <div className="w-1 h-1 bg-gray-200 rounded-full" />
           <button type="button" onClick={() => sendMessage('Find sci-fi books')} className="text-[10px] text-gray-400 uppercase font-bold tracking-tighter cursor-pointer hover:text-indigo-600 transition-colors">Find sci-fi books</button>
           <div className="w-1 h-1 bg-gray-200 rounded-full" />
           <button type="button" onClick={() => sendMessage('What is my account status?')} className="text-[10px] text-gray-400 uppercase font-bold tracking-tighter cursor-pointer hover:text-indigo-600 transition-colors">Account status</button>
        </div>
      </div>
    </PageTransition>
  );
};

export default Chatbot;
