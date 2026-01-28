# Phase 11: Frontend Profile Page - Research

**Researched:** 2026-01-28
**Domain:** Vue.js 3 + Quasar 2 frontend for user profile management
**Confidence:** HIGH

## Summary

The frontend codebase has well-established patterns for building authenticated pages with forms, dialogs, API integration, and i18n support. Key findings:

1. **Existing patterns are comprehensive and consistent** - LoginPage, RegisterPage, ResetPasswordPage, and SessionWarningDialog provide clear templates for forms, validation, error handling, and dialog components. All use the same composables (`useErrorHandler`, `useSession`) and API patterns.

2. **Backend Profile API is complete** - Phase 9 created ProfileResource at `/api/account/*` with all required endpoints (GET /profile, PUT /email, PUT /password, PUT /phone, PUT /address, PUT /info, PUT /2fa). DTOs define exact request/response structures.

3. **i18n structure is straightforward** - Two locale files (en-US, fr-FR) as JS modules with nested key objects. Profile page translations need to be added following existing auth/validation patterns.

4. **One gap identified** - The UserDto returned by GET /profile does not include address data. Either UserDto/UserMapper needs extending, or a separate GET /address endpoint is needed. This is a backend task for Phase 11 coordination.

**Primary recommendation:** Follow existing page/dialog patterns exactly. Create ProfilePage.vue as the main page, with separate dialog components for each update operation. Use the established API service pattern (account.api.js or new profile.api.js).

## Standard Stack

The established libraries/tools for this frontend:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Vue.js | 3.x | Reactive UI framework | Project standard |
| Quasar | 2.x | Component library | Project standard |
| vue-i18n | 10.x+ | Internationalization | Already configured |
| axios | (via boot/axios.js) | HTTP client | Centralized with interceptors |
| vue-router | 4.x | Routing | Already configured |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Quasar q-dialog | - | Modal dialogs | Update forms |
| Quasar q-form | - | Form validation | All forms |
| Quasar q-input/q-select | - | Form inputs | All forms |
| Quasar q-notify | - | Toast notifications | Success/error messages |

### Do NOT Add
| Library | Reason |
|---------|--------|
| Vuex/Pinia stores | Auth state comes from cookie, not store |
| Form libraries (VeeValidate, etc.) | Quasar's built-in validation is sufficient |
| Additional HTTP libraries | axios is already configured |

## Architecture Patterns

### Recommended Project Structure
```
src/frontend/src/
├── api/
│   ├── auth.api.js         # existing
│   ├── account.api.js      # existing - add profile methods here OR
│   └── profile.api.js      # NEW - dedicated profile API service
├── composables/
│   ├── useErrorHandler.js  # existing - reuse
│   └── useSession.js       # existing - reuse
├── components/
│   └── profile/            # NEW - profile-specific components
│       ├── UpdateEmailDialog.vue
│       ├── UpdatePasswordDialog.vue
│       ├── UpdatePhoneDialog.vue
│       ├── UpdateAddressDialog.vue
│       ├── UpdateInfoDialog.vue
│       └── Toggle2faDialog.vue
├── pages/
│   └── ProfilePage.vue     # NEW - main profile page
└── i18n/
    ├── en-US/index.js      # MODIFY - add profile translations
    └── fr-FR/index.js      # MODIFY - add profile translations
```

### Pattern 1: Page Component Structure
**What:** Standard page component with form and error handling
**When to use:** All authenticated pages with forms
**Example (from LoginPage.vue):**
```vue
<template>
  <q-page class="flex flex-center q-pa-md">
    <div class="col-12 col-sm-8 col-md-6 col-lg-4">
      <q-card>
        <q-card-section>
          <div class="text-h5 text-center">{{ t('profile.title') }}</div>
        </q-card-section>

        <q-card-section>
          <!-- Profile display content here -->
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useErrorHandler } from 'src/composables/useErrorHandler';
import { profileApi } from 'src/api/profile.api';

const { t } = useI18n();
const { setError, clearError, hasError, errorMessage } = useErrorHandler();

const profile = ref(null);
const isLoading = ref(false);

onMounted(async () => {
  await loadProfile();
});

async function loadProfile() {
  isLoading.value = true;
  try {
    profile.value = await profileApi.getProfile();
  } catch (err) {
    setError(err);
  } finally {
    isLoading.value = false;
  }
}
</script>
```

