import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { BrandResponse } from '~/types/tenancy'

export const useTenancyStore = defineStore('tenancy', () => {
  const activeBrand = ref<BrandResponse | null>({
    brandId: '00000000-0000-0000-0000-000000000001',
    legalName: 'Clínica Irma S.A. de C.V.',
    displayName: 'Clínica Irma',
    slug: 'clinica-irma',
    portalUrl: 'https://clinica-irma.medika.com',
    status: 'ACTIVE'
  })

  const resolvedSlug = ref<string>('clinica-irma')

  function setActiveBrand(brand: BrandResponse) {
    activeBrand.value = brand
    resolvedSlug.value = brand.slug
  }

  function setResolvedSlug(slug: string) {
    resolvedSlug.value = slug
  }

  function clearActiveBrand() {
    activeBrand.value = null
  }

  return {
    activeBrand,
    resolvedSlug,
    setActiveBrand,
    setResolvedSlug,
    clearActiveBrand
  }
})
