<script setup lang="ts">
import { computed } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'
import { useTenancyStore } from '~/stores/tenancy'

const tenancyStore = useTenancyStore()
const route = useRoute()

const tenantSlug = computed(() => {
  if (typeof route.query.tenant === 'string' && route.query.tenant) {
    return route.query.tenant
  }
  if (import.meta.client && typeof window !== 'undefined') {
    const hostParts = window.location.hostname.split('.')
    if (hostParts.length > 1 && hostParts[0] !== 'localhost') {
      return hostParts[0]
    }
  }
  return tenancyStore.resolvedSlug || 'clinica-irma'
})

useHead({
  title: computed(() => `Portal Clínico: ${tenantSlug.value}`)
})
</script>

<template>
  <MedikaAppShell
    :title="`Portal Clínico: ${tenantSlug}`"
    breadcrumb-section="Onboarding Institucional"
    story-badge="Portal Multi-Marca"
    active-nav="tenant-portal"
  >
    <template #header-actions>
      <span class="px-3 py-1 rounded-full bg-secondary-container text-on-secondary-container font-code-sm text-code-sm font-semibold">
        Contexto de Tenant Activo: {{ tenantSlug }}
      </span>
    </template>

    <div class="space-y-space-lg">
      <!-- Tenant Context Isolation Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg lg:p-space-xl shadow-subtle border border-outline-variant/30 space-y-space-md">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="font-label-sm text-label-sm uppercase tracking-wider text-primary font-semibold">
            Subdominio Institucional Aislado
          </span>
          <span class="px-3 py-1 rounded-full bg-secondary-container/70 text-on-secondary-container font-code-sm text-code-sm font-semibold">
            Contexto de Tenant Activo: {{ tenantSlug }}
          </span>
        </div>

        <h1 class="font-headline-lg text-headline-lg text-on-surface">
          Portal Clínico: {{ tenantSlug }}
        </h1>

        <p class="font-body-md text-body-md text-on-surface-variant max-w-2xl leading-relaxed">
          Bienvenido al portal institucional <span class="font-code-sm text-primary font-semibold">{{ tenantSlug }}.medika.com</span>.
          Todas las consultas de sedes, consultorios, turnos, bloqueos temporales y catálogo de servicios están aisladas bajo el identificador de tu organización.
        </p>

        <!-- Tenant Isolation Telemetry Grid -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-space-md pt-2">
          <div class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/25">
            <span class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Subdominio Resuelto</span>
            <p class="font-code-sm text-title-md text-on-surface font-semibold mt-1">
              {{ tenantSlug }}.medika.com
            </p>
          </div>
          <div class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/25">
            <span class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Filtro de Persistencia</span>
            <p class="font-title-sm text-title-sm text-secondary font-semibold mt-1 flex items-center gap-1.5">
              <span class="w-2 h-2 rounded-full bg-secondary"></span>
              TenantContext ({{ tenantSlug }})
            </p>
          </div>
          <div class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/25">
            <span class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Seguridad &amp; Red</span>
            <p class="font-code-sm text-code-sm text-on-surface-variant mt-1">
              Wildcard TLS 1.3 · NOM-024
            </p>
          </div>
        </div>

        <div class="flex flex-wrap gap-3 pt-2">
          <NuxtLink
            to="/admin/branches"
            class="px-4 py-2.5 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-title-sm inline-flex items-center gap-1.5 transition-colors"
          >
            <span class="material-symbols-outlined text-[18px]">apartment</span>
            <span>Configurar Sedes y Consultorios</span>
          </NuxtLink>
          <NuxtLink
            to="/doctor/services"
            class="px-4 py-2.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-title-sm text-title-sm inline-flex items-center gap-1.5 transition-colors"
          >
            <span class="material-symbols-outlined text-[18px]">receipt_long</span>
            <span>Catálogo de Servicios</span>
          </NuxtLink>
          <NuxtLink
            to="/appointments/book"
            class="px-4 py-2.5 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-title-sm text-title-sm inline-flex items-center gap-1.5 transition-colors"
          >
            <span class="material-symbols-outlined text-[18px]">calendar_month</span>
            <span>Agendar Cita Médica</span>
          </NuxtLink>
        </div>
      </section>
    </div>
  </MedikaAppShell>
</template>
