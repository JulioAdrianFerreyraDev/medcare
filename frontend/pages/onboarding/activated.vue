<script setup lang="ts">
import BrandOnboardingView from '~/components/tenancy/BrandOnboardingView.vue'

useHead({
  title: 'Paso 3: Confirmación y Activación del Portal'
})
</script>

<template>
  <BrandOnboardingView initial-step="STEP_3_ACTIVATED" :simulate-slug-collision="false" />
</template>