### Pattern 2: Dialog Component Structure
**What:** Modal dialog with form for updates
**When to use:** All update operations (email, password, phone, address, info, 2FA)
**Example (based on SessionWarningDialog.vue):**
```vue
<template>
  <q-dialog v-model="dialogVisible" persistent>
    <q-card style="min-width: 400px;">
      <q-card-section>
        <div class="text-h6">{{ $t('profile.updatePassword') }}</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit.prevent="handleSubmit" class="q-gutter-md">
          <q-input
            v-model="form.currentPassword"
            :type="isPwd ? 'password' : 'text'"
            :label="$t('auth.currentPassword')"
            lazy-rules
            :rules="[required, minLen5]"
            :error="hasFieldError('currentPassword')"
            :error-message="getFieldError('currentPassword')"
          >
            <template #append>
              <q-icon
                :name="isPwd ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="isPwd = !isPwd"
              />
            </template>
          </q-input>

          <!-- More fields... -->

          <q-banner
            v-if="hasError && !isValidationError"
            class="bg-negative text-white"
            rounded
          >
            {{ errorMessage }}
          </q-banner>
        </q-form>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat :label="$t('common.cancel')" @click="close" />
        <q-btn
          color="primary"
          :label="$t('common.save')"
          :loading="isSubmitting"
          @click="handleSubmit"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useQuasar } from 'quasar';
import { useI18n } from 'vue-i18n';
import { profileApi } from 'src/api/profile.api';
import { useErrorHandler } from 'src/composables/useErrorHandler';

const props = defineProps({
  modelValue: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'updated']);

const $q = useQuasar();
const { t } = useI18n();
const { setError, clearError, hasError, errorMessage, isValidationError, hasFieldError, getFieldError } = useErrorHandler();

const dialogVisible = ref(props.modelValue);
const form = ref({ currentPassword: '', newPassword: '' });
const isPwd = ref(true);
const isSubmitting = ref(false);

// Validation rules
const required = val => !!val || t('validation.required');
const minLen5 = val => val.length >= 5 || t('validation.minLength', { min: 5 });

watch(() => props.modelValue, (val) => { dialogVisible.value = val; });
watch(dialogVisible, (val) => { emit('update:modelValue', val); });

function close() {
  dialogVisible.value = false;
}

async function handleSubmit() {
  clearError();
  isSubmitting.value = true;
  try {
    await profileApi.updatePassword(form.value.currentPassword, form.value.newPassword);
    $q.notify({ type: 'positive', message: t('success.passwordChanged') });
    emit('updated');
    close();
  } catch (err) {
    setError(err);
  } finally {
    isSubmitting.value = false;
  }
}
</script>
```

### Pattern 3: API Service Structure
**What:** Dedicated API service module
**When to use:** Grouping related API calls
**Example (based on account.api.js):**
```javascript
import { api } from 'src/boot/axios';

export const profileApi = {
  /**
   * Get current user's profile
   * @returns {Promise} Profile data (UserDto)
   */
  getProfile() {
    return api.get('/api/account/profile');
  },

  /**
   * Update email address
   * @param {string} oldEmail - Current email
   * @param {string} newEmail - New email
   * @param {string} password - Current password for verification
   * @returns {Promise} Success response
   */
  updateEmail(oldEmail, newEmail, password) {
    return api.put('/api/account/email', { oldEmail, newEmail, password });
  },

  /**
   * Update password
   * @param {string} currentPassword - Current password
   * @param {string} newPassword - New password
   * @returns {Promise} Success response
   */
  updatePassword(currentPassword, newPassword) {
    return api.put('/api/account/password', { currentPassword, newPassword });
  },

  // ... additional methods
};
```

