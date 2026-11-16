import type {
  BackendBrandOnboardingResponse,
  BrandOnboardingRequest,
  BrandResponse
} from '~/types/tenancy'

function mapBackendBrand(dto: BackendBrandOnboardingResponse): BrandResponse {
  return {
    brandId: dto.id,
    legalName: dto.legalBusinessName,
    displayName: dto.legalBusinessName,
    slug: dto.slug,
    portalUrl: `https://${dto.subdomainHost}`,
    status: dto.status,
    contactEmail: dto.contactEmail,
    contactPhone: dto.contactPhone,
    activationEmailDispatched: dto.activationEmailDispatched
  }
}

export function useTenancyApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl

  async function onboardBrand(payload: BrandOnboardingRequest): Promise<BrandResponse> {
    const dto = await $fetch<BackendBrandOnboardingResponse>(`${baseUrl}/tenants`, {
      method: 'POST',
      body: {
        legalBusinessName: payload.legalName,
        contactEmail: payload.ownerEmail,
        contactPhone: payload.contactPhone || payload.supportPhone || '+52 55 1234 5678',
        slug: payload.preferredSlug
      }
    })
    return mapBackendBrand(dto)
  }

  async function resolveBrandBySlug(slug: string): Promise<BrandResponse> {
    const dto = await $fetch<BackendBrandOnboardingResponse>(`${baseUrl}/tenants/current`, {
      method: 'GET',
      headers: {
        'X-Tenant-Slug': slug
      }
    })
    return mapBackendBrand(dto)
  }

  return {
    onboardBrand,
    resolveBrandBySlug
  }
}
