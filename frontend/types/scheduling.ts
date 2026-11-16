import type { components } from './openapi'

/**
 * Auto-derived from Spring Boot OpenAPI 3 specification (/api/v1/api-docs -> components.schemas)
 * Domain: High-Concurrency Optimistic Slot Holds & Double-Booking Prevention (AHC-8)
 */
export type AcquireSlotHoldCommand = components['schemas']['AcquireSlotHoldCommand']
export type SlotHoldResponse = Required<components['schemas']['SlotHoldResponse']>
export type ConfirmAppointmentCommand = components['schemas']['ConfirmAppointmentCommand']
export type ScheduledAppointmentResponse = Required<components['schemas']['ScheduledAppointmentResponse']>
