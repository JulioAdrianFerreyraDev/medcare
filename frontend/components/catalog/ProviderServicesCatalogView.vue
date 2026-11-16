<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'
import { useCatalogApi } from '~/composables/useCatalogApi'
import { useTenancyStore } from '~/stores/tenancy'

const props = withDefaults(defineProps<{
  initialMode?: 'DOCTOR_CATALOG' | 'APPOINTMENT_BOOKING'
  initialBookingShell?: 'SIDEBAR' | 'PORTAL_HEADER'
}>(), {
  initialMode: 'DOCTOR_CATALOG',
  initialBookingShell: 'SIDEBAR'
})

const catalogApi = useCatalogApi()
const tenancyStore = useTenancyStore()
const PROVIDER_ID = 'dr-alejandro-vega'

const activeMode = ref<'DOCTOR_CATALOG' | 'APPOINTMENT_BOOKING'>(props.initialMode)
const bookingShell = ref<'SIDEBAR' | 'PORTAL_HEADER'>(props.initialBookingShell)

// =========================================================================
// DOCTOR CATALOG STATE (Stitch Screen: e10491f0e0514e399a2b7194afe98464)
// =========================================================================
interface ClinicalServiceItem {
  id: string
  codeId: string
  snisCode: string
  categoryTag: string
  categoryType: 'CONSULTA' | 'ESTUDIO'
  roomBadge: string
  roomIcon: string
  name: string
  description: string
  durationMinutes: number
  durationLabel: string
  priceMxn: number
  priceUnitLabel: string
  isActive: boolean
}

const catalogServices = ref<ClinicalServiceItem[]>([
  {
    id: '00000000-0000-0000-0000-000000000301',
    codeId: 'ID: SRV-CARD-01',
    snisCode: 'Clave SNIS: CARD-ECO-2D · Área: Gabinete Cardiológico',
    categoryTag: 'Procedimiento Especializado',
    categoryType: 'ESTUDIO',
    roomBadge: 'Consultorio 301 · Sede Centro',
    roomIcon: 'room',
    name: 'Ecocardiograma Transtorácico',
    description:
      'Estudio de gabinete cardíaco con registro doppler continuo, mapeo a color y emisión protocolizada de reporte hemodinámico inmediato.',
    durationMinutes: 60,
    durationLabel: '60 min',
    priceMxn: 1500,
    priceUnitLabel: 'MXN (Tarifa fija)',
    isActive: true
  },
  {
    id: '00000000-0000-0000-0000-000000000302',
    codeId: 'ID: SRV-CARD-02',
    snisCode: 'Clave SNIS: CARD-CON-1V · Área: Consulta Externa',
    categoryTag: 'Consulta Diagnóstica',
    categoryType: 'CONSULTA',
    roomBadge: 'Todas las Sedes',
    roomIcon: 'corporate_fare',
    name: 'Consulta de Primera Vez - Cardiología',
    description:
      'Evaluación integral cardiológica, anamnesis detallada de riesgo vascular, exploración y toma de electrocardiograma de 12 derivaciones basal.',
    durationMinutes: 45,
    durationLabel: '45 min',
    priceMxn: 800,
    priceUnitLabel: 'MXN / cita',
    isActive: true
  },
  {
    id: '00000000-0000-0000-0000-000000000303',
    codeId: 'ID: SRV-CARD-03',
    snisCode: 'Clave SNIS: CARD-SEG-01 · Área: Control Clínico',
    categoryTag: 'Control Rutinario',
    categoryType: 'CONSULTA',
    roomBadge: 'Pausa Médica',
    roomIcon: 'pause_circle',
    name: 'Consulta de Seguimiento / Control',
    description:
      'Revisión periódica de tratamiento antihipertensivo, análisis de laboratorio (perfil lipídico y enzimas) y monitoreo presencial de presión arterial.',
    durationMinutes: 20,
    durationLabel: '20 min',
    priceMxn: 500,
    priceUnitLabel: 'MXN / cita',
    isActive: false
  }
])

const catalogFilter = ref<'ALL' | 'CONSULTA' | 'ESTUDIO'>('ALL')
const catalogSearchQuery = ref('')
const editingServiceId = ref<string>('00000000-0000-0000-0000-000000000301')

onMounted(async () => {
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  try {
    const apiServices = await catalogApi.listProviderServices(tenantSlug, PROVIDER_ID, false)
    if (apiServices.length > 0) {
      apiServices.forEach((dto, index) => {
        const existing = catalogServices.value.find(
          (item) => item.id === dto.serviceId || item.name.toLowerCase() === dto.name.toLowerCase()
        )
        if (existing) {
          existing.id = dto.serviceId
          existing.name = dto.name
          existing.durationMinutes = dto.durationMinutes
          existing.durationLabel = `${dto.durationMinutes} min`
          existing.priceMxn = dto.feeAmount
          existing.isActive = dto.isActive
        } else {
          catalogServices.value.push({
            id: dto.serviceId,
            codeId: `ID: SRV-CARD-0${index + 1}`,
            snisCode: 'Clave SNIS: CARD-API · Área: Cardiología Clínica',
            categoryTag: dto.durationMinutes >= 60 ? 'Procedimiento Especializado' : 'Consulta Diagnóstica',
            categoryType: dto.durationMinutes >= 60 ? 'ESTUDIO' : 'CONSULTA',
            roomBadge: dto.isActive ? 'Consultorio 301 · Sede Centro' : 'Pausa Médica',
            roomIcon: dto.isActive ? 'room' : 'pause_circle',
            name: dto.name,
            description: dto.description,
            durationMinutes: dto.durationMinutes,
            durationLabel: `${dto.durationMinutes} min`,
            priceMxn: dto.feeAmount,
            priceUnitLabel: 'MXN / cita',
            isActive: dto.isActive
          })
        }
      })

      const first = catalogServices.value[0]
      if (first) {
        editingServiceId.value = first.id
        editorName.value = first.name
        editorDuration.value = first.durationMinutes
        editorPrice.value = first.priceMxn
      }
    }
  } catch {
    // Offline fallback uses pre-seeded local state
  }
})

const filteredCatalogServices = computed(() => {
  return catalogServices.value.filter((srv) => {
    const matchesTab = catalogFilter.value === 'ALL' || srv.categoryType === catalogFilter.value
    const matchesSearch =
      !catalogSearchQuery.value.trim() ||
      srv.name.toLowerCase().includes(catalogSearchQuery.value.toLowerCase()) ||
      srv.snisCode.toLowerCase().includes(catalogSearchQuery.value.toLowerCase())
    return matchesTab && matchesSearch
  })
})

const editingService = computed(() => {
  return catalogServices.value.find((s) => s.id === editingServiceId.value) ?? catalogServices.value[0]
})

const editorName = ref(catalogServices.value[0].name)
const editorDuration = ref<number>(catalogServices.value[0].durationMinutes)
const editorPrice = ref<number>(catalogServices.value[0].priceMxn)
const saveFeedbackMessage = ref<string | null>(null)

function selectServiceToEdit(srv: ClinicalServiceItem) {
  editingServiceId.value = srv.id
  editorName.value = srv.name
  editorDuration.value = srv.durationMinutes
  editorPrice.value = srv.priceMxn
}

async function handleToggleServiceActive(srv: ClinicalServiceItem) {
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  try {
    await catalogApi.updateProviderServiceStatus(tenantSlug, PROVIDER_ID, srv.id, srv.isActive)
    saveFeedbackMessage.value = srv.isActive
      ? `Servicio "${srv.name}" activado en el catálogo de reservas.`
      : `Servicio "${srv.name}" pausado (INACTIVE) preservando citas históricas.`
    setTimeout(() => {
      saveFeedbackMessage.value = null
    }, 3500)
  } catch {
    // Offline fallback
  }
}

