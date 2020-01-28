import Vue from 'vue'
import identityQuery from '@/apollo/queries/identity.gql'

export const state = () => ({
  loggedIn: false,
  identity: null,
  timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  sidebarOpen: false,
  sidebarTab: 'channels',
  collapsedCategories: {},
  modal: null,
  searchQuery: null,
  highlightedMessage: null,
  modalSearchActive: false
})

export const getters = {
  modalOpen (state) {
    return state.modal !== null
  }
}

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
  openModal (state, modal) {
    state.modal = modal
  },
  closeModal (state) {
    state.modal = null
  },
  setHighlightedMessage (state, message) {
    state.highlightedMessage = message
  },
  resetHighlightedMessage (state) {
    state.highlightedMessage = null
  },
  search (state, query) {
    state.searchQuery = query
  },
  resetSearch (state) {
    state.searchQuery = null
  },
  startModalSearch (state) {
    state.modalSearchActive = true
  },
  stopModalSearch (state) {
    state.modalSearchActive = false
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
        commit('setTimezone', Intl.DateTimeFormat().resolvedOptions().timeZone)
      }
    }
  }
}
