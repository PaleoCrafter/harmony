import Vue from 'vue'
import identityQuery from '@/apollo/queries/identity.gql'

export const state = () => ({
  loggedIn: false,
  identity: null,
  timezone: new Date().getTimezoneOffset(),
  sidebarOpen: false,
  sidebarTab: 'channels',
  collapsedCategories: {},
  historyMessage: null
})

export const mutations = {
  setLoginStatus (state, { loggedIn, identity }) {
    state.loggedIn = loggedIn
    state.identity = identity
  },
  setTimezone (state, timezone) {
    state.timezone = timezone
  },
  openSidebar (state) {
    state.sidebarOpen = true
    state.sidebarTab = 'channels'
  },
  closeSidebar (state) {
    state.sidebarOpen = false
  },
  setSidebarTab (state, tab) {
    state.sidebarTab = tab
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
  },
  openMessageHistory (state, message) {
    state.historyMessage = message
  },
  closeMessageHistory (state) {
    state.historyMessage = null
  }
}

export const actions = {
  async nuxtServerInit ({ commit }, { app }) {
    try {
      const { data: { identity: { user, timezone } } } = await app.apolloProvider.clients.identityCheck.query({ query: identityQuery })
      commit('setLoginStatus', { loggedIn: true, identity: user })
      commit('setTimezone', timezone)
    } catch (error) {
      if (error.networkError && error.networkError.statusCode === 401) {
        commit('setLoginStatus', { loggedIn: false, identity: null })
        commit('setTimezone', new Date().getTimezoneOffset())
      }
    }
  }
}
