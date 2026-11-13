<script setup lang="ts">
useHead({
  titleTemplate: (titleChunk) => {
    return titleChunk ? `${titleChunk} - Medika Clinical Platform` : 'Medika Clinical Platform'
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900 antialiased">
    <NuxtPage />
  </div>
</template>
