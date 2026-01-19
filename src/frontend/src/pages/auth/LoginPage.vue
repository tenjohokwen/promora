<template>
  <q-page class="flex flex-center q-pa-md">
    <div class="col-12 col-sm-8 col-md-6 col-lg-4" style="max-width: 400px">
      <q-card>
        <q-card-section>
          <div class="text-h5 text-center">{{ t('auth.login') }}</div>
        </q-card-section>

        <q-card-section v-if="route.query.expired === 'true'">
          <q-banner class="bg-warning text-white" rounded>
            {{ t('session.expired') }}
          </q-banner>
        </q-card-section>

        <q-card-section>
          <q-form @submit.prevent="handleLogin" class="q-gutter-md">
            <q-input
              v-model="form.email"
              type="email"
              :label="t('auth.email')"
              lazy-rules
              :rules="[required, validEmail]"
              :error="hasFieldError('id')"
              :error-message="getFieldError('id')"
            />

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

            <q-btn
              type="submit"
              color="primary"
              class="full-width q-mt-md"
              :loading="isSubmitting"
              :disable="isSubmitting"
              :label="t('auth.login')"
            />
          </q-form>
        </q-card-section>

        <q-card-section class="text-center q-pt-none">
          <router-link to="/forgot-password" class="text-primary">
            {{ t('auth.forgotPassword') }}
          </router-link>
        </q-card-section>

        <q-separator />

        <q-card-section class="text-center">
          <span>{{ t('auth.noAccount') }} </span>
          <router-link to="/register" class="text-primary">
            {{ t('auth.createAccount') }}
          </router-link>
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { authApi } from 'src/api/auth.api';
import { useErrorHandler } from 'src/composables/useErrorHandler';

const router = useRouter();
const route = useRoute();
const { t } = useI18n();

const {
  setError,
  clearError,
  hasError,
  errorMessage,
  isValidationError,
  helpCode,
  hasFieldError,
  getFieldError
} = useErrorHandler();

// Form state
const form = ref({
  email: '',
  password: ''
});
const isPwd = ref(true);
const isSubmitting = ref(false);

// Validation rules
const required = val => !!val || t('validation.required');
const validEmail = val => /.+@.+\..+/.test(val) || t('validation.email');

// Handle login submission
async function handleLogin() {
  clearError();
  isSubmitting.value = true;

  try {
    const response = await authApi.login(form.value.email, form.value.password, 1);

    // Check if 2FA is required
    if (response.message === 'check.otp') {
      router.push({
        path: '/otp',
        query: {
          id: response.data.loginInfoId,
          redirect: route.query.redirect
        }
      });
    } else {
      // Login successful, redirect to dashboard or specified redirect
      router.push(route.query.redirect || '/dashboard');
    }
  } catch (err) {
    setError(err);
  } finally {
    isSubmitting.value = false;
  }
}
</script>
