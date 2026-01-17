import { ref, computed, readonly } from 'vue';
import { parseApiError } from 'src/utils/errorHandler';

/**
 * Composable for handling API errors with reactive state.
 * Wraps the errorHandler utility with Vue reactivity.
 *
 * @returns {{
 *   error: import('vue').Ref,
 *   fieldErrors: import('vue').ComputedRef<Object>,
 *   hasError: import('vue').ComputedRef<boolean>,
 *   errorMessage: import('vue').ComputedRef<string|null>,
 *   errorKey: import('vue').ComputedRef<string|null>,
 *   helpCode: import('vue').ComputedRef<string|null>,
 *   isValidationError: import('vue').ComputedRef<boolean>,
 *   setError: (err: Error) => void,
 *   clearError: () => void,
 *   hasFieldError: (fieldName: string) => boolean,
 *   getFieldError: (fieldName: string) => string|null
 * }}
 */
export function useErrorHandler() {
  // Internal state - parsed error object
  const error = ref(null);

  // Computed properties for easy access
  const fieldErrors = computed(() => error.value?.fieldErrors || {});
  const hasError = computed(() => error.value !== null);
  const errorMessage = computed(() => error.value?.message || null);
  const errorKey = computed(() => error.value?.errorKey || null);
  const helpCode = computed(() => error.value?.helpCode || null);
  const isValidationError = computed(() => error.value?.isValidationError || false);

  /**
   * Set error from API error object.
   * @param {Error} err - Axios error object
   */
  function setError(err) {
    error.value = parseApiError(err);
  }

  /**
   * Clear the current error state.
   */
  function clearError() {
    error.value = null;
  }

  /**
   * Check if a specific field has an error.
   * @param {string} fieldName - Name of the field
   * @returns {boolean}
   */
  function hasFieldError(fieldName) {
    return Boolean(fieldErrors.value[fieldName]);
  }

  /**
   * Get error message for a specific field.
   * @param {string} fieldName - Name of the field
   * @returns {string|null}
   */
  function getFieldError(fieldName) {
    return fieldErrors.value[fieldName] || null;
  }

  return {
    error: readonly(error),
    fieldErrors,
    hasError,
    errorMessage,
    errorKey,
    helpCode,
    isValidationError,
    setError,
    clearError,
    hasFieldError,
    getFieldError,
  };
}
