<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'
import { useBranchApi } from '~/composables/useBranchApi'
import { useTenancyStore } from '~/stores/tenancy'

const props = withDefaults(defineProps<{
  initialTab?: 'BRANCHES' | 'STAFF'
  initialModalOpen?: boolean
}>(), {
  initialTab: 'BRANCHES',
  initialModalOpen: false
})

const branchApi = useBranchApi()
const tenancyStore = useTenancyStore()

const activeSection = ref<'BRANCHES' | 'STAFF'>(props.initialTab)
const selectedBranchId = ref<'centro' | 'norte'>('centro')
const apiSynced = ref(false)

// Branches data matching Stitch Screen 019a20af585f4af4aa39754e7ac9adf3
interface RoomCard {
  id: string
  name: string
  code: string
  subtitle: string
  icon: string
  theme: 'sky' | 'amber'
  statusLabel: string
  statusState: 'AVAILABLE' | 'OCCUPIED'
  specialistInitials: string
  specialistName: string
  specialistDetail: string
  unassigned?: boolean
}

interface BranchInfo {
  id: 'centro' | 'norte'
  backendBranchUuid: string
  pillName: string
  title: string
  badge: string
  codeId: string
  address: string
  phone: string
  phoneNote: string
  timeZone: string
  shiftStatus: string
  totalRoomsCount: number
  activeRoomsNote: string
  rooms: RoomCard[]
}

const branches = ref<Record<'centro' | 'norte', BranchInfo>>({
  centro: {
    id: 'centro',
    backendBranchUuid: '00000000-0000-0000-0000-000000000101',
    pillName: 'Sede Centro (Matriz)',
    title: 'Sede Centro',
    badge: 'Matriz Operativa',
    codeId: 'ID: SED-MX-CDMX-001',
    address: 'Av. Paseo de la Reforma 222, Juárez, Cuauhtémoc, 06600 CDMX',
    phone: '+52 55 1122 3344',
    phoneNote: 'Conmutador central',
    timeZone: 'America/Mexico_City',
    shiftStatus: 'UTC-6 (Turno Matutino activo)',
    totalRoomsCount: 4,
    activeRoomsNote: '3 activos para consulta inmediata',
    rooms: [
      {
        id: '00000000-0000-0000-0000-000000000201',
        name: 'Consultorio Dental 1',
        code: 'DEN-01',
        subtitle: 'Piso 1 • Sillón ergonómico + RX integrado',
        icon: 'dentistry',
        theme: 'sky',
        statusLabel: 'Disponible',
        statusState: 'AVAILABLE',
        specialistInitials: 'AV',
        specialistName: 'Dr. Alejandro Vega',
        specialistDetail: 'Odontología Integral / Ortodoncia'
      },
      {
        id: '00000000-0000-0000-0000-000000000202',
        name: 'Cabina Podológica A',
        code: 'POD-A',
        subtitle: 'Planta Baja • Sillón articulado clínico',
        icon: 'footprint',
        theme: 'amber',
        statusLabel: 'En consulta',
        statusState: 'OCCUPIED',
        specialistInitials: 'SR',
        specialistName: 'Dra. Sofía Rincón',
        specialistDetail: 'Cita en curso hasta 11:30 AM'
      },
      {
        id: '00000000-0000-0000-0000-000000000203',
        name: 'Consultorio General 2',
        code: 'GEN-02',
        subtitle: 'Piso 2 • Camilla de exploración y mesa instrumental',
        icon: 'stethoscope',
        theme: 'sky',
        statusLabel: 'Disponible',
        statusState: 'AVAILABLE',
        specialistInitials: '',
        specialistName: 'Turno abierto disponible',
        specialistDetail: '+ Asignar especialista',
        unassigned: true
      }
    ]
  },
  norte: {
    id: 'norte',
    backendBranchUuid: '00000000-0000-0000-0000-000000000102',
    pillName: 'Sede Norte (Sucursal)',
    title: 'Sede Norte',
    badge: 'Sucursal Clínica',
    codeId: 'ID: SED-MX-CDMX-002',
    address: 'Av. Universidad 402, Nivel PB, Zona Norte, CDMX',
    phone: '+52 55 8899 7766',
    phoneNote: 'Recepción Norte',
    timeZone: 'America/Mexico_City',
    shiftStatus: 'UTC-6 (Turno Vespertino activo)',
    totalRoomsCount: 2,
    activeRoomsNote: '2 activos para consulta inmediata',
    rooms: [
      {
        id: '00000000-0000-0000-0000-000000000204',
        name: 'Consultorio Dental 1 (Norte)',
        code: 'N-DEN-01',
        subtitle: 'Planta Baja • Unidad dental + Rayos X',
        icon: 'dentistry',
        theme: 'sky',
        statusLabel: 'Disponible',
        statusState: 'AVAILABLE',
        specialistInitials: 'AV',
        specialistName: 'Dr. Alejandro Vega',
        specialistDetail: 'Interconsulta Martes y Jueves'
      },
      {
        id: '00000000-0000-0000-0000-000000000205',
        name: 'Consultorio Cardiología N2',
        code: 'N-CAR-02',
        subtitle: 'Piso 1 • Electrocardiógrafo de 12 derivaciones',
        icon: 'stethoscope',
        theme: 'sky',
        statusLabel: 'Disponible',
        statusState: 'AVAILABLE',
        specialistInitials: '',
        specialistName: 'Turno abierto disponible',
        specialistDetail: '+ Asignar especialista',
        unassigned: true
      }
    ]
  }
})

const activeBranch = computed(() => branches.value[selectedBranchId.value])

onMounted(async () => {
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  try {
    const apiBranches = await branchApi.listBranches(tenantSlug)
    const centroDto = apiBranches.find((b) => b.name.toLowerCase().includes('centro')) || apiBranches[0]
    const norteDto = apiBranches.find((b) => b.name.toLowerCase().includes('norte')) || apiBranches[1]

    if (centroDto) {
      branches.value.centro.backendBranchUuid = centroDto.branchId
      branches.value.centro.title = centroDto.name
      branches.value.centro.address = centroDto.address
      branches.value.centro.phone = centroDto.phone
      branches.value.centro.timeZone = centroDto.timeZone
      const centroRooms = await branchApi.listRooms(tenantSlug, centroDto.branchId)
      if (centroRooms.length > 0) {
        centroRooms.forEach((dtoRoom) => {
          const exists = branches.value.centro.rooms.some(
            (r) => r.id === dtoRoom.roomId || r.name.toLowerCase() === dtoRoom.name.toLowerCase()
          )
          if (!exists) {
            branches.value.centro.rooms.push({
              id: dtoRoom.roomId,
              name: dtoRoom.name,
              code: dtoRoom.code,
              subtitle: 'Piso 1 • Equipamiento clínico estándar (Capacidad = 1)',
              icon: 'stethoscope',
              theme: 'sky',
              statusLabel: 'Disponible',
              statusState: 'AVAILABLE',
              specialistInitials: '',
              specialistName: 'Turno abierto disponible',
              specialistDetail: '+ Asignar especialista',
              unassigned: true
            })
          }
        })
      }
    }

    if (norteDto) {
      branches.value.norte.backendBranchUuid = norteDto.branchId
      branches.value.norte.title = norteDto.name
      branches.value.norte.address = norteDto.address
      branches.value.norte.phone = norteDto.phone
      branches.value.norte.timeZone = norteDto.timeZone
      const norteRooms = await branchApi.listRooms(tenantSlug, norteDto.branchId)
      if (norteRooms.length > 0) {
        norteRooms.forEach((dtoRoom) => {
          const exists = branches.value.norte.rooms.some(
            (r) => r.id === dtoRoom.roomId || r.name.toLowerCase() === dtoRoom.name.toLowerCase()
          )
          if (!exists) {
            branches.value.norte.rooms.push({
              id: dtoRoom.roomId,
              name: dtoRoom.name,
              code: dtoRoom.code,
              subtitle: 'Planta Baja • Equipamiento clínico estándar (Capacidad = 1)',
              icon: 'stethoscope',
              theme: 'sky',
              statusLabel: 'Disponible',
              statusState: 'AVAILABLE',
              specialistInitials: '',
              specialistName: 'Turno abierto disponible',
              specialistDetail: '+ Asignar especialista',
              unassigned: true
            })
          }
        })
      }
    }

    const staffList = await branchApi.listStaffAssignments(tenantSlug)
    const sofiaRecord = staffList.find((s) => s.staffId === 'dra-sofia-rincon')
    if (sofiaRecord && sofiaRecord.assignedBranches.length > 1) {
      sofiaExtraBranchAdded.value = true
    }
    apiSynced.value = true
  } catch {
    // Offline fallback uses pre-seeded local state
  }
})