function startNewServiceInEditor() {
  const newSrv: ClinicalServiceItem = {
    id: `srv-${Date.now()}`,
    codeId: `ID: SRV-CARD-0${catalogServices.value.length + 1}`,
    snisCode: 'Clave SNIS: CARD-NEW-01 · Área: Cardiología Clínica',
    categoryTag: 'Procedimiento Especializado',
    categoryType: 'ESTUDIO',
    roomBadge: 'Consultorio 301 · Sede Centro',
    roomIcon: 'room',
    name: 'Monitoreo Holter 24 Horas',
    description: 'Registro electrocardiográfico ambulatorio continuo de 24 horas con lectura digital.',
    durationMinutes: 45,
    durationLabel: '45 min',
    priceMxn: 1200,
    priceUnitLabel: 'MXN (Tarifa fija)',
    isActive: true
  }
  catalogServices.value.unshift(newSrv)
  selectServiceToEdit(newSrv)
}

async function handleSaveServiceChanges() {
  const target = editingService.value
  if (!target) return
  target.name = editorName.value
  target.durationMinutes = editorDuration.value
  target.durationLabel = `${editorDuration.value} min`
  target.priceMxn = Number(editorPrice.value)

  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  try {
    const saved = await catalogApi.upsertProviderService(tenantSlug, PROVIDER_ID, {
      serviceId: target.id,
      name: target.name,
      specialty: 'Cardiología',
      description: target.description,
      durationMinutes: target.durationMinutes,
      priceCents: Math.round(target.priceMxn * 100),
      feeAmount: target.priceMxn,
      currencyCode: 'MXN',
      isActive: target.isActive
    })
    target.id = saved.serviceId
    editingServiceId.value = saved.serviceId
  } catch {
    // Offline preview fallback
  }

  saveFeedbackMessage.value = `Tarifa actualizada: ${target.name} ($${target.priceMxn.toLocaleString('es-MX')} MXN · ${target.durationMinutes} min).`
  setTimeout(() => {
    saveFeedbackMessage.value = null
  }, 3500)
}

// =========================================================================
// APPOINTMENT BOOKING STATE (Stitch Screens: 8db41aeb265247ab893eefcdd0934fbf & 3209ede4a2644e96aa90c87800767466)
// =========================================================================
interface BookingServiceOption {
  id: number
  backendServiceUuid: string
  name: string
  duration: number
  price: number
  tag: string
  equipmentOrArea: string
  description: string
}

const bookingServices = ref<BookingServiceOption[]>([
  {
    id: 1,
    backendServiceUuid: '00000000-0000-0000-0000-000000000301',
    name: 'Ecocardiograma Transtorácico',
    duration: 60,
    price: 1500,
    tag: '60 min continuos',
    equipmentOrArea: 'Equipo Philips EPIQ 7',
    description:
      'Estudio de gabinete cardíaco con mapeo Doppler color, valoración de fracción de eyección y emisión de informe firmado inmediato.'
  },
  {
    id: 2,
    backendServiceUuid: '00000000-0000-0000-0000-000000000302',
    name: 'Consulta Primera Vez',
    duration: 45,
    price: 800,
    tag: '45 min',
    equipmentOrArea: 'Clínica médica',
    description:
      'Evaluación integral cardiológica, anamnesis clínica completa, auscultación de ruidos cardíacos y electrocardiograma basal de 12 derivaciones.'
  }
])

const selectedBookingServiceId = ref<number>(1)
const selectedBookingService = computed(() => {
  return bookingServices.value.find((s) => s.id === selectedBookingServiceId.value) ?? bookingServices.value[0]
})

const selectedSlotId = ref<'1100' | '1200' | '1600' | '1700'>('1100')
const slotLabels: Record<'1100' | '1200' | '1600' | '1700', string> = {
  '1100': '11:00 AM – 12:00 PM',
  '1200': '12:00 PM – 01:00 PM',
  '1600': '04:00 PM – 05:00 PM',
  '1700': '05:00 PM – 06:00 PM'
}

const slotIsoStartTimes: Record<'1100' | '1200' | '1600' | '1700', string> = {
  '1100': '2026-11-16T11:00:00Z',
  '1200': '2026-11-16T12:00:00Z',
  '1600': '2026-11-16T16:00:00Z',
  '1700': '2026-11-16T17:00:00Z'
}

const selectedTimeRange = computed(() => slotLabels[selectedSlotId.value])
const showBookingConfirmationToast = ref(false)
const bookingFeedbackDetail = ref<string | null>(null)

function selectBookingService(id: number) {
  selectedBookingServiceId.value = id
  showBookingConfirmationToast.value = false
  bookingFeedbackDetail.value = null
}

function selectSlot(slotId: '1100' | '1200' | '1600' | '1700') {
  selectedSlotId.value = slotId
  showBookingConfirmationToast.value = false
  bookingFeedbackDetail.value = null
}

async function confirmAppointment() {
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  const srv = selectedBookingService.value
  try {
    const appt = await catalogApi.bookServiceAppointment(tenantSlug, PROVIDER_ID, {
      serviceId: srv.backendServiceUuid,
      patientName: 'María Fernanda López',
      startTime: slotIsoStartTimes[selectedSlotId.value]
    })
    bookingFeedbackDetail.value = `Folio API #${appt.appointmentId.slice(0, 8)} (${appt.durationMinutes} min · $${appt.billedFee} MXN)`
  } catch (err: any) {
    const detail = err?.data?.detail || err?.response?._data?.detail
    if (detail) {
      bookingFeedbackDetail.value = detail
    }
  }
  showBookingConfirmationToast.value = true
}
const shellTitle = computed(() =>
  activeMode.value === 'DOCTOR_CATALOG'
    ? 'Mis Servicios Clínicos y Tarifas'
    : 'Agendar Cita por Duración de Servicio'
)

const shellNav = computed(() =>
  activeMode.value === 'DOCTOR_CATALOG' ? 'services' : 'appointments-book'
)
</script>