### Pattern 4: i18n Translation Structure
**What:** Nested key structure for translations
**When to use:** Adding new feature translations
**Example (from en-US/index.js):**
```javascript
export default {
  // ... existing keys ...
  profile: {
    title: 'My Profile',
    email: 'Email Address',
    password: 'Password',
    passwordMasked: 'XXXX',
    phone: 'Phone Number',
    address: 'Address',
    coreInfo: 'Personal Information',
    twoFactorAuth: '2-Factor Authentication',
    enabled: 'Enabled',
    disabled: 'Disabled',
    edit: 'Edit',
    // Dialog titles
    updateEmail: 'Update Email',
    updatePassword: 'Change Password',
    updatePhone: 'Update Phone Number',
    updateAddress: 'Update Address',
    updateInfo: 'Update Personal Information',
    toggle2fa: 'Two-Factor Authentication Settings',
    // Address fields
    addressName: 'Address Label',
    companyName: 'Company Name',
    addressLine1: 'Address Line 1',
    addressLine2: 'Address Line 2',
    addressLine3: 'Address Line 3',
    city: 'City',
    stateProvince: 'State/Province',
    postalCode: 'Postal Code',
    country: 'Country'
  },
  success: {
    // ... existing keys ...
    emailChanged: 'Email update initiated. Please check your new email for verification.',
    passwordChanged: 'Your password has been changed successfully.',
    phoneChanged: 'Your phone number has been updated.',
    addressChanged: 'Your address has been updated.',
    infoChanged: 'Your profile information has been updated.',
    twoFactorEnabled: 'Two-factor authentication has been enabled.',
    twoFactorDisabled: 'Two-factor authentication has been disabled.'
  }
}
```

### Anti-Patterns to Avoid
- **Do NOT use Pinia/Vuex for profile state:** Profile is fetched on mount, no global state needed
- **Do NOT create custom form validation:** Use Quasar's built-in `:rules` prop
- **Do NOT bypass useErrorHandler:** All API errors should go through the composable
- **Do NOT hardcode strings:** All user-facing text must use `t()` or `$t()`
- **Do NOT duplicate validation rules:** Define once in component, reuse across fields

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Form validation | Custom validation logic | Quasar `:rules` prop | Consistent with codebase, handles async validation |
| Error display | Custom error state | `useErrorHandler` composable | Parses backend ErrorDto correctly |
| API calls | fetch() or new axios instance | `api` from boot/axios | Has interceptors for auth, loading, error handling |
| Loading states | Custom global loading | `useLoading` composable | Already tracks pending requests |
| Notifications | Custom toast system | Quasar `$q.notify()` | Consistent styling, built-in |
| Modal dialogs | Custom modal implementation | Quasar `q-dialog` | Consistent, handles overlay/backdrop |
| Password visibility | Custom toggle | Quasar input slot pattern | See existing password fields |

**Key insight:** Every UI pattern needed already exists in the codebase. Copy and adapt, don't reinvent.

## Common Pitfalls

### Pitfall 1: Address Data Not in UserDto
**What goes wrong:** Profile page tries to display address, but GET /profile doesn't return it
**Why it happens:** UserDto and UserMapper don't include addresses field
**How to avoid:**
- Option A (Recommended): Extend UserDto to include addresses, update UserMapper
- Option B: Create separate GET /api/account/address endpoint
- This is a backend coordination task - flag for planning
**Warning signs:** Address section is empty even though user has address in database

### Pitfall 2: Dialog v-model Reactivity
**What goes wrong:** Dialog doesn't open/close properly
**Why it happens:** Direct v-model on readonly computed from composable
**How to avoid:** Create local writable ref, sync with props via watch (see SessionWarningDialog pattern)
```javascript
const dialogVisible = ref(props.modelValue);
watch(() => props.modelValue, (val) => { dialogVisible.value = val; });
watch(dialogVisible, (val) => { emit('update:modelValue', val); });
```
**Warning signs:** Dialog stuck open, or changes to parent don't propagate

### Pitfall 3: Form Reset on Dialog Close
**What goes wrong:** Previous form values persist when reopening dialog
**Why it happens:** Form ref not reset when dialog closes
**How to avoid:** Reset form in close handler or watch dialog visibility
```javascript
function close() {
  dialogVisible.value = false;
  form.value = { currentPassword: '', newPassword: '' };
  clearError();
}
```
**Warning signs:** Opening dialog shows previous (possibly errored) data

### Pitfall 4: Missing Error Translation Keys
**What goes wrong:** Error shows as raw key like "security.badCreds" instead of translated message
**Why it happens:** Backend sends errorKey that doesn't exist in i18n
**How to avoid:** Add all backend error keys to i18n files. Check `useErrorHandler.js` - it uses `te()` to check if key exists before translating.
**Warning signs:** Raw error keys displayed to users