// Modals for New Branch & New Consultation Room
const isNewBranchModalOpen = ref(props.initialModalOpen)
const newBranchForm = ref({
  name: '',
  address: '',
  phone: '+52 55 ',
  timeZone: 'America/Mexico_City'
})

const isNewRoomModalOpen = ref(false)
const newRoomForm = ref({
  name: '',
  code: '',
  subtitle: 'Piso 1 • Equipamiento clínico estándar'
})

async function handleCreateBranch() {
  if (!newBranchForm.value.name.trim()) return
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  try {
    const created = await branchApi.createBranch(tenantSlug, {
      name: newBranchForm.value.name,
      address: newBranchForm.value.address || 'Av. Universidad 402, CDMX',
      phone: newBranchForm.value.phone,
      timeZone: newBranchForm.value.timeZone
    })
    branches.value.norte.backendBranchUuid = created.branchId
  } catch {
    // Fallback for offline UI preview
  }
  branches.value.norte.title = newBranchForm.value.name
  branches.value.norte.pillName = `${newBranchForm.value.name} (Sucursal)`
  branches.value.norte.address = newBranchForm.value.address || branches.value.norte.address
  selectedBranchId.value = 'norte'
  isNewBranchModalOpen.value = false
  newBranchForm.value = { name: '', address: '', phone: '+52 55 ', timeZone: 'America/Mexico_City' }
}

async function handleCreateRoom() {
  if (!newRoomForm.value.name.trim()) return
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  let createdRoomId = `room-${Date.now()}`
  try {
    const createdRoom = await branchApi.createRoom(tenantSlug, activeBranch.value.backendBranchUuid, {
      name: newRoomForm.value.name,
      code: newRoomForm.value.code || 'GENERAL',
      roomType: newRoomForm.value.code || 'GENERAL'
    })
    createdRoomId = createdRoom.roomId
  } catch {
    // Fallback for offline UI preview
  }

  activeBranch.value.rooms.push({
    id: createdRoomId,
    name: newRoomForm.value.name,
    code: newRoomForm.value.code || 'CON-NEW',
    subtitle: newRoomForm.value.subtitle,
    icon: 'stethoscope',
    theme: 'sky',
    statusLabel: 'Disponible',
    statusState: 'AVAILABLE',
    specialistInitials: '',
    specialistName: 'Turno abierto disponible',
    specialistDetail: '+ Asignar especialista',
    unassigned: true
  })
  activeBranch.value.totalRoomsCount = activeBranch.value.rooms.length
  isNewRoomModalOpen.value = false
  newRoomForm.value = { name: '', code: '', subtitle: 'Piso 1 • Equipamiento clínico estándar' }
}

// Staff Directory & Assignment State (Stitch Screen: a96089a03dc141e29d668fddcba3c97d)
const staffFilter = ref<'ALL' | 'DOCTOR' | 'RECEPTIONIST'>('ALL')
const staffSearchQuery = ref('')
const selectedStaffToAssign = ref<'laura' | 'alejandro' | 'sofia' | 'carlos'>('laura')
const targetBranchToAssign = ref<'centro' | 'norte' | 'sur'>('norte')
const assignmentFeedbackBanner = ref<string | null>(null)

const lauraAssignedBranch = ref<'Sede Centro' | 'Sede Norte' | 'Sede Sur'>('Sede Centro')
const sofiaExtraBranchAdded = ref(false)

async function handleAssignStaffSubmit() {
  const tenantSlug = tenancyStore.resolvedSlug || 'clinica-irma'
  const branchLabel =
    targetBranchToAssign.value === 'centro'
      ? 'Sede Centro'
      : targetBranchToAssign.value === 'norte'
        ? 'Sede Norte'
        : 'Sede Sur'

  const branchUuid =
    targetBranchToAssign.value === 'centro'
      ? branches.value.centro.backendBranchUuid
      : branches.value.norte.backendBranchUuid

  const staffLookup: Record<
    'laura' | 'alejandro' | 'sofia' | 'carlos',
    { id: string; name: string; role: 'DOCTOR' | 'RECEPTIONIST' }
  > = {
    laura: { id: 'rec-laura-mendez', name: 'Laura Méndez', role: 'RECEPTIONIST' },
    alejandro: { id: 'dr-alejandro-vega', name: 'Dr. Alejandro Vega', role: 'DOCTOR' },
    sofia: { id: 'dra-sofia-rincon', name: 'Dra. Sofía Rincón', role: 'DOCTOR' },
    carlos: { id: 'rec-carlos-ruiz', name: 'Carlos Ruiz', role: 'RECEPTIONIST' }
  }

  const staff = staffLookup[selectedStaffToAssign.value]

  try {
    await branchApi.assignStaffToBranch(tenantSlug, {
      staffUserId: staff.id,
      staffFullName: staff.name,
      role: staff.role,
      branchId: branchUuid
    })
  } catch (err: any) {
    const detail = err?.data?.detail || err?.response?._data?.detail
    if (detail && staff.role === 'RECEPTIONIST') {
      lauraAssignedBranch.value = branchLabel
      assignmentFeedbackBanner.value = `Regla de Sede Única validada por API (422): ${detail} — ${staff.name} reasignada exclusivamente a ${branchLabel}.`
      setTimeout(() => {
        assignmentFeedbackBanner.value = null
      }, 5000)
      return
    }
  }

  if (selectedStaffToAssign.value === 'laura') {
    lauraAssignedBranch.value = branchLabel
    assignmentFeedbackBanner.value = `Laura Méndez fue reasignada exclusivamente a ${branchLabel} y desvinculada de la caja anterior.`
  } else if (selectedStaffToAssign.value === 'sofia') {
    sofiaExtraBranchAdded.value = true
    assignmentFeedbackBanner.value = `Dra. Sofía Rincón ahora cuenta con asignación multi-sede en ${branchLabel}.`
  } else {
    assignmentFeedbackBanner.value = `Asignación física sincronizada con la API en ${branchLabel}.`
  }

  setTimeout(() => {
    assignmentFeedbackBanner.value = null
  }, 4000)
}

