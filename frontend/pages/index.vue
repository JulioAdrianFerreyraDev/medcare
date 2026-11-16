<script setup lang="ts">
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'

useHead({
  title: 'Panel Principal — Medika Clinical Platform'
})

interface PageCard {
  number: string
  category: string
  title: string
  route: string
  icon: string
  badgeTone: 'primary' | 'secondary' | 'tertiary'
  description: string
  highlights: string[]
}

const standardizedPages: PageCard[] = [
  {
    number: '01',
    category: 'Onboarding · Paso 1',
    title: 'Registro Inicial y Subdominio',
    route: '/onboarding',
    icon: 'domain_add',
    badgeTone: 'primary',
    description:
      'Registro de identidad institucional, validación de dominios reservados y sugerencias automáticas de subdominio disponible.',
    highlights: ['Validación de subdominio', 'Sugerencias inteligentes']
  },
  {
    number: '02',
    category: 'Onboarding · Paso 2',
    title: 'Configuración Clínica Inicial',
    route: '/onboarding/clinical-setup',
    icon: 'pin_drop',
    badgeTone: 'primary',
    description:
      'Configuración de sede matriz (Sede Centro), capacidad física de consultorios y especialidades médicas iniciales.',
    highlights: ['Sede Matriz', 'Especialidades activas']
  },
  {
    number: '03',
    category: 'Onboarding · Paso 3',
    title: 'Confirmación y Activación del Portal',
    route: '/onboarding/activated',
    icon: 'verified',
    badgeTone: 'secondary',
    description:
      'Activación del subdominio institucional (clinica-irma.medika.com), credenciales maestras y accesos directos de operación.',
    highlights: ['Portal activo', 'Credenciales enviadas']
  },
  {
    number: '04',
    category: 'Portal Institucional',
    title: 'Portal Clínico de Marca',
    route: '/portal',
    icon: 'language',
    badgeTone: 'secondary',
    description:
      'Entorno aislado por subdominio con contexto de marca activo y acceso directo a agenda, sedes y catálogo clínico.',
    highlights: ['Aislamiento por subdominio', 'TLS 1.3']
  },
  {
    number: '05',
    category: 'Infraestructura · Sedes',
    title: 'Gestión de Sedes y Consultorios',
    route: '/admin/branches',
    icon: 'apartment',
    badgeTone: 'primary',
    description:
      'Administración multi-sede (Sede Centro, Sede Norte) y mapeo de consultorios físicos con capacidad estricta de 1 consulta activa.',
    highlights: ['Multi-Sede', 'Capacidad = 1 consulta']
  },
  {
    number: '06',
    category: 'Infraestructura · Equipo',
    title: 'Personal por Sede y Asignaciones',
    route: '/admin/staff',
    icon: 'badge',
    badgeTone: 'primary',
    description:
      'Asignación de médicos en múltiples sedes y vinculación de personal de recepción a una única sede operativa.',
    highlights: ['Médico Multi-Sede', 'Recepción Sede Única']
  },
  {
    number: '07',
    category: 'Catálogo Clínico',
    title: 'Mis Servicios Clínicos y Tarifas',
    route: '/doctor/services',
    icon: 'receipt_long',
    badgeTone: 'secondary',
    description:
      'Catálogo médico con duración por servicio (20m, 45m, 60m), aranceles en MXN y pausa de servicios conservando historial.',
    highlights: ['Tarifas en MXN', 'Control de estado']
  },
  {
    number: '08',
    category: 'Agendamiento Clínico',
    title: 'Agendar Cita por Duración Continua',
    route: '/appointments/book',
    icon: 'calendar_month',
    badgeTone: 'primary',
    description:
      'Reserva de bloques continuos según la duración del servicio clínico y prevención de solapamientos de horario.',
    highlights: ['Bloque continuo', 'Prevención de traslapes']
  },
  {
    number: '09',
    category: 'Disponibilidad · Turnos',
    title: 'Turnos y Horarios del Especialista',
    route: '/provider/shifts',
    icon: 'schedule',
    badgeTone: 'primary',
    description:
      'Configuración de jornadas divididas por sede (09:00-13:00 y 15:00-19:00), bloqueo de comida y validador entre sucursales.',
    highlights: ['Jornada y descanso', 'Validador inter-sede']
  },
  {
    number: '10',
    category: 'Disponibilidad · Excepciones',
    title: 'Ausencias y Cancelaciones de Emergencia',
    route: '/provider/time-off',
    icon: 'event_busy',
    badgeTone: 'tertiary',
    description:
      'Bloqueo de vacaciones programadas, cancelación de emergencia con reprogramación prioritaria y bitácora de auditoría.',
    highlights: ['Vacaciones', 'Urgencias y Auditoría']
  },
  {
    number: '11',
    category: 'Recepción y Pacientes',
    title: 'Reserva en Línea y Confirmación',
    route: '/booking/doctor-x',
    icon: 'lock_clock',
    badgeTone: 'secondary',
    description:
      'Pre-bloqueo temporal de 5 minutos para el paciente y confirmación autenticada con prevención de doble reserva.',
    highlights: ['Reserva temporal 5m', 'Confirmación verificada']
  }
]
</script>

