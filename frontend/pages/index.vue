<script setup lang="ts">
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'

useHead({
  title: 'Consola de Módulos Clínicos — Layouts Estandarizados'
})

interface PageCard {
  number: string
  story: string
  title: string
  route: string
  icon: string
  badgeTone: 'primary' | 'secondary' | 'tertiary'
  description: string
  acceptanceCriteria: string[]
}

const standardizedPages: PageCard[] = [
  {
    number: '01',
    story: 'AHC-3 · Paso 1',
    title: 'Registro Inicial y Subdominio',
    route: '/onboarding',
    icon: 'domain_add',
    badgeTone: 'primary',
    description:
      'Registro de marca médica, validación de slugs reservados y manejo de colisión 409 Conflict con 3 sugerencias instantáneas.',
    acceptanceCriteria: ['AC-1: Validación de slug', 'AC-2: Sugerencias 409 Conflict']
  },
  {
    number: '02',
    story: 'AHC-3 · Paso 2',
    title: 'Configuración Clínica Inicial',
    route: '/onboarding/clinical-setup',
    icon: 'pin_drop',
    badgeTone: 'primary',
    description:
      'Configuración de sede matriz (Sede Centro), capacidad física de consultorios y especialidades médicas iniciales.',
    acceptanceCriteria: ['AC-1: Sede Matriz', 'Especialidades iniciales']
  },
  {
    number: '03',
    story: 'AHC-3 · Paso 3',
    title: 'Confirmación y Activación del Portal',
    route: '/onboarding/activated',
    icon: 'verified',
    badgeTone: 'secondary',
    description:
      'Activación del subdominio institucional (clinica-irma.medika.com), credenciales maestras y pasos operativos siguientes.',
    acceptanceCriteria: ['AC-1: Portal activo', 'AC-3: Resolución de Host']
  },
  {
    number: '04',
    story: 'AHC-3 · AC-3',
    title: 'Portal Clínico Multi-Tenant',
    route: '/portal',
    icon: 'language',
    badgeTone: 'secondary',
    description:
      'Vista aislada por subdominio con verificación en vivo de TenantContext y navegación directa a módulos de la marca.',
    acceptanceCriteria: ['AC-3: Aislamiento de Tenant por Host']
  },
  {
    number: '05',
    story: 'AHC-4 · Sedes',
    title: 'Gestión de Sedes y Consultorios',
    route: '/admin/branches',
    icon: 'apartment',
    badgeTone: 'primary',
    description:
      'Administración multi-sede (Sede Centro, Sede Norte) y mapeo de consultorios físicos con capacidad estricta = 1 consulta activa.',
    acceptanceCriteria: ['AC-1: Multi-Sede', 'AC-2: Capacidad = 1 (btree_gist)']
  },
  {
    number: '06',
    story: 'AHC-4 · Personal',
    title: 'Personal por Sede y Asignaciones',
    route: '/admin/staff',
    icon: 'badge',
    badgeTone: 'primary',
    description:
      'Asignación de médicos en múltiples sedes y restricción estricta de recepcionistas a una única sede (422 Unprocessable Entity).',
    acceptanceCriteria: ['AC-3: Médico Multi-Sede', 'AC-3: Recepción Sede Única']
  },
  {
    number: '07',
    story: 'AHC-5 · Catálogo',
    title: 'Mis Servicios Clínicos y Tarifas',
    route: '/doctor/services',
    icon: 'receipt_long',
    badgeTone: 'secondary',
    description:
      'Catálogo médico con duración por servicio (20m, 45m, 60m), tarifas en MXN y desactivación suave (INACTIVE) conservando historial.',
    acceptanceCriteria: ['AC-1: Tarifas MXN', 'AC-3: Soft-deactivation INACTIVE']
  },
  {
    number: '08',
    story: 'AHC-5 · Agenda',
    title: 'Agendar Cita por Duración Continua',
    route: '/appointments/book',
    icon: 'calendar_month',
    badgeTone: 'primary',
    description:
      'Reserva de bloques continuos según la duración del servicio clínico (60 min) y prevención de solapamientos parciales (409 Conflict).',
    acceptanceCriteria: ['AC-2: Bloque continuo', 'Prevención de solapamiento']
  },
  {
    number: '09',
    story: 'AHC-6 · Turnos',
    title: 'Turnos y Horarios del Especialista',
    route: '/provider/shifts',
    icon: 'schedule',
    badgeTone: 'primary',
    description:
      'Configuración de jornadas divididas por sede (09:00-13:00 y 15:00-19:00), bloqueo de comida (13:00-15:00) y validador inter-sede (422).',
    acceptanceCriteria: ['AC-1: Turnos y descanso', 'AC-2: Colisión entre sedes 422']
  },
  {
    number: '10',
    story: 'AHC-7 · Ausencias',
    title: 'Ausencias y Cancelaciones de Emergencia',
    route: '/provider/time-off',
    icon: 'event_busy',
    badgeTone: 'tertiary',
    description:
      'Bloqueo de vacaciones programadas, cancelación de emergencia (REQUIRES_RESCHEDULING), alerta a recepción y bitácora inmutable.',
    acceptanceCriteria: ['AC-1: Vacaciones', 'AC-2: Emergencia', 'AC-3: Auditoría']
  },
  {
    number: '11',
    story: 'AHC-8 · Hold 5m',
    title: 'Reserva Optimista (TTL 300s) y Confirmación',
    route: '/booking/doctor-x',
    icon: 'lock_clock',
    badgeTone: 'secondary',
    description:
      'Pre-bloqueo Redis de 300s sin fricción, confirmación autenticada con OTP/JWT + holdToken (201 Created) y exclusión btree_gist (409).',
    acceptanceCriteria: ['AC-1: Hold 300s', 'AC-2: JWT + holdToken', 'AC-3/4: TTL & GIST']
  }
]
</script>

