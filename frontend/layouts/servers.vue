<template>
  <BaseLayout>
    <div :class="['servers', { 'servers--index': isIndex }]">
      <div class="servers__sidebar servers__sidebar--desktop">
        <ServerList :servers="servers" :index="isChannelIndex" />
        <UserPanel />
      </div>
      <portal :order="2" to="sidebar">
        <div :class="['servers__sidebar', 'servers__sidebar--mobile', { 'servers__sidebar--active': sidebarTab === 'servers' }]">
          <div class="servers__sidebar-header">
            <XIcon @click="$store.commit('closeSidebar')" class="servers__sidebar-close" />
          </div>
          <ServerList :servers="servers" :index="isChannelIndex" />
        </div>
      </portal>
      <nuxt />
    </div>
  </BaseLayout>
</template>

<script>
import { XIcon } from 'vue-feather-icons'
import { mapState } from 'vuex'
import serversQuery from '@/apollo/queries/servers.gql'
import ServerList from '~/components/ServerList.vue'
import UserPanel from '@/components/UserPanel.vue'
import BaseLayout from '@/components/BaseLayout.vue'

export default {
  components: { BaseLayout, UserPanel, ServerList, XIcon },
  computed: {
    ...mapState(['sidebarTab']),
    isIndex () {
      return this.$route.path === '/servers' || this.$route.path === '/servers/'
    },
    isChannelIndex () {
      const server = this.$route.params.id
      let route = this.$route.path
      if (route.endsWith('/')) {
        route = route.substring(0, route.length - 1)
      }
      return this.$route.path === `/servers/${server}` || this.$route.path === `/servers/${server}/channels`
    }
  },
  apollo: {
    servers: {
      query: serversQuery
    }
  }
}
</script>

<style lang="scss">
.servers {
  display: flex;
  align-items: stretch;
  flex-grow: 1;

  &__sidebar {
    display: flex;
    flex-direction: column;
    background: #202225;

    &-header {
      display: flex;
      align-items: center;
      padding: 1rem;
      background: #2b292f;
      height: 4rem;
    }

    &-close {
      cursor: pointer;
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

    @media (max-width: 1199px) {
      &--desktop {
        display: none;
      }

      &--mobile {
        flex: 1;
      }
    }
  }

  &--index {
    @media (max-width: 1199px) {
      .servers__sidebar--desktop {
        display: flex;
      }
    }

    @media (max-width: 767px) {
      .servers__sidebar--desktop {
        width: 100%;

        .server-list {
          width: 100%;
          max-width: 100%;
        }
      }
    }
  }
}
</style>
