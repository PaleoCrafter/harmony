<template>
  <div :class="['server', { 'server--index': isIndex }]">
    <div v-if="server" class="server__sidebar server__sidebar--desktop">
      <header class="server__header">
        <a @click.prevent="$store.commit('openSidebar')" href="#" class="server__header-menu-toggle">
          <MenuIcon />
        </a>
        {{ server.name }}
      </header>
      <ChannelList :server="$route.params.id" :channels="server.channels" />
    </div>
    <portal v-if="server && !isIndex" :order="1" to="sidebar">
      <div :class="['server__sidebar', 'server__sidebar--mobile', { 'server__sidebar--active': sidebarTab === 'channels' }]">
        <header class="server__header">
          <a @click.prevent="$store.commit('setSidebarTab', 'servers')" href="#" class="server__header-menu-toggle">
            <ArrowLeftIcon />
          </a>
          {{ server.name }}
        </header>
        <ChannelList :server="$route.params.id" :channels="server.channels" />
      </div>
    </portal>
    <nuxt-child v-if="server" />
    <div v-else-if="$apollo.loading" class="server__loading">
      <LoadingSpinner />
    </div>
    <div v-else class="server__error">
      <AlertCircleIcon class="server__error-icon" size="4x" />
      The requested server does not exist!
    </div>
  </div>
</template>

<script>
import { AlertCircleIcon, MenuIcon, ArrowLeftIcon } from 'vue-feather-icons'
import { mapState } from 'vuex'
import channelsQuery from '@/apollo/queries/server-channels.gql'
import ChannelList from '@/components/ChannelList.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

export default {
  components: { LoadingSpinner, ChannelList, AlertCircleIcon, MenuIcon, ArrowLeftIcon },
  computed: {
    ...mapState(['sidebarTab']),
    isIndex () {
      const server = this.$route.params.id
      let route = this.$route.path
      if (route.endsWith('/')) {
        route = route.substring(0, route.length - 1)
      }
      return this.$route.path === `/servers/${server}` || this.$route.path === `/servers/${server}/channels`
    }
  },
  apollo: {
    server: {
      query: channelsQuery,
      variables () {
        return {
          id: this.$route.params.id
        }
      }
    }
  }
}
</script>

<style lang="scss">
.server {
  display: flex;
  align-items: stretch;
  flex-grow: 1;

  &__header {
    display: flex;
    align-items: center;
    box-shadow: 0 1px 0 rgba(4, 4, 5, 0.2), 0 1.5px 0 rgba(6, 6, 7, 0.05), 0 2px 0 rgba(4, 4, 5, 0.05);
    padding: 1rem;
    font-weight: 600;
    height: 4rem;
    line-height: 1;

    &-menu-toggle {
      color: white;
      margin-right: 0.25rem;

      @media (min-width: 1200px) {
        display: none;
      }
    }

    @media (max-width: 767px) {
      padding-left: 0.5rem;
    }
  }

  &__sidebar {
    display: flex;
    flex-direction: column;
    background: #2F3136;

    &--mobile {
      display: none;
      flex: 1;
    }

    @media (max-width: 1199px) {
      .channel-list {
        padding-left: 0.25rem;
      }
    }

    @media (max-width: 767px) {
      &--desktop {
        display: none;
      }

      &--active {
        display: flex;

        & + .servers__sidebar {
          display: none !important;
        }
      }
    }

    ::-webkit-scrollbar {
      width: 0.5rem;
      height: 0.5rem;
    }

    ::-webkit-scrollbar-corner {
      background-color: transparent;
    }

    ::-webkit-scrollbar-track {
      border-width: initial;
      background-color: transparent;
      border-color: transparent;
    }

    ::-webkit-scrollbar-track, ::-webkit-scrollbar-thumb {
      background-clip: padding-box;
      border-width: 1px;
      border-style: solid;
      border-radius: 7px;
    }

    ::-webkit-scrollbar-thumb {
      background-color: #202225;
      border-color: transparent;
    }

    ::-webkit-scrollbar-track-piece {
      background-color: #2f3136;
      border: 1px solid #36393f;
      border-bottom: none;
      border-top: none;
    }
  }

  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-grow: 1;
    flex-direction: column;
    font-size: 4rem;
  }

  &__error {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-grow: 1;
    flex-direction: column;
    font-size: 2rem;

    &-icon {
      opacity: 0.5;
    }
  }

  &--index {
    @media (max-width: 767px) {
      .server__sidebar--desktop {
        display: flex;
        width: 100%;

        .channel-list {
          width: 100%;
          max-width: 100%;
        }
      }
    }
  }
}
</style>
