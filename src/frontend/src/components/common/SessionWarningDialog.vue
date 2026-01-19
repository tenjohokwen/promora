<template>
  <q-dialog v-model="dialogVisible" persistent>
    <q-card style="min-width: 320px; max-width: 400px;">
      <q-card-section>
        <div class="text-h6">{{ $t('session.expiring') }}</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <p>{{ $t('session.expiringDesc', { minutes: minutesRemaining }) }}</p>
        <q-linear-progress
          :value="minutesRemaining / 2"
          color="warning"
          class="q-mt-md"
        />
        <p class="q-mt-md">{{ $t('session.continueQuestion') }}</p>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn
          flat
          :label="$t('auth.logout')"
          :disable="isRefreshing"
          @click="handleLogout"
        />
        <q-btn
          color="primary"
          :label="$t('session.continueSession')"
          :loading="isRefreshing"
          :disable="isRefreshing"
          @click="handleRefresh"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useSession } from 'src/composables/useSession';

const {
  showWarning,
  minutesRemaining,
  isRefreshing,
  handleRefresh,
  handleLogout,
} = useSession();

// Create local writable ref for q-dialog v-model (showWarning is readonly)
const dialogVisible = ref(showWarning.value);

// Keep dialogVisible in sync with showWarning
watch(showWarning, (newVal) => {
  dialogVisible.value = newVal;
});
</script>
