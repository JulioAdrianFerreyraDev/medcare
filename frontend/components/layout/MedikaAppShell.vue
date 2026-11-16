<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useTenancyStore } from '~/stores/tenancy'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    breadcrumbSection?: string
    storyBadge?: string
    syncLabel?: string
    activeNav?: string
    userRoleLabel?: string
    userName?: string
    userInitials?: string
  }>(),
  {
    subtitle: '',
    breadcrumbSection: 'Operaciones Clínicas',
    storyBadge: 'Medika OS',
    syncLabel: 'Sincronizado en tiempo real',
    activeNav: '',
    userRoleLabel: 'Admin Clínico & Especialista',
    userName: 'Dra. Marcela Reyes',
    userInitials: 'MR'
  }
)

const route = useRoute()
const tenancyStore = useTenancyStore()
const mobileSidebarOpen = ref(false)
const globalSearch = ref('')
const contentScrollContainer = ref<HTMLElement | null>(null)

const activeTenantSlug = computed(() => {
  return tenancyStore.resolvedSlug || 'clinica-irma'
})

const activeTenantName = computed(() => {
  return tenancyStore.activeBrand?.displayName || 'Clínica Irma'
})

interface NavItem {
  id: string
  label: string
  to: string
  icon: string
  tag: string
}

interface NavGroup {
  groupLabel: string
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    groupLabel: 'Inicio',
    items: [
      {
        id: 'hub',
        label: 'Panel Principal',
        to: '/',
        icon: 'dashboard',
        tag: 'Inicio'
      }
    ]
  },
  {
    groupLabel: 'Onboarding Institucional',
    items: [
      {
        id: 'onboarding-step-1',
        label: '1. Registro y Subdominio',
        to: '/onboarding',
        icon: 'domain_add',
        tag: 'Paso 1'
      },
      {
        id: 'onboarding-step-2',
        label: '2. Configuración Clínica',
        to: '/onboarding/clinical-setup',
        icon: 'pin_drop',
        tag: 'Paso 2'
      },
      {
        id: 'onboarding-step-3',
        label: '3. Activación de Portal',
        to: '/onboarding/activated',
        icon: 'verified',
        tag: 'Paso 3'
      },
      {
        id: 'tenant-portal',
        label: 'Portal Institucional',
        to: '/portal',
        icon: 'language',
        tag: 'Portal'
      }
    ]
  },
  {
    groupLabel: 'Infraestructura y Sedes',
    items: [
      {
        id: 'branches',
        label: 'Sedes y Consultorios',
        to: '/admin/branches',
        icon: 'apartment',
        tag: 'Sedes'
      },
      {
        id: 'staff',
        label: 'Personal por Sede',
        to: '/admin/staff',
        icon: 'badge',
        tag: 'Equipo'
      }
    ]
  },
  {
    groupLabel: 'Catálogo Clínico',
    items: [
      {
        id: 'services',
        label: 'Mis Servicios y Tarifas',
        to: '/doctor/services',
        icon: 'receipt_long',
        tag: 'Tarifas'
      },
      {
        id: 'appointments-book',
        label: 'Agenda por Duración',
        to: '/appointments/book',
        icon: 'calendar_month',
        tag: 'Agenda'
      }
    ]
  },
  {
    groupLabel: 'Disponibilidad Médica',
    items: [
      {
        id: 'provider-shifts',
        label: 'Turnos y Horarios',
        to: '/provider/shifts',
        icon: 'schedule',
        tag: 'Turnos'
      },
      {
        id: 'provider-timeoff',
        label: 'Ausencias y Urgencias',
        to: '/provider/time-off',
        icon: 'event_busy',
        tag: 'Bloqueos'
      }
    ]
  },
  {
    groupLabel: 'Recepción y Pacientes',
    items: [
      {
        id: 'optimistic-booking',
        label: 'Reserva en Línea',
        to: '/booking/doctor-x',
        icon: 'lock_clock',
        tag: 'Citas'
      }
    ]
  }
]

const orderedFlowItems = computed<NavItem[]>(() => {
  return navGroups.flatMap((group) => group.items)
})

function isItemActive(item: NavItem): boolean {
  if (props.activeNav) {
    return props.activeNav === item.id
  }
  return route.path === item.to
}

const currentFlowIndex = computed(() => {
  const idx = orderedFlowItems.value.findIndex((item) => isItemActive(item))
  return idx >= 0 ? idx : 0
})

const previousFlowItem = computed<NavItem | null>(() => {
  return currentFlowIndex.value > 0 ? orderedFlowItems.value[currentFlowIndex.value - 1] ?? null : null
})

const nextFlowItem = computed<NavItem | null>(() => {
  return currentFlowIndex.value < orderedFlowItems.value.length - 1
    ? orderedFlowItems.value[currentFlowIndex.value + 1] ?? null
    : null
})

