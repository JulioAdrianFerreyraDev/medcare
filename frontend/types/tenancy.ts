import type { components } from './openapi'

/**
 * Auto-derived from Spring Boot OpenAPI 3 specification (/api/v1/api-docs -> components.schemas)
 */
export type TenantOnboardingApiRequest = components['schemas']['TenantOnboardingRequest']
export type BackendBrandOnboardingResponse = Required<components['schemas']['BrandOnboardingResponse']>

export interface BrandOnboardingRequest {
  legalName: string
  displayName?: string
  preferredSlug: string
  initialBranchName?: string
  initialBranchTimeZone?: string
  ownerEmail: string
  ownerFullName?: string
  contactPhone?: string
  taxId?: string
  supportEmail?: string
  supportPhone?: string
}

export interface BrandResponse {
  brandId: string
  legalName: string
  displayName: string
  slug: string
  portalUrl: string
  status: string
  contactEmail?: string
  contactPhone?: string
  activationEmailDispatched?: boolean
  defaultBranchId?: string
  createdAt?: string
}
