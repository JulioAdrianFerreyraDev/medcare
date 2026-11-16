<script setup lang="ts">
import ProviderServicesCatalogView from '~/components/catalog/ProviderServicesCatalogView.vue'

useHead({
  title: 'Mis Servicios y Tarifas — Dr. Alejandro Vega'
})
</script>

<template>
  <ProviderServicesCatalogView initial-mode="DOCTOR_CATALOG" />
</template>
