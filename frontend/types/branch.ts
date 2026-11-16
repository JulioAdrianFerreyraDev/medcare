import type { components } from './openapi'

/**
 * Auto-derived from Spring Boot OpenAPI 3 specification (/api/v1/api-docs -> components.schemas)
 */
export type CreateBranchApiRequest = components['schemas']['CreateBranchRequest']
export type BackendBranchResponse = Required<components['schemas']['BranchResponse']>
export type CreateRoomApiRequest = components['schemas']['CreateRoomRequest']
export type BackendRoomResponse = Required<components['schemas']['PhysicalRoomResponse']>
export type AssignStaffApiRequest = components['schemas']['AssignStaffRequest']
export type BackendStaffAssignmentResponse = Required<components['schemas']['StaffAssignmentResponse']>
export type BranchSchedulingMatrixResponse = Required<components['schemas']['BranchSchedulingMatrixResponse']>

export interface CreateBranchRequest {
  name: string
  code?: string
  address: string
  phone: string
  timeZone: string
  isMainBranch?: boolean
}

export interface BranchResponse {
  branchId: string
  brandId: string
  name: string
  code: string
  address: string
  phone: string
  timeZone: string
  status: string
  activeRoomsCount?: number
}

export interface CreateRoomRequest {
  name: string
  code: string
  roomType?: string
  floorOrZone?: string
  equipmentNotes?: string
}

export interface ConsultationRoomResponse {
  roomId: string
  branchId: string
  name: string
  code: string
  roomType?: string
  maxConcurrentConsultations: number
  status: string
}

export interface AssignStaffBranchRequest {
  staffUserId: string
  staffFullName: string
  role: 'DOCTOR' | 'RECEPTIONIST' | 'BRAND_ADMIN'
  branchId: string
  specialty?: string
}
