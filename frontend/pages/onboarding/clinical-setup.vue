<script setup lang="ts">
import BrandOnboardingView from '~/components/tenancy/BrandOnboardingView.vue'

useHead({
  title: 'Configuración Clínica Inicial — Medika OS'
})
</script>

<template>
  <BrandOnboardingView initial-step="STEP_2_CLINICAL" :simulate-slug-collision="false" />
</template>
