<template>
  <GlobalLoadingBar />
  <SessionWarningDialog />
  <router-view />
</template>

<script setup>
import GlobalLoadingBar from 'src/components/common/GlobalLoadingBar.vue';
import SessionWarningDialog from 'src/components/common/SessionWarningDialog.vue';
</script>
