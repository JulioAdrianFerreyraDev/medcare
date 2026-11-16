<script setup lang="ts">
import ProviderServicesCatalogView from '~/components/catalog/ProviderServicesCatalogView.vue'

useHead({
  title: 'Portal de Citas por Duración de Servicio'
})
</script>

<template>
  <ProviderServicesCatalogView initial-mode="APPOINTMENT_BOOKING" initial-booking-shell="PORTAL_HEADER" />
</template>
