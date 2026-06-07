import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

/**
 * Fetch churn prediction for a specific member.
 */
export const getChurnPrediction = async (userId) => {
  try {
    const response = await axios.get(`${API_URL}/predictions/churn/${userId}`);
    return response.data;
  } catch (error) {
    console.error('Churn prediction error:', error);
    return null;
  }
};

/**
 * Fetch churn predictions for all members (admin).
 */
export const getAllChurnPredictions = async () => {
  try {
    const response = await axios.get(`${API_URL}/predictions/churn`);
    return response.data;
  } catch (error) {
    console.error('All churn predictions error:', error);
    return [];
  }
};
