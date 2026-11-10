import { useRoute } from 'vue-router'
import { associationQuery } from '@/navigation/business'

// The layout remounts a business page when its URL changes, including browser back/forward.
export function useListQuery() {
  const route = useRoute()
  const association = associationQuery(route.path, route.query)
  let search: Record<string, unknown> = {}
  return (params?: Record<string, unknown>) => {
    if (params) search = { ...params }
    // Association scope stays explicit until the user clicks “查看全部”.
    return { ...search, ...association }
  }
}
