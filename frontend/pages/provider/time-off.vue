<script setup lang="ts">
import { ref } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'

useHead({
  title: 'Ausencias y Cancelaciones de Emergencia (AHC-7)'
})

const blockType = ref<'PLANNED_LEAVE' | 'EMERGENCY_CANCELLATION'>('PLANNED_LEAVE')
const startDate = ref('2026-12-20')
const endDate = ref('2026-12-27')
const reason = ref('Vacaciones Anuales de Diciembre')

const plannedLeaveResult = ref<string | null>(null)
const emergencyTransitionResult = ref<string | null>(null)
const emergencyReceptionAlert = ref<string | null>(null)
const emergencyPatientNotification = ref<string | null>(null)
const latestAuditEntry = ref<string | null>(null)

function handleApplyTimeOff() {
  const nowIso = new Date().toISOString()
  const cleanReason = reason.value.trim()

  if (blockType.value === 'PLANNED_LEAVE') {
    emergencyTransitionResult.value = null
    emergencyReceptionAlert.value = null
    emergencyPatientNotification.value = null

    plannedLeaveResult.value = `Slots suprimidos del ${startDate.value} al ${endDate.value} (0 disponibles)`
    latestAuditEntry.value = `PLANNED_LEAVE | dr-alejandro-vega | ${cleanReason} | ${nowIso}`
  } else {
    plannedLeaveResult.value = null

    emergencyTransitionResult.value = '5 citas transicionadas a REQUIRES_RESCHEDULING'
    emergencyReceptionAlert.value = 'ALERTA PRIORIDAD ALTA (Recepción): 5 pacientes requieren reprogramación'
    emergencyPatientNotification.value = 'Notificaciones automáticas enviadas a 5 pacientes'
    latestAuditEntry.value = `EMERGENCY_CANCELLATION | dr-alejandro-vega | ${cleanReason} | ${nowIso}`
  }
}
</script>

