<script setup lang="ts">
import { ref } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'

useHead({
  title: 'Reserva en Línea y Prevención de Doble Reserva'
})

const holdStatusMessage = ref<string | null>(null)
const holdToken = ref<string | null>(null)
const jwtToken = ref('')
const confirmStatusMessage = ref<string | null>(null)
const redisLockReleasedMessage = ref<string | null>(null)
const conflictErrorAlert = ref<string | null>(null)

function acquireOptimisticHold() {
  conflictErrorAlert.value = null
  confirmStatusMessage.value = null
  redisLockReleasedMessage.value = null
  holdStatusMessage.value = 'Bloqueo temporal activo: lock:doctor:X:slot:1000 (TTL: 300s)'
  holdToken.value = 'hold_9f8e7d6c'
}

function simulateTtlExpiration() {
  holdToken.value = null
  confirmStatusMessage.value = null
  redisLockReleasedMessage.value = null
  holdStatusMessage.value = 'TTL expirado: Horario 10:00 AM disponible nuevamente'
}

function simulateConcurrentConflict() {
  confirmStatusMessage.value = null
  redisLockReleasedMessage.value = null
  conflictErrorAlert.value = '409 Conflict: El horario seleccionado acaba de ser reservado por otro paciente'
}

function confirmAppointment() {
  conflictErrorAlert.value = null
  confirmStatusMessage.value = '201 Created — Cita confirmada en estado SCHEDULED'
  redisLockReleasedMessage.value = 'Lock Redis liberado tras confirmación'
}
</script>

