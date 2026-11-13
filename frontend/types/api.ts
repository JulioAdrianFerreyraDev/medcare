/**
 * RFC 7807 ProblemDetail standard error contract emitted by the Spring Boot 3 backend.
 */
export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail: string
  instance?: string
  timestamp?: string
  fieldErrors?: Record<string, string>
  [key: string]: unknown
}