watch(
  () => route.fullPath,
  () => {
    mobileSidebarOpen.value = false
    if (contentScrollContainer.value) {
      contentScrollContainer.value.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
)
</script>

<template>
  <div class="h-screen w-full overflow-hidden bg-surface text-on-surface font-body-md antialiased flex selection:bg-primary-fixed selection:text-on-primary-fixed">
    <!-- Mobile Backdrop (< 768px) -->
    <div
      v-if="mobileSidebarOpen"
      class="fixed inset-0 bg-inverse-surface/40 backdrop-blur-xs z-40 md:hidden"
      @click="mobileSidebarOpen = false"
    ></div>

    <!-- =================================================================== -->
    <!-- STICKY / FIXED LEFT SIDEBAR (w-64 / 256px, locked on screen)        -->
    <!-- =================================================================== -->
    <aside
      class="w-64 h-screen shrink-0 bg-surface-container-lowest border-r border-outline-variant/40 flex flex-col justify-between fixed inset-y-0 left-0 z-50 md:sticky md:top-0 md:translate-x-0 transition-transform duration-200 select-none overflow-hidden"
      :class="mobileSidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'"
      aria-label="Navegación Principal Medika OS"
    >
      <div class="flex flex-col min-h-0 flex-1">
        <!-- Brand Identity Header (h-14) -->
        <div class="h-14 px-space-md border-b border-outline-variant/30 flex items-center justify-between shrink-0">
          <NuxtLink to="/" class="flex items-center gap-space-sm group">
            <div class="w-8 h-8 rounded-lg bg-primary text-on-primary flex items-center justify-center shadow-subtle group-hover:bg-primary-container transition-colors">
              <span class="material-symbols-outlined text-[19px]">vital_signs</span>
            </div>
            <div class="flex flex-col">
              <div class="flex items-center gap-1.5">
                <span class="font-headline-sm text-title-md font-bold text-on-surface tracking-tight leading-none">
                  Medika OS
                </span>
                <span class="px-1.5 py-0.5 rounded bg-primary-fixed text-on-primary-fixed font-code-sm text-[10px] font-semibold">
                  v4.2
                </span>
              </div>
              <span class="font-label-sm text-[10px] uppercase tracking-wider text-outline mt-0.5">
                Plataforma Clínica
              </span>
            </div>
          </NuxtLink>

          <button
            type="button"
            class="md:hidden p-1 rounded-lg text-outline hover:text-on-surface hover:bg-surface-container-low"
            aria-label="Cerrar menú lateral"
            @click="mobileSidebarOpen = false"
          >
            <span class="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>

        <!-- Active Tenant / Subdomain Context Switcher -->
        <div class="p-space-sm border-b border-outline-variant/25 shrink-0">
          <NuxtLink
            to="/portal"
            class="p-2.5 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center justify-between transition-colors"
          >
            <div class="flex items-center gap-2.5 min-w-0">
              <div class="w-8 h-8 rounded-lg bg-secondary-container text-on-secondary-container font-title-sm text-xs font-bold flex items-center justify-center shrink-0">
                CI
              </div>
              <div class="flex flex-col min-w-0">
                <div class="flex items-center gap-1.5">
                  <span class="w-1.5 h-1.5 rounded-full bg-secondary shrink-0"></span>
                  <span class="font-title-sm text-xs font-semibold text-on-surface truncate">
                    {{ activeTenantName }}
                  </span>
                </div>
                <span class="font-code-sm text-[11px] text-outline truncate">
                  {{ activeTenantSlug }}.medika.com
                </span>
              </div>
            </div>
            <span class="material-symbols-outlined text-outline text-[16px] shrink-0">unfold_more</span>
          </NuxtLink>
        </div>

        <!-- Standardized Navigation Groups -->
        <nav class="flex-1 overflow-y-auto px-space-sm py-space-sm space-y-4">
          <div v-for="group in navGroups" :key="group.groupLabel" class="space-y-1">
            <div class="px-2.5 py-1 flex items-center justify-between">
              <span class="font-label-sm text-[10px] font-bold uppercase tracking-wider text-outline">
                {{ group.groupLabel }}
              </span>
            </div>

            <NuxtLink
              v-for="item in group.items"
              :key="item.id"
              :to="item.to"
              class="flex items-center justify-between gap-2 px-2.5 py-2 rounded-lg text-xs transition-all"
              :class="
                isItemActive(item)
                  ? 'bg-primary-fixed text-on-primary-fixed font-semibold shadow-subtle border border-primary/15'
                  : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low font-medium'
              "
              @click="mobileSidebarOpen = false"
            >
              <div class="flex items-center gap-2.5 min-w-0">
                <span
                  class="material-symbols-outlined text-[18px] shrink-0"
                  :class="isItemActive(item) ? 'text-primary' : 'text-outline'"
                >
                  {{ item.icon }}
                </span>
                <span class="truncate">{{ item.label }}</span>
              </div>
              <span
                class="font-code-sm text-[10px] px-1.5 py-0.5 rounded shrink-0"
                :class="
                  isItemActive(item)
                    ? 'bg-primary text-on-primary font-semibold'
                    : 'bg-surface-container text-outline'
                "
              >
                {{ item.tag }}
              </span>
            </NuxtLink>
          </div>
        </nav>
      </div>

      <!-- Standardized Sidebar Footer: Active Clinician / Admin Profile -->
      <div class="p-space-sm border-t border-outline-variant/30 bg-surface-container-lowest shrink-0">
        <div class="p-2.5 rounded-xl bg-surface-container-low border border-outline-variant/25 flex items-center justify-between gap-2">
          <div class="flex items-center gap-2.5 min-w-0">
            <div class="relative shrink-0">
              <div class="w-8 h-8 rounded-full bg-primary text-on-primary font-title-sm text-xs font-bold flex items-center justify-center shadow-subtle">
                {{ userInitials }}
              </div>
              <span class="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-secondary ring-2 ring-surface-container-lowest"></span>
            </div>
            <div class="flex flex-col min-w-0">
              <span class="font-title-sm text-xs font-semibold text-on-surface truncate">
                {{ userName }}
              </span>
              <span class="font-label-sm text-[11px] text-primary truncate">
                {{ userRoleLabel }}
              </span>
            </div>
          </div>

          <NuxtLink
            to="/"
            class="p-1.5 rounded-lg text-outline hover:text-on-surface hover:bg-surface-container transition-colors shrink-0"
            title="Ir al Panel Principal"
          >
            <span class="material-symbols-outlined text-[18px]">home</span>
          </NuxtLink>
        </div>
      </div>
    </aside>

    <!-- =================================================================== -->
    <!-- RIGHT WORKSPACE COLUMN: FIXED HEADER + INDEPENDENT SCROLLABLE BODY  -->
    <!-- =================================================================== -->
    <div class="flex-1 flex flex-col min-w-0 h-screen overflow-hidden bg-surface">
      <!-- Standardized Fixed Top Header (h-14 / 56px) -->
      <header class="h-14 px-margin-mobile lg:px-margin bg-surface-container-lowest border-b border-outline-variant/40 z-30 flex items-center justify-between gap-4 shrink-0">
        <!-- Left: Mobile Drawer Trigger + Breadcrumb + Sync Pill -->
        <div class="flex items-center gap-3 min-w-0">
          <button
            type="button"
            class="md:hidden p-1.5 rounded-lg text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low"
            aria-label="Abrir navegación lateral"
            @click="mobileSidebarOpen = true"
          >
            <span class="material-symbols-outlined text-[20px]">menu</span>
          </button>

          <nav aria-label="Breadcrumb" class="flex items-center gap-2 text-xs min-w-0">
            <NuxtLink to="/" class="font-label-md text-outline hover:text-primary transition-colors truncate hidden sm:inline">
              {{ activeTenantName }}
            </NuxtLink>
            <span class="text-outline-variant hidden sm:inline">/</span>
            <span class="font-label-md text-on-surface-variant truncate hidden lg:inline">
              {{ breadcrumbSection }}
            </span>
            <span class="text-outline-variant hidden lg:inline">/</span>
            <span class="font-title-sm text-on-surface font-semibold truncate">
              {{ title }}
            </span>
          </nav>

          <span class="hidden xl:inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-secondary-container/60 text-on-secondary-container font-label-sm text-[11px] font-semibold border border-secondary/20 shrink-0">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
            <span>{{ syncLabel }}</span>
          </span>
        </div>

        <!-- Right: Global Search + Flow Controls + Contextual Header Actions -->
        <div class="flex items-center gap-2 shrink-0">
          <div class="relative hidden lg:flex items-center">
            <span class="material-symbols-outlined absolute left-2.5 text-outline text-[16px] pointer-events-none">
              search
            </span>
            <input
              v-model="globalSearch"
              type="search"
              placeholder="Buscar sede, médico, servicio..."
              aria-label="Búsqueda rápida en plataforma"
              class="h-8 w-52 xl:w-60 pl-8 pr-10 rounded-lg bg-surface-container-low hover:bg-surface-container focus:bg-surface-container-lowest border border-outline-variant/40 text-xs text-on-surface placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
            />
            <kbd class="absolute right-2 px-1.5 py-0.5 text-[10px] font-code-sm text-outline bg-surface-container-lowest border border-outline-variant/40 rounded">
              ⌘K
            </kbd>
          </div>

          <!-- Quick Flow Stepper Buttons in Header -->
          <div class="hidden sm:flex items-center gap-1 bg-surface-container-low p-0.5 rounded-lg border border-outline-variant/30">
            <NuxtLink
              v-if="previousFlowItem"
              :to="previousFlowItem.to"
              class="h-7 px-2 rounded-md text-xs font-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-lowest inline-flex items-center gap-1 transition-colors"
              :title="`Anterior: ${previousFlowItem.label}`"
            >
              <span class="material-symbols-outlined text-[15px]">chevron_left</span>
              <span class="hidden xl:inline">Anterior</span>
            </NuxtLink>
            <span class="px-2 font-code-sm text-[11px] text-primary font-semibold">
              {{ currentFlowIndex + 1 }} / {{ orderedFlowItems.length }}
            </span>
            <NuxtLink
              v-if="nextFlowItem"
              :to="nextFlowItem.to"
              class="h-7 px-2 rounded-md text-xs font-label-md text-primary hover:bg-surface-container-lowest font-semibold inline-flex items-center gap-1 transition-colors"
              :title="`Siguiente: ${nextFlowItem.label}`"
            >
              <span class="hidden xl:inline">Siguiente</span>
              <span class="material-symbols-outlined text-[15px]">chevron_right</span>
            </NuxtLink>
          </div>

          <!-- Page-specific Header Actions Slot -->
          <slot name="header-actions" />
        </div>
      </header>

      <!-- ================================================================= -->
      <!-- INDEPENDENT SCROLLABLE CONTENT CONTAINER (ONLY THIS SCROLLS)      -->
      <!-- ================================================================= -->
      <div
        ref="contentScrollContainer"
        class="flex-1 overflow-y-auto flex flex-col justify-between"
      >
        <main class="flex-1 w-full max-w-7xl mx-auto px-margin-mobile lg:px-margin py-space-lg lg:py-space-xl space-y-space-xl">
          <slot />

          <!-- Guided Navigation Flow Bar at Bottom of Every Page -->
          <nav
            aria-label="Flujo de navegación entre módulos"
            class="pt-space-lg border-t border-outline-variant/30 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-space-md"
          >
            <div>
              <NuxtLink
                v-if="previousFlowItem"
                :to="previousFlowItem.to"
                class="group px-space-md py-2.5 rounded-xl bg-surface-container-lowest hover:bg-surface-container-low border border-outline-variant/40 shadow-subtle inline-flex items-center gap-2.5 transition-all"
              >
                <span class="material-symbols-outlined text-[18px] text-outline group-hover:text-primary group-hover:-translate-x-0.5 transition-transform">
                  arrow_back
                </span>
                <div class="flex flex-col text-left">
                  <span class="font-label-sm text-[10px] uppercase tracking-wider text-outline">Vista Anterior</span>
                  <span class="font-title-sm text-xs text-on-surface group-hover:text-primary transition-colors">
                    {{ previousFlowItem.label }}
                  </span>
                </div>
              </NuxtLink>
            </div>

            <div class="flex items-center justify-end">
              <NuxtLink
                v-if="nextFlowItem"
                :to="nextFlowItem.to"
                class="group px-space-md py-2.5 rounded-xl bg-primary hover:bg-primary-container text-on-primary shadow-subtle inline-flex items-center gap-2.5 transition-all"
              >
                <div class="flex flex-col text-right">
                  <span class="font-label-sm text-[10px] uppercase tracking-wider text-on-primary/80">Siguiente Módulo</span>
                  <span class="font-title-sm text-xs text-on-primary font-semibold">
                    {{ nextFlowItem.label }}
                  </span>
                </div>
                <span class="material-symbols-outlined text-[18px] text-on-primary group-hover:translate-x-0.5 transition-transform">
                  arrow_forward
                </span>
              </NuxtLink>
            </div>
          </nav>
        </main>

        <!-- Standardized Institutional Footer -->
        <footer class="w-full px-margin-mobile lg:px-margin py-space-md border-t border-outline-variant/30 bg-surface-container-lowest/60 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-outline shrink-0">
          <div class="flex flex-wrap items-center gap-3">
            <span class="inline-flex items-center gap-1 font-label-sm text-secondary font-semibold">
              <span class="material-symbols-outlined text-[15px]">verified</span>
              NOM-024-SSA3-2012 &amp; HIPAA Compliant
            </span>
            <span class="text-outline-variant">•</span>
            <span class="font-code-sm text-[11px] text-on-surface-variant">
              Portal: {{ activeTenantSlug }}.medika.com
            </span>
          </div>
          <div class="font-label-sm text-[11px] text-outline">
            Medika Clinical Platform · Sistema Operativo Clínico Multi-Sede
          </div>
        </footer>
      </div>
    </div>
  </div>
</template>
