import { useTenancyStore } from '~/stores/tenancy'

export default defineNuxtRouteMiddleware((to) => {
  const tenancyStore = useTenancyStore()

  if (import.meta.client && typeof window !== 'undefined') {
    const hostname = window.location.hostname
    const parts = hostname.split('.')
    if (parts.length > 1 && parts[0] !== 'localhost' && parts[0] !== 'www') {
      tenancyStore.setResolvedSlug(parts[0])
    }
  }

  if (typeof to.query.tenant === 'string' && to.query.tenant.trim()) {
    tenancyStore.setResolvedSlug(to.query.tenant.trim().toLowerCase())
  }
})
