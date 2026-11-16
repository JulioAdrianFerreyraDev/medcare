import type {
  BackendClinicalServiceResponse,
  BookAppointmentRequest,
  ClinicalServiceResponse,
  ServiceAppointmentResponse,
  UpsertClinicalServiceRequest
} from '~/types/catalog'

function mapBackendService(
  dto: BackendClinicalServiceResponse,
  tenantSlug = 'clinica-irma'
): ClinicalServiceResponse {
  const fee = Number(dto.feeAmount ?? 0)
  return {
    serviceId: dto.id,
    brandId: tenantSlug,
    providerUserId: dto.providerId,
    name: dto.name,
    specialty: dto.specialty,
    description: `${dto.specialty} · Duración continua de ${dto.durationMinutes} min`,
    durationMinutes: dto.durationMinutes,
    priceCents: Math.round(fee * 100),
    feeAmount: fee,
    currencyCode: dto.currency || 'MXN',
    isActive: dto.status === 'ACTIVE'
  }
}

export function useCatalogApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl

  async function listProviderServices(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega',
    activeOnly = false
  ): Promise<ClinicalServiceResponse[]> {
    const list = await $fetch<BackendClinicalServiceResponse[]>(
      `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/services`,
      {
        method: 'GET',
        query: { activeOnly },
        headers: { 'X-Tenant-Slug': tenantSlug }
      }
    )
    return list.map((item) => mapBackendService(item, tenantSlug))
  }

  async function upsertProviderService(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega',
    payload: UpsertClinicalServiceRequest
  ): Promise<ClinicalServiceResponse> {
    const feeAmount = payload.feeAmount ?? Math.round((payload.priceCents || 0) / 100)
    const isUuid =
      payload.serviceId &&
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(payload.serviceId)

    const url = isUuid
      ? `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/services/${encodeURIComponent(payload.serviceId!)}`
      : `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/services`

    const dto = await $fetch<BackendClinicalServiceResponse>(url, {
      method: isUuid ? 'PUT' : 'POST',
      headers: { 'X-Tenant-Slug': tenantSlug },
      body: {
        name: payload.name,
        specialty: payload.specialty || 'Cardiología',
        durationMinutes: payload.durationMinutes,
        feeAmount,
        currency: payload.currencyCode || 'MXN'
      }
    })
    return mapBackendService(dto, tenantSlug)
  }

  async function updateProviderServiceStatus(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega',
    serviceId: string,
    isActive: boolean
  ): Promise<ClinicalServiceResponse> {
    const dto = await $fetch<BackendClinicalServiceResponse>(
      `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/services/${encodeURIComponent(serviceId)}/status`,
      {
        method: 'PATCH',
        headers: { 'X-Tenant-Slug': tenantSlug },
        body: {
          status: isActive ? 'ACTIVE' : 'INACTIVE'
        }
      }
    )
    return mapBackendService(dto, tenantSlug)
  }

  async function deactivateProviderService(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega',
    serviceId: string
  ): Promise<void> {
    await updateProviderServiceStatus(tenantSlug, providerUserId, serviceId, false)
  }

  async function listProviderAppointments(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega'
  ): Promise<ServiceAppointmentResponse[]> {
    return await $fetch<ServiceAppointmentResponse[]>(
      `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/appointments`,
      {
        method: 'GET',
        headers: { 'X-Tenant-Slug': tenantSlug }
      }
    )
  }

  async function bookServiceAppointment(
    tenantSlug = 'clinica-irma',
    providerUserId = 'dr-alejandro-vega',
    payload: BookAppointmentRequest
  ): Promise<ServiceAppointmentResponse> {
    return await $fetch<ServiceAppointmentResponse>(
      `${baseUrl}/providers/${encodeURIComponent(providerUserId)}/appointments`,
      {
        method: 'POST',
        headers: { 'X-Tenant-Slug': tenantSlug },
        body: payload
      }
    )
  }

  return {
    listProviderServices,
    upsertProviderService,
    updateProviderServiceStatus,
    deactivateProviderService,
    listProviderAppointments,
    bookServiceAppointment
  }
}
