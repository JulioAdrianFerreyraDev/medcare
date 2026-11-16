import type {
  AssignStaffBranchRequest,
  BackendBranchResponse,
  BackendRoomResponse,
  BackendStaffAssignmentResponse,
  BranchResponse,
  ConsultationRoomResponse,
  CreateBranchRequest,
  CreateRoomRequest
} from '~/types/branch'

function mapBackendBranch(dto: BackendBranchResponse, index = 0): BranchResponse {
  return {
    branchId: dto.id,
    brandId: dto.tenantSlug,
    name: dto.name,
    code: `SED-MX-CDMX-00${index + 1}`,
    address: dto.address,
    phone: dto.contactPhone,
    timeZone: dto.timezone,
    status: dto.active ? 'ACTIVE' : 'INACTIVE'
  }
}

function mapBackendRoom(dto: BackendRoomResponse): ConsultationRoomResponse {
  return {
    roomId: dto.id,
    branchId: dto.branchId,
    name: dto.name,
    code: dto.roomType ? `${dto.roomType.slice(0, 3).toUpperCase()}-01` : 'CON-01',
    roomType: dto.roomType,
    maxConcurrentConsultations: dto.concurrencyCapacity,
    status: 'AVAILABLE'
  }
}

export function useBranchApi() {
  const config = useRuntimeConfig()
  const baseUrl = config.public.apiBaseUrl

  async function listBranches(tenantSlug = 'clinica-irma'): Promise<BranchResponse[]> {
    const list = await $fetch<BackendBranchResponse[]>(`${baseUrl}/branches`, {
      method: 'GET',
      headers: { 'X-Tenant-Slug': tenantSlug }
    })
    return list.map((b, idx) => mapBackendBranch(b, idx))
  }

  async function createBranch(
    tenantSlug = 'clinica-irma',
    payload: CreateBranchRequest
  ): Promise<BranchResponse> {
    const dto = await $fetch<BackendBranchResponse>(`${baseUrl}/branches`, {
      method: 'POST',
      headers: { 'X-Tenant-Slug': tenantSlug },
      body: {
        name: payload.name,
        address: payload.address,
        timezone: payload.timeZone || 'America/Mexico_City',
        contactPhone: payload.phone
      }
    })
    return mapBackendBranch(dto)
  }

  async function listRooms(
    tenantSlug = 'clinica-irma',
    branchId: string
  ): Promise<ConsultationRoomResponse[]> {
    const list = await $fetch<BackendRoomResponse[]>(
      `${baseUrl}/branches/${encodeURIComponent(branchId)}/rooms`,
      {
        method: 'GET',
        headers: { 'X-Tenant-Slug': tenantSlug }
      }
    )
    return list.map(mapBackendRoom)
  }

  async function createRoom(
    tenantSlug = 'clinica-irma',
    branchId: string,
    payload: CreateRoomRequest
  ): Promise<ConsultationRoomResponse> {
    const dto = await $fetch<BackendRoomResponse>(
      `${baseUrl}/branches/${encodeURIComponent(branchId)}/rooms`,
      {
        method: 'POST',
        headers: { 'X-Tenant-Slug': tenantSlug },
        body: {
          name: payload.name,
          roomType: payload.roomType || payload.code || 'GENERAL',
          concurrencyCapacity: 1
        }
      }
    )
    return mapBackendRoom(dto)
  }

  async function listStaffAssignments(
    tenantSlug = 'clinica-irma'
  ): Promise<BackendStaffAssignmentResponse[]> {
    return await $fetch<BackendStaffAssignmentResponse[]>(`${baseUrl}/branches/staff-assignments`, {
      method: 'GET',
      headers: { 'X-Tenant-Slug': tenantSlug }
    })
  }

  async function assignStaffToBranch(
    tenantSlug = 'clinica-irma',
    payload: AssignStaffBranchRequest
  ): Promise<BackendStaffAssignmentResponse> {
    return await $fetch<BackendStaffAssignmentResponse>(`${baseUrl}/branches/staff-assignments`, {
      method: 'POST',
      headers: { 'X-Tenant-Slug': tenantSlug },
      body: {
        staffId: payload.staffUserId,
        staffName: payload.staffFullName,
        role: payload.role,
        branchId: payload.branchId
      }
    })
  }

  return {
    listBranches,
    createBranch,
    listRooms,
    createRoom,
    listStaffAssignments,
    assignStaffToBranch
  }
}
