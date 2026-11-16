import type { components } from './openapi'

/**
 * Auto-derived from Spring Boot OpenAPI 3 specification (/api/v1/api-docs -> components.schemas)
 * Domain: Provider Availability, Working Shifts, Planned Leave & Emergency Cancellations (AHC-6 & AHC-7)
 */
export type ShiftWindowInput = components['schemas']['ShiftWindowInput']
export type ConfigureWeeklyShiftTemplateCommand = components['schemas']['ConfigureWeeklyShiftTemplateCommand']
export type ProviderShiftResponse = Required<components['schemas']['ProviderShiftResponse']>
export type BookableSlotResponse = Required<components['schemas']['BookableSlotResponse']>
export type PlannedLeaveCommand = components['schemas']['PlannedLeaveCommand']
export type EmergencyCancellationCommand = components['schemas']['EmergencyCancellationCommand']
export type TimeOffBlockResponse = Required<components['schemas']['TimeOffBlockResponse']>
export type CancellationAuditLogResponse = Required<components['schemas']['CancellationAuditLogResponse']>
export type ReceptionistAlertResponse = Required<components['schemas']['ReceptionistAlertResponse']>
