import axios from 'axios';

/** RAG Chatbot API client. */
const RAG_API_BASE = import.meta.env.VITE_RAG_API || 'http://localhost:8003';

/** Ask question to RAG chatbot. */
export const askQuestion = async (message, memberId = null, conversationId = null) => {
const response = await axios.post(`${RAG_API_BASE}/api/v1/chat/ask`, {
  message,
  memberId,
  conversationId
});
return response.data;
};

export const submitFeedback = async (interactionId, isHelpful, comment) => {
  const response = await axios.post(`${RAG_API_BASE}/api/v1/chat/feedback`, {
    interactionId,
    isHelpful,
    comment
  });
  return response.data;
};

export const getChatHistory = async (conversationId) => {
  const response = await axios.get(`${RAG_API_BASE}/api/v1/chat/history/${conversationId}`);
  return response.data;
};

export const getStats = async () => {
  const response = await axios.get(`${RAG_API_BASE}/api/v1/chat/stats`);
  return response.data;
};

export default {
  askQuestion,
  submitFeedback,
  getChatHistory,
  getStats
};

