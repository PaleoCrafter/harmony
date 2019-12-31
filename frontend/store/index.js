import Vue from 'vue'
import identityQuery from '@/apollo/queries/identity.gql'

export const state = () => ({
  loggedIn: false,
  identity: null,
  sidebarOpen: false,
  collapsedCategories: {}
})

export const mutations = {
  setLoginStatus (state, { loggedIn, identity }) {
    state.loggedIn = loggedIn
    state.identity = identity
  },
  openSidebar (state) {
    state.sidebarOpen = true
  },
  closeSidebar (state) {
    state.sidebarOpen = false
  },
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

export const actions = {
  async nuxtServerInit ({ commit }, { app }) {
    try {
      const { data: { identity } } = await app.apolloProvider.clients.identityCheck.query({ query: identityQuery })
      commit('setLoginStatus', { loggedIn: true, identity })
    } catch (error) {
      if (error.networkError && error.networkError.statusCode === 401) {
        commit('setLoginStatus', { loggedIn: false, identity: null })
      }
    }
  }
}