### Pitfall 5: Profile Fetch on Every Navigation
**What goes wrong:** Profile API called every time user navigates to page
**Why it happens:** onMounted runs on each navigation
**How to avoid:** This is acceptable behavior for profile page - data should be fresh. But consider adding a brief loading indicator to avoid flash of empty content.
**Warning signs:** None - this is expected behavior

### Pitfall 6: Phone Number Format Validation
**What goes wrong:** Frontend allows invalid phone, backend rejects
**Why it happens:** Backend uses @CamPhone validator (Cameroon mobile format)
**How to avoid:** Add frontend validation rule matching backend format: 9 digits, starts with 6, matches MTN/Orange/NextTel patterns
```javascript
const validCamPhone = (val) => {
  if (!val) return true; // optional
  const cleaned = val.replace(/\D/g, '');
  if (cleaned.length !== 9) return t('validation.phone.digitCount');
  if (cleaned[0] !== '6') return t('validation.phone.firstDigit');
  // Optionally validate operator prefixes
  return true;
};
```
**Warning signs:** Backend returns phone validation errors

## Code Examples

Verified patterns from existing codebase:

### Validation Rules (from RegisterPage.vue)
```javascript
// Validation rules
const required = (val) => !!val || t('validation.required');
const validEmail = (val) => /.+@.+\..+/.test(val) || t('validation.email');
const minLen5 = (val) => val.length >= 5 || t('validation.minLength', { min: 5 });
const optionalMinLen5 = (val) => !val || val.length >= 5 || t('validation.minLength', { min: 5 });
const maxLen50 = (val) => !val || val.length <= 50 || t('validation.maxLength', { max: 50 });
const maxLen100 = (val) => val.length <= 100 || t('validation.maxLength', { max: 100 });
const passwordMatch = (val) => val === form.value.password || t('validation.passwordMatch');
```

### Form Submission Handler (from ResetPasswordPage.vue)
```javascript
async function handleSubmit() {
  clearError();
  isSubmitting.value = true;

  try {
    await accountApi.resetPassword(resetKey.value, form.value.password);

    $q.notify({
      type: 'positive',
      message: t('success.passwordReset')
    });

    router.push('/login');
  } catch (err) {
    setError(err);
  } finally {
    isSubmitting.value = false;
  }
}
```

### Password Field with Toggle (from LoginPage.vue)
```vue
<q-input
  v-model="form.password"
  :type="isPwd ? 'password' : 'text'"
  :label="t('auth.password')"
  lazy-rules
  :rules="[required]"
  :error="hasFieldError('password')"
  :error-message="getFieldError('password')"
>
  <template #append>
    <q-icon
      :name="isPwd ? 'visibility_off' : 'visibility'"
      class="cursor-pointer"
      @click="isPwd = !isPwd"
    />
  </template>
</q-input>
```

### Error Banner (from LoginPage.vue)
```vue
<q-banner
  v-if="hasError && !isValidationError"
  class="bg-negative text-white q-mt-md"
  rounded
>
  {{ errorMessage }}
  <template v-if="helpCode">
    <br />
    <small>{{ t('error.helpCode') }}: {{ helpCode }}</small>
  </template>
</q-banner>
```

### Select with Options (from RegisterPage.vue)
```vue
<q-select
  v-model="form.gender"
  :options="genderOptions"
  :label="t('auth.gender')"
  outlined
  emit-value
  map-options
  clearable
  :error="hasFieldError('gender')"
  :error-message="getFieldError('gender')"
/>
```
```javascript
const genderOptions = computed(() => [
  { label: t('auth.male'), value: 'MALE' },
  { label: t('auth.female'), value: 'FEMALE' },
  { label: t('auth.other'), value: 'OTHER' },
]);
```

## Backend API Reference

Endpoints from Phase 9 ProfileResource that frontend must call:

| Endpoint | Method | Request Body | Response | Auth Required |
|----------|--------|--------------|----------|---------------|
| `/api/account/profile` | GET | - | UserDto | Yes |
| `/api/account/email` | PUT | `{oldEmail, newEmail, password}` | Success | Yes |
| `/api/account/password` | PUT | `{currentPassword, newPassword}` | Success | Yes |
| `/api/account/phone` | PUT | `{phone}` | Success | Yes |
| `/api/account/address` | PUT | `{name, companyName?, addressLine1, addressLine2?, addressLine3?, city, stateProvince, postalCode, country}` | Success | Yes |
| `/api/account/info` | PUT | `{firstName?, lastName?, langKey?, nationalId?, gender?, title?}` | Success | Yes |
| `/api/account/2fa` | PUT | `{enabled, password}` | Success | Yes |

### UserDto Fields Returned by GET /profile
```javascript
{
  login: string,
  loginIdType: string,
  // password: null (not returned for security)
  title: string?,
  firstName: string?,
  lastName: string?,
  email: string,
  phone: string?,
  activated: boolean,
  langKey: string,
  authorities: string[],
  gender: 'MALE' | 'FEMALE' | 'OTHER',
  nationalId: string?,
  dob: string?, // 'yyyy-MM-dd'
  otpEnabled: boolean
  // NOTE: addresses NOT included - needs backend extension
}
```

### Success Response Structure
```javascript
{
  helpCode: string | null,
  msgKey: string,        // e.g., 'password.changed'
  msg: string,           // Human-readable message
  payload: object        // Usually empty {}
}
```

## State of the Art

| Pattern | Current Implementation | Notes |
|---------|----------------------|-------|
| Composition API | `<script setup>` | Vue 3 standard |
| Form Validation | Quasar `:rules` prop | Inline validation functions |
| Error Handling | `useErrorHandler` composable | Parses backend ErrorDto |
| API Layer | Dedicated api/*.js modules | Axios with interceptors |
| i18n | vue-i18n with legacy: false | Composition API mode |
| Dialogs | Quasar q-dialog | With v-model reactivity |
| Notifications | Quasar $q.notify | Toast-style notifications |
| Routing | vue-router with meta guards | requiresAuth/requiresGuest |

## Open Questions

1. **Address Data in GET /profile**
   - What we know: UserDto doesn't include addresses field
   - What's unclear: Should we extend UserDto or create separate endpoint?
   - Recommendation: Extend UserDto and UserMapper (simpler, single API call)
   - **Action needed:** Backend task before frontend can display address

2. **Address Name Field**
   - What we know: Backend AddressDto requires `name` field (e.g., "HOME", "WORK")
   - What's unclear: Should user choose from preset options or free text?
   - Recommendation: Use preset options (HOME, WORK, OTHER) for v1.1 simplicity
   - Impact: Frontend needs to handle this in address dialog

3. **Profile Refresh After Update**
   - What we know: After successful update, profile data may be stale
   - What's unclear: Refetch entire profile or optimistically update state?
   - Recommendation: Refetch profile after any successful update (simple, correct)

## Sources

### Primary (HIGH confidence)
- Codebase files (direct inspection):
  - `/src/frontend/src/pages/auth/LoginPage.vue` - form patterns
  - `/src/frontend/src/pages/auth/RegisterPage.vue` - complex form patterns
  - `/src/frontend/src/pages/auth/ResetPasswordPage.vue` - password form patterns
  - `/src/frontend/src/components/common/SessionWarningDialog.vue` - dialog patterns
  - `/src/frontend/src/api/*.js` - API service patterns
  - `/src/frontend/src/composables/*.js` - composable patterns
  - `/src/frontend/src/i18n/*/index.js` - translation patterns
  - `/src/main/java/com/softropic/promora/security/api/ProfileResource.java` - backend endpoints
  - `/src/main/java/com/softropic/promora/security/api/dto/*.java` - request DTOs

### Secondary (MEDIUM confidence)
- Phase 9 Research document - backend API context

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH - Direct codebase inspection, consistent patterns
- Architecture Patterns: HIGH - Multiple working examples in codebase
- Don't Hand-Roll: HIGH - Existing solutions documented and working
- Pitfalls: MEDIUM - Based on common Vue/Quasar patterns and codebase specifics
- Backend API: HIGH - Direct inspection of ProfileResource and DTOs

**Research date:** 2026-01-28
**Valid until:** 2026-02-28 (30 days - stable frontend patterns)
