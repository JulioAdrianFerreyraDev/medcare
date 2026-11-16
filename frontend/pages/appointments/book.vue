<script setup lang="ts">
import ProviderServicesCatalogView from '~/components/catalog/ProviderServicesCatalogView.vue'

useHead({
  title: 'Agendar Cita con Navegación y Sidebar Estandarizado (AHC-4)'
})
</script>

<template>
  <ProviderServicesCatalogView initial-mode="APPOINTMENT_BOOKING" initial-booking-shell="SIDEBAR" />
</template>