<template>
  <MedikaAppShell
    :title="shellTitle"
    breadcrumb-section="Catálogo Clínico y Agenda por Duración (AHC-5)"
    story-badge="AHC-5 · AC-1, AC-2 & AC-3"
    :active-nav="shellNav"
    user-name="Dr. Alejandro Vega"
    user-initials="AV"
    user-role-label="Cardiología · DGP #8472910"
  >
    <template #header-actions>
      <div class="flex items-center gap-2">
        <button
          type="button"
          class="h-8 px-3 rounded-lg font-title-sm text-xs transition-colors"
          :class="
            activeMode === 'DOCTOR_CATALOG'
              ? 'bg-primary text-on-primary font-semibold'
              : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
          "
          @click="activeMode = 'DOCTOR_CATALOG'"
        >
          Servicios y Tarifas
        </button>
        <button
          type="button"
          class="h-8 px-3 rounded-lg font-title-sm text-xs transition-colors"
          :class="
            activeMode === 'APPOINTMENT_BOOKING'
              ? 'bg-primary text-on-primary font-semibold'
              : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
          "
          @click="activeMode = 'APPOINTMENT_BOOKING'"
        >
          Agenda por Duración
        </button>
        <button
          v-if="activeMode === 'DOCTOR_CATALOG'"
          type="button"
          class="h-8 px-3 rounded-lg bg-primary text-on-primary font-title-sm text-xs flex items-center gap-1.5 hover:bg-primary-container shadow-subtle transition-all"
          @click="startNewServiceInEditor"
        >
          <span class="material-symbols-outlined text-[16px]">add</span>
          <span>Nuevo Servicio</span>
        </button>
      </div>
    </template>

    <!-- ========================================================================= -->
    <!-- MODE 1: MIS SERVICIOS Y TARIFAS (Stitch: e10491f0e0514e399a2b7194afe98464) -->
    <!-- ========================================================================= -->
    <div v-if="activeMode === 'DOCTOR_CATALOG'" class="w-full">
      <div class="flex flex-col w-full">
        <div class="w-full flex flex-col gap-space-lg">
          <!-- Doctor Workspace Header Section -->
          <header class="flex flex-col gap-space-sm bg-surface-container-lowest rounded-xl p-space-lg shadow-[0_1px_4px_rgba(15,23,42,0.05)] relative overflow-hidden">
              <div class="absolute -right-16 -top-16 w-56 h-56 rounded-full bg-primary-fixed/20 pointer-events-none blur-3xl"></div>

              <div class="flex flex-wrap items-center justify-between gap-y-2">
                <div class="flex items-center gap-space-xs">
                  <span class="font-code-sm text-code-sm text-primary font-semibold uppercase tracking-wider">ESPECIALIDAD MÉDICA &amp; CONSULTA</span>
                  <span class="material-symbols-outlined text-[14px] text-outline-variant">fiber_manual_record</span>
                  <span class="font-code-sm text-code-sm text-outline">DGP #8472910</span>
                </div>
                <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-secondary-container text-on-secondary-container">
                  <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                  <span class="font-label-sm text-label-sm font-semibold tracking-wide">Sincronización Activa con Recepción</span>
                </div>
              </div>

              <div class="flex flex-col md:flex-row md:items-end justify-between gap-space-md pt-1">
                <div class="flex flex-col gap-1 max-w-2xl">
                  <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">Mis Servicios y Tarifas</h1>
                  <p class="font-title-sm text-title-sm text-primary">
                    Dr. Alejandro Vega · <span class="text-on-surface-variant font-normal">Cardiología Clínica e Intervencionista</span>
                  </p>
                  <p class="font-body-md text-body-md text-on-surface-variant mt-1">
                    Configura la oferta clínica, aranceles transparentes y duración asignada para la sincronización automática de la agenda médica y cobro en mostrador.
                  </p>
                </div>

                <div class="flex items-center gap-space-xs self-start md:self-auto">
                  <button
                    type="button"
                    class="h-9 px-3 rounded bg-surface-container hover:bg-surface-container-high text-on-surface font-title-sm text-title-sm flex items-center gap-1.5 transition-colors"
                  >
                    <span class="material-symbols-outlined text-[18px]">history</span>
                    <span>Historial Arancelario</span>
                  </button>
                </div>
              </div>

              <!-- Quick Metrics Strip -->
              <div class="mt-space-sm pt-space-md bg-surface-container-low rounded-lg p-space-md grid grid-cols-1 sm:grid-cols-3 gap-space-md">
                <div class="flex items-center gap-space-sm">
                  <div class="w-8 h-8 rounded bg-primary-container/15 flex items-center justify-center text-primary">
                    <span class="material-symbols-outlined text-[20px]">assignment_turned_in</span>
                  </div>
                  <div>
                    <p class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Servicios en Agenda</p>
                    <p class="font-title-md text-title-md text-on-surface font-semibold">
                      {{ catalogServices.length }} definidos
                      <span class="font-label-sm text-label-sm text-secondary font-normal">
                        ({{ catalogServices.filter((s) => s.isActive).length }} activos)
                      </span>
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-space-sm">
                  <div class="w-8 h-8 rounded bg-primary-container/15 flex items-center justify-center text-primary">
                    <span class="material-symbols-outlined text-[20px]">schedule</span>
                  </div>
                  <div>
                    <p class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Duración Promedio</p>
                    <p class="font-title-md text-title-md text-on-surface font-semibold">
                      41.6 min <span class="font-label-sm text-label-sm text-outline font-normal">/ paciente</span>
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-space-sm">
                  <div class="w-8 h-8 rounded bg-secondary-container/30 flex items-center justify-center text-secondary">
                    <span class="material-symbols-outlined text-[20px]">payments</span>
                  </div>
                  <div>
                    <p class="font-label-sm text-label-sm text-outline uppercase tracking-wider">Arancel Medio Clínico</p>
                    <p class="font-title-md text-title-md text-on-surface font-semibold">
                      $933 MXN <span class="font-code-sm text-code-sm text-outline font-normal">NOM-024</span>
                    </p>
                  </div>
                </div>
              </div>
            </header>

            <!-- Save Feedback Banner -->
            <div
              v-if="saveFeedbackMessage"
              class="p-space-md rounded-xl bg-secondary-container text-on-secondary-container font-title-sm text-title-sm flex items-center justify-between shadow-sm"
            >
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-[20px]">check_circle</span>
                <span>{{ saveFeedbackMessage }}</span>
              </div>
              <button type="button" class="text-xs underline" @click="saveFeedbackMessage = null">Cerrar</button>
            </div>

            <!-- Main Workspace Split 7 / 5 Grid -->
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
              <!-- LEFT COLUMN: Catalog & Switch Controls (7 cols) -->
              <div class="lg:col-span-7 flex flex-col gap-space-md">
                <!-- Filter and Search Header -->
                <div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-space-sm bg-surface-container-lowest p-space-sm rounded-xl shadow-[0_1px_3px_rgba(15,23,42,0.04)]">
                  <div class="relative flex-1">
                    <span class="material-symbols-outlined absolute left-3 top-2.5 text-[18px] text-outline pointer-events-none">search</span>
                    <input
                      v-model="catalogSearchQuery"
                      type="text"
                      placeholder="Filtrar por nombre o clave CIE/CPT..."
                      class="w-full h-9 pl-9 pr-3 rounded bg-surface-container-low font-body-sm text-body-sm text-on-surface placeholder:text-outline focus:outline-none focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary shadow-inner"
                    />
                  </div>
                  <div class="flex items-center gap-1 bg-surface-container-low p-1 rounded-lg">
                    <button
                      type="button"
                      class="px-2.5 py-1 rounded transition-colors"
                      :class="
                        catalogFilter === 'ALL'
                          ? 'bg-surface-container-lowest shadow-[0_1px_2px_rgba(0,0,0,0.04)] text-on-surface font-title-sm text-title-sm'
                          : 'text-outline hover:text-on-surface font-label-md text-label-md'
                      "
                      @click="catalogFilter = 'ALL'"
                    >
                      Todos ({{ catalogServices.length }})
                    </button>
                    <button
                      type="button"
                      class="px-2.5 py-1 rounded transition-colors"
                      :class="
                        catalogFilter === 'CONSULTA'
                          ? 'bg-surface-container-lowest shadow-[0_1px_2px_rgba(0,0,0,0.04)] text-on-surface font-title-sm text-title-sm'
                          : 'text-outline hover:text-on-surface font-label-md text-label-md'
                      "
                      @click="catalogFilter = 'CONSULTA'"
                    >
                      Consultas (2)
                    </button>
                    <button
                      type="button"
                      class="px-2.5 py-1 rounded transition-colors"
                      :class="
                        catalogFilter === 'ESTUDIO'
                          ? 'bg-surface-container-lowest shadow-[0_1px_2px_rgba(0,0,0,0.04)] text-on-surface font-title-sm text-title-sm'
                          : 'text-outline hover:text-on-surface font-label-md text-label-md'
                      "
                      @click="catalogFilter = 'ESTUDIO'"
                    >
                      Estudios (1)
                    </button>
                  </div>
                </div>

                <!-- Service Cards -->
                <div
                  v-for="srv in filteredCatalogServices"
                  :key="srv.id"
                  class="flex flex-col rounded-xl p-space-lg relative overflow-hidden transition-all"
                  :class="[
                    editingServiceId === srv.id
                      ? 'bg-surface-container-lowest shadow-[0_2px_6px_rgba(15,23,42,0.05)] hover:shadow-[0_4px_12px_rgba(15,23,42,0.08)]'
                      : srv.isActive
                        ? 'bg-surface-container-lowest shadow-[0_1px_4px_rgba(15,23,42,0.04)] hover:shadow-[0_3px_8px_rgba(15,23,42,0.07)]'
                        : 'bg-surface-container-lowest/80 shadow-[0_1px_3px_rgba(15,23,42,0.03)] opacity-85 hover:opacity-100'
                  ]"
                >
                  <div v-if="editingServiceId === srv.id" class="absolute left-0 top-0 bottom-0 w-1 bg-primary"></div>

                  <div class="flex items-start justify-between gap-space-sm mb-space-xs">
                    <div class="flex flex-wrap items-center gap-1.5">
                      <span
                        class="px-2 py-0.5 rounded font-label-sm text-label-sm font-semibold"
                        :class="
                          editingServiceId === srv.id
                            ? 'bg-primary-fixed text-on-primary-fixed'
                            : srv.isActive
                              ? 'bg-surface-container text-on-surface-variant'
                              : 'bg-surface-container text-outline'
                        "
                      >
                        {{ srv.categoryTag }}
                      </span>
                      <span
                        class="px-2 py-0.5 rounded font-code-sm text-code-sm bg-surface-container flex items-center gap-1"
                        :class="srv.isActive ? 'text-on-surface-variant' : 'text-outline'"
                      >
                        <span class="material-symbols-outlined text-[13px] text-outline">schedule</span>
                        {{ srv.durationLabel }}
                      </span>
                      <span
                        v-if="!srv.isActive"
                        class="px-2 py-0.5 rounded font-label-sm text-label-sm bg-tertiary-fixed text-on-tertiary-fixed font-semibold"
                      >
                        Pausa Médica
                      </span>
                      <span
                        v-else
                        class="px-2 py-0.5 rounded font-label-sm text-label-sm bg-surface-container-low text-outline flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined text-[13px] text-outline">{{ srv.roomIcon }}</span>
                        {{ srv.roomBadge }}
                      </span>
                    </div>

                    <button
                      type="button"
                      class="p-1 rounded text-outline hover:text-on-surface hover:bg-surface-container transition-colors"
                      title="Ver opciones"
                    >
                      <span class="material-symbols-outlined text-[18px]">more_vert</span>
                    </button>
                  </div>

                  <div class="flex flex-col sm:flex-row sm:items-baseline justify-between gap-space-xs my-1">
                    <h3
                      class="font-headline-sm text-headline-sm font-semibold"
                      :class="srv.isActive ? 'text-on-surface' : 'text-on-surface/80'"
                    >
                      {{ srv.name }}
                    </h3>
                    <div class="flex items-baseline gap-1 text-right">
                      <span
                        class="leading-none font-bold"
                        :class="
                          editingServiceId === srv.id
                            ? 'font-display-lg text-display-lg text-primary'
                            : srv.isActive
                              ? 'font-headline-lg text-headline-lg text-on-surface'
                              : 'font-headline-lg text-headline-lg text-outline font-semibold'
                        "
                      >
                        ${{ srv.priceMxn.toLocaleString('es-MX') }}
                      </span>
                      <span class="font-label-sm text-label-sm text-outline font-semibold">{{ srv.priceUnitLabel }}</span>
                    </div>
                  </div>

                  <p
                    class="font-body-md text-body-md mt-1 mb-space-md"
                    :class="srv.isActive ? 'text-on-surface-variant' : 'text-outline'"
                  >
                    {{ srv.description }}
                  </p>

                  <div class="pt-space-sm bg-surface-container-low/60 -mx-space-lg -mb-space-lg px-space-lg pb-space-md rounded-b-xl flex items-center justify-between gap-space-sm">
                    <div class="flex items-center gap-space-sm">
                      <label class="relative inline-flex items-center cursor-pointer select-none">
                        <input
                          v-model="srv.isActive"
                          type="checkbox"
                          class="sr-only peer"
                          @change="handleToggleServiceActive(srv)"
                        />
                        <div class="w-10 h-5 bg-surface-variant peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-surface-variant after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-secondary"></div>
                        <span
                          v-if="srv.isActive"
                          class="ml-2 font-label-md text-label-md text-secondary font-semibold flex items-center gap-1"
                        >
                          <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                          Activo en agenda
                        </span>
                        <span v-else class="ml-2 font-label-md text-label-md text-outline font-medium">
                          Pausado temporalmente
                        </span>
                      </label>
                    </div>

                    <div class="flex items-center gap-space-xs">
                      <button
                        type="button"
                        class="h-8 px-3 rounded font-title-sm text-title-sm transition-colors flex items-center gap-1"
                        :class="
                          editingServiceId === srv.id
                            ? 'bg-primary-container text-on-primary-container hover:bg-primary shadow-sm'
                            : 'bg-surface-container hover:bg-surface-container-high text-on-surface'
                        "
                        @click="selectServiceToEdit(srv)"
                      >
                        <span class="material-symbols-outlined text-[16px]">
                          {{ editingServiceId === srv.id ? 'edit' : 'tune' }}
                        </span>
                        <span>{{ editingServiceId === srv.id ? 'Configurando' : 'Modificar' }}</span>
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Overlap Prevention Informational Callout Box -->
                <div class="flex items-start gap-space-sm p-space-md bg-surface-container-low rounded-xl text-on-surface shadow-sm">
                  <div class="w-8 h-8 rounded-lg bg-surface-container-highest flex items-center justify-center text-primary flex-shrink-0 mt-0.5">
                    <span class="material-symbols-outlined text-[20px]">lock_clock</span>
                  </div>
                  <div class="flex flex-col gap-0.5">
                    <h4 class="font-title-sm text-title-sm text-on-surface font-semibold">Prevención de Solapamiento Clínico</h4>
                    <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                      Los tiempos de consulta definidos aquí reservan automáticamente el bloque continuo correspondiente en la agenda multi-sede del doctor, bloqueando turnos adyacentes para transcripción de expediente y desinfección de gabinete.
                    </p>
                  </div>
                </div>
              </div>

              <!-- RIGHT COLUMN: Sticky Editing Pane & Governance (5 cols) -->
              <div class="lg:col-span-5 flex flex-col gap-space-md sticky top-20">
                <!-- Form Card -->
                <div class="flex flex-col bg-surface-container-lowest rounded-xl p-space-lg shadow-[0_2px_8px_rgba(15,23,42,0.06)]">
                  <div class="flex items-center justify-between pb-space-sm">
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded bg-primary-fixed flex items-center justify-center text-on-primary-fixed">
                        <span class="material-symbols-outlined text-[18px]">edit_note</span>
                      </div>
                      <h2 class="font-headline-sm text-headline-sm text-on-surface">Configurar Servicio</h2>
                    </div>
                    <span class="font-code-sm text-code-sm px-2 py-0.5 rounded bg-surface-container-high text-on-surface-variant">
                      {{ editingService?.codeId }}
                    </span>
                  </div>

                  <p class="font-body-sm text-body-sm text-on-surface-variant mt-1 mb-space-md">
                    Actualiza los parámetros clínicos y tarifa que se sincronizarán en tiempo real con recepción, app móvil de pacientes y facturación.
                  </p>

                  <form class="flex flex-col gap-space-md" @submit.prevent="handleSaveServiceChanges">
                    <!-- Field 1: Service Name -->
                    <div class="flex flex-col gap-1.5">
                      <label class="font-title-sm text-title-sm text-on-surface flex items-center justify-between">
                        <span>Nombre del Servicio Clínico <span class="text-tertiary">*</span></span>
                        <span class="font-label-sm text-label-sm text-outline">Catálogo Institucional</span>
                      </label>
                      <div class="relative">
                        <input
                          v-model="editorName"
                          type="text"
                          class="w-full h-10 px-3 rounded bg-surface-container-low font-body-md text-body-md text-on-surface focus:outline-none focus:bg-surface-container-lowest focus:ring-1 focus:ring-primary shadow-inner"
                        />
                      </div>
                      <span class="font-code-sm text-code-sm text-outline">{{ editingService?.snisCode }}</span>
                    </div>

                    <!-- Field 2: Appointment Duration Pills -->
                    <div class="flex flex-col gap-1.5">
                      <div class="flex items-center justify-between">
                        <label class="font-title-sm text-title-sm text-on-surface">
                          Duración de Cita (Bloque continuo) <span class="text-tertiary">*</span>
                        </label>
                        <span class="font-label-sm text-label-sm text-secondary font-semibold flex items-center gap-1">
                          <span class="material-symbols-outlined text-[14px]">bolt</span> Auto-reserva
                        </span>
                      </div>

                      <div class="grid grid-cols-3 gap-2">
                        <!-- Pill 1: 20 min -->
                        <button
                          type="button"
                          class="flex flex-col items-center justify-center p-2 rounded-lg transition-all relative"
                          :class="
                            editorDuration === 20
                              ? 'bg-primary-container text-on-primary-container shadow-sm'
                              : 'bg-surface-container-low hover:bg-surface-container text-on-surface'
                          "
                          @click="editorDuration = 20"
                        >
                          <span
                            v-if="editorDuration === 20"
                            class="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-primary text-on-primary flex items-center justify-center text-[10px]"
                          >
                            <span class="material-symbols-outlined text-[12px]">check</span>
                          </span>
                          <span class="font-title-sm text-title-sm font-semibold">20 min</span>
                          <span
                            class="font-label-sm text-label-sm mt-0.5"
                            :class="editorDuration === 20 ? 'text-on-primary-container/80' : 'text-outline'"
                          >
                            Control rápido
                          </span>
                        </button>

                        <!-- Pill 2: 45 min -->
                        <button
                          type="button"
                          class="flex flex-col items-center justify-center p-2 rounded-lg transition-all relative"
                          :class="
                            editorDuration === 45
                              ? 'bg-primary-container text-on-primary-container shadow-sm'
                              : 'bg-surface-container-low hover:bg-surface-container text-on-surface'
                          "
                          @click="editorDuration = 45"
                        >
                          <span
                            v-if="editorDuration === 45"
                            class="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-primary text-on-primary flex items-center justify-center text-[10px]"
                          >
                            <span class="material-symbols-outlined text-[12px]">check</span>
                          </span>
                          <span class="font-title-sm text-title-sm font-semibold">45 min</span>
                          <span
                            class="font-label-sm text-label-sm mt-0.5"
                            :class="editorDuration === 45 ? 'text-on-primary-container/80' : 'text-outline'"
                          >
                            Consulta base
                          </span>
                        </button>

                        <!-- Pill 3: 60 min -->
                        <button
                          type="button"
                          class="flex flex-col items-center justify-center p-2 rounded-lg transition-all relative"
                          :class="
                            editorDuration === 60
                              ? 'bg-primary-container text-on-primary-container shadow-sm'
                              : 'bg-surface-container-low hover:bg-surface-container text-on-surface'
                          "
                          @click="editorDuration = 60"
                        >
                          <span
                            v-if="editorDuration === 60"
                            class="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-primary text-on-primary flex items-center justify-center text-[10px]"
                          >
                            <span class="material-symbols-outlined text-[12px]">check</span>
                          </span>
                          <span class="font-title-sm text-title-sm font-semibold">60 min</span>
                          <span
                            class="font-label-sm text-label-sm mt-0.5"
                            :class="editorDuration === 60 ? 'text-on-primary-container/80' : 'text-outline'"
                          >
                            Procedimiento
                          </span>
                        </button>
                      </div>
                    </div>

                    <!-- Field 3: Currency Input -->
                    <div class="flex flex-col gap-1.5">
                      <label class="font-title-sm text-title-sm text-on-surface flex items-center justify-between">
                        <span>Precio Arancelario en MXN <span class="text-tertiary">*</span></span>
                        <span class="font-label-sm text-label-sm text-secondary font-semibold">Sin comisiones agregadas</span>
                      </label>
                      <div class="flex items-center rounded bg-surface-container-low focus-within:bg-surface-container-lowest focus-within:ring-1 focus-within:ring-primary shadow-inner overflow-hidden">
                        <span class="px-3 text-outline font-headline-sm font-bold bg-surface-container-high/60 h-10 flex items-center select-none">$</span>
                        <input
                          v-model.number="editorPrice"
                          type="number"
                          step="50"
                          class="w-full h-10 px-3 bg-transparent font-headline-sm text-headline-sm text-on-surface font-semibold focus:outline-none"
                        />
                        <span class="px-3 font-code-sm text-code-sm text-outline select-none font-semibold">MXN</span>
                      </div>
                      <p class="font-body-sm text-body-sm text-outline">Incluye material descartable, papel térmico e informe impreso.</p>
                    </div>

                    <!-- Contextual Notification -->
                    <div class="p-space-sm bg-surface-container-high/40 rounded-lg flex items-start gap-2">
                      <span class="material-symbols-outlined text-[16px] text-primary flex-shrink-0 mt-0.5">info</span>
                      <p class="font-body-sm text-body-sm text-on-surface-variant">
                        Los cambios aplican exclusivamente a nuevas citas agendadas; citas previas ya confirmadas conservan su tarifa y duración originales.
                      </p>
                    </div>

                    <!-- Action Buttons -->
                    <div class="flex items-center justify-end gap-space-sm pt-space-xs">
                      <button
                        type="button"
                        class="h-9 px-space-md rounded bg-surface-container-low text-on-surface font-title-sm text-title-sm hover:bg-surface-container transition-colors"
                        @click="selectServiceToEdit(editingService)"
                      >
                        Cancelar
                      </button>
                      <button
                        type="submit"
                        class="h-9 px-space-lg rounded bg-primary text-on-primary font-title-sm text-title-sm hover:bg-primary-container shadow-sm flex items-center gap-1.5 transition-all"
                      >
                        <span class="material-symbols-outlined text-[18px]">save</span>
                        <span>Guardar Cambios de Tarifa</span>
                      </button>
                    </div>
                  </form>
                </div>

                <!-- Governance & Ethical Compliance Card -->
                <div class="flex flex-col bg-surface-container-low rounded-xl p-space-md shadow-sm">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="material-symbols-outlined text-[18px] text-secondary">verified_user</span>
                    <h3 class="font-title-sm text-title-sm text-on-surface font-semibold">Transparencia Arancelaria &amp; Cumplimiento</h3>
                  </div>
                  <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                    Alineado con el Código de Ética Médica y el marco de expediente clínico digital
                    <span class="font-code-sm font-semibold text-on-surface">NOM-024-SSA3-2012</span>.
                    Las tarifas configuradas aquí se muestran desglosadas sin cargos ocultos en el portal del paciente y se envían de forma fidedigna al módulo de facturación CFDI 4.0.
                  </p>
                  <div class="mt-space-sm pt-space-xs flex items-center justify-between text-outline">
                    <span class="font-code-sm text-code-sm">Hash de auditoría: 8f92b7c</span>
                    <a class="font-label-sm text-label-sm text-primary hover:underline flex items-center gap-0.5" href="#">
                      <span>Normativa COFEPRIS</span>
                      <span class="material-symbols-outlined text-[13px]">open_in_new</span>
                    </a>
                  </div>
                </div>
              </div>
            </div>

            <!-- Verification / Audit System Footer Strip -->
            <div class="w-full bg-surface-container-lowest rounded-xl p-space-md shadow-[0_1px_3px_rgba(15,23,42,0.04)] flex flex-col sm:flex-row items-center justify-between gap-space-sm mt-space-sm">
              <div class="flex items-center gap-space-sm">
                <div class="w-2 h-2 rounded-full bg-secondary"></div>
                <span class="font-code-sm text-code-sm text-outline">Módulo Clínico Activo: Servicios, Catálogos y Bloques de Cita</span>
              </div>
              <div class="flex items-center gap-space-md">
                <span class="font-label-sm text-label-sm text-outline">Última actualización por Dr. Alejandro Vega: Hoy, 09:24 AM</span>
                <button
                  type="button"
                  class="text-primary hover:text-primary-container font-label-sm text-label-sm font-semibold flex items-center gap-1 transition-colors"
                >
                  <span class="material-symbols-outlined text-[14px]">refresh</span> Sincronizar Inmediato
                </button>
              </div>
            </div>
        </div>
      </div>
    </div>

    <!-- ========================================================================= -->
    <!-- MODE 2: AGENDAR CITA POR DURACIÓN DE SERVICIO                             -->
    <!-- ========================================================================= -->
    <div v-else class="w-full">
      <div class="w-full flex flex-col gap-space-lg">
          <!-- Context Ribbon & Doctor Bio Summary -->
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm border border-slate-200/60">
            <div class="flex items-start md:items-center gap-space-md">
              <div class="relative">
                <div class="w-14 h-14 rounded-full bg-primary-fixed text-primary font-headline-sm flex items-center justify-center shadow-sm ring-1 ring-slate-200">
                  AV
                </div>
                <span class="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full bg-secondary ring-2 ring-surface-container-lowest"></span>
              </div>
              <div class="flex flex-col">
                <div class="flex items-center gap-space-xs flex-wrap">
                  <h1 class="font-headline-sm text-headline-sm text-on-surface">Dr. Alejandro Vega</h1>
                  <span class="px-space-xs py-0.5 rounded-lg bg-surface-container-high text-on-primary-fixed-variant font-code-sm text-code-sm">
                    CÉD. 7482910
                  </span>
                </div>
                <p class="font-body-md text-body-md text-on-surface-variant">
                  Cardiología Clínica e Intervencionista · Consultorio 301
                </p>
                <div class="flex items-center gap-space-xs mt-1 text-on-surface-variant">
                  <span class="material-symbols-outlined text-[15px] text-primary">apartment</span>
                  <span class="font-label-sm text-label-sm">Clínica Irma · Sede Centro</span>
                  <span class="text-outline text-body-sm">·</span>
                  <span class="font-label-sm text-label-sm text-secondary flex items-center gap-1">
                    <span class="w-1.5 h-1.5 rounded-full bg-secondary inline-block"></span>
                    Agenda sincronizada en tiempo real
                  </span>
                </div>
              </div>
            </div>

            <div class="flex flex-col md:items-end justify-center bg-surface-container-low/70 px-space-md py-space-sm rounded-lg border border-slate-200/50">
              <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Flujo de Cita</span>
              <span class="font-title-sm text-title-sm text-primary flex items-center gap-1 mt-0.5">
                <span class="material-symbols-outlined text-[16px]">hourglass_top</span>
                Reserva por Bloque Continuo
              </span>
            </div>
          </div>

          <!-- 2-Column Responsive Cal.com-style Workflow Grid -->
          <div class="grid grid-cols-1 lg:grid-cols-12 gap-space-lg items-start">
            <!-- Left Column: Paso 1: Servicios Clínicos -->
            <section class="lg:col-span-5 flex flex-col gap-space-md">
              <div class="flex flex-col">
                <div class="flex items-center gap-space-xs text-primary font-label-md text-label-md uppercase tracking-wider">
                  <span>Paso 1</span>
                  <span>·</span>
                  <span>Duración y Aranceles</span>
                </div>
                <h2 class="font-headline-sm text-headline-sm text-on-surface mt-0.5">Selecciona el Servicio</h2>
                <p class="font-body-sm text-body-sm text-on-surface-variant mt-1">
                  El horario del consultorio se adaptará automáticamente a la duración del estudio.
                </p>
              </div>

              <div class="flex flex-col gap-space-sm">
                <div
                  v-for="item in bookingServices"
                  :key="item.id"
                  class="cursor-pointer p-space-md rounded-xl transition-all relative overflow-hidden group border"
                  :class="
                    selectedBookingServiceId === item.id
                      ? 'bg-surface-container-lowest shadow-md border-primary/20'
                      : 'bg-surface-container-lowest/80 shadow-sm border-slate-200/60 hover:bg-surface-container-lowest'
                  "
                  @click="selectBookingService(item.id)"
                >
                  <div
                    class="absolute left-0 top-0 bottom-0 w-1"
                    :class="selectedBookingServiceId === item.id ? 'bg-primary' : 'bg-transparent group-hover:bg-outline-variant'"
                  ></div>
                  <div class="flex items-start justify-between gap-space-sm">
                    <div class="flex items-center gap-space-sm">
                      <div
                        class="w-5 h-5 rounded-full flex items-center justify-center"
                        :class="
                          selectedBookingServiceId === item.id
                            ? 'bg-primary text-on-primary'
                            : 'bg-surface-container-high text-transparent group-hover:text-outline'
                        "
                      >
                        <span class="material-symbols-outlined text-[14px]">check</span>
                      </div>
                      <div>
                        <h3 class="font-title-md text-title-md text-on-surface font-semibold">{{ item.name }}</h3>
                        <div class="flex items-center gap-space-xs mt-1">
                          <span
                            class="px-space-xs py-0.5 rounded-lg font-code-sm text-code-sm"
                            :class="
                              selectedBookingServiceId === item.id
                                ? 'bg-surface-container-high text-primary font-semibold'
                                : 'bg-surface-container text-on-surface-variant'
                            "
                          >
                            {{ item.tag }}
                          </span>
                          <span
                            class="font-label-sm text-label-sm"
                            :class="selectedBookingServiceId === item.id ? 'text-secondary font-medium' : 'text-outline'"
                          >
                            {{ item.equipmentOrArea }}
                          </span>
                        </div>
                      </div>
                    </div>

                    <div class="text-right">
                      <span
                        class="font-headline-sm text-headline-sm"
                        :class="selectedBookingServiceId === item.id ? 'text-primary font-bold' : 'text-on-surface font-semibold'"
                      >
                        ${{ item.price.toLocaleString('es-MX') }}
                      </span>
                      <span class="block font-label-sm text-label-sm text-outline">MXN</span>
                    </div>
                  </div>

                  <p class="font-body-sm text-body-sm text-on-surface-variant mt-space-sm pl-7">
                    {{ item.description }}
                  </p>
                </div>
              </div>

              <!-- Technical Informative Safeguard -->
              <div class="bg-surface-container-low p-space-md rounded-xl flex items-start gap-space-sm text-on-surface-variant border border-slate-200/60">
                <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">verified_user</span>
                <div class="flex flex-col gap-0.5">
                  <span class="font-title-sm text-title-sm text-on-surface font-semibold">Protección de Bloque Indivisible</span>
                  <p class="font-body-sm text-body-sm">
                    La duración seleccionada reserva un bloque continuo en la agenda del especialista para garantizar esterilización y preparación del transductor sin retrasos acumulados.
                  </p>
                </div>
              </div>
            </section>

            <!-- Right Column: Paso 2: Calendario y Horarios Continuos -->
            <section class="lg:col-span-7 flex flex-col gap-space-md">
              <!-- Header & Day Navigator -->
              <div class="bg-surface-container-lowest p-space-md rounded-xl shadow-sm border border-slate-200/60 flex flex-col sm:flex-row sm:items-center justify-between gap-space-sm">
                <div class="flex flex-col">
                  <span class="font-label-sm text-label-sm text-primary uppercase tracking-wider font-semibold">Paso 2 · Selección de Horario</span>
                  <div class="flex items-center gap-space-xs mt-0.5">
                    <span class="material-symbols-outlined text-primary text-[20px]">calendar_month</span>
                    <h2 class="font-title-md text-title-md text-on-surface font-bold">Jueves, 14 de Noviembre de 2024</h2>
                  </div>
                </div>

                <div class="flex items-center gap-space-xs">
                  <span class="px-space-xs py-1 rounded bg-surface-container text-on-surface-variant font-code-sm text-code-sm flex items-center gap-1 border border-slate-200/40">
                    <span class="material-symbols-outlined text-[13px]">schedule</span>
                    CDMX (GMT-6)
                  </span>
                  <div class="flex items-center gap-1">
                    <button
                      type="button"
                      class="w-8 h-8 rounded-lg bg-surface-container-low hover:bg-surface-container flex items-center justify-center text-on-surface-variant transition-colors border border-slate-200/60"
                      title="Día anterior"
                    >
                      <span class="material-symbols-outlined text-[18px]">chevron_left</span>
                    </button>
                    <button
                      type="button"
                      class="w-8 h-8 rounded-lg bg-surface-container-low hover:bg-surface-container flex items-center justify-center text-on-surface-variant transition-colors border border-slate-200/60"
                      title="Día siguiente"
                    >
                      <span class="material-symbols-outlined text-[18px]">chevron_right</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Continuous Slot List -->
              <div class="bg-surface-container-lowest p-space-lg rounded-xl shadow-sm border border-slate-200/60 flex flex-col gap-space-md">
                <div class="flex items-center justify-between pb-space-xs">
                  <span class="font-label-md text-label-md text-on-surface-variant font-medium flex items-center gap-1.5">
                    <span class="w-2 h-2 rounded-full bg-secondary"></span>
                    Bloques Continuos Disponibles (Mínimo {{ selectedBookingService.duration }} min requeridos)
                  </span>
                  <span class="font-code-sm text-code-sm text-outline">Sala Gabinete A</span>
                </div>

                <div class="flex flex-col gap-space-sm">
                  <!-- Disabled Slot: Collision/Occupied -->
                  <div class="bg-surface-container-low/50 p-space-md rounded-xl opacity-75 cursor-not-allowed border border-slate-200/60">
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <div class="flex items-center gap-space-sm">
                        <div class="w-7 h-7 rounded-lg bg-surface-container-high flex items-center justify-center text-outline">
                          <span class="material-symbols-outlined text-[18px]">block</span>
                        </div>
                        <div>
                          <span class="font-title-sm text-title-sm text-outline line-through">10:00 AM – 11:00 AM</span>
                          <span class="block font-body-sm text-body-sm text-tertiary">Reservado · Bloque ocupado</span>
                        </div>
                      </div>
                      <span class="px-space-xs py-1 rounded bg-tertiary-fixed text-on-tertiary-fixed font-label-sm text-label-sm font-semibold">
                        No Disponible
                      </span>
                    </div>
                    <div class="mt-2 text-on-surface-variant bg-surface-container-lowest/60 p-space-xs rounded font-body-sm text-body-sm text-outline flex items-center gap-1">
                      <span class="material-symbols-outlined text-[14px]">info</span>
                      Intervalos fraccionados (ej. 10:20 AM o 10:30 AM) inhabilitados por regla de estudio continuo.
                    </div>
                  </div>

                  <!-- Slot 11:00 AM - 12:00 PM -->
                  <div
                    class="p-space-md rounded-xl transition-all cursor-pointer border"
                    :class="
                      selectedSlotId === '1100'
                        ? 'bg-surface-container-high shadow-sm border-primary/20'
                        : 'group bg-surface-container-low hover:bg-surface-container border-slate-200/50'
                    "
                    @click="selectSlot('1100')"
                  >
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <div class="flex items-center gap-space-sm">
                        <div
                          class="w-7 h-7 rounded-lg flex items-center justify-center"
                          :class="
                            selectedSlotId === '1100'
                              ? 'bg-primary text-on-primary shadow-sm'
                              : 'bg-surface-container-lowest text-on-surface-variant group-hover:text-primary'
                          "
                        >
                          <span class="material-symbols-outlined text-[18px]">
                            {{ selectedSlotId === '1100' ? 'alarm_on' : 'schedule' }}
                          </span>
                        </div>
                        <div>
                          <div class="flex items-center gap-space-xs">
                            <span class="font-title-md text-title-md text-on-surface font-bold">11:00 AM – 12:00 PM</span>
                            <span v-if="selectedSlotId === '1100'" class="w-2 h-2 rounded-full bg-primary animate-pulse"></span>
                          </div>
                          <span class="font-body-sm text-body-sm text-primary font-medium">Turno Matutino Óptimo</span>
                        </div>
                      </div>

                      <span
                        v-if="selectedSlotId === '1100'"
                        class="px-space-sm py-1 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-semibold shadow-xs flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined text-[14px]">check_circle</span>
                        Seleccionado · {{ selectedBookingService.duration }} min garantizados
                      </span>
                      <span
                        v-else
                        class="px-space-sm py-1 rounded-lg bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-semibold flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                        Disponible
                      </span>
                    </div>
                  </div>

                  <!-- Slot 12:00 PM - 01:00 PM -->
                  <div
                    class="p-space-md rounded-xl transition-all cursor-pointer border"
                    :class="
                      selectedSlotId === '1200'
                        ? 'bg-surface-container-high shadow-sm border-primary/20'
                        : 'group bg-surface-container-low hover:bg-surface-container border-slate-200/50'
                    "
                    @click="selectSlot('1200')"
                  >
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <div class="flex items-center gap-space-sm">
                        <div
                          class="w-7 h-7 rounded-lg flex items-center justify-center transition-colors"
                          :class="
                            selectedSlotId === '1200'
                              ? 'bg-primary text-on-primary shadow-sm'
                              : 'bg-surface-container-lowest text-on-surface-variant group-hover:text-primary'
                          "
                        >
                          <span class="material-symbols-outlined text-[18px]">
                            {{ selectedSlotId === '1200' ? 'alarm_on' : 'schedule' }}
                          </span>
                        </div>
                        <div>
                          <span class="font-title-sm text-title-sm text-on-surface group-hover:text-primary transition-colors font-semibold">
                            12:00 PM – 01:00 PM
                          </span>
                          <span class="block font-body-sm text-body-sm text-on-surface-variant">Consultorio 301 libre</span>
                        </div>
                      </div>
                      <span
                        v-if="selectedSlotId === '1200'"
                        class="px-space-sm py-1 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-semibold shadow-xs flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined text-[14px]">check_circle</span>
                        Seleccionado · {{ selectedBookingService.duration }} min garantizados
                      </span>
                      <span
                        v-else
                        class="px-space-sm py-1 rounded-lg bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-semibold flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                        Disponible
                      </span>
                    </div>
                  </div>

                  <!-- Afternoon Divider -->
                  <div class="flex items-center gap-space-sm my-space-xs">
                    <span class="font-label-sm text-label-sm text-outline uppercase tracking-wider font-semibold">Turno Vespertino</span>
                    <div class="h-px bg-surface-container-high flex-1"></div>
                  </div>

                  <!-- Slot 04:00 PM - 05:00 PM -->
                  <div
                    class="p-space-md rounded-xl transition-all cursor-pointer border"
                    :class="
                      selectedSlotId === '1600'
                        ? 'bg-surface-container-high shadow-sm border-primary/20'
                        : 'group bg-surface-container-low hover:bg-surface-container border-slate-200/50'
                    "
                    @click="selectSlot('1600')"
                  >
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <div class="flex items-center gap-space-sm">
                        <div
                          class="w-7 h-7 rounded-lg flex items-center justify-center transition-colors"
                          :class="
                            selectedSlotId === '1600'
                              ? 'bg-primary text-on-primary shadow-sm'
                              : 'bg-surface-container-lowest text-on-surface-variant group-hover:text-primary'
                          "
                        >
                          <span class="material-symbols-outlined text-[18px]">
                            {{ selectedSlotId === '1600' ? 'alarm_on' : 'schedule' }}
                          </span>
                        </div>
                        <div>
                          <span class="font-title-sm text-title-sm text-on-surface group-hover:text-primary transition-colors font-semibold">
                            04:00 PM – 05:00 PM
                          </span>
                          <span class="block font-body-sm text-body-sm text-on-surface-variant">Ventana post-receso</span>
                        </div>
                      </div>
                      <span
                        v-if="selectedSlotId === '1600'"
                        class="px-space-sm py-1 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-semibold shadow-xs flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined text-[14px]">check_circle</span>
                        Seleccionado · {{ selectedBookingService.duration }} min garantizados
                      </span>
                      <span
                        v-else
                        class="px-space-sm py-1 rounded-lg bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-semibold flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                        Disponible
                      </span>
                    </div>
                  </div>

                  <!-- Slot 05:00 PM - 06:00 PM -->
                  <div
                    class="p-space-md rounded-xl transition-all cursor-pointer border"
                    :class="
                      selectedSlotId === '1700'
                        ? 'bg-surface-container-high shadow-sm border-primary/20'
                        : 'group bg-surface-container-low hover:bg-surface-container border-slate-200/50'
                    "
                    @click="selectSlot('1700')"
                  >
                    <div class="flex items-center justify-between flex-wrap gap-2">
                      <div class="flex items-center gap-space-sm">
                        <div
                          class="w-7 h-7 rounded-lg flex items-center justify-center transition-colors"
                          :class="
                            selectedSlotId === '1700'
                              ? 'bg-primary text-on-primary shadow-sm'
                              : 'bg-surface-container-lowest text-on-surface-variant group-hover:text-primary'
                          "
                        >
                          <span class="material-symbols-outlined text-[18px]">
                            {{ selectedSlotId === '1700' ? 'alarm_on' : 'schedule' }}
                          </span>
                        </div>
                        <div>
                          <span class="font-title-sm text-title-sm text-on-surface group-hover:text-primary transition-colors font-semibold">
                            05:00 PM – 06:00 PM
                          </span>
                          <span class="block font-body-sm text-body-sm text-on-surface-variant">Último bloque de la jornada</span>
                        </div>
                      </div>
                      <span
                        v-if="selectedSlotId === '1700'"
                        class="px-space-sm py-1 rounded-lg bg-primary text-on-primary font-label-sm text-label-sm font-semibold shadow-xs flex items-center gap-1"
                      >
                        <span class="material-symbols-outlined text-[14px]">check_circle</span>
                        Seleccionado · {{ selectedBookingService.duration }} min garantizados
                      </span>
                      <span
                        v-else
                        class="px-space-sm py-1 rounded-lg bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm font-semibold flex items-center gap-1"
                      >
                        <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                        Disponible
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Micro Visual Timeline Bar -->
                <div class="mt-space-xs p-space-md bg-surface-container-low rounded-xl flex flex-col gap-2 border border-slate-200/50">
                  <div class="flex items-center justify-between text-on-surface-variant font-label-sm text-label-sm">
                    <span>Distribución de disponibilidad hoy</span>
                    <span class="font-code-sm text-code-sm text-primary font-bold">4 de 5 bloques hábiles</span>
                  </div>
                  <div class="w-full h-2 rounded-full bg-surface-container flex overflow-hidden gap-0.5">
                    <div class="h-full bg-tertiary w-1/5" title="10:00 - 11:00 Reservado"></div>
                    <div class="h-full bg-primary w-1/5" title="11:00 - 12:00 Seleccionado"></div>
                    <div class="h-full bg-secondary w-1/5" title="12:00 - 13:00 Disponible"></div>
                    <div class="h-full bg-secondary w-1/5" title="16:00 - 17:00 Disponible"></div>
                    <div class="h-full bg-secondary w-1/5" title="17:00 - 18:00 Disponible"></div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- Bottom Cal.com Summary & Confirmation Card -->
          <div class="w-full bg-surface-container-lowest rounded-xl shadow-lg border border-slate-200/80 p-space-lg flex flex-col lg:flex-row lg:items-center justify-between gap-space-lg">
            <!-- Summary Metadata Specs -->
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-space-md flex-1">
              <div class="flex items-start gap-space-sm">
                <div class="w-8 h-8 rounded-lg bg-surface-container-low flex items-center justify-center text-primary border border-slate-200/50">
                  <span class="material-symbols-outlined text-[20px]">medical_services</span>
                </div>
                <div>
                  <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block">Servicio Reservado</span>
                  <span class="font-title-sm text-title-sm text-on-surface font-bold block">{{ selectedBookingService.name }}</span>
                  <span class="font-code-sm text-code-sm text-primary font-semibold">{{ selectedBookingService.duration }} min continuos</span>
                </div>
              </div>

              <div class="flex items-start gap-space-sm">
                <div class="w-8 h-8 rounded-lg bg-surface-container-low flex items-center justify-center text-primary border border-slate-200/50">
                  <span class="material-symbols-outlined text-[20px]">event_available</span>
                </div>
                <div>
                  <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block">Fecha &amp; Horario</span>
                  <span class="font-title-sm text-title-sm text-on-surface font-bold block">Jueves 14 Nov · {{ selectedTimeRange }}</span>
                  <span class="font-body-sm text-body-sm text-on-surface-variant">Sede Centro · Consultorio 301</span>
                </div>
              </div>

              <div class="flex items-start gap-space-sm sm:col-span-2 lg:col-span-1">
                <div class="w-8 h-8 rounded-lg bg-surface-container-low flex items-center justify-center text-secondary border border-slate-200/50">
                  <span class="material-symbols-outlined text-[20px]">payments</span>
                </div>
                <div>
                  <span class="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider block">Total a Liquidar</span>
                  <span class="font-headline-sm text-headline-sm text-on-surface block font-bold">
                    ${{ selectedBookingService.price.toLocaleString('es-MX') }} MXN
                  </span>
                  <span class="font-label-sm text-label-sm text-secondary font-medium">Tarifa fija oficial clínica</span>
                </div>
              </div>
            </div>

            <!-- Action Panel & Guarantees -->
            <div class="flex flex-col sm:items-end gap-space-xs shrink-0">
              <button
                type="button"
                class="w-full sm:w-auto px-space-xl py-3 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-title-sm transition-all shadow-md hover:shadow-lg flex items-center justify-center gap-space-sm active:scale-[0.98]"
                @click="confirmAppointment"
              >
                <span class="material-symbols-outlined text-[20px]">lock_clock</span>
                <span>
                  Confirmar Reserva de {{ selectedBookingService.duration }} min · ${{ selectedBookingService.price.toLocaleString('es-MX') }} MXN
                </span>
              </button>
              <div class="flex items-center gap-1.5 text-on-surface-variant mt-1">
                <span class="material-symbols-outlined text-[15px] text-secondary">check_circle</span>
                <span class="font-body-sm text-body-sm">Cancelación sin costo hasta 2 hrs antes · NOM-024-SSA3-2012</span>
              </div>
            </div>
          </div>

          <!-- Booking Confirmation Toast Feedback -->
          <div
            v-if="showBookingConfirmationToast"
            class="bg-secondary-fixed text-on-secondary-fixed p-space-md rounded-xl flex items-center justify-between shadow-md border border-secondary/20 transition-all"
          >
            <div class="flex items-center gap-space-sm">
              <span class="material-symbols-outlined text-[24px] text-secondary">task_alt</span>
              <div>
                <span class="font-title-sm text-title-sm text-on-secondary-fixed font-bold">¡Cita pre-agendada exitosamente!</span>
                <p class="font-body-sm text-body-sm text-on-secondary-fixed-variant">
                  Se ha emitido el bloqueo de {{ selectedBookingService.duration }} minutos ({{ selectedTimeRange }}) en el expediente del Dr. Alejandro Vega.
                </p>
              </div>
            </div>
            <button
              type="button"
              class="font-label-md text-label-md text-on-secondary-fixed hover:underline font-semibold"
              @click="showBookingConfirmationToast = false"
            >
              Cerrar
            </button>
          </div>
      </div>
    </div>
  </MedikaAppShell>
</template>
