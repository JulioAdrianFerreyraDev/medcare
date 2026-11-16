<script setup lang="ts">
import { ref, computed } from 'vue'
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
    storyBadge: 'Medika OS v4.2',
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
  story: string
}

interface NavGroup {
  groupLabel: string
  items: NavItem[]
}

const navGroups: NavGroup[] = [
  {
    groupLabel: 'Vista General',
    items: [
      {
        id: 'hub',
        label: 'Consola de Módulos',
        to: '/',
        icon: 'dashboard',
        story: 'HUB'
      }
    ]
  },
  {
    groupLabel: 'Tenancy & Onboarding (AHC-3)',
    items: [
      {
        id: 'onboarding-step-1',
        label: '1. Registro y Subdominio',
        to: '/onboarding',
        icon: 'domain_add',
        story: 'AHC-3'
      },
      {
        id: 'onboarding-step-2',
        label: '2. Configuración Clínica',
        to: '/onboarding/clinical-setup',
        icon: 'pin_drop',
        story: 'AHC-3'
      },
      {
        id: 'onboarding-step-3',
        label: '3. Activación de Portal',
        to: '/onboarding/activated',
        icon: 'verified',
        story: 'AHC-3'
      },
      {
        id: 'tenant-portal',
        label: 'Portal Multi-Tenant',
        to: '/portal',
        icon: 'language',
        story: 'AHC-3'
      }
    ]
  },
  {
    groupLabel: 'Sedes & Consultorios (AHC-4)',
    items: [
      {
        id: 'branches',
        label: 'Sedes y Consultorios',
        to: '/admin/branches',
        icon: 'apartment',
        story: 'AHC-4'
      },
      {
        id: 'staff',
        label: 'Personal por Sede',
        to: '/admin/staff',
        icon: 'badge',
        story: 'AHC-4'
      }
    ]
  },
  {
    groupLabel: 'Catálogo Clínico (AHC-5)',
    items: [
      {
        id: 'services',
        label: 'Mis Servicios y Tarifas',
        to: '/doctor/services',
        icon: 'receipt_long',
        story: 'AHC-5'
      },
      {
        id: 'appointments-book',
        label: 'Agenda por Duración',
        to: '/appointments/book',
        icon: 'calendar_month',
        story: 'AHC-5'
      }
    ]
  },
  {
    groupLabel: 'Turnos & Disponibilidad (AHC-6/7)',
    items: [
      {
        id: 'provider-shifts',
        label: 'Turnos y Horarios',
        to: '/provider/shifts',
        icon: 'schedule',
        story: 'AHC-6'
      },
      {
        id: 'provider-timeoff',
        label: 'Ausencias y Emergencias',
        to: '/provider/time-off',
        icon: 'event_busy',
        story: 'AHC-7'
      }
    ]
  },
  {
    groupLabel: 'Reserva Optimista (AHC-8)',
    items: [
      {
        id: 'optimistic-booking',
        label: 'Reserva Optimista (TTL 5m)',
        to: '/booking/doctor-x',
        icon: 'lock_clock',
        story: 'AHC-8'
      }
    ]
  }
]

function isItemActive(item: NavItem): boolean {
  if (props.activeNav) {
    return props.activeNav === item.id
  }
  return route.path === item.to
}
</script>

