import { ref } from 'vue';
import { sessionApi } from 'src/api/session.api';

// Constants
const SESSION_WARNING_THRESHOLD = 2 * 60 * 1000; // 2 minutes in ms
const SESSION_CHECK_INTERVAL = 30 * 1000; // 30 seconds
const SESSION_TTL = 15 * 60 * 1000; // 15 minutes in ms

// Reactive state
const lastActivityTime = ref(Date.now());
const showWarning = ref(false);
const timeUntilExpiry = ref(SESSION_TTL);
const isRefreshing = ref(false);

// Internal timer ID (not reactive)
let checkIntervalId = null;

/**
 * Record user activity to reset session timer.
 */
export function recordActivity() {
  lastActivityTime.value = Date.now();
}

/**
 * Start session monitoring with 30-second interval checks.
 */
export function startSessionMonitoring() {
  // Clear any existing interval
  if (checkIntervalId) {
    clearInterval(checkIntervalId);
  }

  // Initialize with current activity
  recordActivity();

  // Start interval timer
  checkIntervalId = setInterval(() => {
    // Calculate time until expiry
    const elapsed = Date.now() - lastActivityTime.value;
    timeUntilExpiry.value = SESSION_TTL - elapsed;

    // Set warning flag when under 2 minutes and still positive
    showWarning.value = timeUntilExpiry.value < SESSION_WARNING_THRESHOLD && timeUntilExpiry.value > 0;

    // Handle session expiry
    if (timeUntilExpiry.value <= 0) {
      // Dispatch session expired event
      window.dispatchEvent(new CustomEvent('session:expired'));
      cleanup();
    }
  }, SESSION_CHECK_INTERVAL);
}

/**
 * Stop session monitoring.
 */
export function stopSessionMonitoring() {
  if (checkIntervalId) {
    clearInterval(checkIntervalId);
    checkIntervalId = null;
  }
}

/**
 * Refresh the session by calling the API.
 */
export async function refreshSession() {
  isRefreshing.value = true;
  try {
    await sessionApi.refresh();
    // On success, record activity and clear warning
    recordActivity();
    showWarning.value = false;
  } catch (e) {
    // Log error - let axios interceptor handle 401
    console.error('Session refresh failed:', e);
  } finally {
    isRefreshing.value = false;
  }
}

/**
 * Clean up session state and stop monitoring.
 */
export function cleanup() {
  stopSessionMonitoring();
  lastActivityTime.value = Date.now();
  showWarning.value = false;
  timeUntilExpiry.value = SESSION_TTL;
  isRefreshing.value = false;
}

// Export reactive refs for composable use
export { showWarning, timeUntilExpiry, isRefreshing };
