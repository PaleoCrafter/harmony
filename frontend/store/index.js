import Vue from 'vue'

export const state = () => ({
  collapsedCategories: {}
})

export const mutations = {
  populateCategories (state) {
    state.collapsedCategories = JSON.parse(window.localStorage.getItem('collapsedCategories') ?? '{}')
  },
  toggleCategory ({ collapsedCategories }, { server, type, category }) {
    const serverTypes = collapsedCategories[server] ?? (Vue.set(collapsedCategories, server, {}))
    const categories = serverTypes[type] ?? (Vue.set(serverTypes, type, {}))
    Vue.set(categories, category, !(categories[category] ?? false))
    if (process.browser) {
      window.localStorage.setItem('collapsedCategories', JSON.stringify(collapsedCategories))
    }
  }
}