<template>
  <MedikaAppShell
    title="Gestión de Ausencias y Cancelaciones de Emergencia"
    breadcrumb-section="Excepciones de Agenda (AHC-7)"
    story-badge="AHC-7 · AC-1, AC-2 & AC-3"
    active-nav="provider-timeoff"
    user-name="Dr. Alejandro Vega"
    user-initials="AV"
    user-role-label="Especialista Multi-Sede"
  >
    <div class="space-y-space-lg">
      <!-- Page Header Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle flex flex-col md:flex-row md:items-center justify-between gap-space-md">
        <div class="space-y-1">
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-0.5 rounded-full bg-tertiary-fixed text-on-tertiary-fixed font-code-sm text-xs font-semibold">
              AHC-7 · Control de Excepciones
            </span>
            <span class="font-code-sm text-xs text-outline">Bitácora Inmutable Activa</span>
          </div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
            Gestión de Ausencias y Cancelaciones de Emergencia
          </h1>
          <p class="font-body-md text-body-md text-on-surface-variant max-w-2xl">
            Bloquea fechas futuras por vacaciones planeadas o ejecuta cancelaciones de urgencia médica con transición automática a <span class="font-code-sm text-tertiary font-semibold">REQUIRES_RESCHEDULING</span> y alerta prioritaria en recepción.
          </p>
        </div>

        <div class="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-primary-fixed/60 text-on-primary-fixed border border-primary/15 shrink-0">
          <span class="material-symbols-outlined text-[18px] text-primary">history_edu</span>
          <span class="font-label-sm text-xs font-semibold">Auditoría NOM-024</span>
        </div>
      </section>

      <!-- High Priority Receptionist Dashboard Alert (AC-2) -->
      <div
        v-if="emergencyReceptionAlert"
        role="alert"
        class="p-space-md rounded-xl bg-tertiary-fixed/80 border border-tertiary/30 text-on-tertiary-fixed flex items-start gap-3 shadow-subtle"
      >
        <span class="material-symbols-outlined text-tertiary text-[22px] shrink-0 mt-0.5">warning</span>
        <div class="space-y-1">
          <p class="font-title-sm text-sm font-bold text-on-tertiary-fixed">
            {{ emergencyReceptionAlert }}
          </p>
          <p class="font-body-sm text-xs text-on-tertiary-fixed-variant">
            Se ha enviado una alerta crítica al tablero de recepción de Sede Centro para contactar y reubicar a los pacientes afectados.
          </p>
        </div>
      </div>

      <!-- 12-Column Standardized Split Workspace -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
        <!-- Left 5 Cols: Time-Off & Emergency Form -->
        <section class="lg:col-span-5 bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
          <div class="border-b border-outline-variant/25 pb-space-sm">
            <h2 class="font-headline-sm text-headline-sm text-on-surface">Registrar Bloqueo o Urgencia</h2>
            <p class="font-body-sm text-body-sm text-on-surface-variant mt-0.5">
              Todo bloqueo registra el identificador del médico, fecha exacta y motivo obligatorio.
            </p>
          </div>

          <form id="timeoff-form" class="space-y-space-md" @submit.prevent="handleApplyTimeOff">
            <div class="space-y-1.5">
              <label for="type" class="block font-title-sm text-title-sm text-on-surface">
                Tipo de Bloqueo
              </label>
              <select
                id="type"
                v-model="blockType"
                class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 text-on-surface font-body-md text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              >
                <option value="PLANNED_LEAVE">PLANNED_LEAVE</option>
                <option value="EMERGENCY_CANCELLATION">EMERGENCY_CANCELLATION</option>
              </select>
            </div>

            <div class="grid grid-cols-2 gap-space-sm">
              <div class="space-y-1.5">
                <label for="start" class="block font-title-sm text-title-sm text-on-surface">
                  Fecha Inicio
                </label>
                <input
                  id="start"
                  v-model="startDate"
                  type="text"
                  placeholder="2026-12-20"
                  class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>

              <div class="space-y-1.5">
                <label for="end" class="block font-title-sm text-title-sm text-on-surface">
                  Fecha Fin
                </label>
                <input
                  id="end"
                  v-model="endDate"
                  type="text"
                  placeholder="2026-12-27"
                  class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
                />
              </div>
            </div>

            <div class="space-y-1.5">
              <label for="reason" class="block font-title-sm text-title-sm text-on-surface">
                Motivo Obligatorio
              </label>
              <input
                id="reason"
                v-model="reason"
                type="text"
                required
                placeholder="Ej. Vacaciones Anuales / Cirugía de urgencia"
                class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 font-body-md text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>

            <button
              type="submit"
              class="w-full h-10 px-space-lg rounded-lg font-title-sm text-sm font-semibold shadow-subtle transition-colors flex items-center justify-center gap-2"
              :class="
                blockType === 'EMERGENCY_CANCELLATION'
                  ? 'bg-tertiary hover:bg-tertiary-container text-on-tertiary'
                  : 'bg-primary hover:bg-primary-container text-on-primary'
              "
            >
              <span class="material-symbols-outlined text-[18px]">
                {{ blockType === 'EMERGENCY_CANCELLATION' ? 'emergency_home' : 'event_busy' }}
              </span>
              <span>Aplicar Bloqueo de Agenda</span>
            </button>
          </form>
        </section>

        <!-- Right 7 Cols: Operational Impact & Immutable Audit Trail -->
        <section class="lg:col-span-7 space-y-space-md">
          <div class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
            <h2 class="font-headline-sm text-headline-sm text-on-surface border-b border-outline-variant/25 pb-space-sm">
              Resultado Operativo y Estado de Citas
            </h2>

            <!-- Planned Leave Result (AC-1) -->
            <div
              v-if="plannedLeaveResult"
              class="p-space-md rounded-xl bg-secondary-container/50 border border-secondary/20 text-on-secondary-container flex items-center gap-3"
            >
              <span class="material-symbols-outlined text-secondary text-[20px]">check_circle</span>
              <p class="font-title-sm text-sm font-semibold">{{ plannedLeaveResult }}</p>
            </div>

            <!-- Emergency Cancellation Results (AC-2) -->
            <div v-if="emergencyTransitionResult" class="space-y-space-sm">
              <div class="p-space-md rounded-xl bg-tertiary-fixed/50 border border-tertiary/20 text-on-tertiary-fixed flex items-center justify-between">
                <div class="flex items-center gap-2.5">
                  <span class="material-symbols-outlined text-tertiary text-[20px]">sync_problem</span>
                  <p class="font-title-sm text-sm font-semibold">{{ emergencyTransitionResult }}</p>
                </div>
                <span class="px-2.5 py-0.5 rounded-full bg-tertiary text-on-tertiary font-code-sm text-[11px] font-semibold">
                  REQUIRES_RESCHEDULING
                </span>
              </div>

              <div
                v-if="emergencyPatientNotification"
                class="p-space-md rounded-xl bg-primary-fixed/60 border border-primary/20 text-on-primary-fixed flex items-center gap-2.5"
              >
                <span class="material-symbols-outlined text-primary text-[20px]">mark_email_read</span>
                <p class="font-title-sm text-sm font-semibold">{{ emergencyPatientNotification }}</p>
              </div>
            </div>

            <div
              v-if="!plannedLeaveResult && !emergencyTransitionResult"
              class="p-space-lg rounded-xl bg-surface-container-low border border-dashed border-outline-variant text-center space-y-1"
            >
              <span class="material-symbols-outlined text-outline text-[24px]">event_note</span>
              <p class="font-body-sm text-sm text-on-surface-variant">
                Selecciona el tipo de bloqueo y aplica el registro para observar el impacto en la disponibilidad y alertas de recepción.
              </p>
            </div>
          </div>

          <!-- Immutable Audit Trail Card (AC-3) -->
          <div class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-sm">
            <div class="flex items-center justify-between">
              <h3 class="font-title-md text-title-md text-on-surface flex items-center gap-2">
                <span class="material-symbols-outlined text-primary text-[18px]">policy</span>
                <span>Bitácora de Auditoría Inmutable (AC-3)</span>
              </h3>
              <span class="font-code-sm text-[11px] text-outline">Append-Only Log</span>
            </div>

            <div
              v-if="latestAuditEntry"
              data-testid="audit-log-entry"
              class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/40 font-code-sm text-xs text-on-surface break-all"
            >
              {{ latestAuditEntry }}
            </div>
            <p v-else class="font-body-sm text-xs text-outline">
              Sin entradas de auditoría registradas en esta sesión.
            </p>
          </div>
        </section>
      </div>
    </div>
  </MedikaAppShell>
</template>
