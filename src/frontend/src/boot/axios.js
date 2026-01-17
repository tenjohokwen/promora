import { defineBoot } from '#q-app/wrappers';
import axios from 'axios';

// Loading state management
let pendingRequests = 0;
const loadingCallbacks = [];

/**
 * Subscribe to loading state changes.
 * @param {Function} callback - Called with (isLoading: boolean) when state changes
 * @returns {Function} Unsubscribe function
 */
function onLoadingChange(callback) {
  loadingCallbacks.push(callback);
  // Return unsubscribe function
  return () => {
    const index = loadingCallbacks.indexOf(callback);
    if (index > -1) {
      loadingCallbacks.splice(index, 1);
    }
  };
}

/**
 * Notify all subscribers of loading state change
 */
function notifyLoadingChange() {
  const isLoading = pendingRequests > 0;
  loadingCallbacks.forEach((callback) => {
    try {
      callback(isLoading);
    } catch (e) {
      console.error('Error in loading callback:', e);
    }
  });
}

// Create axios instance with credentials for cookie-based auth
const api = axios.create({
  baseURL: '', // Empty string - backend uses relative paths with dev proxy
  withCredentials: true,
});

// Request interceptor
api.interceptors.request.use(
  (config) => {
    // Increment pending requests counter
    pendingRequests++;
    notifyLoadingChange();

    // TODO: Call recordActivity() here in Phase 3

    return config;
  },
  (error) => {
    // Decrement on request error
    pendingRequests--;
    notifyLoadingChange();
    return Promise.reject(error);
  }
);

// Response interceptor
api.interceptors.response.use(
  (response) => {
    // Decrement pending requests counter
    pendingRequests--;
    notifyLoadingChange();

    // Unwrap axios response - return just the data
    return response.data;
  },
  (error) => {
    // Decrement pending requests counter
    pendingRequests--;
    notifyLoadingChange();

    // Handle different error scenarios
    if (error.response) {
      const { status, data } = error.response;
      const errorKey = data?.errorMsg?.errorKey || '';

      // Handle 401 - Unauthorized / Session expired
      if (status === 401) {
        if (errorKey === 'security.sessionExpired' || errorKey === 'security.unauthorized') {
          // Redirect to login with current path for redirect after login
          const currentPath = window.location.pathname + window.location.search;
          const redirectUrl = `/login?redirect=${encodeURIComponent(currentPath)}&expired=true`;
          window.location.href = redirectUrl;
        }
      }

      // Handle 403 - Forbidden
      if (status === 403) {
        const message = data?.errorMsg?.message || 'Operation forbidden';
        console.warn('[403 Forbidden]', message, data?.helpCode ? `(Help code: ${data.helpCode})` : '');
      }

      // Handle 5xx - Server errors
      if (status >= 500) {
        const helpCode = data?.helpCode;
        const message = data?.errorMsg?.message || 'Server error';
        console.error('[Server Error]', message, helpCode ? `(Help code: ${helpCode})` : '');
      }
    } else if (error.request) {
      // Network error - request made but no response received
      console.error('[Network Error] Unable to reach server. Please check your connection.');
    } else {
      // Error setting up request
      console.error('[Request Error]', error.message);
    }

    // Always reject with original error for component handling
    return Promise.reject(error);
  }
);

// Quasar boot wrapper - minimal setup since we use imports
export default defineBoot(() => {
  // Boot file is loaded but we don't set global properties
  // Components import { api } from 'src/boot/axios' directly
});

export { api, onLoadingChange };
