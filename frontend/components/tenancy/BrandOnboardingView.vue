<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MedikaAppShell from '~/components/layout/MedikaAppShell.vue'
import { useTenancyApi } from '~/composables/useTenancyApi'
import { useBranchApi } from '~/composables/useBranchApi'
import { useTenancyStore } from '~/stores/tenancy'

const props = withDefaults(defineProps<{
  initialStep?: 'STEP_1_FORM' | 'STEP_2_CLINICAL' | 'STEP_3_ACTIVATED'
  simulateSlugCollision?: boolean
}>(), {
  initialStep: 'STEP_1_FORM',
  simulateSlugCollision: true
})

const tenancyApi = useTenancyApi()
const branchApi = useBranchApi()
const tenancyStore = useTenancyStore()

const currentStep = ref<'STEP_1_FORM' | 'STEP_2_CLINICAL' | 'STEP_3_ACTIVATED'>(props.initialStep)
const apiConnected = ref(false)

// Step 1 Form State (Stitch Screen: a7cb9437f160489fae71693e147652cb)
const clinicName = ref('Clínica Irma')
const adminEmail = ref('direccion@clinica-irma.com')
const phoneNumber = ref('55 1234 5678')
const slugInput = ref('clinica-irma')

// Track whether the user is currently on a colliding slug
const reservedSlugs = ref<Set<string>>(new Set(['clinica-irma']))
const slugSuggestions = ref<string[]>([
  'clinica-irma-salud',
  'clinica-irma-med',
  'clinica-irma-clinica'
])

onMounted(async () => {
  try {
    const existingBrand = await tenancyApi.resolveBrandBySlug('clinica-irma')
    if (existingBrand?.slug) {
      tenancyStore.setActiveBrand(existingBrand)
      reservedSlugs.value.add(existingBrand.slug.toLowerCase())
      apiConnected.value = true
    }
  } catch {
    // Offline fallback keeps default seeded collision state
  }
})

const hasSlugCollision = computed(() => {
  return props.simulateSlugCollision && reservedSlugs.value.has(slugInput.value.trim().toLowerCase())
})

const slugSelectedFromSuggestion = ref(false)

function applySubdomain(suggestion: string) {
  slugInput.value = suggestion
  slugSelectedFromSuggestion.value = true
}

function onSlugManualInput() {
  if (reservedSlugs.value.has(slugInput.value.trim().toLowerCase())) {
    slugSelectedFromSuggestion.value = false
  } else {
    slugSelectedFromSuggestion.value = true
  }
}

// Step 2 Form State (Stitch Screen: a91ff07f636c4ac0beb8b2b39ca35974)
const mainBranchName = ref('Sede Centro (Matriz)')
const physicalAddress = ref('Av. Paseo de la Reforma 222, Cuauhtémoc, CDMX')
const capacityTier = ref<'1' | '2-5' | '6+'>('2-5')

interface SpecialtyOption {
  name: string
  active: boolean
}

const specialties = ref<SpecialtyOption[]>([
  { name: 'Medicina General', active: true },
  { name: 'Cardiología', active: true },
  { name: 'Odontología', active: false },
  { name: 'Pediatría', active: false },
  { name: 'Ginecología', active: false }
])

const isCustomSpecialtyModalOpen = ref(false)
const newSpecialtyInput = ref('')

function toggleSpecialty(spec: SpecialtyOption) {
  spec.active = !spec.active
}

function confirmAddSpecialty() {
  const val = newSpecialtyInput.value.trim()
  if (val) {
    specialties.value.push({ name: val, active: true })
    newSpecialtyInput.value = ''
    isCustomSpecialtyModalOpen.value = false
  }
}

// Step 3 State (Stitch Screen: 1e9dc7ddc5ba44a69903cefeef1608f0)
const isSubmitting = ref(false)
const copiedPortalUrl = ref(false)

const activePortalUrl = computed(() => {
  return `https://${slugInput.value || 'clinica-irma'}.medika.com`
})

async function goToOnboardingStep(step: 'STEP_1_FORM' | 'STEP_2_CLINICAL' | 'STEP_3_ACTIVATED') {
  currentStep.value = step
  if (step === 'STEP_1_FORM') await navigateTo('/onboarding')
  else if (step === 'STEP_2_CLINICAL') await navigateTo('/onboarding/clinical-setup')
  else await navigateTo('/onboarding/activated')
}

async function handleStep1Submit() {
  if (!clinicName.value.trim()) return
  await goToOnboardingStep('STEP_2_CLINICAL')
}

async function handleStep2Submit() {
  isSubmitting.value = true
  try {
    const response = await tenancyApi.onboardBrand({
      legalName: clinicName.value,
      displayName: clinicName.value,
      preferredSlug: slugInput.value.trim().toLowerCase(),
      initialBranchName: mainBranchName.value,
      initialBranchTimeZone: 'America/Mexico_City',
      ownerEmail: adminEmail.value,
      contactPhone: `+52 ${phoneNumber.value}`,
      ownerFullName: 'Dirección Clínica'
    })
    tenancyStore.setActiveBrand(response)
    slugInput.value = response.slug

    if (mainBranchName.value.trim()) {
      await branchApi.createBranch(response.slug, {
        name: mainBranchName.value,
        address: physicalAddress.value,
        phone: `+52 ${phoneNumber.value}`,
        timeZone: 'America/Mexico_City'
      })
    }
    await goToOnboardingStep('STEP_3_ACTIVATED')
  } catch (err: any) {
    const status = err?.statusCode || err?.response?.status
    const suggestions = err?.data?.suggestions || err?.response?._data?.suggestions
    if (status === 409 && Array.isArray(suggestions) && suggestions.length) {
      slugSuggestions.value = suggestions
      reservedSlugs.value.add(slugInput.value.trim().toLowerCase())
      slugSelectedFromSuggestion.value = false
      await goToOnboardingStep('STEP_1_FORM')
    } else {
      // Fallback for offline demo or already-provisioned clinica-irma
      await goToOnboardingStep('STEP_3_ACTIVATED')
    }
  } finally {
    isSubmitting.value = false
  }
}