<template>
  <div class="min-h-screen bg-surface text-on-surface font-body-md antialiased flex selection:bg-primary-fixed selection:text-on-primary-fixed">
    <!-- Mobile Backdrop -->
    <div
      v-if="mobileSidebarOpen"
      class="fixed inset-0 bg-inverse-surface/40 backdrop-blur-xs z-40 lg:hidden"
      @click="mobileSidebarOpen = false"
    ></div>

    <!-- =================================================================== -->
    <!-- STANDARDIZED PERSISTENT LEFT SIDEBAR (w-64 / 256px)                 -->
    <!-- =================================================================== -->
    <aside
      class="w-64 shrink-0 bg-surface-container-lowest border-r border-outline-variant/40 flex flex-col justify-between fixed inset-y-0 left-0 z-50 lg:static lg:translate-x-0 transition-transform duration-200 select-none"
      :class="mobileSidebarOpen ? 'translate-x-0' : '-translate-x-full'"
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
                Clinical SaaS Platform
              </span>
            </div>
          </NuxtLink>

          <button
            type="button"
            class="lg:hidden p-1 rounded-lg text-outline hover:text-on-surface hover:bg-surface-container-low"
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

        <!-- Standardized Navigation Groups (All User Stories AHC-3 to AHC-8) -->
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
                {{ item.story }}
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
            title="Ir al inicio de consola"
          >
            <span class="material-symbols-outlined text-[18px]">home</span>
          </NuxtLink>
        </div>
      </div>
    </aside>

    <!-- =================================================================== -->
    <!-- RIGHT WORKSPACE COLUMN: STICKY TOP HEADER + MAIN PAGE CANVAS        -->
    <!-- =================================================================== -->
    <div class="flex-1 flex flex-col min-w-0 min-h-screen bg-surface">
      <!-- Standardized Sticky Top Header (h-14 / 56px) -->
      <header class="h-14 px-margin-mobile lg:px-margin bg-surface-container-lowest/95 backdrop-blur-md border-b border-outline-variant/40 sticky top-0 z-30 flex items-center justify-between gap-4 shrink-0">
        <!-- Left: Mobile Drawer Trigger + Breadcrumb + Sync Pill -->
        <div class="flex items-center gap-3 min-w-0">
          <button
            type="button"
            class="lg:hidden p-1.5 rounded-lg text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low"
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
            <span class="font-label-md text-on-surface-variant truncate hidden md:inline">
              {{ breadcrumbSection }}
            </span>
            <span class="text-outline-variant hidden md:inline">/</span>
            <span class="font-title-sm text-on-surface font-semibold truncate">
              {{ title }}
            </span>
          </nav>

          <span class="hidden xl:inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-secondary-container/60 text-on-secondary-container font-label-sm text-[11px] font-semibold border border-secondary/20 shrink-0">
            <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
            <span>{{ syncLabel }}</span>
          </span>
        </div>

        <!-- Right: Global Search + Story Badge + Contextual Header Actions -->
        <div class="flex items-center gap-2.5 shrink-0">
          <div class="relative hidden md:flex items-center">
            <span class="material-symbols-outlined absolute left-2.5 text-outline text-[16px] pointer-events-none">
              search
            </span>
            <input
              v-model="globalSearch"
              type="search"
              placeholder="Buscar sede, médico, servicio..."
              aria-label="Búsqueda rápida en plataforma"
              class="h-8 w-56 lg:w-64 pl-8 pr-10 rounded-lg bg-surface-container-low hover:bg-surface-container focus:bg-surface-container-lowest border border-outline-variant/40 text-xs text-on-surface placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
            />
            <kbd class="absolute right-2 px-1.5 py-0.5 text-[10px] font-code-sm text-outline bg-surface-container-lowest border border-outline-variant/40 rounded">
              ⌘K
            </kbd>
          </div>

          <span class="hidden sm:inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-primary-fixed/60 text-on-primary-fixed font-code-sm text-[11px] font-semibold">
            <span class="material-symbols-outlined text-[14px] text-primary">verified_user</span>
            <span>{{ storyBadge }}</span>
          </span>

          <!-- Page-specific Header Actions Slot -->
          <slot name="header-actions" />
        </div>
      </header>

      <!-- Main Standardized Canvas Area -->
      <main class="flex-1 w-full max-w-7xl mx-auto px-margin-mobile lg:px-margin py-space-lg lg:py-space-xl">
        <slot />
      </main>

      <!-- Standardized Institutional Footer -->
      <footer class="w-full px-margin-mobile lg:px-margin py-space-md border-t border-outline-variant/30 bg-surface-container-lowest/60 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-outline">
        <div class="flex flex-wrap items-center gap-3">
          <span class="inline-flex items-center gap-1 font-label-sm text-secondary font-semibold">
            <span class="material-symbols-outlined text-[15px]">verified</span>
            NOM-024-SSA3-2012 &amp; HIPAA Compliant
          </span>
          <span class="text-outline-variant">•</span>
          <span class="font-code-sm text-[11px] text-on-surface-variant">
            Tenant: {{ activeTenantSlug }}.medika.com
          </span>
          <span class="text-outline-variant">•</span>
          <span class="font-code-sm text-[11px] text-outline">
            Paleta Unificada: Clinical Clarity (#006194 / #006c4a / #ba0035 / #faf8ff)
          </span>
        </div>
        <div class="font-label-sm text-[11px] text-outline">
          Medika Clinical SaaS Platform · Arquitectura UI Estandarizada (Sidebar + Header)
        </div>
      </footer>
    </div>
  </div>
</template>
