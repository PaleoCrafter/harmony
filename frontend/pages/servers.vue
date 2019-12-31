<template>
  <div class="servers">
    <div class="servers__sidebar servers__sidebar--desktop">
      <ServerList :servers="servers" />
      <UserPanel />
    </div>
    <portal :order="1" to="sidebar">
      <div class="servers__sidebar servers__sidebar--mobile">
        <ServerList :servers="servers" />
      </div>
    </portal>
    <nuxt-child />
  </div>
</template>

<script>
import serversQuery from '@/apollo/queries/servers.gql'
import ServerList from '~/components/ServerList.vue'
import UserPanel from '@/components/UserPanel.vue'

export default {
  components: { UserPanel, ServerList },
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
}
</style>
