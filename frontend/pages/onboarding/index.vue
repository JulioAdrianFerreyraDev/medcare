<script setup lang="ts">
import BrandOnboardingView from '~/components/tenancy/BrandOnboardingView.vue'

useHead({
  title: 'Paso 1: Registro Inicial de Clínica'
})
</script>

<template>
  <BrandOnboardingView initial-step="STEP_1_FORM" :simulate-slug-collision="true" />
</template>
