import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import LoadingSpinner from './LoadingSpinner';

// Helper to read auth from localStorage synchronously
const readAuthFromStorage = () => {
  try {
    const stored = localStorage.getItem('goandstudyauth');
    if (stored) {
      const parsed = JSON.parse(stored);
      return {
        token: parsed?.state?.token || null,
        role: parsed?.state?.role || null,
        userId: parsed?.state?.user?.userId || null
      };
    }
  } catch (e) {
    console.error('Error reading from localStorage:', e);
  }
  return { token: null, role: null, userId: null };
};

/**
 * Higher-order component to protect routes based on authentication and roles.
 */
export const ProtectedRoute = ({ children, allowedRoles }) => {
  const location = useLocation();
  const { token: storeToken, role: storeRole } = useAuthStore();
  
  // Read from localStorage synchronously (during render, not useEffect)
  const localAuth = readAuthFromStorage();
  
  // Use token/role from store OR localStorage
  const effectiveToken = storeToken || localAuth.token;
  const effectiveRole = storeRole || localAuth.role;

  console.log('[ProtectedRoute] Path:', location.pathname, 'Token:', effectiveToken ? 'exists' : 'null', 'Role:', effectiveRole);

  // Check authentication
  if (!effectiveToken) {
    console.log('[ProtectedRoute] No token, redirecting to login');
    Object.keys(sessionStorage).forEach(key => {
      if (key.startsWith('access_granted_')) {
        sessionStorage.removeItem(key);
      }
    });
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  // Check role authorization
  if (allowedRoles && !allowedRoles.includes(effectiveRole)) {
    console.log('[ProtectedRoute] Role mismatch, redirecting to dashboard', 'EffectiveRole:', effectiveRole, 'AllowedRoles:', allowedRoles);
    
    // Clear session storage to prevent caching issues
    Object.keys(sessionStorage).forEach(key => {
      if (key.startsWith('access_granted_')) {
        sessionStorage.removeItem(key);
      }
    });
    
    const redirectMap = {
      ROLE_ADMIN: '/admin/dashboard',
      ROLE_STAFF: '/staff/dashboard',
      ROLE_ANALYTICS_ENGINEER: '/analytics-engineer/dashboard',
      ROLE_MEMBER: '/member/home',
    };
    
    const redirectPath = redirectMap[effectiveRole] || '/';
    console.log('[ProtectedRoute] Redirecting to:', redirectPath);
    return <Navigate to={redirectPath} replace />;
  }

  return children;
};