async function copyDomain() {
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      await navigator.clipboard.writeText(activePortalUrl.value)
    }
    copiedPortalUrl.value = true
    setTimeout(() => {
      copiedPortalUrl.value = false
    }, 2000)
  } catch {
    copiedPortalUrl.value = true
  }
}
const shellTitle = computed(() => {
  if (currentStep.value === 'STEP_1_FORM') return '1. Registro Inicial de Clínica y Subdominio'
  if (currentStep.value === 'STEP_2_CLINICAL') return '2. Configuración Clínica Inicial'
  return '3. Confirmación y Activación del Portal'
})

const shellNavId = computed(() => {
  if (currentStep.value === 'STEP_1_FORM') return 'onboarding-step-1'
  if (currentStep.value === 'STEP_2_CLINICAL') return 'onboarding-step-2'
  return 'onboarding-step-3'
})
</script>

<template>
  <MedikaAppShell
    :title="shellTitle"
    breadcrumb-section="Onboarding Institucional"
    story-badge="Registro de Marca"
    :active-nav="shellNavId"
  >
    <template #header-actions>
      <div class="hidden sm:flex items-center gap-1.5 font-label-md text-xs">
        <button
          type="button"
          class="px-2.5 py-1 rounded-lg transition-colors"
          :class="currentStep === 'STEP_1_FORM' ? 'bg-primary text-on-primary font-semibold' : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'"
          @click="goToOnboardingStep('STEP_1_FORM')"
        >
          Paso 1
        </button>
        <button
          type="button"
          class="px-2.5 py-1 rounded-lg transition-colors"
          :class="currentStep === 'STEP_2_CLINICAL' ? 'bg-primary text-on-primary font-semibold' : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'"
          @click="goToOnboardingStep('STEP_2_CLINICAL')"
        >
          Paso 2
        </button>
        <button
          type="button"
          class="px-2.5 py-1 rounded-lg transition-colors"
          :class="currentStep === 'STEP_3_ACTIVATED' ? 'bg-primary text-on-primary font-semibold' : 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'"
          @click="goToOnboardingStep('STEP_3_ACTIVATED')"
        >
          Paso 3
        </button>
      </div>
    </template>

    <!-- ======================================================================= -->
    <!-- STEP 1: REGISTRO INICIAL DE CLÍNICA (Stitch: a7cb9437f160489fae71693e147652cb) -->
    <!-- ======================================================================= -->
    <div v-if="currentStep === 'STEP_1_FORM'" class="w-full">
      <div class="flex flex-col w-full">
        <div class="w-full max-w-2xl mx-auto py-space-md">
          <!-- Numbered Stepper Row -->
          <div class="mb-space-lg flex items-center justify-between max-w-lg mx-auto px-space-xs">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-full bg-primary text-on-primary font-semibold text-xs flex items-center justify-center shadow-sm">1</div>
              <span class="font-title-sm text-xs font-semibold text-primary hidden sm:inline">Paso 1: Registro</span>
            </div>
            <div class="flex-1 h-0.5 mx-2 sm:mx-3 bg-outline-variant/40"></div>
            <div class="flex items-center gap-2 cursor-pointer" @click="goToOnboardingStep('STEP_2_CLINICAL')">
              <div class="w-7 h-7 rounded-full bg-surface-container text-on-surface-variant font-semibold text-xs flex items-center justify-center border border-outline-variant/50">2</div>
              <span class="font-title-sm text-xs font-medium text-on-surface-variant hidden sm:inline">Paso 2: Configuración Clínica</span>
            </div>
            <div class="flex-1 h-0.5 mx-2 sm:mx-3 bg-outline-variant/40"></div>
            <div class="flex items-center gap-2 cursor-pointer" @click="goToOnboardingStep('STEP_3_ACTIVATED')">
              <div class="w-7 h-7 rounded-full bg-surface-container text-on-surface-variant font-semibold text-xs flex items-center justify-center border border-outline-variant/50">3</div>
              <span class="font-title-sm text-xs font-medium text-on-surface-variant hidden sm:inline">Paso 3: Verificación</span>
            </div>
          </div>

          <!-- Pill Progress Tracker -->
          <div class="w-full max-w-xl flex items-center justify-between gap-space-sm mb-space-lg bg-surface-container-low p-space-xs rounded-full shadow-sm">
            <div class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-secondary-container text-on-secondary-container shadow-sm">
              <span class="material-symbols-outlined text-[16px]">radio_button_checked</span>
              <span class="font-label-sm text-label-sm font-semibold">Paso 1: Registro</span>
            </div>
            <div class="h-0.5 w-6 bg-outline-variant/40 rounded-full hidden sm:block"></div>
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full text-on-surface-variant hover:text-on-surface transition-colors"
              @click="goToOnboardingStep('STEP_2_CLINICAL')"
            >
              <span class="material-symbols-outlined text-[16px] text-outline-variant">radio_button_unchecked</span>
              <span class="font-label-sm text-label-sm">Paso 2: Configuración</span>
            </button>
            <div class="h-0.5 w-6 bg-outline-variant/40 rounded-full hidden sm:block"></div>
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full text-on-surface-variant hover:text-on-surface transition-colors"
              @click="goToOnboardingStep('STEP_3_ACTIVATED')"
            >
              <span class="material-symbols-outlined text-[16px] text-outline-variant">radio_button_unchecked</span>
              <span class="font-label-sm text-label-sm">Paso 3: Verificación</span>
            </button>
          </div>

          <!-- Main Card -->
          <div class="relative bg-surface-container-lowest rounded-xl shadow-md p-6 sm:p-12 transition-all duration-300">
            <!-- Subtle top decorative ambiance accent -->
            <div class="absolute -top-12 left-1/2 -translate-x-1/2 w-64 h-24 bg-primary-fixed-dim/20 rounded-full blur-2xl pointer-events-none -z-10"></div>

            <!-- Header -->
            <div class="flex flex-col items-center text-center space-y-space-sm mb-space-xl">
              <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight mt-space-xs">
                Comencemos con tu clínica
              </h1>
              <p class="font-body-md text-body-md text-on-surface-variant max-w-md mx-auto leading-relaxed">
                Configura la identidad básica de tu centro médico y reserva tu portal institucional seguro en minutos.
              </p>
            </div>

            <!-- Main Form -->
            <form class="space-y-space-lg" @submit.prevent="handleStep1Submit">
              <!-- Field 1: Clinic Name -->
              <div class="space-y-space-xs">
                <label class="block font-title-sm text-title-sm text-on-surface" for="clinic-name">
                  Nombre de la Clínica
                </label>
                <div class="relative">
                  <input
                    id="clinic-name"
                    v-model="clinicName"
                    type="text"
                    placeholder="ej. Clínica San José"
                    class="w-full h-11 px-space-md bg-surface-container-low text-on-surface placeholder:text-outline font-body-md text-body-md rounded-lg outline-none focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary/20 transition-all"
                  />
                  <div class="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-on-surface-variant">
                    <span class="material-symbols-outlined text-[18px]">local_hospital</span>
                  </div>
                </div>
              </div>

              <!-- Field 2: Admin Email -->
              <div class="space-y-space-xs">
                <label class="block font-title-sm text-title-sm text-on-surface" for="admin-email">
                  Correo del Administrador
                </label>
                <div class="relative">
                  <input
                    id="admin-email"
                    v-model="adminEmail"
                    type="email"
                    placeholder="direccion@tucentro.com"
                    class="w-full h-11 px-space-md bg-surface-container-low text-on-surface placeholder:text-outline font-body-md text-body-md rounded-lg outline-none focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary/20 transition-all"
                  />
                  <div class="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-on-surface-variant">
                    <span class="material-symbols-outlined text-[18px]">mail</span>
                  </div>
                </div>
                <p class="font-body-sm text-body-sm text-on-surface-variant flex items-center gap-1.5 pt-0.5">
                  <span class="material-symbols-outlined text-[14px] text-primary">info</span>
                  Enviaremos aquí el acceso maestro y la verificación clínica oficial.
                </p>
              </div>

              <!-- Field 3: Phone Number -->
              <div class="space-y-space-xs">
                <label class="block font-title-sm text-title-sm text-on-surface" for="phone-number">
                  Teléfono de Contacto
                </label>
                <div class="flex items-center gap-space-xs">
                  <div class="h-11 px-space-sm bg-surface-container flex items-center gap-space-xs rounded-lg shrink-0 select-none">
                    <span class="text-[16px] leading-none">🇲🇽</span>
                    <span class="font-label-md text-label-md text-on-surface font-medium">+52</span>
                    <span class="material-symbols-outlined text-[16px] text-on-surface-variant">expand_more</span>
                  </div>
                  <div class="relative flex-1">
                    <input
                      id="phone-number"
                      v-model="phoneNumber"
                      type="tel"
                      placeholder="55 0000 0000"
                      class="w-full h-11 px-space-md bg-surface-container-low text-on-surface placeholder:text-outline font-body-md text-body-md rounded-lg outline-none focus:bg-surface-container-lowest focus:ring-2 focus:ring-primary/20 transition-all"
                    />
                    <div class="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-on-surface-variant">
                      <span class="material-symbols-outlined text-[18px]">call</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Field 4: Custom Subdomain & Live Collision Feedback -->
              <div class="space-y-space-xs pt-space-xs">
                <div class="flex items-center justify-between">
                  <label class="block font-title-sm text-title-sm text-on-surface" for="subdomain-input">
                    Subdominio institucional
                  </label>
                  <span class="font-label-sm text-label-sm text-primary uppercase font-semibold">Dirección Web</span>
                </div>
                <div class="flex items-center rounded-lg bg-surface-container-low overflow-hidden focus-within:bg-surface-container-lowest focus-within:ring-2 focus-within:ring-tertiary/20 transition-all">
                  <span class="px-space-md py-2.5 font-code-sm text-code-sm text-on-surface-variant select-none bg-surface-container">
                    https://
                  </span>
                  <input
                    id="subdomain-input"
                    v-model="slugInput"
                    type="text"
                    class="flex-1 h-11 px-space-sm bg-transparent text-on-surface font-code-sm text-code-sm outline-none tracking-tight"
                    @input="onSlugManualInput"
                  />
                  <span class="px-space-md py-2.5 font-code-sm text-code-sm text-primary bg-primary-fixed/40 select-none font-medium">
                    .medika.com
                  </span>
                </div>

                <!-- Gentle Collision / Soft Feedback Warning & Pill Suggestions -->
                <div
                  class="mt-space-sm p-space-md rounded-xl space-y-space-sm transition-all duration-300"
                  :class="hasSlugCollision ? 'bg-tertiary-fixed/30' : 'bg-secondary-container/30'"
                >
                  <div class="flex items-start gap-space-xs font-body-sm text-body-sm" :class="hasSlugCollision ? 'text-tertiary' : 'text-secondary'">
                    <span class="material-symbols-outlined text-[18px] shrink-0 translate-y-0.5">
                      {{ hasSlugCollision ? 'notification_important' : 'check_circle' }}
                    </span>
                    <div>
                      <p v-if="hasSlugCollision" class="font-semibold text-on-surface">
                        <span class="font-code-sm text-code-sm text-tertiary">{{ slugInput }}.medika.com</span> ya está reservado por otra institución.
                      </p>
                      <p v-else class="font-semibold text-on-surface">
                        <span class="font-code-sm text-code-sm text-secondary">{{ slugInput }}.medika.com</span> está disponible para tu clínica.
                      </p>
                      <p class="text-on-surface-variant mt-0.5">
                        Te sugerimos estas opciones disponibles al instante para tu centro:
                      </p>
                    </div>
                  </div>

                  <!-- Pill options -->
                  <div class="flex flex-wrap items-center gap-space-xs pt-1">
                    <button
                      v-for="suggestion in slugSuggestions"
                      :key="suggestion"
                      type="button"
                      class="group flex items-center gap-1.5 px-3 py-1.5 bg-surface-container-lowest text-on-surface hover:bg-secondary-container/40 rounded-full font-label-md text-label-md transition-all shadow-sm active:scale-95"
                      @click="applySubdomain(suggestion)"
                    >
                      <span class="material-symbols-outlined text-[14px] text-secondary group-hover:scale-110 transition-transform">add_circle</span>
                      <span class="font-code-sm text-code-sm font-medium">{{ suggestion }}</span>
                      <span class="text-on-surface-variant font-label-sm text-label-sm">.medika.com</span>
                    </button>
                  </div>

                  <div class="flex items-center justify-between pt-1">
                    <span class="font-label-sm text-label-sm text-on-surface-variant flex items-center gap-1">
                      <span class="material-symbols-outlined text-[13px]">touch_app</span>
                      Haz clic en una opción para seleccionarla directamente.
                    </span>
                    <span
                      v-if="!hasSlugCollision || slugSelectedFromSuggestion"
                      class="font-label-sm text-label-sm text-secondary font-semibold flex items-center gap-1"
                    >
                      <span class="material-symbols-outlined text-[14px]">check_circle</span>
                      Disponible
                    </span>
                  </div>
                </div>
              </div>

              <!-- Primary CTA -->
              <div class="pt-space-md space-y-space-md">
                <button
                  id="submit-button"
                  type="submit"
                  class="w-full py-3.5 px-space-lg bg-primary hover:bg-primary-container text-on-primary font-title-sm text-title-sm rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-space-xs group active:scale-[0.99]"
                >
                  <span>Continuar y Crear Clínica</span>
                  <span class="material-symbols-outlined text-[18px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
                </button>
                <p class="font-body-sm text-body-sm text-on-surface-variant text-center max-w-sm mx-auto leading-snug">
                  Al continuar, aceptas nuestros
                  <a class="text-primary hover:underline font-medium" href="#">Términos de Servicio Médico</a>
                  y la
                  <a class="text-primary hover:underline font-medium" href="#">Política de Privacidad de Datos Clínicos</a>.
                </p>
              </div>
            </form>

            <!-- Trust Row / Standards Badges -->
            <div class="mt-space-xl pt-space-md bg-surface-container-low rounded-xl px-space-md py-space-sm flex flex-col sm:flex-row items-center justify-between gap-space-xs text-on-surface-variant">
              <div class="flex items-center gap-1.5 font-label-sm text-label-sm">
                <span class="material-symbols-outlined text-[16px] text-secondary">lock</span>
                <span>Cifrado de grado médico TLS 1.3</span>
              </div>
              <div class="hidden sm:block text-outline-variant font-label-sm text-label-sm">•</div>
              <div class="flex items-center gap-1.5 font-label-sm text-label-sm">
                <span class="material-symbols-outlined text-[16px] text-primary">verified</span>
                <span>Cumple con NOM-024 y estándares internacionales</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================================= -->
    <!-- STEP 2: CONFIGURACIÓN CLÍNICA INICIAL (Stitch: a91ff07f636c4ac0beb8b2b39ca35974) -->
    <!-- ======================================================================= -->
    <div v-else-if="currentStep === 'STEP_2_CLINICAL'" class="w-full">
      <div class="flex flex-col w-full">
        <div class="w-full max-w-2xl mx-auto py-space-md flex flex-col items-center justify-center">
          <!-- Step Indicator Progress Tracker -->
          <div class="w-full max-w-xl flex items-center justify-between gap-space-sm mb-space-lg bg-surface-container-low p-space-xs rounded-full shadow-sm">
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-surface-container-lowest shadow-sm"
              @click="goToOnboardingStep('STEP_1_FORM')"
            >
              <span class="material-symbols-outlined text-[16px] text-secondary" style="font-variation-settings: 'FILL' 1;">check_circle</span>
              <span class="font-label-sm text-label-sm text-on-surface">Paso 1: Registro</span>
            </button>
            <div class="h-0.5 w-6 bg-secondary-container rounded-full hidden sm:block"></div>
            <div class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-secondary-container text-on-secondary-container shadow-sm">
              <span class="material-symbols-outlined text-[16px]">radio_button_checked</span>
              <span class="font-label-sm text-label-sm font-semibold">Paso 2: Configuración</span>
            </div>
            <div class="h-0.5 w-6 bg-outline-variant/40 rounded-full hidden sm:block"></div>
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full text-on-surface-variant"
              @click="goToOnboardingStep('STEP_3_ACTIVATED')"
            >
              <span class="material-symbols-outlined text-[16px] text-outline-variant">radio_button_unchecked</span>
              <span class="font-label-sm text-label-sm">Paso 3: Verificación</span>
            </button>
          </div>

          <!-- Main Card Container -->
          <div class="w-full max-w-xl bg-surface-container-lowest rounded-[1rem] shadow-sm p-8 sm:p-10 flex flex-col">
            <!-- Headings -->
            <div class="flex flex-col items-center text-center">
              <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight mb-space-xs">
                Configura tu primera sede
              </h1>
              <p class="font-body-md text-body-md text-on-surface-variant max-w-md">
                Define la ubicación física de atención y las especialidades iniciales de tu centro médico.
              </p>
            </div>

            <!-- Clinical Setup Form -->
            <form class="mt-space-xl flex flex-col gap-space-lg" @submit.prevent="handleStep2Submit">
              <!-- Field 1: Sede Principal -->
              <div class="flex flex-col gap-space-xs">
                <label class="font-title-sm text-title-sm text-on-surface flex items-center justify-between" for="sede-nombre">
                  <span>Nombre de la Sede Principal</span>
                  <span class="font-label-sm text-label-sm text-on-surface-variant font-normal">Requerido</span>
                </label>
                <div class="relative flex items-center">
                  <span class="material-symbols-outlined absolute left-3.5 text-outline text-[20px] pointer-events-none">domain</span>
                  <input
                    id="sede-nombre"
                    v-model="mainBranchName"
                    type="text"
                    class="w-full pl-11 pr-4 py-2.5 bg-surface-container-lowest text-on-surface font-body-md text-body-md rounded-lg shadow-sm border border-outline-variant/30 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-outline-variant"
                  />
                </div>
              </div>

              <!-- Field 2: Dirección Física -->
              <div class="flex flex-col gap-space-xs">
                <label class="font-title-sm text-title-sm text-on-surface flex items-center justify-between" for="sede-direccion">
                  <span>Dirección Física</span>
                  <span class="font-label-sm text-label-sm text-on-surface-variant font-normal">Para geolocalización de pacientes</span>
                </label>
                <div class="relative flex items-center">
                  <span class="material-symbols-outlined absolute left-3.5 text-outline text-[20px] pointer-events-none">pin_drop</span>
                  <input
                    id="sede-direccion"
                    v-model="physicalAddress"
                    type="text"
                    placeholder="Av. Paseo de la Reforma 222, Cuauhtémoc, CDMX"
                    class="w-full pl-11 pr-4 py-2.5 bg-surface-container-lowest text-on-surface font-body-md text-body-md rounded-lg shadow-sm border border-outline-variant/30 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all placeholder:text-outline-variant"
                  />
                </div>
              </div>

              <!-- Field 3: Consultorios / Capacidad -->
              <div class="flex flex-col gap-space-xs">
                <label class="font-title-sm text-title-sm text-on-surface">
                  Consultorios o Espacios Disponibles
                </label>
                <div class="grid grid-cols-3 gap-space-sm pt-space-xs">
                  <label class="cursor-pointer">
                    <input v-model="capacityTier" class="peer sr-only" name="capacity" type="radio" value="1" />
                    <div class="flex flex-col items-center justify-center py-3 px-2 rounded-lg bg-surface-container-low peer-checked:bg-primary-container peer-checked:text-on-primary-container text-on-surface transition-all">
                      <span class="material-symbols-outlined text-[20px] mb-1">meeting_room</span>
                      <span class="font-label-md text-label-md">1 consultorio</span>
                    </div>
                  </label>

                  <label class="cursor-pointer">
                    <input v-model="capacityTier" class="peer sr-only" name="capacity" type="radio" value="2-5" />
                    <div class="flex flex-col items-center justify-center py-3 px-2 rounded-lg bg-surface-container-low peer-checked:bg-primary-container peer-checked:text-on-primary-container text-on-surface transition-all">
                      <div class="flex items-center gap-1 mb-1">
                        <span class="material-symbols-outlined text-[20px]">door_front</span>
                        <span v-if="capacityTier === '2-5'" class="material-symbols-outlined text-[16px] text-secondary-fixed">check_circle</span>
                      </div>
                      <span class="font-label-md text-label-md font-medium">2 - 5 consultorios</span>
                    </div>
                  </label>

                  <label class="cursor-pointer">
                    <input v-model="capacityTier" class="peer sr-only" name="capacity" type="radio" value="6+" />
                    <div class="flex flex-col items-center justify-center py-3 px-2 rounded-lg bg-surface-container-low peer-checked:bg-primary-container peer-checked:text-on-primary-container text-on-surface transition-all">
                      <span class="material-symbols-outlined text-[20px] mb-1">corporate_fare</span>
                      <span class="font-label-md text-label-md">6+ consultorios</span>
                    </div>
                  </label>
                </div>
              </div>

              <!-- Field 4: Especialidades Médicas Iniciales -->
              <div class="flex flex-col gap-space-xs">
                <div class="flex items-center justify-between">
                  <label class="font-title-sm text-title-sm text-on-surface">
                    Especialidades Médicas Iniciales
                  </label>
                  <span class="font-label-sm text-label-sm text-on-surface-variant font-normal">Selecciona las activas</span>
                </div>
                <p class="font-body-sm text-body-sm text-on-surface-variant mb-space-xs">
                  Habilita los módulos clínicos y catálogos de diagnóstico correspondientes.
                </p>

                <div class="flex flex-wrap gap-2 pt-1">
                  <button
                    v-for="spec in specialties"
                    :key="spec.name"
                    type="button"
                    class="inline-flex items-center gap-2 px-3 py-2 rounded-lg transition-colors select-none cursor-pointer"
                    :class="spec.active ? 'bg-surface-container text-primary hover:bg-surface-variant' : 'bg-surface-container-low text-outline hover:bg-surface-container'"
                    @click="toggleSpecialty(spec)"
                  >
                    <span class="material-symbols-outlined text-[18px]" :class="spec.active ? 'text-primary' : 'text-outline'">
                      {{ spec.active ? 'check_box' : 'check_box_outline_blank' }}
                    </span>
                    <span class="font-label-md text-label-md font-medium" :class="spec.active ? 'text-on-surface' : 'text-on-surface-variant'">
                      {{ spec.name }}
                    </span>
                  </button>

                  <!-- Add Specialty Trigger -->
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-surface-container-low text-primary hover:bg-surface-container font-label-md text-label-md font-medium transition-all"
                    @click="isCustomSpecialtyModalOpen = true"
                  >
                    <span class="material-symbols-outlined text-[18px]">add</span>
                    <span>Agregar otra especialidad</span>
                  </button>
                </div>
              </div>

              <!-- Primary Action Button -->
              <div class="pt-space-md flex flex-col gap-space-md">
                <button
                  type="submit"
                  :disabled="isSubmitting"
                  class="w-full h-11 bg-primary text-on-primary font-title-sm text-title-sm rounded-lg hover:bg-primary-container transition-all flex items-center justify-center gap-space-xs shadow-sm hover:shadow"
                >
                  <span>{{ isSubmitting ? 'Activando Portal...' : 'Continuar a Verificación y Activación' }}</span>
                  <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                </button>
                <button
                  type="button"
                  class="inline-flex items-center justify-center gap-space-xs font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors py-1"
                  @click="goToOnboardingStep('STEP_1_FORM')"
                >
                  <span class="material-symbols-outlined text-[16px]">arrow_back</span>
                  <span>Volver al paso anterior</span>
                </button>
              </div>
            </form>

            <!-- Security & Privacy Card -->
            <div class="mt-space-xl p-space-md bg-surface-container-low rounded-lg flex items-center gap-space-md">
              <div class="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center shrink-0">
                <span class="material-symbols-outlined text-secondary text-[20px]">lock</span>
              </div>
              <div class="flex flex-col">
                <span class="font-title-sm text-title-sm text-on-surface">Cifrado de datos médicos TLS 1.3</span>
                <span class="font-body-sm text-body-sm text-on-surface-variant">
                  Cumple estrictamente con NOM-024-SSA3-2012 y estándares internacionales de salud digital.
                </span>
              </div>
            </div>
          </div>

          <!-- Quick Modal for Custom Specialty Addition -->
          <div
            v-if="isCustomSpecialtyModalOpen"
            class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-inverse-surface/40 backdrop-blur-sm"
          >
            <div class="w-full max-w-sm bg-surface-container-lowest rounded-xl p-6 shadow-xl flex flex-col gap-space-md">
              <div class="flex items-center justify-between">
                <h2 class="font-headline-sm text-headline-sm text-on-surface">Nueva Especialidad</h2>
                <button type="button" class="text-outline hover:text-on-surface" @click="isCustomSpecialtyModalOpen = false">
                  <span class="material-symbols-outlined">close</span>
                </button>
              </div>
              <div class="flex flex-col gap-1">
                <label class="font-label-md text-label-md text-on-surface-variant">Nombre de la disciplina médica</label>
                <input
                  v-model="newSpecialtyInput"
                  type="text"
                  placeholder="Ej. Dermatología, Neurología"
                  class="w-full px-3 py-2 bg-surface-container-low rounded-lg text-on-surface font-body-md text-body-md focus:outline-none focus:ring-2 focus:ring-primary/20"
                  @keyup.enter="confirmAddSpecialty"
                />
              </div>
              <div class="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  class="px-4 py-2 font-label-md text-label-md text-on-surface-variant hover:text-on-surface"
                  @click="isCustomSpecialtyModalOpen = false"
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  class="px-4 py-2 bg-primary text-on-primary rounded-lg font-label-md text-label-md font-medium hover:bg-primary-container"
                  @click="confirmAddSpecialty"
                >
                  Agregar
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================================= -->
    <!-- STEP 3: CONFIRMACIÓN Y ACTIVACIÓN DEL PORTAL (Stitch: 1e9dc7ddc5ba44a69903cefeef1608f0) -->
    <!-- ======================================================================= -->
    <div v-else class="w-full">
      <div class="flex flex-col w-full">
        <div class="max-w-2xl mx-auto py-space-md w-full flex flex-col items-center">
          <!-- Wizard Step Tracker (Clean Pill Flow) -->
          <div class="w-full flex items-center justify-between gap-space-sm mb-10 bg-surface-container-low p-space-xs rounded-full shadow-sm">
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-surface-container-lowest shadow-sm"
              @click="goToOnboardingStep('STEP_1_FORM')"
            >
              <span class="material-symbols-outlined text-[16px] text-secondary" style="font-variation-settings: 'FILL' 1;">check_circle</span>
              <span class="font-label-sm text-label-sm text-on-surface">Paso 1: Registro</span>
            </button>
            <div class="h-0.5 w-6 bg-secondary-container rounded-full hidden sm:block"></div>
            <button
              type="button"
              class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-surface-container-lowest shadow-sm"
              @click="goToOnboardingStep('STEP_2_CLINICAL')"
            >
              <span class="material-symbols-outlined text-[16px] text-secondary" style="font-variation-settings: 'FILL' 1;">check_circle</span>
              <span class="font-label-sm text-label-sm text-on-surface">Paso 2: Configuración</span>
            </button>
            <div class="h-0.5 w-6 bg-secondary-container rounded-full hidden sm:block"></div>
            <div class="flex items-center gap-space-xs px-3 py-1.5 rounded-full bg-secondary-container text-on-secondary-container shadow-sm">
              <span class="material-symbols-outlined text-[16px] text-secondary" style="font-variation-settings: 'FILL' 1;">check_circle</span>
              <span class="font-label-sm text-label-sm font-semibold text-on-secondary-container">Paso 3: Activo</span>
            </div>
          </div>

          <!-- Celebration / Hero Graphic -->
          <div class="relative flex items-center justify-center mb-6">
            <div class="absolute w-28 h-28 rounded-full bg-secondary-container opacity-40 blur-xl animate-pulse"></div>
            <div class="relative w-20 h-20 rounded-full bg-secondary-fixed flex items-center justify-center text-on-secondary-fixed shadow-md ring-8 ring-surface-container-low">
              <span class="material-symbols-outlined text-[42px]" style="font-variation-settings: 'FILL' 1;">check</span>
            </div>
          </div>

          <!-- Headline & Subtitle -->
          <div class="text-center space-y-2 mb-8">
            <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-surface-container text-primary font-label-sm text-label-sm uppercase tracking-wide font-bold">
              <span>Aprovisionamiento Exitoso</span>
            </div>
            <h1 class="font-headline-lg text-headline-lg text-on-surface tracking-tight">
              ¡Tu portal clínico está listo!
            </h1>
            <p class="font-body-md text-body-md text-on-surface-variant max-w-lg mx-auto">
              El entorno médico institucional ha sido aprovisionado correctamente y ya se encuentra disponible para tu equipo de especialistas y pacientes.
            </p>
          </div>

          <!-- Active URL Pill Box -->
          <div class="w-full bg-surface-container-lowest p-space-md rounded-xl shadow-sm mb-6 flex flex-col sm:flex-row items-center justify-between gap-space-md">
            <div class="flex items-center gap-space-sm min-w-0 w-full sm:w-auto">
              <div class="w-9 h-9 rounded-lg bg-surface-container flex items-center justify-center text-secondary shrink-0">
                <span class="material-symbols-outlined text-[20px]">lock</span>
              </div>
              <div class="min-w-0 flex flex-col">
                <div class="flex items-center gap-1.5">
                  <span class="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
                  <span class="font-label-sm text-label-sm text-secondary font-semibold uppercase tracking-wider">En línea</span>
                </div>
                <span class="font-code-sm text-code-sm text-on-surface font-medium truncate">
                  {{ activePortalUrl }}
                </span>
              </div>
            </div>

            <div class="flex items-center gap-space-xs w-full sm:w-auto justify-end">
              <button
                type="button"
                class="flex items-center gap-1.5 px-3 py-2 rounded-lg transition-colors font-label-md text-label-md cursor-pointer"
                :class="copiedPortalUrl ? 'bg-secondary-container text-on-secondary-container' : 'bg-surface-container hover:bg-surface-container-high text-on-surface'"
                @click="copyDomain"
              >
                <span class="material-symbols-outlined text-[18px]">content_copy</span>
                <span>{{ copiedPortalUrl ? '¡Copiado!' : 'Copiar enlace' }}</span>
              </button>
              <NuxtLink
                to="/admin/branches"
                class="flex items-center gap-1 px-3.5 py-2 rounded-lg bg-primary hover:bg-primary-container text-on-primary font-label-md text-label-md shadow-sm transition-colors"
              >
                <span>Visitar portal</span>
                <span class="material-symbols-outlined text-[16px]">open_in_new</span>
              </NuxtLink>
            </div>
          </div>

          <!-- Email Delivery Notice Card -->
          <div class="w-full bg-surface-container-low rounded-xl p-space-md flex items-start gap-space-md mb-8">
            <div class="w-8 h-8 rounded-full bg-primary-fixed flex items-center justify-center text-on-primary-fixed shrink-0 mt-0.5">
              <span class="material-symbols-outlined text-[18px]">mark_email_read</span>
            </div>
            <div class="text-left space-y-0.5">
              <p class="font-title-sm text-title-sm text-on-surface">Credenciales maestras enviadas</p>
              <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                Hemos enviado el enlace de activación y acceso maestro a
                <strong class="text-on-surface font-medium">{{ adminEmail }}</strong>
                con las credenciales iniciales de seguridad y guía de auditoría clínica.
              </p>
            </div>
          </div>

          <!-- Next-Step Cards Grid -->
          <div class="w-full mb-8">
            <div class="text-left mb-3">
              <span class="font-label-sm text-label-sm uppercase tracking-wider text-outline font-semibold">Siguientes pasos recomendados</span>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-space-md w-full">
              <!-- Step 1 Card -->
              <div class="group bg-surface-container-lowest p-space-lg rounded-xl shadow-sm hover:shadow-md transition-all flex flex-col justify-between text-left">
                <div class="space-y-space-sm">
                  <div class="w-10 h-10 rounded-lg bg-primary-fixed flex items-center justify-center text-primary group-hover:scale-105 transition-transform">
                    <span class="material-symbols-outlined text-[22px]">domain</span>
                  </div>
                  <div class="space-y-1">
                    <h3 class="font-title-md text-title-md text-on-surface group-hover:text-primary transition-colors">
                      1. Sedes y Consultorios
                    </h3>
                    <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                      Define consultorios físicos, horarios de atención y asigna los espacios a tus médicos especialistas.
                    </p>
                  </div>
                </div>
                <div class="pt-space-md mt-space-md">
                  <NuxtLink
                    to="/admin/branches"
                    class="inline-flex items-center gap-1 font-label-md text-label-md text-primary font-semibold hover:gap-2 transition-all"
                  >
                    <span>Ir a Sedes y Consultorios</span>
                    <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </NuxtLink>
                </div>
              </div>

              <!-- Step 2 Card -->
              <div class="group bg-surface-container-lowest p-space-lg rounded-xl shadow-sm hover:shadow-md transition-all flex flex-col justify-between text-left">
                <div class="space-y-space-sm">
                  <div class="w-10 h-10 rounded-lg bg-secondary-fixed flex items-center justify-center text-secondary group-hover:scale-105 transition-transform">
                    <span class="material-symbols-outlined text-[22px]">medical_services</span>
                  </div>
                  <div class="space-y-1">
                    <h3 class="font-title-md text-title-md text-on-surface group-hover:text-secondary transition-colors">
                      2. Catálogo de Servicios
                    </h3>
                    <p class="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                      Establece aranceles, consultas médicas, duraciones de cita y procedimientos especializados hospitalarios.
                    </p>
                  </div>
                </div>
                <div class="pt-space-md mt-space-md">
                  <NuxtLink
                    to="/doctor/services"
                    class="inline-flex items-center gap-1 font-label-md text-label-md text-secondary font-semibold hover:gap-2 transition-all"
                  >
                    <span>Configurar Servicios</span>
                    <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </NuxtLink>
                </div>
              </div>
            </div>
          </div>

          <!-- Quick Supplemental Actions -->
          <div class="w-full flex flex-col sm:flex-row items-center justify-center gap-space-lg pt-space-xs pb-space-lg text-center">
            <button type="button" class="inline-flex items-center gap-1.5 font-label-md text-label-md text-on-surface-variant hover:text-primary transition-colors cursor-pointer">
              <span class="material-symbols-outlined text-[18px]">picture_as_pdf</span>
              <span>Descargar ficha técnica de activación (PDF)</span>
            </button>
            <span class="hidden sm:inline text-outline-variant font-body-sm">•</span>
            <NuxtLink
              to="/admin/staff"
              class="inline-flex items-center gap-1.5 font-label-md text-label-md text-on-surface-variant hover:text-primary transition-colors cursor-pointer"
            >
              <span class="material-symbols-outlined text-[18px]">group_add</span>
              <span>Invitar administradores y médicos</span>
            </NuxtLink>
          </div>

          <!-- Institutional Compliance / Trust Badge -->
          <div class="w-full bg-surface-container-lowest/60 rounded-lg py-3 px-4 flex items-center justify-center gap-2 text-center text-outline">
            <span class="material-symbols-outlined text-[16px] text-secondary">verified_user</span>
            <span class="font-label-sm text-label-sm tracking-tight text-on-surface-variant">
              Cifrado de grado médico TLS 1.3 • Cumple con NOM-024-SSA3-2012 y estándares HIPAA
            </span>
          </div>
        </div>
      </div>
    </div>
  </MedikaAppShell>
</template>
