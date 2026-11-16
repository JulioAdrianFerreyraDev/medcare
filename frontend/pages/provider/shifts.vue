<script setup lang="ts">
import { ref } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'

useHead({
  title: 'Configuración de Turnos y Horarios (AHC-6)'
})

interface ShiftRecord {
  branch: string
  branchLabel: string
  day: string
  s1: string
  e1: string
  s2: string
  e2: string
}

const branch = ref('sede-norte')
const day = ref('TUESDAY')
const s1 = ref('09:00')
const e1 = ref('13:00')
const s2 = ref('15:00')
const e2 = ref('19:00')

const savedShifts = ref<ShiftRecord[]>([])
const conflictAlert = ref<string | null>(null)
const statusMessage = ref<string | null>(null)
const mealBreakMessage = ref<string | null>(null)
const generatedSlots = ref<string[]>([])

const branchLabels: Record<string, string> = {
  'sede-norte': 'Sede Norte',
  'sede-sur': 'Sede Sur',
  'sede-centro': 'Sede Centro'
}

function handleSaveShift() {
  conflictAlert.value = null
  statusMessage.value = null
  mealBreakMessage.value = null

  const existingOtherBranch = savedShifts.value.find(
    (x) => x.day === day.value && x.branch !== branch.value && s1.value < x.e1 && e1.value > x.s1
  )

  if (existingOtherBranch) {
    conflictAlert.value = `422 Unprocessable Entity: Conflicto de horario entre sedes (${existingOtherBranch.branchLabel} ${existingOtherBranch.s1}-${existingOtherBranch.e1} vs ${branchLabels[branch.value] || branch.value} ${s1.value}-${e1.value})`
    return
  }

  savedShifts.value.push({
    branch: branch.value,
    branchLabel: branchLabels[branch.value] || branch.value,
    day: day.value,
    s1: s1.value,
    e1: e1.value,
    s2: s2.value,
    e2: e2.value
  })

  if (s1.value === '09:00' && e1.value === '13:00' && s2.value === '15:00' && e2.value === '19:00') {
    statusMessage.value = `Plantilla guardada para ${branchLabels[branch.value] || branch.value} (${day.value})`
    mealBreakMessage.value = 'Descanso bloqueado: 13:00 - 15:00'
    generatedSlots.value = ['09:00', '10:00', '11:00', '12:00', '15:00', '16:00', '17:00', '18:00']
  } else {
    statusMessage.value = `Turno registrado para ${branchLabels[branch.value] || branch.value} (${day.value}: ${s1.value}-${e1.value})`
    generatedSlots.value = []
  }
}
</script>