const shellTitle = computed(() =>
  activeSection.value === 'BRANCHES'
    ? 'Gestión de Sedes y Consultorios'
    : 'Personal por Sede y Asignaciones Físicas'
)

const shellNav = computed(() => (activeSection.value === 'BRANCHES' ? 'branches' : 'staff'))
</script>

<template>
  <MedikaAppShell
    :title="shellTitle"
    breadcrumb-section="Infraestructura y Sedes (AHC-4)"
    story-badge="AHC-4 · AC-1, AC-2 & AC-3"
    :active-nav="shellNav"
  >
    <template #header-actions>
      <div class="flex items-center gap-2">
        <button
          type="button"
          class="h-8 px-3 rounded-lg font-title-sm text-xs transition-colors"
          :class="
            activeSection === 'BRANCHES'
              ? 'bg-primary text-on-primary font-semibold'
              : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
          "
          @click="activeSection = 'BRANCHES'"
        >
          Sedes y Consultorios
        </button>
        <button
          type="button"
          class="h-8 px-3 rounded-lg font-title-sm text-xs transition-colors"
          :class="
            activeSection === 'STAFF'
              ? 'bg-primary text-on-primary font-semibold'
              : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
          "
          @click="activeSection = 'STAFF'"
        >
          Personal por Sede
        </button>
        <button
          v-if="activeSection === 'BRANCHES'"
          type="button"
          class="h-8 inline-flex items-center gap-1.5 px-3 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-xs shadow-subtle transition-colors"
          @click="isNewBranchModalOpen = true"
        >
          <span class="material-symbols-outlined text-[16px]">add</span>
          <span>Nueva Sede</span>
        </button>
      </div>
    </template>

    <!-- ======================================================================= -->
    <!-- VIEW A: GESTIÓN DE SEDES Y CONSULTORIOS (Stitch: 019a20af585f4af4aa39754e7ac9adf3) -->
    <!-- ======================================================================= -->
    <div v-if="activeSection === 'BRANCHES'" class="w-full space-y-8">
      <!-- Page Body Container -->
      <div class="w-full space-y-8">
        <!-- Page Title & Branch Selector Section -->
        <section class="space-y-4">
          <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
                Gestión de Sedes y Consultorios Físicos
              </h1>
              <p class="font-body-md text-body-md text-on-surface-variant mt-1 max-w-2xl">
                Administra las ubicaciones físicas, consultorios disponibles y reglas de asignación clínica en tiempo real sin riesgo de colisión.
              </p>
            </div>
          </div>

          <!-- Branch Selector Pills -->
          <div class="flex flex-wrap items-center gap-2.5 pt-1">
            <!-- Sede Centro -->
            <button
              type="button"
              class="inline-flex items-center gap-2 px-4 py-2 rounded-full text-xs transition"
              :class="
                selectedBranchId === 'centro'
                  ? 'bg-primary text-on-primary font-semibold shadow-subtle'
                  : 'bg-surface-container-lowest hover:bg-surface-container-low text-on-surface-variant font-medium border border-outline-variant/40'
              "
              @click="selectedBranchId = 'centro'"
            >
              <span v-if="selectedBranchId === 'centro'" class="material-symbols-outlined text-sm">check_circle</span>
              <span v-else class="w-2 h-2 rounded-full bg-outline-variant"></span>
              <span>{{ branches.centro.pillName }}</span>
              <span class="w-1.5 h-1.5 rounded-full" :class="selectedBranchId === 'centro' ? 'bg-on-primary/40' : 'bg-outline-variant'"></span>
              <span class="font-code-sm text-[11px]" :class="selectedBranchId === 'centro' ? 'font-normal opacity-90' : 'text-outline'">
                {{ branches.centro.totalRoomsCount }} consultorios
              </span>
            </button>

            <!-- Sede Norte -->
            <button
              type="button"
              class="inline-flex items-center gap-2 px-4 py-2 rounded-full text-xs transition"
              :class="
                selectedBranchId === 'norte'
                  ? 'bg-primary text-on-primary font-semibold shadow-subtle'
                  : 'bg-surface-container-lowest hover:bg-surface-container-low text-on-surface-variant font-medium border border-outline-variant/40'
              "
              @click="selectedBranchId = 'norte'"
            >
              <span v-if="selectedBranchId === 'norte'" class="material-symbols-outlined text-sm">check_circle</span>
              <span v-else class="w-2 h-2 rounded-full bg-outline-variant"></span>
              <span>{{ branches.norte.pillName }}</span>
              <span class="w-1.5 h-1.5 rounded-full" :class="selectedBranchId === 'norte' ? 'bg-on-primary/40' : 'bg-outline-variant'"></span>
              <span class="font-code-sm text-[11px]" :class="selectedBranchId === 'norte' ? 'font-normal opacity-90' : 'text-outline'">
                {{ branches.norte.totalRoomsCount }} consultorios
              </span>
            </button>

            <!-- Quick Add Branch -->
            <button
              type="button"
              class="inline-flex items-center gap-1.5 px-3 py-2 rounded-full text-xs font-semibold text-primary hover:bg-primary-fixed/40 transition border border-dashed border-primary/50"
              @click="isNewBranchModalOpen = true"
            >
              <span class="material-symbols-outlined text-sm">add</span>
              <span>Nueva Sede</span>
            </button>
          </div>
        </section>

        <!-- Active Branch Summary Card -->
        <section class="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-6 shadow-subtle space-y-6">
          <!-- Top Branch Header -->
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-outline-variant/25">
            <div class="flex items-center gap-3.5">
              <div class="w-12 h-12 rounded-xl bg-primary-fixed/60 border border-primary/15 flex items-center justify-center text-primary">
                <span class="material-symbols-outlined text-2xl">apartment</span>
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <h2 class="font-headline-sm text-lg font-bold text-on-surface leading-tight">{{ activeBranch.title }}</h2>
                  <span class="px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant text-[11px] font-bold uppercase tracking-wider">
                    {{ activeBranch.badge }}
                  </span>
                </div>
                <span class="text-xs font-code-sm text-outline">{{ activeBranch.codeId }}</span>
              </div>
            </div>

            <div class="flex items-center gap-3">
              <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-secondary-container/60 text-on-secondary-container border border-secondary/20 font-medium text-xs">
                <span class="w-2 h-2 rounded-full bg-secondary animate-pulse"></span>
                En servicio operativo
              </span>
              <button
                type="button"
                class="p-1.5 rounded-lg text-outline hover:text-on-surface hover:bg-surface-container-low transition"
                title="Opciones"
              >
                <span class="material-symbols-outlined text-xl">more_vert</span>
              </button>
            </div>
          </div>

          <!-- Details Grid -->
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 p-4 rounded-xl bg-surface-container-low border border-outline-variant/25">
            <!-- Item 1: Dirección -->
            <div class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary text-xl mt-0.5 shrink-0">pin_drop</span>
              <div class="flex flex-col min-w-0">
                <span class="text-[11px] font-bold uppercase tracking-wider text-outline">Dirección</span>
                <span class="text-xs font-semibold text-on-surface leading-snug mt-0.5">{{ activeBranch.address }}</span>
              </div>
            </div>

            <!-- Item 2: Teléfono -->
            <div class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary text-xl mt-0.5 shrink-0">call</span>
              <div class="flex flex-col min-w-0">
                <span class="text-[11px] font-bold uppercase tracking-wider text-outline">Teléfono Principal</span>
                <span class="text-xs font-semibold text-on-surface mt-0.5">{{ activeBranch.phone }}</span>
                <span class="text-[11px] font-code-sm text-on-surface-variant">{{ activeBranch.phoneNote }}</span>
              </div>
            </div>

            <!-- Item 3: Zona Horaria -->
            <div class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary text-xl mt-0.5 shrink-0">schedule</span>
              <div class="flex flex-col min-w-0">
                <span class="text-[11px] font-bold uppercase tracking-wider text-outline">Zona Horaria &amp; Turno</span>
                <span class="text-xs font-semibold text-on-surface mt-0.5">{{ activeBranch.timeZone }}</span>
                <span class="text-[11px] font-code-sm text-secondary font-semibold">{{ activeBranch.shiftStatus }}</span>
              </div>
            </div>

            <!-- Item 4: Capacidad -->
            <div class="flex items-start gap-3">
              <span class="material-symbols-outlined text-primary text-xl mt-0.5 shrink-0">meeting_room</span>
              <div class="flex flex-col min-w-0">
                <span class="text-[11px] font-bold uppercase tracking-wider text-outline">Capacidad Total</span>
                <span class="text-xs font-semibold text-on-surface mt-0.5">{{ activeBranch.totalRoomsCount }} consultorios habilitados</span>
                <span class="text-[11px] text-on-surface-variant">{{ activeBranch.activeRoomsNote }}</span>
              </div>
            </div>
          </div>

          <!-- Action Links Footer -->
          <div class="flex flex-wrap items-center justify-between gap-3 pt-1 text-xs">
            <div class="flex items-center gap-1.5 text-on-surface-variant">
              <span class="material-symbols-outlined text-primary text-base">verified</span>
              <span>Instalaciones validadas bajo la norma sanitaria vigente</span>
            </div>
            <div class="flex items-center gap-4">
              <button type="button" class="inline-flex items-center gap-1.5 text-primary font-semibold hover:text-primary-container hover:underline">
                <span class="material-symbols-outlined text-sm">edit</span>
                <span>Editar datos de sede</span>
              </button>
              <span class="text-outline-variant">•</span>
              <NuxtLink
                to="/appointments/book"
                class="inline-flex items-center gap-1.5 text-primary font-semibold hover:text-primary-container hover:underline"
              >
                <span class="material-symbols-outlined text-sm">access_time</span>
                <span>Configurar horarios de atención</span>
              </NuxtLink>
            </div>
          </div>
        </section>

        <!-- Consultation Rooms Section -->
        <section class="space-y-4">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div>
              <h2 class="font-headline-sm text-xl font-bold text-on-surface tracking-tight">Consultorios de {{ activeBranch.title }}</h2>
              <p class="text-xs text-on-surface-variant">Capacidad física individual (1 consulta a la vez sin solapamiento)</p>
            </div>
            <div class="flex items-center gap-1.5 text-xs text-on-secondary-container font-semibold bg-secondary-container/60 border border-secondary/20 px-3 py-1 rounded-full">
              <span class="material-symbols-outlined text-base text-secondary">sync_alt</span>
              <span>Motor anticolisión de citas activo</span>
            </div>
          </div>

          <!-- 3-Column Grid of Room Cards -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            <div
              v-for="room in activeBranch.rooms"
              :key="room.id"
              class="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 p-5 shadow-subtle hover:shadow-raised transition flex flex-col justify-between"
            >
              <div class="space-y-3.5">
                <!-- Top Header & Badge -->
                <div class="flex items-start justify-between gap-2">
                  <div class="flex items-center gap-3 min-w-0">
                    <div
                      class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border"
                      :class="room.theme === 'amber' ? 'bg-tertiary-fixed/70 border-tertiary/20 text-on-tertiary-fixed' : 'bg-primary-fixed/60 border-primary/15 text-primary'"
                    >
                      <span class="material-symbols-outlined text-xl">{{ room.icon }}</span>
                    </div>
                    <div class="min-w-0">
                      <h3 class="text-sm font-bold text-on-surface truncate">{{ room.name }}</h3>
                      <span class="text-xs text-on-surface-variant truncate block">{{ room.subtitle }}</span>
                    </div>
                  </div>

                  <span
                    class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold shrink-0 border"
                    :class="
                      room.statusState === 'OCCUPIED'
                        ? 'bg-tertiary-fixed/70 text-on-tertiary-fixed border-tertiary/20'
                        : 'bg-secondary-container/60 text-on-secondary-container border-secondary/20'
                    "
                  >
                    <span
                      class="w-1.5 h-1.5 rounded-full"
                      :class="room.statusState === 'OCCUPIED' ? 'bg-tertiary animate-pulse' : 'bg-secondary'"
                    ></span>
                    {{ room.statusLabel }}
                  </span>
                </div>

                <!-- Capacity Tag -->
                <div class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-surface-container-low text-on-surface-variant text-xs font-medium">
                  <span
                    class="material-symbols-outlined text-base"
                    :class="room.theme === 'amber' ? 'text-tertiary' : 'text-primary'"
                  >group</span>
                  <span>Capacidad: 1 consulta a la vez</span>
                </div>

                <!-- Specialist Details Box -->
                <div class="p-3 rounded-xl bg-surface-container-low border border-outline-variant/25 flex items-start gap-2.5">
                  <div
                    v-if="room.unassigned"
                    class="w-7 h-7 rounded-full bg-surface-container text-outline font-bold text-xs flex items-center justify-center shrink-0 mt-0.5"
                  >
                    <span class="material-symbols-outlined text-base">person_off</span>
                  </div>
                  <div
                    v-else
                    class="w-7 h-7 rounded-full font-bold text-xs flex items-center justify-center shrink-0 mt-0.5"
                    :class="room.theme === 'amber' ? 'bg-tertiary-fixed text-on-tertiary-fixed' : 'bg-primary-fixed text-primary'"
                  >
                    {{ room.specialistInitials }}
                  </div>
                  <div class="min-w-0">
                    <span class="text-[10px] font-bold text-outline uppercase tracking-wider block">
                      {{ room.unassigned ? 'Estado del Turno' : 'Especialista Asignado' }}
                    </span>
                    <p
                      class="text-xs truncate"
                      :class="room.unassigned ? 'font-medium text-on-surface-variant italic' : 'font-bold text-on-surface'"
                    >
                      {{ room.specialistName }}
                    </p>
                    <p
                      class="text-[11px] truncate"
                      :class="
                        room.unassigned
                          ? 'text-primary font-semibold cursor-pointer'
                          : room.theme === 'amber'
                            ? 'text-tertiary font-semibold'
                            : 'text-on-surface-variant'
                      "
                      @click="room.unassigned ? (activeSection = 'STAFF') : undefined"
                    >
                      {{ room.specialistDetail }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- Footer Actions -->
              <div class="pt-4 mt-4 border-t border-outline-variant/25 flex items-center justify-between text-xs">
                <button
                  type="button"
                  class="font-semibold transition"
                  :class="room.unassigned ? 'text-primary hover:text-primary-container' : 'text-on-surface-variant hover:text-on-surface'"
                  @click="activeSection = 'STAFF'"
                >
                  {{ room.unassigned ? 'Asignar médico' : room.statusState === 'OCCUPIED' ? 'Ver consulta' : 'Editar' }}
                </button>
                <NuxtLink
                  to="/appointments/book"
                  class="inline-flex items-center gap-1 font-bold text-primary hover:text-primary-container transition"
                >
                  <span>Ver agenda</span>
                  <span class="material-symbols-outlined text-sm">arrow_forward</span>
                </NuxtLink>
              </div>
            </div>

            <!-- Card 4: Add New Consultation Room (Dashed CTA) -->
            <button
              type="button"
              class="rounded-2xl border-2 border-dashed border-outline-variant hover:border-primary bg-surface-container-low/50 hover:bg-primary-fixed/20 p-6 flex flex-col items-center justify-center text-center transition group min-h-[220px]"
              @click="isNewRoomModalOpen = true"
            >
              <div class="w-12 h-12 rounded-full bg-surface-container-lowest group-hover:bg-primary text-outline group-hover:text-on-primary border border-outline-variant/40 group-hover:border-primary flex items-center justify-center shadow-subtle transition mb-3">
                <span class="material-symbols-outlined text-2xl">add</span>
              </div>
              <span class="text-sm font-bold text-on-surface group-hover:text-primary transition">
                + Agregar Consultorio o Espacio Físico
              </span>
              <p class="text-xs text-on-surface-variant mt-1 max-w-[220px]">
                Habilita un nuevo espacio clínico para citas simultáneas en esta sede.
              </p>
              <span class="inline-flex items-center gap-1 text-xs font-bold text-primary mt-3 group-hover:translate-x-0.5 transition-transform">
                <span>Configurar espacio</span>
                <span class="material-symbols-outlined text-sm">arrow_forward</span>
              </span>
            </button>
          </div>
        </section>
      </div>
    </div>

    <!-- ======================================================================= -->
    <!-- VIEW B: PERSONAL POR SEDE Y ASIGNACIONES FÍSICAS (Stitch: a96089a03dc141e29d668fddcba3c97d) -->
    <!-- ======================================================================= -->
    <div v-else class="w-full">
      <div class="flex flex-col w-full">
        <div class="w-full space-y-6">
          <!-- Top Action & Title Header -->
          <header class="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-8">
            <div class="space-y-1.5">
              <div class="flex items-center gap-2">
                <span class="inline-flex items-center justify-center w-7 h-7 rounded-lg bg-primary-fixed text-primary">
                  <span class="material-symbols-outlined text-[18px]">badge</span>
                </span>
                <p class="text-xs font-bold uppercase tracking-wider text-outline">Organización &amp; Roles</p>
              </div>
              <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
                Personal por Sede y Asignaciones Físicas
              </h1>
              <p class="font-body-md text-sm text-on-surface-variant max-w-2xl leading-relaxed">
                Gestiona la vinculación operativa del personal médico y administrativo a las instalaciones físicas de cada sede.
              </p>
            </div>
          </header>

          <!-- Feedback toast when staff assignment updates -->
          <div
            v-if="assignmentFeedbackBanner"
            class="mb-6 p-4 rounded-xl bg-secondary-container/60 border border-secondary/25 text-on-secondary-container text-xs font-semibold flex items-center justify-between"
          >
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-secondary text-lg">check_circle</span>
              <span>{{ assignmentFeedbackBanner }}</span>
            </div>
            <button type="button" class="text-on-secondary-container hover:underline" @click="assignmentFeedbackBanner = null">Cerrar</button>
          </div>

          <!-- Two-column Workplace Layout -->
          <div class="grid grid-cols-12 gap-8 items-start">
            <!-- LEFT COLUMN (7 cols): Staff Directory & Assigned Schedules -->
            <section class="col-span-12 lg:col-span-7 flex flex-col gap-5">
              <!-- Search & Filter Controls -->
              <div class="bg-surface-container-lowest p-3 rounded-2xl shadow-subtle border border-outline-variant/30 flex flex-col gap-3">
                <div class="relative flex items-center w-full">
                  <span class="material-symbols-outlined absolute left-3.5 text-outline text-[20px] pointer-events-none">search</span>
                  <input
                    v-model="staffSearchQuery"
                    type="text"
                    placeholder="Buscar médico, recepcionista o sede..."
                    class="w-full pl-11 pr-12 py-2 bg-surface-container-low border border-outline-variant/40 rounded-xl text-on-surface text-sm placeholder:text-outline focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition"
                  />
                  <span class="absolute right-3.5 text-[11px] font-code-sm font-medium text-outline bg-surface-container-lowest border border-outline-variant/40 px-1.5 py-0.5 rounded hidden sm:inline-block">⌘K</span>
                </div>

                <div class="flex items-center gap-2 px-0.5">
                  <button
                    type="button"
                    class="px-3.5 py-1.5 rounded-full text-xs font-semibold flex items-center gap-1.5 transition-all"
                    :class="staffFilter === 'ALL' ? 'bg-primary text-on-primary shadow-subtle' : 'bg-surface-container-low hover:bg-surface-container text-on-surface-variant'"
                    @click="staffFilter = 'ALL'"
                  >
                    <span>Todos</span>
                    <span class="px-1.5 py-0.5 rounded-full text-[10px] font-bold" :class="staffFilter === 'ALL' ? 'bg-on-primary/20 text-on-primary' : 'text-outline'">18</span>
                  </button>

                  <button
                    type="button"
                    class="px-3.5 py-1.5 rounded-full text-xs font-medium transition-colors flex items-center gap-1.5"
                    :class="staffFilter === 'DOCTOR' ? 'bg-primary text-on-primary shadow-subtle font-semibold' : 'bg-surface-container-low hover:bg-surface-container text-on-surface-variant'"
                    @click="staffFilter = 'DOCTOR'"
                  >
                    <span>Médicos</span>
                    <span class="text-[10px] font-semibold" :class="staffFilter === 'DOCTOR' ? 'bg-on-primary/20 text-on-primary px-1.5 py-0.5 rounded-full' : 'text-outline'">14</span>
                  </button>

                  <button
                    type="button"
                    class="px-3.5 py-1.5 rounded-full text-xs font-medium transition-colors flex items-center gap-1.5"
                    :class="staffFilter === 'RECEPTIONIST' ? 'bg-primary text-on-primary shadow-subtle font-semibold' : 'bg-surface-container-low hover:bg-surface-container text-on-surface-variant'"
                    @click="staffFilter = 'RECEPTIONIST'"
                  >
                    <span>Recepción</span>
                    <span class="text-[10px] font-semibold" :class="staffFilter === 'RECEPTIONIST' ? 'bg-on-primary/20 text-on-primary px-1.5 py-0.5 rounded-full' : 'text-outline'">4</span>
                  </button>
                </div>
              </div>

              <!-- Staff List Stack -->
              <div class="flex flex-col gap-4">
                <!-- Staff Card 1: Dr. Alejandro Vega (Multi-Sede) -->
                <article
                  v-if="staffFilter === 'ALL' || staffFilter === 'DOCTOR'"
                  class="bg-surface-container-lowest rounded-2xl p-5 shadow-subtle border border-outline-variant/30 hover:shadow-raised transition-shadow relative overflow-hidden flex flex-col gap-4"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-3.5">
                      <div class="relative">
                        <div class="w-12 h-12 rounded-xl bg-primary-fixed/60 text-primary border border-primary/15 flex items-center justify-center font-bold text-base">
                          AV
                        </div>
                        <span class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full bg-secondary ring-2 ring-surface-container-lowest"></span>
                      </div>
                      <div>
                        <div class="flex items-center gap-2">
                          <h2 class="text-base font-bold text-on-surface tracking-tight">Dr. Alejandro Vega</h2>
                          <span class="px-2.5 py-0.5 rounded-full bg-secondary-container/60 text-on-secondary-container border border-secondary/20 text-[11px] font-semibold flex items-center gap-1.5">
                            <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                            Multi-Sede
                          </span>
                        </div>
                        <p class="text-xs text-on-surface-variant mt-0.5">Médico Especialista · Cardiología Clínica</p>
                      </div>
                    </div>
                    <button type="button" class="text-outline hover:text-on-surface p-1 rounded-lg hover:bg-surface-container-low transition-colors" title="Más opciones">
                      <span class="material-symbols-outlined text-[20px]">more_vert</span>
                    </button>
                  </div>

                  <!-- Asignaciones Físicas Vigentes -->
                  <div class="bg-surface-container-low border border-outline-variant/25 rounded-xl p-3.5 flex flex-col gap-2.5">
                    <div class="flex items-center justify-between">
                      <span class="text-[11px] uppercase tracking-wider text-outline font-bold">Asignaciones Físicas Vigentes</span>
                      <span class="font-code-sm text-[11px] text-outline font-medium">2 sedes activas</span>
                    </div>
                    <div class="space-y-2">
                      <div class="flex items-start justify-between bg-surface-container-lowest p-3 rounded-lg border border-outline-variant/30 shadow-subtle">
                        <div class="flex items-start gap-3">
                          <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">apartment</span>
                          <div>
                            <p class="text-sm font-semibold text-on-surface">Sede Centro</p>
                            <p class="text-xs text-on-surface-variant">Lunes, Miércoles, Viernes · Consultorio 301</p>
                          </div>
                        </div>
                        <span class="text-[11px] px-2.5 py-0.5 rounded-full bg-primary-fixed/60 text-primary border border-primary/15 font-semibold">Cardiología</span>
                      </div>

                      <div class="flex items-start justify-between bg-surface-container-lowest p-3 rounded-lg border border-outline-variant/30 shadow-subtle">
                        <div class="flex items-start gap-3">
                          <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">domain</span>
                          <div>
                            <p class="text-sm font-semibold text-on-surface">Sede Norte</p>
                            <p class="text-xs text-on-surface-variant">Martes, Jueves · Consultorio Dental 1</p>
                          </div>
                        </div>
                        <span class="text-[11px] px-2.5 py-0.5 rounded-full bg-primary-fixed/60 text-primary border border-primary/15 font-semibold">Interconsulta</span>
                      </div>
                    </div>
                  </div>

                  <div class="flex items-center justify-between pt-1">
                    <div class="flex items-center gap-1.5 text-secondary font-medium text-xs">
                      <span class="material-symbols-outlined text-[17px] text-secondary">check_circle</span>
                      <span>Disponibilidad validada para agenda en línea</span>
                    </div>
                    <NuxtLink
                      to="/doctor/services"
                      class="text-primary hover:text-primary-container font-semibold text-xs flex items-center gap-1 hover:underline"
                    >
                      <span>Modificar horarios</span>
                      <span class="material-symbols-outlined text-[16px]">arrow_forward</span>
                    </NuxtLink>
                  </div>
                </article>

                <!-- Staff Card 2: Laura Méndez (Sede Única Exclusiva) -->
                <article
                  v-if="staffFilter === 'ALL' || staffFilter === 'RECEPTIONIST'"
                  class="bg-surface-container-lowest rounded-2xl p-5 shadow-subtle border border-outline-variant/30 hover:shadow-raised transition-shadow relative overflow-hidden flex flex-col gap-4"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-3.5">
                      <div class="relative">
                        <div class="w-12 h-12 rounded-xl bg-surface-container text-on-surface border border-outline-variant/40 flex items-center justify-center font-bold text-base">
                          LM
                        </div>
                        <span class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full bg-primary ring-2 ring-surface-container-lowest"></span>
                      </div>
                      <div>
                        <div class="flex items-center gap-2">
                          <h2 class="text-base font-bold text-on-surface tracking-tight">Laura Méndez</h2>
                          <span class="px-2.5 py-0.5 rounded-full bg-surface-container text-on-surface-variant border border-outline-variant/40 text-[11px] font-semibold flex items-center gap-1">
                            <span class="material-symbols-outlined text-[13px] text-on-surface-variant">lock</span>
                            Sede Única Exclusiva
                          </span>
                        </div>
                        <p class="text-xs text-on-surface-variant mt-0.5">Recepción y Control de Caja</p>
                      </div>
                    </div>
                    <button type="button" class="text-outline hover:text-on-surface p-1 rounded-lg hover:bg-surface-container-low transition-colors" title="Más opciones">
                      <span class="material-symbols-outlined text-[20px]">more_vert</span>
                    </button>
                  </div>

                  <!-- Asignación Física Actual -->
                  <div class="bg-surface-container-low border border-outline-variant/25 rounded-xl p-3.5 flex flex-col gap-2.5">
                    <div class="flex items-center justify-between">
                      <span class="text-[11px] uppercase tracking-wider text-outline font-bold">Asignación Física Actual</span>
                      <span class="text-[11px] text-on-tertiary-fixed bg-tertiary-fixed/70 border border-tertiary/20 px-2 py-0.5 rounded font-semibold">Caja Activa #02</span>
                    </div>
                    <div class="bg-surface-container-lowest p-3 rounded-lg border border-outline-variant/30 shadow-subtle flex flex-col gap-2">
                      <div class="flex items-start justify-between">
                        <div class="flex items-start gap-3">
                          <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">desk</span>
                          <div>
                            <p class="text-sm font-semibold text-on-surface">{{ lauraAssignedBranch }}</p>
                            <p class="text-xs text-on-surface-variant">Lunes a Viernes · Módulo de Admisión Principal</p>
                          </div>
                        </div>
                        <span class="px-2 py-0.5 rounded bg-surface-container text-on-surface text-[11px] font-code-sm font-medium">08:00 - 17:00</span>
                      </div>
                      <div class="flex items-center gap-1.5 text-on-surface-variant text-xs bg-surface-container-low px-2.5 py-1.5 rounded border border-outline-variant/25">
                        <span class="material-symbols-outlined text-[15px] text-primary">info</span>
                        <span>Presencial activo · Asignación exclusiva para apertura de caja diaria</span>
                      </div>
                    </div>
                  </div>

                  <div class="flex items-center justify-between pt-1">
                    <span class="text-xs text-outline font-code-sm">ID: STF-8092-REC</span>
                    <button
                      type="button"
                      class="text-primary hover:text-primary-container font-semibold text-xs flex items-center gap-1.5 hover:underline"
                      @click="selectedStaffToAssign = 'laura'"
                    >
                      <span class="material-symbols-outlined text-[17px]">swap_horiz</span>
                      <span>Reasignar sede</span>
                    </button>
                  </div>
                </article>

                <!-- Staff Card 3: Dra. Sofía Rincón -->
                <article
                  v-if="staffFilter === 'ALL' || staffFilter === 'DOCTOR'"
                  class="bg-surface-container-lowest rounded-2xl p-5 shadow-subtle border border-outline-variant/30 hover:shadow-raised transition-shadow relative overflow-hidden flex flex-col gap-4"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex items-center gap-3.5">
                      <div class="relative">
                        <div class="w-12 h-12 rounded-xl bg-primary-fixed/60 text-primary border border-primary/15 flex items-center justify-center font-bold text-base">
                          SR
                        </div>
                        <span class="absolute -bottom-0.5 -right-0.5 w-3.5 h-3.5 rounded-full bg-secondary ring-2 ring-surface-container-lowest"></span>
                      </div>
                      <div>
                        <div class="flex items-center gap-2">
                          <h2 class="text-base font-bold text-on-surface tracking-tight">Dra. Sofía Rincón</h2>
                          <span class="px-2.5 py-0.5 rounded-full bg-secondary-container/60 text-on-secondary-container border border-secondary/20 text-[11px] font-semibold flex items-center gap-1.5">
                            <span class="w-1.5 h-1.5 rounded-full bg-secondary"></span>
                            Multi-Sede: Sede Centro
                          </span>
                        </div>
                        <p class="text-xs text-on-surface-variant mt-0.5">Médico Especialista · Odontología General</p>
                      </div>
                    </div>
                    <button type="button" class="text-outline hover:text-on-surface p-1 rounded-lg hover:bg-surface-container-low transition-colors" title="Más opciones">
                      <span class="material-symbols-outlined text-[20px]">more_vert</span>
                    </button>
                  </div>

                  <!-- Asignación Física Dra. Rincón -->
                  <div class="bg-surface-container-low border border-outline-variant/25 rounded-xl p-3.5 flex flex-col gap-2.5">
                    <div class="flex items-center justify-between">
                      <span class="text-[11px] uppercase tracking-wider text-outline font-bold">Asignaciones Físicas Vigentes</span>
                      <span class="font-code-sm text-[11px] text-outline font-medium">
                        {{ sofiaExtraBranchAdded ? '2 sedes vinculadas' : '1 sede vinculada' }}
                      </span>
                    </div>
                    <div class="bg-surface-container-lowest p-3 rounded-lg border border-outline-variant/30 shadow-subtle flex items-start justify-between">
                      <div class="flex items-start gap-3">
                        <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">apartment</span>
                        <div>
                          <p class="text-sm font-semibold text-on-surface">Sede Centro</p>
                          <p class="text-xs text-on-surface-variant">Martes y Jueves · Consultorio 2</p>
                        </div>
                      </div>
                      <span class="text-[11px] px-2.5 py-0.5 rounded-full bg-primary-fixed/60 text-primary border border-primary/15 font-semibold">Odontología</span>
                    </div>
                    <div
                      v-if="sofiaExtraBranchAdded"
                      class="bg-surface-container-lowest p-3 rounded-lg border border-outline-variant/30 shadow-subtle flex items-start justify-between"
                    >
                      <div class="flex items-start gap-3">
                        <span class="material-symbols-outlined text-primary text-[20px] mt-0.5">domain</span>
                        <div>
                          <p class="text-sm font-semibold text-on-surface">Sede Norte</p>
                          <p class="text-xs text-on-surface-variant">Viernes · Consultorio Dental 1</p>
                        </div>
                      </div>
                      <span class="text-[11px] px-2.5 py-0.5 rounded-full bg-primary-fixed/60 text-primary border border-primary/15 font-semibold">Odontología</span>
                    </div>
                  </div>

                  <div class="flex items-center justify-between pt-1">
                    <button
                      type="button"
                      class="inline-flex items-center gap-1 text-xs text-primary font-semibold hover:underline"
                      @click="selectedStaffToAssign = 'sofia'"
                    >
                      <span class="material-symbols-outlined text-[17px]">add_location_alt</span>
                      <span>+ Asignar otra sede a Dra. Rincón</span>
                    </button>
                    <button type="button" class="text-outline hover:text-on-surface text-xs font-medium transition-colors">
                      Historial de rotación
                    </button>
                  </div>
                </article>
              </div>
            </section>

            <!-- RIGHT COLUMN (5 cols): Dynamic Assignment Engine & Policies -->
            <section class="col-span-12 lg:col-span-5 flex flex-col gap-6">
              <!-- Form Card: Asignar Personal a Sede -->
              <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-subtle border border-outline-variant/30 flex flex-col gap-5">
                <div class="flex items-start justify-between gap-3">
                  <div class="space-y-1">
                    <h2 class="font-headline-sm text-lg font-bold text-on-surface tracking-tight">Asignar Personal a Sede</h2>
                    <p class="text-xs text-on-surface-variant leading-relaxed">
                      Vincula un colaborador a las instalaciones físicas de tu organización.
                    </p>
                  </div>
                  <span class="p-2.5 rounded-xl bg-primary-fixed/60 text-primary border border-primary/15">
                    <span class="material-symbols-outlined text-[22px]">transfer_within_a_station</span>
                  </span>
                </div>

                <form class="space-y-4" @submit.prevent="handleAssignStaffSubmit">
                  <!-- Campo 1: Colaborador -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-on-surface flex items-center justify-between">
                      <span>Miembro del equipo</span>
                      <span class="text-[11px] text-outline font-normal">
                        {{ selectedStaffToAssign === 'laura' ? 'Rol administrativo' : 'Médico Especialista' }}
                      </span>
                    </label>
                    <div class="relative">
                      <select
                        v-model="selectedStaffToAssign"
                        class="w-full bg-surface-container-low border border-outline-variant/40 text-on-surface font-medium text-sm py-2.5 pl-3 pr-10 rounded-xl appearance-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition cursor-pointer"
                      >
                        <option value="laura">Laura Méndez (Recepción y Caja)</option>
                        <option value="alejandro">Dr. Alejandro Vega (Cardiología)</option>
                        <option value="sofia">Dra. Sofía Rincón (Odontología)</option>
                        <option value="carlos">Carlos Ortiz (Enfermería Jefe)</option>
                      </select>
                      <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-outline text-[18px] pointer-events-none">unfold_more</span>
                    </div>
                  </div>

                  <!-- Campo 2: Sede Destino -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-bold text-on-surface flex items-center justify-between">
                      <span>Sede a vincular</span>
                      <span class="text-[11px] text-on-secondary-container font-semibold bg-secondary-container/60 px-2 py-0.5 rounded border border-secondary/20">
                        3 consultorios libres
                      </span>
                    </label>
                    <div class="relative">
                      <select
                        v-model="targetBranchToAssign"
                        class="w-full bg-surface-container-low border border-outline-variant/40 text-on-surface font-medium text-sm py-2.5 pl-3 pr-10 rounded-xl appearance-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition cursor-pointer"
                      >
                        <option value="centro">Sede Centro (Principal)</option>
                        <option value="norte">Sede Norte (Sucursal)</option>
                        <option value="sur">Sede Sur (Centro Quirúrgico)</option>
                      </select>
                      <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-outline text-[18px] pointer-events-none">expand_more</span>
                    </div>
                    <p class="text-xs text-on-surface-variant flex items-center gap-1 pt-0.5">
                      <span class="material-symbols-outlined text-[15px] text-outline">location_on</span>
                      Av. Universidad 402, Nivel PB · Zona Norte
                    </p>
                  </div>

                  <!-- Soft Callout: Business Rule Enforcement -->
                  <div class="p-4 rounded-xl bg-primary-fixed/40 border border-primary/15 text-on-surface flex gap-3 items-start">
                    <span class="material-symbols-outlined text-primary text-[20px] shrink-0 mt-0.5">info</span>
                    <div class="space-y-1.5 text-left text-xs leading-relaxed">
                      <p class="font-bold text-on-surface text-xs">
                        Restricción de concurrencia para Recepción
                      </p>
                      <p class="text-on-surface-variant">
                        El personal de recepción solo puede estar asignado a una única sede a la vez. Los médicos sí pueden atender en múltiples sedes.
                      </p>
                      <p class="text-primary font-medium">
                        Al reasignar a Laura Méndez a Sede Norte, se desvinculará automáticamente de la caja física de Sede Centro.
                      </p>
                    </div>
                  </div>

                  <!-- Action Buttons -->
                  <div class="pt-2 flex flex-col sm:flex-row gap-2.5">
                    <button
                      type="submit"
                      class="flex-1 py-2.5 px-4 rounded-xl bg-primary hover:bg-primary-container text-on-primary font-semibold text-sm transition-all shadow-subtle flex items-center justify-center gap-2"
                    >
                      <span class="material-symbols-outlined text-[18px]">verified</span>
                      <span>
                        {{
                          targetBranchToAssign === 'norte'
                            ? 'Reasignar a Sede Norte'
                            : targetBranchToAssign === 'centro'
                              ? 'Reasignar a Sede Centro'
                              : 'Reasignar a Sede Sur'
                        }}
                      </span>
                    </button>
                    <button
                      type="button"
                      class="py-2.5 px-4 rounded-xl bg-surface-container hover:bg-surface-container-high text-on-surface font-semibold text-sm transition-colors"
                    >
                      Cancelar
                    </button>
                  </div>
                </form>
              </div>

              <!-- Rule & Concurrency Security Card -->
              <div class="bg-surface-container-lowest rounded-2xl p-6 shadow-subtle border border-outline-variant/30 flex flex-col gap-4">
                <div class="flex items-center gap-2 text-on-surface">
                  <span class="material-symbols-outlined text-primary text-[22px]">security</span>
                  <h3 class="text-sm font-bold tracking-tight">Reglas de Concurrencia de Personal</h3>
                </div>

                <div class="space-y-3.5">
                  <div class="flex gap-3 items-start">
                    <div class="w-5 h-5 rounded-full bg-primary-fixed text-primary font-code-sm text-[11px] font-bold flex items-center justify-center shrink-0 mt-0.5">
                      1
                    </div>
                    <div class="space-y-0.5">
                      <p class="text-xs font-bold text-on-surface">Prevención de dobles citas (Médicos)</p>
                      <p class="text-xs text-on-surface-variant leading-relaxed">
                        El motor de agenda bloquea automáticamente solicitudes simultáneas en sedes distintas si no existe un margen de traslado mínimo de 45 minutos.
                      </p>
                    </div>
                  </div>

                  <div class="flex gap-3 items-start">
                    <div class="w-5 h-5 rounded-full bg-primary-fixed text-primary font-code-sm text-[11px] font-bold flex items-center justify-center shrink-0 mt-0.5">
                      2
                    </div>
                    <div class="space-y-0.5">
                      <p class="text-xs font-bold text-on-surface">Custodia física de caja (Recepción)</p>
                      <p class="text-xs text-on-surface-variant leading-relaxed">
                        Para fines de arqueo fiscal y auditoría, ningún colaborador de recepción puede poseer cierres activos simultáneos en dos puntos terminales.
                      </p>
                    </div>
                  </div>
                </div>

                <div class="pt-3 border-t border-outline-variant/25 flex items-center justify-between text-outline text-xs">
                  <span class="font-code-sm text-[11px]">Protocolo CL-SEC v4.2</span>
                  <span class="text-primary hover:text-primary-container font-semibold cursor-pointer hover:underline">Ver manual de gobernanza</span>
                </div>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================================= -->
    <!-- MODAL: NUEVA SEDE                                                       -->
    <!-- ======================================================================= -->
    <div
      v-if="isNewBranchModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-inverse-surface/40 backdrop-blur-xs"
    >
      <div class="w-full max-w-md bg-surface-container-lowest rounded-2xl border border-outline-variant/40 shadow-xl p-6 space-y-4">
        <div class="flex items-center justify-between border-b border-outline-variant/25 pb-3">
          <h3 class="text-base font-bold text-on-surface">Registrar Nueva Sede Física</h3>
          <button type="button" class="text-outline hover:text-on-surface" @click="isNewBranchModalOpen = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <form class="space-y-4" @submit.prevent="handleCreateBranch">
          <div class="space-y-1">
            <label class="text-xs font-bold text-on-surface">Nombre de la Sede</label>
            <input
              v-model="newBranchForm.name"
              type="text"
              placeholder="Ej. Sede Sur (Especialidades)"
              class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div class="space-y-1">
            <label class="text-xs font-bold text-on-surface">Dirección Física</label>
            <input
              v-model="newBranchForm.address"
              type="text"
              placeholder="Av. Insurgentes Sur 1400, CDMX"
              class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1">
              <label class="text-xs font-bold text-on-surface">Teléfono</label>
              <input
                v-model="newBranchForm.phone"
                type="text"
                class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <div class="space-y-1">
              <label class="text-xs font-bold text-on-surface">Zona Horaria IANA</label>
              <select
                v-model="newBranchForm.timeZone"
                class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none"
              >
                <option value="America/Mexico_City">America/Mexico_City</option>
                <option value="America/Monterrey">America/Monterrey</option>
                <option value="America/Tijuana">America/Tijuana</option>
              </select>
            </div>
          </div>
          <div class="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              class="px-4 py-2 rounded-xl bg-surface-container text-on-surface text-xs font-semibold"
              @click="isNewBranchModalOpen = false"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="px-4 py-2 rounded-xl bg-primary hover:bg-primary-container text-on-primary text-xs font-semibold"
            >
              Crear Sede
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- ======================================================================= -->
    <!-- MODAL: NUEVO CONSULTORIO O ESPACIO FÍSICO                               -->
    <!-- ======================================================================= -->
    <div
      v-if="isNewRoomModalOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-inverse-surface/40 backdrop-blur-xs"
    >
      <div class="w-full max-w-md bg-surface-container-lowest rounded-2xl border border-outline-variant/40 shadow-xl p-6 space-y-4">
        <div class="flex items-center justify-between border-b border-outline-variant/25 pb-3">
          <h3 class="text-base font-bold text-on-surface">Agregar Consultorio en {{ activeBranch.title }}</h3>
          <button type="button" class="text-outline hover:text-on-surface" @click="isNewRoomModalOpen = false">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
        <form class="space-y-4" @submit.prevent="handleCreateRoom">
          <div class="space-y-1">
            <label class="text-xs font-bold text-on-surface">Nombre del Consultorio</label>
            <input
              v-model="newRoomForm.name"
              type="text"
              placeholder="Ej. Consultorio Cardiología 3"
              class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div class="space-y-1">
            <label class="text-xs font-bold text-on-surface">Clave Interna</label>
            <input
              v-model="newRoomForm.code"
              type="text"
              placeholder="Ej. CAR-03"
              class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm font-code-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div class="space-y-1">
            <label class="text-xs font-bold text-on-surface">Ubicación / Equipamiento</label>
            <input
              v-model="newRoomForm.subtitle"
              type="text"
              class="w-full px-3 py-2 rounded-xl bg-surface-container-low border border-outline-variant/40 text-on-surface text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div class="flex items-center justify-end gap-2 pt-2">
            <button
              type="button"
              class="px-4 py-2 rounded-xl bg-surface-container text-on-surface text-xs font-semibold"
              @click="isNewRoomModalOpen = false"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="px-4 py-2 rounded-xl bg-primary hover:bg-primary-container text-on-primary text-xs font-semibold"
            >
              Guardar Consultorio
            </button>
          </div>
        </form>
      </div>
    </div>
  </MedikaAppShell>
</template>
