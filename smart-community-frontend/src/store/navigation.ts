import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { Router } from 'vue-router'
import { getMenuTree, type MenuItem } from '@/api/system'
import { isInternalPath } from '@/navigation/business'

export const useNavigationStore = defineStore('navigation', () => {
  const menus = ref<MenuItem[]>([])
  const loading = ref(false)
  const error = ref(false)
  async function load(router: Router) {
    menus.value = []
    loading.value = true
    error.value = false
    try {
      const tree = await getMenuTree()
      const normalize = (items: MenuItem[], parent = ''): MenuItem[] => items.flatMap(item => {
        if (item.visible === 0 || item.status === 0 || (item.menuType ?? item.type) === 3) return []
        const raw = (item.path || '').trim()
        const path = raw.startsWith('/') ? raw : parent + '/' + raw
        if (!raw || !isInternalPath(path)) return []
        const children = normalize(item.children || [], path)
        const target = router.resolve(path)
        if (!children.length && (!target.name || target.name === 'NotFound' || (item.menuType ?? item.type) === 1)) return []
        return [{ ...item, path, menuName: item.menuName || item.name, menuType: item.menuType ?? item.type, children }]
      })
      menus.value = normalize(Array.isArray(tree) ? tree : [])
    } catch {
      error.value = true
    } finally {
      loading.value = false
    }
  }
  return { menus, loading, error, load }
})