<template>
  <MedikaAppShell
    title="Agenda Pública — Doctor X"
    breadcrumb-section="Recepción y Pacientes"
    story-badge="Reserva en Línea"
    active-nav="optimistic-booking"
    user-name="Portal de Pacientes"
    user-initials="PX"
    user-role-label="Reserva Verificada"
  >
    <div class="space-y-space-lg">
      <!-- Page Header Card -->
      <section class="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle flex flex-col md:flex-row md:items-center justify-between gap-space-md">
        <div class="space-y-1">
          <div class="flex items-center gap-2">
            <span class="px-2.5 py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed font-code-sm text-xs font-semibold">
              Pre-Bloqueo Temporal 5 Minutos
            </span>
            <span class="font-code-sm text-xs text-outline">TTL: 300s · Máx. 2 Reservas Activas</span>
          </div>
          <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
            Agenda Pública — Doctor X
          </h1>
          <p class="font-body-md text-body-md text-on-surface-variant max-w-2xl">
            Pre-bloqueo temporal sin fricción por 5 minutos (<span class="font-code-sm text-primary font-semibold">lock:doctor:X:slot:1000</span>) y confirmación atómica autenticada mediante JWT/OTP + <span class="font-code-sm text-on-surface font-semibold">btree_gist</span>.
          </p>
        </div>

        <div class="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-secondary-container/50 text-on-secondary-container border border-secondary/20 shrink-0">
          <span class="material-symbols-outlined text-[18px] text-secondary">shield_lock</span>
          <span class="font-label-sm text-xs font-semibold">Turnstile + Redis Lock Activo</span>
        </div>
      </section>

      <!-- Concurrent Conflict Alert (409 Conflict) -->
      <div
        v-if="conflictErrorAlert"
        role="alert"
        class="p-space-md rounded-xl bg-tertiary-fixed/80 border border-tertiary/30 text-on-tertiary-fixed flex items-start gap-3 shadow-subtle"
      >
        <span class="material-symbols-outlined text-tertiary text-[20px] shrink-0 mt-0.5">gpp_bad</span>
        <div class="space-y-1">
          <p class="font-title-sm text-sm font-bold text-on-tertiary-fixed">
            {{ conflictErrorAlert }}
          </p>
          <p class="font-body-sm text-xs text-on-tertiary-fixed-variant">
            Garantía de exclusión temporal PostgreSQL <span class="font-code-sm">btree_gist (no_doctor_overlapping_slots)</span> ejecutada.
          </p>
        </div>
      </div>

      <!-- 12-Column Standardized Split Workspace -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
        <!-- Left 7 Cols: Step 1 - Unauthenticated Slot Hold & Concurrency Simulators -->
        <section class="lg:col-span-7 bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
          <div class="flex items-center justify-between border-b border-outline-variant/25 pb-space-sm">
            <div>
              <span class="font-label-sm text-xs uppercase tracking-wider text-primary font-semibold">
                Nivel 1 · Pre-Bloqueo Optimista (Sin Autenticación Previa)
              </span>
              <h2 class="font-headline-sm text-headline-sm text-on-surface mt-0.5">
                Selección de Horario y Control de TTL Redis
              </h2>
            </div>
            <span class="font-code-sm text-xs text-outline">Consultorio 301</span>
          </div>

          <div class="flex flex-wrap gap-space-sm">
            <button
              id="hold-btn"
              type="button"
              class="h-10 px-space-lg rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-sm font-semibold shadow-subtle transition-colors inline-flex items-center gap-2"
              @click="acquireOptimisticHold"
            >
              <span class="material-symbols-outlined text-[18px]">schedule</span>
              <span>Reservar 10:00 AM</span>
            </button>

            <button
              id="expire-btn"
              type="button"
              class="h-10 px-space-md rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-title-sm text-xs font-semibold border border-outline-variant/40 transition-colors inline-flex items-center gap-1.5"
              @click="simulateTtlExpiration"
            >
              <span class="material-symbols-outlined text-[17px] text-outline">timer_off</span>
              <span>Simular Expiración TTL (300s)</span>
            </button>

            <button
              id="conflict-btn"
              type="button"
              class="h-10 px-space-md rounded-lg bg-tertiary-fixed/70 hover:bg-tertiary-fixed text-on-tertiary-fixed font-title-sm text-xs font-semibold border border-tertiary/25 transition-colors inline-flex items-center gap-1.5"
              @click="simulateConcurrentConflict"
            >
              <span class="material-symbols-outlined text-[17px] text-tertiary">bolt</span>
              <span>Simular Colisión Concurrente (btree_gist)</span>
            </button>
          </div>

          <!-- Hold Status Panel -->
          <div id="hold-status" class="p-space-md rounded-xl bg-surface-container-low border border-outline-variant/30 space-y-2">
            <div class="flex items-center justify-between">
              <span class="font-label-sm text-xs uppercase tracking-wider text-outline font-semibold">
                Estado del Lock Distribuido en Redis
              </span>
              <span
                v-if="holdToken"
                data-testid="hold-token"
                class="px-2.5 py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed font-code-sm text-xs font-semibold"
              >
                {{ holdToken }}
              </span>
            </div>

            <p v-if="holdStatusMessage" class="font-title-sm text-sm font-semibold text-on-surface">
              {{ holdStatusMessage }}
            </p>
            <p v-else class="font-body-sm text-xs text-on-surface-variant">
              Selecciona "Reservar 10:00 AM" para adquirir un bloqueo temporal de 300 segundos en Redis.
            </p>
          </div>
        </section>

        <!-- Right 5 Cols: Step 2 - Authenticated Confirmation with Hold Ownership -->
        <section class="lg:col-span-5 bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-subtle space-y-space-md">
          <div class="border-b border-outline-variant/25 pb-space-sm">
            <span class="font-label-sm text-xs uppercase tracking-wider text-secondary font-semibold">
              Nivel 2 · Confirmación Autenticada
            </span>
            <h2 class="font-headline-sm text-headline-sm text-on-surface mt-0.5">
              Verificación OTP / JWT y Cierre Atómico
            </h2>
          </div>

          <form id="confirm-form" class="space-y-space-md" @submit.prevent="confirmAppointment">
            <div class="space-y-1.5">
              <label for="jwt" class="block font-title-sm text-title-sm text-on-surface">
                Código OTP / JWT
              </label>
              <input
                id="jwt"
                v-model="jwtToken"
                type="text"
                placeholder="jwt-verified-patient-token"
                class="w-full h-10 px-3 rounded-lg bg-surface-container-low border border-outline-variant/40 font-code-sm text-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
              />
            </div>

            <button
              type="submit"
              class="w-full h-10 px-space-lg rounded-lg bg-secondary hover:opacity-95 text-on-secondary font-title-sm text-sm font-semibold shadow-subtle transition-all flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-[18px]">verified</span>
              <span>Confirmar Cita Definitiva</span>
            </button>
          </form>

          <div
            v-if="confirmStatusMessage"
            id="confirm-status"
            class="p-space-md rounded-xl bg-secondary-container/60 border border-secondary/25 text-on-secondary-container space-y-1"
          >
            <p class="font-title-sm text-sm font-bold">{{ confirmStatusMessage }}</p>
            <p v-if="redisLockReleasedMessage" class="font-code-sm text-xs text-on-secondary-container">
              {{ redisLockReleasedMessage }}
            </p>
          </div>
        </section>
      </div>
    </div>
  </MedikaAppShell>
</template>