<template>
  <MedikaAppShell
    title="Consola de Layouts Estandarizados"
    breadcrumb-section="Arquitectura UI & Sistema de Diseño"
    story-badge="AHC-3 a AHC-8 · 11 Vistas"
    active-nav="hub"
  >
    <template #header-actions>
      <NuxtLink
        to="/appointments/book"
        class="h-8 px-3 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-xs inline-flex items-center gap-1.5 shadow-subtle transition-colors"
      >
        <span class="material-symbols-outlined text-[16px]">add_circle</span>
        <span>Nueva Cita</span>
      </NuxtLink>
    </template>

    <div class="space-y-space-xl">
      <!-- Standardized Architecture Hero Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg lg:p-space-xl border border-outline-variant/30 shadow-subtle space-y-space-md">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-fixed text-on-primary-fixed font-label-sm text-xs font-semibold">
            <span class="material-symbols-outlined text-[16px] text-primary">dock_to_right</span>
            App Shell Unificado: Sidebar Persistente (256px) + Header Superior (56px)
          </span>
          <span class="font-code-sm text-xs text-outline">
            Stitch Project: 6962433513807822761 · Design System: Clinical Clarity
          </span>
        </div>

        <div class="space-y-2 max-w-3xl">
          <h1 class="font-display-lg text-headline-lg lg:text-display-lg text-on-surface tracking-tight">
            Sistema de Layouts Estandarizados — Medika Clinical OS
          </h1>
          <p class="font-body-lg text-body-md lg:text-body-lg text-on-surface-variant leading-relaxed">
            Todas las páginas requeridas por las Historias de Usuario (<span class="font-code-sm text-on-surface font-semibold">AHC-3</span> a <span class="font-code-sm text-on-surface font-semibold">AHC-8</span>) comparten el mismo contenedor estructural con barra lateral izquierda prioritaria, cabecera contextual fija y la paleta de color unificada <span class="font-semibold text-primary">Clinical Clarity</span>.
          </p>
        </div>

        <!-- Unified Color Palette Swatch Bar -->
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-space-sm pt-space-xs">
          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-primary shrink-0 shadow-subtle"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Primary</p>
              <p class="font-code-sm text-[11px] text-outline">#006194</p>
            </div>
          </div>

          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-primary-container shrink-0 shadow-subtle"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Primary Container</p>
              <p class="font-code-sm text-[11px] text-outline">#007bb9</p>
            </div>
          </div>

          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-secondary shrink-0 shadow-subtle"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Secondary (OK)</p>
              <p class="font-code-sm text-[11px] text-outline">#006c4a</p>
            </div>
          </div>

          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-tertiary shrink-0 shadow-subtle"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Tertiary (Alert)</p>
              <p class="font-code-sm text-[11px] text-outline">#ba0035</p>
            </div>
          </div>

          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-surface border border-outline-variant shrink-0"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Surface Canvas</p>
              <p class="font-code-sm text-[11px] text-outline">#faf8ff</p>
            </div>
          </div>

          <div class="p-3 rounded-lg bg-surface border border-outline-variant/40 flex items-center gap-2.5">
            <span class="w-6 h-6 rounded-md bg-on-surface shrink-0"></span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">On Surface</p>
              <p class="font-code-sm text-[11px] text-outline">#131b2e</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Standardized Pages Grid (All User Stories AHC-3 to AHC-8) -->
      <section class="space-y-space-md">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 class="font-headline-md text-headline-md text-on-surface">
              Páginas Estandarizadas por Historia de Usuario
            </h2>
            <p class="font-body-sm text-body-sm text-on-surface-variant">
              Selecciona cualquier módulo para abrirlo dentro del mismo layout de Sidebar + Header y paleta de colores unificada.
            </p>
          </div>
          <span class="font-code-sm text-xs text-primary bg-primary-fixed/60 px-3 py-1 rounded-full font-semibold">
            11 rutas activas · 6 Historias de Usuario
          </span>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-space-md">
          <NuxtLink
            v-for="page in standardizedPages"
            :key="page.route"
            :to="page.route"
            class="group bg-surface-container-lowest hover:bg-surface-container-low/60 rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle hover:shadow-raised transition-all flex flex-col justify-between gap-space-md"
          >
            <div class="space-y-3">
              <div class="flex items-center justify-between gap-2">
                <div
                  class="w-10 h-10 rounded-lg flex items-center justify-center shrink-0"
                  :class="
                    page.badgeTone === 'secondary'
                      ? 'bg-secondary-container text-on-secondary-container'
                      : page.badgeTone === 'tertiary'
                        ? 'bg-tertiary-fixed text-on-tertiary-fixed'
                        : 'bg-primary-fixed text-primary'
                  "
                >
                  <span class="material-symbols-outlined text-[20px]">{{ page.icon }}</span>
                </div>

                <span
                  class="font-code-sm text-code-sm px-2.5 py-0.5 rounded-full font-semibold"
                  :class="
                    page.badgeTone === 'secondary'
                      ? 'bg-secondary-container/60 text-on-secondary-container'
                      : page.badgeTone === 'tertiary'
                        ? 'bg-tertiary-fixed/70 text-on-tertiary-fixed'
                        : 'bg-primary-fixed/70 text-on-primary-fixed'
                  "
                >
                  {{ page.story }}
                </span>
              </div>

              <div>
                <span class="font-code-sm text-[11px] text-outline">Vista #{{ page.number }}</span>
                <h3 class="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors mt-0.5">
                  {{ page.title }}
                </h3>
              </div>

              <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                {{ page.description }}
              </p>

              <div class="flex flex-wrap gap-1.5 pt-1">
                <span
                  v-for="ac in page.acceptanceCriteria"
                  :key="ac"
                  class="px-2 py-0.5 rounded bg-surface-container text-on-surface-variant font-label-sm text-[11px]"
                >
                  {{ ac }}
                </span>
              </div>
            </div>

            <div class="pt-3 border-t border-outline-variant/25 flex items-center justify-between">
              <span class="font-code-sm text-code-sm text-outline truncate">{{ page.route }}</span>
              <span class="font-title-sm text-title-sm text-primary inline-flex items-center gap-1 group-hover:translate-x-0.5 transition-transform">
                Abrir vista
                <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
              </span>
            </div>
          </NuxtLink>
        </div>
      </section>
    </div>
  </MedikaAppShell>
</template>