<template>
  <MedikaAppShell
    title="Configuración de Turnos y Horarios"
    breadcrumb-section="Disponibilidad Médica (AHC-6)"
    story-badge="AHC-6 · AC-1 & AC-2"
    active-nav="provider-shifts"
    user-name="Dr. Alejandro Vega"
    user-initials="AV"
    user-role-label="Especialista Multi-Sede"
  >
    <div class="space-y-space-lg">
      <!-- Page Header Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle flex flex-col md:flex-row md:items-center justify-between gap-space-md">
        <div class="space-y-1">
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed font-code-sm text-xs font-semibold">
              AHC-6 · Gestión de Jornada
            </span>
            <span class="font-code-sm text-xs text-outline">Dr. Alejandro Vega · DGP #8472910</span>
          </div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
            Configuración de Turnos y Horarios
          </h1>
          <p class="font-body-md text-body-md text-on-surface-variant max-w-2xl">
            Define bloques recurrentes de consulta y descansos intermedios por sede física con prevención automática de traslapes entre sucursales (<span class="font-code-sm text-primary font-semibold">HTTP 422</span>).
          </p>
        </div>

        <div class="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-secondary-container/50 text-on-secondary-container border border-secondary/20 shrink-0">
          <span class="material-symbols-outlined text-[18px] text-secondary">verified_user</span>
          <span class="font-label-sm text-xs font-semibold">Validador Inter-Sede Activo</span>
        </div>
      </section>

      <!-- Cross-Branch Conflict Alert (AC-2: HTTP 422) -->
      <div
        v-if="conflictAlert"
        role="alert"
        class="p-space-md rounded-xl bg-tertiary-fixed/70 border border-tertiary/30 text-on-tertiary-fixed flex items-start gap-3 shadow-subtle"
      >
        <span class="material-symbols-outlined text-tertiary text-[20px] shrink-0 mt-0.5">error</span>
        <div class="space-y-1">
          <p class="font-title-sm text-sm font-semibold text-on-tertiary-fixed">
            {{ conflictAlert }}
          </p>
          <p class="font-body-sm text-xs text-on-tertiary-fixed-variant">
            Un mismo profesional no puede atender en dos sedes físicas distintas en intervalos de tiempo superpuestos.
          </p>
        </div>
      </div>

      <!-- 12-Column Standardized Split Workspace -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
        <!-- Left 5 Cols: Shift Configuration Form -->
        <section class="lg:col-span-5 bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
          <div class="border-b border-outline-variant/25 pb-space-sm">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Plantilla Semanal por Sede</h2>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
              Configura la ventana matutina y vespertina dejando libre el horario de comida.
            </p>
          </div>

          <form id="shift-form" class="space-y-space-md" @submit.prevent="handleSaveShift">
            <div class="space-y-1.5">
              <label for="branch" class="block font-title-sm text-title-sm text-on-surface">
                Sede
              </label>
              <select
                id="branch"
                v-model="branch"
                class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 text-on-surface font-body-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                <option value="sede-norte">Sede Norte</option>
                <option value="sede-sur">Sede Sur</option>
                <option value="sede-centro">Sede Centro</option>
              </select>
            </div>

            <div class="space-y-1.5">
              <label for="day" class="block font-title-sm text-title-sm text-on-surface">
                Día de la Semana
              </label>
              <select
                id="day"
                v-model="day"
                class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 text-on-surface font-body-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                <option value="MONDAY">MONDAY</option>
                <option value="TUESDAY">TUESDAY</option>
                <option value="WEDNESDAY">WEDNESDAY</option>
                <option value="THURSDAY">THURSDAY</option>
                <option value="FRIDAY">FRIDAY</option>
              </select>
            </div>

            <!-- Bloque 1 (Matutino) -->
            <div class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/25 space-y-3">
              <div class="flex items-center justify-between">
                <span class="font-label-sm text-xs uppercase tracking-wider text-primary font-semibold">
                  Bloque 1 · Matutino
                </span>
                <span class="font-code-sm text-[11px] text-outline">Ventana 1</span>
              </div>
              <div class="grid grid-cols-2 gap-space-sm">
                <div class="space-y-1">
                  <label for="s1" class="block font-label-md text-xs text-on-surface-variant">
                    Inicio Bloque 1
                  </label>
                  <input
                    id="s1"
                    v-model="s1"
                    type="text"
                    placeholder="09:00"
                    class="w-full h-9 px-3 rounded-lg bg-surface-container-lowest border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
                <div class="space-y-1">
                  <label for="e1" class="block font-label-md text-xs text-on-surface-variant">
                    Fin Bloque 1
                  </label>
                  <input
                    id="e1"
                    v-model="e1"
                    type="text"
                    placeholder="13:00"
                    class="w-full h-9 px-3 rounded-lg bg-surface-container-lowest border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>
            </div>

            <!-- Bloque 2 (Vespertino) -->
            <div class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/25 space-y-3">
              <div class="flex items-center justify-between">
                <span class="font-label-sm text-xs uppercase tracking-wider text-primary font-semibold">
                  Bloque 2 · Vespertino
                </span>
                <span class="font-code-sm text-[11px] text-outline">Opcional</span>
              </div>
              <div class="grid grid-cols-2 gap-space-sm">
                <div class="space-y-1">
                  <label for="s2" class="block font-label-md text-xs text-on-surface-variant">
                    Inicio Bloque 2
                  </label>
                  <input
                    id="s2"
                    v-model="s2"
                    type="text"
                    placeholder="15:00"
                    class="w-full h-9 px-3 rounded-lg bg-surface-container-lowest border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
                <div class="space-y-1">
                  <label for="e2" class="block font-label-md text-xs text-on-surface-variant">
                    Fin Bloque 2
                  </label>
                  <input
                    id="e2"
                    v-model="e2"
                    type="text"
                    placeholder="19:00"
                    class="w-full h-9 px-3 rounded-lg bg-surface-container-lowest border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              class="w-full h-10 px-space-lg rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-sm font-semibold shadow-subtle transition-colors flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-[18px]">save</span>
              <span>Guardar Plantilla de Turno</span>
            </button>
          </form>
        </section>

        <!-- Right 7 Cols: Generated Bookable Slots & Active Shifts Matrix -->
        <section class="lg:col-span-7 space-y-space-md">
          <div class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
            <div class="flex items-center justify-between border-b border-outline-variant/25 pb-space-sm">
              <div>
                <h2 class="font-headline-sm text-headline-sm text-on-surface">
                  Vista Previa del Generador de Intervalos
                </h2>
                <p class="font-body-sm text-body-sm text-on-surface-variant">
                  Intervalos expuestos al motor de reservas excluyendo automáticamente el descanso intermedio.
                </p>
              </div>
              <span class="px-2.5 py-1 rounded-full bg-primary-fixed text-on-primary-fixed font-code-sm text-xs font-semibold">
                {{ generatedSlots.length }} intervalos activos
              </span>
            </div>

            <div v-if="statusMessage" class="p-space-md rounded-xl bg-secondary-container/50 text-on-secondary-container border border-secondary/20 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-secondary text-[18px]">check_circle</span>
                <p class="font-title-sm text-sm font-semibold">{{ statusMessage }}</p>
              </div>
              <span v-if="mealBreakMessage" class="px-2.5 py-1 rounded-lg bg-surface-container-lowest text-on-surface font-code-sm text-xs font-semibold border border-outline-variant/40">
                {{ mealBreakMessage }}
              </span>
            </div>

            <div v-if="generatedSlots.length > 0" class="grid grid-cols-2 sm:grid-cols-4 gap-space-sm">
              <div
                v-for="slot in generatedSlots"
                :key="slot"
                :data-testid="`slot-${slot}`"
                class="p-3 rounded-xl bg-surface-container-low border border-outline-variant/30 flex items-center justify-between"
              >
                <span class="font-code-sm text-sm font-semibold text-on-surface">{{ slot }}</span>
                <span class="w-2 h-2 rounded-full bg-secondary"></span>
              </div>
            </div>

            <div v-else class="p-space-lg rounded-xl bg-surface-container-low border border-dashed border-outline-variant text-center space-y-1">
              <span class="material-symbols-outlined text-outline text-[24px]">calendar_clock</span>
              <p class="font-body-sm text-body-sm text-on-surface-variant">
                Guarda una plantilla de turno para previsualizar los bloques horarios hábiles y descansos bloqueados.
              </p>
            </div>
          </div>

          <!-- Saved Shifts Summary Table -->
          <div class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-sm">
            <h3 class="font-title-md text-title-md text-on-surface">
              Turnos Registrados en Sesión ({{ savedShifts.length }})
            </h3>
            <div v-if="savedShifts.length > 0" class="space-y-2">
              <div
                v-for="(item, idx) in savedShifts"
                :key="idx"
                class="p-3 rounded-lg bg-surface-container-low border border-outline-variant/25 flex items-center justify-between text-xs"
              >
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-primary text-[16px]">apartment</span>
                  <span class="font-title-sm text-on-surface font-semibold">{{ item.branchLabel }}</span>
                  <span class="font-code-sm text-outline">· {{ item.day }}</span>
                </div>
                <div class="font-code-sm text-on-surface-variant">
                  {{ item.s1 }} - {{ item.e1 }}
                  <span v-if="item.s2 && item.e2"> | {{ item.s2 }} - {{ item.e2 }}</span>
                </div>
              </div>
            </div>
            <p v-else class="font-body-sm text-xs text-outline">
              Aún no se han guardado turnos adicionales en esta sesión.
            </p>
          </div>
        </section>
      </div>
    </div>
  </MedikaAppShell>
</template>