<template>
  <MedikaAppShell
    title="Panel Principal de Módulos Clínicos"
    breadcrumb-section="Inicio"
    story-badge="Medika OS v4.2"
    active-nav="hub"
  >
    <template #header-actions>
      <NuxtLink
        to="/onboarding"
        class="h-8 px-3 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-xs inline-flex items-center gap-1.5 shadow-subtle transition-colors"
      >
        <span class="material-symbols-outlined text-[16px]">play_arrow</span>
        <span>Iniciar Recorrido</span>
      </NuxtLink>
    </template>

    <div class="space-y-space-xl">
      <!-- Platform Overview Hero Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg lg:p-space-xl border border-outline-variant/30 shadow-subtle space-y-space-md">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary-fixed text-on-primary-fixed font-label-sm text-xs font-semibold">
            <span class="material-symbols-outlined text-[16px] text-primary">vital_signs</span>
            Plataforma Clínica Multi-Sede · Navegación Unificada
          </span>
          <span class="font-code-sm text-xs text-outline">
            Clínica Irma · clinica-irma.medika.com
          </span>
        </div>

        <div class="space-y-2 max-w-3xl">
          <h1 class="font-display-lg text-headline-lg lg:text-display-lg text-on-surface tracking-tight">
            Consola Operativa — Medika Clinical Platform
          </h1>
          <p class="font-body-lg text-body-md lg:text-body-lg text-on-surface-variant leading-relaxed">
            Administra el registro institucional, sucursales físicas, consultorios, catálogo de servicios médicos, turnos por sede y agenda de pacientes desde una interfaz estandarizada.
          </p>
        </div>

        <!-- Quick Workflow Pipeline -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-space-sm pt-space-xs">
          <NuxtLink
            to="/onboarding"
            class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center gap-2.5 transition-colors"
          >
            <span class="w-7 h-7 rounded-lg bg-primary text-on-primary font-code-sm text-xs font-bold flex items-center justify-center shrink-0">1</span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Onboarding</p>
              <p class="font-body-sm text-[11px] text-outline truncate">Marca y Subdominio</p>
            </div>
          </NuxtLink>

          <NuxtLink
            to="/admin/branches"
            class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center gap-2.5 transition-colors"
          >
            <span class="w-7 h-7 rounded-lg bg-primary text-on-primary font-code-sm text-xs font-bold flex items-center justify-center shrink-0">2</span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Infraestructura</p>
              <p class="font-body-sm text-[11px] text-outline truncate">Sedes y Personal</p>
            </div>
          </NuxtLink>

          <NuxtLink
            to="/doctor/services"
            class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center gap-2.5 transition-colors"
          >
            <span class="w-7 h-7 rounded-lg bg-primary text-on-primary font-code-sm text-xs font-bold flex items-center justify-center shrink-0">3</span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Catálogo Clínico</p>
              <p class="font-body-sm text-[11px] text-outline truncate">Servicios y Tarifas</p>
            </div>
          </NuxtLink>

          <NuxtLink
            to="/provider/shifts"
            class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center gap-2.5 transition-colors"
          >
            <span class="w-7 h-7 rounded-lg bg-primary text-on-primary font-code-sm text-xs font-bold flex items-center justify-center shrink-0">4</span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Disponibilidad</p>
              <p class="font-body-sm text-[11px] text-outline truncate">Turnos y Ausencias</p>
            </div>
          </NuxtLink>

          <NuxtLink
            to="/booking/doctor-x"
            class="p-3 rounded-xl bg-surface-container-low hover:bg-surface-container border border-outline-variant/30 flex items-center gap-2.5 transition-colors"
          >
            <span class="w-7 h-7 rounded-lg bg-secondary text-on-secondary font-code-sm text-xs font-bold flex items-center justify-center shrink-0">5</span>
            <div class="min-w-0">
              <p class="font-title-sm text-xs text-on-surface truncate">Agenda Pública</p>
              <p class="font-body-sm text-[11px] text-outline truncate">Reserva en Línea</p>
            </div>
          </NuxtLink>
        </div>
      </section>

      <!-- Standardized Modules Grid -->
      <section class="space-y-space-md">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h2 class="font-headline-md text-headline-md text-on-surface">
              Módulos de la Plataforma Clínica
            </h2>
            <p class="font-body-sm text-body-sm text-on-surface-variant">
              Selecciona cualquier vista o utiliza la barra lateral fija para recorrer el flujo completo.
            </p>
          </div>
          <span class="font-code-sm text-xs text-primary bg-primary-fixed/60 px-3 py-1 rounded-full font-semibold">
            11 Módulos Integrados
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
                  {{ page.category }}
                </span>
              </div>

              <div>
                <span class="font-code-sm text-[11px] text-outline">Módulo {{ page.number }}</span>
                <h3 class="font-headline-sm text-headline-sm text-on-surface group-hover:text-primary transition-colors mt-0.5">
                  {{ page.title }}
                </h3>
              </div>

              <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                {{ page.description }}
              </p>

              <div class="flex flex-wrap gap-1.5 pt-1">
                <span
                  v-for="hl in page.highlights"
                  :key="hl"
                  class="px-2 py-0.5 rounded bg-surface-container text-on-surface-variant font-label-sm text-[11px]"
                >
                  {{ hl }}
                </span>
              </div>
            </div>

            <div class="pt-3 border-t border-outline-variant/25 flex items-center justify-between">
              <span class="font-code-sm text-code-sm text-outline truncate">{{ page.route }}</span>
              <span class="font-title-sm text-title-sm text-primary inline-flex items-center gap-1 group-hover:translate-x-0.5 transition-transform">
                Abrir módulo
                <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
              </span>
            </div>
          </NuxtLink>
        </div>
      </section>
    </div>
  </MedikaAppShell>
</template>
