import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const Chatbot = ({ memberId }) => {
const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const conversationIdRef = useRef(localStorage.getItem('chatConversationId') || null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { type: 'user', text: input, time: new Date().toISOString() };
    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);
    const message = input;
    setInput('');

    try {
      const response = await fetch(`${import.meta.env.VITE_RAG_API || 'http://localhost:8003'}/api/v1/chat/ask`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, memberId })
      });
      const data = await response.json();
      const botMessage = { 
        type: 'bot', 
        text: data.answer, 
        sources: data.sources || [], 
        confidence: data.confidence,
        time: new Date().toISOString()
      };
      setMessages(prev => [...prev, botMessage]);
      conversationIdRef.current = data.conversation_id;
      localStorage.setItem('chatConversationId', data.conversation_id);
    } catch (error) {
      const errorMessage = { type: 'bot', text: 'Sorry, I encountered an error. Please try again.', time: new Date().toISOString() };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="fixed bottom-4 right-4 w-96 h-96 bg-white shadow-2xl rounded-2xl border border-gray-200 flex flex-col z-50">
      <div className="p-4 border-b bg-gradient-to-r from-blue-500 to-purple-600 rounded-t-2xl text-white">
        <div className="flex items-center">
          <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center mr-3">
            🤖
          </div>
          <div>
            <div className="font-semibold">Library Assistant</div>
            <div className="text-xs opacity-90">Ask me anything about books, loans, fines!</div>
          </div>
        </div>
      </div>

      <div className="flex-1 p-4 overflow-y-auto space-y-3 max-h-64">
        {messages.length === 0 && (
          <div className="text-center text-gray-500 py-8">
            <div className="w-16 h-16 bg-gray-100 rounded-full mx-auto mb-2 flex items-center justify-center">
              💬
            </div>
            <div>Start asking questions!</div>
            <div className="text-sm">Try "What are the library hours?"</div>
          </div>
        )}
        {messages.map((msg, idx) => (
          <div key={idx} className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] p-3 rounded-2xl ${msg.type === 'user' ? 'bg-blue-500 text-white' : 'bg-gray-100'}`}>
              <div>{msg.text}</div>
              {msg.sources && msg.sources.length > 0 && (
                <div className="text-xs mt-1 opacity-75">
                  Sources: {msg.sources.join(', ')}
                </div>
              )}
              {msg.confidence !== undefined && (
                <div className="text-xs mt-1 opacity-75">
                  Confidence: {(msg.confidence * 100).toFixed(0)}%
                </div>
              )}
              <div className="text-xs mt-1 opacity-50">
                {new Date(msg.time).toLocaleTimeString()}
              </div>
            </div>
          </div>
        ))}
        {isLoading && (
          <div className="flex justify-start">
            <div className="bg-gray-100 p-3 rounded-2xl">
              <div className="flex space-x-1">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
              </div>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="p-3 border-t bg-gray-50">
        <div className="flex space-x-2">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Ask about books, loans, fines, hours..."
            className="flex-1 p-3 border border-gray-300 rounded-xl resize-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 max-h-24"
            rows="1"
            disabled={isLoading}
          />
          <button
            onClick={sendMessage}
            disabled={!input.trim() || isLoading}
            className="p-3 bg-blue-500 hover:bg-blue-600 text-white rounded-xl transition-all duration-200 flex items-center justify-center w-12 h-12 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl"
          >
            {isLoading ? '…' : '➤'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default Chatbot;

