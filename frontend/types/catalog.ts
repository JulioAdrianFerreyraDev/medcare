import type { components } from './openapi'

/**
 * Auto-derived from Spring Boot OpenAPI 3 specification (/api/v1/api-docs -> components.schemas)
 */
export type CreateServiceApiRequest = components['schemas']['CreateServiceRequest']
export type UpdateServiceStatusApiRequest = components['schemas']['UpdateServiceStatusRequest']
export type BackendClinicalServiceResponse = Required<components['schemas']['ClinicalServiceResponse']>
export type BookAppointmentRequest = components['schemas']['BookServiceAppointmentRequest']
export type ServiceAppointmentResponse = Required<components['schemas']['ServiceAppointmentResponse']>

export interface UpsertClinicalServiceRequest {
  serviceId?: string
  name: string
  description?: string
  specialty?: string
  specialtyCode?: string
  durationMinutes: number
  priceCents: number
  feeAmount?: number
  currencyCode: string
  isActive: boolean
}

export interface ClinicalServiceResponse {
  serviceId: string
  brandId: string
  providerUserId: string
  name: string
  specialty: string
  description: string
  durationMinutes: number
  priceCents: number
  feeAmount: number
  currencyCode: string
  isActive: boolean
}
